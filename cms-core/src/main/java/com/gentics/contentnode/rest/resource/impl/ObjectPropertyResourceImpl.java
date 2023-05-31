package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.load;
import static com.gentics.contentnode.rest.util.RequestParamHelper.embeddedParameterContainsAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.gentics.contentnode.job.DeleteJob;
import org.apache.commons.lang.StringUtils;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.EntityInUseException;
import com.gentics.contentnode.exception.MissingFieldException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.AnyChannelTrx;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.model.ObjectPropertyCategory;
import com.gentics.contentnode.rest.model.ObjectPropertyType;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.ObjectPropertyCategoryListResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyCategoryLoadResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyListResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.ObjectPropertyResource;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ObjectPropertyParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.PermFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Resource implementation of {@link ObjectPropertyResource}
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/objectproperty")
@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
@RequiredPerm(type = ObjectTagDefinition.TYPE_OBJTAG_DEFS, bit = PermHandler.PERM_VIEW)
public class ObjectPropertyResourceImpl implements ObjectPropertyResource {

	/** The prefix for all object property keywords. */
	private static final String KEYWORD_PREFIX = "object.";
	/** The pattern all object property keywords must match (letters, digits, '-' and '_' are allowed). */
	private static final Pattern KEYWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,255}$");

	@GET
	@Override
	public ObjectPropertyListResponse list(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam ObjectPropertyParameterBean typeFilter, @BeanParam EmbedParameterBean embed) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Set<Integer> ids = null;
			if (typeFilter != null && !ObjectTransformer.isEmpty(typeFilter.types)) {
				String sql = String.format("SELECT id FROM objtag WHERE obj_id = 0 AND obj_type IN (%s)", StringUtils.repeat("?", ",", typeFilter.types.size()));
				ids = DBUtils.select(sql, stmt -> {
					int index = 1;
					for (ObjectPropertyType type : typeFilter.types) {
						stmt.setInt(index++, type.getCode());
					}
				}, DBUtils.IDS);
			} else {
				ids = DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS);
			}

			ResolvableComparator<ObjectTagDefinition> sorter = ResolvableComparator.get(sorting, "id", "globalId", "name", "description", "keyword", "type", "required",
					"inheritable", "syncContentset", "syncChannelset", "syncVariants", "construct.name",
					"category.name", "category.sortorder");
			if (sorter != null) {
				sorter.setNullsAsLast("category.sortorder");
			}

			ObjectPropertyListResponse response = ListBuilder.from(trx.getTransaction().getObjects(ObjectTagDefinition.class, ids), ObjectTagDefinition.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "description", "keyword"))
					.sort(sorter)
					.embed(embed, "construct", ObjectTagDefinition.EMBED_CONSTRUCT)
					.embed(embed, "category", ObjectTagDefinition.EMBED_CATEGORY)
					.page(paging).to(new ObjectPropertyListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	public ObjectPropertyLoadResponse create(ObjectProperty objectProperty) throws NodeException {
		if (objectProperty == null) {
			throw new RestMappedException(I18NHelper.get("objectproperty.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}

		// Remove the "object." prefix for the validity check (also handles null).
		String keyword = StringUtils.removeStart(objectProperty.getKeyword(), KEYWORD_PREFIX);

		if (StringUtils.isBlank(keyword)) {
			throw new RestMappedException(I18NHelper.get("keyword.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}

		keyword = keyword.trim();

		if (!KEYWORD_PATTERN.matcher(keyword).matches()) {
			throw new RestMappedException(I18NHelper.get("keyword.invalid"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}

		if (objectProperty.getType() == null) {
			throw new RestMappedException(I18NHelper.get("type.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}

		// Update the keyword from the request to make sure it starts with the "object." prefix.
		objectProperty.setKeyword(KEYWORD_PREFIX + keyword);

		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();
			checkObjectPropertyExists(objectProperty);

			if (!t.getPermHandler().canCreate(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.create);
			}
			ObjectTagDefinition nodeObjectTagDefinition = ObjectTagDefinition.REST2NODE.apply(objectProperty, t.createObject(ObjectTagDefinition.class));
			nodeObjectTagDefinition.save();

			ObjectPropertyLoadResponse response = new ObjectPropertyLoadResponse(ObjectTagDefinition.TRANSFORM2REST.apply(nodeObjectTagDefinition.reload()),
					ResponseInfo.ok("Successfully created objectProperty"));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public ObjectPropertyLoadResponse get(
			@PathParam("id") String objectPropertyId,
			@BeanParam EmbedParameterBean embed) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			ObjectTagDefinition objectPropertyNode =  load(ObjectTagDefinition.class, objectPropertyId);
			ObjectProperty objectPropertyDto = ObjectTagDefinition.TRANSFORM2REST.apply(objectPropertyNode);

			if (embeddedParameterContainsAttribute(embed, "construct")) {
				ObjectTagDefinition.EMBED_CONSTRUCT.accept(objectPropertyDto);
			}
			if (embeddedParameterContainsAttribute(embed, "category")) {
				ObjectTagDefinition.EMBED_CATEGORY.accept(objectPropertyDto);
			}

			ObjectPropertyLoadResponse response = new ObjectPropertyLoadResponse(
					objectPropertyDto,
					ResponseInfo.ok("Successfully loaded objectProperty"));
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/constructs")
	public ConstructListResponse listObjectPropertyConstructs(@PathParam("id") String objectPropertyId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canView(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.read);
			}
			ObjectTagDefinition objectProperty = load(ObjectTagDefinition.class, objectPropertyId);
			if (objectProperty == null) {
				I18nString message = new CNI18nString("objectproperty.notfound");
				message.setParameter("0", objectPropertyId);
				throw new EntityNotFoundException(message.toString());
			}
			List<Construct> constructs = new ArrayList<>(objectProperty.getObjectTags().size());
			for (ObjectTag ot : objectProperty.getObjectTags()) {
				constructs.add(com.gentics.contentnode.object.Construct.TRANSFORM2REST.apply(ot.getConstruct()));
			}
			ConstructListResponse response = new ConstructListResponse();
			response.setResponseInfo(ResponseInfo.ok("Successfully loaded constructs, that use objectProperty"));
			response.setConstructs(constructs);

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/nodes")
	public NodeList listObjectPropertyNodes(@PathParam("id") String objectPropertyId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canView(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.read);
			}
			ObjectTagDefinition objectProperty = load(ObjectTagDefinition.class, objectPropertyId);
			if (objectProperty == null) {
				I18nString message = new CNI18nString("objectproperty.notfound");
				message.setParameter("0", objectPropertyId);
				throw new EntityNotFoundException(message.toString());
			}
			List<com.gentics.contentnode.rest.model.Node> constructs = new ArrayList<>(objectProperty.getNodes().size());
			for (Node node : objectProperty.getNodes()) {
				if (checkViewPermission(node)) {
					constructs.add(com.gentics.contentnode.object.Node.TRANSFORM2REST.apply(node));
				}
			}
			NodeList response = new NodeList();
			response.setResponseInfo(ResponseInfo.ok("Successfully loaded nodes, that are linked to the objectProperty"));
			response.setItems(constructs);

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("{id}")
	public ObjectPropertyLoadResponse update(@PathParam("id") String objectPropertyId, ObjectProperty objectProperty) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canEdit(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.update);
			}
			checkObjectPropertyExists(objectProperty);

			if (objectProperty == null) {
				I18nString message = new CNI18nString("objectproperty.notfound");
				message.setParameter("0", objectPropertyId);
				throw new EntityNotFoundException(message.toString());
			}
			ObjectTagDefinition update = load(ObjectTagDefinition.class, objectPropertyId, ObjectPermission.edit);
			update = ObjectTagDefinition.REST2NODE.apply(objectProperty, t.getObject(update, true));

			if (update.save()) {
				updateCorrespondingObjectTags(objectProperty, update.getObjectTag());
				ContentNodeFactory.getInstance()
						.getFactory()
						.clear(ObjectTag.class);
			}

			ObjectPropertyLoadResponse response = new ObjectPropertyLoadResponse(ObjectTagDefinition.TRANSFORM2REST.apply(update.reload()),
					ResponseInfo.ok("Successfully updated object property"));

			trx.success();
			return response;
		}
	}

	private void updateCorrespondingObjectTags(ObjectProperty objectProperty, ObjectTag objectTag) throws NodeException {
		final String UPDATE_OBJECTTAG_SQL = "UPDATE `objtag` SET  obj_type = ?, construct_id = ?, inheritable = ?, required = ? "
				+ "WHERE name = ? AND obj_type = ?";

		String matchName = objectTag.getName();
		String matchType = objectProperty.getType().toString();

		DBUtils.executeUpdate(UPDATE_OBJECTTAG_SQL, new Object[] {
				objectProperty.getType(),
				objectProperty.getConstructId(),
				objectProperty.getInheritable(),
				objectProperty.getRequired(),
				matchName,
				matchType
		});
	}

	@Override
	@DELETE
	@Path("{id}")
	public GenericResponse delete(@PathParam("id") String objectPropertyId, @QueryParam("foregroundTime") @DefaultValue("-1") int foregroundTime) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (foregroundTime < 0) {
				foregroundTime = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty("contentnode.global.config.backgroundjob_foreground_time"), 5);
			}

			if (!t.getPermHandler().canDelete(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.delete);
			}
			ObjectTagDefinition toDelete = load(ObjectTagDefinition.class, objectPropertyId, ObjectPermission.delete);
			if (toDelete == null) {
				I18nString message = new CNI18nString("objectproperty.notfound");
				message.setParameter("0", objectPropertyId);
				throw new EntityNotFoundException(message.toString());
			}

			GenericResponse response = DeleteJob.process(ObjectTagDefinition.class, Collections.singletonList(toDelete.getId()), false, foregroundTime);

			trx.success();

			return response;
		}
	}

	@Override
	@GET
	@Path("/category")
	public ObjectPropertyCategoryListResponse listCategories(@BeanParam SortParameterBean sorting, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging, @BeanParam EmbedParameterBean embed) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Set<Integer> ids = null;
			ids = DBUtils.select("SELECT id FROM objprop_category", DBUtils.IDS);

			ResolvableComparator<ObjectTagDefinitionCategory> sorter = ResolvableComparator.get(sorting, "id", "globalId", "name", "sortorder");
			if (sorter != null) {
				sorter.setNullsAsLast("sortorder");
			}

			ObjectPropertyCategoryListResponse response = ListBuilder.from(trx.getTransaction().getObjects(ObjectTagDefinitionCategory.class, ids), ObjectTagDefinitionCategory.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "name", "sortorder"))
					.sort(sorter)
					.page(paging).to(new ObjectPropertyCategoryListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/category")
	public ObjectPropertyCategoryLoadResponse createCategory(ObjectPropertyCategory category) throws NodeException {
		if (category == null) {
			throw new MissingFieldException("category");
		}
		if (category.getName() == null && category.getNameI18n() == null) {
			throw new RestMappedException(I18NHelper.get("name.i18n.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canCreate(null, ObjectTagDefinitionCategory.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objprop_category.nopermission"), null, null,
						ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, 0, PermType.create);
			}
			ObjectTagDefinitionCategory object = ObjectTagDefinitionCategory.REST2NODE.apply(category, t.createObject(ObjectTagDefinitionCategory.class));
			object.save();

			ObjectPropertyCategoryLoadResponse response = new ObjectPropertyCategoryLoadResponse(ObjectTagDefinitionCategory.TRANSFORM2REST.apply(object.reload()),
					ResponseInfo.ok("Successfully created object property category"));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/category/{id}")
	public ObjectPropertyCategoryLoadResponse getCategory(@PathParam("id") String categoryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canView(null, ObjectTagDefinitionCategory.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objprop_category.nopermission"), null, null,
						ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, 0, PermType.read);
			}
			ObjectTagDefinitionCategory category = load(ObjectTagDefinitionCategory.class, categoryId);
			if (category == null) {
				I18nString message = new CNI18nString("objprop_category.notfound");
				message.setParameter("0", categoryId);
				throw new EntityNotFoundException(message.toString());
			}
			ObjectPropertyCategoryLoadResponse response = new ObjectPropertyCategoryLoadResponse(ObjectTagDefinitionCategory.TRANSFORM2REST.apply(category),
					ResponseInfo.ok("Successfully loaded object property category"));

			trx.success();
			return response;
		}
	}

	@Override
	@PUT
	@Path("/category/{id}")
	public ObjectPropertyCategoryLoadResponse updateCategory(@PathParam("id") String categoryId, ObjectPropertyCategory category) throws NodeException {
		if (category == null) {
			throw new MissingFieldException("category");
		}
		if (category.getName() == null && category.getNameI18n() == null) {
			throw new RestMappedException(I18NHelper.get("name.i18n.required"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canEdit(null, ObjectTagDefinitionCategory.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objprop_category.nopermission"), null, null,
						ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, 0, PermType.update);
			}
			ObjectTagDefinitionCategory update = load(ObjectTagDefinitionCategory.class, categoryId, ObjectPermission.edit);
			if (update == null) {
				I18nString message = new CNI18nString("objprop_category.notfound");
				message.setParameter("0", categoryId);
				throw new EntityNotFoundException(message.toString());
			}
			update = ObjectTagDefinitionCategory.REST2NODE.apply(category, t.getObject(update, true));
			update.save();

			ObjectPropertyCategoryLoadResponse response = new ObjectPropertyCategoryLoadResponse(ObjectTagDefinitionCategory.TRANSFORM2REST.apply(update.reload()),
					ResponseInfo.ok("Successfully updated object property category"));

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/category/{id}")
	public GenericResponse deleteCategory(@PathParam("id") String categoryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canDelete(null, ObjectTagDefinitionCategory.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objprop_category.nopermission"), null, null,
						ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, 0, PermType.delete);
			}
			ObjectTagDefinitionCategory toDelete = load(ObjectTagDefinitionCategory.class, categoryId, ObjectPermission.delete);
			if (toDelete == null) {
				I18nString message = new CNI18nString("objprop_category.notfound");
				message.setParameter("0", categoryId);
				throw new EntityNotFoundException(message.toString());
			}
			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				if (!toDelete.getObjectTagDefinitions().isEmpty()) {
					CNI18nString message = new CNI18nString("resource.cannotdelete");
					message.setParameter("0", "Object Tag Definition Category");
					message.setParameter("1", toDelete.getName());
					message.setParameter("2", "Object Tag Definition");
					message.setParameter("3", toDelete.getObjectTagDefinitions().stream().map(ObjectTagDefinition::getName).collect(Collectors.joining(", ")));
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

	private GenericResponse changeLinkStatus(BulkLinkUpdateRequest request, boolean add) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); AnyChannelTrx aCTrx = new AnyChannelTrx()) {
			Transaction t = trx.getTransaction();

			if (!t.getPermHandler().canEdit(null, ObjectTagDefinition.class, null)) {
				throw new InsufficientPrivilegesException(I18NHelper.get("objectproperty.nopermission"), null, null,
						ObjectTagDefinition.TYPE_OBJTAG_DEF, 0, PermType.update);
			}
			for (Integer nodeId : request.getIds()) {
				Node node = loadNodeForUpdate(Integer.toString(nodeId), PermType.read);

				for (String objectPropertyId : request.getTargetIds()) {
					ObjectTagDefinition update = load(ObjectTagDefinition.class, objectPropertyId, ObjectPermission.edit);
					if (add) {
						node.addObjectTagDefinition(update);
					} else {
						node.removeObjectTagDefinition(update);
					}
				}
			}
			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully " + (add ? "" : "un") + "linked object properties against nodes"));
		}
	}

	/**
	 * Check whether the user is allowed to see the given node or any of its channels.
	 * Currently, there are no real channel specific permissions, so this might be worked around by giving a user
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

	private void checkObjectPropertyExists(ObjectProperty objectProperty) throws RestMappedException, NodeException {
		if (!DBUtils.select("SELECT id FROM objtag WHERE obj_type = ? AND name = ?", ps -> {
					ps.setInt(1, objectProperty.getType());
					ps.setString(2, objectProperty.getKeyword());
				}, DBUtils.IDS).isEmpty()) {
			throw new RestMappedException(I18NHelper.get("objectproperty.duplicate"))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
	}

	/**
	 * Load the node with given ID and check for existence and permission. The permission check will also check for permission to see at least a channel
	 * @param id node ID
	 * @param permType an access permission to check against the node
	 * @return node
	 * @throws NodeException
	 */
	private Node loadNodeForUpdate(String id, PermType permType) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.getObject(Node.class, id, true);
		if (node == null) {
			throw new EntityNotFoundException(I18NHelper.get("node.notfound", id));
		}
		if (!checkViewPermission(node)) {
			throw new InsufficientPrivilegesException(I18NHelper.get("node.nopermission", id), node, permType);
		}

		return node;
	}
}
