# GenAIx Phase One contracts

This package is the set of contracts for integrating GenAIx into Gentics CMP9's Agentic
Workspace: a standardized GenAIx API, the CMS MCP layer specification, the Content RAG contract,
construct and devtools requirements, workflow modules with quality gates, and the integration
guide tying the pieces together. It is designed by ioflair. It is not working software for the
final integration; it is what that integration is built against, together with mocks and a
reference client that let it be tried out end to end.

## Reading order

| # | Document | Covers |
|---|---|---|
| 01 | `01-architecture.md` | The architecture of the GenAIx/CMP9 integration: what GenAIx does, how it connects to the CMS, and the contracts the Workspace UI and the CMS team build against |
| 02 | `02-auth-and-context-flow.md` | Identity, authentication and context propagation in full: actors, headers, sequence diagrams, failure modes, the external-agent path |
| 03 | `03-genaix-api.md` | Narrative guide to `openapi.yaml`: resources, session lifecycle, the event model, the message-part registry, the workflow view, errors, and scenario walkthroughs |
| 04 | `04-mcp-scope.md` | The CMS MCP server: tool groups, per-tool purpose/input/output/REST mapping/role, scenario coverage, what is explicitly excluded |
| 05 | `05-content-rag-contract.md` | The `search_content` tool family, the search endpoint it wraps, and example questions through to answers |
| 06 | `06-construct-devtools-requirements.md` | Construct (tag type) creation through the CMS REST API: the REST call sequence, validation and similarity checks, what remains filesystem-only |
| 08 | `08-workflow-modules.md` | Workflows as versioned modules with quality gates: module layout, the manifest and verdict schemas, the four gate kinds, the gate catalogue per scenario, security boundaries |
| 09 | `09-integration-guide.md` | How the API contract and the MCP specification are wired together: credential provisioning, the streaming proxy's requirements, header forwarding, timeouts |

Read 01 first regardless of role. A Workspace UI developer then wants 03, 08 and the
`examples/` directory; a CMS integrator wants 02, 04, 05, 06 and 09.

## Machine-readable artifacts

These are normative: where a prose document and one of these disagree, the artifact wins.

- **`openapi.yaml`** — the GenAIx API, OpenAPI 3.1, complete schemas, examples and SSE event
  catalogues. What `03-genaix-api.md` narrates.
- **`schemas/events.schema.json`**, **`schemas/parts.schema.json`**,
  **`schemas/user-parts.schema.json`**, **`schemas/problem.schema.json`**,
  **`schemas/workflow-manifest.schema.json`**, **`schemas/gate-verdict.schema.json`** — standalone
  JSON Schema for the SSE event payloads, assistant message parts, user message parts, error
  bodies, the workflow module manifest and the quality-gate verdict shape, for anyone validating
  offline without an OpenAPI toolchain.
- **`mcp/tools.json`** (with `mcp/README.md`) — the machine-readable CMS MCP tool specification:
  every tool's name, input/output schema and REST mapping. What `04-mcp-scope.md` narrates.
- **`examples/`** — `requests.http` (every GenAIx API operation as a runnable request, in
  lifecycle order), five `.sse` transcripts (one per core scenario, plus a failure-and-recovery
  transcript for the error and credential-recovery events, each a complete schema-valid event
  stream), and `session-lifecycle.md` (the same ground as `03-genaix-api.md`, ordered as a
  client's call sequence).

## Mocks and reference client

Two mock services and one static client let integration work start before the real components
exist:

- **`mocks/genaix-mock/`** — a Python service implementing every route of `openapi.yaml` with
  in-memory state, replaying the recorded scenario transcripts in `examples/` as live
  Server-Sent Events. Run with `./run.sh` from that directory; see `mocks/genaix-mock/README.md`.
- **`mocks/cms-mcp-mock/`** — a mock/skeleton of the CMS MCP server, generated from
  `mcp/tools.json`, with fixture CMS data. Run with `./run.sh` from that directory; see
  `mocks/cms-mcp-mock/README.md`.
- **`client/`** — a static HTML/JS reference client consuming the API end to end: chat, message
  parts, the live event stream, interactions, MCP connections, quality gates and the session
  lifecycle. No build step. Serve with `./serve.sh` from that directory; see `client/README.md`.

## Rendered documentation

- **`html/`** — a static HTML rendering of every markdown document here, for reading outside an
  editor. Regenerate with `python3 html/build-html.py` after editing any `.md` file; open
  `html/index.html`.
- **`docs/`** — a browsable rendering of `openapi.yaml` and `mcp/tools.json` with a standard API
  documentation renderer, plus a summary page. Regenerate with `python3 docs/build-docs.py`
  after editing either contract file; open `docs/index.html`.

## Docker compose stack

`deploy/` serves everything above on one port without installing anything locally:
`cd stage1/deploy && docker compose up -d --build`, then <http://localhost:8123/>. One nginx
serves the documents, the contracts, the rendered sites and the reference client, and proxies
the two mocks onto the same origin, so the API mock is at `/api/v1/`, the CMS MCP mock at
`/mcp`, and the whole package is also readable as `/llms.txt`, `/llms-full.txt` and raw
markdown under `/md/`. See `deploy/README.md` for the URL map, the fixture credentials and how
to rebuild after a contract change.

## Contract versions

The GenAIx API (`openapi.yaml`) and the CMS MCP tool specification (`mcp/tools.json`) share the
same version, currently `1.0.0-rc.2`: the contract is agreed by all parties and frozen for development. A change from here is made by agreement, recorded in the changelogs, and a breaking change only where no additive form exists. A revision that touches only one of the two leaves the other at its previous number. Both files carry their own changelog; the summary below
omits detail already there.

- **`1.0.0-draft.1`** — first contract: sessions, messages, runs, the SSE event model, message
  parts, files, the workflow view, RFC 9457 errors.
- **`1.0.0-draft.2`** — MCP access became a first-class resource, with the client registering
  its own credential and GenAIx only verifying, storing and using it. Workflows became versioned
  modules with quality gates: the workflow catalogue publishes each module's inputs, required
  connectors, tool-group allowlist and quality gates; the workflow view gained gate results;
  checks gained gate provenance; transitions gained acknowledgeable non-blocking failures and a
  refusal on unresolved blocking ones.
- **`1.0.0-draft.3`** — MCP access remodelled as connector **types** plus per-user
  **connections**, each connection carrying its own URL and its own credential, so a user can
  hold more than one connection of a type. Connector types are read-only; connections and their
  authorization are managed by the user.
- **`1.0.0-draft.4`** — documentation reorganized: integration guidance moved into its own
  guide, so the API narrative describes GenAIx behaviour and contracts only. No contract change.
- **`1.0.0-draft.5`** — MCP endpoint `/mcp` decided; CMS MCP tool fields camelCase as the REST
  models; typed user message parts. A user message became a list of **typed parts** (`text`,
  `verbatim`, `reference`, `file_ref`, `setting`, `filter`), with the plain text as their
  documented rendering and the references derived from them; input parts are strict where output
  parts fall back to text, and the offset-based verbatim spans are gone. Verbatim files: locking
  a whole upload hashes its extracted text block by block (`unit: blocks`), locking a passage out
  of one hashes just that text (`unit: chars`), and extraction runs in GenAIx either way.
- **`1.0.0-draft.6`** — GenAIx API only, no change to the MCP tool specification. A session can
  be created together with its first message in one call, with the first run already started and
  its id in the response; the two-call flow stays the path for a turn with uploaded files. The
  session's `intent` became derived and read-only: it is the plain rendering of the first user
  message.
- **`1.0.0-draft.7`** — GenAIx API only, no change to the MCP tool specification. Two security
  changes. **Pseudonymous identity**: the caller is one opaque header, `X-GCMS-Subject`,
  optionally a signed assertion GenAIx verifies; the user id, login and group headers are gone,
  no user id or login appears in the contract, and `DELETE /subjects/{subject}` erases
  everything held for one subject. **Session-scoped credentials**: a client registers the token
  one session uses for one connection, on the session or on the create call next to the first
  message, and GenAIx deletes it when the session is published, discarded, archived or the
  token expires. Nothing long-lived is stored. The per-user authorization stays as an optional
  standing fallback, and the session one takes precedence. Every example with a message now shows the `parts` form first, with the plain
  `content` form kept alongside it as the plain client variant.
- **`1.0.0-draft.8`** — GenAIx API only, no change to the MCP tool specification. Settings may be
  stated as plain text and are parsed by GenAIx itself, so the composer's chips are no longer the
  only way to be understood — but a setting that was inferred is never used for a CMS write until
  the user has confirmed it. A new interaction kind, `settings_review`, carries what the run
  proposes (each value with its source, and with the quoted evidence behind an inference) and what
  it could not derive; the answer is a list of `setting` parts. GenAIx stores those confirmed parts
  as a user message linked back to the review and lists their keys in
  `derived_settings.corrected_by_user`, so the decision sits in the history as the user's own. The
  write-governing settings — `node`, `folder`, `template`, `language`, `publish_at` — must come
  from a chip, from the session context, or from a confirmed review before the first write; read
  and search tools may run before it.
- **`1.0.0-rc.1`** — agreed by all parties and frozen for development. No change against
  `1.0.0-draft.8` in either file; the number marks the checkpoint implementations build from.
- **`1.0.0-rc.2`** — agreed by all parties after the first GenAIx implementation against `rc.1`, which
  found the kit silent or inconsistent in a number of places. Both files change, nothing is
  renamed or removed. GenAIx API: `QualityGate.triggers` (a gate bound to several steps or to
  a step and a transition; `when` / `step_id` keep the first trigger), `substeps` on the
  catalogue's step template, `publish_at` on `DerivedSettings` and `SessionContext`, a
  `document` reference type, `model_rate_limited` as a run failure code, `label` and three
  session fields required as every example already showed them; decided that `tool.started.tool`
  is the bare wire name with `group` alongside, how `Check.blocking` and `attempt` are counted,
  the `WorkflowState.state` derivation, the order of gates and run cancellation on `release`,
  the idempotency and `202` body of transitions, and that `request_review` reaches the CMS only
  for a user who cannot publish directly; and clarifications of the event stream, the settings
  review, availability at session creation, credential verification, the verbatim reading of an
  upload and the error vocabulary. CMS MCP: five input schemas lost their root-level `anyOf`
  (the rule moved to a tool-level `inputRule` the server enforces), `tools/list` is computed
  per tool so a content creator sees the construct catalogue reads, `list_templates` is phase
  one in `content_read`, part references are `cms.tag.parts.<keyword>`, `count_content`
  counts what `search_content` matches, and `X-GenAIx-Gate` is a correlation header. The S1,
  S2 and S3 transcripts were re-recorded to the same rules (bare tool names, every declared
  step gate reported, no type 43 part in a draft, an anonymous user actor in the approval
  chain), and both mocks follow.
