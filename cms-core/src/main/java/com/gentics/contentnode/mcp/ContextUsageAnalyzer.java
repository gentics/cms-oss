package com.gentics.contentnode.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gentics.lib.log.NodeLogger;

/**
 * Determines, via static bytecode analysis, whether invoking a given {@code @McpTool} method
 * could actually read one of its declaring class's (or its ancestors') JAX-RS {@code @Context}
 * fields - as opposed to {@code McpToolRegistry}'s previous, class-wide check, which rejected a
 * tool whenever its class merely <em>had</em> such a field, regardless of whether the tool method
 * ever touched it.
 *
 * <p>
 * <b>Approach.</b> Starting from the tool method, follow direct {@code invokevirtual}/
 * {@code invokespecial}/{@code invokestatic}/{@code invokeinterface} call edges (via ASM's tree
 * API), scoped to the resource class and its superclass chain - that is where inherited helpers
 * like {@code AbstractContentNodeResource#getRequest()} live; calls leaving that hierarchy
 * (into {@code java.*}, {@code jakarta.*}, other CMS internals, ...) are not expanded further,
 * since the {@code @Context} fields in question cannot be reached through them. Every reachable
 * method's bytecode is scanned for a {@code GETFIELD}/{@code PUTFIELD} against one of the known
 * {@code @Context} fields.
 * </p>
 *
 * <p>
 * <b>Known limitation.</b> This only follows direct call instructions. It does not follow method
 * references, lambdas (their bodies are separate synthetic methods reached only through an
 * {@code invokedynamic} bootstrap, which is not resolved here) or reflective calls. A method that
 * only reaches a {@code @Context} field through one of those is not detected, and the tool is
 * allowed to register - see the {@code WARN} log {@code McpToolRegistry} emits in that case.
 * </p>
 */
final class ContextUsageAnalyzer {
	private static final NodeLogger logger = NodeLogger.getNodeLogger(ContextUsageAnalyzer.class);

	private ContextUsageAnalyzer() {
	}

	/**
	 * Reference to a method, as found in a method call instruction: not necessarily resolved to
	 * the class that actually declares it yet (that resolution happens in
	 * {@link #resolveDeclaringClass(Class, String, String, Map)}).
	 */
	private record MethodRef(String owner, String name, String desc) {
	}

	/**
	 * @param resourceClass declaring class of the tool method (and root of the hierarchy walk)
	 * @param toolMethod the {@code @McpTool} method to analyze
	 * @param contextFields every {@code @Context} field in {@code resourceClass}'s hierarchy (see
	 * {@link McpToolRegistry#collectContextFields(Class)}); must not be empty
	 * @return {@code true} if {@code toolMethod}, or a method it (transitively) calls within
	 * {@code resourceClass}'s hierarchy, reads one of {@code contextFields}; {@code false} if no
	 * such use was found. On a bytecode-reading failure, conservatively returns {@code true}
	 * (i.e. "assume it's used") rather than silently allowing a tool through unanalyzed.
	 */
	static boolean usesContext(Class<?> resourceClass, Method toolMethod, List<Field> contextFields) {
		Set<String> contextFieldKeys = new HashSet<>();
		for (Field field : contextFields) {
			contextFieldKeys.add(Type.getInternalName(field.getDeclaringClass()) + "#" + field.getName());
		}

		Map<String, Class<?>> hierarchyByInternalName = new HashMap<>();
		for (Class<?> current = resourceClass; current != null && current != Object.class; current = current.getSuperclass()) {
			hierarchyByInternalName.put(Type.getInternalName(current), current);
		}

		Map<Class<?>, ClassNode> classNodeCache = new HashMap<>();
		Deque<MethodRef> queue = new ArrayDeque<>();
		Set<MethodRef> visited = new HashSet<>();
		queue.add(new MethodRef(Type.getInternalName(resourceClass), toolMethod.getName(), Type.getMethodDescriptor(toolMethod)));

		while (!queue.isEmpty()) {
			MethodRef ref = queue.poll();
			if (!visited.add(ref)) {
				continue;
			}

			Class<?> owningClass = hierarchyByInternalName.get(ref.owner());
			if (owningClass == null) {
				// left the resource class's hierarchy (e.g. a call into an interface default
				// method, or Object) - nothing more to expand here.
				continue;
			}

			ResolvedMethod resolved;
			try {
				resolved = resolveDeclaringClass(owningClass, ref.name(), ref.desc(), classNodeCache);
			} catch (IOException e) {
				logger.warn(String.format(
						"Could not read bytecode while checking %s#%s(%s) for @Context field usage (starting from "
								+ "%s#%s); conservatively treating it as if it used one",
						ref.owner(), ref.name(), ref.desc(), resourceClass.getName(), toolMethod.getName()), e);
				return true;
			}
			if (resolved == null) {
				// method isn't declared anywhere in the hierarchy we walked (e.g. inherited from
				// Object, or an interface default) - not expandable, so not a concern.
				continue;
			}

			for (AbstractInsnNode insn : resolved.methodNode().instructions) {
				if (insn instanceof FieldInsnNode fieldInsn) {
					if (contextFieldKeys.contains(fieldInsn.owner + "#" + fieldInsn.name)) {
						return true;
					}
				} else if (insn instanceof MethodInsnNode methodInsn) {
					if (hierarchyByInternalName.containsKey(methodInsn.owner)) {
						queue.add(new MethodRef(methodInsn.owner, methodInsn.name, methodInsn.desc));
					}
				}
			}
		}

		return false;
	}

	private record ResolvedMethod(Class<?> declaringClass, MethodNode methodNode) {
	}

	/**
	 * Find the nearest class - starting at {@code startClass} and walking up its superclass
	 * chain - that actually declares a method matching {@code name}/{@code desc}. Needed because
	 * a call site's bytecode {@code owner} is the compile-time static type of the call
	 * expression (e.g. {@code this}), which for an inherited, non-overridden method is the
	 * calling class itself, not the ancestor that actually declares it.
	 */
	private static ResolvedMethod resolveDeclaringClass(Class<?> startClass, String name, String desc,
			Map<Class<?>, ClassNode> classNodeCache) throws IOException {
		for (Class<?> current = startClass; current != null && current != Object.class; current = current.getSuperclass()) {
			ClassNode classNode = getClassNode(current, classNodeCache);
			if (classNode == null) {
				continue;
			}
			for (MethodNode methodNode : classNode.methods) {
				if (methodNode.name.equals(name) && methodNode.desc.equals(desc)) {
					return new ResolvedMethod(current, methodNode);
				}
			}
		}
		return null;
	}

	private static ClassNode getClassNode(Class<?> clazz, Map<Class<?>, ClassNode> cache) throws IOException {
		if (cache.containsKey(clazz)) {
			return cache.get(clazz);
		}

		ClassNode classNode = null;
		String resourceName = Type.getInternalName(clazz) + ".class";
		ClassLoader classLoader = clazz.getClassLoader() != null ? clazz.getClassLoader() : ClassLoader.getSystemClassLoader();

		try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
			if (in != null) {
				ClassReader reader = new ClassReader(in);
				classNode = new ClassNode();
				reader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			}
		}

		cache.put(clazz, classNode);
		return classNode;
	}
}
