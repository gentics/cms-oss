# GenAIx Integration Kit

> Contracts, mocks and a reference client for integrating GenAIx into the Gentics CMP9 Agentic Workspace: one standardized GenAIx API, the CMS MCP tool specification, the Content RAG contract, construct and devtools requirements, and workflow modules with quality gates.

The machine-readable artifacts are normative: where a prose document and a contract file disagree, the contract file wins. Read the architecture document first, then the API narrative for a Workspace UI role or the MCP and integration documents for a CMS role. This package is a specification with runnable mocks, not the finished integration.

Contract version: 1.0.0-rc.2, shared by openapi.yaml and mcp/tools.json.

<!-- Source: llm-index.md of the hand-out, with links rewritten to local paths. The documents
     are llm-full.md split at its "# File:" separators, content unchanged. -->

## Reading path for the Workspace UI

1. [01-architecture.md](01-architecture.md): read first, regardless of role (README, "Reading order")
2. [03-genaix-api.md](03-genaix-api.md): the narrative for `../openapi.yaml`
3. [08-workflow-modules.md](08-workflow-modules.md): workflow view, quality gates
4. [examples-session-lifecycle.md](examples-session-lifecycle.md): the call sequence a client actually makes
5. [09-integration-guide.md](09-integration-guide.md): sections 3 to 6 only (its "Purpose and audience" section)

## Documents

- [GenAIx Phase One contracts](README.md): What this package contains, the reading order, the machine-readable artifacts and the contract versions.
- [Architecture](01-architecture.md): The architecture of the GenAIx/CMP9 integration: what GenAIx does, how it connects to the CMS, and the contracts the Workspace UI and the CMS team build against.
- [Auth and context flow](02-auth-and-context-flow.md): Identity, authentication and context propagation in full: actors, headers, sequence diagrams, failure modes, the external-agent path.
- [The GenAIx API](03-genaix-api.md): Narrative guide to `openapi.yaml`: resources, session lifecycle, the event model, the message-part registry, the workflow view, errors, and scenario walkthroughs.
- [CMS MCP layer scope (cms-mcp)](04-mcp-scope.md): The CMS MCP server: tool groups, per-tool purpose/input/output/REST mapping/role, scenario coverage, what is explicitly excluded.
- [Content RAG contract](05-content-rag-contract.md): The `search_content` tool family, the search endpoint it wraps, and example questions through to answers.
- [Construct and devtools API requirements](06-construct-devtools-requirements.md): Construct (tag type) creation through the CMS REST API: the REST call sequence, validation and similarity checks, what remains filesystem-only.
- [Workflow modules and quality gates](08-workflow-modules.md): Workflows as versioned modules with quality gates: module layout, the manifest and verdict schemas, the four gate kinds, the gate catalogue per scenario, security boundaries.
- [Integration guide](09-integration-guide.md): How the API contract and the MCP specification are wired together: credential provisioning, the streaming proxy's requirements, header forwarding, timeouts.
- [CMS MCP tool specification](mcp-README.md): How to read mcp/tools.json: the tool specification's structure, its groups, roles and REST mappings.
- [GenAIx API v1 mock](mocks-genaix-mock-README.md): The GenAIx API mock: how to run it, its headers, the scenario transcripts it replays, its configuration and its limits.
- [cms-mcp-mock](mocks-cms-mcp-mock-README.md): The CMS MCP server mock: how to run and connect to it, its fixture tokens and roles, and what is mocked rather than real.
- [GenAIx API v1 reference client](client-README.md): The reference client: what it proves about the contract, its module layout and its known limitations.
- [The hand-out as a docker compose stack](deploy-README.md): The docker compose stack: how to serve this whole package on one port, the URL map, the fixture credentials and how to rebuild it.
- [Session lifecycle](examples-session-lifecycle.md): The same ground as the API narrative, ordered as the call sequence a client actually makes.
- [The hand-out on OpenShift](deploy-openshift-README.md): The same stack as deploy/docker-compose.yml, on a cluster, as one pod with three containers behind one route.

## Contracts

- [GenAIx API](../openapi.yaml): OpenAPI 3.1 description of every route, schema, example and SSE event of the GenAIx API. Normative.
- [CMS MCP tool specification](../mcp-tools.json): Every CMS MCP tool with its input and output schema, required roles and REST mapping. Normative.

## Paths used inside the documents

The documents cite files by their hand-out path. In this folder they map as follows.

| Cited as | Here |
|---|---|
| `openapi.yaml` | `../openapi.yaml` |
| `mcp/tools.json` | `../mcp-tools.json` |
| `NN-*.md`, `mcp/README.md`, `examples/session-lifecycle.md`, … | the same name with `/` replaced by `-`, in this folder |
| `schemas/events.schema.json` | not included; the corresponding schemas are `Event` and `*Event` under `components.schemas` in `../openapi.yaml` |
| `schemas/parts.schema.json` | not included; see `MessagePart` and `*Part` in `../openapi.yaml` |
| `schemas/user-parts.schema.json` | not included; see `UserMessagePart` and `User*Part` in `../openapi.yaml` |
| `schemas/problem.schema.json` | not included; see `Problem` in `../openapi.yaml` |
| `schemas/workflow-manifest.schema.json`, `schemas/gate-verdict.schema.json` | not included (for workflow-module authors); the API-side shapes are `Workflow`, `QualityGate`, `GateResult`, `WorkflowState` in `../openapi.yaml` |
| `examples/requests.http`, `examples/s1…s5-*.sse` | not included; served by the docker stack at `/examples/` (see `deploy-README.md`) |
| mock and client source (`mocks/`, `client/`) | not included |
