package com.gentics.contentnode.rest.resource.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.InstantPublishingTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.LocalizePageCallable;
import com.gentics.contentnode.job.LocalizeRootTagCallable;
import com.gentics.contentnode.job.PushToMasterJob;
import com.gentics.contentnode.job.UnlocalizeJob;
import com.gentics.contentnode.job.UnlocalizeTagJob;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.LocalizationType;
import com.gentics.contentnode.rest.model.request.DisinheritRequest;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.MultiUnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
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
import com.gentics.contentnode.rest.resource.MCPageResource;
import com.gentics.contentnode.rest.util.DisinheritRestUtil;
import com.gentics.contentnode.rest.util.MCMiscUtils;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.rest.util.Operator.LockType;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;
import io.reactivex.Observable;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;

import static com.gentics.contentnode.rest.util.MiscUtils.checkChannelSyncPerm;
import static com.gentics.contentnode.rest.util.MiscUtils.checkDeletePermSome;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/page")
@Authenticated
public class MCPageResourceImpl implements MCPageResource {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCPageResourceImpl.class);

	@GET
	@Path("/usage/syncableObjects")
	@Override
	public ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> pageIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (ObjectTransformer.isEmpty(pageIds)) {
				return new ReferencedSyncableObjectsListResponse(
					null,
					new ResponseInfo(ResponseCode.OK, "No page IDs supplied, nothing to do"));
			}

			if (ObjectTransformer.isEmpty(srcNodeId) || ObjectTransformer.isEmpty(dstNodeId)) {
				return new ReferencedSyncableObjectsListResponse(
					null,
					new ResponseInfo(ResponseCode.INVALIDDATA, "The parameters <srcNodeId> and <dstNodeId> are mandatory"));
			}

			return MCMiscUtils.getSyncableObjects(Page.class, pageIds, srcNodeId, dstNodeId);
		}
	}

	@POST
	@Path("/push2master/{id}")
	@Override
	public GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("page.nopermission");
				return new GenericResponse(new Message(Type.CRITICAL,
						message.toString()), new ResponseInfo(
						ResponseCode.PERMISSION, message.toString()));
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(Page.class)
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
				I18nString message = new CNI18nString("page.nopermission");
				return new GenericResponse(new Message(Type.CRITICAL,
						message.toString()), new ResponseInfo(
						ResponseCode.PERMISSION, message.toString()));
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(Page.class)
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
			if (!checkChannelSyncPerm(channelId) && !checkDeletePermSome(channelId, Page.class, idList)) {
				I18nString message = new CNI18nString("page.nopermission");

				return new GenericResponse(new Message(Type.CRITICAL, message.toString()), new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}

			UnlocalizeJob job = new UnlocalizeJob()
					.setClazz(Page.class)
					.setIds(idList)
					.setChannelId(channelId);

			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@POST
	@Path("/localize/{id}")
	@Override
	public GenericResponse localize(@PathParam("id") Integer id, PageLocalizeRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (request == null) {
				logger.error("Error while localizing page: no data provided");
				I18nString m = new CNI18nString("rest.general.error");
				return new GenericResponse(new Message(Message.Type.CRITICAL,
						m.toString()), new ResponseInfo(ResponseCode.FAILURE,
								"Error while localizing page: no data provided"));
			}

			if (id == null) {
				logger.error("Error while localizing page {" + id
						+ "} in channel {" + request.getChannelId() + "}: " +
								"page ID should be provided");
				I18nString m = new CNI18nString("rest.general.error");
				return new GenericResponse(new Message(Message.Type.CRITICAL,
						m.toString()), new ResponseInfo(ResponseCode.FAILURE,
								"Error while localizing page {" + id
								+ "} in channel {" + request.getChannelId() + "}: " +
										"page ID should be provided"));
			}

			Transaction t = trx.getTransaction();
			Page page = t.getObject(Page.class, id);

			if(page == null) {
				logger.error("Error while localizing page {" + id
						+ "} in channel {" + request.getChannelId() + "}: " +
								"the specified page does not exist");
				I18nString m = new CNI18nString("rest.general.error");
				return new GenericResponse(new Message(Message.Type.CRITICAL,
						m.toString()), new ResponseInfo(ResponseCode.FAILURE,
								"Error while localizing page {" + id
								+ "} in channel {" + request.getChannelId() + "}: " +
										"the specified page does not exist"));
			}

			var partial = request.getLocalizationType() == LocalizationType.PARTIAL;

			if (partial && !Feature.PARTIAL_MULTICHANNELLING.isActivated(page.getNode())) {
				logger.error("Ignoring flag for partial localization of page {" + id + "} in channel {" + request.getChannelId() + "}: Feature PARTIAL_MULTICHANNELLING not available");
				partial = false;
			}

			Node channel = t.getObject(Node.class, request.getChannelId());
			if (channel == null || !channel.isChannel()) {
				logger.error("Error while localizing page {" + id
						+ "} in channel {" + request.getChannelId() + "}: " +
								"the specified channel does not exist or the node with that ID is not a channel");
				I18nString m = new CNI18nString("rest.general.error");
				return new GenericResponse(new Message(Message.Type.CRITICAL,
						m.toString()), new ResponseInfo(ResponseCode.FAILURE,
								"Error while localizing page {" + id
								+ "} in channel {" + request.getChannelId() + "}: " +
										"the specified channel does not exist or the node with that ID is not a channel"));
			}

			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				if (!PermHandler.ObjectPermission.create.checkObject(page)) {
					I18nString message = new CNI18nString("page.nopermission");
					return new GenericResponse(new Message(Type.CRITICAL,
							message.toString()), new ResponseInfo(
									ResponseCode.PERMISSION, message.toString()));
				}
			}

			Node pageNode = page.getFolder().getNode();

			// Check whether page's folder node is one of the channel master nodes (check if the folder
			// of the page is visible from the channel).
			if(!channel.getMasterNodes().contains(pageNode)) {
				logger.error("Error while localizing page {" + id
						+ "} in channel {" + request.getChannelId() + "}: " +
								"page's node is not master node of the specified channel");
				I18nString m = new CNI18nString("rest.general.error");
				return new GenericResponse(new Message(Message.Type.CRITICAL,
						m.toString()), new ResponseInfo(ResponseCode.FAILURE,
								"Error while localizing page {" + id
								+ "} in channel {" + request.getChannelId() + "}: " +
										"page's node is not master node of the specified channel"));
			}

			return Operator.executeLocked(new CNI18nString("localizejob").toString(), AbstractContentNodeResource.getJobForegroundTime(request.getForegroundTime()) * 1000L,
					Operator.lock(LockType.channelSet, page.getChannelSetId()), new LocalizePageCallable(id, request.getChannelId(), false, partial));
		}
	}

	@POST
	@Path("/localize/{pageId}/tags/{tagId}")
	@Override
	public GenericResponse localizeTag(@PathParam("pageId") Integer pageId, @PathParam("tagId") String tagId) throws NodeException {
		if (pageId == null) {
			var msg = "Error while localizing tag: no page ID provided";

			logger.error(msg);

			var m = new CNI18nString("rest.general.error");

			return new GenericResponse(
					new Message(Message.Type.CRITICAL, m.toString()),
					new ResponseInfo(ResponseCode.FAILURE, msg));

		}

		if (StringUtils.isBlank(tagId)) {
			var msg = "Error while localizing tag: no tag ID provided";

			logger.error(msg);

			var m = new CNI18nString("rest.general.error");

			return new GenericResponse(
					new Message(Message.Type.CRITICAL, m.toString()),
					new ResponseInfo(ResponseCode.FAILURE, msg));
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			Page page = t.getObject(Page.class, pageId);

			if (page == null) {
				var msg = String.format("Error while localizing tag {%s} for page {%d}: the specified page does not exist", tagId, pageId);

				logger.error(msg);

				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(
						new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, msg));
			}

			try (ChannelTrx cTrx = new ChannelTrx(page.getChannel())) {
				if (!ObjectPermission.edit.checkObject(page)) {
					String msg = new CNI18nString("page.nopermission").toString();
					return new GenericResponse(
							new Message(Type.CRITICAL, msg),
							new ResponseInfo(ResponseCode.PERMISSION, "Not enough permissions to edit this page in the channel"));
				}
			}

			if (!Feature.PARTIAL_MULTICHANNELLING.isActivated(page.getNode())) {
				var msg = String.format("Error while localizing tag {%s} for page {%d}: page is not partially localized", tagId, pageId);

				logger.error(msg);

				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(
						new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, msg));
			}

			if (!page.getContent().isPartiallyLocalized()) {
				var msg = String.format("Error while localizing tag {%s} for page {%d}: page is not partially localized", tagId, pageId);

				logger.error(msg);

				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(
						new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, msg));
			}

			var tag = page.getContentTag(tagId);

			if (tag == null) {
				var msg = String.format("Error while localizing tag {%s} for page {%d}: the specified tag does not exist", tagId, pageId);
				logger.error(msg);
				I18nString m = new CNI18nString("rest.general.error");
				return new GenericResponse(
						new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, msg));
			}

			if (!tag.comesFromTemplate()) {
				var msg = String.format("Error while localizing tag {%s} for page {%d}: tag is not a root tag", tagId, pageId);

				logger.error(msg);

				I18nString m = new CNI18nString("rest.general.error");

				return new GenericResponse(
						new Message(Message.Type.CRITICAL, m.toString()),
						new ResponseInfo(ResponseCode.FAILURE, msg));
			}

			if (!page.getMaster().getTag(tagId).equals(tag)) {
				return new GenericResponse(
						new Message(Type.SUCCESS, "Tag already localized"),
						new ResponseInfo(ResponseCode.OK, "Tag already localized"));
			}

			return Operator.executeLocked(
					new CNI18nString("localizejob").toString(),
					AbstractContentNodeResource.getJobForegroundTime(null) * 1000L,
					Operator.lock(LockType.channelSet, page.getChannelSetId()),
					new LocalizeRootTagCallable(pageId, page.getChannel().getId(), tagId)
			);
		}
	}

	@POST
	@Path("/unlocalize/{pageId}/tags/{tagId}")
	@Override
	public GenericResponse unlocalizeTag(@PathParam("pageId") Integer pageId, @PathParam("tagId") String tagId, UnlocalizeRequest request) throws Exception {
		if (request == null) {
			var msg = "Error while unlocalizing root tag: no UnlocalizeRequest provided";

			logger.error(msg);

			return new GenericResponse(
				new Message(Message.Type.CRITICAL, new CNI18nString("rest.general.error").toString()),
				new ResponseInfo(ResponseCode.FAILURE, msg));
		}

		var channelId = request.getChannelId();

		try (var trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(channelId) && !checkDeletePermSome(channelId, Page.class, Collections.singletonList(pageId))) {
				var message = new CNI18nString("page.nopermission");

				return new GenericResponse(
					new Message(Type.CRITICAL, message.toString()),
					new ResponseInfo(ResponseCode.PERMISSION, message.toString()));
			}

			var job = new UnlocalizeTagJob()
				.setPageId(pageId)
				.setTagId(tagId)
				.setChannelId(channelId);

			return job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
		}
	}

	@GET
	@Path("/localizations/{id}")
	@Override
	public LocalizationsResponse localizations(
			@PathParam("id") Integer id,
			@QueryParam("status") @DefaultValue("false") boolean addStatus) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page page = t.getObject(Page.class, id);

			if (page == null) {
				I18nString message = new CNI18nString("page.notfound");
				message.setParameter("0", id.toString());

				return new LocalizationsResponse(
						new Message(Type.CRITICAL, message.toString()),
						new ResponseInfo(ResponseCode.NOTFOUND, message.toString()));
			}

			if (!ObjectPermission.view.checkObject(page, page.getChannel())) {
				String msg = new CNI18nString("page.nopermission").toString();

				return new LocalizationsResponse(
					new Message(Type.CRITICAL, msg),
					new ResponseInfo(ResponseCode.PERMISSION, "Not enough permissions to view this page"));
			}

			Page master = page.getMaster();
			Node masterNode = master.getNode();
			Map<Integer, Integer> nodeIds = MultichannellingFactory.getLocalizedCopyChannels(page);
			List<Integer> online = null;
			List<Integer> offline = null;

			int total = nodeIds.size();

			MultichannellingFactory.filterPermissions(nodeIds.values(), page);

			if (addStatus) {
				List<Page> pages = t.getObjects(Page.class, nodeIds.keySet());
				online = Observable.fromIterable(pages).filter(Page::isOnline).map(Page::getId).toList().blockingGet();
				offline = Observable.fromIterable(pages).filter(p -> !p.isOnline()).map(Page::getId).toList().blockingGet();
			}

			String msg = new CNI18nString("rest.localizations.success").toString();
			return new LocalizationsResponse(
				new Message(Type.SUCCESS, msg),
				master.getId(),
				masterNode.getId(),
				nodeIds)
				.setTotal(total)
				.setHidden(total - nodeIds.size())
				.setOnline(online)
				.setOffline(offline);
		}
	}

	@GET
	@Path("/localizationinfo")
	@Override
	public LocalizationInfo getLocalizationInfo(@QueryParam("id") List<Integer> ids, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<LocalizableNodeObject<? extends NodeObject>> pages = new Vector<LocalizableNodeObject<? extends NodeObject>>();
			for (Integer id : ids) {
				pages.add(MiscUtils.load(Page.class, id.toString(), ObjectPermission.view));
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
			LocalizationInfo result = new LocalizationInfo(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully collected localization info"));
			result.setChannels(MCMiscUtils.getLocalizationInfo(channelIds, pages));

			return result;
		}
	}

	@GET
	@Path("/localizationinfo/{id}")
	@Override
	public LocalizationInfo getLocalizationInfo(@PathParam("id") Integer id,
			@QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page page = null;
			try (ChannelTrx cTrx = new ChannelTrx(nodeId)) {
				page = MiscUtils.load(Page.class, id.toString(), ObjectPermission.view);
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
			LocalizationInfo result = new LocalizationInfo(null,
					new ResponseInfo(ResponseCode.OK,
							"Successfully collected localization info"));
			result.setChannels(MCMiscUtils.getLocalizationInfo(channelIds, page));

			return result;
		}
	}

	/**
	 * Get the multichannelling exclusion and disinherit status of the page
	 * @param id page ID
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit/{id}")
	@Override
	public DisinheritResponse disinherit(@PathParam("id") String id, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return DisinheritRestUtil.get(Page.class, id);
		}
	}

	/**
	 * Set the multichannelling exclusion and disinherit status
	 * @param id page ID
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
			return Operator.execute(new CNI18nString("inheritance.change").toString(), waitMs, new Callable<GenericResponse>() {
				@Override
				public GenericResponse call() throws Exception {
					try (ChannelTrx trx = new ChannelTrx(TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId));
							InstantPublishingTrx ipTrx = new InstantPublishingTrx(false)) {
						return DisinheritRestUtil.set(Page.class, id, request, true);
					}
				}
			});
		}
	}

	/**
	 * Get the multichannelling exclusion and disinherit status for a list of pages
	 * @param ids list of page IDs
	 * @param nodeId node ID
	 * @return response containing the exclusion and disinherit status
	 * @throws Exception
	 */
	@GET
	@Path("/disinherit")
	@Override
	public MultiDisinheritResponse disinherit(@QueryParam("id") List<String> ids, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx(); ChannelTrx cTrx = new ChannelTrx(nodeId)) {
			return DisinheritRestUtil.get(Page.class, ids);
		}
	}

	/**
	 * Set the multichannelling exclusion and disinherit status for a list of pages
	 * @param ids list of page IDs
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
			return Operator.execute(new CNI18nString("inheritance.change").toString(), waitMs, new Callable<GenericResponse>() {
				@Override
				public GenericResponse call() throws Exception {
					try (ChannelTrx trx = new ChannelTrx(TransactionManager.getCurrentTransaction().getObject(Node.class, nodeId));
							InstantPublishingTrx ipTrx = new InstantPublishingTrx(false)) {
						return DisinheritRestUtil.set(Page.class, ids, request);
					}
				}
			});
		}
	}

}
