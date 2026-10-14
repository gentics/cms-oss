package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.checkChannelSyncPerm;
import static com.gentics.contentnode.rest.util.MiscUtils.checkDeletePermSome;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.LocalizeFolderCallable;
import com.gentics.contentnode.job.PushToMasterJob;
import com.gentics.contentnode.job.UnlocalizeJob;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Folder.FileSearch;
import com.gentics.contentnode.object.Folder.PageSearch;
import com.gentics.contentnode.object.Folder.TemplateSearch;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.ContentNodeItem;
import com.gentics.contentnode.rest.model.ContentNodeItem.ItemType;
import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.MultiUnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.DisinheritResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.MultiDisinheritResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse.SyncDependencies;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.MCFolderResource;
import com.gentics.contentnode.rest.util.DisinheritRestUtil;
import com.gentics.contentnode.rest.util.MCMiscUtils;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/folder")
@Authenticated
public class MCFolderResourceImpl implements MCFolderResource {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCFolderResourceImpl.class);

	@GET
	@Path("/usage/syncableObjects")
	@Override
	public ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> folderIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId,
			@QueryParam("recursive") Boolean recursive,
			@QueryParam("types") List<PushToMasterRequest.Type> types) throws NodeException {
		if (ObjectTransformer.isEmpty(folderIds)) {
			return new ReferencedSyncableObjectsListResponse(
				null,
				new ResponseInfo(ResponseCode.OK, "No folder IDs supplied, nothing to do"));
		}

		if (ObjectTransformer.isEmpty(srcNodeId) || ObjectTransformer.isEmpty(dstNodeId)) {
			return new ReferencedSyncableObjectsListResponse(
				null,
				new ResponseInfo(ResponseCode.INVALIDDATA, "The parameters <srcNodeId> and <dstNodeId> are mandatory"));
		}

		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(srcNodeId)) {
			ReferencedSyncableObjectsListResponse ret = MCMiscUtils.getSyncableObjects(com.gentics.contentnode.object.Folder.class, folderIds, srcNodeId, dstNodeId);
			Transaction t = TransactionManager.getCurrentTransaction();

			if (types.contains(PushToMasterRequest.Type.page)) {
				PageSearch search = PageSearch.create();

				search.setRecursive(Boolean.TRUE.equals(recursive));

				for (Integer folderId : folderIds) {
					com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);

					List<Integer> pageIds = folder.getPages(search).stream().map(Page::getId).collect(Collectors.toList());
					ReferencedSyncableObjectsListResponse pageDeps = MCMiscUtils.getSyncableObjects(
						Page.class,
						pageIds,
						srcNodeId,
						dstNodeId);

					mergeAndPruneResponses(ret, pageDeps, pageIds, ItemType.page);
				}
			}

			if (types.contains(PushToMasterRequest.Type.folder)) {
				for (Integer folderId : folderIds) {
					com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);
					List<com.gentics.contentnode.object.Folder> folders = folder.getChildFolders();

					if (Boolean.TRUE.equals(recursive)) {
						loadAllSubfolders(folders);
					}

					List<Integer> depFolderIds = folders.stream().map(NodeObject::getId).collect(Collectors.toList());
					ReferencedSyncableObjectsListResponse folderDeps = MCMiscUtils.getSyncableObjects(
						com.gentics.contentnode.object.Folder.class,
						depFolderIds,
						srcNodeId,
						dstNodeId);

					mergeAndPruneResponses(ret, folderDeps, depFolderIds, null);
				}
			}

			if (types.contains(PushToMasterRequest.Type.file)) {
				FileSearch search = FileSearch.create();

				search.setRecursive(Boolean.TRUE.equals(recursive));

				for (Integer folderId : folderIds) {
					com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);

					List<Integer> fileIds = folder.getFiles(search).stream().map(File::getId).collect(Collectors.toList());
					ReferencedSyncableObjectsListResponse fileDeps = MCMiscUtils.getSyncableObjects(
						File.class,
						fileIds,
						srcNodeId,
						dstNodeId);

					mergeAndPruneResponses(ret, fileDeps, fileIds, ItemType.file);
				}
			}

			if (types.contains(PushToMasterRequest.Type.image)) {
				FileSearch search = FileSearch.create();

				search.setRecursive(Boolean.TRUE.equals(recursive));

				for (Integer folderId : folderIds) {
					com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);

					List<Integer> imageIds = folder.getImages(search).stream().map(ImageFile::getId).collect(Collectors.toList());
					ReferencedSyncableObjectsListResponse imageDeps = MCMiscUtils.getSyncableObjects(
						ImageFile.class,
						imageIds,
						srcNodeId,
						dstNodeId);

					mergeAndPruneResponses(ret, imageDeps, imageIds, ItemType.image);
				}
			}

			if (types.contains(PushToMasterRequest.Type.template)) {
				TemplateSearch search = TemplateSearch.create();

				for (Integer folderId : folderIds) {
					com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);

					List<Integer> templateIds = folder.getTemplates(search).stream().map(Template::getId).collect(Collectors.toList());
					ReferencedSyncableObjectsListResponse templateDeps = MCMiscUtils.getSyncableObjects(
						Template.class,
						templateIds,
						srcNodeId,
						dstNodeId);

					mergeAndPruneResponses(ret, templateDeps, templateIds, null);
				}
			}

			return ret;
		}
	}

	@POST
	@Path("/push2master/{id}")
	@Override
	public GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("folder.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(Folder.class)
					.setId(id)
					.setMasterId(request.getMasterId())
					.setChannelId(request.getChannelId())
					.setRecursive(request.getRecursive());

			List<PushToMasterRequest.Type> pushedTypes = request.getTypes();
			if (pushedTypes != null) {
				job.setPages(pushedTypes.contains(PushToMasterRequest.Type.page));
				job.setTemplates(pushedTypes.contains(PushToMasterRequest.Type.template));
				job.setImages(pushedTypes.contains(PushToMasterRequest.Type.image));
				job.setFiles(pushedTypes.contains(PushToMasterRequest.Type.file));
				job.setFolders(pushedTypes.contains(PushToMasterRequest.Type.folder));
			}
			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@POST
	@Path("/push2master")
	@Override
	public GenericResponse pushToMaster(MultiPushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("folder.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(Folder.class)
					.setIds(request.getIds())
					.setMasterId(request.getMasterId())
					.setChannelId(request.getChannelId())
					.setRecursive(request.getRecursive());

			List<PushToMasterRequest.Type> pushedTypes = request.getTypes();
			if (pushedTypes != null) {
				job.setPages(pushedTypes.contains(PushToMasterRequest.Type.page));
				job.setTemplates(pushedTypes.contains(PushToMasterRequest.Type.template));
				job.setImages(pushedTypes.contains(PushToMasterRequest.Type.image));
				job.setFiles(pushedTypes.contains(PushToMasterRequest.Type.file));
				job.setFolders(pushedTypes.contains(PushToMasterRequest.Type.folder));
			}
			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@POST
	@Path("/unlocalize/{id}")
	@Override
	public GenericResponse unlocalize(@PathParam("id") Integer id, UnlocalizeRequest request) throws NodeException {
		return unlocalize(new MultiUnlocalizeRequest(id, request));
	}

	@POST
	@Path("/unlocalize")
	@Override
	public GenericResponse unlocalize(MultiUnlocalizeRequest request) throws NodeException {
		Integer channelId = request.getChannelId();
		LinkedList<Integer> idList = new LinkedList<>(request.getIds());
		Class<? extends NodeObject> folderClass = com.gentics.contentnode.object.Folder.class;

		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(channelId) && !checkDeletePermSome(channelId, folderClass, idList)) {
				I18nString message = new CNI18nString("folder.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}

			UnlocalizeJob job = new UnlocalizeJob()
					.setClazz(Folder.class)
					.setIds(idList)
					.setChannelId(channelId)
					.setRecursive(request.getRecursive());

			List<UnlocalizeRequest.Type> pushedTypes = request.getTypes();

			if (pushedTypes != null) {
				job.setPages(pushedTypes.contains(UnlocalizeRequest.Type.page));
				job.setTemplates(pushedTypes.contains(UnlocalizeRequest.Type.template));
				job.setImages(pushedTypes.contains(UnlocalizeRequest.Type.image));
				job.setFiles(pushedTypes.contains(UnlocalizeRequest.Type.file));
				job.setFolders(pushedTypes.contains(UnlocalizeRequest.Type.folder));
			}
			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@POST
	@Path("/localize/{id}")
	@Override
	public GenericResponse localize(@PathParam("id") Integer id, LocalizeRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (request == null) {
				logger.error("Error while localizing folder: no data provided");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while localizing folder: no data provided"));
			}

			if (id == null) {
				logger.error("Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: " + "folder ID should be provided");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
								"Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: " + "folder ID should be provided"));
			}

			Transaction t = trx.getTransaction();
			com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, id);

			if (folder == null) {
				logger.error("Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: " + "the specified folder does not exist");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: " + "the specified folder does not exist"));
			}

			Node channel = t.getObject(Node.class, request.getChannelId());

			if (channel == null || !channel.isChannel()) {
				logger.error(
						"Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "the specified channel does not exist or the node with that ID is not a channel");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "the specified channel does not exist or the node with that ID is not a channel"));
			}

			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				if (!PermHandler.ObjectPermission.create.checkObject(folder)) {
					I18nString message = new CNI18nString("folder.nopermission");

					return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
				}
			}

			Node folderNode = folder.getMother().getNode();

			// Check whether page's folder node is one of the channel master nodes (check if the folder
			// of the page is visible from the channel).
			if (!channel.getMasterNodes().contains(folderNode)) {
				logger.error(
						"Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "folder's node is not master node of the specified channel");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing folder {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "folder's node is not master node of the specified channel"));
			}

			return Operator.executeLockedRethrowing(new CNI18nString("localizejob").toString(), AbstractContentNodeResource.getJobForegroundTime(request.getForegroundTime()) * 1000,
					Operator.lock(LockType.channelSet, folder.getChannelSetId()), new LocalizeFolderCallable(id, request.getChannelId(), false));
		}
	}

	@GET
	@Path("/localizations/{id}")
	@Override
	public LocalizationsResponse localizations(@PathParam("id") Integer id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			com.gentics.contentnode.object.Folder folder =
				t.getObject(com.gentics.contentnode.object.Folder.class, id);

			if (folder == null) {
				I18nString message = new CNI18nString("folder.notfound");
				return new LocalizationsResponse(
						new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, message.toString()));
			}

			if (!ObjectPermission.view.checkObject(folder)) {
				String msg = new CNI18nString("folder.nopermission").toString();

				return new LocalizationsResponse(
					new Message(Type.CRITICAL, msg),
					new ResponseInfo(ResponseCode.PERMISSION, "Not enough permissions to view this folder"));
			}

			com.gentics.contentnode.object.Folder master = folder.getMaster();
				Node masterNode = master.getNode();
				Map<Integer, Integer> localized = MultichannellingFactory.getLocalizedCopyChannels(folder);

			// get localization counts for folder, subfolders and all items in the folder and subfolders
			Map<Integer, Map<Integer, AtomicInteger>> counts = MultichannellingFactory.getLocalizationCounts(folder);

			// calculate total and hidden
			int total = 0;
			int hidden = 0;
			for (Map.Entry<Integer, Map<Integer, AtomicInteger>> entry : counts.entrySet()) {
				int folderId = entry.getKey();
				com.gentics.contentnode.object.Folder tmpFolder = t.getObject(com.gentics.contentnode.object.Folder.class, folderId);
				for (Map.Entry<Integer, AtomicInteger> folderEntry : entry.getValue().entrySet()) {
					int channelId = folderEntry.getKey();
					Node channel = t.getObject(Node.class, channelId);
					total += folderEntry.getValue().get();
					if (!ObjectPermission.view.checkObject(tmpFolder, channel)) {
						hidden += folderEntry.getValue().get();
					}
				}
			}

			MultichannellingFactory.filterPermissions(localized.values(), folder);

			String msg = new CNI18nString("rest.localizations.success").toString();
			return new LocalizationsResponse(
				new Message(Type.SUCCESS, msg),
				master.getId(),
				masterNode.getId(),
			localized).setTotal(total).setHidden(hidden);
		}
	}

	@GET
	@Path("/localizationinfo/{id}")
	@Override
	public LocalizationInfo getLocalizationInfo(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId,
			@QueryParam("recursive") boolean recursive) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.setChannelId(nodeId);
			try {
				com.gentics.contentnode.object.Folder folder = MiscUtils.load(Folder.class, id);
				Node node = t.getObject(Node.class, nodeId);
				List<Node> masterNodes = node.getMasterNodes();
				List<Integer> channelIds = new Vector<>(masterNodes.size() + 1);

				channelIds.add(nodeId);
				for (Node master : masterNodes) {
					if (master.isChannel()) {
						channelIds.add(ObjectTransformer.getInt(master.getId(), 0));
					} else {
						channelIds.add(0);
					}
				}
				Collections.reverse(channelIds);

				// create the accumulated result
				LocalizationInfo result = new LocalizationInfo(null, new ResponseInfo(ResponseCode.OK, "Successfully collected localization info"));
				com.gentics.contentnode.object.Folder master = folder;

				while (!master.isMaster()) {
					master = master.getMaster();
				}

				List<ContentLanguage> languages = folder.getNode().getLanguages();

				// collect all folder IDs
				List<Integer> folderIds = new Vector<>();

				collectFolderIds(ObjectTransformer.getInt(master.getId(), -1), folderIds, recursive);

				result.setChannels(
						MCMiscUtils.getFolderLocalizationInfo(channelIds, folderIds,
								recursive ? folderIds : Arrays.asList(ObjectTransformer.getInteger(master.getId(), null)), languages));

				return result;
			} finally {
				t.resetChannel();
			}
		}
	}

	/**
	 * Get the multichannelling exclusion and disinherit status of the folder
	 * @param id folder ID
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit/{id}")
	@Override
	public DisinheritResponse disinherit(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return DisinheritRestUtil.get(com.gentics.contentnode.object.Folder.class, id);
		}
	}

	/**
	 * Set the multichannelling exclusion and disinherit status
	 * @param id folder ID
	 * @param nodeId node ID
	 * @param waitMs wait timeout in milliseconds
	 * @param request request object
	 * @return generic response
	 */
	@POST
	@Path("/disinherit/{id}")
	@Override
	public GenericResponse disinherit(final @PathParam("id") String id, final @QueryParam("nodeId") Integer nodeId,
			@QueryParam("wait") @DefaultValue("0") long waitMs, final DisinheritRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return Operator.executeRethrowing(new CNI18nString("inheritance.change").toString(), waitMs, new Callable<GenericResponse>() {
				@Override
				public GenericResponse call() throws Exception {
					try (ChannelTrx trx = new ChannelTrx(TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId));
							InstantPublishingTrx ipTrx = new InstantPublishingTrx(false)) {
						return DisinheritRestUtil.set(com.gentics.contentnode.object.Folder.class, id, request, true);
					}
				}
			});
		}
	}

	/**
	 * Get the multichannelling exclusion and disinherit status for a list of folders
	 * @param ids list of folder IDs
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit")
	@Override
	public MultiDisinheritResponse disinherit(@QueryParam("id") List<String> ids, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return DisinheritRestUtil.get(com.gentics.contentnode.object.Folder.class, ids);
		}
	}

	/**
	 * Set the multichannelling exclusion and disinherit status for a list of folders
	 * @param ids list of folder IDs
	 * @param nodeId node ID
	 * @param waitMs wait timeout in milliseconds
	 * @param request request object
	 * @return generic response
	 * @throws Exception
	 */
	@POST
	@Path("/disinherit")
	@Override
	public GenericResponse disinherit(final @QueryParam("id") List<String> ids, final @QueryParam("nodeId") Integer nodeId,
			@QueryParam("wait") @DefaultValue("0") long waitMs, final DisinheritRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			return Operator.executeRethrowing(new CNI18nString("inheritance.change").toString(), waitMs, new Callable<GenericResponse>() {
				@Override
				public GenericResponse call() throws Exception {
					try (ChannelTrx trx = new ChannelTrx(TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId));
							InstantPublishingTrx ipTrx = new InstantPublishingTrx(false)) {
						return DisinheritRestUtil.set(com.gentics.contentnode.object.Folder.class, ids, request);
					}
				}
			});
		}
	}

	/**
	 * Method to collect all folder ids starting with a given folder id
	 * @param mother mother id
	 * @param folderIds list of folder ids (will be modified)
	 * @param recursive true if also children of children (and so on) shall be added
	 * @throws NodeException
	 */
	private void collectFolderIds(Integer mother, List<Integer> folderIds, boolean recursive) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// first add the given mother id (and all channelset variants) to the list
		com.gentics.contentnode.object.Folder motherFolder = t.getObject(com.gentics.contentnode.object.Folder.class, mother);
		if (motherFolder == null) {
			return;
		}
		Map<Integer, Integer> channelSet = motherFolder.getChannelSet();

		if (channelSet.isEmpty()) {
			folderIds.add(mother);
		} else {
			for (Object cId : channelSet.values()) {
				folderIds.add(ObjectTransformer.getInteger(cId, null));
			}
		}

		// now get subfolders
		PreparedStatement pst = null;
		ResultSet res = null;
		List<Integer> tmpFolderIds = new Vector<>();

		try {
			pst = t.prepareStatement("SELECT id FROM folder WHERE mother = ? and is_master = 1");
			pst.setInt(1, mother);

			res = pst.executeQuery();
			while (res.next()) {
				if (checkFolderPermission(res.getInt("id"), PermHandler.PERM_VIEW)) {
					tmpFolderIds.add(res.getInt("id"));
				}
			}
		} catch (SQLException e) {
			throw new NodeException(e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}

		if (recursive) {
			for (Integer fId : tmpFolderIds) {
				collectFolderIds(fId, folderIds, recursive);
			}
		} else {
			for (Integer fId : tmpFolderIds) {
				com.gentics.contentnode.object.Folder folder = t.getObject(com.gentics.contentnode.object.Folder.class, fId);

				if (folder == null) {
					continue;
				}
				channelSet = folder.getChannelSet();
				if (channelSet.isEmpty()) {
					folderIds.add(fId);
				} else {
					for (Object cId : channelSet.values()) {
						folderIds.add(ObjectTransformer.getInteger(cId, null));
					}
				}
			}
		}
	}

	/**
	 * Add all subfolders of the specified folders to the list.
	 *
	 * @param folders The list of starting folders
	 *
	 * @throws NodeException When the child folders cannot be loaded
	 */
	private void loadAllSubfolders(List<com.gentics.contentnode.object.Folder> folders) throws NodeException {
		List<com.gentics.contentnode.object.Folder> remainingFolders = new ArrayList<>(folders);

		while (!remainingFolders.isEmpty()) {
			List<com.gentics.contentnode.object.Folder> children = new ArrayList<>();
			for (com.gentics.contentnode.object.Folder child : remainingFolders) {
				children.addAll(child.getChildFolders());
			}

			folders.addAll(children);
			remainingFolders.clear();
			remainingFolders.addAll(children);
		}
	}

	/**
	 * Removes objects with the specified <code>objIds</code> from the dependency map for the
	 * specified <code>type</code>
	 *
	 * When <code>removeTop</code> is <code>true</code> the entries which keys are in <code>objIds</code>
	 * will also be removed from <code>deps</code>.
	 *
	 * @param deps The dependency map to prune
	 * @param objIds The object IDs to remove
	 * @param type The object type to remove dependencies for
	 * @param removeTop Whether to remove entries in <code>deps</code> with matching IDs
	 */
	private void pruneDependencies(SyncDependencies deps, List<Integer> objIds, ItemType type, boolean removeTop) {
		if (removeTop) {
			for (Integer id : objIds) {
				deps.remove(id);
			}
		}

		for (Map<ItemType, Map<Integer, Integer>> typeDep : deps.values()) {
			Map<Integer, Integer> dep = typeDep.get(type);

			if (dep != null) {
				for (Integer id : objIds) {
					dep.remove(id);
				}
			}
		}
	}

	/**
	 * Merge the object lists and dependencies from <code>src</code> into <code>dest</code>,
	 * and removes all references to objects that will be moved in any case.
	 *
	 * @param dest The target response to merge everything into
	 * @param src The response to merge into <code>dest</code>
	 * @param movedObjIds The IDs of the moved objects
	 * @param movedObjType The type of the objects referenced by <code>movedObjIds</code>
	 */
	private void mergeAndPruneResponses(
			ReferencedSyncableObjectsListResponse dest,
			ReferencedSyncableObjectsListResponse src,
			List<Integer> movedObjIds,
			ItemType movedObjType) {
		Predicate<ContentNodeItem> isAlreadyMoved = o -> movedObjIds.contains(o.getId());

		if (movedObjType != null) {
			switch (movedObjType) {
				case page:
					src.getPages().removeIf(isAlreadyMoved);
					dest.getPages().removeIf(isAlreadyMoved);
					break;

				case file:
					src.getFiles().removeIf(isAlreadyMoved);
					dest.getFiles().removeIf(isAlreadyMoved);
					break;

				case image:
					src.getImages().removeIf(isAlreadyMoved);
					dest.getImages().removeIf(isAlreadyMoved);
					break;

				default:
					// Nothing to do at this point.
			}
		}

		src.getPages().removeAll(dest.getPages());
		dest.getPages().addAll(src.getPages());

		src.getFiles().removeAll(dest.getFiles());
		dest.getFiles().addAll(src.getFiles());

		src.getImages().removeAll(dest.getImages());
		dest.getImages().addAll(src.getImages());

		for (Map.Entry<ItemType, SyncDependencies> entry : dest.getDependencies().entrySet()) {
			pruneDependencies(entry.getValue(), movedObjIds, movedObjType, movedObjType == entry.getKey());
		}

		for (Map.Entry<ItemType, SyncDependencies> entry : src.getDependencies().entrySet()) {
			ItemType key = entry.getKey();

			pruneDependencies(entry.getValue(), movedObjIds, movedObjType, movedObjType == key);
			dest.getDependencies().merge(key, entry.getValue(), this::mergeSyncDependencies);
		}
	}

	/**
	 * A remapping function for merging {@link SyncDependencies} entries in
	 * {@link ReferencedSyncableObjectsListResponse}s.
	 *
	 * This mapping will recursively merge everything into <code>dest</code> and
	 * return it at the end.
	 *
	 * @param dest The target response to merge into
	 * @param src The response to merge
	 * @return The resulting merged dependency map
	 */
	private SyncDependencies mergeSyncDependencies(SyncDependencies dest, SyncDependencies src) {
		// I am really sorry for this :-/.
		for (Map.Entry<Integer, Map<ItemType, Map<Integer, Integer>>> objDeps : src.entrySet()) {
			dest.merge(
				objDeps.getKey(),
				objDeps.getValue(),
				(oldObjDeps, newObjDeps) -> {
					for (Map.Entry<ItemType, Map<Integer, Integer>> typeDeps : newObjDeps.entrySet()) {
						oldObjDeps.merge(
							typeDeps.getKey(),
							typeDeps.getValue(),
							(oldVal, newVal) -> {
								oldVal.putAll(newVal);

								return oldVal;
							});
					}

					return oldObjDeps;
				});
		}

		return dest;
	}

	/**
	 * Check whether the user has the given permission bit set on the given folder
	 * @param folderId id of the folder to check
	 * @param permBit permission bit to check
	 * @return true when the user has the permission bit set, false if not
	 */
	private boolean checkFolderPermission(Integer folderId, int permBit) throws NodeException {
		PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();

		return permHandler.checkPermissionBit(Folder.TYPE_FOLDER, folderId, permBit) || permHandler.checkPermissionBit(Node.TYPE_NODE, folderId, permBit);
	}
}
