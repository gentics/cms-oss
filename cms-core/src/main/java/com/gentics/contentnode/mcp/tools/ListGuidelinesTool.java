package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.McpSchemas.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.Guidelines;
import com.gentics.contentnode.mcp.util.Guidelines.Guideline;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.MiscUtils;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the guidelines of a node, from the devtools packages assigned to it. Delegates to {@link PackageResourceImpl},
 * whose class-level permissions apply.
 */
public class ListGuidelinesTool extends AbstractMcpTool {
	static final String ARG_NODE_ID = "nodeId";

	/**
	 * Result of the tool
	 * @param items guidelines, by priority, then ID
	 */
	public record Result(List<Guideline> items) {
	}

	/**
	 * Build the JSON schema of a guideline
	 * @return schema properties
	 */
	static Map<String, Object> itemProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("id", schema("string", "The ID, <package>/<name>[.default]."));
		properties.put("package", schema("string", null));
		properties.put("name", schema("string", null));
		properties.put("default", schema("boolean",
				"Applies to every session of the package's nodes. Any other guideline is added by the user."));
		properties.put("priority", schema("integer", "Lower comes first, those without one last. Not part of the ID."));
		return properties;
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_NODE_ID, schema("integer", "ID of the node. A channel resolves to its master node.",
				"minimum", 1));

		return Tool.builder().name("list_guidelines").title("List guidelines")
				.description("Lists the guidelines of a node, gathered from the devtools packages assigned to it. A "
						+ "channel resolves to its master node. Ordered by priority, then ID. Of two files with the "
						+ "same ID, only the first is listed.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_NODE_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("items", schema("array", null, "items",
						schema("object", null, "properties", itemProperties(), "required",
								List.of("id", "package", "name", "default")))), "required", List.of("items")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int nodeId = Args.id(arguments, ARG_NODE_ID);
		String masterId;
		try (Trx trx = ContentNodeHelper.trx()) {
			masterId = Integer.toString(MiscUtils.load(Node.class, Integer.toString(nodeId)).getMaster().getId());
			trx.success();
		}

		ListPackagesTool.checkDevtools();
		PackageResource resource = RestPermissions.guard(PackageResource.class, new PackageResourceImpl());
		List<Package> packages = ListResponses.items(resource.listNodePackages(masterId, new FilterParameterBean(),
				new SortParameterBean(), new PagingParameterBean()));
		List<Guideline> guidelines = new ArrayList<>();
		for (Package pkg : packages) {
			guidelines.addAll(Guidelines.inPackage(resource, pkg.getName()));
		}
		return new Result(Guidelines.sorted(guidelines));
	}
}
