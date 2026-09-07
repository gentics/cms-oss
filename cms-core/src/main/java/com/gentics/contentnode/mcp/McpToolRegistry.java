package com.gentics.contentnode.mcp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.rest.mcp.McpTool;
import com.gentics.contentnode.rest.mcp.McpToolParam;
import com.gentics.lib.log.NodeLogger;

import jakarta.ws.rs.core.Context;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Scans the REST resource implementations for methods annotated with {@link McpTool} and
 * registers them as tools on a {@link McpSyncServer}.
 *
 * <p>
 * <b>Input schema and argument binding.</b> For each parameter of the tool method:
 * </p>
 * <ul>
 * <li>if the parameter itself is annotated with {@link McpToolParam}, it becomes a single scalar
 * (or enum/collection) argument;</li>
 * <li>otherwise, if the parameter's type declares fields annotated with {@link McpToolParam}
 * (e.g. a JAX-RS {@code @BeanParam} bean like {@code ActionLogParameterBean}), those fields
 * become arguments, flattened into the same top-level input schema, and the bean is instantiated
 * and populated from the call arguments before the tool method is invoked;</li>
 * <li>otherwise the parameter is left out of the schema entirely and is simply instantiated with
 * its no-arg constructor (its JAX-RS default values apply).</li>
 * </ul>
 * <p>
 * Only a handful of argument types are mapped to a JSON schema (strings, booleans, numbers, enums
 * and collections thereof); anything else gets a schema with no {@code type} constraint. Argument
 * values are converted with a plain Jackson {@link ObjectMapper}, which already handles the
 * common cases (numbers, strings, collections) generically; nested/complex bean types are not
 * specifically supported yet.
 * </p>
 *
 * <p>
 * <b>Tool naming.</b> Every tool name must be lower snake_case (see {@link #SNAKE_CASE}). If
 * {@link McpTool#name()} is left empty, a name is derived as {@code <resource>_<method>}, e.g.
 * {@code AdminResourceImpl#getActionLog} becomes {@code admin_get_action_log} (the class name has
 * any trailing {@code ResourceImpl}/{@code Impl} stripped first). This - rather than the bare
 * method name - is what keeps tools from different resource classes from colliding: the
 * underlying {@code McpSyncServer#addTool} does not reject a duplicate tool name, it silently
 * *replaces* the previously registered tool with the same name (only logging a warning). Because
 * that would otherwise be an easy way to lose a tool without any error, this registry additionally
 * rejects (logs and skips, does not abort the whole scan) any tool whose final name - explicit or
 * derived - is not valid snake_case, or collides with another tool registered in the same scan.
 * </p>
 *
 * <p>
 * <b>Visibility.</b> The scan (both class and method) ignores Java visibility
 * ({@code ignoreClassVisibility()}/{@code ignoreMethodVisibility()}), and every reflective
 * instantiation/invocation calls {@code setAccessible(true)} first - so a non-{@code public}
 * {@code @McpTool} method, its declaring class, or a bean-param class is still found and callable,
 * not silently skipped by ClassGraph's default visibility filter (found while writing this
 * registry's unit tests: nothing in {@code com.gentics.contentnode.rest.resource.impl} is
 * currently non-public, so this had no effect on {@code AdminResourceImpl#getActionLog}, but would
 * otherwise have silently excluded any future non-public tool method with no error at all).
 * </p>
 *
 * <p>
 * <b>No JAX-RS {@code @Context} injection.</b> Tool methods are invoked on an instance created
 * with a bare no-arg constructor, not through Jersey/HK2, so nothing populates fields or methods
 * annotated with {@code @Context} (e.g. {@code AbstractContentNodeResource}'s injected
 * {@code HttpServletRequest}/{@code HttpServletResponse}/{@code ContainerRequestContext}, or a
 * method parameter like {@code FileResourceImpl#createSimple}'s
 * {@code @Context HttpServletRequest}). Rather than let such a field silently stay {@code null}
 * and NullPointerException on first use, {@link #checkNoContextInjection(Class, Method)} rejects
 * (logs and skips) any method whose declaring class (walking up the hierarchy) or own parameters
 * use {@code @Context} at all, regardless of whether that specific tool method actually touches
 * the injected member - a coarser check than analyzing what the method body actually accesses,
 * but a safe one.
 * </p>
 *
 * <p>
 * <b>Known limitation (no authentication/authorization):</b> a tool call arrives on the MCP
 * transport, completely outside of the Jersey request pipeline that normally binds an
 * authenticated CMS session to the current thread and enforces {@code @RequiredPerm} via
 * {@code AuthorizationRequestFilter}. This registry does none of that. If the invoked method
 * opens its own transaction via {@link com.gentics.contentnode.etc.ContentNodeHelper#trx()} (as
 * {@code AdminResourceImpl#getActionLog} does), it will run as the CMS "system user" (id
 * {@code 1}), which {@code PermHandler} grants full permissions to - i.e. every
 * {@code @RequiredPerm} check trivially passes. Methods that instead expect an already-open
 * transaction (e.g. via {@code TransactionManager#getCurrentTransaction()}) will fail. Tying MCP
 * tool calls to a real CMS session (SID/API token) is a follow-up, see
 * {@code docs/mcp-server-integration.md}.
 * </p>
 */
public final class McpToolRegistry {
	/**
	 * Package that is scanned for {@link McpTool} annotated methods. Mirrors the package that
	 * Jersey itself scans for REST resources (see {@code jersey.config.server.provider.packages}
	 * in {@code RESTApplication}).
	 */
	private static final String RESOURCE_IMPL_PACKAGE = "com.gentics.contentnode.rest.resource.impl";

	/**
	 * Every tool name (explicit via {@link McpTool#name()}, or derived) must match this pattern:
	 * lower snake_case, starting with a letter.
	 */
	private static final Pattern SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)*$");

	private static final NodeLogger logger = NodeLogger.getNodeLogger(McpToolRegistry.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Static class, no instances
	 */
	private McpToolRegistry() {
	}

	/**
	 * Scan {@value #RESOURCE_IMPL_PACKAGE} (and subpackages) for methods annotated with
	 * {@link McpTool} and register a tool for each of them on the given server.
	 * @param server MCP server to register the tools on
	 */
	public static void scanAndRegister(McpSyncServer server) {
		scanAndRegister(server, RESOURCE_IMPL_PACKAGE);
	}

	/**
	 * Scan the given package (and subpackages) for methods annotated with {@link McpTool} and
	 * register a tool for each of them on the given server. Package-private overload of
	 * {@link #scanAndRegister(McpSyncServer)} that lets tests scan a test-only package instead of
	 * {@value #RESOURCE_IMPL_PACKAGE}.
	 * @param server MCP server to register the tools on
	 * @param packageName package to scan
	 */
	static void scanAndRegister(McpSyncServer server, String packageName) {
		Set<String> registeredNames = new HashSet<>();

		try (ScanResult scanResult = new ClassGraph()
				.enableMethodInfo()
				.enableAnnotationInfo()
				.ignoreClassVisibility()
				.ignoreMethodVisibility()
				.acceptPackages(packageName)
				.scan()) {

			for (ClassInfo classInfo : scanResult.getAllClasses()) {
				for (MethodInfo methodInfo : classInfo.getMethodInfoWithAnnotation(McpTool.class)) {
					try {
						Class<?> resourceClass = classInfo.loadClass();
						Method method = methodInfo.loadClassAndGetMethod();

						checkNoContextInjection(resourceClass, method);
						String name = resolveToolName(resourceClass, method);
						checkNotDuplicate(registeredNames, name);

						SyncToolSpecification spec = buildToolSpecification(resourceClass, method, name);
						server.addTool(spec);

						logger.info(String.format("Registered MCP tool '%s' (%s#%s)", name, resourceClass.getName(),
								method.getName()));
					} catch (Exception e) {
						logger.error(String.format("Could not register MCP tool for %s#%s", classInfo.getName(),
								methodInfo.getName()), e);
					}
				}
			}
		}
	}

	/**
	 * Reject a tool name that was already registered in the current scan.
	 * @param registeredNames names registered so far in the current scan; the given name is added
	 * to it if not already present
	 * @param name tool name to check
	 * @throws IllegalStateException if the name is already present in {@code registeredNames}
	 */
	static void checkNotDuplicate(Set<String> registeredNames, String name) {
		if (!registeredNames.add(name)) {
			throw new IllegalStateException(String.format(
					"Tool name '%s' is already used by another @McpTool method; "
							+ "set an explicit, unique McpTool#name() on one of them",
					name));
		}
	}

	/**
	 * Resolve the (snake_case) name of the tool for the given method, either from
	 * {@link McpTool#name()}, or derived as {@code <resource>_<method>} (see class Javadoc).
	 * @throws IllegalStateException if the resolved name is not valid snake_case
	 */
	static String resolveToolName(Class<?> resourceClass, Method method) {
		McpTool annotation = method.getAnnotation(McpTool.class);
		String name = !annotation.name().isEmpty() ? annotation.name()
				: resourcePrefix(resourceClass) + "_" + toSnakeCase(method.getName());

		if (!SNAKE_CASE.matcher(name).matches()) {
			throw new IllegalStateException(String.format(
					"Tool name '%s' (%s#%s) is not valid lower snake_case", name, resourceClass.getName(),
					method.getName()));
		}

		return name;
	}

	/**
	 * Derive the resource name prefix used in a default tool name, from the simple name of a
	 * resource implementation class, e.g. {@code AdminResourceImpl} becomes {@code admin}.
	 */
	static String resourcePrefix(Class<?> resourceClass) {
		String simpleName = resourceClass.getSimpleName();

		if (simpleName.endsWith("ResourceImpl")) {
			simpleName = simpleName.substring(0, simpleName.length() - "ResourceImpl".length());
		} else if (simpleName.endsWith("Impl")) {
			simpleName = simpleName.substring(0, simpleName.length() - "Impl".length());
		}

		return toSnakeCase(simpleName);
	}

	/**
	 * Convert a camelCase (or PascalCase) identifier to lower snake_case, e.g.
	 * {@code getActionLog} becomes {@code get_action_log}.
	 */
	static String toSnakeCase(String identifier) {
		StringBuilder result = new StringBuilder();

		for (int i = 0; i < identifier.length(); i++) {
			char c = identifier.charAt(i);
			if (Character.isUpperCase(c)) {
				if (result.length() > 0 && result.charAt(result.length() - 1) != '_') {
					result.append('_');
				}
				result.append(Character.toLowerCase(c));
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	/**
	 * Reject a tool method whose declaring class (or its superclasses) relies on JAX-RS
	 * {@code @Context} injection - as a field, a context-injection setter method (JAX-RS also
	 * supports {@code @Context} on methods, not just fields, e.g.
	 * {@code AbstractContentNodeResource#setSessionSecretFromCookie}), or a parameter of the tool
	 * method itself (e.g. {@code FileResourceImpl#createSimple}'s
	 * {@code @Context HttpServletRequest}).
	 *
	 * <p>
	 * {@code McpToolRegistry} instantiates the resource class with a bare no-arg constructor and
	 * never runs it through Jersey/HK2, so any such field/parameter would simply stay {@code null}
	 * - this fails fast at registration time with a clear message, instead of a confusing
	 * {@link NullPointerException} the first time a tool call actually touches it.
	 * </p>
	 * @throws IllegalStateException if the method's declaring class (or the method itself) uses
	 * {@code @Context} injection
	 */
	static void checkNoContextInjection(Class<?> resourceClass, Method method) {
		List<String> contextMembers = new ArrayList<>();

		for (Class<?> current = resourceClass; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.isAnnotationPresent(Context.class)) {
					contextMembers.add(String.format("field %s#%s", current.getName(), field.getName()));
				}
			}
			for (Method candidate : current.getDeclaredMethods()) {
				if (candidate.isAnnotationPresent(Context.class)) {
					contextMembers.add(String.format("method %s#%s", current.getName(), candidate.getName()));
				}
			}
		}

		for (Parameter parameter : method.getParameters()) {
			if (parameter.isAnnotationPresent(Context.class)) {
				contextMembers.add(String.format("parameter '%s' of %s#%s", parameter.getName(),
						resourceClass.getName(), method.getName()));
			}
		}

		if (!contextMembers.isEmpty()) {
			throw new IllegalStateException(String.format(
					"%s#%s cannot be an MCP tool: it relies on JAX-RS @Context injection (%s), which "
							+ "McpToolRegistry does not provide (the resource class is instantiated with a "
							+ "bare no-arg constructor, not through Jersey/HK2)",
					resourceClass.getName(), method.getName(), String.join(", ", contextMembers)));
		}
	}

	/**
	 * Build a {@link Tool} and {@link SyncToolSpecification} for the given method. Pure (does not
	 * touch any {@link McpSyncServer}), so it can be unit tested directly - including invoking the
	 * returned specification's {@code callHandler()} with a hand-built
	 * {@code McpSchema.CallToolRequest} and a {@code null} exchange (the call handler never uses
	 * the exchange parameter).
	 * @param resourceClass declaring class of the annotated method
	 * @param method annotated method
	 * @param name resolved (and already validated) tool name
	 * @return tool specification, ready to be passed to {@link McpSyncServer#addTool(SyncToolSpecification)}
	 */
	static SyncToolSpecification buildToolSpecification(Class<?> resourceClass, Method method, String name) {
		McpTool annotation = method.getAnnotation(McpTool.class);

		Tool tool = Tool.builder()
				.name(name)
				.title(annotation.title())
				.description(annotation.description())
				.inputSchema(buildInputSchema(method))
				.build();

		return SyncToolSpecification.builder()
				.tool(tool)
				.callHandler((exchange, request) -> invoke(resourceClass, method, request.arguments()))
				.build();
	}

	/**
	 * Build the input schema of a tool method, from every {@link McpToolParam} annotated parameter
	 * and every {@link McpToolParam} annotated field of a parameter's type.
	 * @param method tool method
	 * @return JSON schema describing the tool's arguments
	 */
	static JsonSchema buildInputSchema(Method method) {
		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();

		for (Parameter parameter : method.getParameters()) {
			McpToolParam paramAnnotation = parameter.getAnnotation(McpToolParam.class);
			if (paramAnnotation != null) {
				addProperty(properties, required, resolveName(paramAnnotation, parameter.getName()), paramAnnotation,
						parameter.getType(), parameter.getParameterizedType());
				continue;
			}

			for (Field field : parameter.getType().getDeclaredFields()) {
				McpToolParam fieldAnnotation = field.getAnnotation(McpToolParam.class);
				if (fieldAnnotation != null) {
					addProperty(properties, required, resolveName(fieldAnnotation, field.getName()), fieldAnnotation,
							field.getType(), field.getGenericType());
				}
			}
		}

		return JsonSchema.builder()
				.type("object")
				.properties(properties)
				.required(required)
				.additionalProperties(false)
				.build();
	}

	/**
	 * Add a single property to the input schema being built.
	 */
	private static void addProperty(Map<String, Object> properties, List<String> required, String name,
			McpToolParam annotation, Class<?> type, Type genericType) {
		Map<String, Object> propertySchema = typeToSchema(type, genericType);
		if (!annotation.description().isEmpty()) {
			propertySchema.put("description", annotation.description());
		}
		properties.put(name, propertySchema);

		if (annotation.required()) {
			required.add(name);
		}
	}

	/**
	 * Map a Java type to a (partial) JSON schema. Only scalars, enums and collections thereof are
	 * given a {@code type}; anything else is left unconstrained.
	 */
	static Map<String, Object> typeToSchema(Class<?> rawType, Type genericType) {
		Map<String, Object> schema = new LinkedHashMap<>();

		if (rawType == String.class || rawType.isEnum()) {
			schema.put("type", "string");
			if (rawType.isEnum()) {
				schema.put("enum", Stream.of(rawType.getEnumConstants()).map(Object::toString).toList());
			}
		} else if (rawType == boolean.class || rawType == Boolean.class) {
			schema.put("type", "boolean");
		} else if (rawType == int.class || rawType == Integer.class || rawType == long.class || rawType == Long.class
				|| rawType == short.class || rawType == Short.class) {
			schema.put("type", "integer");
		} else if (rawType == double.class || rawType == Double.class || rawType == float.class || rawType == Float.class) {
			schema.put("type", "number");
		} else if (Collection.class.isAssignableFrom(rawType)) {
			schema.put("type", "array");
			if (genericType instanceof ParameterizedType parameterizedType
					&& parameterizedType.getActualTypeArguments().length == 1
					&& parameterizedType.getActualTypeArguments()[0] instanceof Class<?> elementType) {
				schema.put("items", typeToSchema(elementType, elementType));
			}
		}

		return schema;
	}

	/**
	 * Invoke the given tool method on a fresh instance of its declaring class and turn the result
	 * into a {@link CallToolResult}.
	 * @param resourceClass declaring class of the method
	 * @param method method to invoke
	 * @param callArguments arguments of the incoming tool call
	 * @return result of the tool call
	 */
	static CallToolResult invoke(Class<?> resourceClass, Method method, Map<String, Object> callArguments) {
		try {
			Constructor<?> constructor = resourceClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			Object instance = constructor.newInstance();

			Object[] args = buildMethodArguments(method, callArguments);

			method.setAccessible(true);
			Object result = method.invoke(instance, args);

			return CallToolResult.builder().addTextContent(MAPPER.writeValueAsString(result)).build();
		} catch (Exception e) {
			// method.invoke() wraps any exception thrown by the tool method itself in an
			// InvocationTargetException, whose own getMessage() is null - unwrap it so the actual
			// error (e.g. a NodeException) is logged/reported instead of "null".
			Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
			logger.error(String.format("Error while invoking MCP tool %s#%s", resourceClass.getName(), method.getName()), cause);
			return CallToolResult.builder().isError(true).addTextContent(String.valueOf(cause.getMessage())).build();
		}
	}

	/**
	 * Build the arguments to invoke the tool method with, from the incoming tool call arguments.
	 * Mirrors the parameter/field handling of {@link #buildInputSchema(Method)}.
	 */
	static Object[] buildMethodArguments(Method method, Map<String, Object> callArguments) throws ReflectiveOperationException {
		Parameter[] parameters = method.getParameters();
		Object[] args = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			McpToolParam paramAnnotation = parameter.getAnnotation(McpToolParam.class);

			if (paramAnnotation != null) {
				String name = resolveName(paramAnnotation, parameter.getName());
				args[i] = convert(callArguments.get(name), parameter.getParameterizedType());
				continue;
			}

			Constructor<?> beanConstructor = parameter.getType().getDeclaredConstructor();
			beanConstructor.setAccessible(true);
			Object bean = beanConstructor.newInstance();

			for (Field field : parameter.getType().getDeclaredFields()) {
				McpToolParam fieldAnnotation = field.getAnnotation(McpToolParam.class);
				if (fieldAnnotation == null) {
					continue;
				}

				String name = resolveName(fieldAnnotation, field.getName());
				if (callArguments.containsKey(name)) {
					field.setAccessible(true);
					field.set(bean, convert(callArguments.get(name), field.getGenericType()));
				}
			}
			args[i] = bean;
		}

		return args;
	}

	/**
	 * Convert a raw tool call argument value into the target (parameter or field) type, using
	 * Jackson, which already handles the common cases (numbers, strings, collections) generically.
	 */
	static Object convert(Object rawValue, Type targetType) {
		if (rawValue == null) {
			return null;
		}

		JavaType javaType = MAPPER.getTypeFactory().constructType(targetType);
		return MAPPER.convertValue(rawValue, javaType);
	}

	/**
	 * Resolve the argument name of an {@link McpToolParam} annotation, falling back to the given
	 * reflected name (a field's name is always reliable; a parameter's name is only reliable if
	 * the code was compiled with {@code -parameters}).
	 */
	static String resolveName(McpToolParam annotation, String reflectedName) {
		return !annotation.name().isEmpty() ? annotation.name() : reflectedName;
	}
}
