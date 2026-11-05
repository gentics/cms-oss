# MCP Server Integration (GPU-2665)

Integration of the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) into the
Gentics CMS OSS server, so that the CMS acts as an MCP (Model Context Protocol) server.

This document describes the MCP integration as of 2026-11-02: the merge (`49b974a94`) of the GenAIx
work (`01d36e0a3`) with the removal of the annotation-based registration (`aa839ffe7`), the test
fixes (`bf342250c`) and the clean-up of §9 step 9.
§1-§8 describe the implementation; §9 is a condensed history; §10 lists related documents.

| Ticket | Scope | Status |
| --- | --- | --- |
| **GPU-2665** | **Umbrella story: MCP server for the CMS** | authentication, explicit tool framework, **47 tools** (the phase-one tools of the GenAIx tool contract), problem results, REST permission enforcement |
| GPU-2666 | MCP server as a servlet under `/mcp` | implemented |
| GPU-2667 | 10 more endpoints via the annotation-based registry | implemented, later **removed** with the whole annotation approach (§9) |
| GPU-2672, GPU-2674 | follow-ups | open |

---

## 1. Architecture overview

```
HTTP POST/GET/DELETE /mcp
  └─ Jetty ServletHolder (asyncSupported)            cms-oss-server: OSSRunner
      └─ HttpServletStreamableServerTransportProvider  (MCP SDK; is an HttpServlet)
          ├─ CmsMcpContextExtractor   per request: copies bearer token / session cookie
          │                           into the McpTransportContext (no DB access)
          ├─ CmsMcpSecurityValidator  optional (MCP_REQUIRE_AUTH): 401 if no credential at all
          └─ McpSyncServer
              └─ 47 tools registered by McpTools.registerAll
                  └─ one AbstractMcpTool subclass per tool
                      AbstractMcpTool#call:
                        resolve credentials → CMS Session (McpAuthenticator)
                        reject if no session (all tools require one)
                        bind session to the thread (McpSessionBinding)
                        invoke(arguments, session)
                          → REST resource impls, wrapped by RestPermissions.guard
                            (checks their @RequiredPerm like the REST API does)
                          → or EnterpriseSearch (search tools, enterprise module by reflection)
                        success: text + structuredContent (if outputSchema)
                        failure: isError + text + structuredContent.problem (Problem)
```

All MCP code lives in **cms-core**, package `com.gentics.contentnode.mcp`. `cms-oss-server` only
mounts the servlet, registers the tools and shuts the server down. Outside the MCP packages, the
merged work changed only `ConfigurationValue` (§3) and added `NodeException#getMessageKey()` in
`base-lib` (used to recognize "locked" failures, §5.3); the test fixes changed `rest.version.Main` in
`cms-restapi` (§1.1). No REST resource or model (cms-restapi) was changed for MCP.

### 1.1 File inventory

Main sources (`cms-core/src/main/java/com/gentics/contentnode/mcp/`):

| File | Role |
| --- | --- |
| `MCPServer.java` | static holder: builds transport provider + `McpSyncServer` on first `getServlet()` |
| `AbstractMcpTool.java` | base class of every tool: `tool()`, `requiresAuthentication()`, `call(...)`, `toSyncToolSpecification()`; authentication, session binding, result and problem serialization, shared helpers |
| `McpTools.java` | explicit list of the 47 tool instances (`tools()`), `registerAll(McpSyncServer)` |
| `McpArgs.java`, `util/Args.java` | strict argument parsing (`McpArgs`: `stringArg`, `intArg`, `booleanArg`, `nullIfBlank`; `Args`: arguments not covered by `McpArgs`) |
| `McpSchemas.java` | `schema(type, description, keyValues...)` for hand-built JSON schemas |
| `auth/*` | `McpRequestCredentials`, `CmsMcpContextExtractor`, `McpAuthenticator`, `McpSessionBinding`, `CmsMcpSecurityValidator` (§4) |
| `tools/*Tool.java` | the 47 tools (§6) |
| `model/*.java` | output records with `jsonSchema(...)`: `ObjectRef`, `UserRef`, `UserItem`, `WhoamiUser`, `VersionInfo`, `TranslationStatusInfo`, `PageInfo`, `FileInfo`, `FolderItem`, `TagInfo`, `ConstructInfo`, `LanguageInfo`, `NodeInfo`, `SearchHit`, `ListResult<T>`, `Timestamps`; and `Problem` (§5.3) |
| `util/*.java` | shared helpers (§5.5) |

Other touched places:

| File | Role |
| --- | --- |
| `cms-core/…/runtime/ConfigurationValue.java` | `MCP_ENABLED`, `MCP_PATH`, `MCP_REQUIRE_AUTH`, `MCP_UPLOAD_URL_HOSTS`, `MCP_BASE_URL` (§3) |
| `base-lib/…/api/lib/exception/NodeException.java` | new getter `getMessageKey()` |
| `cms-restapi/…/rest/version/Main.java` | `getImplementationVersion()` falls back to the package version (or `DEBUG`) when `cms.build.properties` is missing, instead of throwing (`bf342250c`); used by `whoami` and `MCPServer` |
| `cms-oss-server/…/server/OSSRunner.java` | `addMcpServlet(context)`, `registerMcpTools()`, `MCPServer.shutdown()` |
| `pom.xml` | property `mcp.version` = `2.0.1` |
| `cms-oss-bom/pom.xml` | import of `io.modelcontextprotocol.sdk:mcp-bom` |
| `cms-core/pom.xml` | dependencies `mcp-core`, `mcp-json-jackson2` |
| `cms-core/src/test/resources/com/gentics/contentnode/mcp/tools.json` | vendored GenAIx tool contract (§5.6) |
| `cms-oss-changelog/…/entries/2026/09/8883.GPU-2666.enhancement` | changelog: the `/mcp` endpoint |
| `cms-oss-changelog/…/entries/2026/10/8985.GPU-2665.enhancement` | changelog: the tools, configuration, problem results |

Tests: §7.

---

## 2. Server bootstrap

### 2.1 Dependencies

`mcp-core` + `mcp-json-jackson2`, version `2.0.1` via `mcp-bom`. Not the `io.modelcontextprotocol.sdk:mcp`
bundle: in SDK 2.x that is `mcp-core + mcp-json-jackson3`, which pulls in **Jackson 3**
(`tools.jackson.*`), while the CMS uses Jackson 2.21.x everywhere. Switching would be a one-line
change in `cms-core/pom.xml` plus the `JacksonMcpJsonMapper`/`DefaultJsonSchemaValidator` imports
in `MCPServer`.

New transitive dependencies: `io.projectreactor:reactor-core` (by `mcp-core`) and
`com.networknt:json-schema-validator` (by `mcp-json-jackson2`, used for tool input/output schema
validation). Jackson 2, `slf4j-api` and the servlet API were already present. The SDK is compiled
against Servlet 6.1 but its servlet transport only uses Servlet 6.0 API, so Jetty 12 **ee10** works.

### 2.2 `MCPServer`

```java
transportProvider = HttpServletStreamableServerTransportProvider.builder()
        .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
        .mcpEndpoint(path)
        .contextExtractor(new CmsMcpContextExtractor())
        .securityValidator(new CmsMcpSecurityValidator())
        .build();

server = McpServer.sync(transportProvider)
        .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
        .jsonSchemaValidator(new DefaultJsonSchemaValidator())
        .serverInfo(SERVER_NAME, Main.getImplementationVersion())   // "Gentics CMS", CMS version
        .instructions(SERVER_INSTRUCTIONS)
        .capabilities(ServerCapabilities.builder().tools(true).build())
        .build();
```

| Method | Purpose |
| --- | --- |
| `isEnabled()` / `getPath()` | read `MCP_ENABLED` / `MCP_PATH` |
| `getServlet()` | builds both objects on the first call, returns the transport provider (an `HttpServlet`) |
| `getServer()` | `Optional<McpSyncServer>`, empty until `getServlet()` ran |
| `shutdown()` | `closeGracefully()`, resets the holder |

JSON mapper and schema validator are passed explicitly instead of relying on the SDK's
`ServiceLoader` lookup, so the result does not depend on the classpath of the shaded jar.
Input validation against each tool's `inputSchema` is **on** (SDK default `validateToolInputs(true)`,
not changed here).

### 2.3 `OSSRunner`

`start()` calls `addMcpServlet(context)` right after the `/rest/*` and `/jmx` servlets, then
`registerMcpTools()` (`MCPServer.getServer().ifPresent(McpTools::registerAll)`), and
`MCPServer.shutdown()` in its `finally` block. Two details matter in `addMcpServlet`:

1. **`setAsyncSupported(true)` on the `ServletHolder` is mandatory.** The transport calls
   `request.startAsync()` for SSE streams; the SDK's `@WebServlet(asyncSupported = true)` is not
   evaluated in embedded Jetty.
2. **Exact path mapping, not a prefix.** The transport checks `requestURI.endsWith(mcpEndpoint)`
   and answers 404 otherwise, so `/mcp/*` would gain nothing.

---

## 3. Configuration

All are `ConfigurationValue`s, so environment variable, system property and configuration property
all work:

| Setting | Env variable | System property | Config property | Default |
| --- | --- | --- | --- | --- |
| enable endpoint | `MCP_ENABLED` | `com.gentics.contentnode.mcp.enabled` | `mcp.enabled` | `true` |
| endpoint path | `MCP_PATH` | `com.gentics.contentnode.mcp.path` | `mcp.path` | `/mcp` (normalized: leading `/`, no trailing `/`) |
| reject requests without any credential (HTTP 401) | `MCP_REQUIRE_AUTH` | `com.gentics.contentnode.mcp.requireAuth` | `mcp.requireAuth` | `false` |
| hosts `upload_file` may fetch a URL source from (comma-separated `host` or `host:port`) | `MCP_UPLOAD_URL_HOSTS` | `com.gentics.contentnode.mcp.uploadUrlHosts` | `mcp.upload_url_hosts` | empty: URL sources refused |
| base URL of the CMS as users open it, returned by `whoami` as `cms.baseUrl` | `MCP_BASE_URL` | `com.gentics.contentnode.mcp.baseUrl` | `mcp.base_url` | empty: omitted |

Note the inconsistent config property style (`mcp.requireAuth` vs. `mcp.upload_url_hosts`,
`mcp.base_url`). The MCP server is built once at startup; it does not react to
`onReloadConfiguration()`.

---

## 4. Authentication and permissions

A tool call runs as the real, authenticated CMS user, so permission checks apply to the caller,
not to the CMS system user.

### 4.1 Credential extraction (per request, no DB)

`CmsMcpContextExtractor` (the transport's `contextExtractor`) reads the same two credential shapes
`AuthenticationRequestFilter` accepts for `/rest/*`: an `Authorization: Bearer <api token>`
header and the `GCN_SESSION_SECRET` cookie. It stores them as `McpRequestCredentials` on the
`McpTransportContext` (`McpRequestCredentials#CONTEXT_KEY`). It does **no DB access and never
throws**: it runs outside the transport's own try/catch (checked against the SDK `v2.0.1`
sources), so an exception would reach the client as a raw HTTP 500.

### 4.2 Credential resolution (per tool call)

`McpAuthenticator.resolve(McpRequestCredentials)` mirrors `AuthenticationRequestFilter
#tryApiToken()`/`#trySessionSecretSession()`: API token first, then session cookie
(`ApiTokenFactory#hash`/`#load` + `ApiTokenSession`, respectively `SessionToken` + `DBSession#load`
+ `#touch()`), inside its own short-lived system-user `Trx` (`Trx.supply(...)`). Empty credentials
short-circuit before any DB access.

### 4.3 Two independent guards

* **Per tool call, always on.** `AbstractMcpTool#call` resolves the session; if
  `requiresAuthentication()` (default `true`; no tool overrides it, only the test tools of
  `AbstractMcpToolTest` do) and no session was resolved,
  it returns `isError(true)` with a text block, **without** a `problem` (§5.3), and without
  running the tool.
* **Transport level, opt-in.** With `MCP_REQUIRE_AUTH=true`, `CmsMcpSecurityValidator` rejects a
  request with **HTTP 401** if it carries neither a bearer header nor the session cookie. This
  checks **presence only, not validity**; an invalid credential is still only caught by the
  per-call guard. Default `false`, so clients like the MCP Inspector can connect without credentials.

### 4.4 Session binding and thread-locals

The SDK runs sync tool handlers on pooled `Schedulers.boundedElastic()` threads, reused across
unrelated calls (`McpServerFeatures.AsyncToolSpecification#fromSync`, SDK `v2.0.1`).
`McpSessionBinding` (try-with-resources in `AbstractMcpTool#call`) binds the resolved session on
`ContentNodeHelper` for the duration of the call and restores the previous state afterwards, also
when the tool throws. Without a session it sets the fixed backend language ID `2`, so i18n messages
are translated rather than shown as raw keys.

### 4.5 Permissions

* **`@RequiredPerm` is enforced.** The REST API checks these annotations in
  `AuthorizationRequestFilter`, which a tool calling a resource implementation directly bypasses.
  `util/RestPermissions.guard(ResourceInterface.class, impl)` wraps a resource implementation in a
  proxy that first checks the `@RequiredPerm` annotations of the implementing method and class.
  45 of the 47 tools call it.
* **The two exceptions are the search tools** `search_content` and `find_similar`, which do not
  call guarded REST resources, only `util/EnterpriseSearch`: it adds the CMS permission, node,
  folder and wastebin filters of `SearchResourceImpl#addFilters` to the Elasticsearch body (§6.3).
  (`count_content` uses `EnterpriseSearch` too, but also calls `RestPermissions`.)
* **Object permissions** (view/edit/… on a specific object) are checked by the delegated-to REST
  resource code itself, as for REST calls. A new tool's author still has to verify that the
  delegated-to call performs the object-level check they expect.

---

## 5. Tool framework

### 5.1 Writing a tool

1. Extend `AbstractMcpTool`; implement `tool()` (name in lower snake_case, title, description,
   hand-built `inputSchema`, an `outputSchema`) and
   `invoke(Map<String, Object> arguments, Optional<Session> session)`.
2. Add an instance to `McpTools.TOOLS`.
3. Delegate to the existing REST resource implementation, wrapped by `RestPermissions.guard`
   (e.g. `RestPermissions.guard(PageResource.class, new PageResourceImpl())`), rather than
   reimplementing logic.
4. Check that the delegated-to method does not read JAX-RS `@Context` fields (e.g. those of
   `AbstractContentNodeResource`): the resource class is instantiated directly, so they are `null`.
5. If the tool is part of the GenAIx contract, its name, required inputs and input property names
   must match `tools.json` (§5.6); otherwise add it to `McpContractTest.EXEMPT`.
6. Write a unit test (no DB) and, for anything touching permissions or persistence, a
   `DBTestContext` integration test under `tests/mcp/`. Test helpers: `tools/ToolAssertions`,
   `tools/PageFixtures`.

**Duplicate names are not detected.** `McpAsyncServer#addTool` silently *replaces* an existing tool
with the same name and only logs a warning (checked against the SDK `v2.0.1` sources), and
`McpTools.registerAll` has no own check.

### 5.2 Results and structured output

`AbstractMcpTool#call` serializes the result of `invoke` with Jackson into a text content block.
If the tool declares an `outputSchema` (all tools do), it also sets
`structuredContent` (the same result as a `Map`). This is required: in SDK `2.0.1`, every tool
handler is wrapped in `McpAsyncServer.StructuredOutputCallToolHandler`, which turns a successful
result **without** `structuredContent` into an error when the tool has an output schema, and
validates present structured content against the schema. Hence the model records are
`@JsonInclude(NON_NULL)` (`"type": "integer"` does not accept `null`).

### 5.3 Problem results

When `invoke` throws, `AbstractMcpTool#call` returns `isError(true)` with the exception message as
text **and** `structuredContent.problem`: a `model/Problem` record shaped like the contract's
`Problem` (RFC 9457) with `type`, `title`, `status`, `detail`, `tool`, `retryable` and `cms`
(`responseCode`, `messages`). `Problem.of(tool, cause)` searches the cause chain (after unwrapping
an `InvocationTargetException`) in this order:

| Kind (`type` = `https://cms.gentics.com/problems/` + slug) | Status | Retryable |
| --- | --- | --- |
| `search-unavailable` (`SearchUnavailableException`) | 503 | no |
| `object-locked` (read-only exception whose message key ends in `.readonly.locked`) | 423 | yes |
| `permission-denied` (other read-only exception, missing privileges) | 403 | no |
| `not-found` | 404 | no |
| from a CMS response code: `permission-denied`, `not-found`, `invalid-data`, `object-locked`, `maintenance-mode` (503, yes), `feature-not-licensed` (403, no) | | |
| `invalid-data` (rejected tool argument) | 422 | no |
| `cms-failure` (anything else) | 500 | yes |

`detail` is not localized: the session binding, and with it the language, is gone when a failure
is mapped. `status` is the status of the problem type, not necessarily the HTTP status the REST API
would use.

### 5.4 Input validation

The SDK validates arguments against `inputSchema` before calling the tool. Tools still re-check
arguments via `McpArgs`/`Args`/`ListArgs`, because `call()` can be invoked without the SDK (as the
tests do). The helpers are strict: out-of-range values are **rejected**, not clamped, and only real
JSON integers/booleans/strings are accepted (no numeric strings).

### 5.5 Shared building blocks

| Where | What |
|---|---|
| `AbstractMcpTool` | `protected static` helpers: `errorMessage`, `requireOk`, `saveOrReleaseLock`, `releaseLock`. Tools import the argument and schema helpers of `McpArgs`/`McpSchemas` statically |
| `util/RestPermissions` | `guard(...)`: `@RequiredPerm` enforcement (§4.5) |
| `util/Writes` | write tool helpers: `pageInNode` (resolve `pageId` + `nodeId` to the page as that node sees it, its localized copy if any; **refuses a page the node only inherits**), `folderInNode` (folder must be visible in the node), `logIdempotencyKey`, `readBack` |
| `util/ChangedFields` | named getters; `snapshot(before).diff(after)` |
| `util/Slice`, `util/ListArgs`, `util/ListResponses`, `model/ListResult` | offset paging (`q`/`size`/`from`, per-tool `Limits`) over fully fetched lists; envelope `{total, totalIsExact, items, nextFrom, truncated}` |
| `util/EnterpriseSearch`, `util/SearchBodies`, `util/SearchArgs`, `util/SearchUnavailableException` | search via the enterprise module (§6.3) |
| `util/PartSpecs` | parts of constructs created/updated by tools: parsing and rules the CMS does not check (part type allowlist, one Handlebars part, unique keywords, select-part datasources), mapping to REST parts |
| `util/PartValues` | write plain part values into REST properties by part type; every value `TagInfo#value(Property)` returns can be written back unchanged; object references also as plain IDs |
| `util/Constructs` | look up constructs by keyword, and the nodes of a construct (no suitable REST route) |
| `util/UrlSources` | fetch a URL file source `{kind: url, url, expiresAt}` for `upload_file`: allowlisted hosts only (`MCP_UPLOAD_URL_HOSTS`), https (http only from localhost), no redirects, timeouts and size limit; refusals start with a fixed prefix so clients can fall back to base64 |
| `util/UserGroups` | users, groups and a user's groups for the admin tools |
| `model/ObjectRef` | shared object reference (`type`/`id`/`globalId`/`nodeId`/`name`/`path`/`language`/`niceUrl`/`url`); lowercase `type` strings; unknown input fields ignored, so a ref returned by one tool can be passed back |

`releaseLock` calls `NodeObject#unlock()`: for pages and templates this only clears the caller's
own lock (`… WHERE id = ? AND locked_by = ?`); forms throw `ReadOnlyException` if another user holds
the lock (logged); for most other types `unlock()` does nothing. Output schemas are hand-built;
`ModelJsonSchemaTest` checks that each record's schema describes exactly its record components.

### 5.6 The GenAIx tool contract (`tools.json`)

`cms-core/src/test/resources/com/gentics/contentnode/mcp/tools.json` is a **vendored, unmodified**
copy of the external tool specification: version `1.0.0-rc.2`, status "agreed and frozen for
development (release candidate 2)", owner of the specification `ioflair`, of the implementation
"Gentics (cms-mcp Java module)". It defines 54 tools in 7 groups, each with `access` (read/write),
`phase` (`one` or `future`), input/output schemas, and permission metadata, plus transport, auth and
error-mapping conventions.

`McpContractTest` compares every registered tool (`EXEMPT` is empty) with its contract
entry: tool name, phase (must be `one`), required input members and input property names.
Descriptions, output schemas and the group/access/permission metadata are **not** compared, and not
every contract tool has to be registered.

**Note:** the contract's `implementation_model` describes an annotation-based mechanism
(`@McpTool(name, group, access, permissions)` on resource methods, "scanned at start-up") and a
conformance test that scans annotations. The implementation does not work that way: all tools are
explicit classes (§9 step 5), and the conformance test reads `McpTools.tools()`.

---

## 6. Tools

All 47 tools require authentication and declare an output schema. Group,
access and phase are taken from `tools.json`.

| Group (contract) | Tools | Access |
| --- | --- | --- |
| `identity` | `whoami`, `get_permissions`, `list_nodes` | read |
| `search` | `search_content`, `find_similar`, `get_related`, `count_content` | read |
| `content_read` | `get_folder_tree`, `list_folder_items`, `get_page`, `get_page_tags`, `list_templates`, `get_template`, `get_file`, `get_image`, `get_page_versions`, `render_preview` | read |
| `content_write` | `create_page`, `update_page_tags`, `add_page_tag`, `update_page_properties`, `publish_page`, `take_offline`, `delete_page`, `translate_page`, `restore_page_version`, `upload_file`, `create_folder` | write |
| `constructs` | `list_constructs`, `get_construct`, `list_construct_categories`, `list_packages`, `list_part_types`, `list_datasources`, `get_datasource` | read |
| `constructs` | `create_construct`, `update_construct`, `ensure_construct_category`, `assign_construct_to_nodes`, `add_construct_to_package` | write |
| `admin` | `list_users`, `list_groups`, `get_group_permissions`, `preview_permission_impact` | read |
| `admin` | `create_user`, `assign_user_to_group`, `remove_user_from_group` | write |

**Not implemented** (contract phase `future`): `get_publish_state`, `request_tier2_field_mapping`,
and the `design` group (`create_template`, `update_template`, `link_template_to_folders`,
`render_diff`, `decompose_design`).

The sections below describe the tools documented in detail so far. For the others, the tool class,
its unit test and its `tools.json` entry are the reference; they have not been analysed for this
document beyond the facts in §4-§6.

### 6.1 Multichannelling in write tools

The page write tools (`update_page_properties`, `update_page_tags`, `add_page_tag`, `publish_page`,
`take_offline`, `delete_page`, `translate_page`, `restore_page_version`) accept `nodeId` and resolve
the page with `Writes.pageInNode`: the page as that node sees it, i.e. its localized copy if the node
has one. **A page the node only inherits is refused**, because a write would change it in its
master node and every inheriting channel. `create_page` and `upload_file` check with
`Writes.folderInNode` that the target folder is visible in the node.

### 6.2 `list_nodes`

Input: `q` (optional string, max 200; blank = no filter; matched case-insensitively against node
ID and name by the resource's `ResolvableFilter`), `size` (1-200, default 25), `from` (0-10000,
default 0). Output: `ListResult<NodeInfo>` with `total`, `totalIsExact` (always `true`),
`nextFrom`, `truncated`, `items`; each item has a `ref`, `host` (as the REST model reports it,
including protocol), `publishDir`, `languages` (`{id, code, name}`), default file/image folder IDs
and `contentRepositoryId`.

* Delegates to `NodeResourceImpl#list` (unpaged) and `#languages` per node. Paging is "fetch all,
  then slice in memory", because the offset-based input does not map onto
  `PagingParameterBean`'s page numbers.
* Permissions: `list` filters to `ObjectPermission.view`, `languages` re-checks `view` per node.
* Sort: the resource's default (`name`). `sort`, `perms` and staging `package` are not exposed.

### 6.3 Search tools

`search_content`, `find_similar` and `count_content` use `util/EnterpriseSearch`, the Elasticsearch
passthrough of the **enterprise module cms-search**, which cms-oss cannot compile against: every
enterprise class and method is looked up by reflection. If the module or the `elasticsearch`
feature is missing, `SearchUnavailableException` is thrown and the call fails with the problem type
`search-unavailable`. Every search body has `query.bool` (`SearchBodies`), because the CMS adds its
permission, node, folder and wastebin filters (`SearchResourceImpl#addFilters`) only to
`query.bool.filter` and forwards other bodies unfiltered. The passthrough's own response handling
is not used (it needs JAX-RS injection and reads the hit type from `_type`, which Elasticsearch 8 no
longer returns). These tools cannot be exercised fully in cms-oss without the enterprise module.

### 6.4 `update_page_properties`

Changes page metadata without touching content/tags.

Input: `pageId` (required, ≥ 1), `nodeId` (§6.1), `name` (1-255), `fileName` (≤ 255),
`description` (≤ 4000, `""` clears), `niceUrl` (≤ 500, `""` clears, ignored without the `NICE_URLS`
feature), `priority` (1-100), `templateId` (≥ 1), `deriveFileName` (boolean, default `false`, only if
`fileName` is absent), `failOnDuplicate` (boolean, default `true`), `idempotencyKey` (≤ 128, logged
only, no de-duplication). Output: `{ page: PageInfo, changedFields: [...] }`, both required.

Flow: `Writes.pageInNode` → argument validation → `PageResourceImpl#load(update = true)` (checks
`edit`, locks) → `#save(unlock = true)` → `#load(update = false)` in the given node with `folder`,
`langvars`, `translationstatus`, `versioninfo` for the output. Invalid input never locks a page.

* **Only supplied fields are submitted.** The tool builds a fresh REST `Page` with only the given
  fields (`UpdateRequest#toRestPage`) instead of resubmitting the loaded page. `ModelBuilder
  #getPage` applies every non-null field, and a loaded page carries tags and translation status:
  resubmitting it would re-save all tags (requiring edit permission on every visible object tag),
  change the translation sync state, and prevent filename derivation.
* **`changedFields`** is a before/after diff (`ChangedFields`) of `name`, `fileName`,
  `description`, `niceUrl`, `priority`, `templateId`, so it also reports changes `save()` makes on
  its own (derived filename, appended extension) and omits resubmitted unchanged values.
* **Lock release on failure.** `save()` unlocks only at its end, so early returns (duplicates) and
  exceptions would leave the page locked for `lock_time` (default 600 s). `saveOrReleaseLock`
  turns a non-`OK` response into a tool error and calls `releaseLock` (not
  `PageResourceImpl#cancel`, which would restore the last version). If the caller already held
  the lock before the call, it is released too (same as REST `save(unlock = true)`); another
  user's lock is left alone.
* `save()` was deliberately **not** changed to return the saved page (tried and reverted: stale
  lock state, broke Java `RestClient`s expecting `GenericResponse`, and made every save, including
  the Aloha keepalive, more expensive).
* Output details: `ref` from `ObjectRef.forPage(page)` (`nodeId` from the folder, `url` = preview
  URL with `liveUrl` fallback); `languageVariants` keyed by language **code**; users as
  `{id, login}`; `versions` newest first; unset timestamps (`-1`/`0`) omitted.

### 6.5 `whoami` and `upload_file`

* `whoami` returns the caller and a `cms` object with `baseUrl` (`MCP_BASE_URL`, omitted if empty)
  and the CMS version (`Main#getImplementationVersion()`).
* `upload_file` accepts a URL source only for hosts in `MCP_UPLOAD_URL_HOSTS` (`util/UrlSources`,
  §5.5); with the default (empty) every URL source is refused.

---

## 7. Tests

### 7.1 Test classes

* **Unit tests, no DB** (73 classes under `cms-core/src/test/java/com/gentics/contentnode/mcp/`):
  one test class per tool under `tools/`, plus `AbstractMcpToolTest`, `McpToolsTest`,
  `McpContractTest` (§5.6), `McpArgsTest`, `McpSchemasTest`, `auth/*`, `model/{ModelJsonSchemaTest,
  ObjectRefTest, ProblemTest, TagInfoTest}`, `util/*`. Helpers: `tools/ToolAssertions`,
  `tools/PageFixtures`.
* **Integration tests** (`DBTestContext`, 5 classes under
  `cms-core/src/test/java/com/gentics/contentnode/tests/mcp/`): `ListNodesToolIntegrationTest`,
  `UpdatePagePropertiesToolIntegrationTest`, `GetPermissionsToolIntegrationTest`,
  `WhoamiToolIntegrationTest`, `McpProblemIntegrationTest`.

### 7.2 Running the tests

All MCP unit and integration tests of `cms-core`:

```bash
mvn -pl cms-core test \
  -Dtest='com/gentics/contentnode/mcp/**/*Test,com/gentics/contentnode/tests/mcp/*Test'
```

* The integration tests need the test DB containers (`testdb-containers` profile, see the
  repository's `CLAUDE.md`).
* Mockito has to mock `McpSyncServer` (`McpToolsTest`); on a JDK newer than the one the
  project's Byte Buddy supports (JDK 21), this needs `-Dnet.bytebuddy.experimental=true` in the
  test JVM's `argLine`. CI uses JDK 17 and does not need it.
* The search tools need the enterprise module cms-search to run for real (§6.3).

### 7.3 Manual testing

`docs/mcp-tests.md` holds a resumable manual test protocol for `page_load`, `list_nodes` and
`update_page_properties` (its §9-§11; §0-§8 are historical). It does not cover the GenAIx tools, and
its `page_load` part is obsolete since the tool was removed (§9 step 9).

Against a running server, without credentials:

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{
        "protocolVersion":"2025-06-18","capabilities":{},
        "clientInfo":{"name":"curl","version":"1.0"}}}'
```

Expected: `200`, an `Mcp-Session-Id` header, `serverInfo.name == "Gentics CMS"`. A tool call then
needs that session ID plus a credential: `Authorization: Bearer <api token>` or
`Cookie: GCN_SESSION_SECRET=<value>` (from `/rest/auth/login`).

* **MCP Inspector** (`npx @modelcontextprotocol/inspector`, transport Streamable HTTP, URL
  `http://localhost:8080/mcp`): set credentials under "Custom Headers". A `Cookie` header works
  there, because Inspector's Node backend (not the browser) sends the request.
* **Insomnia**: its cookie jar captures the session cookie from `/rest/auth/login`, and a Response
  template tag (attribute Header) can copy `Mcp-Session-Id` from the `initialize` response.

---

## 8. Open points

* **`Origin`/`Host` validation** of the transport (protection against cross-origin browser requests
  that carry the user's session cookie). `CmsMcpSecurityValidator` only checks credential presence;
  the SDK's `DefaultServerTransportSecurityValidator` supports `Origin`/`Host` checks.
* **Unauthenticated calls return no `problem`** (§4.3), unlike every other failure.
* **Duplicate tool names** are not detected (§5.1).
* **Configuration reload**: the server is built once at startup.
* **Contract tools in phase `future`** are not implemented (§6).
* **Contract vs. implementation model**: `tools.json` describes annotation-based registration (§5.6).
* **Config property naming** is inconsistent (§3).
* **Documentation** in `cms-oss-doc` (Textile) for the endpoint, configuration and tools.
* **`docs/mcp-tests.md`** does not cover the GenAIx tools.

---

## 9. History (condensed)

1. **GPU-2666** (`3efe6724f`): MCP SDK integrated, servlet under `/mcp`, `MCP_ENABLED`/`MCP_PATH`.
2. **Annotation-based registration** (GPU-2665/GPU-2667, `5a98a82b7`-`8e7b1e544`):
   `@McpTool`/`@McpToolParam` (in cms-restapi, so parameter beans could be annotated) on 12 REST
   resource methods and 10 parameter beans; `McpToolRegistry` discovered them by ClassGraph
   classpath scan, generated input schemas, bound arguments by reflection, and used
   `ContextUsageAnalyzer` (ASM bytecode analysis) to reject tools whose code read JAX-RS `@Context`
   fields. It had no authentication: tools ran as the CMS system user.
3. **Authentication and explicit tools** (`215c49044`): §4.1-§4.4, `AbstractMcpTool`,
   `ManualMcpTools`, `page_load`. `OSSRunner` stopped calling the classpath scan; the annotation
   code was kept dormant.
4. **`list_nodes`** and **`update_page_properties`** (`3a5c231c8`), `ObjectRef`, structured output
   (§5.2), shared building blocks (`f1d2be8c8`).
5. **GenAIx** (branch `f-gpu-2665-genaix`, 32 commits from `f1d2be8c8`, 2026-10-05 to 2026-10-15):
   problem results, the contract conformance test, `@RequiredPerm` enforcement, then all phase-one
   tools of the GenAIx contract in groups (identity, read, search, write, constructs, devtools
   packages, admin), changelog `8985`, URL uploads and `create_folder` (2026-10-12), CMS version in
   `whoami` (2026-10-14), `nodeId` in write tools (2026-10-14), writing every part type (2026-10-15).
6. **Removal of the annotation-based registration** (`aa839ffe7`, on `f-gpu-2665`, 2026-11-02),
   because all tools are implemented explicitly: deleted `McpTool`, `McpToolParam`,
   `McpToolRegistry`, `ContextUsageAnalyzer`, `McpToolRegistryTest`; removed the ClassGraph and ASM
   dependencies; restored the 11 REST resource implementations and 11 parameter beans to their
   pre-GPU-2665 state (`e45f2779d`), including dropping a `reduce` field in
   `TemplateListParameterBean` that hid `ReducedListParameterBean#reduce`.
7. **Merge** of `origin/f-gpu-2665-genaix` into `f-gpu-2665-merge-prep` (`49b974a94`,
   2026-11-02): no conflicts; the only files changed on both sides were `AbstractMcpTool` and
   `ManualMcpTools` (comment-only changes from step 6).
8. **Test fixes** (`bf342250c`, 2026-11-02): 5 of the 535 MCP tests (all integration tests) were red
   on the merged state, hidden by `testFailureIgnore`: `whoami` failed outside the server jar because
   `rest.version.Main` threw on a missing `cms.build.properties`; `get_permissions` without `id` on
   a type with instances now answers with its own hint to pass `id` instead of the CMS message;
   `WhoamiToolIntegrationTest#testWithoutGroups` was updated for the `cms` object added in `6f853b48d`.
9. **Clean-up** (2026-11-02): `McpToolProvider` folded into `AbstractMcpTool` (its only
   implementation); `ManualMcpTools` renamed to `McpTools`; the argument and schema pass-throughs of
   `AbstractMcpTool` removed (tools import `McpArgs`/`McpSchemas` statically, `objectRefSchema` →
   `ObjectRef.jsonSchema`); the unused `ChangedFields#names()` removed; outdated comments fixed;
   **`page_load` removed** (not in the contract, no output schema, covered by `get_page`), so
   `McpContractTest.EXEMPT` is empty.

The earlier, detailed sections of this document about the annotation approach are in the history
of this file (e.g. `git show f1d2be8c8:docs/mcp-server-integration.md`). The changelog entries
`8884`/`8885` for steps 2 were deleted in `215c49044`; no changelog entry exists for step 6.

---

## 10. Related documents

| Document | Content |
| --- | --- |
| `cms-core/src/test/resources/com/gentics/contentnode/mcp/tools.json` | GenAIx tool contract `1.0.0-rc.2` (§5.6) |
| `docs/mcp-tests.md` | manual test protocol for `list_nodes`, `update_page_properties` and the removed `page_load` |
| `docs/plan-tool-list-nodes.md`, `docs/plan-tool-update-page-properties.md` | design briefs of those two tools (historical) |
