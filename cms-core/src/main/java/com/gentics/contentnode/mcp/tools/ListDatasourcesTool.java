package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Datasource;
import com.gentics.contentnode.rest.model.DatasourceType;
import com.gentics.contentnode.rest.resource.DatasourceResource;
import com.gentics.contentnode.rest.resource.impl.DatasourceResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the datasources ({@link DatasourceResourceImpl#list}, which needs the datasource admin view permission and
 * lists the datasources the caller can view). The datasources are fetched unpaged, filtered by type and sliced; the
 * entries of the slice are counted in an own transaction.
 */
public class ListDatasourcesTool extends AbstractMcpTool {
	static final String ARG_TYPE = "type";

	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	/**
	 * A datasource. Fields that are not set are omitted on serialization.
	 * @param id ID
	 * @param globalId global ID
	 * @param name name
	 * @param type type
	 * @param entryCount number of entries, only in lists
	 */
	@JsonInclude(Include.NON_NULL)
	public record DatasourceInfo(int id, String globalId, String name, DatasourceType type, Integer entryCount) {
		/**
		 * Map the REST datasource
		 * @param datasource REST datasource
		 * @param entryCount number of entries, may be null
		 * @return datasource info
		 */
		static DatasourceInfo of(Datasource datasource, Integer entryCount) {
			return new DatasourceInfo(datasource.getId(), datasource.getGlobalId(), datasource.getName(),
					datasource.getType(), entryCount);
		}

		/**
		 * Build the output schema. Must be kept in sync with the components.
		 * @return schema
		 */
		static Map<String, Object> jsonSchema() {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("id", schema("integer", null));
			properties.put("globalId", schema("string", null));
			properties.put("name", schema("string", null));
			properties.put("type", schema("string", null, "enum", List.of("STATIC", "SITEMINDER")));
			properties.put("entryCount", schema("integer", "Number of entries."));
			return schema("object", null, "properties", properties, "required", List.of("id"));
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> properties = ListArgs.schemaProperties("Optional case-insensitive filter on the name.",
				"datasource", "datasources", LIMITS);
		properties.put(ARG_TYPE, schema("string", "Only datasources of this type.", "enum", List.of("STATIC",
				"SITEMINDER")));

		return Tool.builder().name("list_datasources").title("List datasources")
				.description("Lists the datasources in this CMS. A datasource supplies the options of a select part, "
						+ "so call this to find the datasourceId a select part needs (type id 29 or 30) before "
						+ "create_construct. Datasources cannot be created or changed through this server; if none "
						+ "fits, that is a task for a human in the CMS.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties).required(List.of())
						.additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(DatasourceInfo.jsonSchema(), "datasources"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);
		String type = stringArg(arguments, ARG_TYPE, 0, 20);
		DatasourceType datasourceType;
		try {
			datasourceType = type != null ? DatasourceType.valueOf(type) : null;
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Argument '%s' must be STATIC or SITEMINDER".formatted(ARG_TYPE));
		}

		List<Datasource> datasources = ListResponses.items(RestPermissions.guard(DatasourceResource.class,
				new DatasourceResourceImpl()).list(new SortParameterBean(), new FilterParameterBean().setQuery(args
						.query()), new PagingParameterBean()));
		List<Datasource> matching = datasources.stream()
				.filter(datasource -> datasourceType == null || datasource.getType() == datasourceType).toList();
		Slice slice = args.slice(matching.size());

		List<DatasourceInfo> items = new ArrayList<>();
		try (Trx trx = ContentNodeHelper.trx()) {
			for (Datasource datasource : slice.apply(matching)) {
				com.gentics.contentnode.object.Datasource object = trx.getTransaction()
						.getObject(com.gentics.contentnode.object.Datasource.class, datasource.getId());
				items.add(DatasourceInfo.of(datasource, object != null ? object.getEntries().size() : null));
			}
			trx.success();
		}
		return ListResult.of(slice, items);
	}
}
