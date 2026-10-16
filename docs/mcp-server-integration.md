# MCP Server Integration (GPU-2665)

Integration of the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) into the
Gentics CMS OSS server, so that the CMS can act as an MCP (Model Context Protocol) server.

| Ticket | Scope | Status |
| --- | --- | --- |
| **GPU-2665** | **Umbrella story: integrate MCP server, expose CMS resources as MCP endpoints via annotations** | **authentication + manual tool registration implemented (§10); `list_nodes` tool + shared `ObjectRef` (§11)** |
| GPU-2666 | Integrate the MCP server as a servlet under `/mcp` | implemented |
| GPU-2667 | Wire up 10 more endpoints spanning varied argument shapes; harden `McpToolRegistry` | implemented (§9) |
| GPU-2672 | (follow-up) | open |
| GPU-2674 | (follow-up) | open |

---

## 1. GPU-2666 — what was implemented

The MCP server of the MCP Java SDK is mounted into the existing Jetty/Jersey stack as a
servlet under the path `/mcp`. At this point the server is fully functional on the
protocol level (initialize / capability negotiation / session handling), but it does not
yet expose any tools, resources or prompts — that is the scope of the follow-up subtasks.

### 1.1 Changed and added files

| File | Change |
| --- | --- |
| `pom.xml` | new property `mcp.version` = `2.0.1` |
| `cms-oss-bom/pom.xml` | import of `io.modelcontextprotocol.sdk:mcp-bom` |
| `cms-core/pom.xml` | dependencies `mcp-core` and `mcp-json-jackson2` |
| `cms-core/…/runtime/ConfigurationValue.java` | new values `MCP_ENABLED` and `MCP_PATH` |
| `cms-core/…/mcp/MCPServer.java` | **new** — bootstrap/holder for the MCP server and its servlet |
| `cms-oss-server/…/server/OSSRunner.java` | registers the MCP servlet, shuts the MCP server down |
| `cms-oss-changelog/…/entries/2026/09/8883.GPU-2666.enhancement` | **new** — changelog entry |

---

## 2. Dependencies

```xml
<!-- pom.xml -->
<mcp.version>2.0.1</mcp.version>
```

```xml
<!-- cms-oss-bom/pom.xml -->
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp-bom</artifactId>
    <version>${mcp.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

```xml
<!-- cms-core/pom.xml -->
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp-core</artifactId>
</dependency>
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp-json-jackson2</artifactId>
</dependency>
```

### 2.1 Why `mcp-core` + `mcp-json-jackson2` and not `mcp`

The ticket names the artifact `io.modelcontextprotocol.sdk:mcp`. In SDK 2.x that artifact is
only a convenience bundle:

```
mcp  ==  mcp-core + mcp-json-jackson3
```

`mcp-json-jackson3` pulls in **Jackson 3** (`tools.jackson.*`), while the CMS uses
**Jackson 2.21.x** everywhere. Using the bundle would put two complete Jackson stacks into
the shaded `cms-oss-server` jar. `mcp-core` plus the Jackson 2 binding gives exactly the same
functionality with the Jackson version the CMS already ships.

If the bundle is preferred anyway, it is a one-line change in `cms-core/pom.xml`
(`mcp` instead of `mcp-core` + `mcp-json-jackson2`) plus adapting the two
`JacksonMcpJsonMapper` / `DefaultJsonSchemaValidator` imports in `MCPServer`.

### 2.2 New transitive dependencies

| Dependency | Note |
| --- | --- |
| `io.projectreactor:reactor-core` | required by `mcp-core`; new in the CMS |
| `com.networknt:json-schema-validator` | required by `mcp-json-jackson2` (tool input schema validation) |
| `com.fasterxml.jackson.core:jackson-databind`, `jackson-annotations` | already present |
| `org.slf4j:slf4j-api` | already present (bridged to log4j2 via `log4j-slf4j2-impl`) |
| `jakarta.servlet:jakarta.servlet-api` | `provided` in the SDK, supplied by Jetty (ee10 / Servlet 6.0) |

The SDK is compiled against Servlet 6.1, but the servlet transports only use Servlet 6.0 API
(`getRequestURI`, `getHeader`, `getContentLengthLong`, `startAsync`, `sendError`, `setHeader`,
`setStatus`, `getWriter`, `setContentType`, `setCharacterEncoding`), so Jetty 12 **ee10** is fine.

---

## 3. Implementation

### 3.1 `com.gentics.contentnode.mcp.MCPServer` (cms-core)

A small static holder that lazily creates and owns the two SDK objects:

* **`HttpServletStreamableServerTransportProvider`** — the streamable HTTP transport of the SDK.
  This class *is* an `HttpServlet`, so it can be registered directly with Jetty.
* **`McpSyncServer`** — the protocol server built on top of that transport. This is the object
  the follow-up tickets will register tools/resources/prompts on.

```java
transportProvider = HttpServletStreamableServerTransportProvider.builder()
        .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
        .mcpEndpoint(path)
        .build();

server = McpServer.sync(transportProvider)
        .jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
        .jsonSchemaValidator(new DefaultJsonSchemaValidator())
        .serverInfo(SERVER_NAME, Main.getImplementationVersion())
        .instructions(SERVER_INSTRUCTIONS)
        .capabilities(ServerCapabilities.builder().tools(true).build())
        .build();
```

Public API:

| Method | Purpose |
| --- | --- |
| `isEnabled()` | reads `MCP_ENABLED` |
| `getPath()` | reads `MCP_PATH` |
| `getServlet()` | initializes on first call, returns the transport provider (an `HttpServlet`) |
| `getServer()` | `Optional<McpSyncServer>` — entry point for registering tools later |
| `shutdown()` | `closeGracefully()` on the server, resets the holder |

Notes on the implementation:

* **JSON mapper and schema validator are passed explicitly.** The SDK would otherwise resolve
  them through `McpJsonDefaults` / `ServiceLoader`. That works (the shade plugin is configured
  with `ServicesResourceTransformer`, so `META-INF/services` survives the uber-jar build), but
  passing them explicitly is deterministic and independent of what else ends up on the classpath.
* **The server is placed in `cms-core`, not in `cms-oss-server`.** The REST resource
  implementations that the follow-up tickets will annotate live in `cms-core`, so the MCP
  registration code needs to be reachable from there. `cms-oss-server` only mounts the servlet.
* `serverInfo` reports `Gentics CMS` plus the CMS version from
  `com.gentics.contentnode.rest.version.Main#getImplementationVersion()`.
* `capabilities(...tools(true)...)` already announces `listChanged` support for tools, so tools
  can be added at runtime later without a protocol change.

### 3.2 `OSSRunner`

```java
context.addServlet(servletHolder, "/rest/*");
context.addServlet(JmxServlet.class, "/jmx");

// add MCP Servlet
addMcpServlet(context);
```

```java
private static void addMcpServlet(ServletContextHandler context) {
    if (!MCPServer.isEnabled()) {
        NodeConfigRuntimeConfiguration.runtimeLog.info("MCP endpoint is disabled");
        return;
    }

    String path = MCPServer.getPath();
    ServletHolder mcpServletHolder = new ServletHolder(MCPServer.getServlet());
    mcpServletHolder.setAsyncSupported(true);
    context.addServlet(mcpServletHolder, path);

    NodeConfigRuntimeConfiguration.runtimeLog.info(String.format("Serving MCP endpoint at %s", path));
}
```

Two details that matter:

1. **`setAsyncSupported(true)` is mandatory.** The transport calls `request.startAsync()` for the
   SSE streams. The SDK class carries `@WebServlet(asyncSupported = true)`, but that annotation is
   not evaluated in an embedded Jetty setup, so it has to be set on the `ServletHolder`.
2. **Exact path mapping, not a prefix.** The transport itself checks
   `requestURI.endsWith(mcpEndpoint)` and answers `404` otherwise, so mapping `/mcp/*` would gain
   nothing. `MCP_PATH` is normalised to start with `/` and to have no trailing `/`.

`MCPServer.shutdown()` is called in the `finally` block of `OSSRunner.start()`, right before
`Initializer.get().shutdown()`.

---

## 4. Configuration

Added to `ConfigurationValue`, so all three CMS configuration mechanisms work:

| Setting | Env variable | System property | Config property | Default |
| --- | --- | --- | --- | --- |
| enable/disable endpoint | `MCP_ENABLED` | `com.gentics.contentnode.mcp.enabled` | `mcp.enabled` | `true` |
| endpoint path | `MCP_PATH` | `com.gentics.contentnode.mcp.path` | `mcp.path` | `/mcp` |

The default is *enabled*, because that is what the ticket asks for ("integrate … as servlet under
the path /mcp"). Disabling is a single environment variable if the endpoint should be off by
default in a release.

---

## 5. Testing

No network access was available in this session, so the change could **not be compiled or run**
here. The SDK API used above was verified against the tagged sources of
[`v2.0.1`](https://github.com/modelcontextprotocol/java-sdk/tree/v2.0.1).

Suggested manual verification:

```bash
mvn -pl cms-oss-bom,base-api,base-lib,cms-restapi,cms-api,cms-core,cms-oss-server -am -DskipTests install
# start the server, then:

curl -i -X POST http://localhost:8080/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{
        "protocolVersion":"2025-06-18",
        "capabilities":{},
        "clientInfo":{"name":"curl","version":"1.0"}}}'
```

Expected: `200`, an `Mcp-Session-Id` response header, and a result containing
`serverInfo: {name: "Gentics CMS", version: …}` and `capabilities.tools`.

Alternatively use the [MCP Inspector](https://github.com/modelcontextprotocol/inspector):

```bash
npx @modelcontextprotocol/inspector
# transport: Streamable HTTP, URL: http://localhost:8080/mcp
```

---

## 7. GPU-2665 — exposing REST endpoints as MCP tools

The first slice of the umbrella ticket: REST resource methods can now be marked as MCP tools with
new annotations, are picked up automatically at startup, and can take real (JSON-schema-backed)
arguments.

### 7.1 Changed and added files

| File | Change |
| --- | --- |
| `pom.xml` | new property `classgraph.version` = `4.8.194` |
| `cms-oss-bom/pom.xml` | dependency management entry for `io.github.classgraph:classgraph` |
| `cms-core/pom.xml` | dependency `classgraph` |
| `cms-restapi/…/rest/mcp/McpTool.java` | **new** — method-level annotation marking a tool |
| `cms-restapi/…/rest/mcp/McpToolParam.java` | **new** — parameter/field-level annotation describing a tool argument |
| `cms-core/…/mcp/McpToolRegistry.java` | **new** — classpath scan, input schema generation, argument binding, dispatch |
| `cms-core/…/rest/resource/impl/AdminResourceImpl.java` | `getActionLog` annotated with `@McpTool` |
| `cms-restapi/…/resource/parameter/PagingParameterBean.java` | `page`/`pageSize` fields annotated with `@McpToolParam` |
| `cms-restapi/…/resource/parameter/ActionLogParameterBean.java` | `user`/`action`/`type`/`objId`/`start`/`end` fields annotated with `@McpToolParam` |
| `cms-oss-server/…/server/OSSRunner.java` | calls `McpToolRegistry.scanAndRegister(...)` after mounting the MCP servlet |
| `cms-oss-changelog/…/entries/2026/09/8884.GPU-2665.enhancement` | **new** — changelog entry |

### 7.2 `@McpTool` / `@McpToolParam` — and why they live in cms-restapi, not cms-core

`@McpTool` (method-level) carries `name`, `title` and `description`. Every tool name must be lower
snake_case. If `name` is left empty (as on `getActionLog`), `McpToolRegistry` derives one as
`<resource>_<method>`, e.g. `AdminResourceImpl#getActionLog` becomes `admin_get_action_log` (the
class name has any trailing `ResourceImpl`/`Impl` stripped first, then both parts are converted
from camelCase to snake_case and joined). This - rather than the bare method name - is what keeps
tools from different resource classes from colliding, since many `*ResourceImpl` classes in this
codebase share method names like `list`/`get`/`create`. It matters because
`McpAsyncServer#addTool` (the underlying SDK call) does **not** reject a duplicate tool name — it
silently *replaces* the previously registered tool with the same name, only logging a warning
(`"Replace existing Tool with name '{}'"`, checked directly against the SDK source at the `v2.0.1`
tag). To avoid that turning into a tool silently disappearing without any error,
`McpToolRegistry.scanAndRegister` tracks every name it registers in that scan and rejects (logs and
skips, without aborting the rest of the scan) any tool whose final name - explicit or derived - is
not valid snake_case, or collides with one already registered.

`@McpToolParam` can target either a method parameter directly, or — the common case in this
codebase — a **field** of a parameter's type, since most REST resource methods take JAX-RS
`@BeanParam` parameter beans instead of plain scalar parameters (see 7.3). `name` is optional: on a
field it defaults to the field's own name (always reliable via reflection); on a parameter it
defaults to the parameter's name, which is only reliable if compiled with `-parameters` (this
project is not, see the plain `maven-compiler-plugin` config in the root `pom.xml` — no
`<parameters>true</parameters>`), so `name` should be set explicitly there.

Both annotations live in **cms-restapi**, not cms-core (where `MCPServer`/`McpToolRegistry` live).
Reason: `PagingParameterBean` and `ActionLogParameterBean` (the classes that need `@McpToolParam`
on their fields) are themselves in cms-restapi, and cms-restapi has **no dependency on cms-core** —
by design, so it stays usable standalone by REST clients (see the module graph in this repo's
`CLAUDE.md`). Defining the annotations in cms-core and then annotating cms-restapi classes with
them would have created a cyclic module dependency. cms-core already depends on cms-restapi (via
cms-api), so `AdminResourceImpl` and `McpToolRegistry` can use them without issue.

### 7.3 Input schema and argument binding — `McpToolRegistry`

`McpToolRegistry.scanAndRegister(McpSyncServer)` uses ClassGraph to scan
`com.gentics.contentnode.rest.resource.impl` (recursively) — deliberately the same package Jersey
itself scans via `jersey.config.server.provider.packages` in `RESTApplication` — for methods
annotated with `@McpTool`. For each parameter of such a method:

* if the parameter itself carries `@McpToolParam`, it becomes one scalar/enum/collection argument;
* otherwise, if the parameter's type declares `@McpToolParam` fields (e.g. `ActionLogParameterBean`),
  those fields become arguments, **flattened into the same top-level input schema** as the other
  parameter(s) of the method, and the bean is instantiated and populated from the call arguments
  before the method is invoked;
* otherwise the parameter is left out of the schema and simply instantiated with its no-arg
  constructor (JAX-RS defaults apply) — this is why `getActionLog`'s tool call still works even
  though only some of `PagingParameterBean`'s/`ActionLogParameterBean`'s fields are annotated.

Schema generation (`buildInputSchema`) maps Java types to a JSON schema `type` for strings, booleans,
numbers, enums (as a string `enum`) and collections thereof (as an `array`); anything else is left
without a `type` constraint. This is deliberately much smaller than e.g. the
`org.springaicommunity:mcp-annotations` project's victools-based generator (see the design
discussion in this ticket's history) — it covers everything currently used in this codebase's
parameter beans, without adding the victools/swagger-annotations dependency chain. Argument values
are converted from the call's raw JSON-derived values into the target type with a plain Jackson
`ObjectMapper.convertValue(...)`, which already handles numbers/strings/collections generically; no
new dependency needed (`jackson-databind` is already a cms-core dependency). Nested/complex bean
types are not specifically supported yet.

The call handler:

1. instantiates the declaring class with its no-arg constructor,
2. builds the method arguments as described above,
3. invokes the method via reflection,
4. serializes the return value to JSON (Jackson `ObjectMapper`) and wraps it in a
   `CallToolResult` text content block, or returns `isError(true)` with the exception message.

`OSSRunner.start()` calls `scanAndRegister` right after `addMcpServlet(context)`, via a new private
`registerMcpTools()` helper, so tools are only registered when the MCP endpoint itself is enabled
(`MCPServer.getServer()` is empty otherwise).

> **Update (§10):** `registerMcpTools()` no longer calls `scanAndRegister` - it now calls
> `ManualMcpTools.registerAll` instead. `McpToolRegistry`'s classpath scan (and the annotations it
> looks for) are kept in the codebase and still covered by their own tests, but are no longer
> invoked at startup. See §10 for why, and for how tools are registered now.

### 7.4 Scope of the first tool

`getActionLog` now takes optional paging (`page`, `pageSize`) and filter (`user`, `action`, `type`,
`objId`, `start`, `end`) arguments — all `required = false`, matching their JAX-RS defaults/optional
filtering semantics. What's still **not** supported by `McpToolRegistry` (see 7.3): nested/complex
bean types as a single argument, context/session injection into a tool method, structured/typed
output (`outputSchema`/`structuredContent` — results are always a single JSON text block), and
`ToolAnnotations`/`_meta`. These can be added incrementally to `McpToolRegistry` as soon as a tool
actually needs them.

### 7.5 Known limitation: no authentication/authorization (resolved, see §10)

> **This section describes the state as of GPU-2665/2667, before authentication was added. It is
> kept for historical context. See §10 for the current state**, including a still-open
> caveat: tools registered through the classpath scan described in this section (§7) are no
> longer invoked at all (§10.5) - the limitation below is only still relevant if that scan is ever
> re-enabled.

A tool call arrives on the `/mcp` servlet, entirely outside the Jersey request pipeline that
normally binds an authenticated CMS session to the thread and enforces `@RequiredPerm` via
`AuthorizationRequestFilter`. `McpToolRegistry` does neither. Concretely, for `getActionLog`
(annotated with `@RequiredPerm(TYPE_ADMIN, PERM_VIEW)` and `@RequiredPerm(TYPE_ACTIONLOG,
PERM_VIEW)`): the method opens its own transaction via `ContentNodeHelper.trx()`, which — with no
session bound to the calling thread — falls back to `new Trx()`, i.e. **no session, CMS "system
user" (id `1`)**. `PermHandler` grants the system user full permissions, so both `@RequiredPerm`
checks trivially pass; they are not actually being enforced against a real caller identity. Any
tool whose target method instead expects an already-open transaction (e.g. via
`TransactionManager.getCurrentTransaction()`) would fail when invoked this way.
This is only acceptable for a first, read-only, internal/dev-only tool. Before exposing tools that
should run with a specific, less-privileged CMS user's permissions, `McpToolRegistry` needs to open
a transaction for a real session (SID/API token) and re-run the `@RequiredPerm` checks itself —
see the "Authentication and authorization" point below, which this section does not yet resolve.

### 7.6 Tool naming and safety checks

Two checks were added after the first version of this section, once adding a *second* endpoint was
being planned:

* **Naming.** Every tool name must be lower snake_case. If `McpTool#name()` is left empty, one is
  derived as `<resource>_<method>` (`ResourceImpl`/`Impl` stripped from the class name first, e.g.
  `AdminResourceImpl#getActionLog` → `admin_get_action_log`) rather than the bare method name,
  because many `*ResourceImpl` classes in this codebase share method names like `list`/`get`. This
  matters because `McpAsyncServer#addTool` (checked against the SDK source at the `v2.0.1` tag)
  does **not** reject a duplicate tool name — it silently *replaces* the previously registered tool
  with the same name, only logging a warning. `McpToolRegistry.scanAndRegister` now tracks every
  name it registers in a scan and rejects (logs, skips just that one tool) anything invalid or
  colliding, so a naming mistake becomes a startup-time log message instead of a tool silently
  disappearing.
* **No JAX-RS `@Context` injection.** `McpToolRegistry` instantiates the resource class with a bare
  no-arg constructor, never through Jersey/HK2, so nothing populates a field/method/parameter
  annotated with `@Context` (e.g. `AbstractContentNodeResource`'s injected
  `HttpServletRequest`/`HttpServletResponse`/`ContainerRequestContext`, or a parameter like
  `FileResourceImpl#createSimple`'s `@Context HttpServletRequest`). `checkNoContextInjection` walks
  the declaring class's hierarchy and the tool method's own parameters for any `@Context` use and
  rejects (logs, skips) the tool if found, instead of registering something that would
  `NullPointerException` on first real call. This is a coarse, class-wide check (it does not
  analyze whether the specific tool method actually touches the injected member) — deliberately, to
  keep it simple and avoid false negatives. Of the 58 REST resource impl classes, only 6 use
  `@Context` at all, and only 3 extend `AbstractContentNodeResource`; `AdminResourceImpl` is not
  among them, so `getActionLog` is unaffected. This check only prevents a crash — it does **not**
  make `@Context`-dependent endpoints (mostly auth/upload/proxy-centric ones) work as MCP tools;
  that would additionally need real values to inject, which loops back into the still-unresolved
  authentication story below.

### 7.7 Unit tests, and a visibility gap they caught

`cms-core/src/test/java/com/gentics/contentnode/mcp/McpToolRegistryTest.java` unit-tests
`McpToolRegistry` directly, using small nested fixture "resource" classes (per this codebase's test
convention) rather than the real `com.gentics.contentnode.rest.resource.impl` package, plus one
characterization test against the real `AdminResourceImpl#getActionLog`. To make this practical,
`McpToolRegistry`'s internals were loosened from `private` to package-private, the inline
duplicate-name check was extracted into `checkNotDuplicate`, and `register(...)` was split into a
pure `buildToolSpecification(...)` (no `McpSyncServer` needed — its `callHandler()` can be invoked
directly with a hand-built `CallToolRequest` and a `null` exchange, since the handler never uses
it) plus the one-line `server.addTool(...)` call. `scanAndRegister` also gained a package-private
`(McpSyncServer, String packageName)` overload so tests can scan their own package instead of the
real one.

Writing these tests caught a real gap: **ClassGraph's `getAllClasses()`/method scan silently
excludes non-`public` classes and methods by default.** All of this codebase's REST resource impl
classes and their JAX-RS methods happen to be `public` today, so this had no effect on
`AdminResourceImpl#getActionLog` — but a future non-`public` `@McpTool` method (or its declaring
class) would otherwise have been silently invisible to the scan, with no error at all (worse than
the naming/`@Context` cases, which at least log something). Fixed by adding
`.ignoreClassVisibility()`/`.ignoreMethodVisibility()` to the `ClassGraph` configuration, plus
`setAccessible(true)` on every reflective constructor/method use in `invoke`/`buildMethodArguments`
(matching the field-level `setAccessible(true)` already used for `@McpToolParam` fields) — since
discovering a non-public method but then crashing on first real call with an
`IllegalAccessException` would have been worse than not finding it at all.

---

## 8. Open points / follow-ups

* **Authentication and authorization.** Resolved for the manually implemented tools - see §10.
  `/mcp` now extracts an API token/session cookie from the request and resolves it into a real
  CMS session before a tool runs (`AbstractMcpTool`); a tool without a resolvable session is
  rejected. `McpToolRegistry`'s classpath scan itself (§7.5) still doesn't do any of this, but is
  also no longer invoked (§10.5), so this is moot unless that scan is re-enabled for some tools.
* **Transport security.** Partially addressed - see §10.3. `CmsMcpSecurityValidator` covers
  credential *presence*, not `Origin` header validation, which is a distinct concern (CSRF-style
  cross-origin requests from a browser) and is **still open**. The transport's
  `ServerTransportSecurityValidator` also supports `Origin`/`Host` validation
  (`DefaultServerTransportSecurityValidator`); wiring that in (in addition to, or combined with,
  `CmsMcpSecurityValidator`) is a separate follow-up.
* **Configuration reload.** The MCP server is built once at startup. If it should react to
  `onReloadConfiguration()` (see `ServletContextHandlerService`), that has to be added. Still open.
* **Tool arguments beyond scalars/collections.** `McpToolRegistry` (§7.3) still does not support a
  nested/complex bean type (a field whose own type has further `@McpToolParam` fields) as a single
  argument — needed as soon as a tool requires it. Still open; not needed by any of the endpoints
  wired up in §9.
* **`@Context`-dependent endpoints.** No longer rejected outright at the class level — see §9.3.
  Still not supported for a tool method that actually needs a real `HttpServletRequest`/
  `HttpServletResponse` value (as opposed to one that merely lives on a `@Context`-using class but
  never reads it); that remains blocked, by design, until the authentication/authorization point
  above is resolved (a real request only becomes available once a real session is threaded
  through).
* **Documentation** in `cms-oss-doc` once the endpoint has user-visible functionality. Still open.

---

## 9. GPU-2667 — wiring 10 more endpoints, hardening the registry

To manually test the `@McpTool`/`@McpToolParam` annotations against realistic argument shapes,
10 endpoints were selected (from `Vorschlag=Ja` in the project's endpoint-selection spreadsheet)
spanning bare scalars, multiple `@PathParam`s, method-level `@QueryParam`s (not bean-flattened),
1–5 parameter beans combined in one call, `List`/`Set` collections, enums, and endpoints whose
declaring class relies on JAX-RS `@Context` injection: `ConstructResource#list` (both overloads),
`ConstructResource#list` (`/construct/list`), `ObjectPropertyResource#list`,
`TemplateResource#list`, `MarkupLanguageResource#list`, `PartTypeResource#list`,
`LanguageResource#get`, `PublishProtocolResource#get`, `InfoResource#getMaintenance`,
`FileResource#getFileUsageInfo` and `PageResource#render`.

Wiring these up against the real source (not just the endpoint spreadsheet) surfaced two gaps in
`McpToolRegistry` beyond "just add more annotations", both fixed here.

### 9.1 Argument name collisions within one tool

`buildInputSchema` used to flatten every annotated parameter/field into one shared `properties`
map keyed only by resolved name, with no check that a name wasn't already used earlier in the
same method — a later duplicate would silently overwrite the earlier property (and, in
`buildMethodArguments`, silently bind the wrong field on invocation). This was not theoretical at
the time: `TemplateResourceImpl#list` (`GET /template`) took a direct
`@QueryParam("nodeId") List<String> nodeIds` **and** `TemplateListParameterBean`, whose own field
was `@QueryParam("nodeId") Integer nodeId` — the same JAX-RS query key bound to two differently
typed targets, which JAX-RS itself allows. `buildInputSchema` now tracks resolved names as it
builds a method's schema (`checkNotDuplicateArgument`) and rejects (logs, skips) the whole tool if
two parameters/fields collide — mirroring how `checkNotDuplicate` already handles a duplicate
*tool* name. For the `TemplateResource#list` case, the bean field kept the name `nodeId`, and the
direct parameter was explicitly named `nodeIds`.

*(A later rebase changed `TemplateResourceImpl#list` to use `ReducedListParameterBean` instead of
`TemplateListParameterBean` — which has no `nodeId` field — so this specific collision no longer
occurs there today. The guard itself is still necessary for any future case shaped like it; it's
covered directly by the `DuplicateArgResource` fixture in `McpToolRegistryTest`, independent of
whether any real endpoint currently triggers it.)*

A related, simpler pitfall hit while wiring these endpoints: `ConstructResourceImpl` has two
overloaded `list` methods (`GET /construct` and `GET /construct/list`). `resolveToolName` derives
a name from `method.getName()` alone, with no parameter-signature disambiguation, so both would
derive to `construct_list` — `checkNotDuplicate` would then silently drop whichever is scanned
second. Both now have explicit, distinct `@McpTool#name()`s (`construct_list` and
`construct_list_for_page`). This isn't a registry bug to fix, just a naming pitfall to know about
whenever a resource class has overloaded methods.

### 9.2 `@Context`: per-method reachability analysis instead of a class-wide check

The original `checkNoContextInjection` (§7.6) rejected a tool if its declaring class's hierarchy
had **any** `@Context` field/method, regardless of whether the specific tool method touched it.
This blocked every `@McpTool` candidate on `FileResource`, `FolderResource`, `ImageResource`,
`PageResource` (via `AuthenticatedContentNodeResource` → `AbstractContentNodeResource`, which has
3 `@Context` fields plus a `@Context`-annotated setter) and `NodeResource` (extends
`AbstractContentNodeResource` directly) — the majority of the `Vorschlag=Ja` endpoints, including
`FileResource#getFileUsageInfo` and `PageResource#render` from this batch. Neither method's body
(nor its callees) actually touches those fields.

`checkNoContextInjection` now does two different things depending on what it finds:

* A `@Context` **parameter** on the tool method itself is still an unconditional, certain
  rejection — that always stays `null`, since the resource class is instantiated with a bare
  no-arg constructor, not through Jersey/HK2.
* A `@Context` **field** somewhere in the class hierarchy is only a rejection if a new
  `ContextUsageAnalyzer` (`com.gentics.contentnode.mcp`) finds that the tool method — or a method
  it (transitively) calls within the resource class's own hierarchy — actually reads it. It does
  this via static bytecode analysis (ASM's tree API, added as a new dependency alongside
  ClassGraph): starting from the tool method, it follows direct `invokevirtual`/`invokespecial`/
  `invokestatic`/`invokeinterface` call edges, scoped to the resource class and its superclass
  chain (that's where inherited helpers like `AbstractContentNodeResource#getRequest()` live), and
  scans each reachable method's bytecode for a `GETFIELD`/`PUTFIELD` against one of the class's
  `@Context` fields. `@Context`-annotated *methods* (JAX-RS setter-style injection, e.g.
  `AbstractContentNodeResource#setSessionSecretFromCookie`) are not part of this analysis at all —
  they're only ever invoked by the JAX-RS injection machinery itself, never reachable from a tool
  method's own code.

This is a heuristic, not a guarantee: the analysis only follows direct call instructions, not
method references, lambdas, or reflective calls. A tool that's allowed through despite its class
having `@Context` fields is logged at `WARN`, so a `NullPointerException` at runtime on such a
tool has a paper trail pointing back at "this was allowed by a heuristic analysis, re-check it."
On a bytecode-reading failure, the analysis conservatively treats the field as used (rejects the
tool) rather than silently letting it through unanalyzed.

Note what this does *not* solve: a tool method that actually **needs** a real
`HttpServletRequest`/`HttpServletResponse` (as opposed to merely living on a class that happens to
declare one) is still rejected — there's still no real value to give it. That's tied to the
still-open authentication/authorization point in §8.

### 9.3 Tests

`McpToolRegistryTest` gained fixtures/tests for: a `@Context` field the tool method never reads
(now allowed) vs. one it reads directly, or via a called helper method (both still rejected); a
`@Context`-annotated method alone, with no field (now allowed, whereas the old class-wide check
would have rejected it); two beans combined in one method whose fields collide on the same
argument name (rejected); overloaded methods deriving the same tool name by default; and a
`Set<Enum>` field (schema generation and argument conversion both already handled this generically
via the existing `Collection`/enum branches — no registry change needed, just test coverage, since
`ObjectPropertyParameterBean#types` is the first field of that shape). Two characterization tests
were added against the real, production-annotated `FileResourceImpl#getFileUsageInfo` and
`PageResourceImpl#render`, asserting they now register despite their class's `@Context` members.

---

## 10. GPU-2665 — authentication, and manual tool registration

Two things changed in this slice, both scoped to a new `page_load` tool used to prove the first
one actually works end to end:

1. `/mcp` tool calls are no longer unauthenticated (§7.5/§8's "Authentication and authorization"
   point). A tool call now resolves the same credentials the regular REST API accepts - an API
   token, or the CMS session cookie - into a real CMS session, and runs with that session bound,
   so permission checks the delegated-to code performs run against the actual caller, not the CMS
   system user.
2. Tools are now registered **manually**, one `McpToolProvider` implementation per tool, instead
   of (only) via `McpToolRegistry`'s `@McpTool` classpath scan. That scan is kept in the codebase
   - its tests still pass - but is no longer invoked at startup (§10.5).

### 10.1 Changed and added files

| File | Change |
| --- | --- |
| `cms-core/…/mcp/auth/McpRequestCredentials.java` | **new** — record carrying the credentials extracted from a request |
| `cms-core/…/mcp/auth/CmsMcpContextExtractor.java` | **new** — `McpTransportContextExtractor<HttpServletRequest>`, extracts the bearer token/session cookie |
| `cms-core/…/mcp/auth/McpAuthenticator.java` | **new** — resolves `McpRequestCredentials` into a real CMS `Session` |
| `cms-core/…/mcp/auth/McpSessionBinding.java` | **new** — binds/restores a `Session` on `ContentNodeHelper` for the duration of a tool call |
| `cms-core/…/mcp/auth/CmsMcpSecurityValidator.java` | **new** — optional, disabled-by-default `ServerTransportSecurityValidator` (HTTP 401 on a missing credential) |
| `cms-core/…/mcp/McpToolProvider.java` | **new** — interface for a manually implemented tool |
| `cms-core/…/mcp/AbstractMcpTool.java` | **new** — base class handling authentication/session-binding/result-serialization for a manual tool |
| `cms-core/…/mcp/ManualMcpTools.java` | **new** — explicit list of manual tools + registration on the `McpSyncServer` |
| `cms-core/…/mcp/tools/PageLoadTool.java` | **new** — first manual tool, delegates to `PageResourceImpl#load` |
| `cms-core/…/mcp/MCPServer.java` | wires `CmsMcpContextExtractor`/`CmsMcpSecurityValidator` into the transport provider builder |
| `cms-core/…/runtime/ConfigurationValue.java` | new value `MCP_REQUIRE_AUTH` |
| `cms-oss-server/…/server/OSSRunner.java` | `registerMcpTools()` now calls `ManualMcpTools.registerAll` instead of `McpToolRegistry.scanAndRegister` |
| `cms-oss-changelog/…/entries/2026/09/8943.GPU-2665.enhancement` | **new** — changelog entry |

Plus unit tests for every new class under `cms-core/src/test/java/com/gentics/contentnode/mcp/`
(`auth/` and `tools/` subpackages).

### 10.2 Credential extraction and resolution

`CmsMcpContextExtractor` runs on every incoming request (it is the transport's
`contextExtractor`, set once in `MCPServer.getServlet()`). It reads the same two credential
shapes `AuthenticationRequestFilter` accepts for `/rest/*` - an `Authorization: Bearer <token>`
header, and the `GCN_SESSION_SECRET` cookie - into a `McpRequestCredentials` record, and stores it
on the `McpTransportContext` under `McpRequestCredentials#CONTEXT_KEY`. It deliberately does
**no DB access and never throws**: it runs outside the transport's own try/catch (verified against
the `HttpServletStreamableServerTransportProvider` source at the `v2.0.1` tag), so an exception
here would surface to the client as a raw HTTP 500 instead of a clean MCP-level error.

`McpAuthenticator.resolve(McpRequestCredentials)` is the counterpart that *does* touch the DB - it
mirrors `AuthenticationRequestFilter#tryApiToken()`/`#trySessionSecretSession()` exactly (same
precedence: API token first, then session cookie; same lookups: `ApiTokenFactory#hash`/`#load` +
`ApiTokenSession`, respectively `SessionToken` + `DBSession#load` + `#touch()`), wrapped in its own
short-lived system-user `Trx` (`Trx.supply(...)`, the same pattern `ApiTokenSessionClosure` uses)
since that filter is bound to the Jersey pipeline and never runs for `/mcp`. Empty credentials
short-circuit before any DB access.

### 10.3 Where credentials are checked, and the two independent guards

There are two independent places an unauthenticated/invalid request can be rejected, and they
default to different behavior on purpose:

* **Per tool call, always on.** `AbstractMcpTool#call` resolves the session and, if
  `McpToolProvider#requiresAuthentication()` (default `true`) and no session was resolved, returns
  a `CallToolResult.isError(true)` without ever running the tool's own logic. This is the
  baseline: "most tool calls without authentication should fail." A tool that legitimately does
  *not* need an existing session (e.g. a future login tool that establishes one) overrides
  `requiresAuthentication()` to `false`.
* **At the transport level, opt-in, disabled by default.** `CmsMcpSecurityValidator` (the
  transport's `securityValidator`) rejects a request with **HTTP 401** if it carries *neither* an
  `Authorization: Bearer` header *nor* the session cookie - but only when
  `ConfigurationValue#MCP_REQUIRE_AUTH` is `true` (env `MCP_REQUIRE_AUTH`, system property
  `com.gentics.contentnode.mcp.requireAuth`, config `mcp.requireAuth`; default `false`, so
  existing setups - e.g. the MCP Inspector connecting without configuring any credentials at all -
  keep working unless this is explicitly opted into). **This only checks presence, not validity** -
  it runs before any transaction/DB access is available to the transport
  (`HttpServletStreamableServerTransportProvider#doPost`/`#doGet`/`#doDelete` call the security
  validator synchronously, ahead of session/DB handling), so an invalid or expired token/cookie
  still passes this check and is only caught by the per-tool-call guard above. Enabling the flag
  only makes a missing credential fail earlier and more explicitly (401 instead of a 200 response
  whose body is an MCP-level error) - it does not change what ultimately succeeds.

### 10.4 Session binding and thread-locals

A sync tool handler does not run on the servlet thread that received the HTTP request - the SDK
offloads it onto a pooled `Schedulers.boundedElastic()` thread (`McpServerFeatures
.AsyncToolSpecification#fromSync`, checked against the SDK source at the `v2.0.1` tag), and that
thread is reused across unrelated calls. `McpSessionBinding` (an `AutoCloseable`, always used in a
try-with-resources by `AbstractMcpTool#call`) binds the resolved session on `ContentNodeHelper` for
the duration of the call and restores whatever was bound before - the same session leak concern
`McpToolRegistry#invoke` already had to deal with for its fixed backend language ID (§7's
`invoke` Javadoc), generalized here to the session itself. If no session is bound (only possible
for a tool with `requiresAuthentication() == false`), the same fixed backend language ID (`2`) is
set instead, for the same reason `McpToolRegistry#invoke` sets it: without *some* language,
i18n-translated messages fall back to their raw, untranslated key.

### 10.5 Manual tool registration replaces the classpath scan

`McpToolProvider` (a tool's definition + call handler) and `AbstractMcpTool` (the base class that
does the authentication/session-binding/result-serialization described above, so a concrete tool
only implements `tool()` and `invoke(Map<String, Object>, Optional<Session>)`) replace
`McpToolRegistry`'s classpath scan as the way new tools get added. `ManualMcpTools` holds the
explicit list of tool instances and registers each of them on the `McpSyncServer`;
`OSSRunner.registerMcpTools()` now calls `ManualMcpTools.registerAll` instead of
`McpToolRegistry.scanAndRegister`.

`McpToolRegistry` itself (the scan, the `@McpTool`/`@McpToolParam` annotations, `ContextUsage
Analyzer`, and all of §7/§9's hardening) is **not deleted** - it still compiles, its tests still
pass, and the 12 REST methods already annotated with `@McpTool` are untouched - but none of it
runs anymore, so none of those 12 tools are exposed until/unless `scanAndRegister` is called again
from somewhere. This was a deliberate choice (see the project's planning discussion for this
ticket): keep the annotation-based approach available/dormant rather than deleting it outright,
in case some of those endpoints are migrated to manual tools individually later, but stop treating
it as the live registration path.

### 10.6 The `page_load` tool

`PageLoadTool` is deliberately minimal: it exists to prove that a tool call now runs as the real,
authenticated caller. It takes a required `id` and an optional `nodeId`, and delegates to
`PageResourceImpl#load` (with every other flag `false`/`null`) - the same method the real
`GET /rest/page/load/{id}` REST endpoint calls. That method's own object-permission check
(`ObjectPermission.view`, inside `PageResourceImpl#getPage`) is what actually proves the point:
with a real session bound (§10.4), it runs against that user's actual permissions - a user without
view permission on a page gets a permission error, not the page. No separate permission check was
added to the tool itself, since the delegated-to method already performs the relevant one; per
this ticket's direction, a tool is responsible only for whatever permission checks the code it
calls does *not* already perform on its own (there was none missing here).

### 10.7 Tests

Unit tests were added for every new class (`CmsMcpContextExtractorTest`,
`CmsMcpSecurityValidatorTest`, `McpSessionBindingTest`, `McpAuthenticatorTest`, `PageLoadToolTest`,
`ManualMcpToolsTest`), all runnable without a DB: credential extraction/parsing, the security
validator's enabled/disabled and presence-only behavior, session-binding restore-on-close
(including when the tool body throws), the empty-credentials short circuit, the tool's input
schema and its unauthenticated-call rejection, and that `ManualMcpTools.registerAll` registers
`page_load`. Resolving an actual, valid API token/session cookie against the database, and the
resulting permission check inside `PageResourceImpl#load`, needs a real CMS instance and is
covered by a manual test protocol instead - see `docs/mcp-tests.md`.

**How this was verified in-session, and what that does/doesn't prove.** The sandbox this code was
written in has no credentials for Gentics' internal Maven repository (`repo.gentics.com`), so a
real `mvn compile`/`test`/`install` of this reactor could not be run there - several dependencies
(e.g. `ojdbc7`, `generic-testutils`) only resolve with those credentials. Instead: a classpath was
rebuilt from this checkout's pre-existing `target/classes` directories (a prior successful build)
plus public dependency jars fetched from Maven Central, every new/changed file was compiled
directly with `javac` against that classpath, and the six unit test classes above were run
directly with `org.junit.runner.JUnitCore` - all passed. This is real signal (the code compiles
against the actual SDK/CMS types and the tests' assertions hold), but it is not the same guarantee
a real `mvn -pl cms-core -am test` run gives (Checkstyle/SpotBugs/PMD were not run; the reactor's
own dependency resolution was never exercised end-to-end). Per this repo's `CLAUDE.md`, builds and
tests are now run manually by the user rather than attempted in the sandbox going forward - treat
a user-reported `mvn` run as the actual ground truth for "this builds/passes", not the `javac`
check described here.

### 10.8 Manual testing tooling (MCP Inspector / Insomnia)

Two ways to drive `docs/mcp-tests.md` §9's manual protocol against a real, running server, beyond
plain `curl`:

* **MCP Inspector.** The current web UI (`clients/web` in the
  [Inspector repo](https://github.com/modelcontextprotocol/inspector)) configures a server's
  outgoing headers under its **"Custom Headers"** settings section (`+ Add Header`, then a
  `Key`/`Value` pair per row) - this covers both credential shapes: `Key: Authorization`,
  `Value: Bearer <api token>`, or `Key: Cookie`, `Value: GCN_SESSION_SECRET=<value>`. The `Cookie`
  case is worth calling out: a browser normally refuses to let JavaScript set a raw `Cookie`
  header at all (it's on the Fetch spec's forbidden-header-name list), which would otherwise make
  this untestable from a browser-based tool. Checked against Inspector's own source
  (`core/mcp/node/proxyFetch.ts`): the web client's outgoing request to the target MCP server is
  made by Inspector's own local Node backend (via `undici`), not the browser's `fetch`, so that
  restriction doesn't apply here.
* **Insomnia** (or any REST client with the same two features). Its per-workspace cookie jar
  auto-captures `Set-Cookie` from a `/rest/auth/login` response and auto-attaches matching cookies
  to later requests by default, so the session-cookie path needs no manual extraction once a login
  request has run. Its native **Response** template tag (attribute **Header**) can pull the
  `Mcp-Session-Id` value out of the `initialize` response directly into the `tools/call` request's
  header field, instead of copy-pasting it by hand.

Either way, the underlying protocol exchange is the same three calls `docs/mcp-tests.md` §9 and
§5's example walk through: log in (sets the session cookie) → `initialize` (returns
`Mcp-Session-Id`) → `tools/call` for `page_load` (needs that session ID plus the credential).

### 10.9 Still open

* **`Origin`/`Host` header validation** for the transport, as a defense against a browser making
  cross-origin requests to `/mcp` with the user's own session cookie attached - a different
  concern than `CmsMcpSecurityValidator`'s presence check (§10.3). See the updated §8 bullet.
* **Re-running `@RequiredPerm`** is intentionally **not** done automatically for a manual tool -
  each tool is responsible for whatever permission checks the code it delegates to does not
  already perform (§10.6). This is a deliberate scope decision for this ticket, not an oversight,
  but it does mean a future tool author has to actually verify that the call they delegate to
  performs the check they expect.
* **A tool with `requiresAuthentication() == false`** (e.g. a login tool) is supported by the
  `McpToolProvider` contract, but none exists yet.
* **Configuration reload / transport security (`Origin` validation) / documentation** - see §8,
  unchanged by this section.

## 11. GPU-2665 — `list_nodes` tool and shared `ObjectRef`

A second manual tool, `list_nodes`, plus `ObjectRef`, the first shared, cross-tool MCP output
type. The full design brief is `docs/plan-tool-list-nodes.md`. This section records what was
actually built, including where the implementation had to deviate from that brief (§11.3).

### 11.1 Changed and added files

| File | Change |
| --- | --- |
| `cms-core/…/mcp/model/ObjectRef.java` | **new** — shared object reference record (`type`/`id`/`globalId`/`nodeId`/`name`/`path`/`language`/`niceUrl`/`url`) |
| `cms-core/…/mcp/tools/ListNodesTool.java` | **new** — the `list_nodes` tool, delegates to `NodeResourceImpl#list` and `#languages` |
| `cms-core/…/mcp/ManualMcpTools.java` | registers `ListNodesTool` after `PageLoadTool` |
| `cms-core/…/mcp/AbstractMcpTool.java` | also sets `structuredContent` on the result when the tool declares an output schema (§11.3) |
| `cms-oss-changelog/…/entries/2026/09/8943.GPU-2665.enhancement` | **new** — changelog entry (§10.1 refers to an `8943` entry for the auth work, but no such file exists in the tree - `a898bc84f` deleted the earlier GPU-2665/2667 entries - so `8943` was the next free number after `8942`) |

Tests: `ObjectRefTest`, `ListNodesToolTest`, `AbstractMcpToolTest` (all new, no DB),
`ManualMcpToolsTest` (extended), and `tests/mcp/ListNodesToolIntegrationTest` (new,
`DBTestContext`).

### 11.2 Design decisions

* **`ObjectRef` lives in `com.gentics.contentnode.mcp.model` (cms-core)**, not in cms-restapi: it
  is not a JAX-RS DTO, only glue for the MCP tool layer, which lives entirely in cms-core. Its
  `Type` enum serializes to the spec's lowercase strings (`node`, `folder`, …) via
  `@JsonValue`/`@JsonCreator`. An unknown string fails with an `IllegalArgumentException` that
  lists the valid values, rather than silently becoming `null`. On input only `type`/`id` are
  meaningful. Unknown JSON fields are ignored (`@JsonIgnoreProperties(ignoreUnknown = true)`), so
  a ref emitted by one tool can be passed back unchanged. Unset fields are omitted on output
  (`@JsonInclude(NON_NULL)`, see §11.3). Only one factory exists so far, `forNode(...)`; factories
  for other object types are to be added by the tools that need them.
* **Languages are folded into every item** (`items[].languages`, `{id, code, name}`), fetched
  per node via `NodeResourceImpl#languages` in the same call. Node counts are small, so this is
  cheap even at `size = 200`, and the tool's stated purpose ("learn which languages a node
  supports") depends on the field. `LanguageInfo` was first a nested record of `ListNodesTool`;
  that decision was reversed on 2026-09-28, and it is now the shared `mcp.model.LanguageInfo`
  (§12.11).
* **Paging is "fetch all, then slice in memory".** The tool's input is offset-based (`from`,
  arbitrary in `[0, 10000]`), while `PagingParameterBean` is page-number based (`page`/`pageSize`).
  The two only line up when `from` is a multiple of `size`. So `NodeResourceImpl#list` is called
  unpaged (`pageSize = -1`, the bean's own default) and `[from, from + size)` is sliced out
  afterwards (`mcp.util.Slice`, §12.11, which also computes `truncated`/`nextFrom` and uses `long`
  arithmetic so `from + size` cannot overflow). `total` is always the exact count of visible,
  matching nodes, so `totalIsExact` is always `true`.
* **No extra permission check.** `NodeResourceImpl#list` already filters to
  `ObjectPermission.view` (`PermissionFilter`), and `#languages` re-checks `view` per node
  (`MiscUtils.getNode`). Same principle as `page_load` (§10.6).
* **`@Context` safety** was re-checked by hand at implementation time (the tool bypasses
  `ContextUsageAnalyzer`, like every manual tool). `list`/`languages` and everything they call use
  only `ContentNodeHelper`/`TransactionManager`/static `MiscUtils` helpers. None read the
  `@Context` fields declared on `AbstractContentNodeResource`.
* **No outer `Trx`.** Both resource methods open their own via `ContentNodeHelper.trx()`, like
  `PageResourceImpl#load` for `page_load`.
* Arguments: `q` (optional, max 200 chars, blank = no filter; matched case-insensitively against
  the node's ID and name by the resource's own `ResolvableFilter`), `size` (1–200, default 25),
  `from` (0–10000, default 0). All three are checked again in the tool (`mcp.util.ListArgs`,
  §12.11), which rejects out-of-range values, a `q` that is not a JSON string (or `null`), and a
  `size`/`from` that is not an integral JSON number (no numeric strings). Results keep the
  resource's own default sort (`name`).
* `host` is passed through exactly as the REST model reports it. That value includes the
  protocol, e.g. `http://www.example.com` (`ModelBuilder#getNode`), not the bare hostname.

### 11.3 Deviation from the plan: output schema requires structured content

The plan said no MCP infrastructure had to change for this tool. That turned out to be wrong for
the output schema it also asked for. In MCP Java SDK `2.0.1`, every registered tool's handler is
wrapped in `McpAsyncServer.StructuredOutputCallToolHandler` (checked against the `mcp-core-2.0.1`
sources jar). If the tool declares an `outputSchema` and a successful result has no
`structuredContent`, the result is replaced with an `isError(true)` "Response missing structured
content which is expected when calling tool with non-empty outputSchema". If structured content is
present, it is validated against the schema (networknt `json-schema-validator` via
`mcp-json-jackson2`'s `DefaultJsonSchemaValidator`).

`AbstractMcpTool#call` only produced a JSON text block, so as planned every successful
`list_nodes` call would have come back as an error. Two changes fix this:

1. `AbstractMcpTool#call` now also sets `structuredContent` (the same result, converted to a
   `Map` with the same `ObjectMapper`), **but only if `tool().outputSchema() != null`**. Tools
   without an output schema, i.e. `page_load`, are unaffected (the SDK would log a warning for
   structured content without a schema). The text block is still always sent, for clients that
   don't read structured content.
2. The result records (now `ListResult`, `NodeInfo`, `LanguageInfo`, §12.11) and `ObjectRef` are
   `@JsonInclude(NON_NULL)`. JSON Schema's `"type": "integer"` does not accept `null`, so an
   unset optional field (e.g. `nextFrom` on the last page, or a node without a default image
   folder) would otherwise fail validation.

`ListNodesToolTest` runs the declared output schema and a sample result through the SDK's own
`DefaultJsonSchemaValidator`, so a later schema/record mismatch shows up in a unit test instead of
at runtime.

### 11.4 Tests

* `ObjectRefTest`: `Type` wire values for all constants in both directions (incl.
  case-insensitive input), a clear failure for an unknown value, `forNode`/`of` populate exactly
  the expected fields, a full Jackson round trip with every field set and exactly the spec's JSON
  keys, unset fields omitted, unknown input fields ignored.
* `ListNodesToolTest`: tool name/description, input schema (`q`/`size`/`from` with their
  constraints, nothing required, no additional properties), output schema field names, output
  schema is a valid schema, a sample result validates against it, null fields are omitted,
  `requiresAuthentication()`, and unauthenticated-call rejection. The `Slice` math
  (first/middle/last/exact-end/beyond-total/empty/overflow, plus a loop that pages through a list
  via `nextFrom` and checks every item is covered exactly once) moved to `util/SliceTest`
  (§12.11).
* `AbstractMcpToolTest`: `structuredContent` set iff an output schema is declared, and the shared
  helpers (`errorMessage`, `requireOk`, the successful path of `saveOrReleaseLock`, the argument
  and schema delegates). The argument parsing and rejection tests are in `McpArgsTest`.
* `ManualMcpToolsTest`: `registerAll` also registers `list_nodes`.
* `tests/mcp/ListNodesToolIntegrationTest` (`DBTestContext`): a user whose group has *only* view
  permission on three of four freshly created nodes, authenticated via a real API token passed as
  `McpRequestCredentials`, calls `new ListNodesTool().call(...)` directly. Asserts that only the
  three visible nodes are listed, sorted by name, with their assigned languages in order; that
  `q` matching one node returns exactly that node, and `q` matching only the hidden node returns
  nothing; that paging with `size=2` across two calls is consistent and neither duplicates nor
  skips a node; and that `from` beyond `total` returns an empty page.

**Verification status.** Same approach and limits as §10.7. The four main-source files were
compiled with `javac --release 17` against the classpath rebuilt from the pre-existing
`target/classes` directories plus the local Maven repository jars. The four unit test classes
above plus `PageLoadToolTest` (34 tests) were run with `org.junit.runner.JUnitCore`, and all
passed. The existing `auth/` unit tests also still pass against the changed `AbstractMcpTool`.
The integration test was **compiled only, not run** (it needs the MariaDB/test-DB-manager
containers). Checkstyle/SpotBugs/PMD were not run. A user-run `mvn -pl cms-core -am test` remains
the ground truth.

### 11.5 Still open

* `ObjectRef` has only a `forNode` factory. Folder/page/file/etc. factories (and populating
  `nodeId`/`path`/`language`/`niceUrl`/`url`) come with the tools that need them.
* `get_folder_tree`, referenced by `list_nodes`' description, does not exist yet.
* `list_nodes` does not expose the resource's `sort`, `perms` or staging `package` parameters.
* The `ref` object in the output schema describes its fields but, like the rest of the MCP
  schemas here, does not use `$ref` or require any of them.

## 12. GPU-2665 — `update_page_properties` tool

The first **write** tool. It changes page metadata (name, filename, description, nice URL,
priority, template) without touching content/tags. The design brief is
`docs/plan-tool-update-page-properties.md` (revised 2026-09-25). This section records what was
built, including one deviation from that brief (§12.3).

### 12.1 Changed and added files

| File | Change |
| --- | --- |
| `cms-core/…/mcp/tools/UpdatePagePropertiesTool.java` | **new**: the tool, delegates to `PageResourceImpl#load` and `#save` |
| `cms-core/…/mcp/model/ObjectRef.java` | new factory `forPage(Page, Integer nodeId)` |
| `cms-core/…/mcp/ManualMcpTools.java` | registers `UpdatePagePropertiesTool` after `ListNodesTool` |
| `cms-oss-changelog/…/entries/2026/09/8944.GPU-2665.enhancement` | **new**: changelog entry (see §12.10 on the numbering) |

Tests: `UpdatePagePropertiesToolTest` (new, no DB), `ObjectRefTest` and `ManualMcpToolsTest`
(extended), `tests/mcp/UpdatePagePropertiesToolIntegrationTest` (new, `DBTestContext`).

No REST API was changed.

### 12.2 Load → save → reload, and why `save()` was not changed

The tool calls `PageResourceImpl#load(update = true)` (checks `ObjectPermission.edit`, locks the
page), then `#save` (with `unlock = true`), then `#load(update = false)` again for its output.
`save()` keeps returning `GenericResponse`. Changing it to return the saved page was implemented
and reverted (plan §2): the page instance inside `save()` has a stale lock state after
`unlock()`, so it needed a fresh fetch anyway; the new `page` field broke Java `RestClient`s
reading the response as `GenericResponse` (`UnrecognizedPropertyException`); and it made every
save more expensive, including the Aloha keepalive save
(`PageHandlingQueryCountTest#testUpdateTags` exceeded its statement budget). The reload in the
tool keeps that cost on MCP calls only.

Step 1 loads without any references (only the six mutable fields are needed for the snapshot).
Step 3 loads with `folder`, `langvars`, `translationstatus` and `versioninfo`, for the output.

### 12.3 Deviation from the plan: the save request contains only the supplied fields

The plan's sketch mutated the page returned by step 1 and submitted it to `save()`. That page
would carry more than the six fields: `ModelBuilder#getPage(restPage, false)` applies **every
non-null field** of the submitted REST page, and `load()` always fills content tags and visible
object tags, plus the translation status if requested. Submitting it would:

* re-save every tag, and require edit permission on every visible object tag
  (`MiscUtils.checkObjectTagEditPermissions`), although this tool must not touch tags;
* call `page.synchronizeWithPage(...)` whenever a translation status is present, i.e. silently
  change the page's translation sync state;
* make `deriveFileName` a no-op: `save()` only derives the filename if the submitted page's
  `fileName` is empty, and a loaded page always has one.

So the tool submits a fresh REST `Page` with only the supplied fields set (`UpdateRequest
#toRestPage`). Everything else stays `null` and is left unchanged by `ModelBuilder#getPage`. This
is the same shape existing REST tests use for page saves (e.g. `FilenameUniquenessTest`).
Consequences of `ModelBuilder#getPage`'s null/empty handling, documented in the input schema:

* `description: ""` and `niceUrl: ""` clear the value.
* `fileName: ""` does nothing by itself (empty filenames are ignored), but it lets
  `deriveFileName` apply, same as omitting `fileName`.
* `niceUrl` is ignored if the `NICE_URLS` feature is off. It then never shows up in
  `changedFields`.

### 12.4 `changedFields`: before/after snapshot diff

`changedFields` lists the fields whose value **actually differs** after the save, not the fields
present in the request (plan decision 3). The tool's `CHANGED_FIELDS` (a `ChangedFields`, §12.11)
takes a snapshot of `name`/`fileName`/`description`/`niceUrl`/`priority`/`templateId` of the page
loaded in step 1, and `diff(...)` compares them (`Objects.equals`) with the page reloaded in
step 3, in that fixed order. Diffing
around the whole cycle instead of bookkeeping while applying the arguments also catches changes
`save()` makes on its own: a filename derived via `deriveFileName`, or the template's file
extension appended to a given `fileName`. A resubmitted unchanged value is not reported.

### 12.5 Releasing the lock on failure

`load(update = true)` commits the lock. `save()` only unlocks at its very end, so every early
return (`INVALIDDATA` for a duplicate name, filename or nice URL) and every exception would leave
the page locked for the lock timeout (`lock_time`, default 600 s). The tool therefore:

* runs the save through `AbstractMcpTool#saveOrReleaseLock` (§12.11), which checks `save()`'s
  response code (`requireOk`) and turns anything but `OK` into a tool error, with the response's
  messages (`save()` reports duplicates in the response, it does not throw);
* if the save throws or is not `OK`, `saveOrReleaseLock` calls `releaseLock(Page.class, id)`.
  `releaseLock` opens a `Trx` for the bound session and calls `Page#unlock()`, whose
  `UPDATE content … WHERE id = ? AND locked_by = ?` only ever releases the caller's own lock. It
  deliberately does **not** use `PageResourceImpl#cancel`, which restores the latest page version
  before unlocking. A failure in `releaseLock` is logged and does not replace the original error.

Argument validation runs **before** step 1 (`UpdateRequest.of`), so an invalid argument never
locks the page in the first place.

Known edge case, same as the REST `save(unlock = true)`: if the calling user already had the page
locked before the call (e.g. open in the editor under the same account), the tool releases that
lock too, on success and on failure. If the page is locked by **another** user, step 1 does not
lock it (`load()` returns it read-only with a warning), `save()` then fails with the "locked by"
error, and `releaseLock` leaves the other user's lock alone.

### 12.6 `nodeId` is rejected

The input schema declares `nodeId` (to match the external tool catalog), but a call that supplies
it is rejected with "update_page_properties does not yet support nodeId (multichannelling); …",
before any argument validation or CMS access. Ignoring an explicit channel on a write could edit
the wrong page variant without the caller noticing. This refinement of plan decision 4 was
confirmed on 2026-09-25, before implementing it.

### 12.7 Input validation

The plan asked to verify whether the SDK enforces the input schema. It does. In MCP Java SDK
`2.0.1` (checked against the `mcp-core-2.0.1` sources jar), `McpAsyncServer`'s tool-call handler
runs `ToolInputValidator.validate(...)` before the tool's handler, unless the server was built
with `validateToolInputs(false)`. The builder default is `true`, and `MCPServer` does not change
it. `DefaultJsonSchemaValidator` was then run on this tool's constraint types in isolation
(scratch program against the cached jars): `minimum`/`maximum`, `minLength`/`maxLength`,
`required`, `additionalProperties: false`, wrong JSON types and `null` values were all rejected
with "Tool (…) input validation failed: …". `UpdatePagePropertiesToolTest
#testSdkInputValidationEnforcesConstraints` pins this down for the real schema.

The tool still repeats every check itself (`UpdateRequest.of`), because `AbstractMcpTool#call`
can be invoked without the SDK (as the tests do). Like all tools using the argument helpers of
`AbstractMcpTool`, it **rejects** out-of-range values instead of silently adjusting them, and
requires real JSON integers/booleans/strings (no numeric strings).

### 12.8 Output

`{ page, changedFields }`, both required. `page` is a `PageInfo` record (`mcp.model`, §12.11),
mapped from the reloaded REST `Page`. It has **no `tags` field** (plan decision 2). Details
resolved at implementation time:

* **`ref`** comes from `ObjectRef.forPage(page)`, which takes `nodeId` from the page's folder. `path` is `Page#getPath()`, which
  `ModelBuilder` always fills with `ModelBuilder#getFolderPath(folder)` (e.g. `/Node/News/2026/`),
  so no new path logic was needed. `nodeId` is the page's folder's `nodeId` (`Reference.FOLDER`
  is requested in step 3). `url` is the preview URL, falling back to `liveUrl` if blank.
* **`translationStatus`** is `Page#getTranslationStatus()`, a
  `com.gentics.contentnode.rest.model.TranslationStatus`, mapped 1:1: `pageId`, `name`,
  `language`, `inSync`, `version`, `versionTimestamp`, `latestVersion {version,
  versionTimestamp}`. For a page not synchronized with another variant, `ModelBuilder
  #getTranslationStatus` sets only `inSync: true`.
* **`languageVariants`** is keyed by **language code**, not by the REST model's map key (the
  numeric language ID), with an `ObjectRef` per variant. The page itself is included, as in the
  REST model.
* Users (`lockedBy`, `creator`, `editor`, `publisher`, version `editor`) are `{id, login}`.
  `versions` is newest first, as in the REST model.
* Timestamps that are unset in the REST model (`lockedSince = -1`, `pdate = 0` for a page never
  published) are omitted rather than reported as `-1`/`0`.

Like `list_nodes` (§11.3), all records are `@JsonInclude(NON_NULL)` so that the structured result
validates against the output schema.

### 12.9 Tests

* `UpdatePagePropertiesToolTest` (no DB): tool name/description and every input property's type
  and constraints; the SDK's own input validation against the real schema (valid call accepted, 13
  invalid shapes rejected); unauthenticated-call rejection; `nodeId` rejected before any CMS access
  (no transaction exists in the test, so reaching `load()` would fail differently; the same message
  comes back even with an otherwise invalid `pageId`); `UpdateRequest` defaults, every field, every
  rejection, boundary values, and that `toRestPage()` leaves tags/translation status/language
  unset; `CHANGED_FIELDS` snapshot diff for no change, each field individually, all fields in order,
  a resubmitted unchanged value, a filename changed by `save()`, `null` ↔ value in both directions,
  and `""` vs. `null`; the full output mapping; a full
  and a minimal result validated against the output schema with `DefaultJsonSchemaValidator`.
* `ObjectRefTest`: `forPage` with all fields, and the `liveUrl` fallback.
* `ManualMcpToolsTest`: `registerAll` also registers `update_page_properties`.
* `tests/mcp/UpdatePagePropertiesToolIntegrationTest` (`DBTestContext`): an editor (page view +
  update on the test folder) and a viewer (page view only), each with a real API token, call
  `new UpdatePagePropertiesTool().call(...)` directly. Every test uses its own pages and checks the
  page state independently of the tool (object layer, and the `content.locked` column for the
  lock). Cases: name only (`changedFields == ["name"]`, only the name changed, unlocked); the same
  name again (`changedFields == []`, nothing changed); description + priority; template change to
  a second template; `deriveFileName` (`["name", "fileName"]`); duplicate name and duplicate
  filename (error, page **unlocked** and unchanged); viewer rejected (unchanged, unlocked);
  `nodeId` rejected (unchanged, unlocked); a page locked by another user (error "Could not
  lock…", and that user's lock is **kept**, §12.5); unknown page ID. Every successful result is
  also validated against the output schema, since the direct call bypasses the SDK's check.
  Note for fixtures: a newly created page is locked by its creator (`PageFactory
  #saveContentObject` inserts `content` with `locked`/`locked_by` set), so the test unlocks each
  page after `createPage` (as the system user). The first run of this test lacked that, and
  every editor call failed with "Could not lock … locked for user {1}".

**Verification status.** The changed main sources and all new/changed test classes were compiled
with `javac --release 17` against the pre-existing `target/classes` directories plus the local
Maven repository jars. `UpdatePagePropertiesToolTest`, `ObjectRefTest`, `ManualMcpToolsTest`,
`ListNodesToolTest`, `PageLoadToolTest` and `AbstractMcpToolTest` (60 tests) were run with
`org.junit.runner.JUnitCore`, and all passed. The integration test was **compiled only, not run**
(it needs the MariaDB/test-DB-manager containers), so the DB-backed expectations, in particular
the exact `changedFields` after a real save and the lock release after a rejected save, are
unconfirmed until it runs. Checkstyle/SpotBugs/PMD were not run. A user-run
`mvn -pl cms-core -am test` remains the ground truth.

### 12.10 Still open

* Multichannelling: `nodeId` is rejected (§12.6).
* Tag content is out of scope, for a future `update_page_tags`. `get_template` and
  `update_page_tags`, referenced by the tool description, do not exist yet.
* The tool description is **not** verbatim from the external tool-catalog spec (which is not in
  this repository). Only the "Do NOT use this tool to change content, that is `update_page_tags`"
  sentence quoted in the plan is. Replace it with the spec's text if they differ.
* `alternateUrls`, `customCdate`/`customEdate`, language, and publish/offline times are not
  exposed.
* Changelog numbering: `8943` is already used by `8943.SUP-20182.bugfix` (commit `6c4f88666`
  on `origin/hotfix-6.4.x-sup-20182`, not on this branch). The `8943.GPU-2665.enhancement` that
  §11.1 mentions for `list_nodes` does not exist in the working tree. This tool's entry is
  `8944`. The `list_nodes` entry still needs to be created, under a free number.
* `idempotencyKey` is only logged. Calls are not de-duplicated.

### 12.11 Shared building blocks

Pieces of `update_page_properties` and `list_nodes` that other tools will need were moved out of
the tools, so that future tools reuse them instead of copying them:

| Where | What |
|---|---|
| `mcp/AbstractMcpTool` | `protected static` helpers: `errorMessage(failure, response)`, `requireOk(response, failure)`; `saveOrReleaseLock(clazz, id, failure, save)` and `releaseLock(clazz, id)`; delegates `stringArg`/`intArg`/`booleanArg`/`nullIfBlank` (to `McpArgs`) and `schema`/`objectRefSchema` (to `McpSchemas`/`ObjectRef`) |
| `mcp/McpArgs` | public argument parsing, usable outside tool classes (e.g. `ListArgs`): `stringArg`, `intArg` (strict, with and without default), `booleanArg`, `nullIfBlank` |
| `mcp/McpSchemas` | public `schema(type, description, keyValues...)`, usable outside tool classes (the model records) |
| `mcp/model` | public output records, each with a static `jsonSchema(...)` next to it: `ObjectRef` (plus `forPage(Page)`, node ID from the folder), `UserRef`, `VersionInfo` (maps any `ItemVersion`, not only `PageVersion`), `TranslationStatusInfo` with nested `LatestVersionInfo`, `PageInfo` (was `UpdatePagePropertiesTool.PageProperties`), `LanguageInfo` (maps a REST `ContentLanguage`), `NodeInfo` (was `ListNodesTool.NodeListItem`), generic `ListResult<T>` with `of(Slice, items)` and `jsonSchema(itemSchema, plural)` (was `ListNodesTool.ListNodesResult`); `Timestamps.orNull` |
| `mcp/util/ChangedFields` | generic `changedFields` support: a named list of getters, `snapshot(before).diff(after)` (was `UpdatePagePropertiesTool.MutableFieldsSnapshot`) |
| `mcp/util/Slice` | offset-based page `[from, from + size)` of a fully fetched list, with `apply(list)`, `truncated()`, `nextFrom()` (was `ListNodesTool.Slice`) |
| `mcp/util/ListArgs` | the list arguments `q`/`size`/`from`: `of(arguments, limits)`, `schemaProperties(queryDescription, singular, plural, limits)`, `slice(total)`; the limits (`ListArgs.Limits`) are per tool |
| `mcp/util/ListResponses` | `items(response)`: the items of any REST `AbstractListResponse`, never null |

`releaseLock` delegates to `NodeObject#unlock()`. That was checked for the types that implement
it: pages (`PageFactory`, `UPDATE content … WHERE id = ? AND locked_by = ?`) and templates
(`TemplateFactory#unlock(int, int)`, `… WHERE id = ? AND locked_by = ?`) only clear the caller's
own lock; forms (`FormFactory`) throw `ReadOnlyException` if another user holds the lock, which
`releaseLock` logs. For most other object types, `unlock()` is the empty implementation in
`AbstractContentObject`, so `releaseLock` does nothing for them.

`list_nodes` now parses `q` with the strict `stringArg` (via `ListArgs`) instead of its former
lenient `queryArg`, which converted any value with `String.valueOf`. A non-string or `null` `q` is
therefore rejected when `call()` is invoked directly; through the MCP server the SDK already
rejected it against `"type": "string"`. A blank `q` still means "no filter". `list()` and
`languages()` of `NodeResourceImpl` throw on failure instead of returning a non-`OK` response, so
`list_nodes` does not need `requireOk`.

The output schemas of the model records are still hand-built. `ModelJsonSchemaTest` checks for
each record that its schema describes exactly its record components and is a valid schema, so a
component added without updating the schema fails the build.

Tests: `McpSchemasTest`, `McpArgsTest`, `util/ChangedFieldsTest`, `util/SliceTest`,
`util/ListArgsTest`, `util/ListResponsesTest`, `model/ModelJsonSchemaTest` (new);
`ObjectRefTest` (`forPage(Page)`, `jsonSchema` description), `AbstractMcpToolTest` (`requireOk`,
`saveOrReleaseLock` success path). Releasing the lock after a failed save needs a DB and is
covered by `UpdatePagePropertiesToolIntegrationTest` (duplicate name/filename leave the page
unlocked).
