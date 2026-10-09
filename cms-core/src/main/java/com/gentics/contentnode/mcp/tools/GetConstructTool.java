package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ConstructInfo;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.Constructs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads a construct by ID or keyword. Delegates to {@link ConstructResourceImpl#get}, which checks the view permission
 * on the construct. A keyword is resolved to the ID first ({@link Constructs#resolve}); the nodes are those the caller
 * can view ({@link Constructs#nodeRefs}).
 */
public class GetConstructTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_KEYWORD = "keyword";

	static final String ARG_EMBED_CATEGORY = "embedCategory";

	/**
	 * Result of the tool
	 * @param construct construct
	 * @param nodeRefs nodes the construct is assigned to
	 */
	public record Result(ConstructInfo construct, List<ObjectRef> nodeRefs) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the construct.", "minimum", 1));
		properties.put(ARG_KEYWORD, schema("string", "Keyword of the construct.", "maxLength", 100));
		properties.put(ARG_EMBED_CATEGORY, schema("boolean", "Whether to return the category.", "default", true));

		return Tool.builder().name("get_construct").title("Get construct")
				.description("Loads one construct in full, including every part and the Handlebars template source. "
						+ "This is the mandatory first step before update_construct, because an update replaces the "
						+ "whole part list and you need to know what is there. The template is returned under "
						+ "template, not among parts: REST reports its type as RICHTEXT, so you cannot identify it by "
						+ "type alone. Pass exactly one of id or keyword.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(outputSchema())
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = constructId(arguments, ARG_ID, ARG_KEYWORD);
		boolean embedCategory = booleanArg(arguments, ARG_EMBED_CATEGORY, true);

		ConstructLoadResponse response = RestPermissions.guard(ConstructResource.class, new ConstructResourceImpl())
				.get(Integer.toString(id), new EmbedParameterBean().withEmbed(embedCategory ? "category" : null));
		requireOk(response, "Construct %d could not be loaded".formatted(id));
		return new Result(ConstructInfo.of(response.getConstruct()), Constructs.nodeRefs(id));
	}

	/**
	 * Get the construct ID from exactly one of an ID and a keyword argument, resolving the keyword
	 * @param arguments arguments
	 * @param idArg name of the ID argument
	 * @param keywordArg name of the keyword argument
	 * @return construct ID
	 * @throws Exception
	 * @throws IllegalArgumentException if neither or both are given
	 */
	static int constructId(Map<String, Object> arguments, String idArg, String keywordArg) throws Exception {
		if (Args.exactlyOne(arguments, idArg, keywordArg).equals(idArg)) {
			return Args.id(arguments, idArg);
		}
		String keyword = nullIfBlank(stringArg(arguments, keywordArg, 1, 100));
		if (keyword == null) {
			throw new IllegalArgumentException("Argument '%s' must not be blank".formatted(keywordArg));
		}
		return Constructs.resolve(keyword);
	}

	/**
	 * Build the output schema
	 * @return output schema
	 */
	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("construct", ConstructInfo.jsonSchema(null));
		properties.put("nodeRefs", schema("array", "Nodes the construct is assigned to, that the caller can view.",
				"items", objectRefSchema(null)));
		return schema("object", null, "properties", properties, "required", List.of("construct"));
	}
}
