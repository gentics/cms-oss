package com.gentics.contentnode.rest.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a single argument of a {@link McpTool} annotated method, so that it can be added to
 * the tool's JSON input schema and populated from an incoming tool call.
 *
 * <p>
 * Can be placed either directly on a method parameter, or on a field of a parameter's type (the
 * latter is the common case in this codebase, where REST resource methods take JAX-RS
 * {@code @BeanParam} parameter beans instead of plain scalar parameters, e.g.
 * {@code ActionLogParameterBean#action}). Only annotated parameters/fields are exposed to MCP
 * clients as tool arguments; parameters (or bean fields) that are only needed for the JAX-RS
 * binding are left unannotated and keep their default value.
 * </p>
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolParam {
	/**
	 * Name of the argument, as it appears in the tool's input schema and in the arguments map of
	 * an incoming tool call. If left empty:
	 * <ul>
	 * <li>on a field, the field's own name is used (always reliable via reflection);</li>
	 * <li>on a method parameter, the parameter's name is used, which is only reliable if the code
	 * was compiled with {@code -parameters} (not the case in this project) - so {@code name} should
	 * be set explicitly when annotating a parameter directly.</li>
	 * </ul>
	 * @return argument name, or empty to derive it as described above
	 */
	String name() default "";

	/**
	 * Description of the argument, as reported to MCP clients.
	 * @return argument description
	 */
	String description() default "";

	/**
	 * Whether the argument is required.
	 * @return true iff the argument is required
	 */
	boolean required() default true;
}
