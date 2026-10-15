package com.gentics.contentnode.mcp.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.mcp.AbstractMcpTool;
import com.gentics.contentnode.mcp.model.FolderItem;
import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.util.Args;
import com.gentics.contentnode.mcp.util.ListArgs;
import com.gentics.contentnode.mcp.util.RestPermissions;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.ItemListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;

import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Lists the pages, files, images and subfolders of a folder. Pages, files and images come unpaged from
 * {@link FolderResourceImpl#getItems}, folders from {@link FolderResourceImpl#getFolders}; both filter by
 * {@code ObjectPermission.view}. The tool sorts and slices the merged list itself, because {@code getItems} pages
 * every type before merging and cannot sort the merged list by publish date or filename. Forms are not supported.
 */
public class ListFolderItemsTool extends AbstractMcpTool {
	static final String ARG_FOLDER_ID = "folderId";

	static final String ARG_NODE_ID = "nodeId";

	static final String ARG_TYPES = "types";

	static final String ARG_RECURSIVE = "recursive";

	static final String ARG_LANGUAGE = "language";

	static final String ARG_FILTERS = "filters";

	static final String ARG_SORT = "sort";

	/**
	 * Item types, as in the contract
	 */
	static final List<String> TYPES = List.of("page", "file", "image", "folder", "form");

	/**
	 * Sort fields, as in the contract
	 */
	static final List<String> SORT_FIELDS = List.of("name", "edate", "cdate", "pdate", "filename");

	/**
	 * Filters on timestamps
	 */
	static final List<String> TIMESTAMP_FILTERS = List.of("editedSince", "editedBefore", "createdSince",
			"createdBefore", "publishedSince", "publishedBefore");

	/**
	 * Filters that only publishable items (pages, files, images) can match
	 */
	static final List<String> PUBLISH_FILTERS = List.of("online", "modified", "publishedSince", "publishedBefore");

	/**
	 * Limits of the paging arguments (the tool has no query)
	 */
	static final ListArgs.Limits LIMITS = new ListArgs.Limits(0, 25, 200, 10000);

	/**
	 * Validated arguments of a call
	 * @param folderId folder ID
	 * @param nodeId node ID, may be null
	 * @param types requested item types
	 * @param recursive whether to include subfolders
	 * @param language page language, may be null
	 * @param publishParams filters as parameter bean
	 * @param search name filter, may be null
	 * @param sortField sort field
	 * @param descending whether to sort descending
	 * @param paging paging arguments
	 * @param filtersPublishState whether a filter only publishable items can match is set
	 */
	record Request(int folderId, Integer nodeId, List<String> types, boolean recursive, String language,
			PublishableParameterBean publishParams, String search, String sortField, boolean descending,
			ListArgs paging, boolean filtersPublishState) {
		/**
		 * Parse and validate the arguments, without accessing the CMS
		 * @param arguments arguments
		 * @return request
		 * @throws IllegalArgumentException if an argument is invalid
		 */
		static Request of(Map<String, Object> arguments) {
			int folderId = Args.id(arguments, ARG_FOLDER_ID);
			Integer nodeId = intArg(arguments, ARG_NODE_ID, 1, Integer.MAX_VALUE);
			List<String> types = Args.enumList(arguments, ARG_TYPES, TYPES, List.of("page"));
			if (types.isEmpty()) {
				throw new IllegalArgumentException("Argument '%s' must not be empty".formatted(ARG_TYPES));
			}
			if (types.contains("form")) {
				throw new IllegalArgumentException(
						"Listing forms is not supported, omit 'form' from '%s'".formatted(ARG_TYPES));
			}
			boolean recursive = booleanArg(arguments, ARG_RECURSIVE, false);
			String language = nullIfBlank(stringArg(arguments, ARG_LANGUAGE, 0, 10));

			Map<String, Object> filters = mapArg(arguments, ARG_FILTERS);
			PublishableParameterBean publishParams = new PublishableParameterBean();
			if (filters.containsKey("online")) {
				publishParams.online = booleanArg(filters, "online", false);
			}
			if (filters.containsKey("modified")) {
				publishParams.modified = booleanArg(filters, "modified", false);
			}
			publishParams.editedSince = intArg(filters, "editedSince", 0, Integer.MAX_VALUE, 0);
			publishParams.editedBefore = intArg(filters, "editedBefore", 0, Integer.MAX_VALUE, 0);
			publishParams.createdSince = intArg(filters, "createdSince", 0, Integer.MAX_VALUE, 0);
			publishParams.createdBefore = intArg(filters, "createdBefore", 0, Integer.MAX_VALUE, 0);
			publishParams.publishedSince = intArg(filters, "publishedSince", 0, Integer.MAX_VALUE, 0);
			publishParams.publishedBefore = intArg(filters, "publishedBefore", 0, Integer.MAX_VALUE, 0);
			Integer creatorId = intArg(filters, "creatorId", 1, Integer.MAX_VALUE);
			if (creatorId != null) {
				publishParams.creatorIds = List.of(creatorId);
			}
			Integer editorId = intArg(filters, "editorId", 1, Integer.MAX_VALUE);
			if (editorId != null) {
				publishParams.editorIds = List.of(editorId);
			}
			String search = nullIfBlank(stringArg(filters, "nameContains", 0, 200));
			boolean filtersPublishState = PUBLISH_FILTERS.stream().anyMatch(filters::containsKey);

			Map<String, Object> sort = mapArg(arguments, ARG_SORT);
			String sortField = "name";
			boolean descending = false;
			if (!sort.isEmpty()) {
				sortField = stringArg(sort, "field", 1, 16);
				if (!SORT_FIELDS.contains(sortField)) {
					throw new IllegalArgumentException(
							"Argument 'sort.field' must be one of %s, but was '%s'".formatted(SORT_FIELDS, sortField));
				}
				String order = Optional.ofNullable(stringArg(sort, "order", 1, 4)).orElse("asc");
				if (!List.of("asc", "desc").contains(order)) {
					throw new IllegalArgumentException("Argument 'sort.order' must be 'asc' or 'desc'");
				}
				descending = order.equals("desc");
			}

			return new Request(folderId, nodeId, types, recursive, language, publishParams, search, sortField,
					descending, ListArgs.of(arguments, LIMITS), filtersPublishState);
		}

		/**
		 * Get an optional object argument
		 * @param arguments arguments
		 * @param name argument name
		 * @return value, empty if not supplied
		 */
		@SuppressWarnings("unchecked")
		private static Map<String, Object> mapArg(Map<String, Object> arguments, String name) {
			Object value = arguments.get(name);
			if (value == null) {
				return Map.of();
			}
			if (!(value instanceof Map<?, ?>)) {
				throw new IllegalArgumentException("Argument '%s' must be an object".formatted(name));
			}
			return (Map<String, Object>) value;
		}
	}

	@Override
	public Tool tool() {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("online", schema("boolean", null));
		filters.put("modified", schema("boolean", null));
		for (String timestamp : TIMESTAMP_FILTERS) {
			filters.put(timestamp, schema("integer", "Unix timestamp in seconds."));
		}
		filters.put("creatorId", schema("integer", null, "minimum", 1));
		filters.put("editorId", schema("integer", null, "minimum", 1));
		filters.put("nameContains", schema("string", null, "maxLength", 200));

		Map<String, Object> sort = new LinkedHashMap<>();
		sort.put("field", schema("string", null, "enum", SORT_FIELDS));
		sort.put("order", schema("string", null, "enum", List.of("asc", "desc"), "default", "asc"));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(ARG_FOLDER_ID, schema("integer", "ID of the folder.", "minimum", 1));
		properties.put(ARG_NODE_ID, schema("integer", "Node (channel) to list the folder in.", "minimum", 1));
		properties.put(ARG_TYPES, schema("array", "Item types to list. Forms are not supported.", "minItems", 1,
				"maxItems", 6, "uniqueItems", true, "items", schema("string", null, "enum", TYPES), "default",
				List.of("page")));
		properties.put(ARG_RECURSIVE, schema("boolean", "Include the items of all subfolders.", "default", false));
		properties.put(ARG_LANGUAGE, schema("string", "Language code of the pages.", "maxLength", 10));
		properties.put(ARG_FILTERS, schema("object", "Filters. Publish state filters exclude folders.",
				"additionalProperties", false, "properties", filters));
		ListArgs.schemaProperties(null, "item", "items", LIMITS).forEach((name, schema) -> {
			if (!name.equals(ListArgs.ARG_QUERY)) {
				properties.put(name, schema);
			}
		});
		properties.put(ARG_SORT, schema("object", null, "additionalProperties", false, "properties", sort, "required",
				List.of("field")));

		return Tool.builder().name("list_folder_items").title("List folder items")
				.description("Lists the pages, files, images, folders and forms in one folder, with edit and publish "
						+ "state. This is the exact inventory of a folder, unlike search_content which is ranked "
						+ "retrieval. Use it when the user names a folder, and to check whether a page already exists "
						+ "there before creating one.")
				.inputSchema(JsonSchema.builder().type("object").properties(properties)
						.required(List.of(ARG_FOLDER_ID)).additionalProperties(false).build())
				.outputSchema(ListResult.jsonSchema(FolderItem.jsonSchema(), "items"))
				.build();
	}

	@Override
	protected Object invoke(Map<String, Object> arguments, Optional<Session> session) throws Exception {
		Request request = Request.of(arguments);
		FolderResource folderResource = RestPermissions.guard(FolderResource.class, new FolderResourceImpl());
		String folderId = Integer.toString(request.folderId());

		List<ContentNodeItem> items = new ArrayList<>();
		List<ItemType> itemTypes = new ArrayList<>();
		for (String type : request.types()) {
			if (!type.equals("folder")) {
				itemTypes.add(ItemType.valueOf(type));
			}
		}
		if (!itemTypes.isEmpty()) {
			ItemListResponse response = folderResource.getItems(folderId, itemTypes, request.nodeId(), false, false,
					request.language(), true, inFolder(request), filter(request), new LegacySortParameterBean(),
					new LegacyPagingParameterBean(), request.publishParams());
			requireOk(response, "The items of folder %s could not be listed".formatted(folderId));
			items.addAll(response.getItems());
		}
		if (request.types().contains("folder") && !request.filtersPublishState()) {
			FolderListParameterBean folderListParams = new FolderListParameterBean();
			folderListParams.nodeId = request.nodeId();
			LegacyFolderListResponse response = folderResource.getFolders(folderId, null, false, inFolder(request),
					folderListParams, filter(request), new LegacySortParameterBean(), new LegacyPagingParameterBean(),
					request.publishParams(), new WastebinParameterBean());
			requireOk(response, "The folders of folder %s could not be listed".formatted(folderId));
			items.addAll(response.getFolders());
		}

		return result(items, request);
	}

	/**
	 * Sort and slice the items
	 * @param items REST items
	 * @param request request
	 * @return result
	 */
	static ListResult<FolderItem> result(List<ContentNodeItem> items, Request request) {
		List<ContentNodeItem> sorted = new ArrayList<>(items);
		Comparator<ContentNodeItem> comparator = comparator(request.sortField());
		sorted.sort(request.descending() ? comparator.reversed() : comparator);

		Slice slice = request.paging().slice(sorted.size());
		List<FolderItem> page = new ArrayList<>();
		for (ContentNodeItem item : slice.apply(sorted)) {
			page.add(FolderItem.of(item, request.nodeId()));
		}
		return ListResult.of(slice, page);
	}

	/**
	 * Get the comparator for a sort field, ties broken by ID
	 * @param field sort field
	 * @return comparator
	 */
	static Comparator<ContentNodeItem> comparator(String field) {
		Comparator<ContentNodeItem> comparator = switch (field) {
		case "edate" -> Comparator.comparingInt(ContentNodeItem::getEdate);
		case "cdate" -> Comparator.comparingInt(ContentNodeItem::getCdate);
		case "pdate" -> Comparator.comparingInt(item -> item instanceof Page page ? page.getPdate() : 0);
		case "filename" -> Comparator.comparing(ListFolderItemsTool::fileName, String.CASE_INSENSITIVE_ORDER);
		default -> Comparator.comparing(item -> item.getName() != null ? item.getName() : "",
				String.CASE_INSENSITIVE_ORDER);
		};
		return comparator.thenComparing(ContentNodeItem::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
	}

	/**
	 * Get the filename of an item: the page's filename, otherwise the name
	 * @param item item
	 * @return filename, never null
	 */
	private static String fileName(ContentNodeItem item) {
		String name = item instanceof Page page ? page.getFileName() : item.getName();
		return name != null ? name.toLowerCase(Locale.ROOT) : "";
	}

	/**
	 * Create the folder parameter of a request
	 * @param request request
	 * @return parameter bean
	 */
	private static InFolderParameterBean inFolder(Request request) {
		InFolderParameterBean inFolder = new InFolderParameterBean();
		inFolder.folderId = Integer.toString(request.folderId());
		inFolder.recursive = request.recursive();
		return inFolder;
	}

	/**
	 * Create the name filter of a request
	 * @param request request
	 * @return parameter bean
	 */
	private static LegacyFilterParameterBean filter(Request request) {
		LegacyFilterParameterBean filter = new LegacyFilterParameterBean();
		filter.search = request.search();
		return filter;
	}
}
