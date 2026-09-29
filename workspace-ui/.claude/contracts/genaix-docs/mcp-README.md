# CMS MCP tool specification

`tools.json` is the machine-readable specification of the Gentics CMS MCP server (`cms-mcp`). It is the
contract the CMS team hosts in the cms-mcp module, ioflair implements as tools inside it, and any MCP client consumes. The prose companion, with rationale, scenario coverage,
exclusions and the requirements towards the CMS implementation, is `../04-mcp-scope.md`.

Current version: `1.0.0-rc.2`, agreed and frozen for development, aligned with `../openapi.yaml`. The
`changelog` array records what moved between drafts. Specification owner ioflair, implementation owner
Gentics.

## 1. How to read tools.json

Top-level keys:

| Key | Contents |
| --- | --- |
| `version`, `date`, `status`, `owner` | Contract identity. `version` moves to `1.0.0` at the demo-readiness checkpoint. |
| `roles` | Four suggested role profiles a CMS administrator may map to groups, with the CMS permissions each implies. **Planning metadata**: roles and groups are created dynamically in a CMS installation, so the annotation never names one and the CMS does not generate this field. |
| `scenarios` | The four showcase scenarios S1 to S4, and which of them are mandatory. **Planning metadata**, used to prove coverage; not part of the annotation. |
| `genaix_side_tools` | Agent tools that are **not** part of this server, listed so the split is unambiguous. |
| `implementation_model` | How the server is built: one `@McpTool(name, group, access, permissions)` annotation scanned at start-up, what the scanner derives from the annotated method, the `naming` rule (`camelCase` field names taken straight from the REST models, no transformation), where permissions are evaluated, which fields here are planning metadata rather than annotation input, the generated versus composite split, the three rules, and the conformance test both sides run. |
| `transport` | Endpoint, authentication, headers, statelessness, dynamic tool listing, the CMS-to-MCP error mapping, and the result conventions. `url_template` is `{cms_base_url}/mcp`: the path is decided, the MCP server being its own servlet rather than part of the Jersey REST servlet. Treat it as the default an installation may override through `MCP_PATH`, never as a fixed address. `transport.implementation_state` records what the CMS already implements and the authentication gap that blocks everything. |
| `groups` | The seven tool groups. Each has `id`, `title`, `description`, `roles` and `tools`. |
| `definitions` | `ObjectRef` and `Problem`, referenced from tool schemas. |

Each entry in `groups[].tools[]`:

| Field | Meaning |
| --- | --- |
| `name` | The MCP tool name, `snake_case`, globally unique. Not group-prefixed. |
| *(field names)* | Every input and output field, nested fields included, is `camelCase` and identical to the CMS REST model name. The module renames nothing: schemas are generated from the annotated parameters and the Jackson models the REST layer already uses. |
| `title` | Short human label for a UI. |
| `description` | The LLM-facing text. States what the tool does, what to call first, and when **not** to use it. This is what the model sees; treat changes to it as contract changes. |
| `group`, `access`, `roles`, `scenarios`, `phase` | Metadata. `access` is `read` or `write`; `phase` is `one` (Phase One) or `future`. |
| `idempotent` | Whether calling twice with the same input is safe. |
| `inputSchema` | JSON Schema draft 2020-12, **strict**: `additionalProperties: false`, explicit `required`, enums and maxima. Never `anyOf`, `oneOf` or `allOf` at the root (since `1.0.0-rc.2`): MCP hosts that convert tool schemas for a model reject or drop such a root. |
| `inputRule` | Optional, since `1.0.0-rc.2`: `{"exactlyOne": [members], "note"}` for a tool that takes one of two identifying members (`get_construct`: `id` or `keyword`). Enforced by the server with `invalid-data`; a client may pre-validate. Kept outside `inputSchema` for the reason above. |
| `outputSchema` | JSON Schema draft 2020-12, intentionally **open**, so `cms-mcp` can add fields without a contract break. |
| `cms.endpoints` | The CMS REST endpoints the tool wraps, as `{method, path}`, relative to the CMS REST prefix (conventionally `/rest`). |
| `cms.notes` | Exact request and response model names, parameter names, and any implementation constraint. Read this before implementing a tool. |
| `implementation` | `{"kind": "generated"}` when the tool is a one-to-one mapping of an annotated REST resource method, or `{"kind": "composite", "composes": [...], "transaction": "single"}` when a method in the `cms-mcp` module calls several resource implementations in one transaction. `search_content` also carries `"never_exposed_unwrapped": true`, because the raw Elasticsearch passthrough must never become a tool of its own. |
| `permissions` | The CMS permission requirements the annotation declares, as `{type, perms, scope?}` entries in the CMS permission vocabulary. Checked twice: `tools/list` hides a tool the user's effective permissions can never satisfy — per tool, not per group, so a content creator sees the read tools of `constructs` (`1.0.0-rc.2`) — and every call re-checks against the concrete target object. An entry with `"confirm": false` names the type and permission expected rather than one read off the CMS source. |

31 of the 53 tools are generated and 22 are composite; `implementation_model` explains why the split falls
where it does. A `group`'s `roles` summarise its tools' roles and are not a filter; the tool's own `roles` and
`permissions` decide what a user sees.

Three conventions that are easy to miss:

- **`$ref` resolves against the root of `tools.json`**, as `#/definitions/ObjectRef`. Dereference before
  handing a schema to an MCP client or to a validator, since an MCP `inputSchema` must be self-contained:

  ```python
  def deref(node, defs):
      if isinstance(node, dict):
          if set(node) == {"$ref"} and node["$ref"].startswith("#/definitions/"):
              return deref(defs[node["$ref"].split("/")[-1]], defs)
          return {k: deref(v, defs) for k, v in node.items()}
      return [deref(v, defs) for v in node] if isinstance(node, list) else node
  ```
- **A tool with `"phase": "future"` is specified but not implemented.** It exists so the extension has a
  defined shape. Seven of the 53 tools are `future` (`list_templates` became phase one in `1.0.0-rc.2`).
- **`cms.notes` is normative where it contradicts an intuition about the CMS API.** Several tools wrap more
  than one endpoint, and `cms.notes` says in what order and inside what transaction.

Validate the file after any edit:

```bash
python3 -c "import json;json.load(open('mcp/tools.json'));print('ok')"
python3 - <<'EOF'
import json
from jsonschema import Draft202012Validator
d = json.load(open('mcp/tools.json'))
for g in d['groups']:
    for t in g['tools']:
        Draft202012Validator.check_schema(t['inputSchema'])
        Draft202012Validator.check_schema(t['outputSchema'])
print('all', sum(len(g['tools']) for g in d['groups']), 'tools have valid schemas')
EOF
```

## 2. What the groups are for

| Group | Tools | Phase One | Purpose |
| --- | --- | --- | --- |
| `identity` | 3 | 3 | Who the user is, which nodes they see, what they may do. |
| `search` | 4 | 4 | Permission-filtered retrieval over the CMS Elasticsearch indices, plus relational reverse lookups. |
| `content_read` | 10 | 9 | Reading the tree and individual objects; page preview. |
| `content_write` | 10 | 10 | The only mutation path for content. One object per call. |
| `constructs` | 13 | 12 | Tier 1 Handlebars construct authoring, part type and datasource discovery, and devtools package membership. |
| `admin` | 7 | 7 | Users, groups and the permission impact preview. Scenario S4, optional. |
| `design` | 6 | 0 | Design Studio placeholders: templates and render diff. |

`tools/list` is computed per user, and whole groups disappear when the user's type-level permissions rule
them out. A client must tolerate the list changing between calls.

## 3. How a client consumes it

A client should not hand the model every tool the server offers. Narrow the toolbox per task, because a
smaller, relevant tool set measurably improves tool choice:

```
effective_tools = tools_list_from_server  ∩  per_task_allowlist  ∩  phase_one
```

Suggested allowlists for the four showcase scenarios:

| Task | Scenario | Allowed groups | Notes |
| --- | --- | --- | --- |
| Content creation from source material | S1 | `identity`, `search`, `content_read`, `content_write`, plus `list_constructs` and `get_construct` | No construct creation: S1 must use existing, allowed constructs only. |
| Content research and reporting | S2 | `identity`, `search`, `content_read` | Read-only by default. `take_offline`, `delete_page` and `restore_page_version` are added only once the user asks for a change, and each should still be confirmed. |
| Construct creation | S3 | `identity`, `search`, `constructs`, `content_read`, plus `create_page`, `add_page_tag`, `update_page_tags` | The page tools exist so the agent can prove the construct renders. `list_part_types` is the mandatory first step, since part type ids are installation configuration. |
| Administration | S4 | `identity`, `admin` | Optional scenario. |
| Free chat | none | `identity`, `search`, `content_read` | Read-only. Any write should require an explicit task. |

Four things the server cannot do for a client, which a client therefore has to:

- **Compile the Handlebars template before calling `create_construct`.** No CMS endpoint compiles one, so an
  invalid template is accepted and fails later at render time.
- **Decompose a question into several `search_content` calls and verify the hits with `get_page` before
  citing them.** Retrieval indexes only text an editor typed into a tag, so a snippet is evidence, not proof.
- **Prove verbatim text** by reading the page back with `get_page` after `update_page_tags` and comparing
  character by character.
- **Keep its own audit trail** and send the correlation headers of `../04-mcp-scope.md` §2.3, so its log and
  the CMS access log can be joined afterwards.

How a multi-user host obtains and stores a per-user CMS token, models connections to one or more CMS
instances, and recovers when a credential is refused are integration concerns. They are described in
`../09-integration-guide.md`, not here.

Drift control: `tools.json` is the specification and the server's generated `tools/list` is the
implementation. A test that diffs tool names, `required` lists and `access` between the two catches drift in
both directions, and should run in both repositories.

## 4. Connecting an external LLM client

Nothing in the server is specific to one client. A developer needs a CMS API token for their own user. Create one
against the CMS, either in the token management UI or directly:

```bash
# log in, keep the session cookie, then create a token for yourself
curl -sc cookies.txt -X POST https://cms.example.com/rest/auth/login \
  -H 'Content-Type: application/json' -d '{"login":"<user>","password":"<password>"}'
curl -sb cookies.txt -X POST https://cms.example.com/rest/admin/token \
  -H 'Content-Type: application/json' \
  -d '{"name":"claude-code","expires":0,"pruneOnExpiry":false}'     # secret is returned once
```

The creation request is `{name, expires, pruneOnExpiry}`. `expires` is a Unix timestamp in seconds at which
the token stops working, `0` means never, a past value is rejected, and the token is valid only while
`expires > now`. `pruneOnExpiry: true` makes the CMS delete the token row when it expires. A developer token
like the one above is the long-lived case; an agent host does the opposite and creates one short-lived token
per workflow session, named `genaix-<session id>`, with `pruneOnExpiry: true`, so `cms-mcp` sees many
short-lived tokens per user rather than one standing credential (`../04-mcp-scope.md` §2.2).

`GET /rest/admin/token` lists your tokens and `DELETE /rest/admin/token/{id}` revokes one; both act on the
current user only. Then:

```bash
claude mcp add --transport http gentics-cms https://cms.example.com/mcp \
  --header "Authorization: Bearer <CMS API token>"
claude mcp list          # shows gentics-cms as connected
```

The endpoint is `/mcp`, mounted on the servlet context outside the REST application as an exact servlet
mapping, because the MCP server is its own servlet rather than part of the Jersey REST servlet. It stays
configurable through `MCP_PATH`, so substitute whatever endpoint your installation is configured with rather
than hard-coding the command above. See `../04-mcp-scope.md` §2.1 and §2.6.

The `--header` form below is the norm rather than a workaround: `cms-mcp` is not an OAuth resource server
yet, so there is no flow for a client to run (`../04-mcp-scope.md` §2.2.1).

**What works today.** The transport is implemented (the CMS MCP servlet draft): `initialize`,
capability negotiation and session handling all work, and an `initialize` call returns `serverInfo`
`{name: "Gentics CMS", version: ...}` with `capabilities.tools`. But **no tool is registered yet**, and
`/mcp` is **not yet authenticated**, so a bearer token is currently ignored rather than checked. Because
`/mcp` lies outside the `/rest/*` filter chain, that authentication has to be wired into the MCP servlet
itself; nothing in the REST stack can supply it. A connection that succeeds today proves the transport
works, not that authentication does. Everything below describes the endpoint once the CMS implementation
lands the authentication requirements in `../04-mcp-scope.md` §2.2.

A quick check against a running CMS, without any client:

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{
        "protocolVersion":"2025-06-18","capabilities":{},
        "clientInfo":{"name":"curl","version":"1.0"}}}'
```

Expect `200`, an `Mcp-Session-Id` response header, and `serverInfo` naming `Gentics CMS`. The MCP Inspector
works too, with transport Streamable HTTP against the same URL.

What to expect:

- The tool list is scoped to **your** CMS permissions, so another developer connecting to the same server
  may see fewer groups.
- The correlation headers `X-GenAIx-Session`, `X-GenAIx-Run` and `X-GenAIx-Gate` are optional. Omitting them yields a log line
  with null correlation ids, not a rejection.
- A revoked or expired token produces **HTTP 401 at the transport**, before any MCP envelope, so the client
  reports a connection failure rather than a tool error. Revoking with `DELETE /rest/admin/token/{id}` takes
  effect on the next call.
- The token identifies you, not a snapshot of your rights, so a group change applies on the next call without
  a new token.
- Write tools behave the same for every client, so nothing protects you except the tool descriptions
  themselves. Read them rather than inferring behaviour from the names: they say what a tool needs first and
  when not to use it.

The server declares `tools` with `listChanged`, so a client may be notified when the set of tools changes
rather than re-polling. Per-user filtering is a separate matter: one `McpSyncServer` holds a single tool
registry, so the filtering has to happen in the list handler (`../04-mcp-scope.md` §2.4).

## 5. Example: `tools/call` for `search_content`

Request:

```json
{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"search_content","arguments":{
  "query":"Nutzungsbedingungen Aktualisierung","types":["page"],"nodeId":3,
  "languages":["de"],"filters":{"online":true,"editedAfter":1767225600},
  "size":2}}}
```

Response (abridged to the shape; `structuredContent` matches the tool's `outputSchema`):

```json
{"jsonrpc":"2.0","id":7,"result":{"isError":false,
 "content":[{"type":"text","text":"2 of 7 pages match 'Nutzungsbedingungen Aktualisierung' in node 3 (de), online only. Read one with get_page."}],
 "structuredContent":{"total":7,"totalIsExact":false,"tookMs":24,"hits":[
  {"ref":{"type":"page","id":4711,"nodeId":3,"name":"Neue Nutzungsbedingungen ab dem naechsten Quartal","path":"/Policies/Terms of service/","language":"de"},
   "score":8.41,"snippets":["Die <em>Aktualisierung</em> der <em>Nutzungsbedingungen</em> betrifft ..."],
   "language":"de","online":true,"edited":1772150400,"templateId":9,"folderId":57},
  {"ref":{"type":"page","id":4690,"nodeId":3,"name":"FAQ zu den neuen Nutzungsbedingungen","path":"/Service/","language":"de"},
   "score":5.02,"snippets":["... die geaenderten <em>Widerrufsfristen</em> ..."],
   "language":"de","online":true,"edited":1770854400,"templateId":9,"folderId":61}],
  "queryUsed":{"query":{"bool":{"must":[{"query_string":{"query":"Nutzungsbedingungen Aktualisierung","fields":["name","description","content","path"]}}],
   "filter":[{"term":{"online":true}},{"range":{"edited":{"gte":1767225600}}}]}},"highlight":{"fields":{"content":{},"description":{}}},"size":2}}}}
```

Every field in that exchange is spelled as the CMS REST models spell it: `nodeId`, `templateId`, `folderId`,
`editedAfter`, `totalIsExact`. Only the tool name itself is `snake_case`.

The hits carry a reference and selection metadata only: no embedded CMS object, so a client calls `get_page`
on a hit before quoting it. `totalIsExact: false` says the 7 is a permission-group upper bound.

Note the `bool` wrapper in `queryUsed`. It is not cosmetic: the CMS adds the `groupId` permission filter,
the `nodeId` term and the `deleted` term to `query.bool.filter`, and its `addFilters` method forwards the
body **unfiltered** when there is no `query.bool` to add them to. `cms-mcp` must therefore wrap every
outgoing body, including a caller-supplied `rawQuery`, which is a MUST in `../04-mcp-scope.md` §4.2.1 and
recorded in the `cms.notes` of all three search tools. `queryUsed` shows the body `cms-mcp` sent, not the
final body Elasticsearch executed after the CMS added its filters. On failure the result carries `isError: true` and
`structuredContent.problem` shaped like `definitions.Problem`, for example
`{"type":"https://cms.gentics.com/problems/search-unavailable","status":503,...}` when the CMS feature
`elasticsearch` is not activated.
