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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.contentnode.etc.ContentNodeHelper;
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
 * {@code @Context HttpServletRequest}). {@link #checkNoContextInjection(Class, Method)} always
 * rejects a tool method that itself declares a {@code @Context} parameter - that would definitely
 * stay {@code null} and NullPointerException on first use. For {@code @Context} <em>fields</em>
 * declared somewhere in the class hierarchy, it is more permissive: {@link ContextUsageAnalyzer}
 * statically analyzes whether the specific tool method - or a method it (transitively) calls
 * within the resource class's own hierarchy - actually reads one of those fields, and only
 * rejects the tool if it does. A tool that is allowed through despite its class having
 * {@code @Context} fields is still logged at {@code WARN}, since the analysis only follows direct
 * method calls (not lambdas, method references, or reflection) and can therefore miss an
 * indirect use.
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

	/**
	 * Language ID set on {@link ContentNodeHelper} for the duration of a tool call, so that
	 * i18n-translated messages (e.g. from thrown {@code RestMappedException}s) resolve to readable
	 * text instead of a raw key. Same value/convention as {@code PublishWorker}/{@code Publisher}/
	 * {@code JobController} already use for their own sessionless background work.
	 */
	private static final int BACKEND_LANGUAGE_ID = 2;

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
	 * Reject a tool method that itself declares a JAX-RS {@code @Context} parameter (e.g.
	 * {@code FileResourceImpl#createSimple}'s {@code @Context HttpServletRequest}) - that always
	 * stays {@code null}, since {@code McpToolRegistry} instantiates the resource class with a
	 * bare no-arg constructor and never runs it through Jersey/HK2. For {@code @Context} fields
	 * declared somewhere in the class's hierarchy (e.g. {@code AbstractContentNodeResource}'s
	 * injected {@code HttpServletRequest}/{@code HttpServletResponse}/
	 * {@code ContainerRequestContext}), reject the tool only if {@link ContextUsageAnalyzer}
	 * finds that the method (directly, or via a method it calls within the hierarchy) actually
	 * reads one of them - otherwise allow it through, but log a {@code WARN} (see
	 * {@link ContextUsageAnalyzer}'s class Javadoc for why this is a heuristic, not a guarantee).
	 * @throws IllegalStateException if the method declares a {@code @Context} parameter, or if
	 * static analysis found a reachable use of a {@code @Context} field
	 */
	static void checkNoContextInjection(Class<?> resourceClass, Method method) {
		List<String> contextParams = new ArrayList<>();
		for (Parameter parameter : method.getParameters()) {
			if (parameter.isAnnotationPresent(Context.class)) {
				contextParams.add(String.format("parameter '%s' of %s#%s", parameter.getName(),
						resourceClass.getName(), method.getName()));
			}
		}
		if (!contextParams.isEmpty()) {
			throw new IllegalStateException(String.format(
					"%s#%s cannot be an MCP tool: it declares JAX-RS @Context parameter(s) (%s), which "
							+ "McpToolRegistry does not provide (the resource class is instantiated with a "
							+ "bare no-arg constructor, not through Jersey/HK2)",
					resourceClass.getName(), method.getName(), String.join(", ", contextParams)));
		}

		List<Field> contextFields = collectContextFields(resourceClass);
		if (contextFields.isEmpty()) {
			return;
		}

		if (ContextUsageAnalyzer.usesContext(resourceClass, method, contextFields)) {
			throw new IllegalStateException(String.format(
					"%s#%s cannot be an MCP tool: static analysis found that it (directly, or via a method it "
							+ "calls within %s's hierarchy) reads a JAX-RS @Context field (%s), which "
							+ "McpToolRegistry does not provide (the resource class is instantiated with a bare "
							+ "no-arg constructor, not through Jersey/HK2)",
					resourceClass.getName(), method.getName(), resourceClass.getSimpleName(), describeFields(contextFields)));
		}

		logger.warn(String.format(
				"Registering %s#%s as an MCP tool even though %s's hierarchy declares JAX-RS @Context field(s) "
						+ "(%s): static analysis found no reachable use of them from this method. That analysis "
						+ "only follows direct method calls (not lambdas, method references, or reflection), so "
						+ "if this tool throws a NullPointerException at runtime, re-check whether it reaches one "
						+ "of these fields indirectly",
				resourceClass.getName(), method.getName(), resourceClass.getSimpleName(), describeFields(contextFields)));
	}

	/**
	 * Collect every {@code @Context}-annotated field in {@code resourceClass}'s hierarchy (up to
	 * but not including {@link Object}). {@code @Context}-annotated <em>methods</em> (JAX-RS also
	 * supports setter-style injection, e.g.
	 * {@code AbstractContentNodeResource#setSessionSecretFromCookie}) are deliberately not
	 * collected here: they are only ever invoked by the JAX-RS injection machinery itself, never
	 * reachable from a tool method's own code, so they are not a concern for
	 * {@link ContextUsageAnalyzer}.
	 */
	static List<Field> collectContextFields(Class<?> resourceClass) {
		List<Field> contextFields = new ArrayList<>();
		for (Class<?> current = resourceClass; current != null && current != Object.class; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.isAnnotationPresent(Context.class)) {
					contextFields.add(field);
				}
			}
		}
		return contextFields;
	}

	private static String describeFields(List<Field> fields) {
		return fields.stream()
				.map(field -> field.getDeclaringClass().getSimpleName() + "#" + field.getName())
				.collect(Collectors.joining(", "));
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
	 * and every {@link McpToolParam} annotated field of a parameter's type. Rejects the method if
	 * two parameters/fields resolve to the same argument name (see
	 * {@link #checkNotDuplicateArgument(Set, String, Method)}) - this can happen when a method
	 * combines several parameter beans, or a bean and a directly-annotated parameter, whose fields
	 * happen to share a name. For example, a resource method could take a direct
	 * {@code @QueryParam("nodeId") List<String> nodeIds} parameter alongside a bean with its own
	 * {@code @QueryParam("nodeId") Integer nodeId} field - the same JAX-RS query key bound to two
	 * differently-typed targets, which JAX-RS itself allows (this was the case for
	 * {@code TemplateResourceImpl#list}'s {@code nodeIds} parameter and
	 * {@code TemplateListParameterBean#nodeId} when this guard was added; the resource method has
	 * since been changed to use a different, unrelated parameter bean, but the guard remains
	 * necessary for any future case shaped like it - see {@code DuplicateArgResource} in
	 * {@code McpToolRegistryTest}).
	 * @param method tool method
	 * @return JSON schema describing the tool's arguments
	 * @throws IllegalStateException if two parameters/fields resolve to the same argument name
	 */
	static JsonSchema buildInputSchema(Method method) {
		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();
		Set<String> seenNames = new HashSet<>();

		for (Parameter parameter : method.getParameters()) {
			McpToolParam paramAnnotation = parameter.getAnnotation(McpToolParam.class);
			if (paramAnnotation != null) {
				String name = resolveName(paramAnnotation, parameter.getName());
				checkNotDuplicateArgument(seenNames, name, method);
				addProperty(properties, required, name, paramAnnotation, parameter.getType(), parameter.getParameterizedType());
				continue;
			}

			for (Field field : parameter.getType().getDeclaredFields()) {
				McpToolParam fieldAnnotation = field.getAnnotation(McpToolParam.class);
				if (fieldAnnotation != null) {
					String name = resolveName(fieldAnnotation, field.getName());
					checkNotDuplicateArgument(seenNames, name, method);
					addProperty(properties, required, name, fieldAnnotation, field.getType(), field.getGenericType());
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
	 * Reject an argument name that was already used by another parameter/field of the same tool
	 * method's input schema.
	 * @param seenNames argument names used so far while building this method's schema; {@code name}
	 * is added to it if not already present
	 * @param name argument name to check
	 * @param method tool method being built, used only for the error message
	 * @throws IllegalStateException if {@code name} is already present in {@code seenNames}
	 */
	static void checkNotDuplicateArgument(Set<String> seenNames, String name, Method method) {
		if (!seenNames.add(name)) {
			throw new IllegalStateException(String.format(
					"Tool argument name '%s' is used by more than one parameter/field of %s#%s; set an explicit, "
							+ "unique McpToolParam#name() on one of them",
					name, method.getDeclaringClass().getName(), method.getName()));
		}
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
	 *
	 * <p>
	 * Sets a fixed backend language ({@link #BACKEND_LANGUAGE_ID}) for the duration of the call,
	 * the same way {@code PublishWorker}/{@code Publisher}/{@code JobController} already do for
	 * their own sessionless background work. Without this, {@link ContentNodeHelper#getLanguageId()}
	 * has nothing to return (no CMS session is bound to an MCP call, see §7.5/§8 of
	 * {@code docs/mcp-server-integration.md}), which makes any i18n-translated message (e.g. from
	 * {@code com.gentics.lib.i18n.CNI18nString}, used by most {@code RestMappedException}s such as
	 * {@code EntityNotFoundException}) fall back to its raw, untranslated key instead of readable
	 * text - confirmed by manual testing, see {@code docs/mcp-tests.md}.
	 * </p>
	 * @param resourceClass declaring class of the method
	 * @param method method to invoke
	 * @param callArguments arguments of the incoming tool call
	 * @return result of the tool call
	 */
	static CallToolResult invoke(Class<?> resourceClass, Method method, Map<String, Object> callArguments) {
		ContentNodeHelper.setLanguageId(BACKEND_LANGUAGE_ID);
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
		} finally {
			// the reactor scheduler threads that run tool calls are pooled/reused across many
			// unrelated invocations, unlike PublishWorker's dedicated thread - reset the ThreadLocal
			// so it doesn't leak into whatever runs next on this thread.
			ContentNodeHelper.setLanguageId(-1);
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
