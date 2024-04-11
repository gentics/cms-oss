package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.checkFields;
import static com.gentics.contentnode.rest.util.MiscUtils.comparator;
import static com.gentics.contentnode.rest.util.MiscUtils.executeJob;
import static com.gentics.contentnode.rest.util.MiscUtils.filter;
import static com.gentics.contentnode.rest.util.MiscUtils.getFolder;
import static com.gentics.contentnode.rest.util.MiscUtils.getNode;
import static com.gentics.contentnode.rest.util.MiscUtils.getTemplate;
import static com.gentics.contentnode.rest.util.MiscUtils.permFunction;
import static com.gentics.contentnode.rest.util.MiscUtils.reduceList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.EntityInUseException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.UniquifyHelper;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.TemplateFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.job.TemplateSaveJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.TemplateInNode;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.LinkRequest;
import com.gentics.contentnode.rest.model.request.MultiLinkRequest;
import com.gentics.contentnode.rest.model.request.SortOrder;
import com.gentics.contentnode.rest.model.request.TagSortAttribute;
import com.gentics.contentnode.rest.model.request.TemplateCopyRequest;
import com.gentics.contentnode.rest.model.request.TemplateCreateRequest;
import com.gentics.contentnode.rest.model.request.TemplateSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.PagedFolderListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TagList;
import com.gentics.contentnode.rest.model.response.TagListResponse;
import com.gentics.contentnode.rest.model.response.TagStatus;
import com.gentics.contentnode.rest.model.response.TagStatusResponse;
import com.gentics.contentnode.rest.model.response.TemplateInNodeResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.NodeObjectFilter;
import com.gentics.contentnode.rest.util.PermissionFilter;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.rest.util.ResolvableFilter;
import com.gentics.contentnode.rest.util.StringFilter;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.genericexceptions.IllegalUsageException;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

/**
 * Resource used for loading, saving and manipulating GCN templates.
 */
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/template")
public class TemplateResourceImpl implements TemplateResource {
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Count the pages in sync with the template with regards to the tag
	 * @param templateId template ID
	 * @param tagName tag name
	 * @param constructId construct ID (used by the template)
	 * @return number of pages in sync
	 * @throws NodeException
	 */
	public static int getPagesInSync(int templateId, String tagName, int constructId) throws NodeException {
		return DBUtils.select(
				"SELECT count(DISTINCT page.id) c FROM page, contenttag WHERE page.deleted = 0 AND page.content_id = contenttag.content_id AND page.template_id = ? AND contenttag.name = ? AND contenttag.construct_id = ?",
				pst -> {
					pst.setInt(1, templateId);
					pst.setString(2, tagName);
					pst.setInt(3, constructId);
				}, rs -> {
					rs.next();
					return rs.getInt("c");
				});
	}

	/**
	 * Get counts for pages not in sync with the template with regards to the tag
	 * @param templateId template ID
	 * @param tagName tag name
	 * @param constructId construct ID (used by the template)
	 * @return map of construct IDs used by pages to page counts
	 * @throws NodeException
	 */
	public static Map<Integer, AtomicInteger> getPagesOutOfSync(int templateId, String tagName, int constructId) throws NodeException {
		return DBUtils.select(
				"SELECT contenttag.construct_id FROM page, contenttag WHERE page.deleted = 0 AND page.content_id = contenttag.content_id AND page.template_id = ? AND contenttag.name = ? AND contenttag.construct_id != ?",
				pst -> {
					pst.setInt(1, templateId);
					pst.setString(2, tagName);
					pst.setInt(3, constructId);
				}, rs -> {
					Map<Integer, AtomicInteger> counts = new HashMap<>();
					while (rs.next()) {
						int sourceConstructId = rs.getInt("construct_id");
						counts.computeIfAbsent(sourceConstructId, id -> new AtomicInteger()).incrementAndGet();
					}
					return counts;
				});
	}

	/**
	 * Count the pages missing the tag
	 * @param templateId template ID
	 * @param tagName tag name
	 * @return number of pages missing the tag
	 * @throws NodeException
	 */
	public static int getPagesMissing(int templateId, String tagName) throws NodeException {
		return DBUtils.select(
				"SELECT count(DISTINCT page.id) c FROM page LEFT JOIN contenttag ON page.content_id = contenttag.content_id AND contenttag.name = ? WHERE page.deleted = 0 AND page.template_id = ? AND contenttag.id IS NULL",
				pst -> {
					pst.setString(1, tagName);
					pst.setInt(2, templateId);
				}, rs -> {
					rs.next();
					return rs.getInt("c");
				});
	}

	/**
	 * Check whether a tag can be migrated from one construct ID to another without losing data
	 * @param fromConstructId source construct ID
	 * @param toConstructId target construct ID
	 * @param cache cache of check results
	 * @return true iff tags can be converted
	 * @throws NodeException
	 */
	public static boolean canConvert(int fromConstructId, int toConstructId, Map<Integer, Map<Integer, Boolean>> cache) throws NodeException {
		return cache.computeIfAbsent(fromConstructId, id -> new HashMap<>()).computeIfAbsent(toConstructId, id -> {
			try {
				Transaction t = TransactionManager.getCurrentTransaction();
				Construct fromConstruct = t.getObject(Construct.class, fromConstructId);
				Construct toConstruct = t.getObject(Construct.class, toConstructId);
				return fromConstruct.canConvertTo(toConstruct);
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		}).booleanValue();
	}

	/**
	 * Get tag status response
	 * @param template template
	 * @param sort sorting parameter bean
	 * @param filter filter parameter bean
	 * @param paging paging parameter bean
	 * @return response
	 * @throws NodeException
	 */
	public static TagStatusResponse getTagStatus(Template template, SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		int templateId = template.getId();
		List<TemplateTag> tags = template.getTemplateTags().values().stream().filter(TemplateTag::isPublic).collect(Collectors.toList());

		return ListBuilder.from(tags, tag -> {
			String tagName = tag.getName();
			int constructId = tag.getConstructId();
			String constructName = tag.getConstruct().getName().toString();

			Map<Integer, AtomicInteger> pagesOutOfSync = getPagesOutOfSync(templateId, tagName, constructId);
			int outOfSync = pagesOutOfSync.values().stream().mapToInt(AtomicInteger::get).sum();

			int incompatible = 0;
			Map<Integer, Map<Integer, Boolean>> cache = new HashMap<>();
			for (Map.Entry<Integer, AtomicInteger> entry : pagesOutOfSync.entrySet()) {
				int fromConstructId = entry.getKey();
				if (!canConvert(fromConstructId, constructId, cache)) {
					incompatible += entry.getValue().get();
				}
			}

			return new TagStatus().setName(tagName).setConstructId(constructId).setConstructName(constructName)
					.setInSync(getPagesInSync(templateId, tagName, constructId)).setMissing(getPagesMissing(templateId, tagName)).setOutOfSync(outOfSync)
					.setIncompatible(incompatible);
		}).filter(ResolvableFilter.get(filter, "name")).sort(ResolvableComparator.get(sort, "name")).page(paging).to(new TagStatusResponse());

	}

	@Override
	@GET
	public TemplateInNodeResponse list(
			@QueryParam("nodeId") List<String> nodeIds,
			@BeanParam FilterParameterBean filterParams,
			@BeanParam SortParameterBean sortingParams,
			@BeanParam PagingParameterBean pagingParams,
			@BeanParam PermsParameterBean perms,
			@BeanParam TemplateListParameterBean listParams) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Set<Node> nodes;

			if (nodeIds == null || nodeIds.isEmpty()) {
				nodes = trx.getTransaction().getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDLIST))
					.stream()
					.collect(Collectors.toSet());

				PermissionFilter.get(ObjectPermission.view).filter(nodes);
			} else {
				nodes = new HashSet<>();

				for (String nodeId : nodeIds) {
					nodes.add(getNode(nodeId, ObjectPermission.view));
				}
			}

			List<Pair<Node, Template>> nodeAndTemplates = new ArrayList<>();
			Set<Template> foundTemplates = new HashSet<>();


			for (Node node : nodes) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					List<Template> templates = new ArrayList<>(node.getTemplates());
					PermissionFilter.get(ObjectPermission.view).filter(templates);

					for (Template template : templates) {
						if (!listParams.reduce || !foundTemplates.contains(template)) {
							nodeAndTemplates.add(Pair.of(node, template));
							foundTemplates.add(template);
						}
					}
				}
			}

			TemplateInNodeResponse response = ListBuilder.from(nodeAndTemplates, pair -> {
					TemplateInNode templateInNode = new TemplateInNode();
					try (ChannelTrx cTrx = new ChannelTrx(pair.getLeft())) {
						Template.NODE2REST.apply(pair.getRight(), templateInNode);
					}
					templateInNode.setNodeId(pair.getLeft().getId());
					List<String> nodeNames = new ArrayList<>();
					for (Node node : pair.getRight().getAssignedNodes()) {
						if (PermissionFilter.get(ObjectPermission.view).matches(node)) {
							nodeNames.add(node.getFolder().getName());
						}
					}
					templateInNode.setAssignedNodes(nodeNames);
					return templateInNode;
				})
				.perms(permFunction(perms, pair -> pair.getRight(), ObjectPermission.view, ObjectPermission.edit, ObjectPermission.delete))
				.filter(filter(filterParams, this::getTemplateFieldOfParameter, "id", "name", "description", "inheritedFrom", "locked", "cdate", "edate"))
				.sort(comparator(sortingParams, this::getTemplateFieldOfParameter, "id", "name", "description", "inheritedFrom", "locked", "cdate", "edate"))
				.page(pagingParams)
				.to(new TemplateInNodeResponse());

			trx.success();

			return response;
		}
	}

	@Override
	@POST
	public TemplateLoadResponse create(TemplateCreateRequest request) throws NodeException {
		if (request == null) {
			throw new RestMappedException().setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			checkFields(() -> Pair.of("template", request.getTemplate()));

			// either template.folderId or folderIds have to be set
			if (ObjectTransformer.isEmpty(request.getFolderIds())) {
				checkFields(() -> Pair.of("template.folderId", request.getTemplate().getFolderId()));
			}

			if (request.getNodeId() != null) {
				getNode(Integer.toString(request.getNodeId()), ObjectPermission.view);
			}

			try (ChannelTrx cTrx = new ChannelTrx(request.getNodeId())) {
				Set<Folder> folders = new HashSet<>();
				if (request.getTemplate().getFolderId() != null) {
					folders.add(getFolder(Integer.toString(request.getTemplate().getFolderId()), ObjectPermission.view));
				}

				for (String folderId : request.getFolderIds()) {
					folders.add(getFolder(folderId, ObjectPermission.view));
				}

				for (Folder folder : folders) {
					if (!trx.getTransaction().getPermHandler().canCreate(folder, Template.class, null)) {
						I18nString message = new CNI18nString("folder.nopermission");
						message.setParameter("0", folder.getId().toString());
						throw new InsufficientPrivilegesException(message.toString(), folder, PermType.create);
					}
				}

				// make sure to remove any ID, which was sent
				request.getTemplate().setId(null);
				Template template = ModelBuilder.getTemplate(request.getTemplate());

				if (!StringUtils.isEmpty(template.getName())) {
					// get names of templates linked to the folders (for making the template name unique)
					Set<String> templateNames = new HashSet<>();

					for (Folder folder : folders) {
						templateNames.addAll(folder.getTemplates().stream().map(Template::getName).collect(Collectors.toSet()));
					}

					String uniqueName = UniquifyHelper.makeUnique(() -> template.getName(), name -> !templateNames.contains(name), (name, number) -> {
						return String.format("%s %d", name, number);
					}, null);
					template.setName(uniqueName);
				}

				if (request.getNodeId() != null) {
					template.setChannelInfo(request.getNodeId(), null);
				}

				for (Folder folder : folders) {
					template.addFolder(folder);
				}

				template.save();
				template.unlock();

				TemplateLoadResponse response = new TemplateLoadResponse();
				response.setResponseInfo(ResponseInfo.ok(""));
				response.setTemplate(Template.TRANSFORM2REST.apply(template.reload()));

				trx.success();

				return response;
			}
		}
	}

	@Override
	@GET
	@Path("/{id}")
	public TemplateLoadResponse get(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("update") @DefaultValue("false") boolean update, @QueryParam("construct") @DefaultValue("false") boolean construct)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			String responseMessage = null;

			TemplateLoadResponse response = new TemplateLoadResponse();
			Template template = null;

			if (update) {
				template = getTemplate(id, ObjectPermission.view, ObjectPermission.edit);
				template = trx.getTransaction().getObject(template, true);
			} else {
				template = getTemplate(id, ObjectPermission.view);
			}

			// transform the template into a REST object
			Collection<Reference> fillRefs = new Vector<Reference>();
			fillRefs.add(Reference.TEMPLATE_TAGS);
			fillRefs.add(Reference.TEMPLATE_SOURCE);
			fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
			if (construct) {
				fillRefs.add(Reference.TAG_EDIT_DATA);
			}

			com.gentics.contentnode.rest.model.Template restTemplate = ModelBuilder.getTemplate(template, fillRefs);

			response.setTemplate(restTemplate);
			if (responseMessage == null) {
				responseMessage = "Loaded template with id { " + id + " } successfully";
			}
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, responseMessage));
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{id}")
	public GenericResponse update(@PathParam("id") String id, TemplateSaveRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			// get the template, and check for view and edit permission
			Template template = getTemplate(id, ObjectPermission.view, ObjectPermission.edit);

			com.gentics.contentnode.rest.model.Template restTemplate = request.getTemplate();
			if (restTemplate == null) {
				restTemplate = new com.gentics.contentnode.rest.model.Template();
			}
			restTemplate.setId(ObjectTransformer.getInteger(template.getId(), null));

			// try to get an editable copy of the template
			// this will check whether the template is locked by another user
			trx.getTransaction().getObject(template, true);

			TemplateSaveJob job = new TemplateSaveJob(restTemplate, request.getDelete(), request.isUnlock(), request.isSyncPages(), request.getSync(),
					request.isForceSync());
			GenericResponse response = executeJob(job, null, false);
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{id}/unlock")
	public TemplateLoadResponse unlock(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			String message;
			Template template = MiscUtils.load(Template.class, id);
			if (template.isLockedByCurrentUser()) {
				template.unlock();
				message = "Successfully unlocked template";
			} else {
				message = "Template was not locked by user";
			}

			TemplateLoadResponse response = new TemplateLoadResponse(null, ResponseInfo.ok(message),
					ModelBuilder.getTemplate(template,
							Arrays.asList(Reference.TEMPLATE_SOURCE, Reference.TEMPLATE_TAGS, Reference.OBJECT_TAGS)));
			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/{id}")
	public Response delete(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			// get the template, and check for view and delete permission
			Template template = getTemplate(id, ObjectPermission.view, ObjectPermission.delete);

			try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
				if (!template.getPages().isEmpty()) {
					CNI18nString message = new CNI18nString("notification.templatedelete.islinked");
					message.setParameter("0", template.getName());
					throw new EntityInUseException(message.toString());
				}
			}

			template.delete();
			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@GET
	@Path("/{id}/folders")
	public PagedFolderListResponse folders(@PathParam("id") String id, @BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			// get the template, and check for view permission
			Template template = getTemplate(id, ObjectPermission.view);

			List<Folder> folders = new ArrayList<>(template.getFolders());
			PermissionFilter.get(ObjectPermission.view).filter(folders);

			PagedFolderListResponse response = ListBuilder.from(folders, f -> {
				try (ChannelTrx cTrx = new ChannelTrx(f.getChannel())) {
					return ModelBuilder.getFolder(f);
				}
			}).filter(ResolvableFilter.get(filter, "id", "name", "description"))
					.sort(ResolvableComparator.get(sort, "id", "name", "description", "cdate", "edate", "masterNode", "creator", "editor")).page(paging)
					.to(new PagedFolderListResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/nodes")
	public NodeList nodes(@PathParam("id") String id, @BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			// get the template, and check for view permission
			Template template = getTemplate(id, ObjectPermission.view);
			boolean multichannelling = NodeConfigRuntimeConfiguration.isFeature(Feature.MULTICHANNELLING);

			Set<Node> assignedNodes = template.getAssignedNodes();
			List<Node> nodes = new ArrayList<>();
			for (Node node : assignedNodes) {
				if (multichannelling) {
					Template variant = MultichannellingFactory.getChannelVariant(template, node);
					if (variant != null) {
						nodes.add(node);
					}
					for (Node channel : node.getAllChannels()) {
						variant = MultichannellingFactory.getChannelVariant(template, channel);
						if (variant != null) {
							nodes.add(channel);
						}
					}
				} else {
					nodes.add(node);
				}
			}

			NodeObjectFilter permFilter = PermissionFilter.get(ObjectPermission.view);

			Map<String, String> sortFieldMapping = new HashMap<>();
			sortFieldMapping.put("name", "folder.name");

			NodeList nodeList = ListBuilder.from(nodes, Node.TRANSFORM2REST)
				.filter(o -> permFilter.matches(o))
				.filter(ResolvableFilter.get(filter, "id", "folder.name"))
				.sort(ResolvableComparator.get(sort, sortFieldMapping, "id", "name"))
				.page(paging)
				.to(new NodeList());

			trx.success();
			return nodeList;
		}
	}

	@Override
	@GET
	@Path("/getTags/{id}")
	@Deprecated
	public TagListResponse getTags(@PathParam("id") String id,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("-1") Integer maxItems,
			final @QueryParam("sortby") @DefaultValue("name") TagSortAttribute sortBy,
			final @QueryParam("sortorder") @DefaultValue("asc") SortOrder sortOrder,
			@QueryParam("search") String search
			) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Template template = getTemplate(id, ObjectPermission.view);
			List<Tag> tags = new Vector<Tag>(template.getTags().values());

			// search tags
			if (!ObjectTransformer.isEmpty(search)) {
				NodeObjectFilter filter = new StringFilter("%" + search + "%", Tag.class
						.getMethod("getName"), true,
						StringFilter.Case.INSENSITIVE);
				filter.filter(tags);
			}

			// sort tags
			if (sortBy != null) {
				Collections.sort(tags, new Comparator<Tag>() {
					public int compare(Tag tag1, Tag tag2) {
						String value1 = null;
						String value2 = null;
						switch (sortBy) {
						case name:
							value1 = ObjectTransformer.getString(tag1
									.getName(), "");
							value2 = ObjectTransformer.getString(tag2
									.getName(), "");
							break;
						default:
							return 0;
						}

						return (sortOrder == SortOrder.asc || sortOrder == SortOrder.ASC) ? value1
								.compareToIgnoreCase(value2) : value2
								.compareToIgnoreCase(value1);
					}
				});
			}

			// return tag list
			TagListResponse response = new TagListResponse(null,
				new ResponseInfo(ResponseCode.OK,
					"Successfully fetched tags of template " + id));

			// do paging
			response.setNumItems(tags.size());
			response.setHasMoreItems(maxItems >= 0 && (tags.size() > skipCount + maxItems));
			reduceList(tags, skipCount, maxItems);

			List<com.gentics.contentnode.rest.model.Tag> restTags = new Vector<com.gentics.contentnode.rest.model.Tag>(
					tags.size());
			for (Tag tag : tags) {
                restTags.add(ModelBuilder.getTag(tag, false));
			}
			response.setTags(restTags);
			trx.success();
			return response;
		} catch (NoSuchMethodException | SecurityException e) {
			throw new NodeException(e);
		}
	}

	@Override
	@GET
	@Path("/{id}/tag")
	public TagList listTags(
			@PathParam("id") String id,
			@BeanParam FilterParameterBean filter,
			@BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging,
			@BeanParam EmbedParameterBean embed
	) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Template template = getTemplate(id, ObjectPermission.view);
			List<Tag> nodeTags = new ArrayList<>(template.getTags().values());

			TagList response = ListBuilder.from(nodeTags, (nodeTag) ->
							ModelBuilder.getTag(nodeTag, false))
					.filter(ResolvableFilter.get(filter, "id", "globalId", "constructId", "name", "enabled"))
					.sort(ResolvableComparator.get(sorting, "id", "globalId", "constructId", "name", "enabled"))
					.embed(embed, "construct", Tag.EMBED_CONSTRUCT)
					.page(paging)
					.to(new TagList());

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/load/{id}")
	public TemplateLoadResponse load(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		return get(id, nodeId, false, false);
	}

	@Override
	@POST
	@Path("/link/{id}")
	public GenericResponse link(@PathParam("id") String id, LinkRequest request) throws NodeException {
		return link(transformToMultiRequest(id, request));
	}

	@Override
	@POST
	@Path("/link")
	public GenericResponse link(MultiLinkRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(request.getNodeId())) {

			linkTemplatesToFolders(getTemplates(request), getFolders(request));
			trx.success();
			return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully linked templates"));
		}
	}

	@Override
	@POST
	@Path("/unlink/{id}")
	public GenericResponse unlink(@PathParam("id") String id, LinkRequest request) throws NodeException {
		return unlink(transformToMultiRequest(id, request));
	}

	@Override
	@POST
	@Path("/unlink")
	public GenericResponse unlink(MultiLinkRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(request.getNodeId())) {
			List<Message> messages = unlinkTemplatesFromFolders(getTemplates(request), getFolders(request), request.isDelete());
			trx.success();
			GenericResponse response = new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully unlinked templates"));
			response.setMessages(messages);
			return response;
		}
	}

	@Override
	@GET
	@Path("/{id}/tagstatus")
	public TagStatusResponse tagStatus(@PathParam("id") String id, @BeanParam SortParameterBean sort, @BeanParam FilterParameterBean filter,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Template template = getTemplate(id, ObjectPermission.view);

			TagStatusResponse response = getTagStatus(template, sort, filter, paging);

			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/{id}/copy")
	public TemplateLoadResponse copy(@PathParam("id") String id, TemplateCopyRequest request) throws NodeException {
		if (request == null) {
			throw new RestMappedException().setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		try (Trx trx = ContentNodeHelper.trx()) {
			Template template = getTemplate(id, ObjectPermission.view);
			Folder folder = getFolder(Integer.toString(request.getFolderId()), ObjectPermission.view);

			if (request.getNodeId() != null) {
				getNode(Integer.toString(request.getNodeId()), ObjectPermission.view);
			}

			try (ChannelTrx cTrx = new ChannelTrx(request.getNodeId())) {
				if (!trx.getTransaction().getPermHandler().canCreate(folder, Template.class, null)) {
					I18nString message = new CNI18nString("folder.nopermission");
					message.setParameter("0", id.toString());
					throw new InsufficientPrivilegesException(message.toString(), folder, PermType.createtemplates);
				}
			}

			// get names of templates linked to the folder (for making the template name unique)
			Set<String> templateNames = folder.getTemplates().stream().map(Template::getName).collect(Collectors.toSet());

			String uniqueName = UniquifyHelper.makeUnique(() -> I18NHelper.get("copy_of", "", template.getName()), name -> !templateNames.contains(name),
					(name, number) -> {
						return I18NHelper.get("copy_of", " " + Integer.toString(number), template.getName());
					}, null);

			Template templateCopy = (Template) template.copy();
			if (request.getNodeId() != null) {
				templateCopy.setChannelInfo(request.getNodeId(), null);
			}
			templateCopy.setName(uniqueName);
			templateCopy.addFolder(folder);
			templateCopy.save();
			templateCopy.unlock();

			templateCopy = templateCopy.reload();

			Collection<Reference> fillRefs = new ArrayList<>();
			fillRefs.add(Reference.TEMPLATE_TAGS);
			fillRefs.add(Reference.TEMPLATE_SOURCE);
			fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
			com.gentics.contentnode.rest.model.Template restTemplate = ModelBuilder.getTemplate(templateCopy, fillRefs);

			TemplateLoadResponse response = new TemplateLoadResponse();
			response.setTemplate(restTemplate);
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));

			trx.success();

			return response;
		}
	}

	/**
	 * Get the list of folders specified in the given linkrequest
	 *
	 * @param request
	 *			request
	 * @return list of folders (may be empty, but never null)
	 * @throws NodeException
	 */
	protected Set<Folder> getFolders(LinkRequest request) throws NodeException {
		if (ObjectTransformer.isEmpty(request.getFolderIds())) {
			return Collections.emptySet();
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		List<Folder> folders = t.getObjectsByStringIds(Folder.class, request.getFolderIds());
		Set<Folder> allFolders = new HashSet<Folder>(folders);

		if (request.isRecursive()) {
			for (Folder folder : folders) {
				recursiveGetChildFolders(folder, allFolders);
			}
		}

		Set<Folder> masterFolders = new HashSet<Folder>(allFolders.size());
		for (Folder folder : allFolders) {
			masterFolders.add(folder.getMaster());
		}

		// if a folder was specified, to which the user is not allowed to link
		// templates to, an InsufficientPrivilegesException is thrown
		for (Iterator<Folder> i = masterFolders.iterator(); i.hasNext();) {
			Folder folder = i.next();

			if (!t.canView(folder) || !t.canView(folder, Template.class, null)
					|| !permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folder.getId(), PermHandler.PERM_TEMPLATE_LINK)) {
				I18nString message = new CNI18nString("folder.nopermission");
				message.setParameter("0", Integer.toString(folder.getId()));
				throw new InsufficientPrivilegesException(message.toString(), folder, PermType.linktemplates);
			}
		}

		return masterFolders;
	}

	/**
	 * Get the templates specified in the given request. If the user is not
	 * allowed to view one of the specified templates, an
	 * {@link InsufficientPrivilegesException} will be thrown
	 *
	 * @param request
	 *			request object
	 * @return list of templates
	 * @throws NodeException
	 */
	protected List<Template> getTemplates(MultiLinkRequest request) throws NodeException {
		if (ObjectTransformer.isEmpty(request.getTemplateIds())) {
			return Collections.emptyList();
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		List<Template> templates = t.getObjectsByStringIds(Template.class, request.getTemplateIds());

		List<Template> masterTemplates = new ArrayList<Template>(templates.size());
		for (Template template : templates) {
			masterTemplates.add(template.getMaster());
		}

		for (Template template : masterTemplates) {
			if (!t.canView(template)) {
				I18nString message = new CNI18nString("template.nopermission");
				message.setParameter("0", Integer.toString(template.getId()));
				throw new InsufficientPrivilegesException(message.toString(), template, PermType.read);
			}
		}

		return masterTemplates;
	}

	/**
	 * Link the given set of templates to the given folders
	 *
	 * @param templates
	 *			templates to link
	 * @param folders
	 *			folders to link to
	 * @return link result
	 * @throws NodeException
	 */
	protected void linkTemplatesToFolders(List<Template> templates, Set<Folder> folders) throws NodeException {
		for (Template template : templates) {
			TemplateFactory.link(template, folders);
		}
	}

	/**
	 * Unlink the templates from the given set of folders. If a single template
	 * would be unlinked from all its folders, the method will throw a
	 * {@link EntityInUseException}
	 *
	 * @param templates
	 *			templates
	 * @param folders
	 *			folders
	 * @param delete true to delete the template, if unlinked from last folder
	 * @return list of messages to be sent
	 * @throws NodeException
	 */
	protected List<Message> unlinkTemplatesFromFolders(List<Template> templates, Set<Folder> folders, boolean delete) throws NodeException {
		List<Message> messages = new ArrayList<>();

		for (Template template : templates) {
			try {
				TemplateFactory.unlink(template, folders);
			} catch (IllegalUsageException e) {
				if (delete) {
					// delete (if possible)
					try (WastebinFilter wb = Wastebin.INCLUDE.set()) {
						if (!template.getPages().isEmpty()) {
							CNI18nString message = new CNI18nString("notification.templatedelete.islinked");
							message.setParameter("0", template.getName());
							throw new EntityInUseException(message.toString());
						}
					}
					messages.add(new Message(Type.INFO, I18NHelper.get("template.deleted", template.getName())));
					template.delete();
				} else {
					I18nString message = new CNI18nString("template.unlink.notlinked");
					message.setParameter("0", Integer.toString(template.getId()));
					throw new EntityInUseException(message.toString());
				}
			}
		}

		return messages;
	}

	/**
	 * Recursively get all child folders from the given folder and put into the
	 * given set of folders. If a child folder is already present in the set,
	 * the recursion will not be continued with that folder
	 *
	 * @param folder
	 *			folder to start with
	 * @param allChildFolders
	 *			set of folders to modify
	 * @throws NodeException
	 */
	protected void recursiveGetChildFolders(Folder folder, Set<Folder> allChildFolders) throws NodeException {
		List<Folder> childFolders = folder.getChildFolders();
		for (Folder child : childFolders) {
			if (allChildFolders.add(child)) {
				recursiveGetChildFolders(child, allChildFolders);
			}
		}
	}

	/**
	 * Transform the LinkRequest to a MultiLinkRequest for a single template
	 *
	 * @param id
	 *			template id
	 * @param request
	 *			LinkRequest
	 * @return MultiLinkRequest instance
	 */
	protected MultiLinkRequest transformToMultiRequest(String id, LinkRequest request) {
		MultiLinkRequest multiRequest = new MultiLinkRequest();
		multiRequest.setFolderIds(request.getFolderIds());
		multiRequest.setRecursive(request.isRecursive());
		multiRequest.setNodeId(request.getNodeId());
		multiRequest.setTemplateIds(new HashSet<String>(Arrays.asList(id)));

		return multiRequest;
	}

	protected Object getTemplateFieldOfParameter(Pair<Node, Template> nt, String key) throws NodeException {
		switch (key) {
		case "inheritedFrom":
			if (nt.getRight().getChannel() != null && nt.getRight().getChannel().getFolder() != null) {
				return nt.getRight().getChannel().getFolder().getName();
			} else {
				return StringUtils.EMPTY;
			}
		default:
			return nt.getRight().get(key);
		}
	}
}
