# CMS MCP layer scope (cms-mcp)

Companion machine-readable specification: `mcp/tools.json` (version `1.0.0-rc.2`), reading guide in
`mcp/README.md`. This document describes the CMS MCP server: the tools it exposes, its transport,
authentication, error mapping and the requirements its implementation has to meet. How a particular client
stores credentials or drives a workflow is integration, covered in `09-integration-guide.md`.

## 1. Purpose and ownership

`cms-mcp` is the machine-facing door into Gentics CMS for agents: a Model Context Protocol server running
in-process inside the CMS as a Java module, calling the existing JAX-RS resource implementations directly
without an HTTP hop. Every MCP-capable agent connects to the same server with the same kind of credential and
sees the same tools, scoped to the permissions of the user it acts as.

Responsibilities are split inside the module. The CMS team provides the module package (the servlet mounting,
the transport, request authentication against CMS API tokens, the transaction and permission context), any
REST extension a tool needs, and the Elasticsearch mapping additions. ioflair implements the tools within that
package: the annotations on the resource methods that generate the one-to-one tools and the composite tools, and
specifies the tool surface in this document and `mcp/tools.json`.

Three rules constrain every tool. Every mutation goes through a CMS REST resource implementation, so no client
writes to the CMS database, filesystem or content repository by any other route. Anything resembling full-text
or analytical search goes through the Elasticsearch passthrough, which the CMS permission-filters server-side.
And there is no service account: every call carries the calling user's CMS API token, so permission checks,
locks and audit records see the real user.

The surface is a minimum set, with every tool tracing to one of the four showcase scenarios. Useful but
unneeded tools are extension points (§8) or carry `"phase": "future"` in `mcp/tools.json`.

## 2. Transport, endpoint and authentication

### 2.1 Endpoint and transport

| Aspect | Decision |
| --- | --- |
| Transport | MCP **streamable HTTP** (single endpoint, POST for requests, optional SSE upgrade for server-initiated messages) |
| Endpoint | `<cms_base_url>/mcp`, decided by the CMS team. The MCP server is its own servlet, not part of the Jersey REST servlet, so it is mounted on the servlet context and **not** under `/rest`, as an exact path mapping rather than a prefix. `MCP_PATH` keeps it configurable, so a client uses the endpoint its installation is configured with. See §2.6 |
| Protocol version | MCP `2025-06-18` or later; the server advertises its version in `initialize` |
| Session model | The SDK transport issues an `Mcp-Session-Id` and manages its own transport sessions, but no CMS authorization state and no conversational state is bound to that id: every request is independently authenticated from its bearer token, so a client may reconnect at will |
| Capabilities | `tools` with `listChanged` (already declared by the CMS, §2.6), `resources` (deferred, §9.4), `prompts` (none), `logging` (optional) |
| Content negotiation | `application/json` request, `application/json` or `text/event-stream` response |

Statelessness is a constraint, not a simplification: a client may run many concurrent sessions for one user and
reconnect mid-run, and binding CMS authorization state to an MCP transport session would reintroduce the
DB-backed-session problem the CMS has removed. The `sid` query parameter is gone from the REST API; a username
and password login returns a session cookie, which session-mode REST clients send on every request. `cms-mcp`
does not use session mode: a client authenticates with a bearer token on every request and holds no CMS
session.

### 2.2 Authentication

`Authorization: Bearer <CMS API token>` on every request, where the token is a named CMS API token belonging
to the calling **human** user. There is no service account and no installation-wide CMS credential.

The CMS exposes three token endpoints on `AdminResource`, each scoped to the currently authenticated user:

| Endpoint | Signature | Purpose |
| --- | --- | --- |
| `POST /rest/admin/token` | `createAPIToken(ApiTokenCreationRequest) -> ApiTokenCreationResponse` | Create a token for the current user, from `{name, expires, pruneOnExpiry}`. The secret is returned **once**. |
| `GET /rest/admin/token` | `listAPITokens(FilterParameterBean, SortParameterBean, PagingParameterBean) -> ApiTokenListResponse` | List the current user's tokens, with filter, sort and paging. |
| `DELETE /rest/admin/token/{id}` | `deleteAPIToken(int) -> GenericResponse` | Delete one of the current user's tokens. 404 when it is not theirs. |

A user, or a client acting with that user's CMS session, creates a named token and the client presents it on
every MCP request. `cms-mcp` sees only the bearer token: there is nothing to provision, look up or negotiate,
and no per-request user-token or session-id header is involved. A client confirms a token belongs to the
expected user with `whoami` (§4.1). Deleting the token makes the next MCP request answer 401.

The creation request is `{name, expires, pruneOnExpiry}`:

| Field | Meaning |
| --- | --- |
| `name` | The token's label, and the only marker a client has. There is no purpose field, so a client that wants to find its own token again encodes what it needs into the name. |
| `expires` | Unix timestamp in seconds at which the token stops working. `0` means never. A value in the past is rejected, and a token is valid only while `expires > now`, so expiry is evaluated on every call rather than at creation. |
| `pruneOnExpiry` | When `true`, the CMS deletes the token row once it expires instead of leaving a dead row behind. |

**Expect many short-lived tokens per user.** An agent host is expected to create one token per workflow
session rather than one per user or one per connection: name `genaix-<session id>`, `expires` a few hours
out, `pruneOnExpiry: true`. A credential then lives no longer than the piece of work it was created for, it
disappears from the CMS by itself, and revoking one session's access does not disturb the user's other
sessions. For `cms-mcp` this is a load and lifecycle statement rather than a protocol change: the same bearer
header, the same `whoami` check, but token creation happens often and the token table churns. §11 records
what that asks of the CMS.

**The endpoint must authenticate, and the MCP servlet has to do it itself.** The CMS session filter is
mapped to `/rest/*` only, and `/mcp` deliberately sits outside that chain, so no existing filter can
authenticate the endpoint and it is unauthenticated today. Four requirements, in order:

1. **Accept `Authorization: Bearer <CMS API token>`** in the MCP servlet and resolve it to a CMS user. The
   transport's `contextExtractor` (`McpTransportContextExtractor<HttpServletRequest>`) is the hook, and it
   can carry the resolved CMS session into the tool handlers. This is the servlet's own work precisely
   because `/mcp` is outside the `/rest/*` filter chain.
2. **Reject anything unresolvable with HTTP 401 before the MCP envelope.** No anonymous access to any tool,
   no fallback to a service identity, no read-only mode for unauthenticated callers.
3. **Validate the `Origin` header** with a `ServerTransportSecurityValidator`. The SDK default is a no-op and
   the CMS is browser facing.
4. **Run each tool call in a transaction owned by the resolved user**, so CMS permissions, locks and audit
   attribution apply unchanged (§6, §9.1).

Until the first requirement is met a bearer token is ignored rather than rejected, so a call that appears to
succeed proves the transport works, not that authentication does.

Two consequences a client relies on (`1.0.0-rc.2`). `initialize` is authenticated like every other request,
so an unauthenticated reachability probe learns only that the endpoint answers and sees nothing of the server;
a client must not expect `serverInfo` from a probe without a credential. And `whoami` is required of this
server: it is the call a client uses to verify a credential it holds, and a `tools/list` that succeeds proves a
credential only because the transport is authenticated.

The 401 sits **before the MCP envelope** deliberately: it lets a client distinguish a failed tool from a lost
credential without parsing a tool result. A token identifies the user rather than a permission snapshot, so a
group change takes effect on the next call without a new token, which is why `tools/list` can be recomputed
per request (§2.4).

#### 2.2.1 OAuth 2.1 as a later extension

Phase One supports bearer tokens; OAuth 2.1 is a later extension. The MCP Authorization specification models
one authorization path and it is not a static token: the MCP server is an OAuth 2.1 resource server, the
authorization server is a separate role, discovery runs through RFC 9728 protected resource metadata, and
RFC 8707 resource indicators bind a token to one server. The 2025-06-18 revision introduced that pattern; the
current 2026-07-28 revision adds RFC 9207 issuer validation, Client ID Metadata Documents as the preferred
registration route with dynamic client registration deprecated, dual discovery across RFC 8414 and OpenID
Connect Discovery, and a 403 step-up flow carrying `error="insufficient_scope"`.

To become a spec-compliant protected resource, `cms-mcp` would need to:

1. Serve `GET /.well-known/oauth-protected-resource` (RFC 9728), whose `authorization_servers` array names at
   least one authorization server. The CMS need not be that server; it can name an existing identity provider.
2. Answer 401 with
   `WWW-Authenticate: Bearer resource_metadata="https://cms.example.com/.well-known/oauth-protected-resource"`,
   optionally with a `scope=` hint. Clients are required to parse that header, which is what makes a bare 401
   actionable.
3. Validate the token's audience against its own canonical resource URI on every request (RFC 8707 §2). A
   valid token from a trusted issuer is not enough: it must have been minted for this resource.
4. Never pass the received token through to an upstream API, obtaining its own credential instead. This is the
   confused-deputy mitigation and it is normative.
5. Answer 403 with `error="insufficient_scope"` plus scope and metadata pointer, so a client can step up.
6. Optionally accept Client ID Metadata Document URLs as `client_id`, or support RFC 7591 dynamic client
   registration, for onboarding without per-client pre-registration.

Requirement 3 is why a CMS API token is inadmissible in the strict model: it is not audience-bound. The
specification never names static tokens or API keys as an alternative scheme.

The MCP Java SDK ships no built-in authorization by design, exposing pluggable hooks at the server transport
layer and leaving the mechanism to the host framework; first-party OAuth and API-key support lives in Spring
AI MCP Security, which does not fit a Jetty and Jersey application. Resource-server support in the CMS is
therefore custom servlet or transport work.

Authorization is a transport concern: **no tool in `mcp/tools.json` changes** when the CMS moves from a static
bearer token to OAuth 2.1.

### 2.3 Correlation headers

A client may send these so that its own logs and the CMS log can be joined after the fact. They are
**optional**, and a server must not reject a request that omits them:

| Header | Meaning |
| --- | --- |
| `X-GenAIx-Session` | Identifier of the calling client's conversation or task |
| `X-GenAIx-Run` | Identifier of the individual agent run within it, the unit a user can cancel |
| `X-GenAIx-Gate` | Identifier of the quality gate or reviewer agent making the call on the user's behalf, next to or instead of `X-GenAIx-Run` (`1.0.0-rc.2`) |
| `X-GenAIx-Client` | Client identifier and version, for example `somehost/1.4.2` |
| `User-Agent` | Free text, where a client has nothing more structured to send |

`cms-mcp` treats all five as **opaque correlation strings**: logged verbatim, never parsed, and never a source
of authorisation. The names are fixed by this contract so that one log format works for every client; a client
with no notion of sessions or runs simply omits them. §6 defines the log line they land in.

### 2.4 Dynamic tool listing

`tools/list` is computed **per request, per user, per tool**: the server resolves the bearer token to a CMS
user, reads that user's effective permissions from their groups and roles, and omits every tool whose declared
permission requirements (§10) those permissions can never satisfy. The unit is the tool, not the group
(`1.0.0-rc.2`): a group with mixed access lists only what the user may call, and a group disappears as a whole
only when none of its tools can be used.

| Condition | Effect on `tools/list` |
| --- | --- |
| User lacks `updateconstructs` on the construct type | the write tools of `constructs` omitted; its read tools (`list_constructs`, `get_construct`, `list_construct_categories`, `list_part_types`, `list_datasources`, `get_datasource`) stay, since they need construct `view` only, which a Content Creator holds |
| User lacks `createuser`/`updategroupuser` admin permissions | whole `admin` group omitted |
| User has no node with page create or edit permission | `content_write` group omitted |
| CMS feature `elasticsearch` inactive | whole `search` group omitted (§2.5) |
| Anything else | tool listed |

A client that builds a read-only allowlist for a quality gate or a reviewer agent may name the read tools of
`constructs` by their `access` and never its writes; the group's `roles` in `tools.json` are a summary of its
tools' roles, not a filter.

The CMS already declares `tools` with `listChanged`, which helps twice: the server can push a notification
when the set of tools changes, so a client need not re-poll to notice a permission change, and tools can be
registered at runtime without a protocol change. One caution: `listChanged` is a notification mechanism, not a
filter. A single `McpSyncServer` holds one global tool registry, so per-user filtering has to happen in the
list handler using the identity the `contextExtractor` resolved; registering tools globally and relying on
`listChanged` alone would show every user the same list. If that filtering is not ready, a static full list
plus correct `permission-denied` errors is an acceptable fallback (§9.4).

Omission is coarse by design: a *type-level* filter, not an instance-level one. A Content Creator who can edit
pages in node 3 but not node 7 still sees `update_page_tags`, and the per-object check happens inside the CMS
on the actual call. A model behaves better when an absent capability is simply not in its toolbox, but it must
not be misled into thinking a listed tool succeeds everywhere, which is why a client should call
`get_permissions` before proposing a write. Clients must also tolerate the list changing between calls, so
re-read it at the start of a conversation and after recovering from a 401.

### 2.5 Error mapping

CMS REST responses carry a `ResponseInfo.responseCode` enum alongside the HTTP status (enum values verified
in the CMS OpenAPI document). `cms-mcp` maps both onto one MCP tool result with `isError: true` and a
`structuredContent.problem` object shaped like RFC 9457:

```json
{
  "problem": {
    "type": "https://cms.gentics.com/problems/permission-denied",
    "title": "Permission denied",
    "status": 403,
    "detail": "You are not allowed to publish page 4711 in node 3.",
    "cms": {
      "responseCode": "PERMISSION",
      "messages": [ { "type": "CRITICAL", "fieldName": null, "message": "..." } ]
    },
    "tool": "publish_page",
    "retryable": false
  }
}
```

| CMS `responseCode` | HTTP | `problem.type` slug | `status` | Retryable | Notes |
| --- | --- | --- | --- | --- | --- |
| `AUTHREQUIRED` | 401 | *(no MCP envelope)* | 401 | no | Transport-level 401, so a client sees a connection failure rather than a tool error |
| `PERMISSION` | 403 | `permission-denied` | 403 | no | The user lacks the right, not the calling client |
| `NOTFOUND` | 404 | `not-found` | 404 | no | also used for wastebin-deleted objects |
| `INVALIDDATA` | 400 | `invalid-data` | 422 | no | `cms.messages[].fieldName` names the offending field |
| `LOCKED` | 409 | `object-locked` | 423 | yes, later | another user holds the edit lock; `detail` names them |
| `MAINTENANCEMODE` | 503 | `maintenance-mode` | 503 | yes | |
| `NOTLICENSED` | 403 | `feature-not-licensed` | 403 | no | e.g. a non-OSS module is absent |
| `FAILURE` | 500 | `cms-failure` | 500 | yes, once | unexpected server-side error |
| *(search only)* | 405 | `search-unavailable` | 503 | no | feature `elasticsearch` not activated; the search resource answers 405 by contract (`cms-search-api/.../SearchResource.java`, `@StatusCodes`). The 401 condition text on that same annotation, "No valid sid and session secret cookie were provided", is **pre-the CMS branch wording** and no longer describes the mechanism: there is no sid, and `cms-mcp` clients authenticate with a bearer token (§2.2) |
| *(transport)* | 429 | `rate-limited` | 429 | yes | reserved; no CMS rate limiter today |

Tool results never carry a stack trace. The text summary for an error is one sentence, in the user's
language where the CMS supplies an i18n message, because a client will usually surface it verbatim.

### 2.6 State of the CMS MCP servlet draft

A servlet draft of the MCP endpoint exists in the CMS today, mounted on the decided path `/mcp`. It is
functional at the protocol level, meaning `initialize`, capability negotiation and session handling all work,
but **no tool is registered yet** and the endpoint is not authenticated (§2.2).

| Aspect | State in the servlet draft |
| --- | --- |
| SDK | MCP Java SDK 2.0.1, as `mcp-core` plus `mcp-json-jackson2`. The `io.modelcontextprotocol.sdk:mcp` convenience bundle is avoided because it pulls Jackson 3 while the CMS uses Jackson 2, which would put two Jackson stacks into the shaded server jar |
| New transitive dependencies | `io.projectreactor:reactor-core` and `com.networknt:json-schema-validator`, both new to the CMS |
| Transport | `HttpServletStreamableServerTransportProvider`, which **is** an `HttpServlet`, so it mounts directly into the existing Jetty stack |
| Mounting | Registered with `ServletHolder.setAsyncSupported(true)`, which is mandatory because the transport calls `request.startAsync` for SSE streams |
| Path | `/mcp`, an **exact** mapping rather than a prefix: the transport checks `requestURI.endsWith(mcpEndpoint)` and answers 404 otherwise. Outside `/rest`, and therefore outside the CMS session filter |
| Protocol server | `McpSyncServer`, held by `com.gentics.contentnode.mcp.MCPServer` in **cms-core**, chosen over cms-oss-server because the REST resource implementations that tools will annotate live in cms-core |
| Server identity | `serverInfo` reports name `Gentics CMS` plus the CMS implementation version |
| Capabilities | `ServerCapabilities.builder.tools(true)`, which already announces `listChanged` |
| Tools | **None registered.** `MCPServer.getServer` returning `Optional<McpSyncServer>` is the registration hook |
| Servlet namespace | Jetty 12 ee10, `jakarta.servlet` (Servlet 6.0). The SDK targets Servlet 6.1 but its servlet transports use only 6.0 API, so this is settled (§9.3) |

Configuration, wired through `ConfigurationValue` so all three CMS mechanisms work:

| Setting | Env | System property | Config property | Default |
| --- | --- | --- | --- | --- |
| Enable the endpoint | `MCP_ENABLED` | `com.gentics.contentnode.mcp.enabled` | `mcp.enabled` | `true` |
| Endpoint path | `MCP_PATH` | `com.gentics.contentnode.mcp.path` | `mcp.path` | `/mcp` |

`MCP_PATH` is normalised to start with `/` and to carry no trailing `/`. The path is settled at `/mcp`, but it
stays configurable so an installation can move the endpoint, and **no client should hard-code it**: a client
uses the endpoint its installation is configured with rather than deriving one from a base URL. Written that
way, moving the endpoint costs one configuration value and no contract change.

Two further requirements on the implementation. Tool registration should be driven from the resource
annotations (§9.2) and cross-checked against `mcp/tools.json` so the generated `tools/list` and the
specification cannot drift. And the MCP server is currently built once at startup and does not react to a
configuration reload, so a changed path needs a restart.

## 3. Naming and result conventions

### 3.1 Naming

- Tool names are `snake_case`, verb-first for actions (`create_page`), noun-first for lookups (`get_page`).
- Tool names are not group-prefixed. The group is metadata used for allowlisting and the §2.4 permission
  filter, not a namespace.
- Field names are `camelCase` and **identical to the CMS REST model names**, in inputs and outputs alike and
  in nested objects too: `nodeId`, `folderId`, `pageId`, `templateId`, `constructId`, `datasourceId`,
  `typeId`, `niceUrl`, `editedAfter`, `groupBy`, `groupLimit`. There is no mapping table and no naming
  transformation in the module, because the schemas are generated from the annotated parameters and the
  Jackson models the REST layer already uses (§10). `cms.notes` therefore names the same fields the tool does.
- The GenAIx API on the other side of the agent keeps its own `snake_case` field names. Nothing crosses that
  boundary field by field, so the two conventions never meet: tool arguments travel as opaque JSON shaped by
  the tool schema.
- Identifiers are integers where the CMS uses integers; `globalId` is the optional string UUID. `"4711"` and
  `4711` are not interchangeable.

### 3.2 Result shape

Every tool returns both a `structuredContent` object matching its `outputSchema`, which is what the agent
reasons over and a client renders, and one `content[0]` text block of at most about 400 characters stating the
outcome: `"Created page 4711 'Acme GmbH terms of service update' in /News (node 3, de), offline."` Models ground
better on a sentence than on a blob.

Read tools that return collections use the envelope
`{ "total": 128, "items": [...], "nextFrom": 20, "truncated": false }`. `total` is the server-side match
count where the CMS provides one, otherwise the returned item count with `totalIsExact: false`; `nextFrom`
appears only when more results exist.

### 3.3 `ObjectRef`

Every reference to a CMS object, in every tool, in both directions, uses one shape:

```json
{ "type": "page", "id": 4711, "globalId": "A547.69f2...", "nodeId": 3,
  "name": "Acme GmbH terms of service update", "path": "/News/2026/", "language": "de" }
```

`type` is one of `node | folder | page | file | image | form | template | construct | tag | user | group`.
`type`, `id` and `name` are always populated on output, `nodeId` for everything below a node, and `path`,
`globalId`, `language`, `niceUrl` and `url` best-effort. `url` is the object's preview or published URL as the
CMS would open it, `niceUrl` its public path, `path` its folder path; a client that needs one link takes `url`,
then `niceUrl`, then `path` (`1.0.0-rc.2`). On input, tools accept `{type, id}` and ignore the rest, so an agent
can pass a ref back unchanged.

### 3.4 Idempotency, locking and write safety

- **Locking.** CMS page edits are lock-based: `GET /page/load/{id}?update=true` takes the lock,
  `POST /page/save/{id}` with `unlock: true` releases it. No tool exposes the lock as a separate step. Each
  write tool does load-lock-mutate-save-unlock **inside one tool call**, so an abandoned run cannot leave a
  page locked.
- **Idempotency.** Desired-state tools (`update_page_tags`, `update_page_properties`,
  `assign_construct_to_nodes`, `ensure_construct_category`, `take_offline`) are idempotent. `create_page`,
  `add_page_tag` and `create_construct` are not, so they take a guard: `create_page` passes
  `failOnDuplicate`, and `create_construct` refuses an existing keyword unless `allowExisting: true`, in
  which case it returns the existing construct with `created: false`.
- **Optional `idempotencyKey`.** Every write tool accepts one. Phase One behaviour: logged, otherwise
  ignored, so a de-duplication cache can be added later without a signature change.
- **No bulk writes.** One object per write call. The CMS has bulk endpoints but a single-object surface keeps
  the audit trail one line per object and stops a confused agent publishing forty pages at once.

### 3.5 Paging, sizes and limits

| Limit | Value | Rationale |
| --- | --- | --- |
| `size` on search tools | default 10, max 100 | ES `from`+`size` paging; the Editor UI caps at 25 |
| `size` on list tools | default 25, max 200 | |
| `depth` on `get_folder_tree` | default 2, max 5 | a 5-level tree of a real site already exceeds a sane context budget |
| `maxChars` on `render_preview` HTML | default 200000 | above that the tool returns `previewUrl` only and `truncated: true` |
| Base64 payload in `upload_file` | 5 MB hard | MCP has no binary channel; see §4.4 |
| Tag values in `update_page_tags` | 100 tags, 200 KB total | |

Exceeding a limit is a validation error (`invalid-data`), never a silent truncation, except where a
`truncated` flag is part of the output schema.

## 4. Tool groups

Roles are the showcase roles from the scenario definition: **CC** = Content Creator, **Chief CC** = Chief Content
Creator, **Admin** = CMP Administrator, **any** = any authenticated user. They label this document and the
a client's own UI; they are not CMS entities. The CMS enforces group and role permissions from the enum on
`GET /perm/{perm}/{type}/{id}`.

All paths are relative to the CMS REST prefix (conventionally `/rest`).

### 4.1 Group `identity`

Needed by every scenario, available to every user. These answer "who am I, where may I work, what may I do
here", and a client should call them before proposing anything.

| Tool | Purpose | Input (summary) | Output (summary) | CMS endpoint(s) | R/W | Roles |
| --- | --- | --- | --- | --- | --- | --- |
| `whoami` | Identify the acting user and their groups, so the agent can address the user and reason about scope | `{ includeGroups?: true }` | `{ user: {id, login, firstName, lastName, email}, groups: [{id, name}], cms: {version?, baseUrl?} }` | `GET /user/me?groups=true` | R | any |
| `get_permissions` | Check what the user may do on one object or one type **before** attempting a write | `{ type, id?, nodeId?, permissions?: [enum] }` | `{ ref?, type, granted: {view, edit, create, delete, publish, translate, updateconstructs, ...}, permBits?, roleBits? }` | `GET /perm/{type}/{id}` (bits + `privilegeMap`), `GET /perm/{perm}/{type}/{id}` (single check) | R | any |
| `list_nodes` | Enumerate the nodes (sites/channels) the user can see, with their default folders and languages | `{ q?, size?, from? }` | `{ total, items: [{ ref, host, publishDir, languages: [{id, code, name}], defaultFileFolderId, defaultImageFolderId }] }` | `GET /node`, `GET /node/{id}/languages` | R | any |

Notes. `GET /perm/{type}/{id}` returns `PermBitsResponse {perm, rolePerm, privilegeMap, permissionsMap}`,
where `perm` and `rolePerm` are raw bit strings no model should see. `cms-mcp` decodes them into the named
boolean map above from `privilegeMap.privileges` (a `{name: boolean}` map) plus `privilegeMap.languages` for
per-language publish rights. `type` accepts the CMS type names (`page`, `folder`, `template`, `construct`,
`node`, `group`, `user`, `admin`).

### 4.2 Group `search`

The Content RAG surface; full contract in `05-content-rag-contract.md`. The `search_content` input
and output below are exactly the scenario definition and must stay in sync with `mcp/tools.json`.

Ground truth: `POST /rest/elastic/{type}/_search` exists in the non-OSS `cms-search` module
(`cms/cms-search/cms-search-api/.../search/SearchResource.java`), which is why the OSS OpenAPI snapshot
lacks it, so it is easy to miss when reading the OSS specification alone. The CMS injects permission and scope
filters into `query.bool.filter` and post-filters every hit for view permission
(`cms-search-core/.../rest/resource/impl/search/SearchResourceImpl.java`, `addFilters` and the hit loop).
`cms-mcp` **must not** re-implement any of it.

It must, however, shape the body so the injection actually happens. Filter injection is **conditional on the
body already containing `query.bool`**, and `cms-mcp` is therefore required to wrap every outgoing query.
This is the single most important implementation rule in this group and §4.2.1 states it in full.

| Tool | Purpose | Input (summary) | Output (summary) | CMS endpoint(s) | R/W | Roles |
| --- | --- | --- | --- | --- | --- | --- |
| `search_content` | The one retrieval tool: find pages, folders, files, images and forms by natural-language or Lucene query, with three filters and highlight snippets. Finds candidates; it does not return content | `{ query, types?: [page…], nodeId?, folderId?, recursive?, languages?, filters?: {online, templateIds, editedAfter}, size?: 10 (max 50), from?, rawQuery? }` | `{ total, totalIsExact, tookMs, hits: [{ ref, score, snippets, language, online, edited, templateId, folderId }], queryUsed }` | `POST /rest/elastic/{type}/_search` with query params `nodeId`, `folderId` (multi), `recursive`, `language` (multi) | R | any |
| `find_similar` | Duplicate and near-duplicate detection: "is there already a page about this?" (S1) and "does a construct like this exist?" (S3) | `{ ref?, text?, types?, nodeId?, size?, minScore? }` | `{ total, hits: [{ ref, score, snippets }] }` | `POST /rest/elastic/{type}/_search` with a `more_like_this` query over `name`, `description`, `content` | R | any |
| `get_related` | Reverse lookup: what links to or uses this object, for "can I safely change/delete it" and "link related pages" | `{ ref, relations?: [links_to_page, links_to_file, links_to_image, uses_template, uses_tag, total], size?, from? }` | `{ total, groups: [{ relation, total, items: [ObjectRef] }] }` | `GET /page/usage/{linkedFile\|linkedImage\|linkedPage\|page\|tag\|template\|variant\|total}`, `GET /file/usage/*`, `GET /image/usage/*` | R | any |
| `count_content` | Counting, optionally grouped one way, without retrieving documents, for S2 reports ("how many pages are offline?") | `{ query?, types?, nodeId?, folderId?, recursive?, languages?, filters?, groupBy?: language \| template \| online, groupLimit? }` | `{ total, totalIsExact, groupBy?, groups?: [{ key, label?, count }], tookMs, queryUsed }` | `POST /rest/elastic/{type}/_search` with `size: 0`, and one `terms` aggregation only when `groupBy` is given | R | any |

#### 4.2.1 Mandatory query wrapping

`cms-mcp` **MUST** send every body to the search passthrough in this shape, and **MUST** rewrite a
caller-supplied `rawQuery` into it when the caller's body does not already carry `query.bool`:

```json
{ "query": { "bool": { "must": [ <the actual query> ], "filter": [] } }, "size": 10 }
```

The reason is a fail-open path in the CMS. `SearchResourceImpl.addFilters` returns the body **unmodified** on
three branches:

| Branch | Body that triggers it |
| --- | --- |
| `if (query == null) return body;` | No `query` key at all, for example a bare `{"size": 0, "aggs": {...}}` counting body, or a `query` that is not a JSON object |
| `if (bool == null) return body;` | `query` present without a `bool` key, for example `{"query": {"multi_match": {...}}}` |
| `if (filter == null) return body;` | `bool.filter` present but neither an object nor an array |

On any of those branches none of the four filters is applied: not the `terms` filter on the user's permitted
`groupId` values, not the `nodeId` term, not the `folderId` terms filter, not the wastebin `deleted` term.
Only the per-hit `Transaction.canView` post-filter still applies.

Two consequences. A counting query is the dangerous case, because `{"size": 0, "aggs": {...}}` has no `query`
key and returns nothing in `hits.hits` for the post-filter to drop; `count_content` therefore always sends
`{"query": {"bool": {"must": [{"match_all": {}}], "filter": []}}, "size": 0, "aggs": {...}}`. And totals and
aggregation counts are group-filtered but not object-filtered, because the hit loop replaces `hits.hits`
without adjusting `hits.total` and does not touch `aggregations`, so treat every count as an upper bound.

Notes on the group.

- **`content` is an array of raw editor-entered strings, not rendered HTML.** The page branch of
  `cms/cms-search/cms-search-core/.../object/search/SearchIndex.java` collects every content-tag value whose
  part type is a `TextPartType`. A phrase is findable only if an editor typed it into a tag; template
  boilerplate and navigation are not searchable.
- **The language query parameter is `language`, not `languages`,** and is multi-valued; the wastebin parameter
  is `wastebin` with `exclude|include|only`, default `exclude`
  (`cms-oss/cms-restapi/.../resource/parameter/SearchParameterBean.java`).
- **Index types are exactly** `page | folder | file | image | form`
  (`cms-search-core/.../object/search/IndexType.java`), with `page` and `form` indexed per language.
- **Search finds candidates, it does not return content.** A hit carries a reference plus just enough metadata
  to choose between hits: language, online state, last edit, template and folder. The CMS `_object` attribute
  is deliberately **not** passed through, so an agent reads a page with `get_page` before quoting it. That
  keeps results small and makes the verification step explicit rather than optional.
- **Highlighting is always on**, three fragments of 200 characters over the indexed text and description
  fields. There is no switch, because a snippet is what makes a hit judgeable.
- **Only three filters are supported**: `online`, `templateIds` and `editedAfter`. Anything else, including
  sorting and creator or editor filters, goes through `rawQuery`. `count_content` groups one way at a time, by
  language, template or online state, and requests no aggregation at all when `groupBy` is omitted.
- `rawQuery` that cannot be wrapped must be rejected with an `invalid-data` problem, never forwarded. The
  tool returns `queryUsed` so a client can show what was actually asked; it is the Elasticsearch body the
  server sent, informative and **not** an input either tool accepts back.
- **`count_content` counts what `search_content` matches** (`1.0.0-rc.2`): the same indexed fields and the
  same object types, both being one `_search` over the same indices. A `count_content` call with a search's
  own inputs (`query`, `types`, `nodeId`, `folderId`, `recursive`, `languages`, `filters`) therefore reproduces
  that search's `total`, which is what a reproducibility gate replays.
- `get_related` does not use Elasticsearch: reverse-link data is relational and only the usage endpoints have
  it.

### 4.3 Group `content_read`

Reading the tree and individual objects. Retrieval finds candidates; these tools verify them, which is how
the agent avoids asserting something it only saw in a snippet.

| Tool | Purpose | Input (summary) | Output (summary) | CMS endpoint(s) | R/W | Roles |
| --- | --- | --- | --- | --- | --- | --- |
| `get_folder_tree` | Orient in the site structure and pick a target folder (S1 `derived_settings.folder`) | `{ nodeId, folderId?, depth?: 2, includeCounts? }` | `{ root: ObjectRef, tree: [{ ref, children: [...], pageCount?, hasMoreChildren? }] }` | `GET /folder/getFolders/{id}?tree=true&recursive=true&nodeId=`, `GET /folder/count/{id}` | R | any |
| `list_folder_items` | List what is actually in a folder, filtered by type and edit/publish state | `{ folderId, nodeId?, types?: [page,file,image,folder,form], recursive?, language?, filters?: {online, modified, editedSince, editedBefore, creatorId, editorId}, size?, from?, sort? }` | `{ total, items: [ObjectRef + {online, modified, edited, editor, templateId}] }` | `GET /folder/getItems/{folderId}` (with `type`, `recursive`, `online`, `modified`, `editedsince`, `editedbefore`, `creatorId`, `editorId`, `skipCount`, `maxItems`, `sortby`, `sortorder`) | R | any |
| `get_page` | Load one page: properties, tags with their values, optionally template, language variants and version list | `{ id, nodeId?, include?: [tags, template, folder, languageVariants, versions, translationStatus, constructs] }` | `{ page: {ref, fileName, description, niceUrl, templateId, folderId, language, online, modified, locked, lockedBy?, cdate, edate, pdate, tags?: {keyword: {constructId, constructKeyword?, type, properties: {part: value}}}, versions?, languageVariants? } }` | `GET /page/load/{id}` with `template`, `folder`, `langvars`, `versioninfo`, `translationstatus`, `construct`, `nodeId` | R | any |
| `get_page_tags` | Just the tags of a page, paged and searchable, when the full page object is too much | `{ id, q?, size?, from? }` | `{ total, items: [{ name, constructId, constructKeyword?, type, active, properties }] }` | `GET /page/getTags/{id}` | R | any |
| `list_templates` | List the templates a folder or node allows, so a client can offer a template choice before a page is created (S1 settings review, S3 proof page). Phase one since `1.0.0-rc.2`, moved here from the `design` group | `{ nodeId?, folderId?, q?, size?, from? }` | `{ total, items: [{ ref, description, markupLanguage, locked, templateTagCount }] }` | `GET /folder/getTemplates/{folderId}`, `GET /node/{nodeId}/templates`, `GET /template` | R | any |
| `get_template` | Learn which tags a template offers and which of them a page may fill — the precondition for building page content (S1) | `{ id, nodeId? }` | `{ template: {ref, description, markupLanguage, source?, templateTags: [{ name, constructId, constructKeyword, editableInPage, mandatory }] } }` | `GET /template/load/{id}`, `GET /template/{id}/tag` | R | any |
| `get_file` | Inspect a file: metadata, size, mime type, online state, URL | `{ id, nodeId? }` | `{ file: {ref, fileName, description, size, mimeType, online, niceUrl, url} }` | `GET /file/load/{id}` | R | any |
| `get_image` | Inspect an image, including dimensions, for picking a hero image (S1) | `{ id, nodeId? }` | `{ image: {ref, fileName, description, size, mimeType, width, height, online, url} }` | `GET /image/load/{id}` | R | any |
| `get_page_versions` | Version history with editor and timestamp, for "what changed", "who changed it", and to pick a version to restore (S2) | `{ id, nodeId?, size? }` | `{ ref, currentVersion?, publishedVersion?, versions: [{ number, timestamp, editor: {id, login}, published? }] }` | `GET /page/load/{id}?versioninfo=true` | R | any |
| `render_preview` | Render the page the way the CMS will publish it, or produce a URL a client can iframe (S1 preview, S3 construct preview) | `{ pageId, nodeId?, mode?: view\|edit, version?, maxChars? }` | `{ previewUrl, html?, truncated, tags?: [{name, constructKeyword}], renderedAt }` | `GET /page/render/{id}` (`edit`, `version`, `nodeId`, `links`), `GET /devtools/preview/page/{id}` for raw HTML | R | any |

Notes.

- `get_folder_tree` uses `GET /folder/getFolders/{id}` with `tree=true&recursive=true`. The CMS has no depth
  limit, so `cms-mcp` trims to `depth` and sets `hasMoreChildren` on the cut nodes.
- `list_templates` scoped with `folderId` returns exactly the set `create_page` accepts for that folder, which
  is what a settings review offers as template options; without a scope it lists the installation's templates
  the user may view. It never returns template source (`get_template` with `includeSource` does).
- `get_page` flattens `tags[].properties[]` into `{partKeyword: value}` typed by part type, keeping the raw
  CMS `Property` object under `propertiesRaw` only on request. A `Property` carries one populated field out
  of twenty depending on type, which models handle badly.
- `render_preview` in `mode: edit` renders with Aloha edit markers. Preview covers **pages only**.
- `get_publish_state` wraps `GET /publish/state[/{type}/{objId}]` returning
  `PublishLogDto {objId, type, state, user, date}`
  (`cms-oss/cms-restapi/.../rest/resource/PublishProtocolResource.java`). It carries `"phase": "future"`,
  because the indexed `published` and `online` fields already answer the S2 questions.

### 4.4 Group `content_write`

The only mutation path for content. Available to users with page create/edit/publish permission in the
target folder; the CMS decides per object.

| Tool | Purpose | Input (summary) | Output (summary) | CMS endpoint(s) | R/W | Roles |
| --- | --- | --- | --- | --- | --- | --- |
| `create_page` | Create an **offline draft** page in a folder from a template (S1 step "create unpublished draft") | `{ folderId, templateId, name, language, nodeId?, fileName?, description?, niceUrl?, failOnDuplicate?: true, idempotencyKey? }` | `{ page: {...as get_page...}, created: true }` | `POST /page/create` (`PageCreateRequest {folderId, templateId, pageName, language, nodeId, fileName, description, niceUrl, failOnDuplicate}`) | **W** | CC |
| `update_page_tags` | Fill or change the content blocks of a page in one call: load with lock, set the named parts, save, unlock | `{ pageId, nodeId?, tags: {tagName: {constructKeyword?, properties: {partKeyword: value}}}, createMissing?: true, deleteTags?: [name], createVersion?: true, idempotencyKey? }` | `{ page, changedTags: [name], createdTags: [name], deletedTags: [name] }` | `GET /page/load/{id}?update=true` then `POST /page/save/{id}` (`PageSaveRequest {page, unlock: true, createVersion, delete}`) | **W** | CC |
| `add_page_tag` | Add one new content block of a given construct to a page, returning its generated tag name | `{ pageId, constructKeyword?, constructId?, tagName?, copyFrom?: {pageId, tagName}, idempotencyKey? }` | `{ tag: {name, constructId, constructKeyword, properties}, pageRef }` | `POST /page/newtag/{id}?constructId=\|keyword=` (`ContentTagCreateRequest`), `POST /page/newtags/{id}` for several | **W** | CC |
| `update_page_properties` | Change page metadata without touching content: name, filename, description, nice URL, priority, template | `{ pageId, nodeId?, name?, fileName?, description?, niceUrl?, priority?, templateId?, deriveFileName?, idempotencyKey? }` | `{ page }` | `GET /page/load/{id}?update=true` then `POST /page/save/{id}` | **W** | CC |
| `publish_page` | Publish now or at a time, and report whether the CMS published directly or queued the page for approval | `{ pageId, nodeId?, at?, allLanguages?, message?, keepVersion?, idempotencyKey? }` | `{ ref, result: published\|queued_for_approval\|scheduled, publishAt?, approval: {required, queuedSince?, message?, groups?: [ObjectRef]}, online }` | `POST /page/publish/{id}` (`PagePublishRequest {message, alllang, at, keepVersion}`); approval state read back via `GET /page/load/{id}?workflow=true` and `GET /page/pubqueue` | **W** | CC |
| `take_offline` | Unpublish a page now or at a time | `{ pageId, at?, allLanguages?, idempotencyKey? }` | `{ ref, online: false, offlineAt? }` | `POST /page/takeOffline/{id}` (`PageOfflineRequest {alllang, at}`) | **W** | CC |
| `delete_page` | Move a page to the wastebin (recoverable), never a hard delete | `{ pageId, nodeId? }` | `{ ref, deleted: true, restorable: true }` | `POST /page/delete/{id}` | **W** | CC |
| `translate_page` | Create or open the language variant of a page, as the starting point for a translation | `{ pageId, language, nodeId?, locked?: true }` | `{ page, sourceRef, created: bool }` | `POST /page/translate/{id}?language=&locked=&channelId=` | **W** | CC |
| `restore_page_version` | Undo: restore a previous version of a page (S2 "revert that change") | `{ pageId, version, nodeId? }` | `{ page, restoredVersion }` | `POST /page/restore/{id}?version=` | **W** | CC |
| `upload_file` | Put a binary into the CMS so a page can reference it: hero image for S1 | `{ folderId, nodeId?, name, mediaType, description?, source: {kind: "base64", data} \| {kind: "url", url, expiresAt?}, overwrite?: false, idempotencyKey? }` | `{ file: {ref, fileName, mimeType, size, url}, created: bool }` | `POST /file/createSimple?folderId=&nodeId=&description=&overwrite=` (multipart), or `POST /file/create` | **W** | CC |

Notes.

- `update_page_tags` collapses the load-mutate-save cycle into one tool. The CMS has no patch-one-tag
  endpoint: changing a tag value means loading the whole `Page`, mutating
  `page.tags[name].properties[part]` and POSTing the whole object back, so `cms-mcp` does the round-trip
  internally and the agent names only the tags and parts it wants changed.
- **There is no review-request tool** (`1.0.0-rc.2`). `publish_page` is the one call on the publishing path:
  when the acting user lacks `publishpages` on the folder the CMS queues the page for approval and the tool
  reports `queued_for_approval`; for a user who may publish directly there is nothing in the CMS that enqueues a
  page, so a "request review" by such a user is a client-side workflow step and no CMS call. Approving a queued
  page stays a human action in the CMS (§7).
- **Verbatim guarantee (S1).** `update_page_tags` writes exactly the string it is given: no whitespace
  normalisation, entity re-encoding or markup reflow. A hard implementation requirement, verifiable by
  reading the page back with `get_page`.
- `publish_page` reads the resulting state back and reports the branch that happened, because the CMS decides
  between direct publish and the approval workflow and the agent cannot predict it.
- `upload_file`: MCP carries no binary channel, so `source.kind: "base64"` is capped at **5 MB** and
  `source.kind: "url"` has `cms-mcp` fetch the file server-side, which needs a host allowlist, HTTPS only, a
  25 MB cap and no redirects (§11).
- `delete_page` maps to `POST /page/delete/{id}`, which moves the page to the wastebin. The hard-delete route
  is not exposed.

### 4.5 Group `constructs`

Scenario S3; the write tools are restricted to users with `updateconstructs` — the Chief Content Creator in
the showcase — while the read tools (catalogue, part types, categories, datasources) are listed for any user
with construct `view`, so a content creation flow can check which constructs a template and node allow
(`1.0.0-rc.2`, §2.4). Full rationale and call sequence in `06-construct-devtools-requirements.md`.

| Tool | Purpose | Input (summary) | Output (summary) | CMS endpoint(s) | R/W | Roles |
| --- | --- | --- | --- | --- | --- | --- |
| `list_constructs` | Find existing constructs before creating a new one: keyword collision check and similarity shortlist | `{ nodeId?, pageId?, categoryId?, partTypeIds?, q?, size?, from?, changeable? }` | `{ total, items: [{ ref, keyword, nameI18n, descriptionI18n?, category?, partKeywords: [string], mayBeSubtag, mayContainSubtags }] }` | `GET /construct` (`q`, `nodeId`, `pageId`, `category`, `partTypeId[]`, `page`, `pageSize`, `embed=category`) | R | Chief CC, CC |
| `get_construct` | Read a construct in full, including every part and the Handlebars template source — the mandatory pre-step to any update | `{ id?, keyword?, embedCategory? }` | `{ construct: {ref, keyword, nameI18n, descriptionI18n, categoryId?, mayBeSubtag, mayContainSubtags, autoEnable, editorControlStyle, template?: {partKeyword, source}, parts: [{keyword, nameI18n, typeId, type, editable, liveEditable, mandatory, hidden, partOrder, defaultValue?}] }, nodeRefs: [ObjectRef] }` | `GET /construct/{id}` or `GET /construct/load/{constructId}`, `GET /construct/{id}/nodes` | R | Chief CC, CC |
| `create_construct` | Create a Tier 1 Handlebars construct with its editable parts and assign it to nodes, in one CMS call | `{ keyword, name: {de, en, ...}, description?, categoryId?, nodeIds (required, min 1), handlebarsTemplate, templatePartKeyword?: "handlebars", parts?: [{keyword, name, typeId, editable?, mandatory?, liveEditable?, partOrder?, defaultValue?}], mayBeSubtag?, mayContainSubtags?, autoEnable?, editorControlStyle?, allowExisting?: false, idempotencyKey? }` | `{ construct, created: true, nodeRefs, templatePart: {keyword, typeId: 43, reportedType: "RICHTEXT"} }` | `POST /construct?nodeId=<id>[&nodeId=…]` | **W** | Chief CC |
| `update_construct` | Change a construct: full part-list replacement done safely, by reading first and merging inside the tool | `{ id?, keyword?, name?, description?, categoryId?, handlebarsTemplate?, parts?: [...full list...], nodeIds?, mayBeSubtag?, mayContainSubtags?, autoEnable?, editorControlStyle?, idempotencyKey? }` | `{ construct, changedFields: [string], replacedParts: bool, nodeRefs }` | `GET /construct/{id}` then `PUT /construct/{id}?nodeId=…` | **W** | Chief CC |
| `ensure_construct_category` | Get a category id by name, creating the category if it does not exist (the CMS create path takes ids only) | `{ name: {de, en, ...}, sortOrder? }` | `{ category: {id, nameI18n, sortOrder}, created: bool }` | `GET /construct/category` then `POST /construct/category` when absent | **W** | Chief CC |
| `list_construct_categories` | List categories so the agent can choose an existing one | `{ q?, size?, from? }` | `{ total, items: [{id, nameI18n, sortOrder}] }` | `GET /construct/category` | R | Chief CC, CC |
| `assign_construct_to_nodes` | Set the nodes a construct is available in (desired-state; also used to unassign) | `{ constructIds: [int], nodeIds: [int], mode?: "set" \| "add" \| "remove" }` | `{ assignments: [{ constructRef, nodeRefs }] }` | `POST /construct/link/nodes`, `POST /construct/unlink/nodes` (`BulkLinkUpdateRequest {ids, targetIds}`), `GET /construct/{id}/nodes` to read back | **W** | Chief CC |
| `add_construct_to_package` | Put a construct into a devtools package so it is version-controllable and promotable | `{ packageName, constructId?, constructKeyword? }` | `{ packageName, constructRef, added: true }` | `PUT /devtools/packages/{name}/constructs/{construct}` | **W** | Chief CC |
| `list_packages` | List devtools packages, to choose a target for the above | `{ q?, size?, from? }` | `{ total, items: [{name, description?, counts: {constructs, templates, datasources, objectProperties}}] }` | `GET /devtools/packages` | R | Chief CC |
| `list_part_types` | Discover which part types this installation has, and which of them may be used in a generated construct. The mandatory discovery step before creating or updating a construct | `{ q?, allowedOnly?, size?, from? }` | `{ total, items: [{ id, name, description, javaClass, deprecated, allowedForGeneratedConstructs, requiresDatasource }] }` | `GET /parttype` | R | Chief CC, CC |
| `list_datasources` | Find the datasource a select part needs, since a select part draws its options from one | `{ q?, type?: STATIC\|SITEMINDER, size?, from? }` | `{ total, items: [{ id, globalId, name, type, entryCount? }] }` | `GET /datasource` | R | Chief CC, CC |
| `get_datasource` | Read one datasource with its entries, to check it holds the options the construct needs | `{ id, includeEntries?, size?, from? }` | `{ datasource: {id, globalId, name, type}, total, entries: [{id, key, value}], constructRefs }` | `GET /datasource/{id}`, `GET /datasource/{id}/entries`, `GET /datasource/{id}/constructs` | R | Chief CC, CC |

Notes.

- **Handlebars is the only template engine this surface offers.** There is no engine option: a construct
  created here renders through Handlebars. Velocity exists in the CMS as `typeId` 33 and is deliberately not
  reachable, so an agent cannot produce a construct in a second templating language that nobody expects.
- **The Handlebars part is `typeId` 43 and `cms-mcp` hard-codes it.** `create_construct` takes
  `handlebarsTemplate` as a plain string and builds the part itself:
  `{keyword: "handlebars", typeId: 43, editable: false, partOrder: 1, defaultProperty: {type: "RICHTEXT", stringValue: <template>}}`.
  REST reports the part's `Property.type` as `RICHTEXT`; there is no `HANDLEBARS` literal in the enum
  (`cms-oss/cms-restapi/src/main/java/com/gentics/contentnode/rest/model/Property.java`, `case 43`;
  `cms-oss/cms-core/.../object/parttype/handlebars/HandlebarsPartType.java` extends `TextPartType` and
  returns `RICHTEXT`). Velocity is `typeId` 33 and also exists. `get_construct` surfaces the template under
  a dedicated `template` field.
- `nodeIds` is **required** on `create_construct`: `ConstructResourceImpl.create` throws
  `MissingFieldException("nodeId")` without it, and the same call links the nodes, so create and assignment
  are one transaction.
- **`parts[].typeId` is a plain integer, validated against an installation-level allowlist rather than a
  fixed enum.** An agent calls `list_part_types` and uses an id reported with
  `allowedForGeneratedConstructs`, and the CMS rejects anything else with a validation problem. The
  allowlist default is the 15 Tier 1 ids 1, 2, 3, 4, 6, 8, 9, 10, 21, 25, 29, 30, 31, 38 and 39, covering
  text, text and HTML, checkbox, page, image, file and folder URLs and the two selects. Type id 43 is always
  rejected in `parts`, because the Handlebars template goes into `handlebarsTemplate`; a draft a client
  renders therefore never lists a type 43 entry among its parts either. Inside the template a part is addressed
  as `cms.tag.parts.<keyword>` (`{{cms.tag.parts.headline}}`), the one spelling the CMS Handlebars part type
  resolves; `tag.parts.<keyword>` and a bare `{{keyword}}` render nothing (`1.0.0-rc.2`). Overview (13),
  datasource (32), Velocity (33), breadcrumb (34), navigation (35) and the form types (41 and 42) are
  excluded by the default allowlist in Phase One, because they need settings this surface does not model or
  imply Tier 2 work. An installation may widen the allowlist, which is why the schema does not hard-code it.
- **"Exactly one of" is a tool-level rule, not a schema root.** `get_construct` and `update_construct` take
  `id` or `keyword`, `add_page_tag` and `add_construct_to_package` a construct id or keyword, `find_similar`
  a `ref` or a `text`. Until `1.0.0-rc.1` each expressed that as a root-level `anyOf` in `inputSchema`; MCP
  hosts that convert tool schemas for a model reject or silently drop such a root, and the tools never reached
  the model. Since `1.0.0-rc.2` the schema root is a plain object and the rule is the tool-level
  `inputRule: {exactlyOne: [...]}` next to it, enforced by the server with `invalid-data` when neither or both
  members are present. A tool schema must never carry `anyOf`, `oneOf` or `allOf` at its root.
- **Select parts need a datasource.** `typeId` 29 (single select) and 30 (multi select) require
  `datasourceId` on the part, and the field is rejected on any other type. The input schema states this as an
  `if`/`then` conditional as well as in prose. An agent finds a datasource with `list_datasources` and checks
  its options with `get_datasource`; creating a datasource is out of Phase One (§7).
- `update_construct` replaces the whole `parts[]` array (`Construct.REST2NODE` clears and rebuilds, matching
  by `globalId`), so the tool reads first and merges. `nodeIds` on update is a desired-state list the CMS
  diffs; omitting it leaves assignments untouched.
- **Handlebars validation is not a CMS tool.** No endpoint compiles a template:
  `POST /validate/tagPart/{partId}` validates one field value against its regex, and the devtools package
  check validates structural completeness only. A client compiles the template itself, then verifies by
  inserting the construct into a scratch page and calling `render_preview`.
- `add_construct_to_package` and `list_packages` are the only devtools tools. Filesystem sync, `files` and
  `files-internal`, and the consistency check are out of scope (§7).
- `request_tier2_field_mapping` carries `"phase": "future"`: it would create a tagmap entry and explicitly not
  trigger a content-repository repair.

### 4.6 Group `admin`

Scenario S4, which the scenario definition marks **optional** and not an acceptance criterion unless confirmed.
Restricted to users holding `createuser`, `updategroupuser` and `userassignment`.

| Tool | Purpose | Input (summary) | Output (summary) | CMS endpoint(s) | R/W | Roles |
| --- | --- | --- | --- | --- | --- | --- |
| `list_users` | Find a user by name or login, to check whether they already exist | `{ q?, size?, from?, includeGroups? }` | `{ total, items: [{ ref, login, firstName, lastName, email, groups?: [ObjectRef] }] }` | `GET /user` (`q`, `page`, `pageSize`, `embed`) | R | Admin |
| `list_groups` | List groups, to pick the one that carries the intended role | `{ q?, size?, from? }` | `{ total, items: [{ ref, description? }] }` | `GET /group` | R | Admin |
| `get_group_permissions` | Read what a group may do, type-level or on one object — the input to the impact preview | `{ groupId, type, instanceId? }` | `{ groupRef, type, instanceRef?, permissions: [{ name, label, category, granted, editable }], roles?: [{id, name, granted}] }` | `GET /group/{id}/perms/{type}`, `GET /group/{id}/perms/{type}/{instanceId}` (`TypePermissionResponse {perms: [TypePermissionItem], roles}`) | R | Admin |
| `create_user` | Create a user and place them in a group in one call | `{ groupId, login, email, firstName, lastName, password?, description?, idempotencyKey? }` | `{ user: {ref, login, email, groups: [ObjectRef]}, created: true }` | `PUT /group/{id}/users` with a `User` body (`GroupResource.createUser`) | **W** | Admin |
| `assign_user_to_group` | Grant a role by adding the user to a group, optionally scoped to nodes | `{ userId, groupId, nodeIds? }` | `{ userRef, groupRef, groups: [ObjectRef] }` | `PUT /user/{id}/groups/{groupId}`, `PUT /user/{id}/groups/{groupId}/nodes/{nodeId}` for the node scope | **W** | Admin |
| `remove_user_from_group` | Revoke a role | `{ userId, groupId }` | `{ userRef, groupRef, groups: [ObjectRef] }` | `DELETE /user/{id}/groups/{groupId}` | **W** | Admin |
| `preview_permission_impact` | Show, before any change, what a user will and will not be able to do after a group change | `{ userId, addGroupIds?, removeGroupIds?, types?: [page, folder, template, construct, admin], nodeIds? }` | `{ userRef, before: [{ scope, permission, granted }], after: [...], added: [...], removed: [...], computedBy: "composition", warnings: [string] }` | **none directly.** Composed read-only from `GET /user/{id}/groups`, `GET /group/{id}/perms/{type}[/{instanceId}]` and `GET /perm/{type}/{id}` | R | Admin |

Notes.

- **User creation has no standalone endpoint.** There is no `POST /user`. The only create path is
  `PUT /group/{id}/users` with a `User` body, which creates the user and adds them to that group in one call
  (`cms-oss/cms-restapi/.../rest/resource/GroupResource.java`, `createUser`). `PUT /user/{id}` updates and
  `DELETE /user/{id}` deactivates; neither is exposed. `create_user` therefore requires `groupId`.
- **`preview_permission_impact` needs no new CMS endpoint but is not a CMS feature either.** No simulation
  endpoint exists, so `cms-mcp` unions the group permissions the user would hold afterwards. That does not
  reproduce folder-tree inheritance or role-per-language subtleties, so the output carries `warnings` and
  `computedBy: "composition"` and a client must label it a preview (§11).
- No tool writes permissions. `POST /perm/{type}/{id}` and `POST /group/{id}/perms/{type}` exist and are
  deliberately not exposed: bit edits stay a human action in the CMS UI.

## 5. Scenario coverage matrix

Every tool traces to a scenario and every scenario is covered end to end. S1 content creation from source
material, S2 content research and reporting, S3 construct creation, S4 optional administration.

| Group | Tool | S1 | S2 | S3 | S4 |
| --- | --- | :-: | :-: | :-: | :-: |
| identity | `whoami` | x | x | x | x |
| identity | `get_permissions` | x | x | x | x |
| identity | `list_nodes` | x | x | x | x |
| search | `search_content` | x | x | x | |
| search | `find_similar` | x | x | x | |
| search | `get_related` | x | x | | |
| search | `count_content` | | x | | |
| content_read | `get_folder_tree` | x | x | | |
| content_read | `list_folder_items` | x | x | | |
| content_read | `get_page` | x | x | | |
| content_read | `get_page_tags` | x | x | | |
| content_read | `get_template` | x | | x | |
| content_read | `get_file` | x | x | | |
| content_read | `get_image` | x | | | |
| content_read | `get_page_versions` | | x | | |
| content_read | `render_preview` | x | | x | |
| content_write | `create_page` | x | | x | |
| content_write | `update_page_tags` | x | | x | |
| content_write | `add_page_tag` | x | | x | |
| content_write | `update_page_properties` | x | | | |
| content_write | `publish_page` | x | | | |
| content_write | `take_offline` | | x | | |
| content_write | `delete_page` | | x | | |
| content_write | `translate_page` | x | | | |
| content_write | `restore_page_version` | | x | | |
| content_write | `upload_file` | x | | | |
| constructs | `list_constructs` | x | | x | |
| constructs | `get_construct` | x | | x | |
| constructs | `create_construct` | | | x | |
| constructs | `update_construct` | | | x | |
| constructs | `ensure_construct_category` | | | x | |
| constructs | `list_construct_categories` | | | x | |
| constructs | `assign_construct_to_nodes` | | | x | |
| constructs | `add_construct_to_package` | | | x | |
| constructs | `list_packages` | | | x | |
| constructs | `list_part_types` | | | x | |
| constructs | `list_datasources` | | | x | |
| constructs | `get_datasource` | | | x | |
| admin | `list_users` | | | | x |
| admin | `list_groups` | | | | x |
| admin | `get_group_permissions` | | | | x |
| admin | `create_user` | | | | x |
| admin | `assign_user_to_group` | | | | x |
| admin | `remove_user_from_group` | | | | x |
| admin | `preview_permission_impact` | | | | x |

Read per column to check sufficiency. **S1** identifies the user and their nodes, avoids duplicates, derives
folder, template and language, restricts itself to constructs allowed in the target node, creates an offline
draft, fills it, previews it and then publishes or requests review, with `publish_page` reporting which
branch happened. **S2** answers questions with `search_content`, counts with `count_content`, verifies every
claim with `get_page` before stating it, and can act on what it finds. **S3** similarity-checks existing
constructs, resolves a category, discovers which part types this installation allows and which datasource a
select part should use (`list_part_types`, `list_datasources`, `get_datasource`), creates the construct with
its template, parts and node assignment, proves it renders through a scratch page, and promotes it into a
package. **S4** checks the user does not exist,
finds the group carrying the role, previews the impact, creates the user in the group and verifies afterwards.

Two scenario steps have no CMS tool by design. The steps that are not CMS operations at all (showing a plan,
asking for feedback, checking editorial guidelines, proving verbatim text, compiling a Handlebars template)
belong to the client. And "request review" is the `queued_for_approval` branch of `publish_page` rather than a
separate tool, because the CMS decides the branch.

## 6. Impersonation and audit

`cms-mcp` has no service identity. Every tool call runs in a CMS transaction owned by the user resolved from
the bearer token, which buys four properties. Authorisation is the CMS's existing permission system,
unchanged, so no client can exceed what the user can do in the Editor UI. Search filtering is server-side: the
passthrough injects a `terms` filter on the user's permitted `groupId` values and post-filters every returned
hit with `canView` (`cms-search-core/.../SearchResourceImpl.java`), so no hit the user may not see is
returned, subject to the wrapping requirement in §4.2.1. Locking and versioning attribute correctly, so the
human appears as editor and a concurrent human editor gets a normal lock conflict. And revocation is instant:
the user deletes the token and the next call returns 401, so access ends at the CMS.

What `cms-mcp` must log, once per tool call:

| Field | Source |
| --- | --- |
| timestamp, duration | server |
| `cmsUserId`, `login` | resolved from the bearer token |
| `tokenName` | the named API token used |
| `tool`, `group`, `access` | tool metadata |
| object references touched | `{type, id, nodeId}` for every object read or written; writes mandatory, reads best-effort |
| outcome | `ok` or the `problem.type` slug and CMS `responseCode` |
| `session`, `run` | the `X-GenAIx-Session` and `X-GenAIx-Run` correlation headers, verbatim, nullable (§2.3) |
| `client` | `X-GenAIx-Client` or `User-Agent` |

The log must not contain tag values, page content, search queries or template source. It is an access log, not
a content log: enough to answer which agent run changed a page, on whose behalf, and whether it succeeded.
Writes additionally appear in the CMS action log (`GET /admin/actionlog`) automatically, because `cms-mcp`
calls the resource implementations rather than the database. A client keeps the other half of the trail, and
the two join on the correlation headers of §2.3, which is why those names are fixed by this contract.

## 7. Explicitly excluded from Phase One

Exclusions are as much a part of the contract as inclusions: they say what not to build, and they keep the
agent's toolbox small enough to behave.

| Excluded | Why |
| --- | --- |
| **Design Studio tools** (template sets, page decomposition, render diff) | Out of scope for Phase One. The shapes stay extensible (§8.1) but no tool ships. Editing a template through an agent also has a far larger blast radius than editing one page. |
| **devtools `files` and `files-internal`** | No REST surface exists: they are static directories served by Jetty, and `files-internal` is scoped to CMS-internal edit-mode assets such as custom tag editors. Construct JavaScript and CSS therefore belong to the portal build. Templates are Handlebars part values via REST and are available; JS and CSS are out of Phase One. |
| **devtools filesystem sync** (`cms2fs`, `fs2cms`, `/devtools/sync`) | Promotion between environments is a release process, not an agent action. An agent triggering `fs2cms` could overwrite CMS state from a filesystem it cannot see. `add_construct_to_package` is the safe half: it records membership, a human runs the sync. |
| **Tier 2 constructs and content-repository repair** | A structured-field construct is REST-creatable but not REST-live: it needs a tagmap entry, a non-instant CR structure repair, a republish and portal changes. `PUT /contentrepositories/{id}/structure/repair` is a long-running schema migration against a production repository and is never an agent action. |
| **Datasource creation and editing** | A select part needs a datasource, and `list_datasources` plus `get_datasource` let an agent find and inspect one, but `POST /datasource` and the entry routes are not exposed. A datasource is shared configuration that other constructs already depend on, so adding or changing options is a human decision; if no datasource fits, the construct waits. |
| **Scheduler** (`/scheduler/*`) | `TaskModel.command` is a shell command executed by the scheduler daemon. Exposing it to an LLM is remote code execution by another name, and no scenario needs it. |
| **Forms** (`/form/*`) | Forms are an index type and therefore findable through `search_content`, but no form read or write tool ships. No scenario needs form authoring, and forms are a second editing paradigm with their own publish cycle. |
| **Binary uploads above 5 MB inline** | MCP carries no binary channel, and base64 in a JSON-RPC payload costs 33 percent overhead inside the model's own transport budget. Larger files use `source.kind: "url"` with a server-side fetch (§4.4). |
| **Bulk and batch writes** | One object per write call, so the audit trail and the failure mode stay legible (§3.4). |
| **Permission bit writes** (`POST /perm/...`, `POST /group/{id}/perms/...`) | Role grants via group membership are reversible and legible; editing permission bits is neither (§4.6). |
| **Page and template migration** (`/migration/*`) | Bulk re-pointing of pages between constructs has site-wide effect and no undo. |
| **Hard delete, wastebin purge, node and folder creation** | Destructive or structural. `delete_page` moves to the wastebin and stops there. The agent picks an existing folder, which is what a content team expects anyway. |
| **Publishing another user's queued page** (`POST /page/pubqueue/approve`) | Approval is the human gate the showcase demonstrates, not automates. |
| **MCP prompts** | No server-side prompt templates. Prompting belongs to the client; a CMS-authored prompt would be a second, invisible place where agent behaviour is defined. |

## 8. Extension points

### 8.1 For Design Studio

Design Studio is template-level rather than page-level work, and the contract is shaped so it slots in:

- **New group `design`**, listed only for users holding `createtemplates`/`updatetemplates`. The group-level
  permission filter (§2.4) already supports that.
- **Template tools**: `create_template`
  (`POST /template`, `TemplateCreateRequest`), `update_template` (`POST /template/{id}`,
  `TemplateSaveRequest {template, syncPages, sync[], forceSync}`) and `link_template_to_folders`
  (`POST /template/link`). `get_template` and, since `1.0.0-rc.2`, `list_templates` already exist in
  `content_read`. `Template.source` carries the raw
  markup with `<node keyword>` references, so a decomposition tool reads and writes one string plus a
  `templateTags` map.
- **Decomposition** is a client-side planning problem whose outputs are existing tools: `create_construct`
  per detected block, `create_template` once, `link_template_to_folders`. The only new CMS need is template
  creation.
- **Render diff**: `POST /diff/html`, `POST /diff/source` and `GET /page/diff/versions/{id}` already exist. A
  `render_diff` tool wrapping them lets an agent prove a template change did not alter forty other pages,
  which is what makes template editing safe enough to automate. `GET /template/{id}/tagstatus` reports which
  pages fall out of sync and belongs in the same group.
- **Preview beyond pages**: Phase One previews pages only. `POST /page/preview` renders an unsaved `Page` and
  approximates a template preview against a scratch page.

### 8.2 For other LLM clients

Nothing in this contract is specific to one client:

- **Authentication** is a plain CMS API token, so a developer can register the server with any MCP client and
  get the same tools, scoped to their own permissions (`mcp/README.md` §4).
- **Correlation headers are optional** (§2.3). Omitting them yields a log line with null correlation ids, not
  a rejection.
- **Tool descriptions are self-contained.** Each states what the tool does, what it needs first, and when not
  to use it, because a client with no domain skills of its own has only the description.
- **No stateful handshake**, so a cron job, a CI step or a reconnecting chat session behave identically
  (§2.1).
- **Graceful degradation.** A client that cannot render `structuredContent` still gets a usable text summary
  (§3.2), and unknown fields are ignored. `cms-mcp` must add fields additively and never repurpose one.

What a client adds on top, whether a plan, editorial checks, verbatim proof or a workflow view, is its own
concern: the CMS exposes capability, a client adds method.

## 9. Gentics implementation notes for the `cms-mcp` module

This section is advisory: implementation is Gentics's call. It records the decisions already made with
the CMS team and the risks the contract depends on.

### 9.1 In-process resource implementation calls

`cms-mcp` instantiates and calls the existing `...ResourceImpl` classes directly, as the CMS test suite does,
rather than making HTTP calls to itself. Four consequences the tool design relies on:

- A tool needing two CMS operations runs them **in one transaction**. `update_page_tags` (load with lock,
  mutate, save, unlock) and `create_construct` (create, then link nodes inside one `Trx`) are correct only
  because of this.
- Permission checks, lock handling, i18n messages and action-log entries come from the resource
  implementations unchanged. `cms-mcp` adds only bearer-token-to-user resolution and `Trx` set-up with that
  user as the acting user.
- `ResponseInfo.responseCode` is a typed enum rather than an HTTP status, which is why §2.5 keys on it.
- Resources return `...Response` objects with `messages[]` and `responseInfo`, which must not be passed
  through raw: they carry `inBackground`, legacy list wrappers, nested `Property` variants and the
  `Legacy*ListResponse` versus `Paged*ListResponse` split.

### 9.2 Annotation-driven tool metadata

Tool metadata should come from the existing JavaDoc on the resource interfaces rather than a parallel
description language: an annotation on the exposed method, for example
`@McpTool(name = "publish_page", group = "content_write", access = WRITE, permissions = {...})`, plus
`@McpParam` where a parameter needs an LLM-facing description beyond the JavaDoc `@param`; a build-time or start-up step
that reads both to produce `tools/list`, so the schema cannot drift from the method signature; and a
cross-check against `mcp/tools.json`, which is the specification while the generated list is the
implementation. A test diffing tool names, `required` lists and `access` catches drift both ways.

Two cautions. Several tools are not one resource method: `update_page_tags` is two calls plus a merge,
`update_construct` a read-merge-write, `publish_page` reads workflow state back,
`ensure_construct_category` is lookup-or-create, and `preview_permission_impact` composes several reads.
Those need hand-written tool classes. And the input schemas in `mcp/tools.json` are deliberately narrower
than the CMS request models: strict `additionalProperties: false`, enums, maxima, no `perm`/`rolePerm` bit
strings, no raw `Property` unions. A generator that reflects the JAX-RS signature produces schemas that are
technically correct and practically unusable by a model. Where the two disagree, `tools.json` wins.

What the generator must **not** do is rename anything. Field names travel from the annotated parameters and
the Jackson models into the schema unchanged, so a tool field is spelled exactly like the REST field behind
it. That is why `tools.json` uses `camelCase` throughout (§3.1): the specification records what the scanner
will produce rather than a second vocabulary the module would have to translate into.

### 9.3 The servlet namespace

The CMS is Jetty 12 ee10 on `jakarta.servlet` (Servlet 6.0), and the servlet draft mounts the SDK's own
servlet transport into it successfully: the SDK targets Servlet 6.1 but its servlet transports use only 6.0
API. The REST layer has moved to `jakarta.ws.rs` as well, visible in
`cms-oss/cms-restapi/.../resource/parameter/SearchParameterBean.java` and
`cms/cms-search/cms-search-api/.../search/SearchResource.java`. No shading, relocation or separate process is
needed, so the single-transaction guarantee in §9.1 holds and `update_page_tags` and `update_construct` keep
their atomicity. What the SDK does add to the CMS is two new dependencies, `io.projectreactor:reactor-core`
and `com.networknt:json-schema-validator`.

### 9.4 Minimum and full surface

**Minimum surface** — 21 tools covering the three mandatory scenarios:

- **Authentication on the endpoint**, that is the four requirements in §2.2, of which the first two are the
  hard gate: nothing else may ship on an unauthenticated endpoint. A static `tools/list` is acceptable if
  per-user filtering is not ready, provided unauthorised calls fail cleanly with `permission-denied`.
- `identity`: all three tools. Every scenario starts with them.
- `search`: `search_content` and `count_content`.
- `content_read`: `get_folder_tree`, `list_folder_items`, `get_page`, `get_template`, `render_preview`.
- `content_write`: `create_page`, `update_page_tags`, `add_page_tag`, `publish_page` — the S1 path.
- `constructs`: `list_constructs`, `get_construct`, `list_part_types`, `create_construct`,
  `update_construct`, `ensure_construct_category`, `assign_construct_to_nodes` — the S3 path.
  `list_part_types` is in the minimum because without it an agent has to guess part type ids.
- Error mapping with the `problem` payload, and the §6 access log.

The remaining Phase One tools are conveniences whose data is reachable through the minimum set:
`find_similar` and `get_related` can be approximated with `search_content`, `get_page_tags`, `get_file`,
`get_image` and `get_page_versions` through `get_page` and `search_content`, `upload_file` is needed only for
a hero image, `list_datasources` and `get_datasource` only if the demo construct has a select part, and
`translate_page`, `update_page_properties`, `add_construct_to_package` and `list_packages` serve steps
outside the core story.

**Full surface** adds per-user dynamic `tools/list`, the `admin` group and S4, those conveniences, MCP
resources (`cms://page/{id}` and siblings) for clients that prefer resources to tools, the `idempotencyKey`
cache, `get_publish_state`, and the Design Studio group of §8.1.

Ordering principle: **read tools before write tools within a scenario, and S1 and S3 before S2 and S4.** S1
and S3 carry the write paths and therefore the risk to retire early; S2 is almost entirely `search_content`,
and S4 is optional.

## 10. Tool implementation model

One mechanism carries the whole surface: an annotation on a Java method, scanned at start-up.

```java
@McpTool(name = "publish_page", group = "content_write", access = WRITE,
         permissions = { @Perm(type = PAGE, value = "publish") })
```

No attribute names a field and none renames one. The DTO the method already takes is the input schema, so a
composite is written in the same style as the resource methods around it:

```java
public class UpdatePageTagsRequest {
    public int pageId;                  // -> "pageId" in the tool schema, as in the REST model
    public Integer nodeId;
    public Map<String, TagUpdate> tags; // keys are tag names; TagUpdate has constructKeyword, properties
    public boolean createMissing = true;
    public String idempotencyKey;
}
```

The annotation carries four attributes and nothing else. `permissions` states the CMS permission requirements
a caller must hold, as object type plus permission bits, in the CMS's existing permission vocabulary: the
`perm` resource types and the `PermHandler` privileges. It **never names a role or a group**, because roles
and groups are created dynamically in an installation and a hard-coded name would be wrong in the next one.

The scanner derives everything else from the annotated method:

| Derived | From |
| --- | --- |
| Tool name, group, access, permission requirements | the annotation itself |
| LLM-facing description | the method's JavaDoc, including `@param` text per field |
| Input schema | the parameter DTO, through the Jackson model already used for REST, so the field names are the REST field names |
| Output schema | the return type, the same way |
| The `tools/list` filter | `access` and the declared permission requirements, against the user's effective permissions (§2.4) |
| The audit line | name, group, access and the object references the call touched (§6) |

**Permissions are evaluated twice, and both points matter.** `tools/list` hides a tool when the acting user's
effective permissions, resolved from their groups and roles, can never satisfy its requirements: a capability
check, not an object check, so a user who may edit pages somewhere still sees the page write tools. Every call
then re-checks the same requirements against the concrete target object inside the transaction, and that is
the check which actually protects the object. A user who sees a tool may still be refused on a particular
page, folder or node, as a `permission-denied` problem.

The `roles` and `scenarios` values in `mcp/tools.json` are **planning metadata of this specification**, not
annotation input: `roles` are suggested role profiles a CMS administrator may map to groups, and `scenarios`
exist to prove the showcase scenarios are covered. The CMS generates neither.

### 10.1 Two kinds of tool, one mechanism

**Generated tools** carry the annotation on an existing REST resource method and are a one-to-one mapping. No
new code, no separate schema to maintain: the tool exists because the method does. 31 of the 53 tools are
this kind.

**Composite tools** are ordinary methods in the `cms-mcp` module carrying the same annotation, whose body
calls two or three resource implementations inside one transaction, with the user context taken from the
bearer token. They exist where a single REST call cannot express what an agent needs to do safely, and there
are 22 of them.

```java
@McpTool(name = "update_page_tags", group = "content_write", access = WRITE,
         permissions = { @Perm(type = PAGE, value = "edit") })
public Page updatePageTags(UpdatePageTagsRequest req) throws NodeException {
    try (Trx trx = ContentNodeHelper.trx()) {
        PageLoadResponse loaded = pages.load(req.pageId, true, ...);   // update=true locks the page
        Page page = loaded.getPage();
        applyTagChanges(page, req.tags, req.createMissing);
        pages.save(req.pageId, new PageSaveRequest(page, /*unlock*/ true));
        trx.success();
        return pages.load(req.pageId, false, ...).getPage();
    }
}
```

```java
@McpTool(name = "search_content", group = "search", access = READ,
         permissions = { @Perm(type = PAGE, value = "view") })
public SearchResult searchContent(SearchContentRequest req) throws NodeException {
    ObjectNode body = QueryBuilder.from(req);
    body = ensureBoolWrapper(body);       // the CMS injects permission filters only into query.bool
    String raw = search.searchType(req.type, params(req), body.toString());
    return trimHits(raw);
}
```

Both examples show why the composite is the safer half of the design. `update_page_tags` holds the lock, the
mutation and the save in one `Trx`, so an abandoned agent run cannot leave a page locked and a failed save
cannot leave half the tags written. `search_content` guarantees the `query.bool` wrapper that the CMS needs
before it will inject its permission filters (§4.2.1), which no caller can be relied upon to do.

### 10.2 Why composites are less fragile than they look

A composite is Java inside the CMS build. If `PageResourceImpl.save` changes its signature, the composite
stops compiling and someone fixes it before the build passes. A generated tool has the opposite failure mode:
the method still compiles, the scanner still produces a schema, and the schema silently changes shape. An
agent then sends what worked yesterday and gets a validation error at run time, or worse, sends a field that
is now ignored. Compile-time breakage inside the CMS build is cheaper than silent runtime drift, so the
orchestration that matters most belongs in composites rather than in each client.

### 10.3 Three rules

1. **Expose the primitives next to every composite.** `update_page_tags` does not replace `get_page`, and
   `create_construct` does not replace `list_constructs`. An agent that needs to do something the composite
   does not cover should be able to fall back to the underlying operations. The one exception is the raw
   Elasticsearch passthrough, which **must never be exposed unwrapped**: a raw body reaches Elasticsearch
   without the CMS permission filter unless it already carries `query.bool` (§4.2.1), so the only search
   surface is the wrapped tools.
2. **`mcp/tools.json` is the contract.** A test in the CMS build compares the generated tool list against it
   on names, groups, `access`, required input fields and declared permission requirements, and fails the
   build on a difference. `roles` and `scenarios` are not compared, because the CMS does not generate them.
   A client mirrors the check: it compares `tools/list` against the allowlist its workflow declares and marks
   the workflow unavailable on a mismatch, rather than discovering the gap mid-run.
3. **Changes are additive once tools ship.** New tools, new optional input fields and new output fields are
   free. Renaming a tool or a field, removing one, or narrowing an input is a breaking change and bumps the
   tool specification version.

### 10.4 Which tools are composite

| Tool | What it composes |
| --- | --- |
| `whoami` | `GET /user/me` plus the group list, reduced to the fields an agent needs |
| `get_permissions` | `GET /perm/{type}/{id}` and `GET /perm/{perm}/{type}/{id}`, decoding the `perm` and `rolePerm` bit strings into a named boolean map |
| `list_nodes` | `GET /node` plus `GET /node/{id}/languages` per node |
| `search_content` | Query construction from the structured input, the mandatory `query.bool` wrapper, the passthrough call, and hit trimming |
| `find_similar` | The same, with a `more_like_this` clause built from a ref or a text block |
| `count_content` | The same, with `size: 0`, a `match_all` inside the wrapper so the filter is injected, and one `terms` aggregation only when `groupBy` is given |
| `get_related` | Up to thirteen `usage` endpoints across pages, files and images, normalised into one grouped result |
| `get_folder_tree` | `GET /folder/getFolders/{id}` in tree mode, optional `GET /folder/count/{id}` per folder, and depth trimming the CMS does not offer |
| `get_template` | `GET /template/load/{id}`, `GET /template/{id}/tag` and optionally `GET /template/{id}/folders` |
| `render_preview` | `GET /page/render/{id}` or the devtools preview route, plus truncation |
| `update_page_tags` | Load with lock, optional tag creation, save with unlock, read-back — one transaction |
| `update_page_properties` | Load with lock, field merge, save with unlock — one transaction |
| `publish_page` | `POST /page/publish/{id}` plus the state read-back that tells published from queued for approval |
| `upload_file` | Base64 decode or server-side fetch, then the multipart create call |
| `create_construct` | Synthesises the whole `Construct` body, including the Handlebars part at `typeId` 43, and links the nodes in the same call |
| `update_construct` | Read, merge the caller's changes over the current construct, then full replacement `PUT` |
| `ensure_construct_category` | Category lookup by name, then create when absent |
| `assign_construct_to_nodes` | Read the current assignment, then link and unlink to reach the desired state |
| `assign_user_to_group` | Group membership plus optional per-node scoping |
| `preview_permission_impact` | The user's groups and each group's type permissions, unioned into a before and after view |

Two future Design Studio tools are composite as well: `render_diff` composes the version diff, the generic
HTML or source diff and a render, and `decompose_design` is client-side planning whose outputs are
`create_construct`, `create_template` and `link_template_to_folders`.

Everything else in `mcp/tools.json` is a generated tool. Each tool carries
`"implementation": {"kind": "generated"}` or `{"kind": "composite", "composes": [...], "transaction":
"single"}`, so the split is machine-readable, and `search_content` additionally carries
`"never_exposed_unwrapped": true`. Each tool also carries `"permissions"`, the object type and permission
bits the annotation declares; entries marked `"confirm": false` name the type and permission expected rather
than one read off the CMS source, and belong to the requirements in §11.

## 11. Requirements towards the CMS implementation

The tool surface rests on the following work in the CMS. Items 1 to 4 are prerequisites for any tool; the rest
are needed for individual tools or scenarios.

1. **Authenticate the MCP endpoint.** Accept `Authorization: Bearer <CMS API token>`, resolve it to a CMS user
   through the transport's `contextExtractor`, answer HTTP 401 for anything unresolvable before the MCP
   envelope with no anonymous fallback, validate `Origin` with a `ServerTransportSecurityValidator`, and run
   each tool call in a transaction owned by the resolved user (§2.2).
2. **Make `/mcp` reachable.** The path is decided and `MCP_PATH` configures it. It must be reachable through
   the same reverse proxy as `/rest`, with request and response streaming left unbuffered for the SSE upgrade,
   and stable per installation so a client can store it once (§2.1, §2.6).
3. **Harden the Elasticsearch filter injection.** `addFilters` should build the `query.bool` wrapper itself and
   fail closed rather than forwarding an unfiltered body (§4.2.1). Adjusting `hits.total` after the `canView`
   post-filter would additionally make counts object-accurate.
4. **Activate the `elasticsearch` feature** with the page index populated per language. Without it the whole
   `search` group answers HTTP 405 and scenario S2 has no implementation.
5. **Publish the API token model classes and support `pruneOnExpiry`.** `ApiTokenCreationRequest`,
   `ApiTokenCreationResponse` and `ApiTokenListResponse` are not yet in the tree. The creation request is
   `{name, expires, pruneOnExpiry}` (§2.2); `pruneOnExpiry` is the new field and the one the session-scoped
   model depends on, because without it every expired session leaves a dead token row for someone to clean up.
   `GET /rest/admin/token` should expose the token name, so a client can find and replace a token of its own
   name after an interrupted run.
6. **Make token creation cheap enough to do often.** One token per workflow session means creation is a
   routine call on the hot path of starting work, not an administrative act: no unnecessary write
   amplification, no full-table scan, no cost that grows with the number of tokens a user has accumulated.
7. **Keep token lookup a hash lookup.** Resolving a bearer token to a user on every MCP request must stay a
   single indexed lookup on the hashed secret, with the expiry check part of that lookup. A per-request scan
   over a user's tokens would turn the session-scoped model into a performance problem precisely when an
   installation uses it most.
8. **Extend the index mapping** with `constructKeywords`, the construct keywords used in a page, which lets S1
   find reusable page patterns and S3 find where a construct is used. `templateId`, `path` and `niceUrl` are
   already indexed.
9. **Confirm the `content` field semantics** as editor-entered text only, an array, current version only
   (`cms-search-core/.../object/search/SearchIndex.java`). Every retrieval claim depends on it.
10. **Publish an authoritative part type-id table**, including whether type id 43 is stable across versions.
    The allowlist in §4.5 is derived from the `switch` in `cms-oss/cms-restapi/.../rest/model/Property.java`.
11. **Support partial construct updates.** `PUT /construct/{id}` replaces `parts[]` wholesale, so
    `update_construct` needs read-merge-write, which races between two clients.
12. **Confirm whether a construct icon can be set through REST.** The devtools package format supports an
    `icon` field; the REST `Construct` and `Part` schemas do not.
13. **Confirm `/devtools/preview/page/{id}` for a non-Editor-UI client**, and define the lifetime and
    authentication model of a `previewUrl` a client will put in an iframe (§4.3).
14. **Decide on the outbound fetch for `upload_file`**: a configured host allowlist, HTTPS only, a 25 MB cap
    and no redirects (§4.4).
15. **Provide a signal for the publish branch**, so `publish_page` can report online versus queued for
    approval more cheaply than `GET /page/load/{id}?workflow=true` plus `GET /page/pubqueue`.
16. **Decide whether a permission simulation endpoint is worth adding.** `preview_permission_impact` composes
    group permissions today, which does not reproduce folder-tree inheritance or role-per-language subtleties.
17. **Choose the MCP access log destination.** The per-call record covers reads as well as writes, so a
    dedicated log plus the automatic action-log entries for writes is the recommendation (§6).
18. **Provide a construct refresh path for the editor.** A construct created through the API is immediately
    visible to `GET /construct`, but the Editor UI loads constructs once per content-frame load, so a new
    construct does not appear until a reload.
19. **Define the Tier 2 path**: the route for a tagmap entry and the supported procedure for the subsequent
    structure repair (§7).
20. **Later: implement the MCP Authorization specification**, as listed in §2.2.1. Phase One uses a static
    bearer token; adopting the specification changes no tool in `mcp/tools.json`.
