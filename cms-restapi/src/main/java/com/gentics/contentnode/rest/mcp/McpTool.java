package com.gentics.contentnode.rest.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a REST resource implementation as an MCP tool.
 *
 * <p>
 * Annotated methods are found via classpath scan (see {@code McpToolRegistry} in cms-core) and
 * registered on the MCP server, so that they can be invoked by MCP clients.
 * </p>
 *
 * <p>
 * This annotation lives in cms-restapi (not cms-core, where the MCP server itself lives),
 * because it - together with {@link McpToolParam} - also needs to be usable on the parameter
 * bean classes under {@code com.gentics.contentnode.rest.resource.parameter}, which are part of
 * cms-restapi and must not depend on cms-core.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {
	/**
	 * Name of the tool, as reported to MCP clients. If left empty, the name of the annotated
	 * method is used.
	 * @return tool name
	 */
	String name() default "";

	/**
	 * Human readable title of the tool, as reported to MCP clients.
	 * @return tool title
	 */
	String title() default "";

	/**
	 * Description of the tool, as reported to MCP clients.
	 * @return tool description
	 */
	String description();
}
