package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.getNode;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.reduceList;
import static com.gentics.contentnode.rest.util.RequestParamHelper.embeddedParameterContainsAttribute;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.collections.ListUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.exception.EntityInUseException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.AnyChannelTrx;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.PublishData;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ConstructCategory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.UserLanguage;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.request.ConstructSortAttribute;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.response.ConstructCategoryListResponse;
import com.gentics.contentnode.rest.model.response.ConstructCategoryLoadResponse;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.ConstructLoadResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PagedConstructListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.parameter.ConstructCategoryParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ConstructParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.AbstractNodeObjectFilter;
import com.gentics.contentnode.rest.util.AndFilter;
import com.gentics.contentnode.rest.util.Filter;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.NodeObjectFilter;
import com.gentics.contentnode.rest.util.OrFilter;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.PermissionFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.rest.util.StringFilter;
import com.gentics.contentnode.rest.util.StringFilter.Case;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Resource for loading construct information
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/construct")
@Authenticated
public class ConstructResourceImpl implements ConstructResource {

	@Override
	@GET
	public PagedConstructListResponse list(
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam ConstructParameterBean constructFilter,
			@BeanParam PermsParameterBean perms,
			@BeanParam EmbedParameterBean embed
	) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			TransactionManager.getCurrentTransaction().setPublishData(new PublishData(true, false, false, true));

			if (paging == null || paging.pageSize != 0) {
				prefillDictionaries(true, true, embeddedParameterContainsAttribute(embed, "category"));
			}

			List<com.gentics.contentnode.object.Construct> constructs;

			if (constructFilter != null && constructFilter.pageId != null) {
				constructs = fetchPageConstructs(constructFilter.pageId);
			} else if (constructFilter != null && constructFilter.nodeId != null) {
				constructs = fetchNodeConstructs(constructFilter.nodeId);
			} else {
				constructs = fetchSystemConstructs();
			}

			if (filter != null || constructFilter != null) {
				AndFilter objectFilter = buildFilter(new NodeObjectFilter[] {
						createChangeableFilter(constructFilter != null ? constructFilter.changeable : null),
						createConstructCategoryFilter(constructFilter != null ? constructFilter.categoryId : null),
						createConstructSearchFilter(filter != null ? filter.query : null),
						createPartTypeIdFilter(constructFilter != null ? constructFilter.partTypeId : null) });
				objectFilter.filter(constructs);
			}

			PagedConstructListResponse response = ListBuilder
					.from(constructs, com.gentics.contentnode.object.Construct.TRANSFORM2REST)
					.perms(permFunction(perms, ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "keyword", "name", "description"))
					.embed(embed, "category", com.gentics.contentnode.object.Construct.EMBED_CATEGORY)
					.page(paging).to(new PagedConstructListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/load/{constructId}")
	public ConstructLoadResponse load(
			@PathParam("constructId") final Integer id,
			@BeanParam EmbedParameterBean embed) throws NodeException {
			return get(id.toString(), embed);
	}

	/**
	 * Creates a NodeObjectFilter that will filter objects based on whether or
	 * not they can be changed.
	 *
	 * @param changeable If `true`, will create a filter that matches any
	 *                   object that can be changed, otherwise creates a filter
	 *                   that does the opposite.
	 * @return A filter.
	 */
	private static AbstractNodeObjectFilter createChangeableFilter(
			final Boolean changeable
			) {
		if (null == changeable) {
			return null;
		}
		return new AbstractNodeObjectFilter() {

			/* (non-Javadoc)
			 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
			 */
			public boolean matches(NodeObject object) throws NodeException {
				return (
						changeable == PermHandler.ObjectPermission.edit.checkObject(object)
						);
			}
		};
	}

	/**
	 * Creates a NodeObjectFilter that will filter objects based on whether or
	 * not they belong to a given construct category.
	 *
	 * @param categoryId The id of a construct category against which to tests
	 *                   each object's assigned category.
	 * @return A filter.
	 */
	private static AbstractNodeObjectFilter createConstructCategoryFilter(
			final Integer categoryId
			) {
		if (null == categoryId) {
			return null;
		}
		return new AbstractNodeObjectFilter() {

			/* (non-Javadoc)
			 * @see com.gentics.contentnode.rest.util.NodeObjectFilter#matches(com.gentics.lib.base.object.NodeObject)
			 */
			public boolean matches(NodeObject object) throws NodeException {
				if (object instanceof com.gentics.contentnode.object.Construct) {
					com.gentics.contentnode.object.ConstructCategory category = ((com.gentics.contentnode.object.Construct) object).getConstructCategory();

					return (
							(null == category) ? false : categoryId.equals(category.getId())
							);
				}
				return false;
			}
		};
	}

	/**
	 * Creates a NodeObjectFilter that will filter objects based on whether or
	 * not their keyword, name, or description contains the given search term.
	 *
	 * @param term The search term.
	 * @return A filter.
	 */
	private static OrFilter createConstructSearchFilter(final String term) throws NodeException {
		if (ObjectTransformer.isEmpty(term)) {
			return null;
		}

		try {
			String search = "%" + term + "%";
			OrFilter filter = new OrFilter();

			// search in keyword
			filter.addFilter(new StringFilter(search, com.gentics.contentnode.object.Construct.class.getMethod("getKeyword"), true, Case.INSENSITIVE));

			// search in name
			filter.addFilter(new StringFilter(search, com.gentics.contentnode.object.Construct.class.getMethod("getName"), true, Case.INSENSITIVE));

			// search in description
			filter.addFilter(new StringFilter(search, com.gentics.contentnode.object.Construct.class.getMethod("getDescription"), true, Case.INSENSITIVE));

			return filter;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Create filter for filtering construct having at least one part using one of the given part type IDs.
	 * Will return null, if partTypeId is null or empty
	 *
	 * @param partTypeId list of part type IDs
	 * @return filter (or null)
	 */
	private static NodeObjectFilter createPartTypeIdFilter(final List<Integer> partTypeId) {
		if (ObjectTransformer.isEmpty(partTypeId)) {
			return null;
		}

		return new AbstractNodeObjectFilter() {
			@Override
			public boolean matches(NodeObject object) throws NodeException {
				if (object instanceof com.gentics.contentnode.object.Construct) {
					return ((com.gentics.contentnode.object.Construct) object).getParts().stream().filter(p -> partTypeId.contains(p.getPartTypeId()))
							.findFirst().isPresent();
				}
				return false;
			}
		};
	}

	/**
	 * Joins a list of sub filters into a single AndFilter.
	 *
	 * The sub filters will be concatenated in the order that they were found in
	 * the `subFilters` argument, and all null references will be ignored.
	 *
	 * @param subFilters An arrays of ObjectNodeFilters.
	 * @return A filter.
	 */
	private static AndFilter buildFilter(final NodeObjectFilter[] subFilters) {
		AndFilter filter = new AndFilter();

		for (int i = 0; i < subFilters.length; i++) {
			if (null != subFilters[i]) {
				filter.addFilter(subFilters[i]);
			}
		}
		return filter;
	}

	/**
	 * Returns a tuple representing a range's start and indices, constrained to
	 * a given size.
	 *
	 * If a length of -1 is specified, then `size` will be the end index of the
	 * range.
	 *
	 * @param offset The offset index from which to begin the range.
	 * @param length The length of the range.
	 * @return The start index and end index of a range.
	 */
	private static int[] calculateRange(final int size,
			final int offset,
			final int length) {
		int from = Math.min(Math.max(0, offset), size);
		int to = (length < 0) ? size : Math.min(from + length, size);

		return new int[] { from, to};
	}

	/**
	 * Sorts a list of constructs.
	 *
	 * @param constructs The list of constructs to sort.
	 * @param sortBy The name of the field by which to sort the list.
	 * @param sortOrder The order in which to sort the elements. (Either
	 *                  ascending or descending.)
	 */
	private static void sortConstructs(
			List<com.gentics.contentnode.object.Construct> constructs,
			final ConstructSortAttribute sortBy,
			final SortOrder sortOrder
			) {
		if (null == sortBy) {
			return;
		}
		Collections.sort(constructs,
				new Comparator<com.gentics.contentnode.object.Construct>() {
			public int compare(
					com.gentics.contentnode.object.Construct construct1,
					com.gentics.contentnode.object.Construct construct2
					) {
				int compare = 0;

				switch (sortBy) {
				case category:
					try {
						com.gentics.contentnode.object.ConstructCategory category1 = construct1.getConstructCategory();
						com.gentics.contentnode.object.ConstructCategory category2 = construct2.getConstructCategory();

						if (category1 == null) {
							compare = (category2 == null) ? 0 : 1;
						} else if (category2 == null) {
							compare = -1;
						} else {
							compare = category1.getSortorder() - category2.getSortorder();
							if (compare == 0) {
								compare = StringUtils.mysqlLikeCompare(category1.getName().toString(), category2.getName().toString());
							}
						}
						break;
					} catch (NodeException e) {
						return 0;
					}

				case description:
					compare = StringUtils.mysqlLikeCompare(ObjectTransformer.getString(construct1.getDescription(), ""),
							ObjectTransformer.getString(construct2.getDescription(), ""));
					break;

				case keyword:
					compare = StringUtils.mysqlLikeCompare(construct1.getKeyword(), construct2.getKeyword());
					break;

				case name:
					compare = StringUtils.mysqlLikeCompare(ObjectTransformer.getString(construct1.getName(), ""),
							ObjectTransformer.getString(construct2.getName(), ""));
					break;

				default:
					return 0;
				}

				if (compare == 0 && sortBy != ConstructSortAttribute.name) {
					compare = StringUtils.mysqlLikeCompare(ObjectTransformer.getString(construct1.getName(), ""),
							ObjectTransformer.getString(construct2.getName(), ""));
				}

				if (sortOrder == SortOrder.desc || sortOrder == SortOrder.DESC) {
					compare = -compare;
				}

				return compare;
			}
		}); // Collections.sort()
	}

	/**
	 * Fetch all constructs available for the given Node.
	 *
	 * @param id The id of the node whose constructs are to be fetched.
	 * @return  A list of constructs.
	 */
	private List<com.gentics.contentnode.object.Construct> fetchNodeConstructs(final Integer id) throws NodeException {
		List<com.gentics.contentnode.object.Construct> constructs = new ArrayList<com.gentics.contentnode.object.Construct>();

		Node node = loadNode(Integer.toString(id));

		constructs.addAll(node.getConstructs());

		return constructs;
	}

	/**
	 * Load the node with given ID and check for existence and permission. The permission check will also check for permission to see at least a channel
	 * @param id node ID
	 * @return node
	 * @throws NodeException
	 */
	private Node loadNode(String id) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.getObject(Node.class, id, true);
		if (node == null) {
			throw new EntityNotFoundException(I18NHelper.get("node.notfound", id));
		}
		if (!checkViewPermission(node)) {
			throw new InsufficientPrivilegesException(I18NHelper.get("node.nopermission", id), node, PermType.read);
		}

		return node;
	}

	/**
	 * Check whether the user is allowed to see the given node or any of its channels.
	 * Currently, there are no real channel specific permissions, so this might be workarounded by giving a user
	 * the permission to see the channel, but not its master node. Therefore, in order to list the constructs for the given node,
	 * the user must be allowed to see either the node or any of its channels.
	 *
	 * @param node node
	 * @return true if the user is allowed to see either the node or any of its channels
	 * @throws NodeException
	 */
	private boolean checkViewPermission(Node node) throws NodeException {
		if (node == null) {
			return false;
		}
		if (PermHandler.ObjectPermission.view.checkObject(node.getFolder())) {
			return true;
		} else {
			Collection<Node> channels = node.getChannels();

			for (Node channel : channels) {
				if (checkViewPermission(channel)) {
					return true;
				}
			}

			return false;
		}
	}

	/**
	 * Fetch all constructs that are used by tags in the given page, or that are
	 * available to be used in new tags created in this page.
	 *
	 * @param id The id of the page whose constructs are to be fetched.
	 * @return  A list of constructs.
	 */
	private List<com.gentics.contentnode.object.Construct> fetchPageConstructs(final Integer id) throws NodeException {
		List<com.gentics.contentnode.object.Construct> constructs = new ArrayList<com.gentics.contentnode.object.Construct>();
		Transaction t = TransactionManager.getCurrentTransaction();
		Node channel = null;

		Page tmpPage = t.getObject(Page.class, id);
		if (tmpPage != null) {
			channel = tmpPage.getChannel();
		}

		Page page = null;
		// load page in scope of its own channel (for correct permission check)
		try (ChannelTrx cTrx = new ChannelTrx(channel)) {
			page = MiscUtils.load(Page.class, Integer.toString(id));
		}

		List<Tag> tags = new ArrayList<Tag>();

		tags.addAll(page.getContent().getContentTags().values());
		tags.addAll(page.getObjectTags().values());

		com.gentics.contentnode.object.Construct construct;

		for (Tag tag : tags) {
			construct = tag.getConstruct();
			if (!constructs.contains(construct)) {
				constructs.add(construct);
			}
		}

		Integer nodeId = ObjectTransformer.getInteger(page.getFolder().getNode().getId(), null);

		List<com.gentics.contentnode.object.Construct> nodeConstructs = fetchNodeConstructs(nodeId);

		for (com.gentics.contentnode.object.Construct nodeConstruct : nodeConstructs) {
			if (!constructs.contains(nodeConstruct)) {
				constructs.add(nodeConstruct);
			}
		}

		return constructs;
	}

	/**
	 * Fetch all constructs available in this Content.Node installation.
	 * @return  A list of constructs.
	 */
	private List<com.gentics.contentnode.object.Construct> fetchSystemConstructs() throws NodeException {
		List<com.gentics.contentnode.object.Construct> constructs = new ArrayList<com.gentics.contentnode.object.Construct>();

		final List<Integer> ids = new Vector<Integer>();

		DBUtils.executeStatement("SELECT id FROM construct", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet result) throws SQLException, NodeException {
				while (result.next()) {
					ids.add(result.getInt("id"));
				}
			}
		});

		// This will be an expensive operation on installations with many
		// constructs and the cache is not (yet) populated.
		constructs.addAll(TransactionManager.getCurrentTransaction().getObjects(com.gentics.contentnode.object.Construct.class, ids));

		// Because we must filter out those that are not assigned to any node
		// that the user may see
		PermissionFilter.get(ObjectPermission.view).filter(constructs);

		return constructs;
	}

	@Override
	@GET
	@Path("/list")
	public ConstructListResponse list(
			@QueryParam("skipCount") @DefaultValue("0")    final Integer skipCount,
			@QueryParam("maxItems")  @DefaultValue("-1")   final Integer maxItems,
			@QueryParam("search")                          final String search,
			@QueryParam("changeable")                      final Boolean changeable,
			@QueryParam("pageId")                          final Integer pageId,
			@QueryParam("nodeId")                          final Integer nodeId,
			@QueryParam("category")                        final Integer categoryId,
			@QueryParam("partTypeId")                      final List<Integer> partTypeId,
			@QueryParam("sortby")    @DefaultValue("name") final ConstructSortAttribute sortBy,
			@QueryParam("sortorder") @DefaultValue("asc")  final SortOrder sortOrder
			) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			TransactionManager.getCurrentTransaction().setPublishData(new PublishData(true, false, false, true));
			if (maxItems != 0) {
				prefillDictionaries(true, true, false);
			}

			List<com.gentics.contentnode.object.Construct> constructs;

			constructs = (null != pageId) ? fetchPageConstructs(pageId)
					: (null != nodeId) ? fetchNodeConstructs(nodeId) : fetchSystemConstructs();

			AndFilter filter = buildFilter(new NodeObjectFilter[] {
				createChangeableFilter(changeable), createConstructCategoryFilter(categoryId), createConstructSearchFilter(search), createPartTypeIdFilter(partTypeId)
			});
			filter.filter(constructs);

			int numItems = constructs.size();
			int[] range = calculateRange(numItems, skipCount, maxItems);

			sortConstructs(constructs, sortBy, sortOrder);
			reduceList(constructs, skipCount, maxItems);

			List<Construct> restConstructs = new ArrayList<Construct>(constructs.size());

			for (com.gentics.contentnode.object.Construct construct : constructs) {
				restConstructs.add(ModelBuilder.getConstruct(construct));
			}

			ConstructListResponse response = new ConstructListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched constructs"));

			response.setNumItems(numItems);
			response.setHasMoreItems(range[1] < numItems);
			response.setConstructs(restConstructs);

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/delete/{constructId}/{idOrKeyname}")
	public GenericResponse deletePart(
			@PathParam("constructId") final String constructId,
			@PathParam("idOrKeyname") final String idOrKeyname
	) throws NodeException {
		Transaction t        = null;
		Part part            = null;
		String logIdentifier = "{" + idOrKeyname + "}";
		com.gentics.contentnode.object.Construct construct = null;

		try (Trx trx = ContentNodeHelper.trx()) {
			t = TransactionManager.getCurrentTransaction();

			// Load the passed construct
			construct = t.getObject(com.gentics.contentnode.object.Construct.class, constructId, true);

			if (construct == null) {
				I18nString message = new CNI18nString("construct.notfound");
				message.setParameter("0", constructId);
				throw new EntityNotFoundException(message.toString());
			}

			// Check if the user has permission to delete a part in this construct
			if (!t.getPermHandler().canEdit(construct)) {
				I18nString message = new CNI18nString("construct.nopermission");
				message.setParameter("0", constructId);
				throw new InsufficientPrivilegesException(message.toString(), construct, PermType.update);
			}

			// First, check if keynameOrId is an integer
			if (StringUtils.isInteger(idOrKeyname)) {
				// If yes, try to load the object by this ID
				part = t.getObject(Part.class, idOrKeyname, true);

				if (part != null && !part.getConstructId().equals(construct.getId())) {
					// If the part doesn't belong to the passed constructId,
					// set it to null again.
					part = null;
				}
			}

			// If we didn't find the part by its ID, we maybe have luck
			// when finding a matching keyword
			if (part == null) {
				// Find the part with the specified keyname
				for (Part partItem : construct.getParts()) {
					if (partItem.getKeyname().equals(idOrKeyname)) {
						part = partItem;
						break;
					}
				}
			}

			if (part == null) {
				I18nString message = new CNI18nString("construct.part.notfound");
				message.setParameter("0", logIdentifier);
				throw new EntityNotFoundException(message.toString());
			}

			// Create a new, more detailed, log identifier
			logIdentifier = "{" + part.getKeyname() + ", " + part.getId() + "}";

			// And finally: delete it
			construct.getParts().remove(part);
			construct.save();

			trx.success();

			I18nString message = new CNI18nString("construct.part.delete.success");
			message.setParameter("0", logIdentifier);
			return new GenericResponse(new Message(Type.INFO, message.toString()),
					new ResponseInfo(ResponseCode.OK, message.toString()));
		}
	}

	@Override
	@POST
	public ConstructLoadResponse create(Construct construct, @QueryParam("nodeId") List<Integer> nodeIds) throws NodeException {
		if (construct == null) {
			throw new MissingFieldException("construct");
		}
		if (nodeIds == null || nodeIds.isEmpty()) {
			throw new MissingFieldException("nodeId");
		}
		if (construct.getNameI18n() == null && construct.getName() == null) {
			throw new RestMappedException(I18NHelper.get("name.i18n.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();

			for (Integer nodeId : nodeIds) {
				Folder folder = getNode(Integer.toString(nodeId), ObjectPermission.view).getFolder();
				if (!t.getPermHandler().canCreate(folder, com.gentics.contentnode.object.Construct.class, null)) {
					I18nString message = new CNI18nString("node.nopermission");
					message.setParameter("0", folder.getId().toString());
					throw new InsufficientPrivilegesException(message.toString(), null, null, com.gentics.contentnode.object.Construct.TYPE_CONSTRUCT, nodeId, PermType.create);
				}
			}

			com.gentics.contentnode.object.Construct object = com.gentics.contentnode.object.Construct.REST2NODE.apply(construct, t.createObject(com.gentics.contentnode.object.Construct.class));
			object.save();

			for (Integer nodeId : nodeIds) {
				Node node = getNode(Integer.toString(nodeId), ObjectPermission.view);
				node.addConstruct(object);
			}

			ConstructLoadResponse response = new ConstructLoadResponse(null, ResponseInfo.ok("Successfully created construct"));
			response.setConstruct(com.gentics.contentnode.object.Construct.TRANSFORM2REST.apply(t.getObject(object)));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public ConstructLoadResponse get(
			@PathParam("id") String id,
			@BeanParam EmbedParameterBean embed) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();

			com.gentics.contentnode.rest.model.Construct restConstruct = ModelBuilder.getConstruct(findConstruct(id, PermType.read, t));

			if (embeddedParameterContainsAttribute(embed, "category")) {
				com.gentics.contentnode.object.Construct.EMBED_CATEGORY.accept(restConstruct);
			}

			ConstructLoadResponse response = new ConstructLoadResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched construct"));
			response.setConstruct(restConstruct);

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/{id}")
	public ConstructLoadResponse update(@PathParam("id") String id, Construct model, @QueryParam("nodeId") List<Integer> nodeIds) throws NodeException {
		Transaction t        = null;
		com.gentics.contentnode.object.Construct construct = null;

		try (Trx trx = ContentNodeHelper.trx()) {
			t = TransactionManager.getCurrentTransaction();

			// Load the passed construct
			construct = findConstruct(id, PermType.update, t);
			construct = com.gentics.contentnode.object.Construct.REST2NODE.apply(model, construct);
			construct.save();

			if (nodeIds != null && !nodeIds.isEmpty()) {
				NodeObjectFilter permFilter = PermissionFilter.get(ObjectPermission.view);
				List<Node> nodes = trx.getTransaction().getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS));

				for (Node node: nodes) {
					if (!node.getConstructs().contains(construct) && !nodeIds.contains(node.getId())) {
						continue;
					}
					if (!permFilter.matches(node)) {
						I18nString message = new CNI18nString("folder.nopermission");
						message.setParameter("0", node.getFolder().getId().toString());
						throw new InsufficientPrivilegesException(message.toString(), null, null, com.gentics.contentnode.object.Construct.TYPE_CONSTRUCT, node.getId(), PermType.updateconstructs);
					}
					if (node.getConstructs().contains(construct) && !nodeIds.contains(node.getId())) {
						node.removeConstruct(construct);
					} else if (!node.getConstructs().contains(construct) && nodeIds.contains(node.getId())) {
						node.addConstruct(construct);
					}
				}
			}
			ConstructLoadResponse response = new ConstructLoadResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully updated construct"));
			com.gentics.contentnode.rest.model.Construct restConstruct = ModelBuilder.getConstruct(construct.reload());
			response.setConstruct(restConstruct);

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public GenericResponse delete(@PathParam("id") String id) throws NodeException {
		Transaction t        = null;
		com.gentics.contentnode.object.Construct construct = null;

		try (Trx trx = ContentNodeHelper.trx()) {
			t = TransactionManager.getCurrentTransaction();

			// Load the passed construct
			construct = findConstruct(id, PermType.delete, t);
			if (construct.isUsed()) {
				I18nString message = new CNI18nString("construct.cannotdelete.used");
				message.setParameter("0", I18NHelper.getName(construct));
				throw new EntityInUseException(message.toString());
			}
			construct.delete();
			trx.success();

			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully deleted construct"));
		}
	}

	@Override
	@GET
	@Path("/category")
	public ConstructCategoryListResponse listCategories(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed, @BeanParam ConstructCategoryParameterBean categoryFilters) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			prefillDictionaries(embeddedParameterContainsAttribute(embed, "constructs"),
					embeddedParameterContainsAttribute(embed, "constructs"), true);

			Set<Integer> ids = null;
			ids = DBUtils.select("SELECT id FROM construct_category", DBUtils.IDS);

			Map<String, String> fieldMap = new HashMap<>();
			fieldMap.put("sortOrder", "sortorder");

			ResolvableComparator<ConstructCategory> sorter = ResolvableComparator.get(sorting, fieldMap, "id", "globalId", "name", "sortOrder", "sortorder");
			if (sorter != null) {
				sorter.setNullsAsLast("sortOrder", "sortorder");
			}

			Consumer<com.gentics.contentnode.rest.model.ConstructCategory> embedder = ConstructCategory.EMBED_CONSTRUCTS;
			Filter<ConstructCategory> constructsFilter = null;
			List<com.gentics.contentnode.object.Construct> constructs = null;
			if (categoryFilters != null && categoryFilters.pageId != null) {
				constructs = fetchPageConstructs(categoryFilters.pageId);
			} else if (categoryFilters != null && categoryFilters.nodeId != null) {
				constructs = fetchNodeConstructs(categoryFilters.nodeId);
			}
			if (constructs != null) {
				final List<com.gentics.contentnode.object.Construct> finalList = constructs;
				constructsFilter = o -> !ListUtils.intersection(o.getConstructs(), finalList).isEmpty();
				embedder = restConstructCategory -> {
					Transaction t = TransactionManager.getCurrentTransaction();
					// set the constructs
					Map<String, com.gentics.contentnode.rest.model.Construct> tmp = new HashMap<String, com.gentics.contentnode.rest.model.Construct>();

					ConstructCategory constructCategory = t.getObject(ConstructCategory.class,
							restConstructCategory.getId());
					for (com.gentics.contentnode.object.Construct construct : constructCategory.getConstructs()) {
						if (finalList.contains(construct)) {
							tmp.put(construct.getKeyword(), ModelBuilder.getConstruct(construct));
						}
					}
					restConstructCategory.setConstructs(tmp);
				};
			}

			ConstructCategoryListResponse response = ListBuilder.from(trx.getTransaction().getObjects(ConstructCategory.class, ids), ConstructCategory.TRANSFORM2REST_SHALLOW)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(constructsFilter)
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name"))
					.embed(embed, "constructs", embedder)
					.sort(sorter)
					.page(paging).to(new ConstructCategoryListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/category/sortorder")
	public ConstructCategoryListResponse sortCategories(IdSetRequest categoryOrder) throws NodeException {
		List<String> categoryIds = categoryOrder.getIds();

		if (categoryIds == null || categoryIds.isEmpty()) {
			return listCategories(new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean(), new ConstructCategoryParameterBean());
		}

		List<ConstructCategory> categories = new ArrayList<>();

		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			int order = 1;

			for (String categoryId : categoryIds) {
				ConstructCategory category = MiscUtils.load(ConstructCategory.class, categoryId, ObjectPermission.edit);

				category = trx.getTransaction().getObject(category, true);
				category.setSortorder(order++);
				category.save();

				categories.add(category);
			}

			trx.success();

			ConstructCategoryListResponse response = ListBuilder
				.from(categories, ConstructCategory.TRANSFORM2REST)
				.to(new ConstructCategoryListResponse());

			return response;
		}
	}

	@Override
	@POST
	@Path("/category")
	public ConstructCategoryLoadResponse createCategory(com.gentics.contentnode.rest.model.ConstructCategory category) throws NodeException {
		if (category.getNameI18n() == null && category.getName() == null) {
			throw new RestMappedException(I18NHelper.get("name.i18n.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (category.getSortOrder() == null) {
				int defaultSortOrder = DBUtils.select(
					"SELECT MAX(sortorder) FROM construct_category",
					rs -> rs.next() ? rs.getInt(1) + 1 : 0);

				category.setSortOrder(defaultSortOrder);
			}

			if (!t.getPermHandler().canCreate(null, ConstructCategory.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("construct_category.nopermission"), null, null,
						ConstructCategory.TYPE_CONSTRUCT_CATEGORY, 0, PermType.create);
			}
			ConstructCategory object = ConstructCategory.REST2NODE.apply(category, t.createObject(ConstructCategory.class));
			object.save();

			ConstructCategoryLoadResponse response = new ConstructCategoryLoadResponse(null, ResponseInfo.ok("Successfully created object property category"));
			response.setConstruct(ConstructCategory.TRANSFORM2REST.apply(object.reload()));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/category/{id}")
	public ConstructCategoryLoadResponse getCategory(@PathParam("id") String categoryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canView(null, ConstructCategory.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("construct_category.nopermission"), null, null,
						ConstructCategory.TYPE_CONSTRUCT_CATEGORY, 0, PermType.read);
			}
			ConstructCategory category = MiscUtils.load(ConstructCategory.class, categoryId);
			ConstructCategoryLoadResponse response = new ConstructCategoryLoadResponse(null, ResponseInfo.ok("Successfully loaded object property category"));
			response.setConstruct(ConstructCategory.TRANSFORM2REST.apply(category));

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/category/{id}")
	public ConstructCategoryLoadResponse updateCategory(@PathParam("id") String categoryId, com.gentics.contentnode.rest.model.ConstructCategory category) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			ConstructCategory update = MiscUtils.load(ConstructCategory.class, categoryId, ObjectPermission.edit);

			update = ConstructCategory.REST2NODE.apply(category, trx.getTransaction().getObject(update, true));
			update.save();

			ConstructCategoryLoadResponse response = new ConstructCategoryLoadResponse(null, ResponseInfo.ok("Successfully updated object property category"));
			response.setConstruct(ConstructCategory.TRANSFORM2REST.apply(update.reload()));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/category/{id}")
	public GenericResponse deleteCategory(@PathParam("id") String categoryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			ConstructCategory toDelete = MiscUtils.load(ConstructCategory.class, categoryId, ObjectPermission.delete);
			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				List<com.gentics.contentnode.object.Construct> toDeleteConstructs = toDelete.getConstructs();
				if (!toDeleteConstructs.isEmpty()) {
					CNI18nString message = new CNI18nString("resource.cannotdelete");
					message.setParameter("0", "Construct Category");
					message.setParameter("1", toDelete.getName());
					message.setParameter("2", "Construct");
					message.setParameter("3", toDeleteConstructs.stream().map(c -> c.getName().toString()).collect(Collectors.joining(", ")));
					throw new EntityInUseException(message.toString());
				}
			}
			toDelete.delete();

			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully deleted object property category"));
		}
	}

	@POST
	@Path("/link/nodes")
	@Override
	public GenericResponse link(BulkLinkUpdateRequest request) throws NodeException {
		return changeLinkStatus(request, true);
	}

	@POST
	@Path("/unlink/nodes")
	@Override
	public GenericResponse unlink(BulkLinkUpdateRequest request) throws NodeException {
		return changeLinkStatus(request, false);
	}

	@Override
	@GET
	@Path("/{id}/nodes")
	public NodeList listConstructNodes(@PathParam("id") String constructId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canView(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.read);
			}
			com.gentics.contentnode.object.Construct construct = findConstruct(constructId, PermType.read, t);
			List<com.gentics.contentnode.rest.model.Node> constructs = new ArrayList<>(construct.getNodes().size());
			for (Node node : construct.getNodes()) {
				if (checkViewPermission(node)) {
					constructs.add(com.gentics.contentnode.object.Node.TRANSFORM2REST.apply(node));
				}
			}
			NodeList response = new NodeList();
			response.setResponseInfo(ResponseInfo.ok("Successfully loaded nodes, that are linked to the construct"));
			response.setItems(constructs);

			trx.success();
			return response;
		}
	}

	private GenericResponse changeLinkStatus(BulkLinkUpdateRequest request, boolean add) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			for (Integer nodeId : request.getIds()) {
				Node node = loadNode(Integer.toString(nodeId));

				for (String constructId : request.getTargetIds()) {
					com.gentics.contentnode.object.Construct update = findConstruct(constructId, PermType.update, t);
					if (add) {
						node.addConstruct(update);
					} else {
						node.removeConstruct(update);
					}
				}
			}
			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully " + (add ? "" : "un") + "linked constructs against nodes"));
		}
	}

	protected com.gentics.contentnode.object.Construct findConstruct(String id, PermType permType, Transaction t) throws ReadOnlyException, NodeException {
		com.gentics.contentnode.object.Construct construct = null;

		// Load the passed construct
		construct = t.getObject(com.gentics.contentnode.object.Construct.class, id, true);

		if (construct == null) {
			I18nString message = new CNI18nString("construct.notfound");
			message.setParameter("0", id);
			throw new EntityNotFoundException(message.toString());
		}

		Function<com.gentics.contentnode.object.Construct, Boolean> permTest;

		switch (permType) {
		case read:
			permTest = t.getPermHandler()::canView;
			break;
		case update:
			permTest = t.getPermHandler()::canEdit;
			break;
		case delete:
			permTest = t.getPermHandler()::canDelete;
			break;
		default:
			throw new IllegalArgumentException("Unsupported permission type: " + permType);
		}

		// Check if the user has permission to process this construct
		if (!permTest.apply(construct)) {
			I18nString message = new CNI18nString("construct.nopermission");
			message.setParameter("0", id);
			throw new InsufficientPrivilegesException(message.toString(), construct, permType);
		}

		return construct;
	}

	protected Part findPart(com.gentics.contentnode.object.Construct construct, String idOrKeyname, Transaction t) throws ReadOnlyException, NodeException {
		Part part = null;

		// First, check if keynameOrId is an integer
		if (StringUtils.isInteger(idOrKeyname)) {
			// If yes, try to load the object by this ID
			part = t.getObject(Part.class, idOrKeyname, true);

			if (part != null && !part.getConstructId().equals(construct.getId())) {
				// If the part doesn't belong to the passed constructId,
				// set it to null again.
				part = null;
			}
		}

		// If we didn't find the part by its ID, we maybe have luck
		// when finding a matching keyword
		if (part == null) {
			// Find the part with the specified keyname
			for (Part partItem : construct.getParts()) {
				if (partItem.getKeyname().equals(idOrKeyname)) {
					part = partItem;
					break;
				}
			}
		}
		return part;
	}

	/**
	 * Prefill the dictionaries for all languages with the translations for all constructs,parts,categories
	 * @param construct true to prefill for all constructs
	 * @param part true to prefill for all parts
	 * @param category true to prefill for all categories
	 * @throws NodeException
	 */
	protected void prefillDictionaries(boolean construct, boolean part, boolean category) throws NodeException {
		for (UserLanguage language : UserLanguageFactory.getActive()) {
			try (LangTrx langTrx = new LangTrx(language)) {
				// Pre-fill the i18n dictionary with all translated names and
				// description, so that it will not be necessary to make an
				// additional SQL query for each a construct object is initialized
				CNDictionary.prefillDictionary("construct", "name_id");
				CNDictionary.prefillDictionary("construct", "description_id");

				if (part) {
					CNDictionary.prefillDictionary("part", "name_id");
				}
				if (category) {
					CNDictionary.prefillDictionary("construct_category", "name_id");
				}
			}
		}
	}
}
