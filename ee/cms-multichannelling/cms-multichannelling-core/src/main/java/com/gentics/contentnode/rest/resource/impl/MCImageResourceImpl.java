package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.checkChannelSyncPerm;
import static com.gentics.contentnode.rest.util.MiscUtils.checkDeletePermSome;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
import com.gentics.contentnode.job.LocalizeImageCallable;
import com.gentics.contentnode.job.PushToMasterJob;
import com.gentics.contentnode.job.UnlocalizeJob;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.filters.Authenticated;
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
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.MCImageResource;
import com.gentics.contentnode.rest.util.DisinheritRestUtil;
import com.gentics.contentnode.rest.util.MCMiscUtils;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/image")
@Authenticated
public class MCImageResourceImpl implements MCImageResource {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCImageResourceImpl.class);

	@GET
	@Path("/usage/syncableObjects")
	@Override
	public ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> imageIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (ObjectTransformer.isEmpty(imageIds)) {
				return new ReferencedSyncableObjectsListResponse(
					null,
					new ResponseInfo(ResponseCode.OK, "No image IDs supplied, nothing to do"));
			}

			if (ObjectTransformer.isEmpty(srcNodeId) || ObjectTransformer.isEmpty(dstNodeId)) {
				return new ReferencedSyncableObjectsListResponse(
					null,
					new ResponseInfo(ResponseCode.INVALIDDATA, "The parameters <srcNodeId> and <dstNodeId> are mandatory"));
			}

			return MCMiscUtils.getSyncableObjects(ImageFile.class, imageIds, srcNodeId, dstNodeId);
		}
	}

	@POST
	@Path("/push2master/{id}")
	@Override
	public GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("image.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(ImageFile.class)
					.setId(id)
					.setMasterId(request.getMasterId())
					.setChannelId(request.getChannelId());

			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@POST
	@Path("/push2master")
	@Override
	public GenericResponse pushToMaster(MultiPushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("image.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(ImageFile.class)
					.setIds(request.getIds())
					.setMasterId(request.getMasterId())
					.setChannelId(request.getChannelId());

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
		LinkedList<Integer> idList = new LinkedList<Integer>(request.getIds());

		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(channelId) && !checkDeletePermSome(channelId, ImageFile.class, idList)) {
				I18nString message = new CNI18nString("image.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}

			UnlocalizeJob job = new UnlocalizeJob()
					.setClazz(ImageFile.class)
					.setIds(idList)
					.setChannelId(channelId);

			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@POST
	@Path("/localize/{id}")
	@Override
	public GenericResponse localize(@PathParam("id") Integer id, LocalizeRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (request == null) {
				logger.error("Error while localizing image: no data provided");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, "Error while localizing image: no data provided"));
			}

			if (id == null) {
				logger.error("Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: " + "image ID should be provided");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: " + "image ID should be provided"));
			}

			Transaction t = trx.getTransaction();
			ImageFile imageFile = t.getObject(ImageFile.class, id);

			if (imageFile == null) {
				logger.error("Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: " + "the specified image does not exist");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: " + "the specified image does not exist"));
			}

			Node channel = t.getObject(Node.class, request.getChannelId());

			if (channel == null || !channel.isChannel()) {
				logger.error(
						"Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "the specified channel does not exist or the node with that ID is not a channel");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "the specified channel does not exist or the node with that ID is not a channel"));
			}

			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				if (!PermHandler.ObjectPermission.create.checkObject(imageFile)) {
					I18nString message = new CNI18nString("image.nopermission");

					return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
				}
			}

			Node imageNode = imageFile.getFolder().getNode();

			// Check whether file's folder node is one of the channel master nodes (check if the folder
			// of the image is visible from the channel).
			if (!channel.getMasterNodes().contains(imageNode)) {
				logger.error(
						"Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "image's node is not master node of the specified channel");
				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE,
						"Error while localizing image {" + id + "} in channel {" + request.getChannelId() + "}: "
						+ "image's node is not master node of the specified channel"));
			}

			return Operator.executeLockedRethrowing(new CNI18nString("localizejob").toString(), AbstractContentNodeResource.getJobForegroundTime(request.getForegroundTime()) * 1000,
					Operator.lock(LockType.channelSet, imageFile.getChannelSetId()), new LocalizeImageCallable(id, request.getChannelId(), false), Function.identity());
		}
	}

	@GET
	@Path("/localizations/{id}")
	@Override
	public LocalizationsResponse localizations(@PathParam("id") Integer id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			ImageFile image = t.getObject(ImageFile.class, id);

			if (image == null) {
				I18nString message = new CNI18nString("image.notfound");
				message.setParameter("0", id.toString());
				return new LocalizationsResponse(
						new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, message.toString()));
			}

			if (!ObjectPermission.view.checkObject(image, image.getChannel())) {
				String msg = new CNI18nString("image.nopermission").toString();

				return new LocalizationsResponse(
					new Message(Type.CRITICAL, msg),
					new ResponseInfo(ResponseCode.PERMISSION, "Not enough permissions to view this image"));
			}

			File master = image.getMaster();
			Node masterNode = master.getNode();
			Map<Integer, Integer> localized = MultichannellingFactory.getLocalizedCopyChannels(image);
			int total = localized.size();
			MultichannellingFactory.filterPermissions(localized.values(), image);

			String msg = new CNI18nString("rest.localizations.success").toString();
			return new LocalizationsResponse(
				new Message(Type.SUCCESS, msg),
				master.getId(),
				masterNode.getId(),
				localized).setTotal(total).setHidden(total - localized.size());
		}
	}

	@GET
	@Path("/localizationinfo")
	@Override
	public LocalizationInfo getLocalizationInfo(@QueryParam("id") List<Integer> ids, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<LocalizableNodeObject<? extends NodeObject>> images = new Vector<LocalizableNodeObject<? extends NodeObject>>();

			for (Integer id : ids) {
				images.add((ContentFile) MiscUtils.load(ImageFile.class, id.toString(), ObjectPermission.view));
			}
			Node node = t.getObject(Node.class, nodeId);
			List<Node> masterNodes = node.getMasterNodes();
			List<Integer> channelIds = new Vector<Integer>(masterNodes.size() + 1);

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

			result.setChannels(MCMiscUtils.getLocalizationInfo(channelIds, images));

			return result;
		}
	}

	@GET
	@Path("/localizationinfo/{id}")
	@Override
	public LocalizationInfo getLocalizationInfo(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentFile image = (ContentFile) MiscUtils.load(ImageFile.class, id, ObjectPermission.view);
			Node node = t.getObject(Node.class, nodeId);
			List<Node> masterNodes = node.getMasterNodes();
			List<Integer> channelIds = new Vector<Integer>(masterNodes.size() + 1);

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

			result.setChannels(MCMiscUtils.getLocalizationInfo(channelIds, image));

			return result;
		}
	}

	/**
	 * Get the multichannelling exclusion and disinherit status of the file
	 * @param id file ID
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit/{id}")
	@Override
	public DisinheritResponse disinherit(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return DisinheritRestUtil.get(ImageFile.class, id);
		}
	}

	/**
	 * Set the multichannelling exclusion and disinherit status
	 * @param id file ID
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
						return DisinheritRestUtil.set(ImageFile.class, id, request, true);
					}
				}
			}, Function.identity());
		}
	}

	/**
	 * Get the multichannelling exclusion and disinherit status for a list of images
	 * @param ids list of image IDs
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit")
	@Override
	public MultiDisinheritResponse disinherit(@QueryParam("id") List<String> ids, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return DisinheritRestUtil.get(ImageFile.class, ids);
		}
	}

	/**
	 * Set the multichannelling exclusion and disinherit status for a list of images
	 * @param ids list of image IDs
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
						return DisinheritRestUtil.set(ImageFile.class, ids, request);
					}
				}
			}, Function.identity());
		}
	}
}
