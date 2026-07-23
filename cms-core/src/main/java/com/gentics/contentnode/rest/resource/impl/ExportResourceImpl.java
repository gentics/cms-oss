package com.gentics.contentnode.rest.resource.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.request.ExportSelectionRequest;
import com.gentics.contentnode.rest.model.response.ExportSelectionResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.ExportResource;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Export helper resource
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/export")
public class ExportResourceImpl implements ExportResource {

	/* (non-Javadoc)
	 * @see com.gentics.contentnode.rest.api.ExportResource#getExportSelection(com.gentics.contentnode.rest.model.request.ExportSelectionRequest)
	 */
	@POST
	@Path("/selection")
	public ExportSelectionResponse getExportSelection(
			ExportSelectionRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			List<Integer> subselFolders = new Vector<Integer>();
			Map<Integer, List<Integer>> subselInheritedFolders = new HashMap<Integer, List<Integer>>();

			// get all selected pages
			List<Integer> pageIds = request.getPages();

			if (!ObjectTransformer.isEmpty(pageIds)) {
				List<Page> pages = t.getObjects(Page.class, pageIds);
				
				for (Page page : pages) {
					addFolders(page.getChannel(), page.getFolder(), subselFolders, subselInheritedFolders);
				}
			}

			// get all selected images
			List<Integer> imageIds = request.getImages();

			if (!ObjectTransformer.isEmpty(imageIds)) {
				List<ImageFile> images = t.getObjects(ImageFile.class, imageIds);
				
				for (ImageFile imageFile : images) {
					addFolders(imageFile.getChannel(), imageFile.getFolder(), subselFolders, subselInheritedFolders);
				}
			}

			// get all selected files
			List<Integer> fileIds = request.getFiles();

			if (!ObjectTransformer.isEmpty(fileIds)) {
				List<File> files = t.getObjects(File.class, fileIds);
				
				for (File file : files) {
					addFolders(file.getChannel(), file.getFolder(), subselFolders, subselInheritedFolders);
				}
			}

			// get all selected folders
			List<Integer> folderIds = request.getFolders();

			if (!ObjectTransformer.isEmpty(folderIds)) {
				List<Folder> folders = t.getObjects(Folder.class, folderIds);
				
				for (Folder folder : folders) {
					addFolders(folder.getChannel(), folder.getMother(), subselFolders, subselInheritedFolders);
				}
			}

			// inherited folders
			Map<Integer, List<Integer>> inheritedFolders = request.getInheritedFolders();

			if (!ObjectTransformer.isEmpty(inheritedFolders)) {
				for (Entry<Integer, List<Integer>> entry : inheritedFolders.entrySet()) {
					Node channel = t.getObject(Node.class, entry.getKey());
					List<Folder> folders = t.getObjects(Folder.class, entry.getValue());
					
					for (Folder folder : folders) {
						addFolders(channel, folder.getMother(), subselFolders, subselInheritedFolders);
					}
				}
			}

			ExportSelectionResponse response = new ExportSelectionResponse(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched export selection data"));

			response.setFolders(subselFolders);
			response.setInheritedFolders(subselInheritedFolders);
			trx.success();
			return response;
		}
	}

	/**
	 * Add the folders to the list/map of folders
	 * @param channel channel of the object (may be null)
	 * @param parent parent folder of the object
	 * @param folders list of folders
	 * @param inheritedFolders map of inherited folders
	 * @throws NodeException
	 */
	protected void addFolders(Node channel, Folder parent,
			List<Integer> folders, Map<Integer, List<Integer>> inheritedFolders) throws NodeException {
		if (parent == null) {
			return;
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Integer> channelInheritedFolders = null;

		if (channel != null) {
			t.setChannelId(channel.getId());
			// get the parent in the correct channel
			parent = t.getObject(Folder.class, parent.getId());
			channelInheritedFolders = inheritedFolders.get(channel.getId());
			if (channelInheritedFolders == null) {
				channelInheritedFolders = new Vector<Integer>();
				inheritedFolders.put(ObjectTransformer.getInteger(channel.getId(), null), channelInheritedFolders);
			}
		}
		try {
			// add the folder to the list of folders
			addFolder(parent, folders, channelInheritedFolders);
		} finally {
			if (channel != null) {
				t.resetChannel();
			}
		}
	}

	/**
	 * Recursively add the given folder and all mother folders to the given list
	 * of folder ids (if not already added)
	 * 
	 * @param folder
	 *            folder to add
	 * @param folders
	 *            list of folder ids (will be modified)
	 * @param inheritedFolders
	 * 			  list of inherited folders
	 * @throws NodeException
	 */
	protected void addFolder(Folder folder, List<Integer> folders, List<Integer> inheritedFolders) throws NodeException {
		if (folder == null) {
			return;
		}
		if (folder.isInherited()) {
			if (!inheritedFolders.contains(folder.getId())) {
				// add to list of inherited folders
				inheritedFolders.add(ObjectTransformer.getInteger(folder.getId(), null));
				// do recursion
				addFolder(folder.getMother(), folders, inheritedFolders);
			}
		} else {
			if (!folders.contains(folder.getId())) {
				// add to list of folders
				folders.add(ObjectTransformer.getInteger(folder.getId(), null));
				// do recursion
				addFolder(folder.getMother(), folders, inheritedFolders);
			}
		}
	}
}
