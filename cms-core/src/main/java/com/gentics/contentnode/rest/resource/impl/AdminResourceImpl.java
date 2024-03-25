package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.checkBody;
import static com.gentics.contentnode.rest.util.MiscUtils.comparator;
import static com.gentics.contentnode.rest.util.MiscUtils.filterByPermission;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.db.DBUtils.PrepareStatement;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.MaintenanceMode;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.events.EventQueueQuery;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.QueueEntry;
import com.gentics.contentnode.events.QueueEntryType;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.jmx.PublisherInfo;
import com.gentics.contentnode.log.Action;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.log.ActionLogger.LogQuery;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.publish.GetPublisherInfo;
import com.gentics.contentnode.publish.PublishQueueStats;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.CmpCompatibility;
import com.gentics.contentnode.rest.model.CmpProduct;
import com.gentics.contentnode.rest.model.CmpVersionInfo;
import com.gentics.contentnode.rest.model.ContentMaintenanceAction;
import com.gentics.contentnode.rest.model.ContentMaintenanceType;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.DirtQueueSummaryEntry;
import com.gentics.contentnode.rest.model.request.ContentMaintenanceActionRequest;
import com.gentics.contentnode.rest.model.request.MaintenanceModeRequest;
import com.gentics.contentnode.rest.model.response.DirtQueueEntryList;
import com.gentics.contentnode.rest.model.response.DirtQueueSummaryResponse;
import com.gentics.contentnode.rest.model.response.FeatureResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.MaintenanceResponse;
import com.gentics.contentnode.rest.model.response.PublishQueueCounts;
import com.gentics.contentnode.rest.model.response.PublishQueueResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.model.response.VersionResponse;
import com.gentics.contentnode.rest.model.response.admin.CustomTool;
import com.gentics.contentnode.rest.model.response.admin.PublishInfoResponse;
import com.gentics.contentnode.rest.model.response.admin.ToolsResponse;
import com.gentics.contentnode.rest.model.response.admin.UpdatesInfoResponse;
import com.gentics.contentnode.rest.model.response.log.ActionLogEntryList;
import com.gentics.contentnode.rest.model.response.log.ActionLogType;
import com.gentics.contentnode.rest.model.response.log.ActionLogTypeList;
import com.gentics.contentnode.rest.model.response.log.ActionModel;
import com.gentics.contentnode.rest.model.response.log.ActionModelList;
import com.gentics.contentnode.rest.model.response.log.ErrorLogEntry;
import com.gentics.contentnode.rest.model.response.log.ErrorLogEntryList;
import com.gentics.contentnode.rest.resource.AdminResource;
import com.gentics.contentnode.rest.resource.parameter.ActionLogParameterBean;
import com.gentics.contentnode.rest.resource.parameter.DirtQueueParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.CmpVersionUtils;
import com.gentics.contentnode.rest.util.ListBuilder;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.rest.version.Main;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.runtime.ReloadConfigurationTask;
import com.gentics.contentnode.update.AutoUpdate;
import com.gentics.contentnode.update.CMSVersion;
import com.gentics.contentnode.version.CmpProductVersion;
import com.gentics.contentnode.version.CmpVersionRequirement;
import com.gentics.contentnode.version.CmpVersionRequirements;
import com.gentics.lib.log.NodeLogger;

import io.reactivex.Flowable;

/**
 * Resource for various tasks used by the administrator (like retrieving version numbers)
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class AdminResourceImpl implements AdminResource {

	private static final NodeLogger logger = NodeLogger.getNodeLogger(AdminResourceImpl.class);

	/**
	 * CMP version requirements for this CMS version.
	 *
	 * @see #getVersionRequirement(CmpProductVersion)
	 */
	CmpVersionRequirement versionRequirement;

	@Override
	@GET
	@Path("/version")
	public VersionResponse currentVersion() throws NodeException {
		VersionResponse response = new VersionResponse();
		String implementationVersion = Main.getImplementationVersion();

		try {
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, "Server version successfully retrieved."));
			response.setVersion(implementationVersion);
		} catch (Exception e) {
			response.setResponseInfo(new ResponseInfo(ResponseCode.FAILURE, "Failed to retrieve server version!"));
		}

		// This assumes that there is a one-to-one relation between CMS and CMP versions.
		CmpVersionRequirement req = getVersionRequirement(new CmpProductVersion(implementationVersion));
		Map<String, CmpVersionInfo> nodeInfo = new HashMap<>();

		if (req != null) {
			response.setCmpVersion(req.getCmpVersion());
		}

		try (Trx trx = ContentNodeHelper.trx()) {
			Set<Integer> ids = DBUtils.select("SELECT id FROM contentrepository", DBUtils.IDS);
			List<ContentRepository> crs = trx.getTransaction().getObjects(ContentRepository.class, ids);

			for (ContentRepository cr : crs) {
				if (!ObjectPermission.view.checkObject(cr) || cr.getCrType() != Type.mesh) {
					continue;
				}

				try (MeshPublisher meshPublisher = new MeshPublisher(cr, true)) {
					CmpProductVersion meshVersion = meshPublisher.getMeshServerVersion();

					for (Node node : cr.getNodes()) {
						if (!ObjectPermission.view.checkObject(node)) {
							continue;
						}

						CmpVersionInfo versionInfo;
						try {
							versionInfo = CmpVersionUtils.createVersionInfo(
								req,
								meshVersion,
								CmpVersionUtils.getPortalVersion(node.getEffectiveMeshPreviewUrl()));
						} catch (Exception e) {
							logger.error("Could not get version for the node {" + node.getMeshPreviewUrl() + "}", e);
							versionInfo = new CmpVersionInfo().setCompatibility(CmpCompatibility.UNKNOWN);
						}

						nodeInfo.put(node.getFolder().getName(), versionInfo);
					}
				} catch (Exception e) {
					logger.error("Could not initialize Mesh CR {" + cr.getName() + "}", e);
				}
			}
		}

		response.setNodeInfo(nodeInfo);

		return response;
	}

	@Override
	@GET
	@Path("/features/{name}")
	public FeatureResponse featureInfo(@PathParam("name") String name) {
		try {
			Feature feature = Feature.valueOf(name.toUpperCase());
			return new FeatureResponse(new ResponseInfo(ResponseCode.OK, ""), name,
					feature.isActivated());
		} catch (IllegalArgumentException e) {
			return new FeatureResponse(new ResponseInfo(ResponseCode.OK, ""), name,
					NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences().getFeature(name));
		}
	}

	@Override
	@GET
	@Path("/tools")
	public ToolsResponse tools() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			NodePreferences prefs = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences();
			Collection<?> customTools = ObjectTransformer.getCollection(prefs.getPropertyObject("custom_tools"),
					Collections.emptyList());
			PermHandler permHandler = TransactionManager.getCurrentTransaction().getPermHandler();

			boolean admin = permHandler.checkPermissionBit(PermHandler.TYPE_CUSTOM_TOOLS, null, PermHandler.PERM_VIEW)
					&& permHandler.checkPermissionBit(PermHandler.TYPE_CUSTOM_TOOLS, null, PermHandler.PERM_CHANGE_PERM);

			List<CustomTool> tools = customTools.stream().map(o -> {
				if (o instanceof Map) {
					return ModelBuilder.getCustomTool((Map<?, ?>)o);
				} else {
					return null;
				}
			}).filter(tool -> tool != null).filter(tool -> admin || permHandler.checkPermissionBit(PermHandler.TYPE_CUSTOM_TOOL, tool.getId(), PermHandler.PERM_VIEW))
					.collect(Collectors.toList());

			ToolsResponse response = new ToolsResponse();
			response.setTools(tools);
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/publishInfo")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	public PublishInfoResponse publishInfo() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			PublishInfoResponse response = PublisherResourceImpl.getPublishInfo();

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/updates")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_AUTOUPDATE, bit = PermHandler.PERM_VIEW)
	public UpdatesInfoResponse updatesAvailable() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			UpdatesInfoResponse response = new UpdatesInfoResponse();
			response.setResponseInfo(new ResponseInfo(ResponseCode.OK, ""));

			AutoUpdate autoUpdate = new AutoUpdate(5);
			response.setAvailable(autoUpdate.getAvailableVersions().stream().map(CMSVersion::toString).collect(Collectors.toList()));

			trx.success();
			return response;
		}
	}

	@Override
	@GET
	@Path("/publicKey")
	public JsonNode publicKey() throws NodeException {
		return KeyProvider.getPublicKey();
	}

	@Override
	@GET
	@Path("/actionlog")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_ACTIONLOG, bit = PermHandler.PERM_VIEW)
	public ActionLogEntryList getActionLog(@BeanParam PagingParameterBean paging, @BeanParam ActionLogParameterBean param) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			LogQuery query = new ActionLogger.LogQuery().page(paging).query(param);

			ActionLogEntryList list = new ActionLogEntryList();
			list.setItems(Flowable.fromIterable(query.get()).map(ActionLogger.Log.TRANSFORM2REST::apply).toList().blockingGet());
			list.setNumItems(query.count());
			list.setHasMoreItems(query.hasMore(list.getNumItems()));
			list.setResponseInfo(ResponseInfo.ok(""));

			trx.success();
			return list;
		}
	}

	@Override
	@GET
	@Path("/actionlog/types")
	public ActionLogTypeList getActionLogTypes(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<ActionLogType> logTypes = Flowable.fromIterable(ActionLogger.LOGGED_TYPES)
					.map(TypePerms.TRANSFORM2REST::apply).toList().blockingGet();
			trx.success();
			return ListBuilder.from(logTypes, m -> m).filter(MiscUtils.filter(filter, "name", "label"))
					.sort(comparator(sorting, "name", "label")).page(paging).to(new ActionLogTypeList());
		}
	}

	@Override
	@GET
	@Path("/actionlog/actions")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_ACTIONLOG, bit = PermHandler.PERM_VIEW)
	public ActionModelList getActionLogActions(@BeanParam FilterParameterBean filter, @BeanParam SortParameterBean sorting,
			@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<ActionModel> actionModels = Arrays.asList(Action.values()).stream().map(Action.TRANSFORM2REST).collect(Collectors.toList());
			trx.success();
			return ListBuilder.from(actionModels, m -> m).filter(MiscUtils.filter(filter, "name", "label"))
					.sort(comparator(sorting, "name", "label")).page(paging).to(new ActionModelList());
		}
	}

	@Override
	@GET
	@Path("/errorlog")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_ERRORLOG, bit = PermHandler.PERM_VIEW)
	public ErrorLogEntryList getErrorLog(@BeanParam FilterParameterBean filter, @BeanParam PagingParameterBean paging)
			throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ErrorLogEntryList list = new ErrorLogEntryList();

			PrepareStatement prepare = stmt -> {
				if (filter != null && !StringUtils.isBlank(filter.query)) {
					int paramCounter = 1;
					String pattern = "%" + filter.query + "%";
					stmt.setString(paramCounter++, pattern);
					stmt.setString(paramCounter++, pattern);
					stmt.setString(paramCounter++, pattern);
				}
			};

			StringBuilder countSql = new StringBuilder();
			countSql.append("SELECT COUNT(*) c FROM logerror");
			if (filter != null && !StringUtils.isBlank(filter.query)) {
				countSql.append(" LEFT JOIN systemuser ON (logerror.user_id = systemuser.id)");
				countSql.append(" WHERE (LOWER(systemuser.login) LIKE ? OR LOWER(systemuser.firstname) LIKE ? OR LOWER(systemuser.lastname) LIKE ?)");
			}

			int count = DBUtils.select(countSql.toString(), prepare, DBUtils.firstInt("c"));

			if (count == 0) {
				list.setItems(Collections.emptyList());
				list.setHasMoreItems(false);
				list.setNumItems(0);
			} else {
				boolean hasMore = false;
				StringBuilder sql = new StringBuilder();
				sql.append("SELECT logerror.* FROM logerror");
				if (filter != null && !StringUtils.isBlank(filter.query)) {
					sql.append(" LEFT JOIN systemuser ON (logerror.user_id = systemuser.id)");
					sql.append(" WHERE (LOWER(systemuser.login) LIKE ? OR LOWER(systemuser.firstname) LIKE ? OR LOWER(systemuser.lastname) LIKE ?)");
				}

				sql.append(" ORDER BY logerror.timestamp DESC");

				if (paging != null && paging.pageSize > 0 && paging.page > 0) {
					int start = (Math.max(paging.page, 1) - 1) * paging.pageSize;
					int pageSize = paging.pageSize;
					sql.append(String.format(" LIMIT %d, %d", start, pageSize));

					hasMore = count > start + pageSize;
				}
				list.setItems(DBUtils.select(sql.toString(), prepare, rs -> {
					Transaction t = TransactionManager.getCurrentTransaction();
					List<ErrorLogEntry> items = new ArrayList<>();
					while (rs.next()) {
						SystemUser user = t.getObject(SystemUser.class, rs.getInt("user_id"));
						String userName = user == null ? "[unknown]"
								: String.format("%s %s", user.getLastname(), user.getFirstname());
						items.add(new ErrorLogEntry().setId(rs.getInt("id")).setSid(rs.getString("sid"))
								.setUser(userName).setHaltId(rs.getInt("halt_id")).setRequest(rs.getString("request"))
								.setErrorDo(rs.getInt("errordo")).setTimestamp(rs.getInt("timestamp"))
								.setDetail(rs.getString("detail")).setStacktrace(rs.getString("stacktrace")));
					}
					return items;
				}));
				list.setNumItems(count);
				list.setHasMoreItems(hasMore);
			}

			list.setResponseInfo(ResponseInfo.ok(""));
			trx.success();
			return list;
		}
	}

	@Override
	@GET
	@Path("/content/publishqueue")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public PublishQueueResponse getPublishQueue() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			// get all nodes, the user is allowed to see
			List<Node> nodes = new ArrayList<>(trx.getTransaction().getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS)));
			filterByPermission(nodes);

			PublishQueueResponse response = new PublishQueueResponse();
			if (nodes.isEmpty()) {
				response.setNodes(Collections.emptyMap());
			} else {
				// get the publisher info from the master, since the publish process will always run on the master
				PublisherInfo publisherInfo;
				try {
					publisherInfo = DistributionUtil.call(new GetPublisherInfo());
				} catch (Exception e) {
					throw new NodeException(e);
				}

				PublishQueueStats stats = PublishQueueStats.get();

				Map<Integer, PublishQueueCounts> map = nodes.stream()
					.map(Node::getId)
					.collect(
						Collectors.toMap(id -> id, id -> stats.counts(id, publisherInfo)));
				response.setNodes(map);
			}

			response.setResponseInfo(ResponseInfo.ok("Successfully fetched dirt queue info"));

			trx.success();

			return response;
		}
	}

	@Override
	@POST
	@Path("/content/publishqueue")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public GenericResponse performContentMaintenanceAction(ContentMaintenanceActionRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBody(request, b -> Pair.of("action", b.getAction()));
			int timestamp = (int)(trx.getTransaction().getTimestamp() / 1000);

			// get all nodes, the user is allowed to see
			List<Node> nodes = new ArrayList<>(trx.getTransaction().getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS)));
			filterByPermission(nodes);

			// filter nodes by given node IDs
			if (request.getNodes() != null) {
				nodes.removeIf(node -> !request.getNodes().contains(node.getId()));
			}

			// filter nodes by given cr IDs
			if (request.getContentRepositories() != null) {
				nodes.removeIf(node -> !request.getContentRepositories().contains(node.getContentrepositoryId()));
			}

			if (nodes.isEmpty()) {
				trx.success();
				return new GenericResponse(null, ResponseInfo.ok(""));
			}

			List<String> properties = new ArrayList<>();
			// add node IDs
			properties.add(nodes.stream().map(node -> Integer.toString(node.getId())).collect(Collectors.joining("|")));

			// add creation time restriction
			if (request.getStart() != null || request.getEnd() != null) {
				properties.add("cdate");
				properties.add(ObjectTransformer.getString(request.getStart(), "0"));
				properties.add(ObjectTransformer.getString(request.getEnd(), Integer.toString(timestamp)));
			} else {
				properties.add("-");
				properties.add("-1");
				properties.add("-1");
			}

			// add flag to clear publish cache (dirt as "modified")
			if (request.getAction() == ContentMaintenanceAction.dirt
					&& NodeConfigRuntimeConfiguration.isFeature(Feature.PUBLISH_CACHE)
					&& ObjectTransformer.getBoolean(request.getClearPublishCache(), false)) {
				properties.add("modify");
			} else {
				properties.add("dependency");
			}

			// add attributes to dirt
			if (request.getAction() == ContentMaintenanceAction.dirt
					&& NodeConfigRuntimeConfiguration.isFeature(Feature.ATTRIBUTE_DIRTING)) {
				if (!ObjectTransformer.isEmpty(request.getAttributes())) {
					properties.add(request.getAttributes().stream().filter(a -> !StringUtils.isBlank(a))
							.map(a -> a.trim()).collect(Collectors.joining("|")));
				} else {
					properties.add("");
				}
			}

			for (ContentMaintenanceType type : ContentMaintenanceType.values()) {
				if (request.getTypes() != null && !request.getTypes().contains(type)) {
					continue;
				}

				int eventMask = 0;
				String info = null;
				switch (request.getAction()) {
				case delay:
					eventMask = Events.MAINTENANCE_DELAY;
					info = "Delay publish";
					break;
				case dirt:
					eventMask = Events.MAINTENANCE_PUBLISH;
					info = "Republish";
					break;
				case markPublished:
					eventMask = Events.MAINTENANCE_MARKPUBLISHED;
					info = "Mark objects published";
					break;
				case publishDelayed:
					eventMask = Events.MAINTENANCE_REPUBLISH;
					info = "Republish delayed objects";
					break;
				default:
					break;
				}
				QueueEntry entry = new QueueEntry(timestamp, 0, type.getCode(), eventMask,
						(String[]) properties.toArray(new String[properties.size()]), 0,
						trx.getTransaction().getSessionId());
				entry.store(ContentNodeFactory.getInstance());
				ActionLogger.logCmd(ActionLogger.MAINTENANCE, type.getCode(), 0, 0, info);
			}

			trx.success();

			return new GenericResponse(null, ResponseInfo.ok(""));
		}
	}

	@Override
	@GET
	@Path("/content/dirtqueue")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public DirtQueueEntryList getDirtQueue(@BeanParam PagingParameterBean paging, @BeanParam DirtQueueParameterBean filter) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			EventQueueQuery query = new EventQueueQuery().page(paging).query(filter);

			DirtQueueEntryList list = new DirtQueueEntryList();
			list.setItems(Flowable.fromIterable(query.get()).map(QueueEntry.TRANSFORM2REST::apply).toList().blockingGet());
			list.setNumItems(query.count());
			list.setHasMoreItems(query.hasMore(list.getNumItems()));
			list.setResponseInfo(ResponseInfo.ok(""));

			trx.success();
			return list;
		}
	}

	@Override
	@GET
	@Path("/content/dirtqueue/summary")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public DirtQueueSummaryResponse getDirtQueueSummary() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			List<DirtQueueSummaryEntry> entries = DBUtils.select("SELECT * FROM dirtqueue WHERE failed = 0 ORDER BY timestamp ASC", rs -> {
				List<DirtQueueSummaryEntry> list = new ArrayList<>();
				QueueEntryType lastType = QueueEntryType.unset;
				DirtQueueSummaryEntry lastSummary = null;
				while (rs.next()) {
					QueueEntry entry = new QueueEntry(rs);
					QueueEntryType type = entry.getType();
					if (type == QueueEntryType.log) {
						continue;
					}

					if (lastType != type || lastSummary == null) {
						lastSummary = new DirtQueueSummaryEntry().setLabel(type.getLabel());
						list.add(lastSummary);
						lastType = type;
					}
					lastSummary.setCount(lastSummary.getCount() + 1);
				}
				return list;
			});

			DirtQueueSummaryResponse response = ListBuilder.from(entries, e -> e).to(new DirtQueueSummaryResponse());

			trx.success();
			return response;
		}
	}

	@Override
	@DELETE
	@Path("/content/dirtqueue/{id}")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_CONTENT_MAINTENANCE_UPDATE)
	public Response deleteDirtQueueEntry(@PathParam("id") int entryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			QueueEntry entry = DBUtils.select("SELECT * FROM dirtqueue WHERE id = ?", stmt -> {
				stmt.setInt(1, entryId);
			}, rs -> {
				if (rs.next()) {
					return new QueueEntry(rs);
				} else {
					return null;
				}
			});

			if (entry == null) {
				throw new EntityNotFoundException(I18NHelper.get("dirtqueue.notfound", Integer.toString(entryId)));
			}

			if (!entry.isFailed()) {
				throw new RestMappedException(I18NHelper.get("dirtqueue.delete.notfailed", Integer.toString(entryId)))
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			DBUtils.update("DELETE FROM dirtqueue WHERE id = ?", entryId);

			trx.success();
			return Response.noContent().build();
		}
	}

	@DELETE
	@Path("/content/dirtqueue")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_CONTENT_MAINTENANCE_UPDATE)
	public Response deleteDirtQueueEntries(@BeanParam DirtQueueParameterBean filter) throws NodeException {
		return supply(() -> {
			EventQueueQuery query = new EventQueueQuery().query(filter);
			query.delete();

			return Response.noContent().build();
		});
	}


	@Override
	@PUT
	@Path("/content/dirtqueue/{id}/redo")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_CONTENT_MAINTENANCE_UPDATE)
	public Response redoDirtQueueEntry(@PathParam("id") int entryId) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			QueueEntry entry = DBUtils.select("SELECT * FROM dirtqueue WHERE id = ?", stmt -> {
				stmt.setInt(1, entryId);
			}, rs -> {
				if (rs.next()) {
					return new QueueEntry(rs);
				} else {
					return null;
				}
			});

			if (entry == null) {
				throw new EntityNotFoundException(I18NHelper.get("dirtqueue.notfound", Integer.toString(entryId)));
			}

			if (!entry.isFailed()) {
				throw new RestMappedException(I18NHelper.get("dirtqueue.redo.notfailed", Integer.toString(entryId)))
						.setResponseCode(ResponseCode.INVALIDDATA).setStatus(Status.CONFLICT);
			}

			int timestamp = (int) (trx.getTransaction().getTimestamp() / 1000);
			DBUtils.update("UPDATE dirtqueue SET failed = ?, failreason = ?, timestamp = ? WHERE id = ?", 0, "",
					timestamp, entryId);

			trx.success();
			return Response.noContent().build();
		}
	}

	@Override
	@PUT
	@Path("/config/reload")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONTENT_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public GenericResponse reloadConfiguration() throws Exception {
		try (Trx trx = ContentNodeHelper.trx()) {
			DistributionUtil.call(new ReloadConfigurationTask(), false);
			trx.success();
			return new GenericResponse(null, ResponseInfo.ok("Successfully reloaded configuration"));
		}
	}

	@Override
	@POST
	@Path("/maintenance")
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_MAINTENCANCE, bit = PermHandler.PERM_VIEW)
	public MaintenanceResponse setMaintenanceMode(MaintenanceModeRequest request) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			checkBody(request);
			MaintenanceMode.set(request.getMaintenance(), request.getMessage(), request.getBanner());

			// when maintenance mode was activated, all other user sessions will be invalidated
			if (request.getMaintenance() == Boolean.TRUE) {
				Session session = trx.getTransaction().getSession();
				if (session != null) {
					DBUtils.update("UPDATE systemsession SET secret = ? WHERE id != ?", "", session.getSessionId());
				}
			}

			MaintenanceResponse response = MaintenanceMode.TRANSFORM2REST.apply(MaintenanceMode.get());
			trx.success();
			return response;
		}
	}

	/**
	 * Load the version requirement for this CMS release from the resources.
	 *
	 * @param cmsVersion The CMS version
	 * @return The respective version requirement for the other CMP components
	 * @throws NodeException When the CMP version requirement information cannot be read
	 */
	private CmpVersionRequirement getVersionRequirement(CmpProductVersion cmsVersion) throws NodeException {
		if (versionRequirement == null) {
			CmpVersionRequirements versionRequirements;

			try {
				InputStream cmpVersions = Main.class.getResourceAsStream("/com/gentics/contentnode/rest/version/cmp-versions.json");

				versionRequirements = new ObjectMapper().readValue(cmpVersions, CmpVersionRequirements.class);
			} catch (IOException e) {
				throw new NodeException("Could not load cmp-versions.json from resources", e);
			}

			for (CmpVersionRequirement requirement : versionRequirements.getVersionRequirements()) {
				if (cmsVersion.inRange(requirement.getProductVersions().get(CmpProduct.CMS))) {
					versionRequirement = requirement;
					break;
				}
			}
		}

		return versionRequirement;
	}
}
