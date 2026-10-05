package com.gentics.contentnode.mcp.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.ListResponses;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.devtools.Package;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.devtools.PackageResource;
import com.gentics.contentnode.rest.resource.impl.devtools.PackageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the devtools packages ({@link PackageResourceImpl#list}, which needs the view permission on devtools
 * packages). The REST API refuses the call when the devtools feature is off, direct calls do not, so the tool checks
 * the feature ({@link #checkDevtools}). The packages are fetched unpaged and sliced.
 */
public class ListPackagesTool extends AbstractMcpTool {
	/**
	 * Limits of the list arguments
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(200, 25, 200, 10000);

	/**
	 * Number of objects of each type in a package
	 * @param constructs constructs
	 * @param templates templates
	 * @param datasources datasources
	 * @param objectProperties object properties
	 * @param crFragments content repository fragments
	 * @param contentRepositories content repositories
	 */
	public record Counts(int constructs, int templates, int datasources, int objectProperties, int crFragments,
			int contentRepositories) {
	}

	/**
	 * A package in the list. Fields that are not set are omitted on serialization.
	 * @param name name
	 * @param description description
	 * @param counts number of objects per type
	 */
	@JsonInclude(Include.NON_NULL)
	public record PackageInfo(String name, String description, Counts counts) {
		/**
		 * Map the REST package
		 * @param pkg REST package
		 * @return package info
		 */
		static PackageInfo of(Package pkg) {
			return new PackageInfo(pkg.getName(), pkg.getDescription(), new Counts(pkg.getConstructs(),
					pkg.getTemplates(), pkg.getDatasources(), pkg.getObjectProperties(), pkg.getCrFragments(),
					pkg.getContentRepositories()));
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> countProperties = new LinkedHashMap<>();
		for (String type : List.of("constructs", "templates", "datasources", "objectProperties", "crFragments",
				"contentRepositories")) {
			countProperties.put(type, schema("integer", null));
		}
		Map<String, Object> itemProperties = new LinkedHashMap<>();
		itemProperties.put("name", schema("string", null));
		itemProperties.put("description", schema("string", null));
		itemProperties.put("counts", schema("object", "Number of objects per type.", "properties", countProperties));

		return Tool.builder().name("list_packages").title("List packages")
				.description("Lists the devtools packages, so you can pick a target for add_construct_to_package. "
						+ "Packages are bundles of constructs, templates and other definitions used to promote work "
						+ "between environments. Needs the devtools feature.")
				.inputSchema(JsonSchema.builder().type("object")
						.properties(ListArgs.schemaProperties("Optional case-insensitive filter on the name.",
								"package", "packages", LIMITS))
						.required(List.of()).additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(schema("object", null, "properties", itemProperties, "required",
						List.of("name")), "packages"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		ListArgs args = ListArgs.of(arguments, LIMITS);
		checkDevtools();

		List<Package> packages = ListResponses.items(RestPermissions.guard(PackageResource.class,
				new PackageResourceImpl()).list(new FilterParameterBean().setQuery(args.query()),
						new SortParameterBean(), new PagingParameterBean()));
		Slice slice = args.slice(packages.size());
		return ListResult.of(slice, slice.apply(packages).stream().map(PackageInfo::of).toList());
	}

	/**
	 * Check that the devtools feature is activated, which the REST API checks in a request filter
	 * @throws RestMappedException with {@link ResponseCode#NOTLICENSED} if it is not
	 * @throws NodeException
	 */
	static void checkDevtools() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			boolean activated = Feature.DEVTOOLS.isActivated();
			trx.success();
			if (!activated) {
				throw new RestMappedException("The devtools feature is not activated in this CMS")
						.setResponseCode(ResponseCode.NOTLICENSED);
			}
		}
	}
}
