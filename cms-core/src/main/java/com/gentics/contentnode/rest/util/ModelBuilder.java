/**
 *
 */
package com.gentics.contentnode.rest.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.PublishCacheTrx;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.messaging.Message;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.OverviewEntry;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.PublishWorkflow;
import com.gentics.contentnode.object.PublishWorkflowStep;
import com.gentics.contentnode.object.Regex;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.ValueList;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUrlFactory.LinkManagement;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.contentnode.rest.model.BreadcrumbItem;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.DeleteInfo;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.LanguagePrivileges;
import com.gentics.contentnode.rest.model.NodeFeature;
import com.gentics.contentnode.rest.model.NodeIdObjectId;
import com.gentics.contentnode.rest.model.Overview.ListType;
import com.gentics.contentnode.rest.model.Overview.OrderBy;
import com.gentics.contentnode.rest.model.Overview.OrderDirection;
import com.gentics.contentnode.rest.model.Overview.SelectType;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.PrivilegeMap;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.QueuedTimeManagement;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.RegexModel;
import com.gentics.contentnode.rest.model.SimplePage;
import com.gentics.contentnode.rest.model.TimeManagement;
import com.gentics.contentnode.rest.model.TranslationStatus;
import com.gentics.contentnode.rest.model.TranslationStatus.Latest;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.Workflow;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.PolicyGroupResponse;
import com.gentics.contentnode.rest.model.response.PolicyGroupResponse.GroupPolicyResponse;
import com.gentics.contentnode.rest.model.response.PolicyResponse;
import com.gentics.contentnode.rest.model.response.ValidationResultResponse;
import com.gentics.contentnode.rest.model.response.ValidationResultResponse.ValidationMessageResponse;
import com.gentics.contentnode.rest.model.response.admin.CustomTool;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.validation.map.Policy;
import com.gentics.contentnode.validation.map.PolicyGroup;
import com.gentics.contentnode.validation.util.ValidationUtils;
import com.gentics.contentnode.validation.validator.ValidationMessage;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

import io.reactivex.Flowable;

/**
 * Helper class for building the REST Model (ouf of Node objects)
 *
 * @author norbert
 */
public class ModelBuilder {

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(ModelBuilder.class);

	/**
	 * Transform the given File (Node Object) into the REST Model
	 *
	 * @param nodeFile
	 *            Node Object
	 * @param fillRefs references to be filled
	 * @return REST Model of the File
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.File getFile(File nodeFile, Collection<Reference> fillRefs) throws NodeException {

		com.gentics.contentnode.rest.model.File restFile = new com.gentics.contentnode.rest.model.File();

		fillFileData(nodeFile, restFile, fillRefs);

		return restFile;
	}

	/**
	 * Transform the given Image (Node Object) into the REST Model
	 *
	 * @param nodeImage
	 *            Node Object
	 * @param fillRefs references to be filled
	 * @return REST Model of the Image
	 * @throws NodeException
	 */
	public static Image getImage(ImageFile nodeImage, Collection<Reference> fillRefs) throws NodeException {
		return getImage(nodeImage, new Image(), fillRefs);
	}

	/**
	 * Inflate the image REST Model with its node object.
	 *
	 * @param nodeImage
	 *            Node Object
	 * @param restImage REST model to inflate
	 * @param fillRefs references to be filled
	 * @return REST Model of the Image
	 * @throws NodeException
	 */
	public static Image getImage(ImageFile nodeImage, Image restImage, Collection<Reference> fillRefs) throws NodeException {
		fillFileData(nodeImage, restImage, fillRefs);

		restImage.setDpiX(nodeImage.getDpiX());
		restImage.setDpiY(nodeImage.getDpiY());
		restImage.setSizeX(nodeImage.getSizeX());
		restImage.setSizeY(nodeImage.getSizeY());
		restImage.setFpX(nodeImage.getFpX());
		restImage.setFpY(nodeImage.getFpY());
		restImage.setGisResizable(nodeImage.isGisResizable());

		return restImage;
	}

	/**
	 * Render a static absolute URL including host for a specific object
	 *
	 * @param targetObjClass
	 *            The class of the target nodeobject to link to.
	 * @param object
	 *            A publishable node object (Page, File, ...)
	 * @param node node
	 * @return URL
	 * @throws NodeException
	 */
	protected static String renderLiveUrlForObject(Class<? extends NodeObject> targetObjClass, NodeObject object, Node node) throws NodeException {

		String url = "";

		NodePreferences preferences = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();

		// don't render the url if the feature is not activated
		if (!preferences.isFeature(Feature.LIVE_URLS) && !preferences.isFeature(Feature.LIVE_URLS_PER_NODE, node)) {
			return url;
		}

		// generate the url factory with a static host link
		StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_HOST, RenderUrl.LINK_HOST, null);

		// we don't want the linkway to be changed
		urlFactory.setAllowAutoDetection(false);
		// disable link management
		urlFactory.setLinkManagement(LinkManagement.OFF);

		Transaction t = TransactionManager.getCurrentTransaction();

		RenderType currentRenderType = t.getRenderType();
		RenderType renderType = RenderType.getDefaultRenderType(preferences, RenderType.EM_ALOHA_READONLY, t.getSessionId(), 0);

		t.setRenderType(renderType);

		renderType.push((StackResolvable) object);
		try {
			// Render the URL and set it as live URL
			RenderUrl renderUrl = urlFactory.createRenderUrl(targetObjClass, object.getId());

			url = renderUrl.toString();
		} finally {
			renderType.pop();
			t.setRenderType(currentRenderType);
		}

		return url;
	}

	/**
	 * Render the publish path of the object (without hostname)
	 * @param object object
	 * @return publish path
	 * @throws NodeException
	 */
	protected static String renderPublishPath(NodeObject object) throws NodeException {
		StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINK_REL, RenderUrl.LINK_REL, null);
		urlFactory.setAllowAutoDetection(false);
		urlFactory.setLinkManagement(LinkManagement.OFF);

		try (RenderTypeTrx rTrx = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY, object, false, false)) {
			return urlFactory.createRenderUrl(object.getObjectInfo().getObjectClass(), object.getId()).toString();
		}
	}

	/**
	 * Fill the file data into the REST Model
	 *
	 * @param nodeFile
	 *            Node Object
	 * @param restFile
	 *            REST Model
	 * @param fillRefs references to be filled
	 * @throws NodeException
	 */
	public static void fillFileData(File nodeFile,
			com.gentics.contentnode.rest.model.File restFile, Collection<Reference> fillRefs) throws NodeException {
		restFile.setId(nodeFile.getId());
		restFile.setGlobalId(nodeFile.getGlobalId().toString());
		restFile.setName(nodeFile.getName());
		restFile.setFileSize(nodeFile.getFilesize());
		restFile.setFileType(nodeFile.getFiletype());
		restFile.setDescription(nodeFile.getDescription());
		restFile.setFolderId(nodeFile.getFolder().getId());
		restFile.setFolderName(nodeFile.getFolder().getName());
		restFile.setEditor(getUser(nodeFile.getEditor()));
		restFile.setCreator(getUser(nodeFile.getCreator()));
		restFile.setCdate(nodeFile.getCDate().getIntTimestamp());
		restFile.setEdate(nodeFile.getEDate().getIntTimestamp());
		restFile.setPath(getFolderPath(nodeFile.getFolder()));
		restFile.setLiveUrl(renderLiveUrlForObject(File.class, nodeFile, nodeFile.getNode()));
		restFile.setPublishPath(renderPublishPath(nodeFile));

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			restFile.setNiceUrl(nodeFile.getNiceUrl());
			restFile.setAlternateUrls(new TreeSet<>(nodeFile.getAlternateUrls()));
		}

		restFile.setDeleted(getDeleteInfo(nodeFile));

		if (!nodeFile.isMaster()) {
			restFile.setMasterDeleted(getDeleteInfo(nodeFile.getMaster()));
		}

		restFile.setFolderDeleted(getDeleteInfo(nodeFile.getFolder().getMaster()));

		Node channel = nodeFile.getChannel();

		if (channel != null) {
			restFile.setChannelId(channel.getId());
		} else {
			restFile.setChannelId(new Integer(0));
			channel = getMaster(nodeFile.getFolder().getNode());
		}
		restFile.setInherited(nodeFile.isInherited());
		restFile.setInheritedFrom(channel.getFolder().getName());
		restFile.setInheritedFromId(channel.getId());
		Node masterNode = nodeFile.getMaster().getChannel();

		if (masterNode == null) {
			masterNode = getMaster(nodeFile.getFolder().getNode());
		}
		restFile.setMasterNode(masterNode.getFolder().getName());
		restFile.setMasterNodeId(masterNode.getId());

		restFile.setForceOnline(nodeFile.isForceOnline());
		Transaction t = TransactionManager.getCurrentTransaction();
		Node currentChannel = t.getChannel();

		restFile.setOnline(FileOnlineStatus.isOnline(nodeFile, currentChannel != null ? currentChannel : channel));
		restFile.setBroken(nodeFile.isBroken());
		restFile.setExcluded(nodeFile.isExcluded());
		restFile.setDisinheritDefault(nodeFile.isDisinheritDefault());
		restFile.setDisinherited(!nodeFile.getMaster().getDisinheritedChannels().isEmpty());

		if (fillRefs != null) {
			if (fillRefs.contains(Reference.TAGS)) {
				PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
				// add the objecttags
				Map<String, ObjectTag> objectTagMap = new HashMap<>(nodeFile.getObjectTags());

				// filter out restricted object tags
				new ObjectTagRestrictionFilter(nodeFile.getOwningNode()).filter(objectTagMap.values());

				Map<String, Integer> sortOrderMap = null;
				// the sortorder will only be added, if data necessary for editing shall be added to the REST Model
				if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
					sortOrderMap = getSortOrderMap(objectTagMap.values());
				}
				Map<String, com.gentics.contentnode.rest.model.Tag> restTags = new HashMap<>(objectTagMap.size());

				for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
					String tagName = i.next();
					ObjectTag objectTag = objectTagMap.get(tagName);

					// Transform the object tag, when object tag permission don't matter,
					// or the user has permission to view it
					if (!fillRefs.contains(Reference.OBJECT_TAGS_VISIBLE) || permHandler.canView(objectTag)) {
						com.gentics.contentnode.rest.model.Tag restTag = null;
						if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
							restTag = getObjectTag(objectTag, sortOrderMap.getOrDefault(objectTag.getName(), 0), true, true);
						} else {
							restTag = getTag(objectTag, false, true);
						}
						restTags.put(restTag.getName(), restTag);
					}
				}
				restFile.setTags(restTags);
			}
			if (fillRefs.contains(Reference.DISINHERITED_CHANNELS)) {
				Set<com.gentics.contentnode.rest.model.Node> restChannels = new HashSet<>();
				for (Node c : nodeFile.getDisinheritedChannels()) {
					restChannels.add(ModelBuilder.getNode(c));
				}
				restFile.setDisinheritedChannels(restChannels);
			}
			if (fillRefs.contains(Reference.FOLDER)) {
				restFile.setFolder(getFolder(nodeFile.getFolder()));
			}
		}
	}

	/**
	 * Transform the given folder into its REST Model
	 *
	 * @param nodeFolder
	 *            Node Object
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Folder getFolder(
			Folder nodeFolder) throws NodeException {
		return getFolder(nodeFolder, (Comparator<Folder>) null, null);
	}

	/**
	 * Transform the given folder into its REST Model
	 *
	 * @param nodeFolder
	 *            Node Object
	 * @param comparator
	 *            Comparator for sorting subfolders
	 * @param fillRefs
	 *            References to be filled in the REST Model
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Folder getFolder(
			Folder nodeFolder,
			Comparator<com.gentics.contentnode.object.Folder> comparator,
			Collection<Reference> fillRefs) throws NodeException {
		return getFolder(nodeFolder, new com.gentics.contentnode.rest.model.Folder(), comparator, fillRefs);
	}

	/**
	 * Inflate the given folder REST model from its node oblect.
	 *
	 * @param nodeFolder
	 *            Node Object
	 * @param restFolder
	 * 			  REST API object
	 * @param comparator
	 *            Comparator for sorting subfolders
	 * @param fillRefs
	 *            References to be filled in the REST Model
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Folder getFolder(
			Folder nodeFolder,
			com.gentics.contentnode.rest.model.Folder restFolder,
			Comparator<com.gentics.contentnode.object.Folder> comparator,
			Collection<Reference> fillRefs) throws NodeException {
		restFolder.setId(nodeFolder.getId());
		restFolder.setGlobalId(nodeFolder.getGlobalId().toString());
		restFolder.setName(nodeFolder.getName());
		restFolder.setDescription(nodeFolder.getDescription());

		// set the folder type
		boolean hasMaster = nodeFolder.getChannelMaster() != null;
		boolean hasMother = nodeFolder.getMother() != null;

		if (hasMother) {
			restFolder.setType(ItemType.folder);
		} else {
			restFolder.setType(hasMaster ? ItemType.channel : ItemType.node);
		}
		restFolder.setInherited(nodeFolder.isInherited());
		restFolder.setExcluded(nodeFolder.isExcluded());
		restFolder.setDisinheritDefault(nodeFolder.isDisinheritDefault());
		restFolder.setDisinherited(!nodeFolder.getMaster().getDisinheritedChannels().isEmpty());
		restFolder.setPath(getFolderPath(nodeFolder));
		restFolder.setBreadcrumbs(getBreadcrumbs(nodeFolder));

		// Folders that represent the node root folder have no mother
		if (nodeFolder.getMother() != null) {
			restFolder.setMotherId(nodeFolder.getMother().getId());
		}
		restFolder.setHasSubfolders(nodeFolder.getChildFoldersCount() > 0);

		restFolder.setCdate(nodeFolder.getCDate().getIntTimestamp());
		restFolder.setCreator(getUser(nodeFolder.getCreator()));

		restFolder.setEdate(nodeFolder.getEDate().getIntTimestamp());
		restFolder.setEditor(getUser(nodeFolder.getEditor()));

		restFolder.setPublishDir(nodeFolder.getPublishDir());

		restFolder.setNodeId(ObjectTransformer.getInteger(nodeFolder.getNode().getId(), null));

		restFolder.setDeleted(getDeleteInfo(nodeFolder));

		if (!nodeFolder.isMaster()) {
			restFolder.setMasterDeleted(getDeleteInfo(nodeFolder.getMaster()));
		}

		// set i18n data
		restFolder.setNameI18n(I18nMap.TRANSFORM2REST.apply(nodeFolder.getNameI18n()));
		restFolder.setDescriptionI18n(I18nMap.TRANSFORM2REST.apply(nodeFolder.getDescriptionI18n()));
		restFolder.setPublishDirI18n(I18nMap.TRANSFORM2REST.apply(nodeFolder.getPublishDirI18n()));

		Folder mother = nodeFolder.getMother();

		if (mother != null) {
			restFolder.setFolderDeleted(getDeleteInfo(mother.getMaster()));
		}

		// determine if there is a startpage
		Page p = nodeFolder.getStartpage();

		if (p != null) {
			restFolder.setStartPageId(p.getId());
		}

		if (nodeFolder.isRoot()) {
			restFolder.setMeshProject(nodeFolder.getNode().getMeshProject());
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		if (!ObjectTransformer.isEmpty(fillRefs)) {
			for (Reference ref : fillRefs) {
				PermHandler permHandler = t.getPermHandler();

				if (ref == Reference.PRIVILEGES) {
					// load the privileges and set them
					List<Privilege> privileges = ModelBuilder.getFolderPrivileges(restFolder.getId(), permHandler);

					restFolder.setPrivileges(privileges);
					restFolder.setPrivilegeBits(permHandler.getPermissions(Folder.TYPE_FOLDER, restFolder.getId(), -1, -1).toString());
				} else if (ref == Reference.PRIVILEGEMAP) {
					restFolder.setPrivilegeMap(ModelBuilder.getPrivileges(Folder.TYPE_FOLDER, restFolder.getId()));
				} else if (ref == Reference.TAGS) {
					// fill the objecttags
					Map<String, ObjectTag> objectTagMap = new HashMap<>(nodeFolder.getObjectTags());

					// filter out object tags, which are not available for the Node
					new ObjectTagRestrictionFilter(nodeFolder.getOwningNode()).filter(objectTagMap.values());

					Map<String, Integer> sortOrderMap = null;
					// the sortorder will only be added, if data necessary for editing shall be added to the REST Model
					if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
						sortOrderMap = getSortOrderMap(objectTagMap.values());
					}
					Map<String, com.gentics.contentnode.rest.model.Tag> tags = new HashMap<String, com.gentics.contentnode.rest.model.Tag>(objectTagMap.size());

					restFolder.setTags(tags);
					for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
						String tagName = i.next();
						ObjectTag objectTag = objectTagMap.get(tagName);

						// Transform the object tag, when object tag permission don't matter,
						// or the user has permission to view it
						if (!fillRefs.contains(Reference.OBJECT_TAGS_VISIBLE) || permHandler.canView(objectTag)) {
							com.gentics.contentnode.rest.model.Tag restTag = null;
							if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
								restTag = getObjectTag(objectTag, sortOrderMap.getOrDefault(objectTag.getName(), 0),
										true, true);
							} else {
								restTag = getTag(objectTag, false, true);
							}
							tags.put(restTag.getName(), restTag);
						}
					}
				} else if (ref == Reference.FOLDER) {
					// TODO check for permissions
					// fill the children
					List<com.gentics.contentnode.object.Folder> childFolders = new Vector<com.gentics.contentnode.object.Folder>(nodeFolder.getChildFolders());

					// sort if a comparator is given
					if (comparator != null) {
						Collections.sort(childFolders, comparator);
					}
					List<com.gentics.contentnode.rest.model.Folder> subfolders = new Vector<com.gentics.contentnode.rest.model.Folder>(childFolders.size());

					restFolder.setSubfolders(subfolders);
					for (com.gentics.contentnode.object.Folder child : childFolders) {
						subfolders.add(getFolder(child, comparator, fillRefs));
					}
				} else if (ref == Reference.DISINHERITED_CHANNELS) {
					Set<com.gentics.contentnode.rest.model.Node> restChannels = new HashSet<>();
					for (Node c : nodeFolder.getDisinheritedChannels()) {
						restChannels.add(ModelBuilder.getNode(c));
					}
					restFolder.setDisinheritedChannels(restChannels);
				}
			}
		}

		// set the position of the folder in the tree
		List<Folder> parents = nodeFolder.getParents();

		Collections.reverse(parents);
		StringBuffer atposidx = new StringBuffer();

		for (Folder parent : parents) {
			atposidx.append("-").append(parent.getId());
		}
		atposidx.append("-").append(nodeFolder.getId());
		restFolder.setAtposidx(atposidx.toString());

		Node channel = nodeFolder.getChannel();

		if (channel != null) {
			restFolder.setChannelId(ObjectTransformer.getInteger(channel.getId(), 0));
		} else {
			restFolder.setChannelId(0);
			channel = getMaster(nodeFolder.getNode());
		}

		restFolder.setInheritedFrom(channel.getFolder().getName());
		restFolder.setInheritedFromId(channel.getId());

		restFolder.setMasterNode(nodeFolder.getChannelMasterNode().getFolder().getName());
		restFolder.setMasterNodeId(nodeFolder.getChannelMasterNode().getId());

		Folder master = nodeFolder.getMaster();

		if (master != null && master != nodeFolder) {
			restFolder.setMasterId(ObjectTransformer.getInteger(master.getId(), 0));
		} else {
			restFolder.setMasterId(0);
		}

		try {
			restFolder.setChannelsetId(ObjectTransformer.getInteger(nodeFolder.getChannelSetId(), 0));
		} catch (NodeException e) {
			restFolder.setChannelsetId(0);
		}

		restFolder.setIsMaster(nodeFolder.isMaster());

		return restFolder;
	}

	/**
	 * Returns the list of privileges for the given folderId and the given permhandler.
	 * @param id Folder id
	 * @param permHandler Permission handler that can be used to check permissions
	 * @return
	 */
	public static List<Privilege> getFolderPrivileges(Integer id, PermHandler permHandler) {
		List<Privilege> privileges = new Vector<Privilege>();

		for (Privilege p : Privilege.values()) {
			if (permHandler.checkPermissionBit(com.gentics.contentnode.object.Folder.TYPE_FOLDER_INTEGER, id, p.getPermBit())) {
				privileges.add(p);
			}
		}
		return privileges;
	}

	/**
	 * Transform the given group into the REST Model, without adding child
	 * groups
	 *
	 * @param nodeGroup
	 *            Node Object
	 * @return REST Model
	 * @throws NodeException
	 */
	public static Group getGroup(UserGroup nodeGroup) throws NodeException {
		return getGroup(nodeGroup, false);
	}

	/**
	 * Transform the given group into the REST Model, optionally add child
	 * groups
	 *
	 * @param nodeGroup
	 *            Node Object
	 * @param includeChildren
	 *            true when child groups shall be added, false if not
	 * @return REST Model
	 * @throws NodeException
	 */
	public static Group getGroup(UserGroup nodeGroup, boolean includeChildren) throws NodeException {
		Group restGroup = new Group();

		restGroup.setId(ObjectTransformer.getInteger(nodeGroup.getId(), null));
		restGroup.setName(nodeGroup.getName());
		restGroup.setDescription(nodeGroup.getDescription());
		if (includeChildren) {
			List<UserGroup> childGroups = nodeGroup.getChildGroups();
			List<Group> children = new Vector<Group>(childGroups.size());

			restGroup.setChildren(children);
			for (UserGroup userGroup : childGroups) {
				children.add(getGroup(userGroup, includeChildren));
			}
		} else {
			restGroup.setChildren(null);
		}

		return restGroup;
	}

	/**
	 * Transform the given user into its REST Model, optionally fill the given
	 * references
	 *
	 * @param nodeUser
	 *            Node Object
	 * @param fillRefs
	 *            References to be filled
	 * @return REST Model
	 * @throws NodeException
	 */
	public static User getUser(SystemUser nodeUser, Reference... fillRefs) throws NodeException {
		// beware of NPE
		if (nodeUser == null) {
			return null;
		}
		User restUser = new User();

		restUser.setId(ObjectTransformer.getInt(nodeUser.getId(), 0));
		restUser.setFirstName(nodeUser.getFirstname());
		restUser.setLastName(nodeUser.getLastname());
		restUser.setEmail(nodeUser.getEmail());

		if (!ObjectTransformer.isEmpty(fillRefs)) {
			for (Reference ref : fillRefs) {
				// The description and login are not fields that should
				// be in the model by default for security reasons.
				if (ref == Reference.DESCRIPTION) {
					restUser.setDescription(nodeUser.getDescription());
				}

				if (ref == Reference.USER_LOGIN) {
					restUser.setLogin(nodeUser.getLogin());
				}

				if (ref == Reference.GROUPS) {
					List<UserGroup> userGroups = new ArrayList<>(nodeUser.getUserGroups());
					PermFilter.get(ObjectPermission.view).filter(userGroups);
					List<Group> groups = new Vector<Group>(userGroups.size());

					restUser.setGroups(groups);
					for (UserGroup userGroup : userGroups) {
						groups.add(getGroup(userGroup));
					}
				}
			}
		}

		return restUser;
	}

	/**
	 * Transform the given Overview into its REST Model
	 *
	 * @param nodeOverview
	 *            Node Object
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Overview getOverview(
			Overview nodeOverview) throws NodeException {
		com.gentics.contentnode.rest.model.Overview restOverview = new com.gentics.contentnode.rest.model.Overview();

		if (nodeOverview == null) {
			return restOverview;
		}

		// Fill IDs
		if (nodeOverview.getGlobalId() != null) {
			restOverview.setGlobalId(nodeOverview.getGlobalId().toString());
		}
		restOverview.setId(nodeOverview.getId());

		// set the selection type
		restOverview.setSelectType(Overview.getSelectType(nodeOverview.getSelectionType()));

		// set the type of selected objects
		restOverview.setListType(Overview.getListType(nodeOverview.getObjectClass()));

		// set the type of order
		restOverview.setOrderBy(Overview.getOrderBy(nodeOverview.getOrderKind()));

		// set the order direction
		restOverview.setOrderDirection(Overview.getOrderDirection(nodeOverview.getOrderWay()));

		Value value = nodeOverview.getValue();

		if (null != value) {
			// set the template's source
			restOverview.setSource(value.getValueText());
		}

		// set the entries
		List<OverviewEntry> overviewEntries = nodeOverview.getOverviewEntries();

		if (nodeOverview.isStickyChannel()) {
			List<NodeIdObjectId> selectedObjects = new ArrayList<>(overviewEntries.size());

			for (OverviewEntry entry : overviewEntries) {
				selectedObjects.add(new NodeIdObjectId(entry.getNodeId(), entry.getObjectId()));
			}

			// if the sortorder is DESC, we will reverse the order of the entries
			// (entries will always be set in selection order, not in the real
			// sortorder)
			if (nodeOverview.getOrderWay() == com.gentics.contentnode.object.Overview.ORDERWAY_DESC) {
				Collections.reverse(selectedObjects);
			}
			restOverview.setSelectedNodeItemIds(selectedObjects);
		} else {
			List<Integer> selectedItemIds = new ArrayList<>(overviewEntries.size());
			for (OverviewEntry entry : overviewEntries) {
				selectedItemIds.add(entry.getObjectId());
			}

			// if the sortorder is DESC, we will reverse the order of the entries
			// (entries will always be set in selection order, not in the real
			// sortorder)
			if (nodeOverview.getOrderWay() == com.gentics.contentnode.object.Overview.ORDERWAY_DESC) {
				Collections.reverse(selectedItemIds);
			}
			restOverview.setSelectedItemIds(selectedItemIds);
		}

		// set the maximum number of listed entries
		restOverview.setMaxItems(nodeOverview.getMaxObjects());

		// set the recursive flag
		restOverview.setRecursive(nodeOverview.doRecursion());

		return restOverview;
	}

	/**
	 * Transform the given template tag into its REST Model
	 *
	 * @param templateTag
	 * @param addConstruct true to add construct
	 * @throws NodeException
	 * @return
	 */
	public static com.gentics.contentnode.rest.model.TemplateTag getTemplateTag(TemplateTag templateTag, boolean addConstruct, boolean addPrivateData) throws NodeException {
		com.gentics.contentnode.rest.model.TemplateTag restTemplateTag = new com.gentics.contentnode.rest.model.TemplateTag();

		applyTagProperties(templateTag, restTemplateTag, addConstruct, addPrivateData);
		restTemplateTag.setEditableInPage(templateTag.isPublic());
		restTemplateTag.setMandatory(templateTag.getMandatory());

		return restTemplateTag;
	}

	/**
	 * Transform the given tag into its REST Model
	 *
	 * @param nodeTag
	 *            Node Object
	 * @param addConstruct true to add construct info
	 * @return REST Model
	 * @throws NodeException
	 */
	/**
	 * Transform the given tag into its REST Model
	 *
	 * @param nodeTag
	 *            Node Object
	 * @param addConstruct true to add construct info
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Tag getTag(Tag nodeTag, boolean addConstruct) throws NodeException {
		return getTag(nodeTag, addConstruct, true);
	}

	public static com.gentics.contentnode.rest.model.Tag getTag(Tag nodeTag, boolean addConstruct, boolean addPrivateData) throws NodeException {
		if (nodeTag instanceof TemplateTag) {
			return getTemplateTag((TemplateTag) nodeTag, addConstruct, addPrivateData);
		}

		if (nodeTag instanceof ObjectTag) {
			return getObjectTag((ObjectTag) nodeTag, 0, addConstruct, addPrivateData);
		}

		com.gentics.contentnode.rest.model.Tag restTag = new com.gentics.contentnode.rest.model.Tag();

		applyTagProperties(nodeTag, restTag, addConstruct, addPrivateData);
		return restTag;
	}

	/**
	 * Transform the given object tag into its REST Model
	 * @param nodeTag node tag
	 * @param sortOrder sort order
	 * @param addConstruct true to add construct
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.ObjectTag getObjectTag(ObjectTag nodeTag, int sortOrder, boolean addConstruct, boolean addPrivateData) throws NodeException {
		com.gentics.contentnode.rest.model.ObjectTag restTag = new com.gentics.contentnode.rest.model.ObjectTag();

		applyTagProperties(nodeTag, restTag, addConstruct, addPrivateData);
		ObjectTagDefinition def = nodeTag.getDefinition();
		if (def != null) {
			restTag.setDisplayName(def.getName());
			restTag.setDescription(def.getDescription());
			ObjectTagDefinitionCategory category = def.getCategory();
			if (category != null) {
				restTag.setCategoryName(category.getName());
			}
		} else {
			restTag.setDisplayName(nodeTag.getName());
		}
		restTag.setInheritable(nodeTag.isInheritable());
		restTag.setRequired(nodeTag.isRequired());
		restTag.setSortOrder(sortOrder);
		// temporarily disable publish cache, because checking permissions on an objecttag might try to load the container object, which - when publish cache is enabled - will get the REST model
		// of the object, including its object tags. this would lead to endless recursions
		try (PublishCacheTrx pCacheTrx = new PublishCacheTrx(false)) {
			restTag.setReadOnly(!TransactionManager.getCurrentTransaction().getPermHandler().canEdit(nodeTag));
		}
		return restTag;
	}

	/**
	 * Applies all tag specific properties to the given rest tag by using the values form the node tag
	 *
	 * @param nodeTag
	 * @param restTag
	 * @param addConstruct true to add construct
	 * @throws NodeException
	 */
	private static void applyTagProperties(Tag nodeTag, com.gentics.contentnode.rest.model.Tag restTag, boolean addConstruct, boolean addPrivateData) throws NodeException {
		// Meta Properties
		if (addPrivateData) {
			restTag.setId(ObjectTransformer.getInteger(nodeTag.getId(), null));
		}
		if (nodeTag.getObjectInfo().isEditable()) {
			switch (nodeTag.getEnabledValue()) {
			case 0:
				restTag.setActive(false);
				break;
			default:
				restTag.setActive(true);
				break;
			}
		} else {
			restTag.setActive(nodeTag.isEnabled());
		}
		if (addPrivateData) {
			restTag.setConstructId(ObjectTransformer.getInteger(nodeTag.getConstructId(), null));
		}
		if (addConstruct) {
			restTag.setConstruct(Construct.TRANSFORM2REST.apply(nodeTag.getConstruct()));
		}
		restTag.setName(nodeTag.getName());

		int ttype = ObjectTransformer.getInt(nodeTag.getTType(), 0);

		switch (ttype) {
		case Tag.TYPE_CONTENTTAG:
			restTag.setType(com.gentics.contentnode.rest.model.Tag.Type.CONTENTTAG);
			break;

		case Tag.TYPE_TEMPLATETAG:
			restTag.setType(com.gentics.contentnode.rest.model.Tag.Type.TEMPLATETAG);
			break;

		case Tag.TYPE_OBJECTTAG:
			restTag.setType(com.gentics.contentnode.rest.model.Tag.Type.OBJECTTAG);
			break;
		}

		// values as properties
		ValueList values = nodeTag.getValues();
		Map<String, Property> properties = new HashMap<String, Property>(values.size());

		for (Value value : values) {
			Property property = value.getPartType().toProperty();

			properties.put(value.getPart().getKeyname(), property);
		}
		restTag.setProperties(properties);
	}

	/**
	 * Deletes the contenttags and objecttags from the given page.
	 *
	 * @param tagsToDelete
	 * @param nodePage
	 * @throws NodeException
	 */
	public static void deleteTags(List<String> tagsToDelete, Page nodePage) throws NodeException {
		if (!ObjectTransformer.isEmpty(tagsToDelete)) {
			for (String toDelete : tagsToDelete) {
				if (toDelete == null) {
					continue;
				}
				if (toDelete.startsWith("object.")) {
					String objectTagNameWithoutObject = toDelete.substring(7);
					// this is an object tag
					nodePage.getObjectTags().remove(objectTagNameWithoutObject);
				} else {
					// this is a content tag
					nodePage.getContent().getTags().remove(toDelete);
				}
			}
		}
	}

	/**
	 * Delete the templatetags and objecttags from the given template
	 * @param tagsToDelete list of tag names
	 * @param template template
	 * @throws NodeException
	 */
	public static void deleteTags(List<String> tagsToDelete, Template template) throws NodeException {
		if (ObjectTransformer.isEmpty(tagsToDelete)) {
			return;
		}

		for (String toDelete : tagsToDelete) {
			if (ObjectTransformer.isEmpty(toDelete)) {
				continue;
			}

			if (toDelete.startsWith("object.")) {
				template.getObjectTags().remove(toDelete.substring("object.".length()));
			} else {
				template.getTags().remove(toDelete);
			}
		}
	}

	/**
	 * Transform the given page into its REST Model
	 *
	 * @param nodePage
	 * @return
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Page getPage(Page nodePage) throws NodeException {
		return getPage(nodePage, (Collection<Reference>) null, null);
	}

	/**
	 * Transform the given page into its REST Model
	 *
	 * @param nodePage
	 *            Node Object
	 * @param fillRefs
	 *            References to be filled
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Page getPage(
			Page nodePage, Collection<Reference> fillRefs) throws NodeException {
		return getPage(nodePage, fillRefs, null);
	}

	/**
	 * Transform the given page into its REST Model
	 *
	 * @param nodePage
	 *            Node Object
	 * @param fillRefs
	 *            References to be filled
	 * @param wastebin optional wastebin for filtering language variants or page variants
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Page getPage(
			Page nodePage, Collection<Reference> fillRefs, Wastebin wastebin) throws NodeException {
		return getPage(nodePage, new com.gentics.contentnode.rest.model.Page(), fillRefs, wastebin);
	}

	/**
	 * Inflate the page REST Model from its given node object.
	 *
	 * @param nodePage
	 *            Node Object
	 * @param restPage
	 *            REST model to inflate
	 * @param fillRefs
	 *            References to be filled
	 * @param wastebin optional wastebin for filtering language variants or page variants
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Page getPage(
			Page nodePage, com.gentics.contentnode.rest.model.Page restPage, Collection<Reference> fillRefs, Wastebin wastebin) throws NodeException {
		restPage.setId(nodePage.getId());
		restPage.setGlobalId(nodePage.getGlobalId().toString());
		restPage.setContentId(ObjectTransformer.getInteger(nodePage.getContent().getId(), null));
		restPage.setChannelSetId(ObjectTransformer.getInteger(nodePage.getChannelSetId(), null));
		restPage.setName(nodePage.getName());
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			restPage.setNiceUrl(nodePage.getNiceUrl());
			restPage.setAlternateUrls(new TreeSet<>(nodePage.getAlternateUrls()));
		}
		restPage.setFileName(nodePage.getFilename());
		restPage.setFolderId(nodePage.getFolder().getId());
		restPage.setDescription(nodePage.getDescription());
		restPage.setPriority(nodePage.getPriority());
		restPage.setTemplateId(nodePage.getTemplate().getId());

		restPage.setDeleted(getDeleteInfo(nodePage));

		if (!nodePage.isMaster()) {
			restPage.setMasterDeleted(getDeleteInfo(nodePage.getMaster()));
		}

		restPage.setFolderDeleted(getDeleteInfo(nodePage.getFolder().getMaster()));

		restPage.setMaster(nodePage.isMaster());
		Node channel = nodePage.getChannel();

		if (channel != null) {
			restPage.setChannelId(ObjectTransformer.getInteger(channel.getId(), null));
		}

		if (channel == null) {
			channel = getMaster(nodePage.getFolder().getNode());
		}
		restPage.setInheritedFrom(channel.getFolder().getName());
		restPage.setInheritedFromId(channel.getId());

		restPage.setMasterNode(nodePage.getMasterNodeFolderName());
		restPage.setMasterNodeId(nodePage.getChannelMasterNode().getId());

		restPage.setCdate(nodePage.getCDate().getIntTimestamp());
		restPage.setCreator(getUser(nodePage.getCreator()));

		restPage.setCustomCdate(nodePage.getCustomCDate().getIntTimestamp());

		restPage.setEdate(nodePage.getEDate().getIntTimestamp());
		restPage.setEditor(getUser(nodePage.getEditor()));

		restPage.setCustomEdate(nodePage.getCustomEDate().getIntTimestamp());

		restPage.setPdate(nodePage.getPDate().getIntTimestamp());
		if (nodePage.getPublisher() != null) {
			restPage.setPublisher(getUser(nodePage.getPublisher()));
		}

		restPage.setContentSetId(nodePage.getContentsetId());

		restPage.setInherited(nodePage.isInherited());
		restPage.setOnline(nodePage.isOnline());
		restPage.setModified(nodePage.isModified());
		restPage.setQueued(nodePage.isQueued());
		restPage.setPlanned(nodePage.isPlanned());
		restPage.setExcluded(nodePage.isExcluded());
		restPage.setDisinheritDefault(nodePage.isDisinheritDefault());
		restPage.setDisinherited(!nodePage.getMaster().getDisinheritedChannels().isEmpty());

		restPage.setLocked(nodePage.getContent().isLocked());
		if (restPage.isLocked()) {
			restPage.setLockedBy(getUser(nodePage.getContent().getLockedBy()));
			ContentNodeDate lockedSince = nodePage.getContent().getLockedSince();

			if (lockedSince != null) {
				restPage.setLockedSince(lockedSince.getIntTimestamp());
			}
		}

		// eventually build the language exactly like in GCN
		ContentLanguage language = nodePage.getLanguage();

		if (language != null) {
			restPage.setLanguage(language.getCode());
			restPage.setLanguageName(language.getName());
			restPage.setContentGroupId(language.getId());
		}

		// timemanagement
		TimeManagement timeManagement = new TimeManagement();
		timeManagement.setAt(nodePage.getTimePub().getTimestamp());
		timeManagement.setVersion(getPageVersion(nodePage.getTimePubVersion()));
		timeManagement.setOfflineAt(nodePage.getTimeOff().getTimestamp());

		if (nodePage.getPubQueueUser() != null) {
			timeManagement.setQueuedPublish(new QueuedTimeManagement().setAt(nodePage.getTimePubQueue().getIntTimestamp())
					.setUser(getUser(nodePage.getPubQueueUser())).setVersion(getPageVersion(nodePage.getTimePubVersionQueue())));
		}
		if (nodePage.getOffQueueUser() != null) {
			timeManagement.setQueuedOffline(
					new QueuedTimeManagement().setAt(nodePage.getTimeOffQueue().getIntTimestamp()).setUser(getUser(nodePage.getOffQueueUser())));
		}
		restPage.setTimeManagement(timeManagement);

		restPage.setPath(getFolderPath(nodePage.getFolder()));

		// Render the Live URL property
		restPage.setLiveUrl(renderLiveUrlForObject(Page.class, nodePage, nodePage.getNode()));

		restPage.setPublishPath(renderPublishPath(nodePage));

		// eventually fill references
		if (fillRefs != null) {
			for (Reference reference : fillRefs) {
				switch (reference) {
				case PAGEVARIANTS:
					List<com.gentics.contentnode.rest.model.Page> pageVariants = new ArrayList<com.gentics.contentnode.rest.model.Page>();

					restPage.setPageVariants(pageVariants);

					// Page Variants should contain the same information as the
					// other pages (but NOT the pagevariants or languagevariants
					// of course)
					Collection<Reference> pageVariantRefs = new Vector<Reference>(fillRefs);

					pageVariantRefs.remove(Reference.PAGEVARIANTS);
					pageVariantRefs.remove(Reference.LANGUAGEVARIANTS);
					List<Page> variants = new ArrayList<Page>(nodePage.getPageVariants());
					if (wastebin != null) {
						wastebin.filter(variants);
					}
					for (com.gentics.contentnode.object.Page pageVariant : variants) {
						// exclude the page itself from the list of pages
						if (pageVariant.getId().equals(nodePage.getId())) {
							continue;
						}
						pageVariants.add(getPage(pageVariant, pageVariantRefs));
					}
					// Clear the field if no data was retrieved
					if (pageVariants.size() == 0) {
						restPage.setPageVariants(null);
					}
					break;

				case TEMPLATE:
					restPage.setTemplate(getTemplate(nodePage.getTemplate(), fillRefs));
					break;

				case FOLDER:
					restPage.setFolder(getFolder(nodePage.getFolder()));
					break;

				case LANGUAGEVARIANTS:
					List<com.gentics.contentnode.object.Page> nodeLangVars = new ArrayList<Page>(nodePage.getLanguageVariants(true));
					if (wastebin != null) {
						wastebin.filter(nodeLangVars);
					}
					Map<Object, com.gentics.contentnode.rest.model.Page> languageVariants = new LinkedHashMap<Object, com.gentics.contentnode.rest.model.Page>(
							nodeLangVars.size());

					restPage.setLanguageVariants(languageVariants);

					// Language Variants should contain the same information as the
					// other pages (but NOT the pagevariants or languagevariants
					// of course)
					Collection<Reference> langVariantRefs = new Vector<Reference>(fillRefs);

					langVariantRefs.remove(Reference.PAGEVARIANTS);
					langVariantRefs.remove(Reference.LANGUAGEVARIANTS);

					for (com.gentics.contentnode.object.Page page : nodeLangVars) {
						languageVariants.put(page.getLanguageId(), getPage(page, langVariantRefs));
					}
					break;

				case WORKFLOW:
					PublishWorkflow publishWorkflow = nodePage.getWorkflow();

					if (publishWorkflow != null) {
						restPage.setWorkflow(getWorkflow(publishWorkflow));
					}
					break;

				case TRANSLATIONSTATUS:
					restPage.setTranslationStatus(getTranslationStatus(nodePage));
					break;

				case VERSIONS:
					// set the current version
					NodeObjectVersion currentVersion = nodePage.getVersion();

					if (currentVersion != null) {
						restPage.setCurrentVersion(getPageVersion(currentVersion));
					}

					// set the published version
					NodeObjectVersion publishedVersion = nodePage.getPublishedVersion();

					if (publishedVersion != null) {
						restPage.setPublishedVersion(getPageVersion(publishedVersion));
					}

					// set all versions
					NodeObjectVersion[] pageVersions = nodePage.getVersions();
					List<com.gentics.contentnode.rest.model.PageVersion> restPageVersions = new Vector<com.gentics.contentnode.rest.model.PageVersion>(
							pageVersions.length);

					for (NodeObjectVersion pageVersion : pageVersions) {
						restPageVersions.add(0, getPageVersion(pageVersion));
					}
					restPage.setVersions(restPageVersions);
					break;

				case CONTENT_TAGS:
					// add contenttags
					Map<String, ContentTag> contenttagMap = nodePage.getContent().getContentTags();
					Map<String, com.gentics.contentnode.rest.model.Tag> restContentTags = new HashMap<String, com.gentics.contentnode.rest.model.Tag>(
							contenttagMap.size());

					for (Iterator<String> i = contenttagMap.keySet().iterator(); i.hasNext();) {
						String tagName = i.next();
						com.gentics.contentnode.rest.model.Tag restTag = getTag(contenttagMap.get(tagName), fillRefs.contains(Reference.TAG_EDIT_DATA), true);

						restContentTags.put(restTag.getName(), restTag);
					}
					restPage.addTags(restContentTags);
					break;

				case OBJECT_TAGS:
				case OBJECT_TAGS_VISIBLE:
					PermHandler permHandler = null;
					if (reference == Reference.OBJECT_TAGS_VISIBLE) {
						permHandler = TransactionManager.getCurrentTransaction().getPermHandler();
					}

					// add objecttags
					Map<String, ObjectTag> objectTagMap = new HashMap<>(nodePage.getObjectTags());

					// filter out restricted object tags
					new ObjectTagRestrictionFilter(nodePage.getOwningNode()).filter(objectTagMap.values());

					Map<String, Integer> sortOrderMap = null;
					// the sortorder will only be added, if data necessary for editing shall be added to the REST Model
					if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
						sortOrderMap = getSortOrderMap(objectTagMap.values());
					}
					Map<String, com.gentics.contentnode.rest.model.Tag> restObjectTags = new HashMap<>(objectTagMap.size());

					for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
						String tagName = i.next();
						ObjectTag objectTag = objectTagMap.get(tagName);

						// Transform the object tag to a rest object tag if either the permissions
						// are NOT checked or the user has permission to see it.
						if (reference.equals(Reference.OBJECT_TAGS) || permHandler.canView(objectTag)) {
							com.gentics.contentnode.rest.model.Tag restTag = null;

							if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
								restTag = getObjectTag(objectTag,
										sortOrderMap.getOrDefault(objectTag.getName(), 0), true, true);
							} else {
								restTag = getTag(objectTag, false, true);
							}

							restObjectTags.put(restTag.getName(), restTag);
						}
					}
					restPage.addTags(restObjectTags);
					break;
				case DISINHERITED_CHANNELS:
					Set<com.gentics.contentnode.rest.model.Node> restChannels = new HashSet<>();
					if (nodePage.isMaster()) {
						for (Node c : nodePage.getDisinheritedChannels()) {
							restChannels.add(ModelBuilder.getNode(c));
						}
						restPage.setDisinheritedChannels(restChannels);
					}
					break;
				default:
					break;
				}
			}
		}
		return restPage;
	}

	/**
	 * Transform the given page into a simple REST Model
	 *
	 * @param nodePage
	 *            Node Object
	 * @return Simple REST Model
	 * @throws NodeException
	 */
	public static SimplePage getSimplePage(Page nodePage) throws NodeException {
		SimplePage restPage = new SimplePage();

		restPage.setId(nodePage.getId());
		restPage.setName(nodePage.getName());
		restPage.setOnline(nodePage.isOnline());
		restPage.setModified(nodePage.isModified());
		StringBuffer pathBuffer = new StringBuffer("/");
		Folder folder = nodePage.getFolder();

		while (folder != null) {
			String folderName = folder.getName();

			if (!ObjectTransformer.isEmpty(folderName)) {
				pathBuffer.insert(0, folderName);
				pathBuffer.insert(0, "/");
			}

			folder = folder.getMother();
		}
		restPage.setPath(pathBuffer.toString());

		return restPage;
	}

	/**
	 * Get the GCN Page corresponding to the given REST Page
	 *
	 * @param restPage
	 *            REST Page object
	 * @param forPreview
	 *            true if the page shall be fetched for preview
	 * @return GCN Page object
	 */
	public static Page getPage(com.gentics.contentnode.rest.model.Page restPage, boolean forPreview) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// if the given page is null, the gcn page is also null
		if (restPage == null) {
			return null;
		}
		// try to load the page by id
		Page page = null;

		if (forPreview) {
			// when fetching for preview, we load the page and afterwards create an editable copy
			page = t.getObject(Page.class, restPage.getId(), false);
			if (page != null) {
				page = PageFactory.getEditableClone(page);
			}
		} else {
			// when not fetching for preview, we get the page for update (locked)
			page = t.getObject(Page.class, restPage.getId(), true);
		}

		// when the page could not be loaded (e.g. the id is not set), we create a new page
		if (page == null) {
			page = (Page) t.createObject(Page.class);
			if (restPage.getFolderId() != null) {
				page.setFolderId(restPage.getFolderId());
			}
		}

		// validate the page TODO implement this
		// PageValidator validator = new PageValidatorAndErrorCollector(page);
		// validator.processRestPage(restPage);
		// for (ValidationResult result : validator.getResults()) {
		// if (result.hasErrors()) {
		// ResponseInfo responseInfo = new ResponseInfo(ResponseCode.FAILURE, ValidationUtils.formatValidationError(result));
		// Message message = new Message(Message.Type.CRITICAL, ValidationUtils.formatValidationError(result));
		// return new GenericResponse(message, responseInfo);
		// }
		// }

		// TODO move the code below into a PageUpdater

		// fill the meta data from the rest page into the gcn page object
		if (restPage.getTemplateId() != null) {
			page.setTemplateId(restPage.getTemplateId());
		}
		if (restPage.getDescription() != null) {
			page.setDescription(restPage.getDescription());
		}
		if (!ObjectTransformer.isEmpty(restPage.getFileName())) {
			page.setFilename(restPage.getFileName());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (restPage.getNiceUrl() != null) {
				page.setNiceUrl(restPage.getNiceUrl());
			}
			if (restPage.getAlternateUrls() != null) {
				page.setAlternateUrls(restPage.getAlternateUrls());
			}
		}
		if (!ObjectTransformer.isEmpty(restPage.getName())) {
			page.setName(restPage.getName());
		}
		if (restPage.getPriority() != null) {
			page.setPriority(restPage.getPriority());
		}
		if (restPage.getCustomCdate() != null) {
			page.setCustomCDate(restPage.getCustomCdate());
		}
		if (restPage.getCustomEdate() != null) {
			page.setCustomEDate(restPage.getCustomEdate());
		}

		// transform the requested language code into a language and set it to the page
		ContentLanguage language = MiscUtils.getRequestedContentLanguage(page, restPage.getLanguage());

		if (language != null) {
			page.setLanguage(language);
		}

		// set the translation status
		if (restPage.getTranslationStatus() != null) {
			TranslationStatus translationStatus = restPage.getTranslationStatus();
			Page syncPage = null;

			if (translationStatus.getVersionTimestamp() == null) {
				syncPage = (Page) t.getObject(Page.class, translationStatus.getPageId());
			} else {
				syncPage = (Page) t.getObject(Page.class, translationStatus.getPageId(), translationStatus.getVersionTimestamp());
			}
			page.synchronizeWithPage(syncPage);
		}

		// set the tags
		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restPage.getTags();

		if (restTags != null) {
			// get the contenttags and objecttags
			Map<String, ContentTag> contentTags = page.getContent().getContentTags();
			Map<String, ObjectTag> objectTags = page.getObjectTags();

			for (Iterator<com.gentics.contentnode.rest.model.Tag> i = restTags.values().iterator(); i.hasNext();) {
				com.gentics.contentnode.rest.model.Tag restTag = i.next();
				Tag tag = null;
				String tagName = getTagNameOrThrow(restTag);

				if (tagName.startsWith("object.")) {
					String realName = restTag.getName().substring(7);
					ObjectTag oTag = objectTags.get(realName);

					if (oTag == null) {
						if (logger.isDebugEnabled()) {
							logger.debug(
									"The object tag {" + restTag.getName()
									+ "} was not found for the given page. Creation of objecttags is currently not supported. Omitting objecttag.");
						}
						// TODO Creation of object tags is currently not possible since it is not implemented in the TagFactory
						// // did not find the tag, so create a new one
						// oTag = (ObjectTag)t.createObject(ObjectTag.class);
						// oTag.setName(realName);
						// oTag.setConstructId(restTag.getConstructId());
						//
						// // and add it to the other tags
						// objectTags.put(restTag.getName(), oTag);
					}
					tag = oTag;
				} else {
					ContentTag contentTag = contentTags.get(restTag.getName());

					if (contentTag == null) {
						// did not find the tag, so create a new one
						contentTag = (ContentTag) t.createObject(ContentTag.class);
						contentTag.setName(restTag.getName());
						contentTag.setConstructId(restTag.getConstructId());

						// and add it to the other tags
						contentTags.put(restTag.getName(), contentTag);
					}
					tag = contentTag;
				}

				if (tag != null) {
					// now set the data from the REST tag
					fillRest2Node(restTag, tag);
				}
			}
		}

		return page;
	}

	/**
	 * Transform the given template into its REST Model
	 *
	 * @param nodeTemplate
	 *            Node Object
	 * @param fillRefs references to be filled
	 * @return REST Model
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Template getTemplate(
			Template nodeTemplate, Collection<Reference> fillRefs) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.getChannel();
		com.gentics.contentnode.rest.model.Template restTemplate = Template.TRANSFORM2REST.apply(nodeTemplate);

		// eventually fill references
		if (fillRefs != null) {
			for (Reference reference : fillRefs) {
				switch (reference) {

				// add templatetags
				case TEMPLATE_TAGS:

					Map<String, TemplateTag> templateTagMap = nodeTemplate.getTags();
					Map<String, com.gentics.contentnode.rest.model.TemplateTag> restTemplateTags = new HashMap<String, com.gentics.contentnode.rest.model.TemplateTag>(
							templateTagMap.size());

					for (Iterator<String> i = templateTagMap.keySet().iterator(); i.hasNext();) {
						String tagName = i.next();
						com.gentics.contentnode.rest.model.TemplateTag restTag = getTemplateTag(templateTagMap.get(tagName),
								fillRefs.contains(Reference.TAG_EDIT_DATA), true);

						restTemplateTags.put(restTag.getName(), restTag);
					}

					restTemplate.setTemplateTags(restTemplateTags);
					break;
				case TEMPLATE_SOURCE:
					restTemplate.setSource(nodeTemplate.getSource());
					break;
				case OBJECT_TAGS:
				case OBJECT_TAGS_VISIBLE:
					PermHandler permHandler = null;
					if (reference == Reference.OBJECT_TAGS_VISIBLE) {
						permHandler = t.getPermHandler();
					}

					// add objecttags
					Map<String, ObjectTag> objectTagMap = new HashMap<>(nodeTemplate.getObjectTags());

					if (node != null) {
						// filter out restricted object tags
						new ObjectTagRestrictionFilter(node.getMaster()).filter(objectTagMap.values());
					}

					Map<String, Integer> sortOrderMap = null;
					// the sortorder will only be added, if data necessary for editing shall be added to the REST Model
					if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
						sortOrderMap = getSortOrderMap(objectTagMap.values());
					}
					Map<String, com.gentics.contentnode.rest.model.Tag> restObjectTags = new HashMap<String, com.gentics.contentnode.rest.model.Tag>(
							objectTagMap.size());

					for (Iterator<String> i = objectTagMap.keySet().iterator(); i.hasNext();) {
						String tagName = i.next();
						ObjectTag objectTag = objectTagMap.get(tagName);

						// Transform the object tag to a rest object tag if either the permissions
						// are NOT checked or the user has permission to see it.
						if (reference.equals(Reference.OBJECT_TAGS) || permHandler.canView(objectTag)) {
							com.gentics.contentnode.rest.model.Tag restTag = null;
							if (fillRefs.contains(Reference.TAG_EDIT_DATA)) {
								restTag = getObjectTag(objectTag, sortOrderMap.getOrDefault(objectTag.getName(), 0), true, true);
							} else {
								restTag = getTag(objectTag, false, true);
							}
							restObjectTags.put(restTag.getName(), restTag);
						}
					}

					restTemplate.setObjectTags(restObjectTags);
					break;
				default:
					break;
				}
			}
		}

		return restTemplate;
	}

	/**
	 * Transform the given pageversion into a REST Model (for the latest
	 * version)
	 *
	 * @param latestVersion
	 *            Node Object
	 * @return REST Model
	 */
	public static Latest getLatest(NodeObjectVersion latestVersion) {
		Latest latest = new Latest();

		latest.setVersion(latestVersion.getNumber());
		latest.setVersionTimestamp(latestVersion.getDate().getIntTimestamp());
		return latest;
	}

	/**
	 * Transform the given page into the REST Model of its translation status
	 *
	 * @param nodePage
	 *            Node Object
	 * @return REST Model for the translation status
	 * @throws NodeException
	 */
	public static TranslationStatus getTranslationStatus(Page nodePage) throws NodeException {
		TranslationStatus restTranslationStatus = new TranslationStatus();

		Page syncPage = nodePage.getSynchronizedWith();

		if (syncPage == null) {
			restTranslationStatus.setPageId(null);
			restTranslationStatus.setName(null);
			restTranslationStatus.setVersionTimestamp(null);
			restTranslationStatus.setLanguage(null);
			restTranslationStatus.setInSync(true);
			restTranslationStatus.setVersion(null);
			restTranslationStatus.setLatestVersion(null);
		} else {
			restTranslationStatus.setPageId(ObjectTransformer.getInt(syncPage.getId(), 0));
			restTranslationStatus.setName(syncPage.getName());
			restTranslationStatus.setVersionTimestamp(syncPage.getObjectInfo().getVersionTimestamp());
			restTranslationStatus.setLanguage(syncPage.getLanguage().getCode());
			restTranslationStatus.setInSync(nodePage.isInSync());
			NodeObjectVersion pageVersion = syncPage.getVersion();

			if (pageVersion != null) {
				restTranslationStatus.setVersion(pageVersion.getNumber());
			} else {
				restTranslationStatus.setVersion("");
			}
			NodeObjectVersion[] syncPageVersions = syncPage.getVersions();

			if (syncPageVersions.length > 0) {
				restTranslationStatus.setLatestVersion(getLatest(syncPageVersions[0]));
			}
		}

		return restTranslationStatus;
	}

	/**
	 * Transform the given workflow into its REST Model
	 *
	 * @param nodeWorkflow
	 *            Node Object
	 * @return REST Model
	 * @throws NodeException
	 */
	public static Workflow getWorkflow(PublishWorkflow nodeWorkflow) throws NodeException {
		Workflow restWorkflow = new Workflow();

		PublishWorkflowStep currentStep = nodeWorkflow.getCurrentStep();

		restWorkflow.setMessage(currentStep.getMessage());
		restWorkflow.setUser(getUser(currentStep.getCreator(), Reference.GROUPS));
		restWorkflow.setModified(currentStep.isPageModified());
		List<UserGroup> userGroups = currentStep.getUserGroups();
		List<Group> groups = new Vector<Group>(userGroups.size());

		restWorkflow.setGroups(groups);
		for (UserGroup userGroup : userGroups) {
			groups.add(getGroup(userGroup));
		}
		restWorkflow.setTimestamp(currentStep.getEDate().getIntTimestamp());

		return restWorkflow;
	}

	/**
	 * Transform the given policy group into a response
	 *
	 * @param group
	 *            Policy Group
	 * @return REST Model of the Response
	 */
	public static PolicyGroupResponse getPolicyGroupResponse(PolicyGroup group) {
		PolicyGroupResponse response = new PolicyGroupResponse();
		Policy defaultPolicy = group.getDefaultPolicy();

		for (Policy policy : group.getPolicies()) {
			response.policies.add(getGroupPolicyResponse(policy, policy.equals(defaultPolicy)));
		}
		return response;
	}

	/**
	 * Transform the given policy into a response
	 *
	 * @param policy
	 *            Policy
	 * @return REST Model of the Response
	 */
	public static PolicyResponse getPolicyResponse(Policy policy) {
		PolicyResponse response = new PolicyResponse();

		response.name = policy.getEffectiveName();
		response.uri = policy.getURI().toString();
		return response;
	}

	/**
	 * Transform the given policy and default into a response
	 *
	 * @param policy
	 *            Policy
	 * @param _default
	 *            true if the policy is the default
	 * @return REST Model of a GroupPolicyResponse
	 */
	public static GroupPolicyResponse getGroupPolicyResponse(Policy policy,
			boolean _default) {
		GroupPolicyResponse response = new GroupPolicyResponse();

		response.name = policy.getEffectiveName();
		response.uri = policy.getURI().toString();
		response._default = _default;
		return response;
	}

	/**
	 * Transform a validation message into a REST Response
	 * @param message Validation Message
	 * @return REST Response
	 */
	public static ValidationMessageResponse getValidationMessageResponse(
			ValidationMessage message) {
		ValidationMessageResponse response = new ValidationMessageResponse();

		response.message = message.toString();
		response.fatal = message.isFatal();
		return response;
	}

	/**
	 * Transform a validation result into a REST Response
	 * @param result Validation Result
	 * @param formatted true of errors shall be formatted
	 * @return REST Response
	 */
	public static ValidationResultResponse getValidationResultResponse(
			ValidationResult result, boolean formatted) {
		ValidationResultResponse response = new ValidationResultResponse();

		for (ValidationMessage message : result.getMessages()) {
			response.messages.add(getValidationMessageResponse(message));
		}
		response.cleanMarkup = result.getCleanMarkup();
		if (formatted && result.hasErrors()) {
			response.formattedError = ValidationUtils.formatValidationError(result);
		}

		return response;
	}

	/**
	 * Transform the given node message into the REST Model
	 * @param message node message
	 * @return REST Model of the message
	 */
	public static com.gentics.contentnode.rest.model.response.Message getMessage(Message message) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		com.gentics.contentnode.rest.model.response.Message restMessage = new com.gentics.contentnode.rest.model.response.Message();

		restMessage.setType(Type.NEUTRAL);
		restMessage.setMessage(message.getParsedMessage());
		restMessage.setSender(getUser(t.getObject(SystemUser.class, message.getFromId())));
		restMessage.setTimestamp(message.getCreationTime());

		return restMessage;
	}

	/**
	 * Transform the given node message into the REST Model
	 * @param nodeMessage node message
	 * @return REST Model of the message
	 */
	public static com.gentics.contentnode.rest.model.response.Message getMessage(NodeMessage nodeMessage) {
		com.gentics.contentnode.rest.model.response.Message restMessage = new com.gentics.contentnode.rest.model.response.Message();
		// set the type
		Level level = nodeMessage.getLevel();

		if (level.equals(Level.FATAL)) {
			restMessage.setType(Type.CRITICAL);
		} else if (level.equals(Level.ERROR) || level.equals(Level.WARN)) {
			// We handle errors as warnings, because Velocity errors etc.
			// are not critical in this context.
			restMessage.setType(Type.WARNING);
		} else {
			restMessage.setType(Type.INFO);
		}
		restMessage.setMessage(nodeMessage.getMessage());
		restMessage.setTimestamp(System.currentTimeMillis());

		return restMessage;
	}

	/**
	 * Transform the given pageversion into the REST Model
	 * @param pageVersion pageVersion
	 * @return REST Model of the pageVersion
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.PageVersion getPageVersion(
			NodeObjectVersion pageVersion) throws NodeException {
		if (pageVersion == null) {
			return null;
		}
		com.gentics.contentnode.rest.model.PageVersion restPageVersion = new com.gentics.contentnode.rest.model.PageVersion();

		restPageVersion.setNumber(pageVersion.getNumber());
		restPageVersion.setTimestamp(pageVersion.getDate().getIntTimestamp());
		restPageVersion.setEditor(getUser(pageVersion.getEditor()));

		return restPageVersion;
	}

	/**
	 * Get the folder path of the folder
	 * @param folder folder
	 * @return folder path
	 * @throws NodeException
	 */
	public static String getFolderPath(Folder folder) throws NodeException {
		StringBuffer pathBuffer = new StringBuffer("/");

		while (folder != null) {
			String folderName = folder.getName();

			if (!ObjectTransformer.isEmpty(folderName)) {
				pathBuffer.insert(0, folderName);
				pathBuffer.insert(0, "/");
			}

			folder = folder.getMother();
		}

		return pathBuffer.toString();
	}

	/**
	 * Transform the given construct into the REST Model
	 * @param nodeConstruct construct
	 * @return REST Model of the construct
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Construct getConstruct(Construct nodeConstruct) throws NodeException {
		return Construct.TRANSFORM2REST.apply(nodeConstruct);
	}

	/**
	 * Transform the given part into the REST Model
	 * @param nodePart part
	 * @return REST Model of the part
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Part getPart(Part nodePart) throws NodeException {
		return Part.NODE2REST.apply(nodePart, new com.gentics.contentnode.rest.model.Part());
	}

	/**
	 * Transforms the given node REST model into the corresponding node object.
	 * TODO: Only existing nodes are supported as of yet and the node object is
	 * not yet updated from the rest object.
	 *
	 * @param restNode
	 *            the model to transform
	 * @return the transformed node object
	 * @throws NodeException
	 */
	public static Node getNode(com.gentics.contentnode.rest.model.Node restNode) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		return t.getObject(Node.class, restNode.getId());
	}

	/**
	 * Transform the given node into the REST Model
	 * @param nodeNode node
	 * @return REST Model of the node
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Node getNode(Node nodeNode) throws NodeException {
		return getNode(nodeNode, new com.gentics.contentnode.rest.model.Node());
	}

	/**
	 * Inflate the node REST model with ins node object.
	 * @param nodeNode node
	 * @param node REST model
	 * @return REST Model of the node
	 * @throws NodeException
	 */
	public static com.gentics.contentnode.rest.model.Node getNode(Node nodeNode, com.gentics.contentnode.rest.model.Node node) throws NodeException {
		node.setId(ObjectTransformer.getInteger(nodeNode.getId(), -1));
		node.setGlobalId(nodeNode.getGlobalId().toString());
		node.setName(nodeNode.getFolder().getName());
		node.setCdate(nodeNode.getCDate().getIntTimestamp());
		node.setCreator(getUser(nodeNode.getCreator()));
		node.setEdate(nodeNode.getEDate().getIntTimestamp());
		node.setEditor(getUser(nodeNode.getEditor()));
		node.setType(nodeNode.isChannel() ? ItemType.channel : ItemType.node);
		node.setMasterNodeId(nodeNode.getMaster().getId());
		node.setMasterName(nodeNode.getMaster().getName());

		// Determine from which node this node was inherited and set the found nodeId.
		if (nodeNode.isChannel()) {
			node.setInheritedFromId(nodeNode.getFolder().getChannelMaster().getNode().getId());
		} else {
			// Fallback to the node's id if the node is no channel. This is similar to the way the inheritedFromId field is handled for folders, files.
			node.setInheritedFromId(nodeNode.getId());
		}
		node.setFolderId(ObjectTransformer.getInt(nodeNode.getFolder().getId(), -1));
		node.setPublishDir(nodeNode.getPublishDir());
		node.setBinaryPublishDir(nodeNode.getBinaryPublishDir());
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.PUB_DIR_SEGMENT)) {
			node.setPubDirSegment(nodeNode.isPubDirSegment());
		}
		node.setHttps(nodeNode.isHttps());
		node.setHost(nodeNode.getHostname());
		node.setUtf8(nodeNode.isUtf8());
		node.setPublishFs(nodeNode.doPublishFilesystem());
		node.setPublishFsPages(nodeNode.doPublishFilesystemPages());
		node.setPublishFsFiles(nodeNode.doPublishFilesystemFiles());
		node.setPublishContentMap(nodeNode.doPublishContentmap());
		node.setPublishContentMapPages(nodeNode.doPublishContentMapPages());
		node.setPublishContentMapFiles(nodeNode.doPublishContentMapFiles());
		node.setPublishContentMapFolders(nodeNode.doPublishContentMapFolders());
		node.setContentRepositoryId(ObjectTransformer.getInteger(nodeNode.getContentrepositoryId(), null));
		node.setContentRepositoryName(getContentRepositoryName(nodeNode));
		node.setDisablePublish(nodeNode.isPublishDisabled());
		node.setEditorVersion(nodeNode.getEditorversion());
		node.setDefaultFileFolderId(getId(nodeNode.getDefaultFileFolder(), null));
		node.setDefaultImageFolderId(getId(nodeNode.getDefaultImageFolder(), null));
		node.setUrlRenderWayPages(nodeNode.getUrlRenderWayPages());
		node.setUrlRenderWayFiles(nodeNode.getUrlRenderWayFiles());
		node.setOmitPageExtension(nodeNode.isOmitPageExtension());
		node.setPageLanguageCode(nodeNode.getPageLanguageCode());

		if (NodeConfigRuntimeConfiguration.isFeature(Feature.MESH_CONTENTREPOSITORY)) {
			node.setMeshPreviewUrl(nodeNode.getMeshPreviewUrl());
			node.setInsecurePreviewUrl(nodeNode.isInsecurePreviewUrl());
			node.setMeshProject(nodeNode.getMeshProject());
			node.setPublishImageVariants(nodeNode.isPublishImageVariants());
		}

		List<ContentLanguage> languages = nodeNode.getLanguages();
		List<Integer> languagesId = new ArrayList<Integer>();

		for (ContentLanguage language : languages) {
			languagesId.add(language.getId());
		}

		node.setLanguagesId(languagesId);

		return node;
	}

	/**
	 * Get the name of the content repository of a give node
	 * @param node node object
	 * @return name of the content repository
	 */
	private static String getContentRepositoryName(Node node) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentRepository contentRepository = t.getObject(ContentRepository.class,
				node.getContentrepositoryId());

		if (contentRepository != null) {
			return contentRepository.getName();
		}

		return "";
	}

	/**
	 * Transform the given feature into the REST Model
	 * @param feature feature
	 * @return REST Model of the feature (might be null, if the feature cannot be activated for nodes)
	 * @throws NodeException
	 */
	public static NodeFeature getFeature(Feature feature) throws NodeException {
		if (feature == null) {
			return null;
		} else {
			return NodeFeature.valueOf(feature.toString().toLowerCase());
		}
	}

	/**
	 * Get the ID of the object or the defaultId, if object is null
	 * @param object object
	 * @param defaultId default id
	 * @return id of the object or default id
	 */
	protected static Integer getId(NodeObject object, Integer defaultId) {
		if (object != null) {
			return ObjectTransformer.getInteger(object.getId(), defaultId);
		} else {
			return defaultId;
		}
	}

	/**
	 * Get the master Node of the given node. This will return the node itself, if it is no channel
	 * @param node node
	 * @return master node or the node itself, never null
	 * @throws NodeException
	 */
	public static Node getMaster(Node node) throws NodeException {
		List<Node> masterNodes = node.getMasterNodes();

		if (masterNodes.size() > 0) {
			return masterNodes.get(masterNodes.size() - 1);
		} else {
			return node;
		}
	}

	/**
	 * Create a Template NodeObject from a Template REST model
	 *
	 * @param restTemplate
	 *            the Template REST model to create a NodeObject from
	 * @return a Template NodeObject created from the given Template REST model
	 * @throws NodeException
	 */
	public static Template getTemplate(com.gentics.contentnode.rest.model.Template restTemplate) throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();

		// If the given template is null, the corresponding gcn template should also be null
		if (restTemplate == null) {
			return null;
		}

		// try to load the template by id
		Template nodeTemplate = t.getObject(Template.class, restTemplate.getId(), true);

		// if the template could not be loaded (e.g. the id is not set), we create a new template
		if (nodeTemplate == null) {
			nodeTemplate = (Template) t.createObject(Template.class);
			// TODO do we need to link this template to a specific folder as well?
			if (restTemplate.getFolderId() != null) {
				nodeTemplate.setFolderId(restTemplate.getFolderId());
			}
		}

		// fill the meta data from the template into the gcn template object
		if (restTemplate.getDescription() != null) {
			nodeTemplate.setDescription(restTemplate.getDescription());
		}
		if (restTemplate.getName() != null) {
			nodeTemplate.setName(restTemplate.getName());
		}
		if (restTemplate.getSource() != null) {
			nodeTemplate.setSource(restTemplate.getSource());
		}
		if (restTemplate.getMarkupLanguage() != null && restTemplate.getMarkupLanguage().getId() != null) {
			nodeTemplate.setMlId(restTemplate.getMarkupLanguage().getId());
		}

		// TODO ignore? Check with nop
		// inherited
		// creator
		// editor
		// cdate
		// edate
		// locked
		// channelInfo

		// Get the template tags from the Node Object
		Map<String, TemplateTag> templateTags = nodeTemplate.getTemplateTags();

		// Get the template tags from the REST model
		Map<String, com.gentics.contentnode.rest.model.TemplateTag> templateRestTags = restTemplate.getTemplateTags();

		if (templateRestTags != null) {
			for (com.gentics.contentnode.rest.model.TemplateTag restTag : templateRestTags.values()) {
				String tagName = getTagNameOrThrow(restTag);
				TemplateTag templateTag = templateTags.get(tagName);

				if (templateTag == null) {

					// No corresponding tag was found in the node object, so create a new one
					templateTag = (TemplateTag) t.createObject(TemplateTag.class);
					templateTag.setName(restTag.getName());
					templateTag.setConstructId(restTag.getConstructId());

					// Handle the editable flag here as well
					if (restTag.getEditableInPage() != null) {
						templateTag.setPublic(restTag.getEditableInPage());
					}

					// Handle mandatory field
					if (restTag.getMandatory() != null) {
						templateTag.setMandatory(restTag.getMandatory());
					}

					// and add it to the other tags
					templateTags.put(restTag.getName(), templateTag);
				}
				Tag tag = templateTag;

				if (tag != null) {
					// now set the data from the REST tag
					fillRest2Node(restTag, tag);

					if (restTag.getEditableInPage() != null) {
						templateTag.setPublic(restTag.getEditableInPage());
					}
					if (restTag.getMandatory() != null) {
						templateTag.setMandatory(restTag.getMandatory());
					}
				}
			}
		}

		// Get the object tags from the Node Object
		Map<String, ObjectTag> objectTags = nodeTemplate.getObjectTags();

		// Get the template tags from the REST model
		Map<String, com.gentics.contentnode.rest.model.Tag> objectRestTags = restTemplate.getObjectTags();

		if (objectRestTags != null) {
			for (com.gentics.contentnode.rest.model.Tag restTag : objectRestTags.values()) {
				Tag tag = getOrCreateObjectTag(t, objectTags, restTag);
				fillRest2Node(restTag, tag);
			}
		}

		return nodeTemplate;
	}

	/**
	 * Helper method to get or create an ObjectTag
	 * @param t the current transaction
	 * @param objectTags map of ObjectTags to extract the ObjectTag
	 * @param restTag the rest version of the tag
	 * @return The sought tag
	 * @throws NodeException
	 */
	private static Tag getOrCreateObjectTag(Transaction t, Map<String, ObjectTag> objectTags, com.gentics.contentnode.rest.model.Tag restTag)
			throws NodeException {
		String tagName = getTagNameOrThrow(restTag);
		ObjectTag objectTag = objectTags.get(tagName);

		if (objectTag == null) {
			objectTag = objectTags.get(restTag.getName().replace("object.", ""));
		}

		if (objectTag == null) {
			objectTag = t.createObject(ObjectTag.class);
			objectTag.setName(tagName);
			objectTag.setConstructId(restTag.getConstructId());
		}

		return objectTag;
	}

	/**
	 * Helper to get or throw a tag Name
	 * @param restTag to get the name from
	 * @return The tag name
	 * @throws NodeException
	 */
	private static String getTagNameOrThrow(com.gentics.contentnode.rest.model.Tag restTag) throws NodeException {
		String name = restTag.getName();

		if (name == null) {
			logger.error("Could not handle tag since it did not contain a valid name.");
			throw new NodeException("A tag that was specified within the request did not have a name.");
		}
		return name;
	}

	/**
	 * Helper method to fill the data from the given restTag into the given tag
	 *
	 * @param restTag
	 *            rest tag holding data
	 * @param tag
	 *            NodeObject tag to fill data into
	 * @throws NodeException
	 */
	public static void fillRest2Node(com.gentics.contentnode.rest.model.Tag restTag, com.gentics.contentnode.object.Tag tag) throws NodeException {
		if (restTag.getConstructId() != null) {
			tag.setConstructId(restTag.getConstructId());
		}

		tag.setEnabled(ObjectTransformer.getBoolean(restTag.getActive(), true));
		Map<String, Property> properties = restTag.getProperties();

		if (properties != null) {
			for (Value value : tag.getValues()) {
				String keyName = value.getPart().getKeyname();
				Property property = properties.get(keyName);

				PartType partType = value.getPartType();
				if (property != null && Objects.equals(property.getType(), partType.getPropertyType())) {
					partType.fromProperty(property);
				}
			}
		}
	}

	public static void fillRestOverview2Node(Transaction t, Property property, Value value) throws NodeException {
		// if the value is a default value of a construct, we fill the template
		if (value.getContainer() instanceof Construct) {
			if (property.getStringValue() != null) {
				value.setValueText(property.getStringValue());
			}
			if (property.getBooleanValue() != null) {
				value.setInfo(property.getBooleanValue() ? 1 : 0);
			}
		}

		com.gentics.contentnode.rest.model.Overview restOverview = property.getOverview();
		if(restOverview == null) {
			// do nothing when the overview provided in the request is empty
			return;
		}
		OverviewPartType overviewPartType = (OverviewPartType) value.getPartType();
		com.gentics.contentnode.object.Overview overview = overviewPartType.getOverview();

		if (org.apache.commons.lang.StringUtils.isNotBlank(restOverview.getGlobalId())) {
			overview.setGlobalId(new GlobalId(restOverview.getGlobalId()));
		}

		// set selection type
		SelectType selectType = restOverview.getSelectType();

		if (selectType != null) {
			// TODO optionally check whether the selecttype may be changed
			switch (selectType) {
			case AUTO:
				overview.setSelectionType(com.gentics.contentnode.object.Overview.SELECTIONTYPE_PARENT);
				break;

			case FOLDER:
				overview.setSelectionType(com.gentics.contentnode.object.Overview.SELECTIONTYPE_FOLDER);
				break;

			case MANUAL:
				overview.setSelectionType(com.gentics.contentnode.object.Overview.SELECTIONTYPE_SINGLE);
				break;

			case UNDEFINED:
				break;
			}
		}

		// set the type of selected objects
		ListType listType = restOverview.getListType();

		if (listType != null) {
			// TODO optionally check whether the listtype may be changed
			switch (listType) {
			case FILE:
				overview.setObjectClass(File.class);
				break;

			case FOLDER:
				overview.setObjectClass(Folder.class);
				break;

			case IMAGE:
				overview.setObjectClass(ImageFile.class);
				break;

			case PAGE:
				overview.setObjectClass(Page.class);
				break;

			case UNDEFINED:
				break;
			}
		}

		// set the type of order
		OrderBy orderBy = restOverview.getOrderBy();

		if (orderBy != null) {
			switch (orderBy) {
			case ALPHABETICALLY:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_NAME);
				break;

			case CDATE:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_CDATE);
				break;

			case EDATE:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_EDATE);
				break;

			case FILESIZE:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_FILESIZE);
				break;

			case PDATE:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_PDATE);
				break;

			case PRIORITY:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_PRIORITY);
				break;

			case SELF:
				overview.setOrderKind(com.gentics.contentnode.object.Overview.ORDER_SELECT);
				break;

			case UNDEFINED:
				break;
			}
		}

		// set the order direction
		OrderDirection orderDirection = restOverview.getOrderDirection();

		if (orderDirection != null) {
			switch (orderDirection) {
			case ASC:
				overview.setOrderWay(com.gentics.contentnode.object.Overview.ORDERWAY_ASC);
				break;

			case DESC:
				overview.setOrderWay(com.gentics.contentnode.object.Overview.ORDERWAY_DESC);
				break;

			case UNDEFINED:
				break;
			}
		}

		// set the template's source
		String source = restOverview.getSource();

		if (source != null) {
			// TODO optionally check whether the template may be changed
			value.setValueText(source);
		}

		// set the entries
		fillRestOverviewEntries2Node(t, restOverview, overview);

		// set maximum number of listed entries
		Integer maxItems = restOverview.getMaxItems();

		if (maxItems != null) {
			overview.setMaxObjects(maxItems);
		}

		// set the recursive flag
		Boolean recursive = restOverview.isRecursive();

		if (recursive != null) {
			overview.setRecursion(recursive);
		}
	}

	private static void fillRestOverviewEntries2Node(Transaction t, com.gentics.contentnode.rest.model.Overview restOverview,
			com.gentics.contentnode.object.Overview overview) throws NodeException {
		if (overview.isStickyChannel()) {
			List<NodeIdObjectId> selectedItemIds = restOverview.getSelectedNodeItemIds();

			if (null == selectedItemIds) {
				selectedItemIds = Collections.emptyList();
			}

			// load items and replace IDs with master IDs
			Class<? extends NodeObject> entriesClass = overview.getEntriesClass();
			if (entriesClass != null && !selectedItemIds.isEmpty()) {
				try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
					// load all selected objects (to get them into the cache)
					t.getObjects(entriesClass, Flowable.fromIterable(selectedItemIds).map(NodeIdObjectId::getObjectId).toList().blockingGet());

					selectedItemIds = Flowable.fromIterable(selectedItemIds).map(entry -> {
						int objectId = entry.getObjectId();
						NodeObject o = t.getObject(entriesClass, objectId);
						if (o instanceof LocalizableNodeObject<?>) {
							objectId = ((LocalizableNodeObject<?>) o).getMaster().getId();
						}
						return new NodeIdObjectId(entry.getNodeId(), objectId);
					}).toList().blockingGet();
				}
			}

			List<NodeIdObjectId> newItemIds = new ArrayList<>(selectedItemIds);

			// get the current overview entries
			List<OverviewEntry> entries = overview.getOverviewEntries();

			// this will map the nodeId/objectId to its OverviewEntry
			Map<NodeIdObjectId, OverviewEntry> entryMap = new HashMap<>(selectedItemIds.size());

			// remove all entries that no longer exist, collect all new object ids
			for (Iterator<OverviewEntry> i = entries.iterator(); i.hasNext();) {
				OverviewEntry entry = i.next();
				NodeIdObjectId existing = new NodeIdObjectId(entry.getNodeId(), entry.getObjectId());

				if (!selectedItemIds.contains(existing)) {
					// the object id of the entry is no longer found in
					// the list of selected items, so remove it
					i.remove();
				} else {
					// the object id of the entry is still selected, but
					// it is not new (because we already have an entry
					// for it).
					newItemIds.remove(existing);

					// add to entries map
					entryMap.put(existing, entry);
				}
			}

			// for the new objects, create entries
			for (NodeIdObjectId newId : newItemIds) {
				OverviewEntry newEntry = t.createObject(OverviewEntry.class);

				newEntry.setNodeId(newId.getNodeId());
				newEntry.setObjectId(newId.getObjectId());
				entries.add(newEntry);

				// also add the entry to the entries map
				entryMap.put(newId, newEntry);
			}

			// next we will give every entry its correct order number (starting with 1)
			int orderNumber = 1;

			for (NodeIdObjectId itemId : selectedItemIds) {
				OverviewEntry entry = entryMap.get(itemId);

				if (entry != null) {
					entry.setObjectOrder(orderNumber++);
				} else {
					// this must not happen
					throw new NodeException("Error while saving overview: Could not find overview entry for item " + itemId);
				}
			}

			// now we sort the list of entries
			Collections.sort(entries, new Comparator<OverviewEntry>() {
				public int compare(OverviewEntry o1, OverviewEntry o2) {
					return o1.getObjectOrder() - o2.getObjectOrder();
				}
			});
		} else {
			// get the new selected items
			List<Integer> selectedItemIds = restOverview.getSelectedItemIds();

			if (null == selectedItemIds) {
				selectedItemIds = Collections.emptyList();
			}

			// load items and replace IDs with master IDs
			Class<? extends NodeObject> entriesClass = overview.getEntriesClass();
			if (entriesClass != null && !selectedItemIds.isEmpty()) {
				try (WastebinFilter wbf = Wastebin.INCLUDE.set()) {
					List<? extends NodeObject> selectedObjects = t.getObjects(entriesClass, selectedItemIds);
					selectedItemIds = Flowable.fromIterable(selectedObjects).map(o -> {
						if (o instanceof LocalizableNodeObject) {
							return ((LocalizableNodeObject<?>) o).getMaster();
						} else {
							return o;
						}
					}).map(NodeObject::getId).toList().blockingGet();
				}
			}

			// this list will contain selected items, that were not selected before
			List<Integer> newItemIds = new Vector<Integer>(selectedItemIds);

			// get the current overview entries
			List<OverviewEntry> entries = overview.getOverviewEntries();

			// this will map the objectId to its OverviewEntry
			Map<Object, OverviewEntry> entryMap = new HashMap<Object, OverviewEntry>(selectedItemIds.size());

			// remove all entries that no longer exist, collect all new object ids
			for (Iterator<OverviewEntry> i = entries.iterator(); i.hasNext();) {
				OverviewEntry entry = i.next();

				if (!selectedItemIds.contains(entry.getObjectId())) {
					// the object id of the entry is no longer found in
					// the list of selected items, so remove it
					i.remove();
				} else {
					// the object id of the entry is still selected, but
					// it is not new (because we already have an entry
					// for it).
					newItemIds.remove(entry.getObjectId());

					// add to entries map
					entryMap.put(entry.getObjectId(), entry);
				}
			}

			// for the new objects, create entries
			for (Integer newId : newItemIds) {
				OverviewEntry newEntry = t.createObject(OverviewEntry.class);

				newEntry.setObjectId(newId);
				entries.add(newEntry);

				// also add the entry to the entries map
				entryMap.put(newId, newEntry);
			}

			// next we will give every entry its correct order number (starting with 1)
			int orderNumber = 1;

			for (Integer itemId : selectedItemIds) {
				OverviewEntry entry = entryMap.get(itemId);

				if (entry != null) {
					entry.setObjectOrder(orderNumber++);
				} else {
					// this must not happen
					throw new NodeException("Error while saving overview: Could not find overview entry for item " + itemId);
				}
			}

			// now we sort the list of entries
			Collections.sort(entries, new Comparator<OverviewEntry>() {
				public int compare(OverviewEntry o1, OverviewEntry o2) {
					return o1.getObjectOrder() - o2.getObjectOrder();
				}
			});
		}
	}

	/**
	 * Get the GCN File corresponding to the given REST File
	 *
	 * @param restFile
	 *            File object
	 * @return GCN File object
	 */
	public static File getFile(com.gentics.contentnode.rest.model.File restFile) throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();

		// try to load the file
		File file = t.getObject(File.class, restFile.getId(), true);

		// fill in the data
		if (restFile.getName() != null) {
			file.setName(restFile.getName());
		}
		if (restFile.getDescription() != null) {
			file.setDescription(restFile.getDescription());
		}
		if (restFile.getFileType() != null) {
			file.setFiletype(restFile.getFileType());
		}
		if (restFile.isForceOnline() != null) {
			file.setForceOnline(restFile.isForceOnline());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (restFile.getNiceUrl() != null) {
				file.setNiceUrl(restFile.getNiceUrl());
			}
			if (restFile.getAlternateUrls() != null) {
				file.setAlternateUrls(restFile.getAlternateUrls());
			}
		}

		// set the tags
		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restFile.getTags();

		if (restTags != null) {
			// get the objecttags
			Map<String, ObjectTag> objectTags = file.getObjectTags();

			for (Iterator<com.gentics.contentnode.rest.model.Tag> i = restTags.values().iterator(); i.hasNext();) {
				com.gentics.contentnode.rest.model.Tag restTag = i.next();
				Tag tag = null;

				if (restTag.getName().startsWith("object.")) {
					String realName = restTag.getName().substring(7);
					ObjectTag oTag = objectTags.get(realName);

					if (oTag == null) {
						// did not find the tag, so create a new one
						oTag = (ObjectTag) t.createObject(ObjectTag.class);
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
		}

		return file;
	}

	/**
	 * Get the GCN Image corresponding to the given REST Image
	 *
	 * @param restImage
	 *            REST Image object
	 * @return GCN Image object
	 */
	public static ImageFile getImage(Image restImage) throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();

		// try to load the file
		ImageFile image = t.getObject(ImageFile.class, restImage.getId(), true);

		// fill in the data
		if (restImage.getName() != null && !StringUtils.isEmpty(restImage.getName())) {
			image.setName(restImage.getName());
		}
		if (restImage.getDescription() != null) {
			image.setDescription(restImage.getDescription());
		}
		if (restImage.getFileType() != null && !StringUtils.isEmpty(restImage.getFileType())) {
			image.setFiletype(restImage.getFileType());
		}
		if (restImage.isForceOnline() != null) {
			image.setForceOnline(restImage.isForceOnline());
		}
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.NICE_URLS)) {
			if (restImage.getNiceUrl() != null) {
				image.setNiceUrl(restImage.getNiceUrl());
			}
			if (restImage.getAlternateUrls() != null) {
				image.setAlternateUrls(restImage.getAlternateUrls());
			}
		}

		// Update focal point information
		if (restImage.getFpX() != null) {
			image.setFpX(restImage.getFpX());
		}
		if (restImage.getFpY() != null) {
			image.setFpY(restImage.getFpY());
		}

		// set the tags
		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restImage.getTags();

		if (restTags != null) {
			// get the objecttags
			Map<String, ObjectTag> objectTags = image.getObjectTags();

			for (Iterator<com.gentics.contentnode.rest.model.Tag> i = restTags.values().iterator(); i.hasNext();) {
				com.gentics.contentnode.rest.model.Tag restTag = i.next();
				Tag tag = null;

				if (restTag.getName().startsWith("object.")) {
					String realName = restTag.getName().substring(7);
					ObjectTag oTag = objectTags.get(realName);

					if (oTag == null) {
						// did not find the tag, so create a new one
						oTag = (ObjectTag) t.createObject(ObjectTag.class);
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
		}

		return image;
	}

	/**
	 * Recursively replace attributes in the given node
	 * @param node node to start with
	 * @param matcher matcher for the attribute names to replace. If the matcher returns null, the attribute is not replaced. If the matcher returns a string, the attribute is replace with another attribute with the given name
	 * @param replacer replacer function, which will get the attribute value as JsonNode and should return it as another JsonNode
	 * @throws NodeException
	 */
	public static void recursivelyReplaceAttributes(JsonNode node, Function<String, String> matcher,
			BiFunction<ObjectNode, JsonNode, JsonNode> replacer) throws NodeException {
		if (node.isObject()) {
			ObjectNode objectNode = (ObjectNode) node;
			Map<String, JsonNode> replace = new HashMap<>();
			for (Iterator<String> i = objectNode.fieldNames(); i.hasNext();) {
				String name = i.next();
				matcher.apply(name);
				if (matcher.apply(name) != null) {
					replace.put(name, replacer.apply(objectNode, objectNode.path(name)));
				} else {
					// recursion
					recursivelyReplaceAttributes(objectNode.get(name), matcher, replacer);
				}
			}

			// replace
			for (Map.Entry<String, JsonNode> entry : replace.entrySet()) {
				String oldName = entry.getKey();
				JsonNode replacement = entry.getValue();
				String newName = matcher.apply(oldName);

				objectNode.remove(oldName);
				objectNode.set(newName, replacement);
			}
		} else if (node.isArray()) {
			for (JsonNode sub : node) {
				recursivelyReplaceAttributes(sub, matcher, replacer);
			}
		}
	}

	/**
	 * Get the GCN Folder corresponding to the given REST Folder
	 *
	 * @param restFolder
	 *            REST Folder object
	 * @return GCN Folder object
	 */
	public static Folder getFolder(com.gentics.contentnode.rest.model.Folder restFolder) throws NodeException {

		Transaction t = TransactionManager.getCurrentTransaction();

		Folder folder = null;

		folder = (com.gentics.contentnode.object.Folder) t.getObject(com.gentics.contentnode.object.Folder.class, restFolder.getId(), true);

		if (!ObjectTransformer.isEmpty(restFolder.getName())) {
			folder.setName(restFolder.getName());
		}
		if (restFolder.getDescription() != null) {
			folder.setDescription(restFolder.getDescription());
		}
		if (!ObjectTransformer.isEmpty(restFolder.getPublishDir())) {
			folder.setPublishDir(restFolder.getPublishDir());
		}

		// transform the i18n data
		if (restFolder.getNameI18n() != null) {
			folder.setNameI18n(I18nMap.TRANSFORM2NODE.apply(restFolder.getNameI18n()));
		}
		if (restFolder.getDescriptionI18n() != null) {
			folder.setDescriptionI18n(I18nMap.TRANSFORM2NODE.apply(restFolder.getDescriptionI18n()));
		}
		if (restFolder.getPublishDirI18n() != null) {
			folder.setPublishDirI18n(I18nMap.TRANSFORM2NODE.apply(restFolder.getPublishDirI18n()));
		}

		Map<String, com.gentics.contentnode.rest.model.Tag> restTags = restFolder.getTags();

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
						oTag = (ObjectTag) t.createObject(ObjectTag.class);
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
		}

		return folder;
	}

	/**
	 * Adds the messages from the {@link OpResult} to the {@link GenericResponse} object
	 *
	 * @param result
	 * @return
	 */
	public static void addMessagesFromOpResult(OpResult result, GenericResponse response) {
		for (NodeMessage message : result.getMessages()) {
			response.addMessage(new com.gentics.contentnode.rest.model.response.Message(com.gentics.contentnode.rest.model.response.Message.Type.CRITICAL, message.getMessage()));
		}
	}

	/**
	 * Get the delete info for the given object
	 * @param object object
	 * @return delete info or null if object was not deleted
	 * @throws NodeException
	 */
	public static DeleteInfo getDeleteInfo(NodeObject object) throws NodeException {
		if (object.isDeleted()) {
			return new DeleteInfo(object.getDeleted(), getUser(object.getDeletedBy()));
		} else if (NodeConfigRuntimeConfiguration.isFeature(Feature.WASTEBIN)) {
			return new DeleteInfo();
		} else {
			return null;
		}
	}

	/**
	 * Get the privileges of the current user on the given object as PrivilegeMap
	 * @param objType object type
	 * @param objId object id
	 * @return privilege map
	 * @throws NodeException
	 */
	public static PrivilegeMap getPrivileges(int objType, int objId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PrivilegeMap map = new PrivilegeMap();
		Map<Privilege, Boolean> general = new TreeMap<>();
		map.setPrivileges(general);
		switch (objType) {
		case Node.TYPE_NODE:
		case Folder.TYPE_FOLDER:
			// get the folder permissions
			Folder folder = t.getObject(Folder.class, objId);
			if (folder == null) {
				return null;
			}

			PermissionPair generalPage = t.getPermHandler().getPermissions(objType, objId, Page.TYPE_PAGE, 0);
			PermissionPair generalFile = t.getPermHandler().getPermissions(objType, objId, File.TYPE_FILE, 0);
			Set<Privilege> available = new HashSet<>(Privilege.getAvailable(Folder.TYPE_FOLDER));
			for (Privilege p : available) {
				if (p.getPermBit() < 0) {
					continue;
				}
				switch (p.getRoleCheckType()) {
				case Page.TYPE_PAGE:
					general.put(p, generalPage.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
					break;
				case File.TYPE_FILE:
					general.put(p, generalFile.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
					break;
				default:
					general.put(p, generalPage.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
					break;
				}
			}

			// get the node permissions
			Folder rootFolder = folder.getNode().getFolder();
			generalPage = t.getPermHandler().getPermissions(objType, rootFolder.getId(), Page.TYPE_PAGE, 0);
			generalFile = t.getPermHandler().getPermissions(objType, rootFolder.getId(), File.TYPE_FILE, 0);
			for (Privilege p : Privilege.getAvailable(Node.TYPE_NODE)) {
				if (p.getPermBit() < 0) {
					continue;
				}
				switch (p.getRoleCheckType()) {
				case Page.TYPE_PAGE:
					general.put(p, generalPage.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
					break;
				case File.TYPE_FILE:
					general.put(p, generalFile.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
					break;
				default:
					general.put(p, generalPage.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
					break;
				}
			}

			// get all language specific permissions
			List<ContentLanguage> langs = folder.getNode().getLanguages();
			if (!ObjectTransformer.isEmpty(langs)) {
				List<LanguagePrivileges> langPrivs = new ArrayList<>();
				map.setLanguages(langPrivs);
				for (ContentLanguage lang : langs) {
					LanguagePrivileges langPriv = new LanguagePrivileges();
					langPrivs.add(langPriv);
					langPriv.setLanguage(ContentLanguage.TRANSFORM2REST.apply(lang));
					Map<Privilege, Boolean> langMap = new TreeMap<>();
					langPriv.setPrivileges(langMap);

					PermissionPair langPage = t.getPermHandler().getPermissions(objType, objId, Page.TYPE_PAGE, lang.getId());
					PermissionPair langFile = t.getPermHandler().getPermissions(objType, objId, File.TYPE_FILE, lang.getId());

					for (Privilege p : available) {
						if (p.getRoleCheckType() < 0) {
							continue;
						}
						switch (p.getRoleCheckType()) {
						case Page.TYPE_PAGE:
							langMap.put(p, langPage.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
							break;
						case File.TYPE_FILE:
							langMap.put(p, langFile.checkPermissionBits(p.getPermBit(), p.getRoleBit()));
							break;
						default:
							break;
						}
					}
				}
			}

			break;
		}
		return map;
	}

	/**
	 * Transform the tool data into CustomTool
	 * @param toolData tool data
	 * @return CustomTool
	 */
	@SuppressWarnings("unchecked")
	public static CustomTool getCustomTool(Map<?, ?> toolData) {
		CustomTool custTool = new CustomTool();
		custTool.setId(ObjectTransformer.getInt(toolData.get("id"), -1));
		custTool.setKey(ObjectTransformer.getString(toolData.get("key"), null));

		if (toolData.get("name") instanceof Map) {
			custTool.setName((Map<String, String>)toolData.get("name"));
		} else {
			Map<String, String> nameMap = new HashMap<>();
			nameMap.put("de", ObjectTransformer.getString(toolData.get("name"), null));
			nameMap.put("en", ObjectTransformer.getString(toolData.get("name"), null));
			custTool.setName(nameMap);
		}
		String toolUrl = ObjectTransformer.getString(toolData.get("toolUrl"), null);
		// replace ${SID} placeholder
		if (!ObjectTransformer.isEmpty(toolUrl) && toolUrl.contains("${SID}")) {
			try {
				Transaction t = TransactionManager.getCurrentTransaction();
				Session session = t.getSession();
				if (session != null) {
					toolUrl = toolUrl.replaceAll("\\$\\{SID\\}", String.valueOf(session.getSessionId()));
				} else {
					toolUrl = toolUrl.replaceAll("\\$\\{SID\\}", "");
				}
			} catch (TransactionException e) {
			}
		}
		custTool.setToolUrl(toolUrl);
		custTool.setIconUrl(ObjectTransformer.getString(toolData.get("iconUrl"), null));
		custTool.setNewtab(ObjectTransformer.getBoolean(toolData.get("newtab"), false));

		return custTool;
	}

	/**
	 * Transform a regex into its model
	 * @param regex regex
	 * @return model
	 */
	public static RegexModel getRegex(Regex regex) {
		RegexModel model = new RegexModel();
		model.setId(regex.getId());
		model.setName(regex.getName().toString());
		model.setDescription(regex.getDescription().toString());
		model.setExpression(regex.getExpression());
		return model;
	}

	/**
	 * Get a list of breadcrumb items for the folder. The first element will be the root folder of the Node/Channel and the last item will be the folder itself.
	 * @param folder folder
	 * @return list of breadcrumb items
	 * @throws NodeException
	 */
	public static List<BreadcrumbItem> getBreadcrumbs(Folder folder) throws NodeException {
		List<BreadcrumbItem> breadcrumb = new ArrayList<>();
		Folder current = folder;
		while (current != null) {
			breadcrumb.add(new BreadcrumbItem().setId(current.getId()).setGlobalId(current.getGlobalId().toString()).setName(current.getName()));
			current = current.getMother();
		}

		Collections.reverse(breadcrumb);
		return breadcrumb;
	}

	/**
	 * Get the sort order map for the object tags
	 * @param objectTags collection of object tags
	 * @return map for name -> sortindex
	 */
	public static Map<String, Integer> getSortOrderMap(Collection<ObjectTag> objectTags) {
		List<ObjectTag> sorted = new ArrayList<>(objectTags);

		Collections.sort(sorted, (t1, t2) -> {
			try {
				ObjectTagDefinitionCategory cat1 = t1.getCategory();
				ObjectTagDefinitionCategory cat2 = t2.getCategory();
				if (cat1 == null && cat2 == null) {
					return StringUtils.mysqlLikeCompare(t1.getDisplayName(), t2.getDisplayName());
				} else if (cat1 == null && cat2 != null) {
					return 1;
				} else if (cat1 != null && cat2 == null) {
					return -1;
				} else {
					if (cat1.getSortorder() == cat2.getSortorder()) {
						return StringUtils.mysqlLikeCompare(t1.getDisplayName(), t2.getDisplayName());
					} else {
						return cat1.getSortorder() - cat2.getSortorder();
					}
				}
			} catch (NodeException e) {
				return 0;
			}
		});

		Map<String, Integer> sortOrderMap = new HashMap<>();
		AtomicInteger count = new AtomicInteger();
		for (ObjectTag tag : sorted) {
			sortOrderMap.put(tag.getName(), count.getAndIncrement());
		}

		return sortOrderMap;
	}
			}
