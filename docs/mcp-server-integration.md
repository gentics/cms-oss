# MCP Server Integration (GPU-2665)

Integration of the [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) into the
Gentics CMS OSS server, so that the CMS can act as an MCP (Model Context Protocol) server.

| Ticket | Scope | Status |
| --- | --- | --- |
| GPU-2665 | Umbrella story: integrate MCP server, expose CMS resources as MCP endpoints via annotations | in progress |
| **GPU-2666** | **Integrate the MCP server as a servlet under `/mcp`** | **implemented (this document)** |
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

## 6. Open points / follow-ups

* **Authentication and authorization.** `/mcp` is currently unauthenticated — the CMS session
  filter is only mapped to `/rest/*`. Before any tool touches CMS data (GPU-2667 and later), the
  endpoint needs to be tied to the CMS authentication (SID / API token), e.g. through the
  transport's `contextExtractor` (`McpTransportContextExtractor<HttpServletRequest>`), which can
  hand the CMS session down into the tool handlers.
* **Transport security.** The transport provider accepts a `ServerTransportSecurityValidator`
  (default: NOOP). `Origin` header validation should be considered, since the CMS is a browser
  facing application.
* **Configuration reload.** The MCP server is built once at startup. If it should react to
  `onReloadConfiguration()` (see `ServletContextHandlerService`), that has to be added.
* **Tool registration from resource annotations** (the actual goal of GPU-2665) —
  `MCPServer.getServer()` is the hook for it.
* **Documentation** in `cms-oss-doc` once the endpoint has user-visible functionality.
