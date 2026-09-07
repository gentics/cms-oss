package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.rest.mcp.McpTool;
import com.gentics.contentnode.rest.mcp.McpToolParam;

import jakarta.ws.rs.core.Context;

import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson2.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link McpToolRegistry}.
 *
 * <p>
 * Uses small, self-contained fixture "resource" classes (nested static classes, per this
 * codebase's test convention) instead of scanning/registering against the real
 * {@code com.gentics.contentnode.rest.resource.impl} package, except for
 * {@link #testScanAndRegisterRealAdminResource()}, which deliberately exercises the real,
 * production-annotated {@code AdminResourceImpl#getActionLog} as a characterization/regression
 * check.
 * </p>
 */
public class McpToolRegistryTest {

	// ---------------------------------------------------------------------------------------
	// Fixtures
	// ---------------------------------------------------------------------------------------

	enum Mode {
		FAST, SLOW
	}

	static class FilterBean {
		@McpToolParam(description = "name filter", required = false)
		public String name;

		@McpToolParam(description = "count", required = true)
		public int count;

		@McpToolParam(description = "tags", required = false)
		public Set<String> tags;

		@McpToolParam(description = "mode", required = false)
		public Mode mode;

		/** Deliberately not annotated - must never show up in the schema or be populated. */
		public String notExposed;
	}

	static class SearchResult {
		public String query;
		public String name;
		public int count;
		public Set<String> tags;
		public Mode mode;
	}

	static class SearchResource {
		@McpTool(name = "search_tool", description = "search tool")
		public SearchResult search(@McpToolParam(name = "query", description = "search text") String query, FilterBean filter) {
			SearchResult result = new SearchResult();
			result.query = query;
			result.name = filter.name;
			result.count = filter.count;
			result.tags = filter.tags;
			result.mode = filter.mode;
			return result;
		}
	}

	static class FailingResource {
		@McpTool(name = "failing_tool", description = "always fails")
		public String fail() throws Exception {
			throw new IllegalStateException("boom");
		}
	}

	static class CleanResource {
		@McpTool(name = "clean_tool", description = "clean tool")
		public String doStuff() {
			return "ok";
		}
	}

	static class FieldContextResource {
		@Context
		private Object request;

		@McpTool(description = "uses context field")
		public String doStuff() {
			return "ok";
		}
	}

	static class MethodContextResource {
		@Context
		public void setContext(Object ctx) {
		}

		@McpTool(description = "uses context method")
		public String doStuff() {
			return "ok";
		}
	}

	static class ParamContextResource {
		@McpTool(description = "uses context param")
		public String doStuff(@Context Object ctx) {
			return "ok";
		}
	}

	static class DuplicateOneResource {
		@McpTool(name = "duplicate_tool", description = "first")
		public String run() {
			return "one";
		}
	}

	static class DuplicateTwoResource {
		@McpTool(name = "duplicate_tool", description = "second")
		public String run() {
			return "two";
		}
	}

	static class FooResourceImpl {
	}

	static class BarImpl {
	}

	static class BazResource {
	}

	static class ExplicitNameResource {
		@McpTool(name = "custom_name", description = "explicit name")
		public void run() {
		}
	}

	static class InvalidNameResource {
		@McpTool(name = "InvalidName", description = "not snake_case")
		public void run() {
		}
	}

	static class DefaultNameResource {
		@McpTool(description = "no explicit name")
		public void doThing() {
		}
	}

	/** No-op transport provider, just enough to build a working {@link McpSyncServer} in a test. */
	private static class NoOpTransportProvider implements McpServerTransportProvider {
		@Override
		public void setSessionFactory(McpServerSession.Factory sessionFactory) {
		}

		@Override
		public Mono<Void> notifyClients(String method, Object params) {
			return Mono.empty();
		}

		@Override
		public Mono<Void> closeGracefully() {
			return Mono.empty();
		}
	}

	private static McpSyncServer newTestServer() {
		return McpServer.sync(new NoOpTransportProvider())
				.jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
				.jsonSchemaValidator(new DefaultJsonSchemaValidator())
				.serverInfo("Test Server", "1.0")
				.capabilities(ServerCapabilities.builder().tools(true).build())
				.build();
	}

	private static String textOf(CallToolResult result) {
		return ((TextContent) result.content().get(0)).text();
	}

	// ---------------------------------------------------------------------------------------
	// toSnakeCase / resourcePrefix / resolveToolName
	// ---------------------------------------------------------------------------------------

	@Test
	public void testToSnakeCase() {
		assertThat(McpToolRegistry.toSnakeCase("getActionLog")).isEqualTo("get_action_log");
		assertThat(McpToolRegistry.toSnakeCase("list")).isEqualTo("list");

		// Documents current behavior for acronym-like identifiers (not necessarily desired, but
		// nothing in this codebase currently produces one) - every uppercase letter gets its own
		// underscore, including consecutive ones.
		assertThat(McpToolRegistry.toSnakeCase("getURLPath")).isEqualTo("get_u_r_l_path");
	}

	@Test
	public void testResourcePrefix() {
		assertThat(McpToolRegistry.resourcePrefix(FooResourceImpl.class)).isEqualTo("foo");
		assertThat(McpToolRegistry.resourcePrefix(BarImpl.class)).isEqualTo("bar");
		assertThat(McpToolRegistry.resourcePrefix(BazResource.class)).isEqualTo("baz_resource");
	}

	@Test
	public void testResolveToolName_explicitNameWins() throws Exception {
		Method method = ExplicitNameResource.class.getDeclaredMethod("run");
		assertThat(McpToolRegistry.resolveToolName(ExplicitNameResource.class, method)).isEqualTo("custom_name");
	}

	@Test
	public void testResolveToolName_derivesDefault() throws Exception {
		Method method = DefaultNameResource.class.getDeclaredMethod("doThing");
		assertThat(McpToolRegistry.resolveToolName(DefaultNameResource.class, method))
				.isEqualTo("default_name_resource_do_thing");
	}

	@Test
	public void testResolveToolName_invalidNameThrows() throws Exception {
		Method method = InvalidNameResource.class.getDeclaredMethod("run");
		assertThatThrownBy(() -> McpToolRegistry.resolveToolName(InvalidNameResource.class, method))
				.isInstanceOf(IllegalStateException.class);
	}

	// ---------------------------------------------------------------------------------------
	// checkNotDuplicate
	// ---------------------------------------------------------------------------------------

	@Test
	public void testCheckNotDuplicate() {
		Set<String> registeredNames = new HashSet<>();

		McpToolRegistry.checkNotDuplicate(registeredNames, "foo");
		McpToolRegistry.checkNotDuplicate(registeredNames, "bar");
		assertThat(registeredNames).containsExactlyInAnyOrder("foo", "bar");

		assertThatThrownBy(() -> McpToolRegistry.checkNotDuplicate(registeredNames, "foo"))
				.isInstanceOf(IllegalStateException.class);
	}

	// ---------------------------------------------------------------------------------------
	// checkNoContextInjection
	// ---------------------------------------------------------------------------------------

	@Test
	public void testCheckNoContextInjection_clean() throws Exception {
		Method method = CleanResource.class.getDeclaredMethod("doStuff");
		// must not throw
		McpToolRegistry.checkNoContextInjection(CleanResource.class, method);
	}

	@Test
	public void testCheckNoContextInjection_field() throws Exception {
		Method method = FieldContextResource.class.getDeclaredMethod("doStuff");
		assertThatThrownBy(() -> McpToolRegistry.checkNoContextInjection(FieldContextResource.class, method))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("field")
				.hasMessageContaining("request");
	}

	@Test
	public void testCheckNoContextInjection_method() throws Exception {
		Method method = MethodContextResource.class.getDeclaredMethod("doStuff");
		assertThatThrownBy(() -> McpToolRegistry.checkNoContextInjection(MethodContextResource.class, method))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("setContext");
	}

	@Test
	public void testCheckNoContextInjection_parameter() throws Exception {
		Method method = ParamContextResource.class.getDeclaredMethod("doStuff", Object.class);
		assertThatThrownBy(() -> McpToolRegistry.checkNoContextInjection(ParamContextResource.class, method))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("parameter");
	}

	// ---------------------------------------------------------------------------------------
	// buildInputSchema / typeToSchema
	// ---------------------------------------------------------------------------------------

	@Test
	public void testTypeToSchema() {
		assertThat(McpToolRegistry.typeToSchema(String.class, String.class)).containsEntry("type", "string");
		assertThat(McpToolRegistry.typeToSchema(boolean.class, boolean.class)).containsEntry("type", "boolean");
		assertThat(McpToolRegistry.typeToSchema(int.class, int.class)).containsEntry("type", "integer");
		assertThat(McpToolRegistry.typeToSchema(double.class, double.class)).containsEntry("type", "number");
		assertThat(McpToolRegistry.typeToSchema(Mode.class, Mode.class))
				.containsEntry("type", "string")
				.containsEntry("enum", List.of("FAST", "SLOW"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBuildInputSchema() throws Exception {
		Method method = SearchResource.class.getDeclaredMethod("search", String.class, FilterBean.class);
		JsonSchema schema = McpToolRegistry.buildInputSchema(method);

		assertThat(schema.type()).isEqualTo("object");
		assertThat(schema.additionalProperties()).isFalse();
		assertThat(schema.required()).containsExactlyInAnyOrder("query", "count");

		Map<String, Object> properties = schema.properties();
		assertThat(properties).containsOnlyKeys("query", "name", "count", "tags", "mode");

		assertThat((Map<String, Object>) properties.get("query"))
				.containsEntry("type", "string")
				.containsEntry("description", "search text");
		assertThat((Map<String, Object>) properties.get("count")).containsEntry("type", "integer");

		Map<String, Object> tagsSchema = (Map<String, Object>) properties.get("tags");
		assertThat(tagsSchema).containsEntry("type", "array");
		assertThat((Map<String, Object>) tagsSchema.get("items")).containsEntry("type", "string");

		assertThat((Map<String, Object>) properties.get("mode"))
				.containsEntry("type", "string")
				.containsEntry("enum", List.of("FAST", "SLOW"));
	}

	// ---------------------------------------------------------------------------------------
	// invoke / buildMethodArguments - the "call with arguments" pipeline
	// ---------------------------------------------------------------------------------------

	@Test
	public void testInvokeWithArguments() throws Exception {
		Method method = SearchResource.class.getDeclaredMethod("search", String.class, FilterBean.class);
		Map<String, Object> callArguments = Map.of(
				"query", "hello",
				"name", "bob",
				"count", 5,
				"tags", List.of("a", "b"),
				"mode", "SLOW");

		CallToolResult result = McpToolRegistry.invoke(SearchResource.class, method, callArguments);

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		SearchResult parsed = new ObjectMapper().readValue(textOf(result), SearchResult.class);
		assertThat(parsed.query).isEqualTo("hello");
		assertThat(parsed.name).isEqualTo("bob");
		assertThat(parsed.count).isEqualTo(5);
		assertThat(parsed.tags).containsExactlyInAnyOrder("a", "b");
		assertThat(parsed.mode).isEqualTo(Mode.SLOW);
	}

	@Test
	public void testInvokeWithMissingOptionalArguments() throws Exception {
		Method method = SearchResource.class.getDeclaredMethod("search", String.class, FilterBean.class);
		// only the required "query"/"count" are given; "name"/"tags"/"mode" are all optional
		Map<String, Object> callArguments = Map.of("query", "hello", "count", 2);

		CallToolResult result = McpToolRegistry.invoke(SearchResource.class, method, callArguments);

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		SearchResult parsed = new ObjectMapper().readValue(textOf(result), SearchResult.class);
		assertThat(parsed.query).isEqualTo("hello");
		assertThat(parsed.count).isEqualTo(2);
		assertThat(parsed.name).isNull();
		assertThat(parsed.tags).isNull();
		assertThat(parsed.mode).isNull();
	}

	@Test
	public void testInvokeUnwrapsInvocationTargetException() throws Exception {
		// Regression test: method.invoke() wraps the method's own exception in an
		// InvocationTargetException, whose own getMessage() is null - invoke() must unwrap it via
		// getCause() so the real message ("boom") is reported, not the literal string "null".
		Method method = FailingResource.class.getDeclaredMethod("fail");

		CallToolResult result = McpToolRegistry.invoke(FailingResource.class, method, Map.of());

		assertThat(result.isError()).isEqualTo(Boolean.TRUE);
		assertThat(textOf(result)).isEqualTo("boom");
	}

	// ---------------------------------------------------------------------------------------
	// buildToolSpecification - pure, no McpSyncServer needed
	// ---------------------------------------------------------------------------------------

	@Test
	public void testBuildToolSpecification() throws Exception {
		Method method = SearchResource.class.getDeclaredMethod("search", String.class, FilterBean.class);
		SyncToolSpecification spec = McpToolRegistry.buildToolSpecification(SearchResource.class, method, "search_tool");

		assertThat(spec.tool().name()).isEqualTo("search_tool");
		assertThat(spec.tool().description()).isEqualTo("search tool");
		assertThat(spec.tool().inputSchema()).isNotNull();

		// the call handler never uses the exchange parameter, so a null exchange is fine here
		CallToolResult result = spec.callHandler().apply(null,
				new CallToolRequest("search_tool", Map.of("query", "x", "count", 1)));
		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
	}

	// ---------------------------------------------------------------------------------------
	// scanAndRegister - through a real (stub-transport) McpSyncServer
	// ---------------------------------------------------------------------------------------

	@Test
	public void testScanAndRegister() {
		McpSyncServer server = newTestServer();

		McpToolRegistry.scanAndRegister(server, McpToolRegistryTest.class.getPackage().getName());

		List<Tool> tools = server.listTools();
		Set<String> names = tools.stream().map(Tool::name).collect(Collectors.toSet());

		// good registrations
		assertThat(names).contains("search_tool", "clean_tool", "failing_tool");

		// duplicate name: exactly one "duplicate_tool" survives, whichever of
		// DuplicateOneResource/DuplicateTwoResource ClassGraph happened to visit first - the scan
		// order is not a documented guarantee, so only the count is asserted, not which one won
		long duplicateCount = tools.stream().filter(t -> t.name().equals("duplicate_tool")).count();
		assertThat(duplicateCount).isEqualTo(1);

		// @Context-using fixtures must never be registered
		assertThat(names).doesNotContain("uses_context_field", "uses_context_method", "uses_context_param");

		// invalid explicit name must never be registered
		assertThat(names).doesNotContain("InvalidName");

		server.closeGracefully();
	}

	/**
	 * Characterization/regression test against the real, production-annotated
	 * {@code AdminResourceImpl#getActionLog} - unlike the other tests here, this one scans the
	 * real {@code com.gentics.contentnode.rest.resource.impl} package via the public
	 * {@link McpToolRegistry#scanAndRegister(McpSyncServer)}. Deliberately brittle: whoever
	 * annotates a second real endpoint, or changes the fields of {@code PagingParameterBean}/
	 * {@code ActionLogParameterBean}, will need to touch this test too.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testScanAndRegisterRealAdminResource() {
		McpSyncServer server = newTestServer();

		McpToolRegistry.scanAndRegister(server);

		Tool adminTool = server.listTools().stream()
				.filter(t -> t.name().equals("admin_get_action_log"))
				.findFirst()
				.orElse(null);

		assertThat(adminTool).isNotNull();

		Map<String, Object> schema = adminTool.inputSchema();
		assertThat(schema.get("type")).isEqualTo("object");
		assertThat((List<String>) schema.get("required")).isEmpty();

		Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
		assertThat(properties).containsOnlyKeys("page", "pageSize", "user", "action", "type", "objId", "start", "end");

		server.closeGracefully();
	}
}
