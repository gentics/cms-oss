package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.FileInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.resource.FileResource;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads the metadata of one file, without its binary content. Delegates to {@link FileResourceImpl#load}, which checks
 * {@code ObjectPermission.view}.
 */
public class GetFileTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	/**
	 * Result of the tool
	 * @param file the file
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(FileInfo file) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the file.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to load the file in.", "minimum", 1));

		return Tool.builder().name("get_file").title("Get file")
				.description("Loads the metadata of one file: name, size, mime type, online state and URL. Use it to "
						+ "confirm a file exists and is published before linking to it. It does NOT return the "
						+ "binary content.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("file", FileInfo.jsonSchema(null)),
						"required", List.of("file")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);

		FileLoadResponse response = RestPermissions.guard(FileResource.class, new FileResourceImpl())
				.load(Integer.toString(id), false, false, nodeId, null);
		requireOk(response, "The file %d could not be loaded".formatted(id));

		return new Result(FileInfo.of(response.getFile(), nodeId));
	}
}
