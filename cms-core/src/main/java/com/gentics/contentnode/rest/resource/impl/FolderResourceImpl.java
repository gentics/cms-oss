package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.createNodeConflictMessage;
import static com.gentics.contentnode.rest.util.MiscUtils.getItemList;
import static com.gentics.contentnode.rest.util.MiscUtils.getMatchingSystemUsers;
import static com.gentics.contentnode.rest.util.MiscUtils.reduceList;

import com.gentics.contentnode.publish.protocol.PublishProtocolUtil;
import com.gentics.contentnode.publish.protocol.PublishType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.AutoCommit;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.PageLanguageFallbackList;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionLockManager;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.factory.object.FolderFactory.ReductionType;
import com.gentics.contentnode.factory.object.ObjectModificationException;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.job.MoveJob;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder.FileSearch;
import com.gentics.contentnode.object.Folder.PageSearch;
import com.gentics.contentnode.object.Folder.TemplateSearch;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.utility.FileComparator;
import com.gentics.contentnode.object.utility.FolderComparator;
import com.gentics.contentnode.object.utility.PageComparator;
import com.gentics.contentnode.object.utility.TemplateComparator;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUrlFactory;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.FolderCreateRequest;
import com.gentics.contentnode.rest.model.request.FolderListRequest;
import com.gentics.contentnode.rest.model.request.FolderMoveRequest;
import com.gentics.contentnode.rest.model.request.FolderPublishDirSanitizeRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.IdSetRequest;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.request.MultiFolderLoadRequest;
import com.gentics.contentnode.rest.model.request.MultiFolderMoveRequest;
import com.gentics.contentnode.rest.model.request.StartpageRequest;
import com.gentics.contentnode.rest.model.request.WastebinSearch;
import com.gentics.contentnode.rest.model.response.FolderExternalLinksResponse;
import com.gentics.contentnode.rest.model.response.FolderListResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderObjectCountResponse;
import com.gentics.contentnode.rest.model.response.FolderPublishDirSanitizeResponse;
import com.gentics.contentnode.rest.model.response.FoundFilesListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ItemListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFolderListResponse;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.MultiFolderLoadResponse;
import com.gentics.contentnode.rest.model.response.PageExternalLink;
import com.gentics.contentnode.rest.model.response.PrivilegesResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.TemplateListResponse;
import com.gentics.contentnode.rest.resource.FolderResource;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FolderListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.TemplateListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.contentnode.rest.util.ResolvableComparator;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.staging.StagingUtil;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Resource for loading and manipulating folders in GCN
 * @author norbert
 */
@Path("/folder")
public class FolderResourceImpl extends AuthenticatedContentNodeResource implements FolderResource {
	/**
	 * Keyed lock to synchronize folder creation in same mother
	 */
	private static final TransactionLockManager<FolderLoadResponse> createLock = new TransactionLockManager<>();

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#create(com.gentics.contentnode.rest.model.request.FolderCreateRequest)
	 */
	@POST
	@Path("/create")
	public FolderLoadResponse create(FolderCreateRequest request) {
		try {
			return createLock.execute(request.getMotherId(), () -> doCreate(request));
		} catch (Exception e) {
			logger.error("Error while creating new folder", e);
			I18nString m = new CNI18nString("rest.general.error");

			return new FolderLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while creating new folder: " + e.getLocalizedMessage()), null);
		}
	}

	/**
	 * Create a folder
	 * @param request request
	 * @return response
	 */
	protected FolderLoadResponse doCreate(FolderCreateRequest request) {
		com.gentics.contentnode.object.Folder nodeFolder = null;

		try {
			Transaction t = TransactionManager.getCurrentTransaction();
			// get the mother folder
			com.gentics.contentnode.object.Folder motherFolder = null;
			try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
				motherFolder = getFolder(request.getMotherId(), false).getMaster();

				// check permission to create a folder
				if (!PermHandler.ObjectPermission.create.checkClass(motherFolder, com.gentics.contentnode.object.Folder.class, null)) {
					I18nString message = new CNI18nString("folder.nopermission");

					return new FolderLoadResponse(new Message(Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.PERMISSION, "Insufficient permissions to create folder"), null);
				}
			}

			// get the requested language for a possible startpage
			ContentLanguage language = MiscUtils.getRequestedContentLanguage(motherFolder, request.getLanguage());

			// when a startpage shall be created, we need to check the
			// permission (we check the permission on the motherfolder, since
			// the new folder will inherit all permissions from the
			// motherfolder)
			try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
				if (request.isStartpage() && !PermHandler.ObjectPermission.create.checkClass(motherFolder, Page.class, language)) {
					I18nString message = new CNI18nString("folder.nopermission");

					return new FolderLoadResponse(new Message(Type.CRITICAL, message.toString()),
							new ResponseInfo(ResponseCode.PERMISSION, "Insufficient permissions to create startpage"), null);
				}
			}

			// validate translations
			Message validationMessage = validate(motherFolder.getOwningNode(),
					Arrays.asList(request.getNameI18n(), request.getDescriptionI18n(), request.getPublishDirI18n()));
			if (validationMessage != null) {
				return new FolderLoadResponse(validationMessage, new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while creating folder: " + validationMessage.getMessage()), null);
			}

			// If set nodeId, create the page as local in the channel, corresponding to the provided nodeId.
			Integer channelId = request.getNodeId();
			NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

			// create a new folder
			nodeFolder = t.createObject(com.gentics.contentnode.object.Folder.class);
			nodeFolder.setMotherId(motherFolder.getId());

			// If multichannelling is active and nodeId is provided start preparing the page for local channel creation.
			boolean processMultichanneling = channelId != null && prefs.isFeature(Feature.MULTICHANNELLING);

			if (processMultichanneling) {
				Node folderNode = motherFolder.getNode();

				// for the case that a folder should be created in the root of a channel
				if (motherFolder.isChannelRoot()) {
					List<Node> masterNodes = folderNode.getMasterNodes();

					if (!masterNodes.isEmpty()) {
						folderNode = masterNodes.get(masterNodes.size() - 1);
					}
				}

				Node channel = t.getObject(Node.class, channelId);

				if (!folderNode.equals(channel)) {
					if (channel == null || !channel.isChannel()) {
						logger.error("Error while creating new folder: there is no channel for the specified nodeId.");
						I18nString m = new CNI18nString("rest.general.error");

						return new FolderLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
							new ResponseInfo(ResponseCode.FAILURE, "Error while creating new folder: there is no channel for the specified nodeId."), null);
					}

					// Check whether mother folder's node is one of the channel master nodes (check if the mother folder
					// is visible from the channel).
					if (!channel.getMasterNodes().contains(folderNode)) {
						logger.error("Error while creating new folder: mother folder is not in a master node of the specified channel.");
						I18nString m = new CNI18nString("rest.general.error");

						return new FolderLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
							new ResponseInfo(ResponseCode.FAILURE,
								"Error while creating new folder: mother folder is not in a master node of the specified channel."),
							null);
					}

					nodeFolder.setChannelInfo(channel.getId(), nodeFolder.getChannelSetId());
				}
			}

			// set the name and possibly check for duplicates
			Message message = updateName(nodeFolder, request.getName(), request.getNameI18n(), request.isFailOnDuplicate());
			if (message != null) {
				return new FolderLoadResponse(message, new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while creating folder: " + message.getMessage(), "name"), null);
			}

			if (request.getPublishDir() != null && request.getPublishDir().length() > 255) {
				I18nString m = new CNI18nString("rest.folder.pub_dir.maxlength");
				return new FolderLoadResponse(new Message(Message.Type.CRITICAL, m.toString()), new ResponseInfo(ResponseCode.FAILURE, m.toString()), null);
			}

			Set<String> translations = new HashSet<>();
			translations.addAll(Optional.ofNullable(request.getNameI18n()).map(Map::keySet).orElse(Collections.emptySet()));
			translations.addAll(Optional.ofNullable(request.getDescriptionI18n()).map(Map::keySet).orElse(Collections.emptySet()));
			translations.addAll(Optional.ofNullable(request.getPublishDirI18n()).map(Map::keySet).orElse(Collections.emptySet()));

			message = updatePublishDir(nodeFolder, request.getPublishDir(), request.getPublishDirI18n(), request.isFailOnDuplicate(), translations);
			if (message != null) {
				return new FolderLoadResponse(message, new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while creating folder: " + message.getMessage(), "publishDir"), null);
			}

			// get the inheritable object tags from the mother folder (from the channel, were we create the folder)
			com.gentics.contentnode.object.Folder copyObjectTags = motherFolder;
			if (processMultichanneling) {
				Node channel = t.getObject(Node.class, channelId);
				if (channel != null) {
					copyObjectTags = MultichannellingFactory.getChannelVariant(motherFolder, channel);
				}
			}
			Map<String, ObjectTag> motherObjectTags = copyObjectTags.getObjectTags();
			for (Map.Entry<String, ObjectTag> entry : motherObjectTags.entrySet()) {
				String name = entry.getKey();
				ObjectTag motherObjectTag = entry.getValue();
				ObjectTagDefinition definition = motherObjectTag.getDefinition();
				if (definition == null) {
					continue;
				}
				if (definition.getObjectTag().isInheritable()) {
					ObjectTag objectTag = nodeFolder.getObjectTag(name);
					if (objectTag == null) {
						continue;
					}
					objectTag.copyFrom(motherObjectTag);
				}
			}

			if (request.getDescription() != null) {
				nodeFolder.setDescription(request.getDescription());
			}
			if (request.getDescriptionI18n() != null) {
				nodeFolder.setDescriptionI18n(I18nMap.TRANSFORM2NODE.apply(request.getDescriptionI18n()));
			}

			// link the templates, which are linked to the mother folder
			List<Template> templates = motherFolder.getTemplates();

			nodeFolder.setTemplates(templates);

			nodeFolder.save();
			t.refreshPermHandler();

			// when a startpage shall be created, we do this now
			if (request.isStartpage()) {
				Page page = null;
				Template template = null;

				// if a template id was requested, try to get that template
				if (request.getTemplateId() != null) {
					for (Iterator<Template> i = templates.iterator(); i.hasNext();) {
						Template temp = i.next();

						if (request.getTemplateId().equals(temp.getId())) {
							template = temp;
							break;
						}
					}
				}

				// if no template id requested or template not found in the folder, get the first template from the list
				if (template == null && templates.size() > 0) {
					template = templates.iterator().next();
				}

				// if still no template found, we have a failure
				if (template == null) {
					I18nString msg = new CNI18nString("rest.general.insufficientdata");

					return new FolderLoadResponse(new Message(Type.CRITICAL, msg.toString()),
							new ResponseInfo(ResponseCode.FAILURE, "No template was found for creating the startpage."), null);
				}

				// now we are ready to create the page
				page = t.createObject(Page.class);

				// set the folder id
				page.setFolderId(nodeFolder.getId());

				// set the template id
				page.setTemplateId(template.getId());

				// the page name defaults to the folder name
				page.setName(request.getName());

				// transform the requested language code into a language and set it to the page
				if (language != null) {
					page.setLanguage(language);
				}

				page.save();

				// and set the page as startpage
				String startPagePropName = getStartpageObjectPropertyName(nodeFolder);

				// when a property is defined, we try to set the start page
				if (!StringUtils.isEmpty(startPagePropName)) {
					// get the objecttag
					ObjectTag startPageTag = nodeFolder.getObjectTags().get(startPagePropName);

					if (startPageTag != null) {
						// find the first value of type PageUrlPartType
						ValueList values = startPageTag.getValues();
						Value urlValue = null;

						for (Value v: values) {
							if (v.getPartType() instanceof PageURLPartType) {
								urlValue = v;
								break;
							}
						}
						// when we found an appropriate value, we set the data
						if (urlValue != null) {
							// set the internal page
							urlValue.setInfo(1);
							urlValue.setValueRef(ObjectTransformer.getInt(page.getId(), 0));
							// enable the objecttag (otherwise we won't see anything)
							startPageTag.setEnabled(true);
							// save the folder
							nodeFolder.save();
						}
					}
				}
			}

			// commit the transaction
			t.commit(false);

			String folderId = Integer.toString(ObjectTransformer.getInteger(nodeFolder.getId(), null));

			// load the folder and return
			return load(folderId, true, false, false, request.getNodeId(), null);
		} catch (NodeException e) {
			logger.error("Error while creating new folder", e);
			I18nString m = new CNI18nString("rest.general.error");

			return new FolderLoadResponse(new Message(Message.Type.CRITICAL, m.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while creating new folder: " + e.getLocalizedMessage()), null);
		}
	}

	/**
	 * Creates critical error response
	 * @param throwable Error exception
	 * @param responseCode Error code response
	 * @param message Message
	 * @return A {@code FolderExternalLinksResponse} with the message type
	 * 		{@code CRITICAL} and the given response code and message
	 */
	private FolderExternalLinksResponse createCriticalErrorResponse(Throwable throwable, ResponseCode responseCode, String message) {
		return new FolderExternalLinksResponse(
				new Message(Type.CRITICAL, message),
				new ResponseInfo(responseCode, throwable.getMessage()));
	}

	/**
	 * Creates critical error response
	 * @param throwable Error exception
	 * @param responseCode Error Code response
	 * @return A {@code FolderExternalLinksResponse} with the message type
	 * 		{@code CRITICAL} and the given response code and the localized
	 * 		message from the throwable
	 */
	private FolderExternalLinksResponse createCriticalErrorResponse(Throwable throwable, ResponseCode responseCode) {
		return createCriticalErrorResponse(throwable, responseCode, throwable.getLocalizedMessage());
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FolderResource#getExternalLinks(java.lang.String, boolean)
	 */
	public FolderExternalLinksResponse getExternalLinks(Integer folderId,
			boolean recursive) {
		FolderExternalLinksResponse externalLinksResponse = new FolderExternalLinksResponse();

		try {
			List<Integer> folderIds = new ArrayList<>();
			folderIds.add(folderId);

			if (recursive) {
				recursiveGetFolderIds(folderId, folderIds);
			}

			externalLinksResponse.setPages(getExternalLinks(folderIds));
		} catch (EntityNotFoundException e) {
			return createCriticalErrorResponse(e, ResponseCode.NOTFOUND);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return createCriticalErrorResponse(e, ResponseCode.PERMISSION);
		} catch (NodeException e) {
			logger.error("Error while loading folder " + folderId, e);
			I18nString message = new CNI18nString("rest.general.error");

			return createCriticalErrorResponse(e, ResponseCode.FAILURE, message.toString());
		} catch (SQLException e) {
			String message = "Error while finding pages, starting from folder {" + folderId + "}";

			throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build());
		}

		return externalLinksResponse;
	}

	/**
	 * Gets external links from all the pages of the folder list 'folderIds'.
	 * @param folderIds Folder List
	 * @return list of all external links
	 * @throws NodeException
	 */
	private List<PageExternalLink> getExternalLinks(final List<Integer> folderIds) throws NodeException {
		final List<PageExternalLink> externalLinks = new ArrayList<>();

		String sql = "select pag.id as page_id, pag.name as page_name, v.value_text as value_text  " +
					"from value v, contenttag c, part p,  page pag " +
					"where v.contenttag_id = c.id  " +
					  "and p.id = v.part_id  " +
					  "and c.content_id = pag.content_id " +
					  "and p.type_id = ?  " +
					  "and v.info = ? " +
					  "and pag.deleted = 0 " +
					  "and pag.folder_id in ";

		DBUtils.executeMassStatement(sql, " order by pag.id ", folderIds, 3, new SQLExecutor() {
			/* (non-Javadoc)
			 * @see com.gentics.lib.db.SQLExecutor#prepareStatement(java.sql.PreparedStatement)
			 */
			@Override
			public void prepareStatement(PreparedStatement stmt)
					throws SQLException {
				super.prepareStatement(stmt);

				stmt.setInt(1, PageURLPartType.EXTERNAL_URL_TYPE);
				stmt.setInt(2, PageURLPartType.EXTERNAL_URL_INFO);
			}

			/* (non-Javadoc)
			 * @see com.gentics.lib.db.SQLExecutor#handleResultSet(java.sql.ResultSet)
			 */
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException,
					NodeException {
				super.handleResultSet(rs);

				int pageId;
				int lastPageId = -1;

				String valueText;
				PageExternalLink pageExternalLink = null;

				List<String> links = null;

				while(rs.next()) {
					pageId = rs.getInt("page_id");
					valueText = rs.getString("value_text");

					// As list is ordered by pageId, we can know which
					// links belongs to which pages just by looking when the pageId
					// changes.
					if (lastPageId != pageId) {
						lastPageId = pageId;

						if (links != null) {
							pageExternalLink.setLinks(links);
							externalLinks.add(pageExternalLink);
						}

						pageExternalLink = new PageExternalLink();
						pageExternalLink.setPageId(pageId);
						pageExternalLink.setPageName(rs.getString("page_name"));

						links = new ArrayList<>();
					}

					links.add(valueText);
				}

				if (links != null) {
					pageExternalLink.setLinks(links);
					externalLinks.add(pageExternalLink);
				}
			}
		});
		return externalLinks ;
	}
	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#load(java.lang.String, boolean, java.lang.Integer)
	 */
	@GET
	@Path("/load/{id}")
	public FolderLoadResponse load(@PathParam("id") String folderId,
			@DefaultValue("false") @QueryParam("update") boolean update,
			@QueryParam("privileges") @DefaultValue("false") boolean addPrivileges,
			@QueryParam("construct") @DefaultValue("false") boolean construct,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("package") String stagingPackageName) {
		Folder restFolder = null;

		// Check which references shall be added
		Collection<Reference> fillRefs = new Vector<>();

		if (addPrivileges) {
			fillRefs.add(Reference.PRIVILEGES);
		}
		fillRefs.add(Reference.TAGS);
		fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
		if (construct) {
			fillRefs.add(Reference.TAG_EDIT_DATA);
		}

		// load the folder
		com.gentics.contentnode.object.Folder nodeFolder;
		boolean channelIdSet = false;

		Transaction t = null;
		try {
			t = TransactionManager.getCurrentTransaction();
			channelIdSet = setChannelToTransaction(nodeId);

			nodeFolder = getFolder(folderId, update);

			// transform the folder into a REST object
			restFolder = ModelBuilder.getFolder(nodeFolder, null, fillRefs);

			ResponseInfo responseInfo = new ResponseInfo(ResponseCode.OK, "Folder loaded");

			FolderLoadResponse response = new FolderLoadResponse(null, responseInfo, restFolder);
			response.setStagingStatus(StagingUtil.checkStagingStatus(nodeFolder, stagingPackageName));
			return response;
		} catch (EntityNotFoundException e) {
			return new FolderLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getLocalizedMessage()),
					null);
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FolderLoadResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getLocalizedMessage()), null);
		} catch (NodeException e) {
			logger.error("Error while loading folder " + folderId, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FolderLoadResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()), null);
		} finally {
			if (t != null && channelIdSet) {
				t.resetChannel();
			}
		}
	}

	@Override
	@POST
	@Path("/load")
	public MultiFolderLoadResponse load(MultiFolderLoadRequest request, @QueryParam("fillWithNulls") @DefaultValue("false") boolean fillWithNulls) {
		Transaction t = getTransaction();
		Set<Reference> references = new HashSet<>();

		if (ObjectTransformer.getBoolean(request.isAddPrivileges(), false)) {
			references.add(Reference.PRIVILEGES);
		}

		references.add(Reference.TAGS);
		references.add(Reference.OBJECT_TAGS_VISIBLE);

		try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
			boolean forUpdate = ObjectTransformer.getBoolean(request.isForUpdate(), false);
			List<com.gentics.contentnode.object.Folder> allFolders =
				t.getObjects(com.gentics.contentnode.object.Folder.class, request.getIds());

			List<Folder> returnedFolders = getItemList(request.getIds(), allFolders, folder -> {
				Set<Integer> ids = new HashSet<>();
				ids.add(folder.getId());
				ids.addAll(folder.getChannelSet().values());
				return ids;
			}, folder -> {
				if (forUpdate) {
					folder = t.getObject(folder, true);
				}
				return ModelBuilder.getFolder(folder, null, references);
			}, folder -> {
				return ObjectPermission.view.checkObject(folder)
						&& (!forUpdate || ObjectPermission.edit.checkObject(folder));
			}, fillWithNulls);

			MultiFolderLoadResponse response = new MultiFolderLoadResponse(returnedFolders);

			// TODO make for filtered folders
			response.setStagingStatus(StagingUtil.checkStagingStatus(allFolders, request.getPackage(), o -> o.getGlobalId().toString()));
			return response;
		} catch (NodeException e) {
			return new MultiFolderLoadResponse(
				new Message(Type.CRITICAL, e.getLocalizedMessage()),
				new ResponseInfo(ResponseCode.FAILURE, "Could not load folders"));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#getBreadcrumb(java.lang.String, java.lang.Integer)
	 */
	@GET
	@Path("/breadcrumb/{id}")
	public LegacyFolderListResponse getBreadcrumb(
			@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("wastebin") @DefaultValue("false") boolean includeWastebin,
			@QueryParam("tags") @DefaultValue("false") boolean includeTags) {
		Transaction t = getTransaction();
		boolean channelIdSet = false;
		Collection<Reference> fillRefs = new Vector<>();

		if (includeTags) {
			fillRefs.add(Reference.TAGS);
			fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
		}

		try {
			channelIdSet = setChannelToTransaction(nodeId);

			try (WastebinFilter filter = getWastebinFilter(includeWastebin, id)) {
				com.gentics.contentnode.object.Folder folder = getFolder(id, false);
				List<Folder> restFolders = new Vector<>();

				while (folder != null) {
					restFolders.add(0, ModelBuilder.getFolder(folder, null, fillRefs));
					folder = folder.getMother();
				}

				LegacyFolderListResponse response = new LegacyFolderListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched breadcrumb for folder " + id));

				response.setFolders(restFolders);
				return response;
			}
		} catch (EntityNotFoundException e) {
			return new LegacyFolderListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getLocalizedMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new LegacyFolderListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getLocalizedMessage()));
		} catch (NodeException e) {
			logger.error("Error while loading breadcrumb for folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new LegacyFolderListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				t.resetChannel();
			}
		}
	}

	@Override
	@GET
	@Path("/getPages/{id}")
	public LegacyPageListResponse getPages(
			@PathParam("id") String id,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam PageListParameterBean pageListParams,
			@BeanParam LegacyFilterParameterBean filterParams,
			@BeanParam LegacySortParameterBean sortingParams,
			@BeanParam LegacyPagingParameterBean pagingParams,
			@BeanParam PublishableParameterBean publishParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		try {
			Transaction t = getTransaction();
			boolean channelIdSet = false;
			boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

			inFolder.setFolderId(id);

			try {
				channelIdSet = setChannelToTransaction(pageListParams.nodeId);

				try (WastebinFilter filter = getWastebinFilter(includeWastebin, id)) {
					// load the folder
					com.gentics.contentnode.object.Folder f = getFolder(id, false);

					// if a language was set, we need to do language fallback here
					ContentLanguage lang = null;
					Node node = f.getNode();

					if (!ObjectTransformer.isEmpty(pageListParams.language)) {
						List<ContentLanguage> languages = node.getLanguages();

						for (ContentLanguage contentLanguage : languages) {
							if (pageListParams.language.equals(contentLanguage.getCode()) || pageListParams.language.equals(ObjectTransformer.getString(contentLanguage.getId(), null))) {
								lang = contentLanguage;
								break;
							}
						}

						// when the language was NOT found and language fallback is disabled, we will return an empty list
						if (lang == null && !pageListParams.langFallback && !"0".equals(pageListParams.language)) {
							LegacyPageListResponse response = new LegacyPageListResponse();

							response.setHasMoreItems(false);
							response.setNumItems(0);
							response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully loaded pages"));
							return response;
						} else if (lang == null && pageListParams.langFallback) {
							// language fallback is activated, but the requested
							// language was not found, so get the first language for the
							// node (if any)
							if (languages.size() > 0) {
								lang = languages.get(0);
							}
						}
					}

					// Check which references shall be added
					Collection<Reference> fillRefs = new Vector<>();

					if (pageListParams.template) {
						fillRefs.add(Reference.TEMPLATE);
					}
					if (pageListParams.folder) {
						fillRefs.add(Reference.FOLDER);
					}
					if (pageListParams.languageVariants) {
						fillRefs.add(Reference.LANGUAGEVARIANTS);
					}
					if (pageListParams.workflowOwn || pageListParams.workflowWatch) {
						fillRefs.add(Reference.WORKFLOW);
					}
					if (pageListParams.translationStatus || pageListParams.inSync != null) {
						fillRefs.add(Reference.TRANSLATIONSTATUS);
					}
					if (pageListParams.contentTags) {
						fillRefs.add(Reference.CONTENT_TAGS);
					}
					if (pageListParams.objectTags) {
						fillRefs.add(Reference.OBJECT_TAGS_VISIBLE);
					}

					// if restricting with templateIds, make sure we use the IDs of the master objects
					if (!ObjectTransformer.isEmpty(pageListParams.templateIds) && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING)) {
						List<Template> templates = t.getObjects(Template.class, pageListParams.templateIds);
						List<Integer> templateMasterIds = new ArrayList<>(templates.size());
						for (Template tmpl : templates) {
							templateMasterIds.add(tmpl.getMaster().getId());
						}
						pageListParams.templateIds = templateMasterIds;
					}

					// get the pages
					List<Page> pages = getPagesFromFolder(f,
						PageSearch.create().setSearchString(filterParams.search).setSearchContent(pageListParams.searchContent).setFileNameSearch(pageListParams.filename)
							.setNiceUrlSearch(pageListParams.niceUrl)
							.setWorkflowOwn(pageListParams.workflowOwn).setWorkflowWatch(pageListParams.workflowWatch)
							.setEditor(publishParams.isEditor).setCreator(publishParams.isCreator).setPublisher(publishParams.isPublisher)
							.setOnline(publishParams.online).setModified(publishParams.modified)
							.setPlanned(pageListParams.planned).setQueued(pageListParams.queued)
							.setPriority(pageListParams.priority).setTemplateIds(pageListParams.templateIds)
							.setPermissions(pageListParams.permission)
							.setEditors(getMatchingSystemUsers(publishParams.editor, publishParams.editorIds))
							.setCreators(getMatchingSystemUsers(publishParams.creator, publishParams.creatorIds))
							.setPublishers(getMatchingSystemUsers(publishParams.publisher, publishParams.publisherIds))
							.setEditedBefore(publishParams.editedBefore).setEditedSince(publishParams.editedSince)
							.setCreatedBefore(publishParams.createdBefore).setCreatedSince(publishParams.createdSince)
							.setPublishedBefore(publishParams.publishedBefore).setPublishedSince(publishParams.publishedSince)
							.setRecursive(inFolder.recursive).setInherited(pageListParams.inherited)
							.setWastebin(wastebinParams.wastebinSearch == WastebinSearch.only)
							.setIncludeMlIds(pageListParams.includeMlIds)
							.setExcludeMlIds(pageListParams.excludeMlIds),
							pageListParams.timeDue,
							pageListParams.inSync);

					if (wastebinParams.wastebinSearch == WastebinSearch.only) {
						Wastebin.ONLY.filter(pages);
					}

					// check view permission on the pages
					for (Iterator<Page> iter = pages.iterator(); iter.hasNext(); ) {
						Page page = iter.next();
						if (!PermHandler.ObjectPermission.view.checkObject(page)) {
							iter.remove();
						}
					}

					// if a language was set, we need to filter out pages of the wrong language
					if (lang != null) {
						if (pageListParams.langFallback) {
							// if language fallback shall be used, we need to do it
							// now
							PageLanguageFallbackList fallbackList = new PageLanguageFallbackList(lang, node);

							for (Page page : pages) {
								fallbackList.addPage(page);
							}
							pages = fallbackList.getPages();
						} else {
							// otherwise we simply ignore languages of the wrong
							// language
							for (Iterator<Page> iPages = pages.iterator(); iPages.hasNext();) {
								Page page = iPages.next();

								if (!lang.equals(page.getLanguage())) {
									iPages.remove();
								}
							}
						}
					} else if ("0".equals(pageListParams.language)) {
						// "0" was set as language, so we only return pages WITHOUT language
						for (Iterator<Page> iPages = pages.iterator(); iPages.hasNext();) {
							Page page = iPages.next();

							if (page.getLanguage() != null) {
								iPages.remove();
							}
						}
					} else {
						// do the fallback with no specific language
						PageLanguageFallbackList fallbackList = new PageLanguageFallbackList(null, node);

						fallbackList.setCheckViewPermission(true);

						for (Page page : pages) {
							fallbackList.addPage(page);
						}
						pages = fallbackList.getPages();
					}

					// optionally sort the list
					if (!ObjectTransformer.isEmpty(sortingParams.sortBy) && !ObjectTransformer.isEmpty(sortingParams.sortOrder)) {
						Collections.sort(pages, new PageComparator(sortingParams.sortBy, sortingParams.sortOrder));
					}

					// create the response
					LegacyPageListResponse response = new LegacyPageListResponse();

					response.setHasMoreItems(pagingParams.maxItems >= 0 && (pages.size() > pagingParams.skipCount + pagingParams.maxItems));
					response.setNumItems(pages.size());
					reduceList(pages, pagingParams.skipCount, pagingParams.maxItems);

					List<com.gentics.contentnode.rest.model.Page> restPages = new ArrayList<>(pages.size());

					Wastebin wastebin = null;
					switch (wastebinParams.wastebinSearch) {
					case include:
						wastebin = Wastebin.INCLUDE;
						break;
					case only:
						wastebin = Wastebin.ONLY;
						break;
					case exclude:
					default:
						wastebin = Wastebin.EXCLUDE;
						break;
					}
					for (Page page : pages) {
						try {
							restPages.add(ModelBuilder.getPage(page, fillRefs, wastebin));
						} catch (InconsistentDataException e) {
							logger.error("Error while fetching page {" + page.getId() + "}", e);
						}
					}


					response.setPages(restPages);
					response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully loaded pages"));
					response.setStagingStatus(StagingUtil.checkStagingStatus(pages, inFolder.stagingPackageName, o -> o.getGlobalId().toString(), pageListParams.languageVariants));
					return response;
				}
			} finally {
				if (channelIdSet) {
					// render type channel wieder auf null setzen
					t.resetChannel();
				}
			}
		} catch (EntityNotFoundException e) {
			return new LegacyPageListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new LegacyPageListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting pages", e);
			I18nString message = new CNI18nString("rest.general.error");

			return new LegacyPageListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getMessage()));
		}
	}



	/**
	 * Get the pages from the given folder. Possibly do a search.
	 *
	 * @param f
	 *            folder
	 * @param search
	 *            search (if null, no search is done and all pages will be
	 *            returned)
	 * @param timeDue
	 *            difference in seconds for considering pages which will change
	 *            their state within the given time span. 0 for not considering
	 *            timemanagement
	 * @param inSync
	 *            {@link Boolean#TRUE}, if only pages in sync with their
	 *            translation masters shall be fetched, {@link Boolean#FALSE},
	 *            if only pages not in sync with their translation masters shall
	 *            be fetched, NULL for not considering the translation status
	 * @return list of pages
	 * @throws NodeException
	 */
	protected List<Page> getPagesFromFolder(com.gentics.contentnode.object.Folder f,
			PageSearch search, int timeDue, Boolean inSync) throws NodeException {
		// get the pages of the folder
		List<Page> pages = new Vector<>(f.getPages(search));

		// when time management restriction or translation status restriction is set, we consider it now
		if (timeDue > 0 || inSync != null) {
			int now = getTransaction().getUnixTimestamp();

			for (Iterator<Page> pagesIt = pages.iterator(); pagesIt.hasNext();) {
				Page page = pagesIt.next();
				boolean removePage = false;

				// check timemanagement if that shall be restricted
				if (timeDue > 0) {
					boolean hasTimeManagement = false;

					if (page.isOnline() && page.getTimeOff().getTimestamp() > 0) {
						// page is online and shall go offline due to
						// timemanagement
						if (page.getTimeOff().getTimestamp() <= now + timeDue) {
							hasTimeManagement = true;
						}
					} else if (!page.isOnline() && page.getTimePub().getTimestamp() > 0) {
						// page is offline and shall go online due to
						// timemanagement
						if (page.getTimePub().getTimestamp() <= now + timeDue) {
							hasTimeManagement = true;
						}
					}

					removePage = !hasTimeManagement;
				}

				// check translation status if it shall be restricted
				if (!removePage && inSync != null) {
					removePage = page.isInSync() != inSync.booleanValue();
				}

				if (removePage) {
					pagesIt.remove();
				}
			}
		}

		return pages;
	}

	@Override
	@GET
	@Path("/getFiles/{folderId}")
	public LegacyFileListResponse getFiles(
			@PathParam("folderId") String folderId,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam FileListParameterBean fileListParams,
			@BeanParam LegacyFilterParameterBean filterParams,
			@BeanParam LegacySortParameterBean sortingParams,
			@BeanParam LegacyPagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		return getFilesOrImages(ContentFile.TYPE_FILE, inFolder.setFolderId(folderId), fileListParams, filterParams, sortingParams, pagingParams, editableParams, wastebinParams);
	}

	@Override
	@GET
	@Path("/getImages/{folderId}")
	public LegacyFileListResponse getImages(
			@PathParam("folderId") String folderId,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam FileListParameterBean fileListParams,
			@BeanParam LegacyFilterParameterBean filterParams,
			@BeanParam LegacySortParameterBean sortingParams,
			@BeanParam LegacyPagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		return getFilesOrImages(ContentFile.TYPE_IMAGE, inFolder.setFolderId(folderId), fileListParams, filterParams, sortingParams, pagingParams, editableParams, wastebinParams);
	}

	/**
	 * Get all images or files from a folder, possibly also from its sub-folders.
	 * Returns only images from folders with viewing permissions.
	 *
	 * @param f folder
	 * @param type type of objects to get (images or files)
	 * @param search optional search string to restrict images/files
	 * @return list of images or files
	 * @throws NodeException
	 */
	protected List<File> getFilesOrImagesFromFolder(com.gentics.contentnode.object.Folder f, int type,
			FileSearch search) throws NodeException {
		List<File> imagesOrFiles = null;

		if (type == ContentFile.TYPE_FILE) {
			imagesOrFiles = new Vector<>(f.getFiles(search));
		} else if (type == ContentFile.TYPE_IMAGE) {
			imagesOrFiles = new Vector<>(f.getImages(search));
		} else {
			return null;
		}

		List<File> filesWithPermission = new Vector<>();
		for (File file : imagesOrFiles) {
			if (PermHandler.ObjectPermission.view.checkClass(file.getFolder(), File.class, null)) {
				filesWithPermission.add(file);
			}
		}

		return filesWithPermission;
	}

	/**
	 * get files or images for a folder including search options etc.
	 * @param type type of returned objects
	 * @param inFolder folder parameters
	 * @param fileListParams file list parameters
	 * @param filterParams filter parameters
	 * @param sortingParams sorting parameters
	 * @param pagingParams paging parameters
	 * @param editableParams editable parameters
	 * @param wastebinParams wastebin parameters
	 * @return response
	 */
	protected LegacyFileListResponse getFilesOrImages(
			int type,
			InFolderParameterBean inFolder,
			FileListParameterBean fileListParams,
			LegacyFilterParameterBean filterParams,
			LegacySortParameterBean sortingParams,
			LegacyPagingParameterBean pagingParams,
			EditableParameterBean editableParams,
			WastebinParameterBean wastebinParams) {
		Transaction t = getTransaction();
		boolean channelIdSet = false;
		boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

		try {
			channelIdSet = setChannelToTransaction(fileListParams.nodeId);

			try (WastebinFilter filter = getWastebinFilter(includeWastebin, inFolder.folderId)) {
			com.gentics.contentnode.object.Folder f = getFolder(inFolder.folderId, false);
				List<File> imagesOrFiles = getFilesOrImagesFromFolder(f, type,
					FileSearch.create().setSearchString(filterParams.search).setNiceUrlSearch(fileListParams.niceUrl)
						.setEditors(getMatchingSystemUsers(editableParams.editor, editableParams.editorIds))
						.setCreators(getMatchingSystemUsers(editableParams.creator, editableParams.creatorIds))
						.setEditedBefore(editableParams.editedBefore).setEditedSince(editableParams.editedSince)
						.setCreatedBefore(editableParams.createdBefore).setCreatedSince(editableParams.createdSince)
						.setRecursive(inFolder.recursive).setInherited(fileListParams.inherited).setOnline(fileListParams.online).setBroken(fileListParams.broken)
						.setUsed(fileListParams.used).setUsedIn(fileListParams.usedIn).setWastebin(wastebinParams.wastebinSearch == WastebinSearch.only));

				if (wastebinParams.wastebinSearch == WastebinSearch.only) {
					Wastebin.ONLY.filter(imagesOrFiles);
				}

				if (!ObjectTransformer.isEmpty(sortingParams.sortBy) && !ObjectTransformer.isEmpty(sortingParams.sortOrder)) {
					Collections.sort(imagesOrFiles, new FileComparator(sortingParams.sortBy, sortingParams.sortOrder));
				}

				// create the response
				LegacyFileListResponse response = new LegacyFileListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded files"));

				response.setNumItems(imagesOrFiles.size());
				response.setHasMoreItems(pagingParams.maxItems >= 0 && (imagesOrFiles.size() > pagingParams.skipCount + pagingParams.maxItems));
				reduceList(imagesOrFiles, pagingParams.skipCount, pagingParams.maxItems);

				List<com.gentics.contentnode.rest.model.File> restFiles = new ArrayList<>(imagesOrFiles.size());

				Collection<Reference> refs = new ArrayList<>();
				if (fileListParams.folder) {
					refs.add(Reference.FOLDER);
				}
				for (File file : imagesOrFiles) {
					if (type == ContentFile.TYPE_FILE) {
						restFiles.add(ModelBuilder.getFile(file, refs));
					} else if (type == ContentFile.TYPE_IMAGE) {
						restFiles.add(ModelBuilder.getImage((ImageFile) file, refs));
					}
				}
				response.setFiles(restFiles);
				response.setStagingStatus(StagingUtil.checkStagingStatus(imagesOrFiles, inFolder.stagingPackageName, o -> o.getGlobalId().toString()));
				return response;
			}
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new LegacyFileListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new LegacyFileListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting files or images for folder " + inFolder.folderId, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new LegacyFileListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				// reset transaction channel
				t.resetChannel();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FolderResource#getFolders(com.gentics.contentnode.rest.model.request.FolderListRequest)
	 */
	public LegacyFolderListResponse getFolders(FolderListRequest folderListRequest) {
		return getFolders(
			folderListRequest.getId(),
			folderListRequest.getRecursiveIds(),
			folderListRequest.isAddPrivileges(),
			new InFolderParameterBean().setFolderId(folderListRequest.getId()).setRecursive(folderListRequest.isRecursive()).setStagingPackageName(folderListRequest.getStagingPackageName()),
			new FolderListParameterBean().setNodeId(folderListRequest.getNodeId()).setTree(folderListRequest.isTree()).setInherited(folderListRequest.getInherited()).setPrivilegeMap(folderListRequest.isPrivilegeMap()),
			new LegacyFilterParameterBean().setSearch(folderListRequest.getSearch()),
			new LegacySortParameterBean().setSortBy(folderListRequest.getSortBy()).setSortOrder(folderListRequest.getSortOrder()),
			new LegacyPagingParameterBean().setMaxItems(folderListRequest.getMaxItems()).setSkipCount(folderListRequest.getSkipCount()),
			new EditableParameterBean().setEditor(folderListRequest.getEditor()).setCreator(folderListRequest.getCreator()).setEditedBefore(folderListRequest.getEditedBefore()).setEditedSince(folderListRequest.getEditedSince()).setCreatedBefore(folderListRequest.getCreatedBefore()).setCreatedSince(folderListRequest.getCreatedSince()),
			new WastebinParameterBean().setWastebinSearch(folderListRequest.getWastebin()));
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FolderResource#getFolders(java.lang.String, java.lang.Integer, int, int, boolean, java.lang.String, java.lang.String, java.lang.Boolean, java.lang.String, java.lang.String, java.lang.String, int, int, boolean, java.util.List, boolean)
	 */
	@GET
	@Path("/getFolders/{id}")
	public LegacyFolderListResponse getFolders(
			@PathParam("id") String id,
			@QueryParam("recId") List<String> recursiveIds,
			@QueryParam("privileges") @DefaultValue("false") boolean addPrivileges,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam FolderListParameterBean folderListParams,
			@BeanParam LegacyFilterParameterBean filterParams,
			@BeanParam LegacySortParameterBean sortParams,
			@BeanParam LegacyPagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		Transaction t = getTransaction();
		boolean channelIdSet = false;
		boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

		// Make sure the parameter beans and the explicit parameters are the same.
		inFolder.setFolderId(id);
		folderListParams.setRecursiveIds(recursiveIds);
		folderListParams.setAddPrivileges(addPrivileges);

		try {
			channelIdSet = setChannelToTransaction(folderListParams.nodeId);

			List<com.gentics.contentnode.object.Folder> folders = null;

			if ("0".equalsIgnoreCase(id)) {
				// get the root folders
				PreparedStatement pst = null;
				ResultSet res = null;

				try {
					pst = t.prepareStatement("SELECT id FROM folder WHERE mother = ? AND deleted = 0");
					pst.setInt(1, 0);
					res = pst.executeQuery();

					Collection<Integer> ids = new ArrayList<>();

					while (res.next()) {
						ids.add(res.getInt("id"));
					}

					folders = new ArrayList<>(t.getObjects(com.gentics.contentnode.object.Folder.class, ids));
				} catch (SQLException e) {
					throw new NodeException("Error while getting root folders", e);
				} finally {
					t.closeResultSet(res);
					t.closeStatement(pst);
				}

			} else {
				// get the list of subfolders
				try (WastebinFilter filter = getWastebinFilter(includeWastebin, id)) {
					// get the given folder
					com.gentics.contentnode.object.Folder f = getFolder(id, false);

					folders = new ArrayList<>(f.getChildFolders());
				}
			}

			// remove the folders with insufficient permission
			try (WastebinFilter filter = getWastebinFilter(includeWastebin, id)) {
				for (Iterator<com.gentics.contentnode.object.Folder> i = folders.iterator(); i.hasNext(); ) {
					com.gentics.contentnode.object.Folder folder = i.next();

					if (!PermHandler.ObjectPermission.view.checkObject(folder)) {
						i.remove();
					}
				}
			}

			// when folders shall be fetched recursively, do this now
			if (inFolder.recursive && !folderListParams.tree) {
				List<com.gentics.contentnode.object.Folder> subFolders = new ArrayList<>();

				for (com.gentics.contentnode.object.Folder folder : folders) {
					try (WastebinFilter filter = WastebinFilter.get(includeWastebin, folder.getOwningNode())) {
						subFolders.addAll(recursiveGetSubfolders(folder, recursiveIds, PermHandler.ObjectPermission.view));
					}
				}
				folders.addAll(subFolders);
			}

			// filter by optional search criteria
			filterBySearch(folders, folderListParams.inherited, filterParams.search, editableParams, wastebinParams.wastebinSearch);

			// create a response with the folders
			LegacyFolderListResponse response = new LegacyFolderListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded subfolders"));

			response.setNumItems(folders.size());
			response.setHasMoreItems(pagingParams.maxItems >= 0 && (folders.size() > pagingParams.skipCount + pagingParams.maxItems));

			folders = new ArrayList<>(folders);

			// sort the list
			Comparator<com.gentics.contentnode.object.Folder> comparator = null;
			if (sortParams.sortBy != null && sortParams.sortOrder != null) {
				comparator = new FolderComparator(sortParams.sortBy, sortParams.sortOrder);
				try (WastebinFilter filter = getWastebinFilter(includeWastebin, id)) {
					Collections.sort(folders, comparator);
				}
			}

			// reduce the list if necessary
			reduceList(folders, pagingParams.skipCount, pagingParams.maxItems);

			List<Folder> restFolders = new ArrayList<>(folders.size());

			Collection<Reference> fillRefs = new ArrayList<>();
			if (addPrivileges) {
				fillRefs.add(Reference.PRIVILEGES);
			}
			if (folderListParams.privilegeMap) {
				fillRefs.add(Reference.PRIVILEGEMAP);
			}
			if (fillRefs.isEmpty()) {
				fillRefs = null;
			}
			// convert the folders into REST objects, but omit all children
			for (com.gentics.contentnode.object.Folder folder : folders) {
				try (WastebinFilter filter = WastebinFilter.get(includeWastebin, folder.getOwningNode())) {
					restFolders.add(ModelBuilder.getFolder(folder, comparator, fillRefs));
				}
			}

			if (inFolder.recursive && folderListParams.tree) {
				for (Folder folder : restFolders) {
					Node node = t.getObject(Node.class, folder.getNodeId());
					node = node.getMaster();
					// set the node id
					t.setChannelId(folder.getNodeId());
					try (WastebinFilter filter = WastebinFilter.get(includeWastebin, node)) {
						recursiveAttachSubfolders(folder, recursiveIds, folderListParams.inherited, filterParams.search, editableParams, new FolderComparator(sortParams.sortBy, sortParams.sortOrder), fillRefs, ObjectPermission.view);
					} finally {
						t.resetChannel();
					}
				}
			}

			// if ids where given to be opened, check whether they still exist
			if (inFolder.recursive && !ObjectTransformer.isEmpty(recursiveIds)) {
				response.setDeleted(getDeletedRecIds(t, recursiveIds));
			}
			response.setStagingStatus(StagingUtil.checkStagingStatus(folders, inFolder.stagingPackageName, o -> o.getGlobalId().toString()));
			response.setFolders(restFolders);

			return response;
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new LegacyFolderListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new LegacyFolderListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting folders for folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new LegacyFolderListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				// reset render type channel to null
				t.resetChannel();
			}
		}
	}

	@Override
	public FolderListResponse list(
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam FolderListParameterBean folderListParams,
			@BeanParam FilterParameterBean filterParams,
			@BeanParam SortParameterBean sortingParams,
			@BeanParam PagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		Transaction t = getTransaction();
		boolean channelIdSet = false;
		boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

		try {
			channelIdSet = setChannelToTransaction(folderListParams.nodeId);

			List<com.gentics.contentnode.object.Folder> folders = getFolders(t, inFolder.folderId, includeWastebin);

			// when folders shall be fetched recursively, do this now
			if (inFolder.recursive && !folderListParams.tree) {
				List<com.gentics.contentnode.object.Folder> subFolders = new ArrayList<>();

				for (com.gentics.contentnode.object.Folder folder : folders) {
					try (WastebinFilter filter = WastebinFilter.get(includeWastebin, folder.getOwningNode())) {
						subFolders.addAll(recursiveGetSubfolders(folder, folderListParams.recursiveIds, PermHandler.ObjectPermission.view));
					}
				}

				folders.addAll(subFolders);
			}

			// filter by optional search criteria
			filterBySearch(folders, folderListParams.inherited, filterParams.query, editableParams, wastebinParams.wastebinSearch);

			Map<String, String> fieldMap = new HashMap<>();
			fieldMap.put("publishDir", "pub_dir");

			// create a response with the folders
			ResolvableComparator<com.gentics.contentnode.object.Folder> comparator = ResolvableComparator.get(
				sortingParams,
				fieldMap,
				// From AbstractContentObject
				"id", "ttype", "ispage", "isfolder", "isfile", "isimage", "istag",
				// From AbstractFolder
				"ordner", "folder", "name", "description", "beschreibung", "mother", "node_id", "node", "parent",
				"pub_dir", "publishDir", "path", "creator", "ersteller", "editor", "bearbeiter", "creationtimestamp",
				"erstellungstimestamp", "editdate", "creationdate", "edittimestamp", "bearbeitungstimestamp",
				"ismaster", "inherited");
			Collection<Reference> fillRefs = new ArrayList<>();

			if (folderListParams.addPrivileges) {
				fillRefs.add(Reference.PRIVILEGES);
			}
			if (folderListParams.privilegeMap) {
				fillRefs.add(Reference.PRIVILEGEMAP);
			}

			FolderListResponse response = ListBuilder.from(
					folders,
					f -> {
						Folder restFolder;

						try (WastebinFilter filter = WastebinFilter.get(includeWastebin, f.getOwningNode())) {
							restFolder = ModelBuilder.getFolder(f, comparator, fillRefs.isEmpty() ? null : fillRefs);
						}

						if (inFolder.recursive && folderListParams.tree) {
							Node node = t.getObject(Node.class, restFolder.getNodeId()).getMaster();

							// set the node id
							t.setChannelId(restFolder.getNodeId());
							try (WastebinFilter filter = WastebinFilter.get(includeWastebin, node)) {
								recursiveAttachSubfolders(
									restFolder,
									folderListParams.recursiveIds,
									folderListParams.inherited,
									filterParams.query,
									editableParams,
									comparator,
									fillRefs,
									ObjectPermission.view);
							} finally {
								t.resetChannel();
							}
						}

						return restFolder;
					})
				.sort(comparator)
				.page(pagingParams)
				.to(new FolderListResponse());

			// if ids where given to be opened, check whether they still exist
			if (inFolder.recursive && !ObjectTransformer.isEmpty(folderListParams.recursiveIds)) {
				response.setDeleted(getDeletedRecIds(t, folderListParams.recursiveIds));
			}

			response.setStagingStatus(StagingUtil.checkStagingStatus(folders, inFolder.stagingPackageName, o -> o.getGlobalId().toString()));

			return response;
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FolderListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new FolderListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting folders for folder " + inFolder.folderId, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FolderListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				// reset render type channel to null
				t.resetChannel();
			}
		}
	}

	private List<String> getDeletedRecIds(Transaction t, List<String> recursiveIds) {
		List<String> deleted = new ArrayList<>();

		for (String recId : recursiveIds) {
			// check for existence
			String[] parts = recId.split("/");
			int recNodeId = -1;
			String recFolderId = null;

			if (parts.length == 1) {
				recFolderId = parts[0];
			} else if (parts.length == 2) {
				recNodeId = ObjectTransformer.getInt(parts[0], -1);
				recFolderId = parts[1];
			}

			com.gentics.contentnode.object.Folder recFolder = null;

			if (recNodeId > 0) {
				t.setChannelId(recNodeId);
				try {
					recFolder = getFolder(recFolderId, false);
				} catch (Exception e) {// ignored
				} finally {
					t.resetChannel();
				}
			} else {
				try {
					recFolder = getFolder(recFolderId, false);
				} catch (Exception e) {// ignored
				}
			}

			if (recFolder == null || ObjectTransformer.getInt(recFolder.getId(), -1) != ObjectTransformer.getInt(recFolderId, -1)) {
				deleted.add(recId);
			}
		}

		return deleted;
	}

	private List<com.gentics.contentnode.object.Folder> getFolders(Transaction t, String folderId, boolean includeWastebin) throws NodeException {
		List<com.gentics.contentnode.object.Folder> folders;

		if ("0".equalsIgnoreCase(folderId)) {
			// get the root folders
			PreparedStatement pst = null;
			ResultSet res = null;

			try {
				pst = t.prepareStatement("SELECT id FROM folder WHERE mother = ? AND deleted = 0");
				pst.setInt(1, 0);
				res = pst.executeQuery();

				Collection<Integer> ids = new Vector<Integer>();

				while (res.next()) {
					ids.add(res.getInt("id"));
				}

				folders = new ArrayList<>(t.getObjects(com.gentics.contentnode.object.Folder.class, ids));
			} catch (SQLException e) {
				throw new NodeException("Error while getting root folders", e);
			} finally {
				t.closeResultSet(res);
				t.closeStatement(pst);
			}
		} else {
			// get the list of subfolders
			try (WastebinFilter filter = getWastebinFilter(includeWastebin, folderId)) {
				// get the given folder
				folders = new ArrayList<>(getFolder(folderId, false).getChildFolders());
			}
		}

		// remove the folders with insufficient permission
		try (WastebinFilter filter = getWastebinFilter(includeWastebin, folderId)) {
			for (Iterator<com.gentics.contentnode.object.Folder> i = folders.iterator(); i.hasNext();) {
				com.gentics.contentnode.object.Folder folder = i.next();

				if (!PermHandler.ObjectPermission.view.checkObject(folder)) {
					i.remove();
				}
			}
		}

		return folders;
	}

	/**
	 * Filter the given list of folders by search criteria or permissions
	 * @param folders folders to filter
	 * @param inherited true for inherited folders, false for local folders, null for all
	 * @param search optional search string
	 * @param editableParams optional editable parameters
	 * @param wastebinSearch wastebin search
	 * @param perms optional permissions to filter
	 * @throws NodeException
	 */
	protected void filterBySearch(
			List<com.gentics.contentnode.object.Folder> folders,
			Boolean inherited, String search,
			EditableParameterBean editableParams,
			WastebinSearch wastebinSearch, PermHandler.ObjectPermission... perms) throws NodeException {
		if (inherited == null && ObjectTransformer.isEmpty(search) && ObjectTransformer.isEmpty(editableParams.editor) && ObjectTransformer.isEmpty(editableParams.creator)
				&& editableParams.editedBefore <= 0 && editableParams.editedSince <= 0 && editableParams.createdBefore <= 0 && editableParams.createdSince <= 0 && wastebinSearch == null && ObjectTransformer.isEmpty(perms)) {
			return;
		}

		List<SystemUser> editors = null;
		List<SystemUser> creators = null;

		if (!ObjectTransformer.isEmpty(search)) {
			search = search.toLowerCase();
		}
		if (!ObjectTransformer.isEmpty(editableParams.editor)) {
			editors = getMatchingSystemUsers(editableParams.editor, editableParams.editorIds);
		}
		if (!ObjectTransformer.isEmpty(editableParams.creator)) {
			creators = getMatchingSystemUsers(editableParams.creator, editableParams.creatorIds);
		}

		Wastebin wastebin = null;
		if (wastebinSearch != null) {
			switch (wastebinSearch) {
			case exclude:
				wastebin = Wastebin.EXCLUDE;
				break;
			case include:
				wastebin = Wastebin.INCLUDE;
				break;
			case only:
				wastebin = Wastebin.ONLY;
				break;
			default:
				break;
			}
			if (wastebin != null) {
				wastebin.filter(folders);
			}
		}

		for (Iterator<com.gentics.contentnode.object.Folder> i = folders.iterator(); i.hasNext();) {
			com.gentics.contentnode.object.Folder folder = i.next();

			if (!checkFolderPermissions(folder, perms)) {
				i.remove();
			} else if (inherited != null && folder.isInherited() != inherited.booleanValue()) {
				i.remove();
			} else if (editableParams.editedBefore > 0 && folder.getEDate().getTimestamp() > editableParams.editedBefore) {
				i.remove();
			} else if (editableParams.editedSince > 0 && folder.getEDate().getTimestamp() < editableParams.editedSince) {
				i.remove();
			} else if (editableParams.createdBefore > 0 && folder.getCDate().getTimestamp() > editableParams.createdBefore) {
				i.remove();
			} else if (editableParams.createdSince > 0 && folder.getCDate().getTimestamp() < editableParams.createdSince) {
				i.remove();
			} else if (editors != null && !editors.contains(folder.getEditor())) {
				i.remove();
			} else if (creators != null && !creators.contains(folder.getCreator())) {
				i.remove();
			} else if (!ObjectTransformer.isEmpty(search)) {
				// check if the search string matches
				// the folder id, name or description
				if (!ObjectTransformer.getString(folder.getId(), "").equals(search) && !folder.getName().toLowerCase().contains(search)
						&& !folder.getDescription().toLowerCase().contains(search)) {
					i.remove();
				}
			}
		}
	}

	/**
	 * Filter folders by given folderIds (which might also be nodeId/folderId)
	 * @param f folder to filter
	 * @param folderIds list of folder ids (or nodeId/folderId). If empty, all folders pass
	 * @return true if the folder is contained in the list (or the list is empty), false if not
	 */
	protected boolean filterFolder(Folder f, List<String> folderIds) {
		if (f == null) {
			// empty folder is filtered out
			return false;
		}
		if (ObjectTransformer.isEmpty(folderIds)) {
			// no folder ids given -> accept all folders
			return true;
		}
		String id = f.getId().toString();

		if (folderIds.contains(id)) {
			// found the id
			return true;
		}
		id = f.getNodeId().toString() + "/" + f.getId().toString();
		// also check for nodeId/folderId
		return folderIds.contains(id);
	}

	/**
	 * Recursively get all subfolders of the given folder with the given permission bits
	 * @param f folder
	 * @param recIds list of recursive ids
	 * @param perms permissions to check
	 * @return list of subfolders
	 * @throws NodeException
	 */
	protected List<com.gentics.contentnode.object.Folder> recursiveGetSubfolders(
			com.gentics.contentnode.object.Folder f, List<String> recIds, PermHandler.ObjectPermission... perms) throws NodeException {
		// if recIds are given, we check whether the folder is among the recIds
		if (!ObjectTransformer.isEmpty(recIds) && !recIds.contains(ObjectTransformer.getString(f.getId(), null))) {
			return Collections.emptyList();
		}

		// prepare the return value
		List<com.gentics.contentnode.object.Folder> subFolders = new Vector<>();

		// get the children
		List<com.gentics.contentnode.object.Folder> children = f.getChildFolders();

		// iterate over all children
		for (com.gentics.contentnode.object.Folder child : children) {
			// check the child permission
			if (checkFolderPermissions(child, perms)) {
				// add the child
				subFolders.add(child);
				// and the grandchildren
				subFolders.addAll(recursiveGetSubfolders(child, recIds, perms));
			}
		}

		return subFolders;
	}

	/**
	 * Recursively attach subfolders that match the given search criteria
	 * @param folder mother folder
	 * @param recIds optional list of ids for which subfolders shall be attached
	 * @param inherited true for inherited folders, false for local folders, null for all
	 * @param search optional search string
	 * @param editableParams optional editable params
	 * @param comparator comparator for sorting children
	 * @param fillRefs references to fill
	 * @param perms optional permissions to filter
	 * @throws NodeException
	 */
	protected void recursiveAttachSubfolders(Folder folder,
			List<String> recIds, Boolean inherited, String search,
			EditableParameterBean editableParams,
			Comparator<com.gentics.contentnode.object.Folder> comparator,
			Collection<Reference> fillRefs, PermHandler.ObjectPermission... perms) throws NodeException {
		if (!filterFolder(folder, recIds)) {
			return;
		}
		Transaction t = TransactionManager.getCurrentTransaction();

		com.gentics.contentnode.object.Folder nodeFolder = t.getObject(com.gentics.contentnode.object.Folder.class, folder.getId());

		// get the children
		List<com.gentics.contentnode.object.Folder> children = new Vector<>(nodeFolder.getChildFolders());

		// filter children
		filterBySearch(children, inherited, search, editableParams, null, perms);

		Collections.sort(children, comparator);

		List<Folder> restChildren = new Vector<>();

		for (com.gentics.contentnode.object.Folder child : children) {
			Folder restChild = ModelBuilder.getFolder(child, null, fillRefs);

			restChildren.add(restChild);
			recursiveAttachSubfolders(restChild, recIds, inherited, search, editableParams, comparator, fillRefs, perms);
		}

		folder.setSubfolders(restChildren);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#getTemplates(java.lang.String, java.lang.Integer, java.lang.String, boolean, boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, int, boolean, java.lang.Boolean)
	 */
	@GET
	@Path("/getTemplates/{folderId}")
	public TemplateListResponse getTemplates(
			@PathParam("folderId") String folderId,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam TemplateListParameterBean templateListParams,
			@BeanParam LegacyFilterParameterBean filterParams,
			@BeanParam LegacySortParameterBean sortingParams,
			@BeanParam LegacyPagingParameterBean pagingParams,
			@BeanParam EditableParameterBean editableParams,
			@BeanParam WastebinParameterBean wastebinParams) {
		Transaction t = getTransaction();
		boolean channelIdSet = false;

		try {
			// set the channel
			channelIdSet = setChannelToTransaction(templateListParams.nodeId);
			boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

			try (WastebinFilter filter = getWastebinFilter(includeWastebin, inFolder.folderId)) {
				// load the folder
				com.gentics.contentnode.object.Folder f = getFolder(folderId, false);

				// Check permissions for listing templates
				boolean permViewTemplates = !templateListParams.checkPermission || ObjectPermission.view.checkClass(f, Template.class, null);

				if (permViewTemplates) {
					// get templates from folder
					List<com.gentics.contentnode.rest.model.Template> restTemplates = getTemplatesFromFolder(f,
					TemplateSearch.create().setSearchString(filterParams.search)
						.setEditors(getMatchingSystemUsers(editableParams.editor, editableParams.editorIds))
						.setCreators(getMatchingSystemUsers(editableParams.creator, editableParams.creatorIds))
						.setEditedBefore(editableParams.editedBefore).setEditedSince(editableParams.editedSince)
						.setCreatedBefore(editableParams.createdBefore).setCreatedSince(editableParams.createdSince)
						.setInherited(templateListParams.inherited),
								inFolder.recursive,
								templateListParams.checkPermission);

					// optionally reduce the list of templates to have every template uniquely
					if (inFolder.recursive && templateListParams.reduce) {
						List<com.gentics.contentnode.rest.model.Template> reducedTemplates = new Vector<>();

						for (com.gentics.contentnode.rest.model.Template template : restTemplates) {
							if (!reducedTemplates.contains(template)) {
								reducedTemplates.add(template);
							}
						}
						restTemplates = reducedTemplates;
					}

					// sort the result
					if (!ObjectTransformer.isEmpty(sortingParams.sortBy) && !ObjectTransformer.isEmpty(sortingParams.sortOrder)) {
						Collections.sort(restTemplates, new TemplateComparator(sortingParams.sortBy, sortingParams.sortOrder));
					}

					// return the list in the response
					TemplateListResponse response = new TemplateListResponse();

					response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully loaded templates"));
					response.setNumItems(restTemplates.size());
					response.setHasMoreItems(pagingParams.maxItems > -1 && restTemplates.size() > pagingParams.skipCount + pagingParams.maxItems);
					reduceList(restTemplates, pagingParams.skipCount, pagingParams.maxItems);
					response.setTemplates(restTemplates);
					return response;
				} else {
					TemplateListResponse response = new TemplateListResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded templates"));

					response.setNumItems(0);
					response.setHasMoreItems(false);
					response.setTemplates(new Vector<com.gentics.contentnode.rest.model.Template>(0));
					return response;
				}
			}
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new TemplateListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new TemplateListResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting templates for folder " + folderId, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new TemplateListResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				// reset the channel
				t.resetChannel();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#getItems(java.lang.String, java.util.List, int, int, java.lang.Integer, boolean, java.lang.String, boolean, java.lang.String, boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, int)
	 */
	@Override
	@GET
	@Path("/getItems/{folderId}")
	public ItemListResponse getItems(
			@PathParam("folderId") String folderId,
			@QueryParam("type") List<ItemType> types,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("template") @DefaultValue("false") boolean template,
			@QueryParam("langvars") @DefaultValue("false") boolean languageVariants,
			@QueryParam("language") String language,
			@QueryParam("langfallback") @DefaultValue("true") boolean langFallback,
			@BeanParam InFolderParameterBean inFolder,
			@BeanParam LegacyFilterParameterBean filter,
			@BeanParam LegacySortParameterBean sorting,
			@BeanParam LegacyPagingParameterBean paging,
			@BeanParam PublishableParameterBean publishParams) {

		// accumulate the items here
		List<ContentNodeItem> items = new ArrayList<>();

		inFolder.setFolderId(folderId);

		if (!ObjectTransformer.isEmpty(types)) {
			// accumulate the items from the requested types, don't page or
			// sort, because this will be done later
			if (types.contains(ItemType.page)) {
				PageListParameterBean pageListParams = new PageListParameterBean()
					.setNodeId(nodeId)
					.setTemplate(template)
					.setLanguageVariants(languageVariants)
					.setLanguage(language)
					.setLangFallback(langFallback);
				LegacyPageListResponse resp = getPages(inFolder.folderId, inFolder, pageListParams, filter, sorting, paging, publishParams, new WastebinParameterBean());

				if (resp.getResponseInfo().getResponseCode() == ResponseCode.OK) {
					items.addAll(resp.getPages());
				} else {
					ItemListResponse itemListResponse = new ItemListResponse();

					itemListResponse.setMessages(resp.getMessages());
					itemListResponse.setResponseInfo(resp.getResponseInfo());
					return itemListResponse;
				}
			}
			if (types.contains(ItemType.file)) {
				FileListParameterBean fileListParams = new FileListParameterBean()
					.setNodeId(nodeId);
				LegacyFileListResponse resp = getFiles(inFolder.folderId, inFolder, fileListParams, filter, sorting, paging, publishParams, new WastebinParameterBean());

				if (resp.getResponseInfo().getResponseCode() == ResponseCode.OK) {
					items.addAll(resp.getFiles());
				} else {
					ItemListResponse itemListResponse = new ItemListResponse();

					itemListResponse.setMessages(resp.getMessages());
					itemListResponse.setResponseInfo(resp.getResponseInfo());
					return itemListResponse;
				}
			}
			if (types.contains(ItemType.image)) {
				FileListParameterBean fileListParams = new FileListParameterBean()
					.setNodeId(nodeId);
				LegacyFileListResponse resp = getImages(inFolder.folderId, inFolder, fileListParams, filter, sorting, paging, publishParams, new WastebinParameterBean());

				if (resp.getResponseInfo().getResponseCode() == ResponseCode.OK) {
					items.addAll(resp.getFiles());
				} else {
					ItemListResponse itemListResponse = new ItemListResponse();

					itemListResponse.setMessages(resp.getMessages());
					itemListResponse.setResponseInfo(resp.getResponseInfo());
					return itemListResponse;
				}
			}
		}

		// sort the list
		if (!ObjectTransformer.isEmpty(sorting.sortBy) && !ObjectTransformer.isEmpty(sorting.sortOrder) && !ObjectTransformer.isEmpty(items)) {
			Collections.sort(items, new ItemComparator(sorting.sortBy, sorting.sortOrder));
		}

		// create the response
		ItemListResponse response = new ItemListResponse();

		response.setNumItems(items.size());
		response.setHasMoreItems(paging.maxItems >= 0 && (items.size() > paging.skipCount + paging.maxItems));
		reduceList(items, paging.skipCount, paging.maxItems);
		response.setItems(items);

		response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Successfully loaded items"));
		return response;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#findPages(java.lang.Integer, java.lang.String, java.lang.Integer, java.lang.Integer, com.gentics.contentnode.rest.model.request.LinksType, boolean)
	 */
	@GET
	@Path("/findPages")
	public LegacyPageListResponse findPages(@QueryParam("folderId") @DefaultValue("0") Integer folderId,
			@QueryParam("query") String query,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("100") Integer maxItems,
			@QueryParam("links") @DefaultValue("backend") LinksType links,
			@QueryParam("recursive") @DefaultValue("true") boolean recursive) {
		Transaction t = getTransaction();
		List<Integer> folderIds = new Vector<>();
		PreparedStatement pst = null;
		ResultSet res = null;
		List<com.gentics.contentnode.rest.model.Page> foundPages = new Vector<>(maxItems);
		RenderUrlFactory urlFactory = null;

		// set a rendertype to the transaction
		NodePreferences preferences = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		RenderType renderType = RenderType.getDefaultRenderType(preferences, RenderType.EM_ALOHA_READONLY, t.getSessionId(), 0);

		t.setRenderType(renderType);

		if (skipCount < 0) {
			skipCount = 0;
		}

		// generate the url factory
		if (links == LinksType.backend) {
			urlFactory = new DynamicUrlFactory(t.getSessionId());
		} else if (links == LinksType.frontend) {
			// TODO linkways must be fetched from the configuration
			urlFactory = new StaticUrlFactory(RenderUrl.LINKWAY_PORTAL, RenderUrl.LINKWAY_PORTAL, null);
		}

		try {
			// first check the given folder itself
			if (folderId.intValue() > 0) {
				com.gentics.contentnode.object.Folder folder = getFolder(folderId);

				if (PermHandler.ObjectPermission.view.checkClass(folder, Page.class, null)) {
					folderIds.add(folderId);
				}
			}

			// get the folder id's
			if (recursive) {
				recursiveGetFolderIds(folderId, folderIds);
			}

			// get all pages within those folder id's that match the given
			// criteria
			if (folderIds.size() > 0) {
				StringBuffer sql = new StringBuffer(70 + 2 * folderIds.size());

				sql.append("SELECT id FROM page WHERE deleted = 0 AND name LIKE ? AND folder_id IN (");
				sql.append(StringUtils.repeat("?", folderIds.size(), ","));
				sql.append(") limit " + skipCount + ", " + maxItems);
				pst = t.prepareStatement(sql.toString());
				int paramCounter = 0;

				pst.setString(++paramCounter, "%" + ObjectTransformer.getString(query, "") + "%");
				for (Iterator<Integer> i = folderIds.iterator(); i.hasNext();) {
					pst.setInt(++paramCounter, i.next());
				}

				res = pst.executeQuery();
				Collection<Integer> pageIds = new Vector<>();

				while (res.next()) {
					pageIds.add(res.getInt("id"));
				}

				t.closeStatement(pst);
				t.closeResultSet(res);

				// now get all the pages
				List<Page> pages = t.getObjects(Page.class, pageIds);

				for (Page page : pages) {
					com.gentics.contentnode.rest.model.Page restPage = ModelBuilder.getPage(page, (Collection<Reference>) null);

					// set url to page
					if (urlFactory != null) {
						renderType.push(page);
						try {
							restPage.setUrl(urlFactory.createRenderUrl(Page.class, page.getId()).toString());
						} finally {
							renderType.pop();
						}
					}
					foundPages.add(restPage);
				}
			}

			// return the found pages
			LegacyPageListResponse response = new LegacyPageListResponse();

			response.setPages(foundPages);

			return response;
		} catch (Exception e) {
			String message = "Error while finding pages, starting from folder {" + folderId + "} with name like {" + query + "}";

			throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build());
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#findFiles(java.lang.Integer, java.lang.String, java.lang.Integer, java.lang.Integer, com.gentics.contentnode.rest.model.request.LinksType, boolean)
	 */
	@GET
	@Path("/findFiles")
	public FoundFilesListResponse findFiles(@QueryParam("folderId") @DefaultValue("0") Integer folderId,
			@QueryParam("query") String query,
			@QueryParam("skipCount") @DefaultValue("0") Integer skipCount,
			@QueryParam("maxItems") @DefaultValue("100") Integer maxItems,
			@QueryParam("links") @DefaultValue("backend") LinksType links,
			@QueryParam("recursive") @DefaultValue("true") boolean recursive) {
		Transaction t = getTransaction();
		List<Integer> folderIds = new Vector<>();
		PreparedStatement pst = null;
		ResultSet res = null;
		List<com.gentics.contentnode.rest.model.File> foundFiles = new Vector<>(maxItems);
		RenderUrlFactory urlFactory = null;

		// set a rendertype to the transaction
		NodePreferences preferences = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
		RenderType renderType = RenderType.getDefaultRenderType(preferences, RenderType.EM_ALOHA_READONLY, t.getSessionId(), 0);

		t.setRenderType(renderType);

		if (skipCount < 0) {
			skipCount = 0;
		}

		// generate the url factory
		if (links == LinksType.backend) {
			urlFactory = new DynamicUrlFactory(t.getSessionId());
		} else if (links == LinksType.frontend) {
			// TODO linkways must be fetched from the configuration
			urlFactory = new StaticUrlFactory(RenderUrl.LINKWAY_PORTAL, RenderUrl.LINKWAY_PORTAL, null);
		}

		try {
			// first check the given folder itself
			if (folderId.intValue() > 0) {
				com.gentics.contentnode.object.Folder folder = getFolder(folderId);

				if (PermHandler.ObjectPermission.view.checkClass(folder, File.class, null)) {
					folderIds.add(folderId);
				}
			}

			// get the folder id's
			if (recursive) {
				recursiveGetFolderIds(folderId, folderIds);
			}

			// get all pages within those folder id's that match the given
			// criteria
			if (folderIds.size() > 0) {
				StringBuffer sql = new StringBuffer(70 + 2 * folderIds.size());

				sql.append("SELECT id FROM contentfile WHERE deleted = 0 AND name LIKE ? AND folder_id IN (");
				sql.append(StringUtils.repeat("?", folderIds.size(), ","));
				sql.append(") AND filetype NOT LIKE 'image%' limit " + skipCount + ", " + maxItems);
				pst = t.prepareStatement(sql.toString());
				int paramCounter = 0;

				pst.setString(++paramCounter, "%" + query + "%");
				for (Iterator<Integer> i = folderIds.iterator(); i.hasNext();) {
					pst.setInt(++paramCounter, i.next());
				}

				res = pst.executeQuery();
				Collection<Integer> fileIds = new Vector<>();

				while (res.next()) {
					fileIds.add(res.getInt("id"));
				}

				t.closeStatement(pst);
				t.closeResultSet(res);

				// now get all the files
				List<File> files = t.getObjects(File.class, fileIds);

				for (File file : files) {
					com.gentics.contentnode.rest.model.File restFile = ModelBuilder.getFile(file, Collections.emptyList());

					// set url to page
					if (urlFactory != null) {
						renderType.push(file);
						try {
							restFile.setUrl(urlFactory.createRenderUrl(File.class, file.getId()).toString());
						} finally {
							renderType.pop();
						}
					}
					foundFiles.add(restFile);
				}
			}

			// return the found pages
			FoundFilesListResponse response = new FoundFilesListResponse();

			response.setFiles(foundFiles);

			return response;
		} catch (Exception e) {
			String message = "Error while finding files, starting from folder {" + folderId + "} with name like {" + query + "}";

			throw new WebApplicationException(e, Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build());
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Recursive function to get all folder id's with sufficient permissions for
	 * the user to see pages in
	 * @param folderId starting folder id (may be 0)
	 * @param allIds list, which will be filled with all folder id's
	 * @throws TransactionException
	 * @throws SQLException
	 */
	protected void recursiveGetFolderIds(Integer folderId, List<Integer> allIds) throws TransactionException, SQLException {
		Transaction t = getTransaction();
		List<Integer> folderIds = new Vector<>();
		PreparedStatement pst = null;
		ResultSet res = null;

		// get all subfolders of the given folder
		try {
			pst = t.prepareStatement("SELECT id FROM folder WHERE mother = ?");
			pst.setInt(1, folderId.intValue());

			res = pst.executeQuery();
			while (res.next()) {
				folderIds.add(res.getInt("id"));
			}
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}

		// for all subfolders with sufficient permissions, add the subfolder and
		// do the recursion
		for (Iterator<Integer> i = folderIds.iterator(); i.hasNext();) {
			Integer subFolderId = i.next();

			// the folder-view permission has to be available for all ancestors of
			// a folder to be included in the search results.
			if (checkFolderPermission(subFolderId, PermHandler.PERM_VIEW)) {

				// a folder is only added if the page view permission is available.
				if (checkFolderPermission(subFolderId, PermHandler.PERM_PAGE_VIEW)) {
					// add the subfolder id to the list
					allIds.add(subFolderId);
				}

				// do the recursion
				recursiveGetFolderIds(subFolderId, allIds);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#save(java.lang.String, com.gentics.contentnode.rest.model.request.FolderSaveRequest)
	 */
	@POST
	@Path("/save/{id}")
	public GenericResponse save(@PathParam("id") String id, FolderSaveRequest request) {
		try (AutoCommit cmt = new AutoCommit()) {
			Transaction t = getTransaction();

			// check for sufficient provided data
			if ((GlobalId.isGlobalId(id) || ObjectTransformer.getInt(id, -1) <= 0) || request == null || request.getFolder() == null) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
			}
			// get the folder
			com.gentics.contentnode.object.Folder folder = null;

			folder = t.getObject(com.gentics.contentnode.object.Folder.class, id, true);
			if (folder == null) {
				// could not find the folder to save
				I18nString message = new CNI18nString("folder.notfound");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, "Folder with id " + id + " does not exist"));
			}

			// check the permissions
			if (!PermHandler.ObjectPermission.edit.checkObject(folder)) {
				I18nString message = new CNI18nString("folder.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.PERMISSION, "Insufficient permissions to save Folder " + id));
			}

			Folder restFolder = request.getFolder();

			// validate translations
			Message validationMessage = validate(folder.getOwningNode(), Arrays.asList(restFolder.getNameI18n(),
					restFolder.getDescriptionI18n(), restFolder.getPublishDirI18n()));
			if (validationMessage != null) {
				return new GenericResponse(validationMessage, new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while saving folder " + id + ": " + validationMessage.getMessage()));
			}

			// set the updated names and possibly check for duplicates
			Message message = updateName(folder, restFolder.getName(), restFolder.getNameI18n(), ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false));
			if (message != null) {
				return new GenericResponse(message, new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while saving folder " + id + ": " + message.getMessage(), "name"));
			}

			// set the description(s)
			if (restFolder.getDescription() != null) {
				folder.setDescription(restFolder.getDescription());
			}
			if (restFolder.getDescriptionI18n() != null) {
				folder.setDescriptionI18n(I18nMap.TRANSFORM2NODE.apply(restFolder.getDescriptionI18n()));
			}

			// set the updated publish directories and possibly check for duplicates
			Set<String> translations = new HashSet<>();
			translations.addAll(Optional.ofNullable(restFolder.getNameI18n()).map(Map::keySet).orElse(Collections.emptySet()));
			translations.addAll(Optional.ofNullable(restFolder.getDescriptionI18n()).map(Map::keySet).orElse(Collections.emptySet()));
			translations.addAll(Optional.ofNullable(restFolder.getPublishDirI18n()).map(Map::keySet).orElse(Collections.emptySet()));

			message = updatePublishDir(folder, restFolder.getPublishDir(), restFolder.getPublishDirI18n(),
					ObjectTransformer.getBoolean(request.getFailOnDuplicate(), false), translations);
			if (message != null) {
				return new GenericResponse(message, new ResponseInfo(ResponseCode.INVALIDDATA,
						"Error while saving folder " + id + ": " + message.getMessage(), "publishDir"));
			}

			Map<String, Tag> restTags = restFolder.getTags();

			if (restTags != null) {
				Map<String, ObjectTag> objectTags = folder.getObjectTags();

				for (Iterator<com.gentics.contentnode.rest.model.Tag> i = restTags.values().iterator(); i.hasNext();) {
					com.gentics.contentnode.rest.model.Tag restTag = i.next();
					com.gentics.contentnode.object.Tag tag = null;

					if (restTag.getName().startsWith("object.")) {
						String realName = restTag.getName().substring(7);
						ObjectTag oTag = objectTags.get(realName);

						if (oTag == null) {
							// did not find the tag, so create a new one
							oTag = t.createObject(ObjectTag.class);
							oTag.setName(realName);
							oTag.setConstructId(restTag.getConstructId());

							// and add it to the other tags
							objectTags.put(restTag.getName(), oTag);
						}

						tag = oTag;
					}

					// now set the data from the REST tag
					ModelBuilder.fillRest2Node(restTag, tag);
				}

				// Throw an error if the user doesn't have permission
				// to update all the object properties
				MiscUtils.checkObjectTagEditPermissions(restTags, objectTags, true);
			}

			// conflict test, when changing the root folder of a node with "pubDirSegments"
			if (folder.getNode().isPubDirSegment() && folder.isRoot()) {
				Node editableNode = t.getObject(folder.getNode(), true);
				editableNode.setFolder(folder);
				Node conflictingNode = editableNode.getConflictingNode();
				if (conflictingNode != null) {
					return new GenericResponse(createNodeConflictMessage(editableNode, conflictingNode, "publishDir"),
							new ResponseInfo(ResponseCode.INVALIDDATA, "folder saving failed", "publishDir"));
				}

				ContentRepository cr = folder.getNode().getContentRepository();
				if (cr != null) {
					Map<String, Set<Node>> conflictingNodes = cr.getConflictingNodes(editableNode);
					if (!conflictingNodes.isEmpty()) {
						GenericResponse response = new GenericResponse();
						for (Map.Entry<String, Set<Node>> entry : conflictingNodes.entrySet()) {
							List<String> nodeNames = new ArrayList<>();
							for (Node n : entry.getValue()) {
								nodeNames.add(I18NHelper.getName(n));
							}
							String joinedNames = String.join(",", nodeNames);
							response.addMessage(new Message(Type.CRITICAL, I18NHelper.get(entry.getKey(), joinedNames)));
						}
						response.setResponseInfo(new ResponseInfo(ResponseCode.INVALIDDATA, "folder saving failed"));
						return response;
					}
				}
			}

			// save the folder
			boolean folderChanged = folder.save();
			t.commit(false);

			boolean recursivePubDir = ObjectTransformer.getBoolean(request.getRecursive(), false) && !ObjectTransformer.isEmpty(request.getFolder().getPublishDir());
			boolean propagateTags = !ObjectTransformer.isEmpty(request.getTagsToSubfolders());
			GenericResponse jobResponse = null;

			if (recursivePubDir || propagateTags) {
				int foregroundTime = ObjectTransformer.getInt(
						request.getForegroundTime(),
						ObjectTransformer.getInt(
								t.getNodeConfig().getDefaultPreferences().getProperty("backgroundjob_foreground_time"), 5)) * 1000;
				final String newPublishDir = request.getFolder().getPublishDir();
				final List<String> tagNames = request.getTagsToSubfolders();
				final com.gentics.contentnode.object.Folder modifiedFolder = folder;

				// collect the subfolders
				Set<com.gentics.contentnode.object.Folder> subFolders = new HashSet<>();
				List<com.gentics.contentnode.object.Folder> rec = new ArrayList<>();
				try (ChannelTrx trx = new ChannelTrx(request.getNodeId())) {
					rec.addAll(folder.getChildFolders());
					while (!rec.isEmpty()) {
						com.gentics.contentnode.object.Folder current = rec.remove(0);
						if (!t.canView(current)) {
							continue;
						}
						if (!current.isInherited() && t.canEdit(current)) {
							subFolders.add(current);
						}
						rec.addAll(current.getChildFolders());
					}
				}

				// schedule the job
				if (!subFolders.isEmpty()) {
					jobResponse = Operator.execute(new CNI18nString("folder.save.subfolders.job").toString(), foregroundTime, new Callable<GenericResponse> () {
						@Override
						public GenericResponse call() throws Exception {
							Transaction t = TransactionManager.getCurrentTransaction();
							// get the tags from the modified folder
							List<ObjectTag>	tags = null;
							if (propagateTags) {
								tags = new ArrayList<>(modifiedFolder.getObjectTags().values());
								for (Iterator<ObjectTag> i = tags.iterator(); i.hasNext(); ) {
									ObjectTag tag = i.next();
									String name = tag.getName();
									String shortName = name.substring("object.".length());
									String tagId = ObjectTransformer.getString(tag.getId(), null);
									if (!tagNames.contains(name) && !tagNames.contains(shortName) && !tagNames.contains(tagId)) {
										i.remove();
									}
								}
							}

							try (InstantPublishingTrx ipTrx = new InstantPublishingTrx(false)) {
								for (com.gentics.contentnode.object.Folder toChange : subFolders) {
									try (AutoCommit autoTrx = new AutoCommit()) {
										toChange = t.getObject(com.gentics.contentnode.object.Folder.class, toChange.getId(), true);

										if (recursivePubDir) {
											toChange.setPublishDir(newPublishDir);
										}

										if (propagateTags && !tags.isEmpty()) {
											for (ObjectTag tag : tags) {
												String name = tag.getName();
												if (name.startsWith("object.")) {
													name = name.substring("object.".length());
												}

												ObjectTag editableTag = toChange.getObjectTag(name);
												if (tag.isIntag() && editableTag == null) {
													// Special treatment for intag object properties *sigh*
													ObjectTag containingTag = tag.getInTagObject();
													if (containingTag != null) {
														// Get the mother object property from the subfolder with
														// the same name
														String containingTagName = containingTag.getName();
														if (containingTagName.startsWith("object.")) {
															containingTagName = containingTagName.substring("object.".length());
														}
														ObjectTag newContainingTag = toChange.getObjectTag(containingTagName);

														if (newContainingTag != null) {
															// Create a new intag-tag
															editableTag = t.createObject(ObjectTag.class);
															editableTag.setInTagObject(newContainingTag);
															toChange.getObjectTags().put(name, editableTag);
														}
													}
												}

												if (editableTag != null) {
													editableTag.copyFrom(tag);
												}
											}
										}

										toChange.save();
										autoTrx.success();
									}
								}
							}

							CNI18nString message = new CNI18nString("folder.save.subfolders.success");
							message.addParameter(I18NHelper.getName(modifiedFolder));
							return new GenericResponse(new Message(Type.SUCCESS, message.toString()), new ResponseInfo(
									ResponseCode.OK, "Folder " + id + " was successfully saved."));
						}
					});
				}
			}

			cmt.success();

			if (jobResponse == null || ObjectTransformer.isEmpty(jobResponse.getMessages())) {
				message = null;
				if (folderChanged) {
					message = new Message(Type.SUCCESS, new CNI18nString("folder.save.success").toString());
				}

				return new GenericResponse(message, new ResponseInfo(ResponseCode.OK, "Folder " + id + " was successfully saved."));
			} else {
				return jobResponse;
			}
		} catch (ReadOnlyException e) {
			I18nString message = new CNI18nString("folder.nopermission");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.PERMISSION, "Insufficient permission to save the folder " + id));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (ObjectModificationException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()),
					new ResponseInfo(ResponseCode.INVALIDDATA, e.getMessage(), e.getProperty()));
		} catch (NodeException e) {
			logger.error("Error while saving folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while saving folder. See server logs for details"));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#delete(java.lang.String)
	 */
	@POST
	@Path("/delete/{id}")
	public GenericResponse delete(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId, @QueryParam("noSync") Boolean noCrSync) {
		boolean syncCr = Optional.ofNullable(noCrSync).map(BooleanUtils::negate).orElse(true);
		try (InstantPublishingTrx ip = new InstantPublishingTrx(syncCr)) {
			// set the channel ID if given
			boolean isChannelIdset = setChannelToTransaction(nodeId);

			// get the folder (this will check for existence and delete permission)
			com.gentics.contentnode.object.Folder folder = getFolder(id, ObjectPermission.delete);

			Node node = folder.getChannel();

			int nodeIdOfObject = -1;

			if (node != null) {
				nodeIdOfObject = ObjectTransformer.getInteger(node.getId(), -1);
			}

			if (nodeId == null) {
				nodeId = 0;
			}

			if (isChannelIdset && folder.isInherited()) {
				throw new NodeException("Can't delete an inherated folder, the folder has to be deleted in the master node.");
			}

			if (nodeId > 0 && nodeIdOfObject > 0 && nodeIdOfObject != nodeId) {
				throw new EntityNotFoundException("The specified folder exists, but is not part of the node you specified.");
			}

			int channelSetId = ObjectTransformer.getInteger(folder.getChannelSetId(), 0);

			if (channelSetId > 0 && !folder.isMaster()) {
				throw new NodeException("Deletion of localized folders is currently not implemented, you maybe want to unlocalize it instead.");
			}

			final int folderId = folder.getId();
			return Operator.executeLocked(new CNI18nString("folder.delete.job").toString(), 0, Operator.lock(LockType.channelSet, channelSetId),
					new Callable<GenericResponse>() {
						@Override
						public GenericResponse call() throws Exception {
							com.gentics.contentnode.object.Folder folder = getFolder(String.valueOf(folderId), false);

							// now delete the folder
							folder.delete();

							I18nString message = new CNI18nString("folder.delete.success");
							return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
						}
					});
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (EntityNotFoundException e) {
			return new GenericResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while deleting folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		}
	}

	@Override
	@POST
	@Path("/wastebin/delete/{id}")
	public GenericResponse deleteFromWastebin(final @PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		return deleteFromWastebin(new IdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/delete")
	public GenericResponse deleteFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		List<String> ids = request.getIds();
		if (ObjectTransformer.isEmpty(ids)) {
			I18nString message = new CNI18nString("rest.general.insufficientdata");
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
		}

		I18nString description = null;
		if (ids.size() == 1) {
			description = new CNI18nString("folder.delete.wastebin");
			description.setParameter("0", ids.iterator().next());
		} else {
			description = new CNI18nString("folders.delete.wastebin");
			description.setParameter("0", ids.size());
		}
		return Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
			@Override
			public GenericResponse call() throws Exception {
				try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
					Set<com.gentics.contentnode.object.Folder> folders = new HashSet<>();
					for (String id : ids) {
						com.gentics.contentnode.object.Folder folder = getFolder(id, ObjectPermission.view, ObjectPermission.wastebin);

						if (!folder.isDeleted()) {
							I18nString message = new CNI18nString("folder.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						}

						folders.add(folder);
					}

					// reduce folders (remove subfolders of parents)
					folders = new HashSet<>(FolderFactory.reduce(folders, ReductionType.PARENT));

					String folderPaths = I18NHelper.getPaths(folders, 5);
					for (com.gentics.contentnode.object.Folder folder : folders) {
						folder.delete(true);
					}

					trx.success();
					// generate the response
					I18nString message = new CNI18nString(folders.size() == 1
						? "folder.delete.wastebin.success"
						: "folders.delete.wastebin.success");
					message.setParameter("0", folderPaths);
					return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			}
		});
	}

	@Override
	@POST
	@Path("/wastebin/restore/{id}")
	public GenericResponse restoreFromWastebin(@PathParam("id") String id, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		return restoreFromWastebin(new IdSetRequest(id), waitMs);
	}

	@Override
	@POST
	@Path("/wastebin/restore")
	public GenericResponse restoreFromWastebin(IdSetRequest request, @QueryParam("wait") @DefaultValue("0") long waitMs) {
		List<String> ids = request.getIds();
		if (ObjectTransformer.isEmpty(ids)) {
			I18nString message = new CNI18nString("rest.general.insufficientdata");
			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
		}

		I18nString description = null;
		if (ids.size() == 1) {
			description = new CNI18nString("folder.restore.wastebin");
			description.setParameter("0", ids.iterator().next());
		} else {
			description = new CNI18nString("folders.restore.wastebin");
			description.setParameter("0", ids.size());
		}
		return Operator.execute(description.toString(), waitMs, new Callable<GenericResponse>() {
			@Override
			public GenericResponse call() throws Exception {
				try (WastebinFilter filter = Wastebin.INCLUDE.set(); AutoCommit trx = new AutoCommit();) {
					Set<com.gentics.contentnode.object.Folder> folders = new HashSet<>();
					for (String id : ids) {
						com.gentics.contentnode.object.Folder folder = getFolder(id, ObjectPermission.view, ObjectPermission.wastebin);

						if (!folder.isDeleted()) {
							I18nString message = new CNI18nString("folder.notfound");
							message.setParameter("0", id.toString());
							throw new EntityNotFoundException(message.toString());
						}

						checkImplicitRestorePermissions(folder);
						folders.add(folder);
					}

					// reduce folders (remove parents of subfolders)
					folders = new HashSet<>(FolderFactory.reduce(folders, ReductionType.CHILD));

					String folderPaths = I18NHelper.getPaths(folders, 5);
					for (com.gentics.contentnode.object.Folder folder : folders) {
						folder.restore();
					}

					trx.success();
					// generate the response
					I18nString message = new CNI18nString(folders.size() == 1 ? "folder.restore.wastebin.success" : "folders.restore.wastebin.success");
					message.setParameter("0", folderPaths);
					return new GenericResponse(new Message(Type.INFO, message.toString()), new ResponseInfo(ResponseCode.OK, message.toString()));
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#getPrivileges(java.lang.Integer)
	 */
	@GET
	@Path("/privileges/{id}")
	public PrivilegesResponse getPrivileges(@PathParam("id") Integer id) {
		try {
			Transaction t = getTransaction();

			// get the folder (this will check for existance and permission)
			getFolder(id);

			PermHandler permHandler = t.getPermHandler();
			List<Privilege> privileges = ModelBuilder.getFolderPrivileges(id, permHandler);

			return new PrivilegesResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully loaded privileges on folder " + id), privileges);
		} catch (EntityNotFoundException e) {
			return new PrivilegesResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new PrivilegesResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting privileges for folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new PrivilegesResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#getObjectCounts(java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@GET
	@Path("/count/{id}")
	public FolderObjectCountResponse getObjectCounts(@PathParam("id") Integer id, @QueryParam("nodeId") Integer nodeId,
			@QueryParam("language") String language, @QueryParam("inherited") Boolean inherited,
			@BeanParam InFolderParameterBean inFolder, @BeanParam WastebinParameterBean wastebinParams) {
		Transaction t = getTransaction();
		boolean channelIdSet = false;
		boolean includeWastebin = Arrays.asList(WastebinSearch.include, WastebinSearch.only).contains(wastebinParams.wastebinSearch);

		try {
			String folderId = id.toString();

			channelIdSet = setChannelToTransaction(nodeId);
			inFolder.setFolderId(folderId);

			try (WastebinFilter filter = getWastebinFilter(includeWastebin, folderId)) {
				// get the folder (this will check for existance and permission)
				getFolder(folderId, ObjectPermission.view);

				FolderObjectCountResponse response = new FolderObjectCountResponse(null,
						new ResponseInfo(ResponseCode.OK, "Successfully counted objects in folder " + id));

				LegacyPageListResponse pageList = getPages(
					folderId,
					inFolder,
					new PageListParameterBean().setNodeId(nodeId).setLanguage(language).setInherited(inherited),
					new LegacyFilterParameterBean(),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new PublishableParameterBean(),
					wastebinParams);
				response.setPages(pageList.getNumItems());

				LegacyFileListResponse fileList = getFiles(
					folderId,
					inFolder,
					new FileListParameterBean().setNodeId(nodeId).setInherited(inherited),
					new LegacyFilterParameterBean(),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					wastebinParams);
				response.setFiles(fileList.getNumItems());

				LegacyFileListResponse imageList = getImages(
					folderId,
					inFolder,
					new FileListParameterBean().setNodeId(nodeId).setInherited(inherited),
					new LegacyFilterParameterBean(),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					wastebinParams);
				response.setImages(imageList.getNumItems());

				if (wastebinParams.wastebinSearch != WastebinSearch.only) {
					TemplateListResponse templateList = getTemplates(
							folderId,
							inFolder,
							new TemplateListParameterBean().setNodeId(nodeId).setInherited(inherited),
							new LegacyFilterParameterBean(),
							new LegacySortParameterBean(),
							new LegacyPagingParameterBean(),
							new EditableParameterBean(),
							wastebinParams);
					response.setTemplates(templateList.getNumItems());
				}

				LegacyFolderListResponse folderList = getFolders(
					folderId,
					new ArrayList<>(),
					false,
					inFolder,
					new FolderListParameterBean().setNodeId(nodeId).setInherited(inherited),
					new LegacyFilterParameterBean(),
					new LegacySortParameterBean(),
					new LegacyPagingParameterBean(),
					new EditableParameterBean(),
					wastebinParams);

				response.setFolders(folderList.getNumItems());

				return response;
			}
		} catch (EntityNotFoundException e) {
			return new FolderObjectCountResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.NOTFOUND, e.getMessage()));
		} catch (InsufficientPrivilegesException e) {
			InsufficientPrivilegesMapper.log(e);
			return new FolderObjectCountResponse(new Message(Type.CRITICAL, e.getLocalizedMessage()), new ResponseInfo(ResponseCode.PERMISSION, e.getMessage()));
		} catch (NodeException e) {
			logger.error("Error while getting count for folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new FolderObjectCountResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.FAILURE, e.getLocalizedMessage()));
		} finally {
			if (channelIdSet) {
				// render type channel wieder auf null setzen
				t.resetChannel();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.FolderResource#setStartpage(java.lang.String, com.gentics.contentnode.rest.model.request.StartpageRequest)
	 */
	@POST
	@Path("/startpage/{id}")
	public GenericResponse setStartpage(@PathParam("id") String id, StartpageRequest request) {
		Transaction t = getTransaction();

		try {
			// check for sufficient provided data
			if ((GlobalId.isGlobalId(id) || ObjectTransformer.getInt(id, -1) <= 0) || request == null || request.getPageId() <= 0) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
			}
			// get the folder
			com.gentics.contentnode.object.Folder folder = null;
			folder = t.getObject(com.gentics.contentnode.object.Folder.class, id, true);
			if (folder == null) {
				// could not find the folder to save
				I18nString message = new CNI18nString("folder.notfound");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, "Folder with ID " + id + " does not exist"));
			}

			// check the permissions
			if (!PermHandler.ObjectPermission.edit.checkObject(folder)) {
				I18nString message = new CNI18nString("folder.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.PERMISSION, "Insufficient permissions to edit Folder " + id));
			}

			// get the requested startpage
			Page startPage = t.getObject(Page.class, request.getPageId());

			if (startPage == null) {
				I18nString message = new CNI18nString("rest.general.insufficientdata");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.INVALIDDATA, "Insufficient data provided."));
			}

			boolean startPageSet = false;
			String startPagePropName = getStartpageObjectPropertyName(folder);

			if (!ObjectTransformer.isEmpty(startPagePropName)) {
				ObjectTag startPageObjectTag = folder.getObjectTag(startPagePropName);

				if (startPageObjectTag != null) {
					// get first part of type PageURLPartType
					ValueList values = startPageObjectTag.getValues();
					for (Value value : values) {
						PartType partType = value.getPartType();

						if (partType instanceof PageURLPartType) {
							((PageURLPartType) partType).setTargetPage(startPage);
							startPageSet = true;
							startPageObjectTag.setEnabled(true);
							// produce the same logcmd as in the tag_fill.php
							// and version.cmd.php when setting the startpage
							// via object properties of a folder
							Integer objtagid = 0;
							if (value.getContainer() != null && value.getContainer().getId() != null) {
								try {
									objtagid = value.getContainer().getId();
								} catch (ClassCastException ex) {
								}
							}

							ActionLogger.logCmd(ActionLogger.EDIT, com.gentics.contentnode.object.Tag.TYPE_OBJECTTAG, objtagid,
									0, "val_text:  ref: " + startPage.getId());
							ActionLogger.logCmd(ActionLogger.EDIT, com.gentics.contentnode.object.Tag.TYPE_OBJECTTAG, objtagid,
									com.gentics.contentnode.object.Folder.TYPE_FOLDER, "" + folder.getId());
							break;
						}
					}
				}
			}

			if (!startPageSet) {
				I18nString message = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while setting startpage for folder. Object property for startpage not configured properly"));
			} else {
				folder.save();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully set startpage"));
			}
		} catch (ReadOnlyException e) {
			I18nString message = new CNI18nString("folder.nopermission");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.PERMISSION, "Insufficient permission to edit the folder " + id));
		} catch (NodeException e) {
			logger.error("Error while setting startpage of folder " + id, e);
			I18nString message = new CNI18nString("rest.general.error");

			return new GenericResponse(new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.FAILURE, "Error while setting startpage for folder. See server logs for details"));
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FolderResource#move(java.lang.String, com.gentics.contentnode.rest.model.request.FolderMoveRequest)
	 */
	@POST
	@Path("/move/{id}")
	public GenericResponse move(@PathParam("id") String id, FolderMoveRequest request) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Folder folder = getFolder(id, false);
		MoveJob moveJob = new MoveJob(com.gentics.contentnode.object.Folder.class, Integer.toString(folder.getId()),
				request.getFolderId(), request.getNodeId());
		return moveJob.execute(request.getForegroundTime(), TimeUnit.SECONDS);
	}

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.resource.FolderResource#move(com.gentics.contentnode.rest.model.request.MultiFolderMoveRequest)
	 */
	@POST
	@Path("/move")
	public GenericResponse move(MultiFolderMoveRequest request) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Set<String> ids = request.getIds();
		Set<Integer> localIds = new HashSet<>();
		for (String id : ids) {
			com.gentics.contentnode.object.Folder folder = getFolder(id, false);
			localIds.add(folder.getId());
		}
		MoveJob moveJob = new MoveJob(com.gentics.contentnode.object.Folder.class, localIds, request.getFolderId(),
				request.getNodeId());
		return moveJob.execute(request.getForegroundTime(), TimeUnit.SECONDS);
	}


	@Override
	@POST
	@Path("/sanitize/publishDir")
	public FolderPublishDirSanitizeResponse sanitizePubdir(FolderPublishDirSanitizeRequest request) throws NodeException {
		Node node = MiscUtils.load(Node.class, Integer.toString(request.getNodeId()));
		String sanitized = FolderFactory.cleanPubDir(request.getPublishDir(), node.isPubDirSegment(), true);
		return new FolderPublishDirSanitizeResponse(null, ResponseInfo.ok("")).setPublishDir(sanitized);
	}

	/**
	 * Get the templates from the folder (and possibly subfolders)
	 * @param f folder
	 * @param search template search (may be null)
	 * @param recursive true if the templates shall be fetched from the subfolders as well
	 * @param checkPermission true if permissions shall be checked, false if not
	 * @return list of templates
	 * @throws NodeException
	 */
	protected List<com.gentics.contentnode.rest.model.Template> getTemplatesFromFolder(
			com.gentics.contentnode.object.Folder f, TemplateSearch search, boolean recursive, boolean checkPermission) throws NodeException {
		List<Template> templates = new Vector<>(f.getTemplates(search));
		String folderPath = ModelBuilder.getFolderPath(f);

		// transform the list of templates into their REST representatives
		List<com.gentics.contentnode.rest.model.Template> restTemplates = new Vector<>(templates.size());

		for (Template template : templates) {
			com.gentics.contentnode.rest.model.Template restTemplate = ModelBuilder.getTemplate(template, null);

			restTemplate.setFolderId(ObjectTransformer.getInteger(f.getId(), -1));
			restTemplate.setPath(folderPath);
			restTemplates.add(restTemplate);
		}

		// when we should search recursively, we do this now
		if (recursive) {
			List<com.gentics.contentnode.object.Folder> childFolders = f.getChildFolders();

			for (com.gentics.contentnode.object.Folder folder : childFolders) {
				// don't forget to check the permission
				if (!checkPermission || PermHandler.ObjectPermission.view.checkClass(folder, Template.class, null)) {
					restTemplates.addAll(getTemplatesFromFolder(folder, search, recursive, checkPermission));
				}
			}
		}

		return restTemplates;
	}

	/**
	 * Get the folder with given id, check whether the folder exists (if not,
	 * throw a EntityNotFoundException). Also check the permission to view the folder and
	 * throw a InsufficientPrivilegesException, in case of insufficient permission.
	 * @param id local id of the folder
	 * @return folder
	 * @throws NodeException when loading the folder fails due to underlying
	 *         error
	 * @throws EntityNotFoundException when the folder was not found
	 * @throws InsufficientPrivilegesException when the user has no permission
	 *         on the folder
	 */
	@Deprecated
	protected com.gentics.contentnode.object.Folder getFolder(Integer id) throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {
		return getFolder(Integer.toString(id), false);
	}

	/**
	 * Get the folder with given id, check whether the folder exists (if not,
	 * throw a EntityNotFoundException). Also check the permission to view the folder and
	 * throw a InsufficientPrivilegesException, in case of insufficient permission.
	 * @param id id of the folder. This can either be a local or globalid
	 * @param update true if the folder shall be fetched for update
	 *
	 * @return folder
	 * @throws NodeException when loading the folder fails due to underlying
	 *         error
	 * @throws EntityNotFoundException when the folder was not found
	 * @throws InsufficientPrivilegesException when the user has no permission
	 *         on the folder
	 */
	protected com.gentics.contentnode.object.Folder getFolder(String id, boolean update) throws EntityNotFoundException, InsufficientPrivilegesException, NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();
		com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, id, update);

		if (folder == null) {
			I18nString message = new CNI18nString("folder.notfound");

			throw new EntityNotFoundException(message.toString());
		}

		// check permission to view the folder
		if (!PermHandler.ObjectPermission.view.checkObject(folder)) {
			I18nString message = new CNI18nString("folder.nopermission");

			throw new InsufficientPrivilegesException(message.toString(), folder, PermType.read);
		}

		return folder;
	}

	/**
	 * Get the folder with given id, check whether the folder exists. Check for
	 * given permissions for the current user.
	 *
	 * @param id
	 *            id of the folder
	 * @param perms
	 *            permissions to check
	 * @return folder
	 * @throws NodeException
	 *             when loading the folder fails due to underlying error
	 * @throws EntityNotFoundException
	 *             when the folder was not found
	 * @throws InsufficientPrivilegesException
	 *             when the user doesn't have a requested permission on the
	 *             folder
	 */
	public static com.gentics.contentnode.object.Folder getFolder(String id, PermHandler.ObjectPermission... perms) throws EntityNotFoundException,
			InsufficientPrivilegesException, NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, id);
		if (folder == null) {
			I18nString message = new CNI18nString("folder.notfound");
			message.setParameter("0", id.toString());
			throw new EntityNotFoundException(message.toString());
		}

		// check permission bits
		for (PermHandler.ObjectPermission p : perms) {
			if (!p.checkObject(folder)) {
				I18nString message = new CNI18nString("folder.nopermission");
				message.setParameter("0", id.toString());
				throw new InsufficientPrivilegesException(message.toString(), folder, p.getPermType());
			}

			// delete permissions for master folders must be checked for all channels containing localized copies
			if (folder.isMaster() && p == ObjectPermission.delete) {
				for (int channelSetNodeId : folder.getChannelSet().keySet()) {
					if (channelSetNodeId == 0) {
						continue;
					}
					Node channel = t.getObject(Node.class, channelSetNodeId);
					if (!ObjectPermission.delete.checkObject(folder, channel)) {
						I18nString message = new CNI18nString("folder.nopermission");
						message.setParameter("0", id.toString());
						throw new InsufficientPrivilegesException(message.toString(), folder, PermType.delete);
					}
				}
			}
		}

		return folder;
	}

	/**
	 * Get the name of the startpage object property for the given folder (without the object. prefix) or null if none defined
	 * @param folder folder
	 * @return name of the startpage object property or null
	 * @throws NodeException
	 */
	protected String getStartpageObjectPropertyName(com.gentics.contentnode.object.Folder folder) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		Object nodeId = folder.getNode().getId();

		// default startpage property name
		String startPagePropName = prefs.getProperty("folder_startpage_objprop_name");
		// get "per node" configuration
		Map<?, ?> startPagePerNode = prefs.getPropertyMap("folder_startpage_objprop_per_node");

		// check whether the property is configured for the node, and take the default value if not
		if (startPagePerNode != null && startPagePerNode.containsKey(nodeId)) {
			startPagePropName = ObjectTransformer.getString(startPagePerNode.get(nodeId), startPagePropName);
		}

		// when a property is defined, we try to set the start page
		if (!StringUtils.isEmpty(startPagePropName)) {
			// strip unwanted "object."
			if (startPagePropName.startsWith("object.")) {
				startPagePropName = startPagePropName.substring(7);
			}
		}

		return startPagePropName;
	}

	/**
	 * Get the wastebin filter depending on whether including the wastebin was requested, the feature is activated and the user has permission for the wastebin of the given folder
	 * @param includeWastebin true if including the wastebin was requested
	 * @param folderId folder ID
	 * @return wastebin filter
	 * @throws NodeException
	 */
	public WastebinFilter getWastebinFilter(boolean includeWastebin, String folderId) throws NodeException {
		if (!includeWastebin) {
			return Wastebin.EXCLUDE.set();
		} else if ("0".equals(folderId)) {
			return Wastebin.EXCLUDE.set();
		} else {
			com.gentics.contentnode.object.Folder folder = null;
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				folder = getFolder(folderId, false);
			}
			return WastebinFilter.get(includeWastebin, folder.getOwningNode());
		}
	}

	/**
	 * Update the name(s) of the given folder and do a uniqueness check, if required. Return an error message,
	 * if the uniqueness check fails or null, if everything is ok.
	 *
	 * @param folder folder to update
	 * @param name optional name to update
	 * @param nameI18n optional translated names to update
	 * @param uniquenessCheck true to make uniqueness checks, if something was modified
	 * @return error message or null, if everything is ok
	 * @throws NodeException
	 */
	protected Message updateName(com.gentics.contentnode.object.Folder folder, String name,
			Map<String, String> nameI18n, boolean uniquenessCheck) throws NodeException {
		boolean checkDuplicateName = false;
		if (!ObjectTransformer.isEmpty(name)) {
			if (uniquenessCheck && !StringUtils.isEqual(folder.getName(), name)) {
				checkDuplicateName = true;
			}
			folder.setName(name);
		}
		// set the updated translated names
		if (nameI18n != null) {
			if (uniquenessCheck && !Objects.deepEquals(I18nMap.TRANSFORM2REST.apply(folder.getNameI18n()), nameI18n)) {
				checkDuplicateName = true;
			}
			folder.setNameI18n(I18nMap.TRANSFORM2NODE.apply(nameI18n));
		}
		// check for duplicate names
		if (checkDuplicateName) {
			Pair<String, NodeObject> conflictingObject = FolderFactory.isNameAvailable(folder);
			if (conflictingObject != null) {
				CNI18nString message = new CNI18nString("error.foldername.exists");
				message.addParameter(conflictingObject.getLeft());
				message.addParameter(I18NHelper.getPath(conflictingObject.getRight()));
				return new Message(Message.Type.CRITICAL, message.toString());
			}
		}
		return null;
	}

	/**
	 * Update the publish directories of the given folder and do a uniqueness check,
	 * if required. Return an error message, if the uniqueness checks fail or null,
	 * if everything is ok.
	 *
	 * @param folder folder to update
	 * @param pubDir optional publish directory to update
	 * @param pubDirI18n optional translated publish directories to update
	 * @param uniquenessCheck true to make uniqueness checks, if something was modified
	 * @param requiredLanguages set of language codes, for which translations exist
	 * @return error message or null, if everything is ok
	 * @throws NodeException
	 */
	protected Message updatePublishDir(com.gentics.contentnode.object.Folder folder, String pubDir,
			Map<String, String> pubDirI18n, boolean uniquenessCheck, Set<String> requiredLanguages) throws NodeException {
		boolean checkDuplicatePubDir = false;
		Node node = folder.getNode();
		if (pubDir != null) {
			if (uniquenessCheck && !StringUtils.isEqual(folder.getPublishDir(), pubDir)) {
				checkDuplicatePubDir = true;
			}
			folder.setPublishDir(pubDir);

			// When pub dir segments is active, the segment is only allowed to
			// be empty for the root folder of the node.
			boolean pubDirSegmentRequired = node.isPubDirSegment() && !folder.equals(node.getFolder());

			// setPublishDir will sanitize the name and remove slashes if pub
			// dir segments is active. When the publish dir is empty afterward,
			// the request contained an empty string or a '/' for the pub dir
			// segment.
			if (pubDirSegmentRequired && StringUtils.isEmpty(folder.getPublishDir())) {
				return new Message(Message.Type.CRITICAL, new CNI18nString("error.pubdir.segment_missing").toString());
			}
		}

		if (!CollectionUtils.isEmpty(requiredLanguages) && MapUtils.isEmpty(pubDirI18n) && node.isPubDirSegment()) {
			pubDirI18n = new HashMap<>();
		}

		// set the updated translated publish directories
		if (pubDirI18n != null) {
			if (node.isPubDirSegment()) {
				// make sure that the map contains all required translations
				for (String lang : requiredLanguages) {
					pubDirI18n.computeIfAbsent(lang, k -> folder.getPublishDir());
				}
			}

			if (uniquenessCheck
					&& !Objects.deepEquals(I18nMap.TRANSFORM2REST.apply(folder.getPublishDirI18n()), pubDirI18n)) {
				checkDuplicatePubDir = true;
			}
			folder.setPublishDirI18n(I18nMap.TRANSFORM2NODE.apply(pubDirI18n));
		}
		if (checkDuplicatePubDir) {
			Pair<String, NodeObject> conflictingObject = FolderFactory.isPubDirAvailable(folder);
			if (conflictingObject != null) {
				CNI18nString message = new CNI18nString("error.pubdir.exists");
				message.addParameter(conflictingObject.getLeft());
				message.addParameter(I18NHelper.getPath(conflictingObject.getRight()));
				return new Message(Message.Type.CRITICAL, message.toString());
			}
		}
		return null;
	}

	/**
	 * Validate the given translation maps with the languages of the node
	 * @param node node
	 * @param i18nMapCollection collection of translation map
	 * @return error message, if a translation exists for a language, that is not assigned to the node, null if everything is ok
	 * @throws NodeException
	 */
	protected Message validate(Node node, Collection<Map<String, String>> i18nMapCollection) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ContentLanguage> languages = node.getMaster().getLanguages();
		Map<String, ContentLanguage> languageMap = languages.stream().collect(Collectors.toMap(ContentLanguage::getCode, Function.identity()));

		for (Map<String,String> i18nMap : i18nMapCollection) {
			if (ObjectTransformer.isEmpty(i18nMap)) {
				continue;
			}

			for (String code : i18nMap.keySet()) {
				if (!languageMap.containsKey(code)) {
					// first check whether the language exists
					ContentLanguage language = t.getObject(ContentLanguage.class, DBUtils.select("SELECT id FROM contentgroup WHERE code = ?", pst -> {
						pst.setString(1, code);
					}, DBUtils.firstInt("id")));

					if (language == null) {
						// language does not exist
						return new Message(Type.CRITICAL, I18NHelper.get("rest.language.notfound", code));
					} else {
						// language not assigned not node
						return new Message(Type.CRITICAL, I18NHelper.get("rest.language.notassigned", language.getName(), I18NHelper.getName(node)));
					}
				}
			}
		}

		return null;
	}
}
