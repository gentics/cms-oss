# MCP Server Integration (GPU-2665)

Integration of the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) into the
Gentics CMS OSS server, so that the CMS can act as an MCP (Model Context Protocol) server.

| Ticket | Scope | Status |
| --- | --- | --- |
| **GPU-2665** | **Umbrella story: integrate MCP server, expose CMS resources as MCP endpoints via annotations** | **first tool implemented (§7)** |
| GPU-2666 | Integrate the MCP server as a servlet under `/mcp` | implemented |
| GPU-2667 | (follow-up) | open |
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

### 7.4 Scope of the first tool

`getActionLog` now takes optional paging (`page`, `pageSize`) and filter (`user`, `action`, `type`,
`objId`, `start`, `end`) arguments — all `required = false`, matching their JAX-RS defaults/optional
filtering semantics. What's still **not** supported by `McpToolRegistry` (see 7.3): nested/complex
bean types as a single argument, context/session injection into a tool method, structured/typed
output (`outputSchema`/`structuredContent` — results are always a single JSON text block), and
`ToolAnnotations`/`_meta`. These can be added incrementally to `McpToolRegistry` as soon as a tool
actually needs them.

### 7.5 Known limitation: no authentication/authorization

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

* **Authentication and authorization.** `/mcp` is currently unauthenticated — the CMS session
  filter is only mapped to `/rest/*`, and `McpToolRegistry` (§7.5) does not bind or check a CMS
  session/permissions either. Before any tool touches CMS data as a real user (GPU-2667 and
  later), the endpoint needs to be tied to the CMS authentication (SID / API token), e.g. through
  the transport's `contextExtractor` (`McpTransportContextExtractor<HttpServletRequest>`), which
  can hand the CMS session down into the tool handlers, which `McpToolRegistry` would then use to
  open a `Trx` for that session/user and re-run the method's `@RequiredPerm` checks before invoking
  it.
* **Transport security.** The transport provider accepts a `ServerTransportSecurityValidator`
  (default: NOOP). `Origin` header validation should be considered, since the CMS is a browser
  facing application.
* **Configuration reload.** The MCP server is built once at startup. If it should react to
  `onReloadConfiguration()` (see `ServletContextHandlerService`), that has to be added.
* **Tool arguments beyond scalars/collections.** `McpToolRegistry` (7.3) does not yet support a
  nested/complex bean type as a single argument — needed as soon as a tool requires it.
* **`@Context`-dependent endpoints.** Currently rejected outright at registration time (§7.6), not
  supported — see that section for why a generic fix is likely not worth building speculatively.
* **Documentation** in `cms-oss-doc` once the endpoint has user-visible functionality.
