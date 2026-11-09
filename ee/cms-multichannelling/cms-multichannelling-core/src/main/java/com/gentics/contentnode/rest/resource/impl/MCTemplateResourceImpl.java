package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.rest.util.MiscUtils.checkChannelSyncPerm;
import static com.gentics.contentnode.rest.util.MiscUtils.checkDeletePermSome;
import static com.gentics.contentnode.rest.util.MiscUtils.getNode;
import static com.gentics.contentnode.rest.util.MiscUtils.getTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.LocalizeJob;
import com.gentics.contentnode.job.PushToMasterJob;
import com.gentics.contentnode.job.UnlocalizeJob;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.LocalizeRequest;
import com.gentics.contentnode.rest.model.request.MultiPushToMasterRequest;
import com.gentics.contentnode.rest.model.request.MultiUnlocalizeRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.LocalizationInfo;
import com.gentics.contentnode.rest.model.response.LocalizationsResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ReferencedSyncableObjectsListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.MCTemplateResource;
import com.gentics.contentnode.rest.util.MCMiscUtils;
import com.gentics.lib.i18n.CNI18nString;
import com.gentics.lib.log.NodeLogger;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/template")
@Authenticated
public class MCTemplateResourceImpl implements MCTemplateResource {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(MCTemplateResourceImpl.class);

	@Override
	@GET
	@Path("/usage/syncableObjects")
	public ReferencedSyncableObjectsListResponse getSyncableObjects(
			@QueryParam("ids") List<Integer> templateIds,
			@QueryParam("srcNodeId") Integer srcNodeId,
			@QueryParam("dstNodeId") Integer dstNodeId) throws NodeException {
		if (ObjectTransformer.isEmpty(templateIds)) {
			return new ReferencedSyncableObjectsListResponse(
				null,
				new ResponseInfo(ResponseCode.OK, "No template IDs supplied, nothing to do"));
		}

		if (ObjectTransformer.isEmpty(srcNodeId) || ObjectTransformer.isEmpty(dstNodeId)) {
			return new ReferencedSyncableObjectsListResponse(
				null,
				new ResponseInfo(ResponseCode.INVALIDDATA, "The parameters <srcNodeId> and <dstNodeId> are mandatory"));
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			ReferencedSyncableObjectsListResponse response = MCMiscUtils.getSyncableObjects(Template.class, templateIds, srcNodeId, dstNodeId);
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/push2master/{id}")
	public GenericResponse pushToMaster(@PathParam("id") Integer id, PushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("template.nopermission");
				throw new InsufficientPrivilegesException(message.toString(), null, null, Template.TYPE_TEMPLATE, request.getMasterId(), PermType.channelsync);
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(Template.class)
					.setId(id)
					.setMasterId(request.getMasterId())
					.setChannelId(request.getChannelId());
			GenericResponse response = job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/push2master")
	public GenericResponse pushToMaster(MultiPushToMasterRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(request.getMasterId(), request.getChannelId())) {
				I18nString message = new CNI18nString("template.nopermission");
				throw new InsufficientPrivilegesException(message.toString(), null, null, Template.TYPE_TEMPLATE, request.getMasterId(), PermType.channelsync);
			}
			PushToMasterJob job = new PushToMasterJob()
					.setClazz(Template.class)
					.setIds(request.getIds())
					.setMasterId(request.getMasterId())
					.setChannelId(request.getChannelId());
			GenericResponse response =  job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/localize/{id}")
	public GenericResponse localize(@PathParam("id") String id, LocalizeRequest request) throws NodeException {
		if (request == null) {
			throw new RestMappedException().setMessageType(Type.CRITICAL).setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
		}
		try (Trx trx = ContentNodeHelper.trx()) {
			Template template = getTemplate(id, ObjectPermission.view);
			Node channel = getNode(Integer.toString(request.getChannelId()), ObjectPermission.view);
			if (!channel.isChannel()) {
				I18nString message = new CNI18nString("node.nochannel");
				message.setParameter("0", channel.getFolder().getName());
				throw new RestMappedException(message.toString()).setMessageType(Type.CRITICAL)
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.BAD_REQUEST);
			}

			try (ChannelTrx cTrx = new ChannelTrx(channel)) {
				if (!PermHandler.ObjectPermission.edit.checkObject(template)) {
					I18nString message = new CNI18nString("template.nopermission");
					message.setParameter("0", id.toString());
					throw new InsufficientPrivilegesException(message.toString(), template, PermType.update);
				}
			}

			GenericResponse response = LocalizeJob.process(Template.class, Arrays.asList(template.getId()), channel.getId(),
					AbstractContentNodeResource.getJobForegroundTime(request.getForegroundTime()) * 1000);
			trx.success();
			return response;
		}
	}

	@Override
	@POST
	@Path("/unlocalize/{id}")
	public GenericResponse unlocalize(@PathParam("id") Integer id, UnlocalizeRequest request) throws NodeException {
		return unlocalize(new MultiUnlocalizeRequest(id, request));
	}

	@Override
	@POST
	@Path("/unlocalize")
	public GenericResponse unlocalize(MultiUnlocalizeRequest request) throws NodeException {
		Integer channelId = request.getChannelId();
		LinkedList<Integer> idList = new LinkedList<Integer>(request.getIds());

		try (Trx trx = ContentNodeHelper.trx()) {
			if (!checkChannelSyncPerm(channelId) && !checkDeletePermSome(channelId, Template.class, idList)) {
				I18nString message = new CNI18nString("template.nopermission");
				// TODO 
				throw new InsufficientPrivilegesException(message.toString(), null, null, Template.TYPE_TEMPLATE, 0, PermType.channelsync);
			}

			UnlocalizeJob job = new UnlocalizeJob()
					.setClazz(Template.class)
					.setIds(idList)
					.setChannelId(channelId);

			GenericResponse response = job.execute(request.getForegroundTime(), TimeUnit.SECONDS);
			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/localizationinfo")
	public LocalizationInfo getLocalizationInfo(@QueryParam("id") List<Integer> ids, @QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<LocalizableNodeObject<? extends NodeObject>> templates = new Vector<LocalizableNodeObject<? extends NodeObject>>();
			for (Integer id : ids) {
				templates.add(getTemplate(Integer.toString(id), ObjectPermission.view));
			}
			Node node = getNode(Integer.toString(nodeId), ObjectPermission.view);
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
			result.setChannels(MCMiscUtils.getLocalizationInfo(channelIds, templates));

			trx.success();
			return result;
		}
	}

	@Override
	@GET
	@Path("/localizationinfo/{id}")
	public LocalizationInfo getLocalizationInfo(@PathParam("id") String id,
			@QueryParam("nodeId") Integer nodeId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Template template = getTemplate(id, ObjectPermission.view);
			Node node = getNode(Integer.toString(nodeId), ObjectPermission.view);
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
			result.setChannels(MCMiscUtils.getLocalizationInfo(channelIds, template));

			trx.success();
			return result;
		}
	}

	@GET
	@Path("/localizations/{id}")
	@Override
	public LocalizationsResponse localizations(@PathParam("id") Integer id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Template template = t.getObject(Template.class, id);

			if (!ObjectPermission.view.checkObject(template, template.getChannel())) {
				String msg = new CNI18nString("template.nopermission").toString();

				return new LocalizationsResponse(
					new Message(Type.CRITICAL, msg),
					new ResponseInfo(ResponseCode.PERMISSION, "Not enough permissions to view this template"));
			}

			Template master = template.getMaster();
			Map<Integer, Integer> localized = MultichannellingFactory.getLocalizedCopyChannels(template);
			int total = localized.size();
			MultichannellingFactory.filterPermissions(localized.values(), template);

			String msg = new CNI18nString("rest.localizations.success").toString();
			return new LocalizationsResponse(
				new Message(Type.SUCCESS, msg),
				master.getId(),
				0,
				localized).setTotal(total).setHidden(total - localized.size());
		}
	}
}
