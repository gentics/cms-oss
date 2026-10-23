package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.Constructs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Writes;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Adds, removes or sets the nodes of constructs. Delegates to {@link ConstructResourceImpl#link} and
 * {@link ConstructResourceImpl#unlink}, which check the edit permission on each construct, but only view on the nodes.
 * So the tool first checks the edit permission on every construct and {@code updateconstructs} on every given node,
 * before anything is changed. Mode {@code set} diffs against the current nodes of each construct. The result is read
 * back afterwards.
 */
public class AssignConstructToNodesTool extends AbstractMcpTool {
	static final String NAME = "assign_construct_to_nodes";

	static final String ARG_CONSTRUCT_IDS = "constructIds";

	static final String ARG_NODE_IDS = "nodeIds";

	static final String ARG_MODE = "mode";

	static final List<String> MODES = List.of("set", "add", "remove");

	/**
	 * Nodes of a construct
	 * @param constructRef construct
	 * @param nodeRefs nodes the construct is assigned to, that the caller can view
	 */
	public record Assignment(ObjectRef constructRef, List<ObjectRef> nodeRefs) {
	}

	/**
	 * Result of the tool
	 * @param assignments assignments after the change, per construct
	 */
	public record Result(List<Assignment> assignments) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_CONSTRUCT_IDS, schema("array", "Constructs to change.", "minItems", 1, "maxItems", 50,
				"uniqueItems", true, "items", schema("integer", null, "minimum", 1)));
		properties.put(ARG_NODE_IDS, schema("array", "Nodes to add, remove, or set as the exact assignment.",
				"minItems", 1, "maxItems", 50, "uniqueItems", true, "items", schema("integer", null, "minimum", 1)));
		properties.put(ARG_MODE, schema("string", "set makes nodeIds the exact assignment, add extends it, remove "
				+ "withdraws.", "enum", MODES, "default", "add"));
		properties.put(Writes.ARG_IDEMPOTENCY_KEY, schema("string",
				"Optional client-supplied key for correlating retries. Logged only, calls are not de-duplicated.",
				"maxLength", Writes.MAX_IDEMPOTENCY_KEY_LENGTH));

		Map<String, Object> assignmentProperties = new LinkedHashMap<>();
		assignmentProperties.put("constructRef", objectRefSchema(null));
		assignmentProperties.put("nodeRefs", schema("array", "Nodes the construct is assigned to.", "items",
				objectRefSchema(null)));

		return Tool.builder().name(NAME).title("Assign construct to nodes")
				.description("Sets, adds or removes the nodes in which constructs are available. Use mode set to make "
						+ "the given node list the exact assignment, add to extend it, remove to withdraw. A construct "
						+ "not assigned to a node cannot be inserted into pages of that node, which is the usual reason "
						+ "a construct seems to be missing. Needs the right to edit the constructs and to change the "
						+ "constructs of every given node.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_CONSTRUCT_IDS, ARG_NODE_IDS)).additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", Map.of("assignments", schema("array", null,
						"items", schema("object", null, "properties", assignmentProperties))), "required",
						List.of("assignments")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		List<Integer> constructIds = CreateConstructTool.required(ARG_CONSTRUCT_IDS,
				Args.distinctIntList(arguments, ARG_CONSTRUCT_IDS, 50));
		List<Integer> nodeIds = CreateConstructTool.required(ARG_NODE_IDS, Args.distinctIntList(arguments,
				ARG_NODE_IDS, 50));
		String mode = stringArg(arguments, ARG_MODE, 0, 10);
		mode = mode != null ? mode : "add";
		if (!MODES.contains(mode)) {
			throw new IllegalArgumentException("Argument '%s' must be one of %s".formatted(ARG_MODE, MODES));
		}
		Writes.logIdempotencyKey(NAME, constructIds.get(0), arguments);

		Map<Integer, Set<Integer>> current = new LinkedHashMap<>();
		try (Trx trx = ContentNodeHelper.trx()) {
			for (int constructId : constructIds) {
				Construct construct = trx.getTransaction().getObject(Construct.class, constructId);
				if (construct == null) {
					throw new EntityNotFoundException("Construct %d does not exist".formatted(constructId));
				}
				if (!trx.getTransaction().getPermHandler().canEdit(construct)) {
					throw new InsufficientPrivilegesException("Missing permission to edit construct %d (construct "
							.formatted(constructId) + "admin update and updateconstructs on each of its nodes)",
							construct, PermType.update);
				}
				Set<Integer> nodes = new LinkedHashSet<>();
				for (Node node : construct.getNodes()) {
					nodes.add(node.getId());
				}
				current.put(constructId, nodes);
			}
			trx.success();
		}
		Constructs.checkUpdateConstructs(nodeIds);

		ConstructResource resource = RestPermissions.guard(ConstructResource.class, new ConstructResourceImpl());
		for (int constructId : constructIds) {
			Set<Integer> add = new LinkedHashSet<>();
			Set<Integer> remove = new LinkedHashSet<>();
			changes(mode, current.get(constructId), nodeIds, add, remove);
			if (!add.isEmpty()) {
				requireOk(resource.link(request(add, constructId)),
						"Construct %d was not assigned to nodes %s".formatted(constructId, add));
			}
			if (!remove.isEmpty()) {
				requireOk(resource.unlink(request(remove, constructId)),
						"Construct %d was not removed from nodes %s".formatted(constructId, remove));
			}
		}

		List<Assignment> assignments = new ArrayList<>();
		for (int constructId : constructIds) {
			ConstructLoadResponse response = resource.get(Integer.toString(constructId), new EmbedParameterBean());
			requireOk(response, "Construct %d could not be loaded".formatted(constructId));
			assignments.add(new Assignment(ObjectRef.forConstruct(response.getConstruct()),
					Constructs.nodeRefs(constructId)));
		}
		return new Result(assignments);
	}

	/**
	 * Compute the nodes to add and remove
	 * @param mode set, add or remove
	 * @param current current node IDs
	 * @param nodeIds given node IDs
	 * @param add filled with the node IDs to add
	 * @param remove filled with the node IDs to remove
	 */
	static void changes(String mode, Set<Integer> current, List<Integer> nodeIds, Set<Integer> add,
			Set<Integer> remove) {
		if (!"remove".equals(mode)) {
			nodeIds.stream().filter(id -> !current.contains(id)).forEach(add::add);
		}
		if ("remove".equals(mode)) {
			nodeIds.stream().filter(current::contains).forEach(remove::add);
		} else if ("set".equals(mode)) {
			current.stream().filter(id -> !nodeIds.contains(id)).forEach(remove::add);
		}
	}

	/**
	 * Build the link request. The CMS reads {@code ids} as node IDs and {@code targetIds} as construct IDs.
	 * @param nodeIds node IDs
	 * @param constructId construct ID
	 * @return request
	 */
	static BulkLinkUpdateRequest request(Set<Integer> nodeIds, int constructId) {
		return new BulkLinkUpdateRequest().setIds(nodeIds).setTargetIds(Set.of(Integer.toString(constructId)));
	}
}
