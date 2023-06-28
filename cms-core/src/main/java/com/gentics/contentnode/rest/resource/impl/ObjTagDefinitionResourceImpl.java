package com.gentics.contentnode.rest.resource.impl;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.load;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.filters.Authenticated;
import com.gentics.contentnode.rest.filters.RequiredFeature;
import com.gentics.contentnode.rest.filters.RequiredPerm;
import com.gentics.contentnode.rest.model.ObjTagSyncStatus;
import com.gentics.contentnode.rest.model.objtag.SyncItem;
import com.gentics.contentnode.rest.model.objtag.SyncItemList;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.resource.impl.internal.JobController;
import com.gentics.contentnode.rest.resource.impl.internal.JobProgress;
import com.gentics.contentnode.rest.resource.impl.internal.JobStatus;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.lib.etc.StringUtils;

/**
 * Resource for object property administration
 */
@Produces({ MediaType.APPLICATION_JSON })
@Authenticated
@Path("/prop")
public class ObjTagDefinitionResourceImpl {
	/**
	 * Progress of the running or last sync job
	 */
	protected static JobProgress progress;

	/**
	 * Job Controller for the sync status job
	 */
	protected static JobController controller = new JobController("Sync Status", "objTagSyncStatus", () -> {
		Queue<Integer> objTagIds = supply(t -> {
			Queue<Integer> queue = new LinkedList<>();
			// first get all object property definitions, that are synchronized
			List<ObjectTagDefinition> objTagDefs = t.getObjects(ObjectTagDefinition.class, DBUtils.select(
					"SELECT objtag_id id FROM objprop WHERE sync_contentset = ? OR sync_channelset = ? OR sync_variants = ?",
					ps -> {
						ps.setInt(1, 1);
						ps.setInt(2, 1);
						ps.setInt(3, 1);
					}, DBUtils.IDS));

			// for every definition, get all tags, that are unchecked
			for (ObjectTagDefinition def : objTagDefs) {
				queue.addAll(DBUtils.select("SELECT id FROM objtag WHERE obj_type = ? AND name = ? AND obj_id != ? AND in_sync = ?", ps -> {
					ps.setInt(1, def.getTargetType());
					ps.setString(2, def.getObjectTag().getName());
					ps.setInt(3, 0);
					ps.setInt(4, 0);
				}, DBUtils.IDS));
			}
			return queue;
		});
		progress = new JobProgress().setDone(0).setTotal(objTagIds.size()).setStarted((int) (System.currentTimeMillis() / 1000L));

		// check all tags
		while (!objTagIds.isEmpty()) {
			Integer id = objTagIds.remove();

			Trx.operate(t -> {
				ObjectTag tag = t.getObject(ObjectTag.class, id);
				if (tag != null) {
					Set<Integer> checked = tag.checkSync();
					objTagIds.removeAll(checked);
					progress.incDone(checked.size());
				}
			});
		}

		int finishTime = (int)(System.currentTimeMillis() / 1000L);
		progress.setFinished(finishTime);
	}).setSingleTransaction(false).setProgressSupplier(() -> progress);

	/**
	 * Get synchronization info
	 * @return sync info
	 * @throws NodeException
	 */
	@GET
	@Path("/sync/info")
	@RequiredFeature(Feature.OBJTAG_SYNC)
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_OBJPROP_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public ObjTagSyncStatus syncStatus() throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			int inSync = 0;
			int outOfSync = 0;
			int unchecked = 0;

			Transaction t = trx.getTransaction();
			// first get all object property definitions, that are synchronized
			List<ObjectTagDefinition> objTagDefs = t.getObjects(ObjectTagDefinition.class, DBUtils.select(
					"SELECT objtag_id id FROM objprop WHERE sync_contentset = ? OR sync_channelset = ? OR sync_variants = ?",
					ps -> {
						ps.setInt(1, 1);
						ps.setInt(2, 1);
						ps.setInt(3, 1);
					}, DBUtils.IDS));

			// for every object property definition, count the tags with their status
			for (ObjectTagDefinition def : objTagDefs) {
				Map<Integer, Integer> counts = DBUtils.select("SELECT in_sync, count(*) c FROM objtag WHERE obj_type = ? AND name = ? AND obj_id != ? GROUP BY in_sync",
						ps -> {
							ps.setInt(1, def.getTargetType());
							ps.setString(2, def.getObjectTag().getName());
							ps.setInt(3, 0);
						}, rs -> {
							Map<Integer, Integer> temp = new HashMap<>();
							while (rs.next()) {
								temp.put(rs.getInt("in_sync"), rs.getInt("c"));
							}
							return temp;
						});
				inSync += counts.getOrDefault(1, 0);
				outOfSync += counts.getOrDefault(-1, 0);
				unchecked += counts.getOrDefault(0, 0);
			}

			return new ObjTagSyncStatus(ResponseInfo.ok("")).setInSync(inSync).setOutOfSync(outOfSync).setUnchecked(unchecked);
		}
	}

	/**
	 * Get the tags, which are out of sync
	 * @param paging paging parameter bean
	 * @return list of tags out of sync
	 * @throws NodeException
	 */
	@GET
	@Path("/sync/tags")
	@RequiredFeature(Feature.OBJTAG_SYNC)
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_OBJPROP_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public SyncItemList getTags(@BeanParam PagingParameterBean paging) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			Transaction t = trx.getTransaction();
			List<ObjectTag> tags = t.getObjects(ObjectTag.class, DBUtils.select("SELECT id FROM objtag WHERE in_sync = -1", DBUtils.IDS));
			Set<Integer> handledIds = new HashSet<>();

			List<SyncItem> list = new ArrayList<>();

			int itemCounter = 0;
			int offset = 0;
			int length = -1;
			if (paging != null && paging.pageSize >= 0) {
				offset = (paging.page - 1) * paging.pageSize;
				length = paging.pageSize;
			}
			int from = Math.min(Math.max(0, offset), tags.size());
			int to = (length < 0) ? tags.size() : Math.min(from + length, tags.size());

			for (ObjectTag tag : tags) {
				if (handledIds.contains(tag.getId())) {
					continue;
				}
				List<Pair<NodeObject, ObjectTag>> syncVariants = new ArrayList<>(tag.getSyncVariants());
				Collections.sort(syncVariants, (pair1, pair2) ->  {
					NodeObject o1 = pair1.getKey();
					NodeObject o2 = pair2.getKey();

					// first sort by channel Name
					if (o1 instanceof LocalizableNodeObject<?> && o2 instanceof LocalizableNodeObject<?>) {
						LocalizableNodeObject<?> loc1 = (LocalizableNodeObject<?>) o1;
						LocalizableNodeObject<?> loc2 = (LocalizableNodeObject<?>) o2;

						try (NoMcTrx noMc = new NoMcTrx()) {
							Node n1 = loc1.getChannel();
							if (n1 == null) {
								n1 = loc1.getOwningNode();
							}
							Node n2 = loc2.getChannel();
							if (n2 == null) {
								n2 = loc2.getOwningNode();
							}

							int tmpCmp = StringUtils.mysqlLikeCompare(n1.getFolder().getName(), n2.getFolder().getName());
							if (tmpCmp != 0) {
								return tmpCmp;
							}
						} catch (NodeException e) {
							return 0;
						}
					}

					// second sort by language name
					if (o1 instanceof Page && o2 instanceof Page) {
						Page p1 = (Page) o1;
						Page p2 = (Page) o2;
						try {
							ContentLanguage lang1 = p1.getLanguage();
							ContentLanguage lang2 = p2.getLanguage();

							int tmpCmp = StringUtils.mysqlLikeCompare(lang1 != null ? lang1.getCode() : "", lang2 != null ? lang2.getCode() : "");
							if (tmpCmp != 0) {
								return tmpCmp;
							}
						} catch (NodeException e) {
							return 0;
						}
					}

					// third sort by object name
					if (o1 instanceof LocalizableNodeObject<?> && o2 instanceof LocalizableNodeObject<?>) {
						LocalizableNodeObject<?> loc1 = (LocalizableNodeObject<?>) o1;
						LocalizableNodeObject<?> loc2 = (LocalizableNodeObject<?>) o2;

						return StringUtils.mysqlLikeCompare(loc1.getName(), loc2.getName());
					}

					return 0;
				});
				handledIds.addAll(syncVariants.stream().map(pair -> pair.getValue().getId()).collect(Collectors.toSet()));

				for (Pair<NodeObject, ObjectTag> pair : syncVariants) {
					NodeObject container = pair.getKey();
					ObjectTag syncTag = pair.getValue();
					String name = syncTag.getDisplayName();
					if (!Objects.equals(name, syncTag.getName())) {
						name = name + " (" + syncTag.getName() + ")";
					}
					SyncItem syncItem = new SyncItem().setGroupId(tag.getId()).setId(syncTag.getId())
							.setName(name).setObjId(container.getId()).setObjType(container.getTType())
							.setObjName(I18NHelper.getName(container)).setObjPath(I18NHelper.getPath(container));
					if (container instanceof Page) {
						ContentLanguage language = ((Page) container).getLanguage();
						if (language != null) {
							syncItem.setObjLanguage(language.getCode());
						}
					}
					if (itemCounter >= from && itemCounter < to) {
						list.add(syncItem);
					}
					itemCounter++;
					if (itemCounter >= to) {
						break;
					}
				}
				if (itemCounter >= to) {
					break;
				}
			}

			SyncItemList response = new SyncItemList();
			response.setHasMoreItems(to < tags.size());
			response.setItems(list);
			response.setNumItems(tags.size());
			response.setResponseInfo(ResponseInfo.ok(""));
			return response;
		}
	}

	/**
	 * Synchronize the given tag with all other variants
	 * @param id object tag id
	 * @return generic response
	 * @throws NodeException
	 */
	@POST
	@Path("/sync/tags/{id}")
	@RequiredFeature(Feature.OBJTAG_SYNC)
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_OBJPROP_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public GenericResponse syncTag(@PathParam("id") String id) throws NodeException {
		try (Trx trx = ContentNodeHelper.trx()) {
			ObjectTag tag = load(ObjectTag.class, id);
			tag.sync();
			trx.success();
			return new GenericResponse(null, ResponseInfo.ok(""));
		}
	}

	/**
	 * Get the status of the sync checking job
	 * @return status response
	 * @throws NodeException
	 */
	@GET
	@Path("/sync")
	@RequiredFeature(Feature.OBJTAG_SYNC)
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_OBJPROP_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public JobStatus getSyncJobStatus() throws NodeException {
		return controller.getJobStatus();
	}

	/**
	 * Start the sync checking job (if not running)
	 * @return status response
	 * @throws NodeException
	 */
	@POST
	@Path("/sync")
	@RequiredFeature(Feature.OBJTAG_SYNC)
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_OBJPROP_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public JobStatus startSync() throws NodeException {
		return controller.start();
	}

	/**
	 * Stop the sync checking job
	 * @return generic response
	 * @throws NodeException
	 */
	@DELETE
	@Path("/sync")
	@RequiredFeature(Feature.OBJTAG_SYNC)
	@RequiredPerm(type = PermHandler.TYPE_ADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_CONADMIN, bit = PermHandler.PERM_VIEW)
	@RequiredPerm(type = PermHandler.TYPE_OBJPROP_MAINTENANCE, bit = PermHandler.PERM_VIEW)
	public GenericResponse stopSync() throws NodeException {
		return controller.stop();
	}
}
