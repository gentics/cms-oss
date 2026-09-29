# cms-mcp-mock

A mock/skeleton of the Gentics CMS MCP server (`cms-mcp`), generated from
[`../../mcp/tools.json`](../../mcp/tools.json) (the contract) so GenAIx and other MCP clients
can be built and tested against it before Gentics implements the real Java module described in
[`../../04-mcp-scope.md`](../../04-mcp-scope.md). This is a development aid, not a reference
implementation of CMS behaviour: read "What is mocked vs real" below before relying on any
detail beyond tool names and shapes.

## How to run

```bash
./run.sh
```

This creates `.venv` (via `uv venv` if `uv` is installed, else `python3 -m venv`), installs the
pinned `requirements.txt`, and starts the server on `http://localhost:8765/mcp`. Environment
overrides:

| Variable | Default | Meaning |
| --- | --- | --- |
| `CMS_MCP_PATH` | `/mcp` | The MCP endpoint path (streamable HTTP, GET/POST/DELETE). Decided by the CMS team: the MCP server is its own servlet, not part of the Jersey REST servlet, so it is mounted at `/mcp`, outside `/rest/*`; configurable. |
| `CMS_MCP_HOST` | `0.0.0.0` | Bind host |
| `CMS_MCP_PORT` | `8765` | Bind port |
| `CMS_MCP_AUDIT_LOG` | `./audit.log` | Path to the JSON-lines audit log |
| `CMS_MCP_TOOLS_JSON` | `../../mcp/tools.json` | Path to the contract file |

`GET /healthz` returns `{"status": "ok", ...}` once the server is ready.

## How to connect with Claude Code

```bash
claude mcp add --transport http cms-mcp http://localhost:8765/mcp \
  --header "Authorization: Bearer cc-demo-token"
claude mcp list   # shows cms-mcp as connected
```

Swap the token to see a different role's tool list (§"Fixture tokens" below). Any MCP client
speaking streamable HTTP works the same way; there is nothing GenAIx-specific about the
transport.

`initialize` reports `serverInfo.name` as `"Gentics CMS"` with instructions `"MCP server of a
Gentics CMS instance. It provides access to the CMS REST API."`, and announces the `tools`
capability with `listChanged: true`, matching the CMS MCP servlet draft (streamable HTTP
transport via the MCP Java SDK). The endpoint path is `/mcp`, its own servlet outside the REST
API: the CMS team decided the MCP server is mounted on the servlet context at `/mcp`, not under
`/rest`, and that mapping is what this mock defaults to. The draft servlet registers no tools yet
and does not authenticate the MCP endpoint yet, whereas this mock enforces the bearer-token model
`04-mcp-scope.md` §2.2 specifies as the target contract — build clients against this mock's auth
behaviour, not today's draft servlet's.

Responses also carry an `Mcp-Session-Id` header, again matching that server and the Java SDK's
default (the official Python SDK sets this whenever the transport runs in its default,
non-stateless mode, which this mock now does). It is a transport-level correlation id only: every
call is still independently authenticated by its own bearer token, per `04-mcp-scope.md` §2.1-2.2.

## Fixture tokens

Three named tokens are pre-provisioned in `fixtures/tokens.json`, one per showcase role from
`04-mcp-scope.md` §4:

| Token | User | Login | Groups | Roles seen by `tools/list` |
| --- | --- | --- | --- | --- |
| `cc-demo-token` | Jana Doe | `jdoe` | Content Creator | `content_creator` |
| `chiefcc-demo-token` | Max Mueller | `mmueller` | Chief Content Creator | `content_creator`, `chief_content_creator` |
| `admin-demo-token` | Alex Smith | `asmith` | CMP Administrator | `cmp_admin` |

`tools/list` is filtered per call: a tool is visible if its `roles` in `tools.json` includes
`any` or intersects the caller's roles. An unknown bearer token gets **HTTP 401 before the MCP
envelope** (`04-mcp-scope.md` §2.2), matching the real contract. Two more users exist in
`fixtures/users.json` (`afischer` in Content Creator, `khuber` in Chief Content Creator) without
their own token, to give `list_users`/`list_groups` something to enumerate.

Note the CMP Administrator group does *not* carry the CMS `publishpages` permission in
`fixtures/groups.json` (only the two Content Creator groups do), so `publish_page` called as
`admin-demo-token` deliberately returns `queued_for_approval` while the same call as
`cc-demo-token` or `chiefcc-demo-token` returns `published`. This exercises the approval branch
described in `04-mcp-scope.md` §4.4 without any special-casing in the tool itself.

## Provisioning a token like the CMS proxy would (mock of the token endpoints)

`04-mcp-scope.md` §2.2 and §10 item 2 note that GenAIx needs "create or look up a named API
token for the current user" from the CMS, and that no such REST resource exists yet in the
checked-out CMS source. This mock stands in three routes under the CMS REST prefix, gated by a
mock of the CMS session cookie rather than a bearer token (since token creation is what
bootstraps the bearer token in the first place):

| Route | Purpose |
| --- | --- |
| `POST /rest/admin/token` | Body `{"name": "...", "expires": 0, "pruneOnExpiry": false}` → creates a token for the session's user, returns `{token, id, name, expires}` |
| `GET /rest/admin/token` | Lists the session's user's tokens as `{id, name, created, expires, lastUsed}` (never the raw token again) |
| `DELETE /rest/admin/token/{id}` | Revokes one token; it stops working for MCP auth immediately |

`expires` is a Unix timestamp in seconds; `0` (the default) means the token never expires, and a
past or present value is rejected with **HTTP 400**. A token is accepted for MCP auth only while
`expires` is `0` or still in the future; once past, the transport-level bearer check treats it
exactly like an unknown token (**401 before the MCP envelope**). `pruneOnExpiry: true` additionally
makes the mock delete an expired token outright — on the next MCP auth attempt with it, or the
next `GET /rest/admin/token` listing — so it stops appearing at all rather than just failing
auth; `lastUsed` (`null` until first use) is updated on every successful MCP auth with that
token.

All three require a `GCN_SESSION_SECRET` cookie present in `fixtures/tokens.json`'s `sessions`
map:

| `GCN_SESSION_SECRET` value | Resolves to user |
| --- | --- |
| `jdoe-session-secret` | Jana Doe (Content Creator) |
| `mmueller-session-secret` | Max Mueller (Chief Content Creator) |
| `asmith-session-secret` | Alex Smith (CMP Administrator) |

```bash
curl -X POST http://localhost:8765/rest/admin/token \
  -H "Cookie: GCN_SESSION_SECRET=jdoe-session-secret" \
  -H "Content-Type: application/json" -d '{"name": "my-laptop"}'
# {"token":"<random>","id":4,"name":"my-laptop","expires":0}
```

The returned token is immediately usable as an `Authorization: Bearer` value against `/mcp`, and
the three seeded demo tokens above already appear in `GET /rest/admin/token`'s listing.

## What is mocked vs real

Real, and worth relying on for client development:

- The transport: streamable HTTP, one endpoint, bearer auth, HTTP 401 before the MCP envelope.
- Tool names, `inputSchema`/`outputSchema` (dereferenced from `tools.json`, validated with
  `jsonschema` before every dispatch), descriptions, `access`, `phase`.
- Dynamic, per-caller `tools/list` filtered per tool by role: a content creator sees the read tools
  of the `constructs` group and `list_templates`, never the construct writes (`1.0.0-rc.2`).
- The tool-level `inputRule` (`exactlyOne`) of `get_construct`, `update_construct`, `add_page_tag`,
  `add_construct_to_package` and `find_similar`, enforced with an `invalid-data` problem; their
  `inputSchema` carries no root-level `anyOf` any more, so every MCP host lists them (`1.0.0-rc.2`).
- `count_content` counts over the same fields and object types `search_content` matches, so a count
  with a search's own inputs reproduces its total; `list_templates` (phase one since `1.0.0-rc.2`)
  lists the templates a folder or node allows.
- The `phase: future` tools are listed (per this mock's brief) but every call returns a clean
  `not-implemented-phase-one` problem, isolating "the tool exists in the contract" from "the tool
  works here".
- The `content[0]` text summary / `structuredContent` split, the `Problem` envelope shape, and
  the read-tool list envelope (`total`, `items`, `nextFrom`).
- The audit log line shape (`audit.log`, JSON lines) and that it never carries tag values, page
  content, search queries or template source.
- `update_page_tags` writes exactly the string it is given (the S1 verbatim guarantee): no
  normalisation, re-encoding or markup reflow.
- `create_construct` hard-codes the Handlebars part at `typeId` 43 with the template under a
  dedicated `template` field; `typeId` 43 is always rejected inside `parts`, for the same reason.
- Construct part `typeId` is validated dynamically against `list_part_types`' installation-level
  allowlist (`fixtures/part_types.json`), not a fixed enum in code: `create_construct` and
  `update_construct` reject any `typeId` whose `allowedForGeneratedConstructs` is false with
  an `invalid-data` problem, and name the `list_part_types` call to use instead. A select part
  (`typeId` 29/30) must carry a `datasourceId` naming a real `fixtures/datasources.json` entry;
  presence/absence of `datasourceId` for the right `typeId` is enforced structurally by
  `inputSchema`'s `allOf` (so that specific mistake never reaches the handler), existence of the
  referenced datasource is checked here. Handlebars stays the only template engine.
- The `publishpages`-permission branch on `publish_page` (published vs queued_for_approval).
- `search_content`/`count_content`'s trimmed shape (design brief §16): only `query`, `types`,
  `nodeId`, `folderId`, `recursive`, `languages`, `filters: {online, templateIds,
  editedAfter}`, `size`/`from`, `rawQuery` on `search_content`; highlighting is always on and a
  hit carries no embedded object (call `get_page` for content). `count_content`'s `groupBy` is a
  single value in `{language, template, online}`, not an array. Tool names stay `snake_case`;
  every input/output field name is `camelCase`, identical to the CMS REST model names.

Mocked, and not representative of the real CMS:

- **All data lives in memory** and resets on process restart. No database, no Elasticsearch.
- **Search is a substring/token match**, not Lucene: it approximates real full-text behaviour
  (including one deliberate leniency — a query token matches as a substring of an indexed value,
  so `Widerruf` matches the German genitive `Widerrufs` — a real analyzer would stem this; this
  mock does not tokenize with a real analyzer).
- **Locking is not modelled.** Every write "succeeds" without a lock-conflict path; `locked`,
  `lockedBy` are always `false`/absent.
- **Page version history has no per-version content snapshots.** `restore_page_version` updates
  the audit trail (a new version entry, `edited`/`editor` bumped) but does not literally revert
  tag content, because the fixture does not retain historical values.
- **`upload_file` never performs the outbound fetch** for `source.kind: "url"`; it records a
  zero-byte placeholder. The 5&nbsp;MB base64 path does decode and size the payload for real.
- **`translate_page`'s new page is not linked as a language variant** the way the S1/S2
  `languageVariants` field expects for fixture pages (which pair by adjacent id, `100`↔`101`
  etc.); a translated page is a plain new page with the requested language.
- **`preview_permission_impact` and `get_group_permissions`** compose from the three fixture
  groups' flat permission lists, not real per-node/per-language role inheritance.
- **No object-level permission checks** beyond the role-gated tool visibility: any caller whose
  role can see a write tool can act on any fixture object regardless of node/folder scope.
- **`nodeIds` scoping is trivial**: there is exactly one node (id `1`, "Acme GmbH"), so
  cross-node behaviour cannot be exercised here.

## How to add fixtures

Everything under `fixtures/` is loaded once at startup by `state.build_store()` and then mutated
only in memory. To add content:

1. Edit the relevant `fixtures/*.json` file (`pages.json`, `constructs.json`, `templates.json`,
   `folders.json`, `files.json`, `images.json`, `users.json`, `groups.json`, `tokens.json`,
   `part_types.json`, `datasources.json`). Keep ids stable and below the `next_*_id()` starting
   counters in `state.py` (pages start at 200, constructs at 100, categories at 100, users at
   100, files/images at 100) so runtime creates never collide with fixture ids.
2. A page's `tags` map must use tag names that exist in its template's `templateTags`, and each
   tag's `constructId`/`constructKeyword` must resolve in `constructs.json`. A construct part
   with `typeId` 29/30 needs a `datasourceId` that resolves in `datasources.json`; adding a
   part type to the default allowlist means setting `allowedForGeneratedConstructs: true` on
   its `part_types.json` entry.
3. Re-run `pytest tests/test_smoke.py` — it exercises `search_content`, `get_page`,
   `create_construct` and `publish_page` end to end and will catch a shape mismatch immediately,
   because the official `mcp` client validates `structuredContent` against `outputSchema` on
   every call.
4. Never edit `../../mcp/tools.json`. If a fixture change reveals a schema inconsistency there,
   report it instead of patching the contract from this directory.

## Manual testing

```bash
source .venv/bin/activate
python scripts/call.py --list --token chiefcc-demo-token
python scripts/call.py search_content '{"query": "FAQ", "languages": ["de"]}' --token cc-demo-token
python scripts/call.py create_construct '{"keyword": "my_box", "name": {"en": "My Box"}, "nodeIds": [1], "handlebarsTemplate": "<div>{{title}}</div>", "parts": [{"keyword": "title", "name": {"en": "Title"}, "typeId": 1}]}' --token chiefcc-demo-token --session sess-1 --run run-1
```

## Code layout

| Path | Contents |
| --- | --- |
| `server.py` | Starlette app: MCP transport wiring, bearer auth, `tools/list`/`tools/call` dispatch, `/healthz`, `/preview/page/{id}`, the three `/rest/admin/token` routes |
| `toolspec.py` | Loads and dereferences `tools.json`; role-visibility helper |
| `state.py` | In-memory `Store`, seeded from `fixtures/`, plus id counters and token bookkeeping |
| `common.py` | `ObjectRef`/`Problem` builders, list envelope, schema-driven example generator |
| `audit.py` | JSON-lines audit log writer |
| `handlers/` | One module per tool group; `handlers.REGISTRY` auto-collects every public function by name (a function named `search_content` implements the `search_content` tool) |
| `handlers/generic.py` | Fallback for `phase: future` tools and any tool without a hand-written handler |
| `scripts/call.py` | Manual CLI: `python scripts/call.py <tool> '<json args>' --token ...` |
| `tests/test_smoke.py` | Starts the real server as a subprocess and drives it with the official `mcp` client |
