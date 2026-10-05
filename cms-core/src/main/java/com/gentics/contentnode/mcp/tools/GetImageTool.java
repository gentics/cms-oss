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
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.resource.ImageResource;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads the metadata of one image, without its binary content. Delegates to {@link ImageResourceImpl#load}, which
 * checks {@code ObjectPermission.view}.
 */
public class GetImageTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_NODE_ID = "nodeId";

	/**
	 * Result of the tool
	 * @param image the image
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(FileInfo image) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the image.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to load the image in.", "minimum", 1));

		return Tool.builder().name("get_image").title("Get image")
				.description("Loads the metadata of one image, including pixel dimensions, so you can judge whether "
						+ "it suits a hero or teaser slot. It does NOT return the binary content and it does not "
						+ "resize anything.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("image", FileInfo.jsonSchema(null)),
						"required", List.of("image")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);

		ImageLoadResponse response = RestPermissions.guard(ImageResource.class, new ImageResourceImpl())
				.load(Integer.toString(id), false, false, nodeId, null);
		requireOk(response, "The image %d could not be loaded".formatted(id));

		return new Result(FileInfo.of(response.getImage(), nodeId));
	}
}
