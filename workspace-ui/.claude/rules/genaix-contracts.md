# GenAIx and CMS contracts (version 1.0.0-rc.2)

## Where things are
- `.claude/contracts/openapi.yaml`: the GenAIx API. The source of truth for every route, request, SSE event and message part the UI handles.
- `.claude/contracts/mcp-tools.json`: the CMS MCP tool specification. Background only; see below.
- `.claude/contracts/genaix-docs/INDEX.md`: map of the prose documents. Start here.

## Rules
- Where a prose document and a contract file disagree, the contract file wins.
- The UI never calls GenAIx directly: every call goes through the CMS proxy (`01-architecture.md`, "System context"). The UI never calls the CMS MCP; GenAIx does.
- From `mcp-tools.json` the UI needs only:
  - the `inputSchema` of `search_content` and `count_content`: the keys of `UserFilterPart.criteria` must use these field names, camelCase as spelled there (`openapi.yaml`, `UserFilterPart`)
  - tool names and groups, if `tool.started` gets custom labels
- Tools in the `design` group are `phase: "future"`; don't build UI for them.
- Don't invent fields. If a shape is not in `openapi.yaml`, say so and ask.

## Local mock
`INDEX.md` says the mock source is not included here. It is in the hand-out repo instead:
- `git@git.gentics.com:psc/genaix/api-contract.git`, checked out next to `cmp`, so `../../../api-contract` from this app. The GenAIx mock is `mocks/genaix-mock/`, its transcripts are `examples/*.sse` and `mocks/genaix-mock/scripts/*.sse`.
- Start it with `./run.sh` in `mocks/genaix-mock/` (Python 3.11+, creates `.venv` on first run). It serves `http://localhost:8080/api/v1`.
- Fixture credentials only: `Authorization: Bearer sk_gnx_mock` (any `sk_gnx_` token works) and any `X-GCMS-Subject`.
- Only the GenAIx mock is needed. The CMS MCP mock (`mocks/cms-mcp-mock/`) is not, because the UI never calls the MCP and the GenAIx mock never contacts an MCP server.
- The mock implements `1.0.0-rc.2` (`genaix_mock/config.py`, `API_VERSION`), although its README still says `1.0.0-draft.3`.

## Reading large files
Never read `openapi.yaml` (~10,800 lines) or `mcp-tools.json` (~9,500 lines) in full. Extract what you need:
- OpenAPI schema: `grep -n '^    <SchemaName>:$' .claude/contracts/openapi.yaml`, then read from that line with an offset
- MCP tool: `jq '.groups[].tools[] | select(.name=="search_content")' .claude/contracts/mcp-tools.json`
- All tool names: `jq -r '.groups[].tools[] | "\(.group)\t\(.name)"' .claude/contracts/mcp-tools.json`

For UI work, read the docs in the order given in `INDEX.md` under "Reading path for the Workspace UI".
