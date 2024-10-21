package com.gentics.contentnode.rest.util;

import static com.gentics.contentnode.rest.util.MiscUtils.collectLinkedObjects;
import static com.gentics.contentnode.rest.util.MiscUtils.convertToRest;
import static com.gentics.contentnode.rest.util.MiscUtils.filterByObstructions;
import static com.gentics.contentnode.rest.util.MiscUtils.filterByPermission;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.response.ChannelLocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationInfoItem;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse.SyncDependencies;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

public class MCMiscUtils {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCMiscUtils.class);

	/**
	 * Get objects which are directly or indirectly linked by the specified objects,
	 * which can be synchronised to a master channel along with the objects themselves.
	 *
	 * Linked objects are pages, files and images specified in <code>UrlPartTypes</code> of
	 * content or object tags.
	 *
	 * Direct dependencies are in the {@link ReferencedSyncableObjectsListResponse#getPages() pages},
	 * {@link ReferencedSyncableObjectsListResponse#getFiles() files} and
	 * {@link ReferencedSyncableObjectsListResponse#getImages() images} files of the response. Indirect
	 * dependencies are mapped in {@link ReferencedSyncableObjectsListResponse#getDependencies dependencies}.
	 *
	 * @param objClass The class of the objects to be synchronised
	 * @param ids The IDs of the folders to get linked objects for
	 * @param srcNodeId The channel ID of the source channel
	 * @param dstNodeId The channel ID of the target channel
	 */
	public static <T extends NodeObject> ReferencedSyncableObjectsListResponse getSyncableObjects(
			Class<T> objClass,
			List<Integer> ids,
			Integer srcNodeId,
			Integer dstNodeId) throws NodeException {
		Set<Page> pages = new HashSet<>();
		Set<File> files = new HashSet<>();
		Set<ImageFile> images = new HashSet<>();

		try (ChannelTrx trx = new ChannelTrx(srcNodeId)) {
			collectLinkedObjects(objClass, ids, pages, files, images);

			int pagesWithoutPermission = filterByPermission(pages);
			int filesWithoutPermission = filterByPermission(files);
			int imagesWithoutPermission = filterByPermission(images);

			filterByObstructions(srcNodeId, dstNodeId, pages, Page.class);
			filterByObstructions(srcNodeId, dstNodeId, files, ContentFile.class);
			filterByObstructions(srcNodeId, dstNodeId, images, ContentFile.class);

			// TODO: Is sorting needed?

			SyncDependencies deps;
			HashMap<ItemType, SyncDependencies> dependencies = new HashMap<>();
			Set<NodeObject> srcObjects = new HashSet<>();

			srcObjects.addAll(pages);
			deps = getDependencies(objClass, ids, srcNodeId, dstNodeId, srcObjects);
			if (!ObjectTransformer.isEmpty(deps)) {
				dependencies.put(ItemType.page, deps);
			}

			srcObjects.clear();
			srcObjects.addAll(files);
			deps = getDependencies(objClass, ids, srcNodeId, dstNodeId, srcObjects);
			if (!ObjectTransformer.isEmpty(deps)) {
				dependencies.put(ItemType.file, deps);
			}

			srcObjects.clear();
			srcObjects.addAll(images);
			deps = getDependencies(objClass, ids, srcNodeId, dstNodeId, srcObjects);
			if (!ObjectTransformer.isEmpty(deps)) {
				dependencies.put(ItemType.image, deps);
			}

			return new ReferencedSyncableObjectsListResponse(
				dependencies,
				convertToRest(pages, com.gentics.contentnode.rest.model.Page.class),
				pagesWithoutPermission,
				convertToRest(files, com.gentics.contentnode.rest.model.File.class),
				filesWithoutPermission,
				convertToRest(images, com.gentics.contentnode.rest.model.Image.class),
				imagesWithoutPermission);
		}
	}

	/**
	 * Get depenencies of the <code>srcObjects</code>
	 *
	 * Note: The <code>movedObjectIds</code>, <code>pages</code>, <code>files</code> and
	 * <code>images</code> are only needed to prune the search and detect cyclic dependencies.
	 *
	 * @param objClass The class of the objects specified in <code>srcObjects</code>
	 * @param movedObjectIds The IDs of the objects which are to be synchronised
	 * @param srcNodeId The ID of the source channel
	 * @param dstNodeId The ID of the target channel
	 * @param srcObjects The objects to gather dependencies for
	 *
	 * @return The dependencies for the specified objects
	 *
	 * @throws NodeException On errors
	 */
	public static <T extends NodeObject> SyncDependencies getDependencies(
			Class<T> objClass,
			Collection<Integer> movedObjectIds,
			Integer srcNodeId,
			Integer dstNodeId,
			Collection<NodeObject> srcObjects) throws NodeException {
		SyncDependencies ret = new SyncDependencies();
		Collector<LocalizableNodeObject<?>, ?, Map<Integer, Integer>> toMap = Collectors.toMap(
			o -> o.getId(),
			o -> {
				try {
					return o.getChannel().getId();
				} catch (NodeException e) {
				}

				return -1;
			});

		for (NodeObject obj : srcObjects) {
			Integer id = obj.getId();

			Set<Integer> remainingPageIds = new HashSet<>();
			Set<Integer> remainingFileIds = new HashSet<>();
			Set<Integer> remainingImageIds = new HashSet<>();
			Set<Page> pageDeps = new HashSet<>();
			Set<Page> newPages = new HashSet<>();
			Set<File> fileDeps = new HashSet<>();
			Set<File> newFiles = new HashSet<>();
			Set<ImageFile> imageDeps = new HashSet<>();
			Set<ImageFile> newImages = new HashSet<>();

			if (obj instanceof Page) {
				remainingPageIds.add(obj.getId());
			} else if (obj instanceof File) {
				remainingFileIds.add(obj.getId());
			} else if (obj instanceof ImageFile) {
				remainingImageIds.add(obj.getId());
			}

			do {
				pageDeps.addAll(newPages);
				newPages.clear();
				fileDeps.addAll(newFiles);
				newFiles.clear();
				imageDeps.addAll(newImages);
				newImages.clear();

				if (logger.isDebugEnabled()) {
					int remaining = remainingPageIds.size() + remainingFileIds.size() + remainingImageIds.size();

					logger.debug("Collecting dependencies for " + remaining + " object" + (remaining == 1 ? "" : "s"));
				}

				collectLinkedObjects(Page.class, remainingPageIds, newPages, newFiles, newImages);
				collectLinkedObjects(File.class, remainingFileIds, newPages, newFiles, newImages);
				collectLinkedObjects(ImageFile.class, remainingImageIds, newPages, newFiles, newImages);

				filterByPermission(newPages);
				filterByPermission(newFiles);
				filterByPermission(newImages);
				filterByObstructions(srcNodeId, dstNodeId, newPages, Page.class);
				filterByObstructions(srcNodeId, dstNodeId, newFiles, ContentFile.class);
				filterByObstructions(srcNodeId, dstNodeId, newImages, ContentFile.class);

				remainingPageIds = newPages.stream()
					.filter(p -> !pageDeps.contains(p))
					.map(Page::getId)
					.collect(Collectors.toSet());
				remainingFileIds = newFiles.stream()
					.filter(f -> !fileDeps.contains(f))
					.map(File::getId)
					.collect(Collectors.toSet());
				remainingImageIds = newImages.stream()
					.filter(i -> !imageDeps.contains(i))
					.map(ImageFile::getId)
					.collect(Collectors.toSet());

				if (Page.class.isAssignableFrom(objClass)) {
					remainingPageIds.removeAll(movedObjectIds);
					remainingPageIds.remove(obj.getId());
				} else if (File.class.isAssignableFrom(objClass)) {
					remainingFileIds.removeAll(movedObjectIds);
					remainingFileIds.remove(obj.getId());
				} else if (ImageFile.class.isAssignableFrom(objClass)) {
					remainingImageIds.removeAll(movedObjectIds);
					remainingImageIds.remove(obj.getId());
				}
			} while (!remainingPageIds.isEmpty() || !remainingFileIds.isEmpty() || !remainingImageIds.isEmpty());

			Map<ItemType, Map<Integer, Integer>> dependencies = new HashMap<>();

			if (!pageDeps.isEmpty()) {
				dependencies.put(ItemType.page, pageDeps.stream().collect(toMap));
			}

			if (!fileDeps.isEmpty()) {
				dependencies.put(ItemType.file, fileDeps.stream().collect(toMap));
			}

			if (!imageDeps.isEmpty()) {
				dependencies.put(ItemType.image, imageDeps.stream().collect(toMap));
			}

			if (logger.isDebugEnabled()) {
				logger.debug(
						"Found dependencies for " + obj + ": "
								+ pageDeps.size() + " page" + (pageDeps.size() == 1 ? "" : "s") + " "
								+ fileDeps.size() + " file" + (fileDeps.size() == 1 ? "" : "s") + " "
								+ imageDeps.size() + " image" + (imageDeps.size() == 1 ? "" : "s"));
			}

			if (!ObjectTransformer.isEmpty(dependencies)) {
				ret.put(id, dependencies);
			}
		}

		return ret;
	}

	/**
	 * Get the localization info for the given list of objects
	 * @param channelIds list of channel ids
	 * @param objects list of objects
	 * @return localization info for the objects
	 * @throws NodeException
	 */
	public static List<ChannelLocalizationInfo> getLocalizationInfo(
			List<Integer> channelIds,
			List<LocalizableNodeObject<? extends NodeObject>> objects) throws NodeException {
		List<ChannelLocalizationInfo> channels = new Vector<ChannelLocalizationInfo>(channelIds.size());

		for (LocalizableNodeObject<? extends NodeObject> object : objects) {
			List<ChannelLocalizationInfo> objectInfo = getLocalizationInfo(channelIds, object);

			if (channels.isEmpty()) {
				channels.addAll(objectInfo);
			} else {
				for (int i = 0; i < channels.size(); ++i) {
					channels.get(i).mergeWith(objectInfo.get(i));
				}
			}
		}

		return channels;
	}

	/**
	 * Get the localization info for the given object
	 * @param channelIds list of channel ids
	 * @param object object
	 * @return localization info for the object
	 * @throws NodeException
	 */
	public static List<ChannelLocalizationInfo> getLocalizationInfo(
			List<Integer> channelIds, LocalizableNodeObject<? extends NodeObject> object) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ChannelLocalizationInfo> channels = new Vector<ChannelLocalizationInfo>(channelIds.size());
		Map<Integer, Integer> channelSet = object.getChannelSet();
		Node channel = object.getChannel();

		ChannelLocalizationInfo lastChannel = null;

		for (Integer channelId : channelIds) {
			ChannelLocalizationInfo currentChannel = new ChannelLocalizationInfo();

			currentChannel.setChannelId(channelId);
			LocalizationInfoItem infoItem = new LocalizationInfoItem();

			if (channelSet.containsKey(channelId)) {
				// there is a local(ized) object
				@SuppressWarnings("unchecked")
				LocalizableNodeObject<? extends NodeObject> localObject = (LocalizableNodeObject<? extends NodeObject>) t.getObject(
						object.getObject().getObjectInfo().getObjectClass(), channelSet.get(channelId));

				if (localObject.isMaster()) {
					infoItem.setLocal(1);
				} else {
					infoItem.setLocalized(1);
				}
			} else {
				if (channel != null && channel.getId().equals(channelId)) {
					infoItem.setLocal(1);
				} else if (channel == null && channelId == 0) {
					infoItem.setLocal(1);
				}
				if (lastChannel == null) {
					infoItem.setInherited(0);
				} else {
					infoItem.setInherited(lastChannel.getLocal() + lastChannel.getLocalized() + lastChannel.getInherited());
				}
			}

			if (object instanceof Page) {
				currentChannel.setPage(infoItem);
			} else if (object instanceof ImageFile) {
				currentChannel.setImage(infoItem);
			} else if (object instanceof File) {
				currentChannel.setFile(infoItem);
			} else if (object instanceof Template) {
				currentChannel.setTemplate(infoItem);
			} else if (object instanceof Folder) {
				currentChannel.setFolder(infoItem);
			}
			channels.add(currentChannel);
			lastChannel = currentChannel;
		}

		return channels;
	}

	/**
	 * Get the localization info for the given channel ids and folder ids
	 * @param channelIds list of channel ids
	 * @param folderIds list of folder ids, for which the info is collected
	 * @param inFolderIds list of folder ids, from which pages, images, files and templates are fetched
	 * @param languages list of languages
	 * @return list of localizationinfo data per channel
	 * @throws NodeException
	 */
	public static List<ChannelLocalizationInfo> getFolderLocalizationInfo(
			List<Integer> channelIds, List<Integer> folderIds, List<Integer> inFolderIds, List<ContentLanguage> languages) throws NodeException {
		// get localization info for folders
		Map<Integer, LocalizationInfoItem> folderItems = getLocalizationInfo(channelIds, folderIds, "folder", null, "id", "AND mother != 0");

		// we need to count the pages differently, because the permissions to view might depend on the language
		// first we will get the localization info for pages in folders with general permission
		List<Integer> folderIdsWithGeneralPageView = foldersWithPermBits(inFolderIds, Page.class, null, PermHandler.ObjectPermission.view);
		// get the localizationinfo for pages in those folders
		Map<Integer, LocalizationInfoItem> pageItems = getLocalizationInfo(channelIds, folderIdsWithGeneralPageView, "page", null, "folder_id", null);

		// check which folder ids are in the initial list, but don't have general permission
		List<Integer> remainingFolderIds = new Vector<Integer>(inFolderIds);

		remainingFolderIds.removeAll(folderIdsWithGeneralPageView);

		// then we will check for folders with role permissions on specific languages (that are not listed in the general folder list)
		// and add the localization info for pages in the specific languages
		if (remainingFolderIds.size() > 0) {
			// there are remaining folders, so we need to check language specific permission in them
			for (ContentLanguage lang : languages) {
				List<Integer> foldersWithLangPerm = foldersWithPermBits(remainingFolderIds, Page.class, lang, PermHandler.ObjectPermission.view);

				if (foldersWithLangPerm.size() > 0) {
					// restrict language
					Map<Integer, LocalizationInfoItem> langPageItems = getLocalizationInfo(channelIds, foldersWithLangPerm, "page", null, "folder_id",
							"AND contentgroup_id = " + lang.getId());

					// merge into pageItems
					for (Map.Entry<Integer, LocalizationInfoItem> entry : langPageItems.entrySet()) {
						pageItems.get(entry.getKey()).mergeWith(entry.getValue());
					}
				}
			}
		}

		// get localization info for images
		Map<Integer, LocalizationInfoItem> imageItems = getLocalizationInfo(channelIds,
				foldersWithPermBits(inFolderIds, ImageFile.class, null, PermHandler.ObjectPermission.view), "contentfile", null, "folder_id",
				"AND filetype LIKE 'image/%'");

		// get localization info for files
		Map<Integer, LocalizationInfoItem> fileItems = getLocalizationInfo(channelIds,
				foldersWithPermBits(inFolderIds, File.class, null, PermHandler.ObjectPermission.view), "contentfile", null, "folder_id",
				"AND filetype NOT LIKE 'image/%'");

		// get localization info for templates
		Map<Integer, LocalizationInfoItem> templateItems = getLocalizationInfo(channelIds,
				foldersWithPermBits(inFolderIds, Template.class, null, PermHandler.ObjectPermission.view), "template",
				" LEFT JOIN template master ON (template.is_master AND template.id = master.id) OR (!template.is_master AND template.channelset_id = master.channelset_id AND master.is_master) LEFT JOIN template_folder ON master.id = template_folder.template_id ",
				"template_folder.folder_id", null);

		// combine all localization info items
		List<ChannelLocalizationInfo> channels = new Vector<ChannelLocalizationInfo>(channelIds.size());

		for (Integer cId : channelIds) {
			ChannelLocalizationInfo cInfo = new ChannelLocalizationInfo();

			cInfo.setChannelId(cId);
			cInfo.setFolder(folderItems.get(cId));
			cInfo.setPage(pageItems.get(cId));
			cInfo.setImage(imageItems.get(cId));
			cInfo.setFile(fileItems.get(cId));
			cInfo.setTemplate(templateItems.get(cId));
			channels.add(cInfo);
		}

		return channels;
	}

	/**
	 * Get the localization info for a specific table
	 * @param channelIds list of channel ids
	 * @param folderIds list of folder ids
	 * @param tableName tablename
	 * @param join join (may be null)
	 * @param fieldName fieldname containing the reference to the folder ids
	 * @param restriction restriction statement (may be null, must start with AND)
	 * @return map of localization info items per channel
	 * @throws NodeException
	 */
	private static Map<Integer, LocalizationInfoItem> getLocalizationInfo(
			List<Integer> channelIds, List<Integer> folderIds,
			String tableName, String join, String fieldName, String restriction) throws NodeException {
		if (ObjectTransformer.isEmpty(channelIds)) {
			return Collections.emptyMap();
		}
		Map<Integer, LocalizationInfoItem> info = new HashMap<Integer, LocalizationInfoItem>();

		for (Integer cId : channelIds) {
			info.put(cId, new LocalizationInfoItem());
		}
		if (ObjectTransformer.isEmpty(folderIds)) {
			return info;
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		if (restriction == null) {
			restriction = "";
		}
		if (join == null) {
			join = "";
		}

		try {
			StringBuffer sql = new StringBuffer();

			sql.append("SELECT ").append(tableName).append(".channel_id, ").append(tableName).append(".is_master, count(DISTINCT ").append(tableName).append(".id) c FROM ").append(tableName).append(join).append(" WHERE ").append(fieldName).append(" IN (").append(StringUtils.repeat("?", folderIds.size(), ",")).append(") AND ").append(tableName).append(".channel_id IN (").append(StringUtils.repeat("?", channelIds.size(), ",")).append(") ").append(restriction).append(" group by ").append(tableName).append(".channel_id, ").append(tableName).append(".is_master order by ").append(tableName).append(
					".channel_id");
			pst = t.prepareStatement(sql.toString());
			int pCounter = 1;

			for (Integer fId : folderIds) {
				pst.setInt(pCounter++, fId);
			}
			for (Integer cId : channelIds) {
				pst.setInt(pCounter++, cId);
			}

			res = pst.executeQuery();

			while (res.next()) {
				// get the infoitem
				LocalizationInfoItem item = info.get(res.getInt("channel_id"));

				if (res.getBoolean("is_master")) {
					// if the object are marked as master, they are local objects
					item.setLocal(res.getInt("c"));
				} else {
					// otherwise they are localized objects
					item.setLocalized(res.getInt("c"));
				}
			}

			// finally, we calculate the inherited objects
			LocalizationInfoItem lastItem = null;

			for (Integer cId : channelIds) {
				LocalizationInfoItem currentItem = info.get(cId);

				// the last item is that of the direct master
				if (lastItem != null) {
					// calculate the number of inherited objects (all inherited,
					// localized or local objects from the master minus the
					// localized objects in the channel)
					currentItem.setInherited(lastItem.getInherited() + lastItem.getLocalized() + lastItem.getLocal() - currentItem.getLocalized());
				}
				lastItem = currentItem;
			}

			return info;
		} catch (SQLException e) {
			throw new NodeException(e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Get the folder id's with given permissions on the given object class (for
	 * the given language)
	 * 
	 * @param folderIds
	 *            list of folder id's to check
	 * @param clazz
	 *            object class
	 * @param language
	 *            content language (may be null)
	 * @param perms
	 *            permissions
	 * @return filtered folder id's
	 * @throws NodeException
	 */
	private static List<Integer> foldersWithPermBits(List<Integer> folderIds,
			Class<? extends NodeObject> clazz, ContentLanguage language,
			PermHandler.ObjectPermission... perms) throws NodeException {
		// for a start, take all folder ids
		List<Integer> permittedFolderIds = new Vector<Integer>(folderIds);

		// get the permission handler
		Transaction t = TransactionManager.getCurrentTransaction();
		PermHandler permHandler = t.getPermHandler();

		// if no permission handler is set, we do not need to filter
		if (permHandler == null) {
			return permittedFolderIds;
		}

		// filter out the folder id's with sufficient permission
		for (Iterator<Integer> i = permittedFolderIds.iterator(); i.hasNext();) {
			Folder folder = t.getObject(Folder.class, i.next());

			if (folder == null) {
				i.remove();
			} else {
				for (PermHandler.ObjectPermission p : perms) {
					if (!p.checkClass(folder, clazz, language)) {
						i.remove();
						break;
					}
				}
			}
		}
		return permittedFolderIds;
	}
}
