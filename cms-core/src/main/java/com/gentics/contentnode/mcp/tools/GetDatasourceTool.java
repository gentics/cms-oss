package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.tools.ListDatasourcesTool.DatasourceInfo;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.DatasourceEntryModel;
import com.gentics.contentnode.rest.model.response.DatasourceLoadResponse;
import com.gentics.contentnode.rest.resource.DatasourceResource;
import com.gentics.contentnode.rest.resource.impl.DatasourceResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Loads a datasource with a slice of its entries and the constructs using it. Delegates to
 * {@link DatasourceResourceImpl#get}, {@link DatasourceResourceImpl#listEntries} and
 * {@link DatasourceResourceImpl#constructs}, which need the datasource admin view permission and the view permission
 * on the datasource; the constructs are filtered by view permission.
 */
public class GetDatasourceTool extends AbstractMcpTool {
	static final String ARG_ID = "id";

	static final String ARG_INCLUDE_ENTRIES = "includeEntries";

	static final String ARG_SIZE = "size";

	static final String ARG_FROM = "from";

	/**
	 * An entry of a datasource
	 * @param id ID
	 * @param globalId global ID
	 * @param key key, which the select part stores
	 * @param value value, which the editor shows
	 */
	@JsonInclude(Include.NON_NULL)
	public record Entry(Integer id, String globalId, String key, String value) {
	}

	/**
	 * Result of the tool. Fields that are not set are omitted on serialization.
	 * @param datasource datasource
	 * @param total number of entries
	 * @param entries entries of the requested slice, if requested
	 * @param constructRefs constructs using the datasource
	 */
	@JsonInclude(Include.NON_NULL)
	public record Result(DatasourceInfo datasource, int total, List<Entry> entries, List<ObjectRef> constructRefs) {
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_ID, schema("integer", "ID of the datasource.", "minimum", 1));
		properties.put(ARG_INCLUDE_ENTRIES, schema("boolean", "Whether to return the entries.", "default", true));
		properties.put(ARG_SIZE, schema("integer", "Maximum number of entries to return.", "minimum", 1, "maximum",
				200, "default", 25));
		properties.put(ARG_FROM, schema("integer", "Offset of the first entry to return.", "minimum", 0, "maximum",
				10000, "default", 0));

		Map<String, Object> entryProperties = new LinkedHashMap<>();
		entryProperties.put("id", schema("integer", null));
		entryProperties.put("globalId", schema("string", null));
		entryProperties.put("key", schema("string", null));
		entryProperties.put("value", schema("string", null));

		Map<String, Object> outputProperties = new LinkedHashMap<>();
		outputProperties.put("datasource", DatasourceInfo.jsonSchema());
		outputProperties.put("total", schema("integer", "Number of entries."));
		outputProperties.put("entries", schema("array", null, "items", schema("object", null, "properties",
				entryProperties)));
		outputProperties.put("constructRefs", schema("array", "Constructs using the datasource.", "items",
				objectRefSchema(null)));

		return Tool.builder().name("get_datasource").title("Get datasource")
				.description("Loads one datasource with its entries, so you can see the option keys and values a "
						+ "select part built on it will offer. Use it to check that a datasource actually contains the "
						+ "options the construct needs before wiring a select part to it.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of(ARG_ID))
						.additionalProperties(false).build())
				.outputSchema(schema("object", null, "properties", outputProperties, "required", List.of(
						"datasource")))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		int id = Args.id(arguments, ARG_ID);
		boolean includeEntries = booleanArg(arguments, ARG_INCLUDE_ENTRIES, true);
		int size = intArg(arguments, ARG_SIZE, 1, 200, 25);
		int from = intArg(arguments, ARG_FROM, 0, 10000, 0);

		DatasourceResource resource = RestPermissions.guard(DatasourceResource.class, new DatasourceResourceImpl());
		String datasourceId = Integer.toString(id);
		DatasourceLoadResponse response = resource.get(datasourceId);
		requireOk(response, "Datasource %d could not be loaded".formatted(id));
		List<DatasourceEntryModel> entries = ListResponses.items(resource.listEntries(datasourceId));
		List<Construct> constructs = ListResponses.items(resource.constructs(datasourceId, new SortParameterBean(),
				new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean()));

		return result(DatasourceInfo.of(response.getDatasource(), null), entries, constructs, includeEntries,
				Slice.of(from, size, entries.size()));
	}

	/**
	 * Build the result
	 * @param datasource datasource
	 * @param entries all entries
	 * @param constructs constructs using the datasource
	 * @param includeEntries whether to return the entries
	 * @param slice requested slice of the entries
	 * @return result
	 */
	static Result result(DatasourceInfo datasource, List<DatasourceEntryModel> entries, List<Construct> constructs,
			boolean includeEntries, Slice slice) {
		List<Entry> items = includeEntries ? slice.apply(entries).stream()
				.map(entry -> new Entry(entry.getId(), entry.getGlobalId(), entry.getKey(), entry.getValue())).toList()
				: null;
		return new Result(datasource, entries.size(), items, constructs.stream().map(ObjectRef::forConstruct)
				.toList());
	}
}
