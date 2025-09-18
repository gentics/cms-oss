package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.load;
import static com.gentics.contentnode.rest.util.RequestParamHelper.embeddedParameterContainsAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.ws.rs.BeanParam;
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

import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.util.Operator;
import org.apache.commons.lang3.StringUtils;

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
					"inheritable", "syncContentset", "syncChannelset", "syncVariants", "restricted", "construct.name",
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
			checkObjectPropertyExists(objectProperty.getType(), objectProperty.getKeyword());

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
			ObjectTagDefinition update = load(ObjectTagDefinition.class, objectPropertyId, ObjectPermission.edit);

			if (!StringUtils.isBlank(objectProperty.getKeyword())) {
				String keyword = StringUtils.removeStart(objectProperty.getKeyword(), KEYWORD_PREFIX);
				keyword = keyword.trim();
				if (!KEYWORD_PATTERN.matcher(keyword).matches()) {
					throw new RestMappedException(I18NHelper.get("keyword.invalid"))
						.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
				}

				// Update the keyword from the request to make sure it starts with the "object." prefix.
				objectProperty.setKeyword(KEYWORD_PREFIX + keyword);
				checkObjectPropertyExists(update.getId(), update.getObjectTag().getObjType(), objectProperty.getKeyword());
			}

			update = ObjectTagDefinition.REST2NODE.apply(objectProperty, t.getObject(update, true));

			if (update.save()) {
				updateCorrespondingObjectTags(update.getObjectTag());
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

	private void updateCorrespondingObjectTags(ObjectTag objectTag) throws NodeException {
		final String UPDATE_OBJECTTAG_SQL = "UPDATE `objtag` SET construct_id = ?, inheritable = ?, required = ? "
				+ "WHERE name = ? AND obj_type = ? AND obj_id != ?";

		DBUtils.update(UPDATE_OBJECTTAG_SQL, objectTag.getConstructId(), objectTag.isInheritable(), objectTag.isRequired(), objectTag.getName(), objectTag.getObjType(), 0);
	}

	@Override
	@DELETE
	@Path("{id}")
	public GenericResponse delete(@PathParam("id") String objectPropertyId, @QueryParam("wait") @DefaultValue("0") int wait) throws NodeException {
		I18nString description = new CNI18nString("objectproperty.delete");

		description.setParameter("0", objectPropertyId);

		try (Trx trx = ContentNodeHelper.trx()) {
			return Operator.execute(description.toString(), wait, () -> {
				try ( AnyChannelTrx aCTrx = new AnyChannelTrx()) {
					Transaction t = TransactionManager.getCurrentTransaction();

					if (!t.getPermHandler().canDelete(null, ObjectTagDefinition.class, null)) {
						throw new InsufficientPrivilegesException(
								I18NHelper.get("objectproperty.nopermission"),
								null,
								null,
								ObjectTagDefinition.TYPE_OBJTAG_DEF,
								0,
								PermType.delete);
					}

					ObjectTagDefinition toDelete = load(ObjectTagDefinition.class, objectPropertyId, ObjectPermission.delete);

					if (toDelete == null) {
						I18nString message = new CNI18nString("objectproperty.notfound");

						message.setParameter("0", objectPropertyId);

						throw new EntityNotFoundException(message.toString());
					}

					// delete all tags based on the definition

					for (ObjectTag tag : toDelete.getObjectTags()) {
						t.dirtObjectCache(ObjectTag.class, tag.getId(), true);

						NodeObject tagContainer = tag.getNodeObject();

						if (tagContainer != null) {
							t.dirtObjectCache(tagContainer.getObjectInfo().getObjectClass(), tagContainer.getId(), true);
						}

						if (tag.isEnabled()) {
							Events.trigger(tag, null, Events.DELETE);
						}

						tag.delete();
					}

					t.dirtObjectCache(ObjectTagDefinition.class, toDelete.getId(), true);
					toDelete.delete();

					I18nString message = new CNI18nString("objectproperty.delete.success");

					message.setParameter("0", objectPropertyId);

					return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			});
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

			Map<String, String> fieldMap = new HashMap<>();
			fieldMap.put("sortOrder", "sortorder");

			ResolvableComparator<ObjectTagDefinitionCategory> sorter = ResolvableComparator.get(sorting, fieldMap, "id", "globalId", "name", "sortorder", "sortOrder");
			if (sorter != null) {
				sorter.setNullsAsLast("sortorder", "sortOrder");
			}

			ObjectPropertyCategoryListResponse response = ListBuilder.from(trx.getTransaction().getObjects(ObjectTagDefinitionCategory.class, ids), ObjectTagDefinitionCategory.TRANSFORM2REST)
					.filter(o -> PermFilter.get(ObjectPermission.view).matches(o))
					.filter(ResolvableFilter.get(filter, fieldMap, "id", "globalId", "name", "sortorder", "sortOrder"))
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

			Set<Node> nodes = new HashSet<>(); 
			for (Integer nodeId : request.getIds()) {
				Node givenNode = load(Node.class, Integer.toString(nodeId));
				givenNode = load(Node.class, Integer.toString(givenNode.getMaster().getId()));
				nodes.add(givenNode);
			}

			for (String objectPropertyId : request.getTargetIds()) {
				ObjectTagDefinition update = load(ObjectTagDefinition.class, objectPropertyId, ObjectPermission.edit);

				update = t.getObject(update, true);
				if (add) {
					// add
					update.getNodes().addAll(nodes);
				} else {
					// remove
					update.getNodes().removeAll(nodes);
				}

				update.save();
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

	/**
	 * Check whether an object property with given type and keyword exists
	 * @param objType object type
	 * @param keyword keyword (must be prefixed with object.)
	 * @throws RestMappedException
	 * @throws NodeException
	 */
	private void checkObjectPropertyExists(int objType, String keyword) throws RestMappedException, NodeException {
		if (!DBUtils.select("SELECT id FROM objtag WHERE obj_type = ? AND name = ? AND obj_id = ?", ps -> {
					ps.setInt(1, objType);
					ps.setString(2, keyword);
					ps.setInt(3, 0);
				}, DBUtils.IDS).isEmpty()) {
			throw new RestMappedException(I18NHelper.get("objectproperty.duplicate", keyword))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}
	}

	/**
	 * Check whether an object property with given type and keyword with different id exists
	 * @param id object ID
	 * @param objType object type
	 * @param keyword keyword (must be prefixed with object.)
	 * @throws RestMappedException
	 * @throws NodeException
	 */
	private void checkObjectPropertyExists(int id, int objType, String keyword) throws RestMappedException, NodeException {
		if (!DBUtils.select("SELECT id FROM objtag WHERE obj_type = ? AND name = ? AND obj_id = ? AND id != ?", ps -> {
					ps.setInt(1, objType);
					ps.setString(2, keyword);
					ps.setInt(3, 0);
					ps.setInt(4, id);
				}, DBUtils.IDS).isEmpty()) {
			throw new RestMappedException(I18NHelper.get("objectproperty.duplicate", keyword))
				.setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
		}
	}
}
