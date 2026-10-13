# GenAIx and CMS contracts (version 1.0.0-rc.4)

## Where things are
- `.claude/contracts/openapi.yaml`: the GenAIx API. The source of truth for every route, request, SSE event and message part the UI handles.
- `.claude/contracts/mcp-tools.json`: the CMS MCP tool specification. Background only; see below.
- `.claude/contracts/genaix-docs/INDEX.md`: map of the prose documents. Start here.
- What changed since rc.2: `03-genaix-api.md` §16 (rc.3 and rc.4), `09-integration-guide.md` §10, and the `changelog` in both contract files (authoritative).

## Rules
- Where a prose document and a contract file disagree, the contract file wins.
- The UI never calls GenAIx directly: every call goes through the CMS custom proxy `/rest/proxy/genaix` (`01-architecture.md`, "System context"). The UI never calls the CMS MCP; GenAIx does.
- Two caller modes (`02-auth-and-context-flow.md`; `09-integration-guide.md` §3), told apart by `X-GenAIx-Installation`:
  - proxied (production): the CMS proxy adds `X-GenAIx-Installation: <installation token>` and `Authorization: Bearer <RS256 CMS identity assertion>`.
  - direct (development, mocks): `Authorization: Bearer <installation token>` plus `X-GCMS-Subject`, only against an installation without a CMS key. A dev proxy in direct mode clears `X-GenAIx-Installation`, otherwise a request switches to proxied mode.
- The CMS proxy forwards no `PATCH`: send `PATCH` routes as `POST` with `X-HTTP-Method-Override: PATCH` (`PATCH /sessions/{session_id}`, `PATCH /mcp/connections/{connection_id}`).
- Since rc.4 the session is the conversation and the plan is the unit of work (`03-genaix-api.md` §3, §4, §5.3, §8):
  - A session has no `workflow` of its own; each plan has its own `workflow`, status (`PlanStatus`), steps, checks, gates, publications and approval chain.
  - Plan-scoped events carry `plan_id`; key plan state on `plan_id` plus the object id.
  - Read plans with `GET /sessions/{session_id}/plans` and `.../plans/{plan_id}` (`PlanState`). `GET .../workflow` and `WorkflowState` are deprecated.
  - Plan transitions use `POST /sessions/{session_id}/plans/{plan_id}/transitions`. `POST /sessions/{session_id}/transitions` takes only `archive` and `unarchive`.
  - Only a user message with a `plan` part (`plan_id`, `based_on_version`, optional `items`) and `options.mode: execute` accepts a plan.
  - `free_chat` is gone; without `workflow` a session starts with the session agent.
- From `mcp-tools.json` the UI needs only:
  - the `inputSchema` of `search_content` and `count_content`: the keys of `UserFilterPart.criteria` must use these field names, camelCase as spelled there (`openapi.yaml`, `UserFilterPart`)
  - tool names and groups, if `tool.started` gets custom labels. `tool.started.group` is a CMS group or `genaix`; the `genaix` tools are listed in `genaix_side_tools.tools`
- Tools in the `design` group are `phase: "future"`; don't build UI for them.
- Don't invent fields. If a shape is not in `openapi.yaml`, say so and ask.

## Local mock
`INDEX.md` says the mock source is not included here. It is in the hand-out repo instead:
- `git@git.gentics.com:psc/genaix/api-contract.git`, checked out at `../../api-contract` from this app (the path `npm run mock:genaix` uses). The GenAIx mock is `mocks/genaix-mock/`, its transcripts are `examples/*.sse` and `mocks/genaix-mock/scripts/*.sse`.
- Start it with `npm run mock:genaix` (`GENAIX_MOCK_PORT=8123 ./run.sh` in `mocks/genaix-mock/`; Python 3.11+, creates `.venv` on first run). It serves `http://localhost:8123/api/v1`, the same port as the docker compose stack of the hand-out (`deploy-README.md`). `run.sh` alone defaults to `8080`, which the docs under `contracts/` still name.
- Fixture credentials only, in direct mode: `Authorization: Bearer sk_gnx_mock` (any `sk_gnx_` token works) and any `X-GCMS-Subject`. Proxied mode needs `GENAIX_MOCK_CMS_PUBLIC_KEY` (`mocks-genaix-mock-README.md`, "Identity: two caller modes").
- Only the GenAIx mock is needed. The CMS MCP mock (`mocks/cms-mcp-mock/`) is not, because the UI never calls the MCP and the GenAIx mock never contacts an MCP server.
- `mocks-genaix-mock-README.md` describes the rc.4 mock (plan routing, the `#draft:`, `#ask`, `#planmerge` and `#publishfail` markers). Before relying on it, check the version of the local checkout: `genaix_mock/config.py`, `API_VERSION`. An older checkout answers with rc.2 or rc.3 shapes.

## Reading large files
Never read `openapi.yaml` (~13,000 lines) or `mcp-tools.json` (~9,600 lines) in full. Extract what you need:
- OpenAPI schema: `grep -n '^    <SchemaName>:$' .claude/contracts/openapi.yaml`, then read from that line with an offset
- MCP tool: `jq '.groups[].tools[] | select(.name=="search_content")' .claude/contracts/mcp-tools.json`
- All tool names: `jq -r '.groups[].tools[] | "\(.group)\t\(.name)"' .claude/contracts/mcp-tools.json`

For UI work, read the docs in the order given in `INDEX.md` under "Reading path for the Workspace UI".
