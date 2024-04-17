package com.gentics.contentnode.publish;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InconsistentDataException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.events.DependencyManager;
import com.gentics.contentnode.events.DependencyManager.DirtCounter;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Folder.FileSearch;
import com.gentics.contentnode.object.Folder.PageSearch;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectInFolder;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.PublishableNodeObject;
import com.gentics.contentnode.object.PublishableNodeObjectInFolder;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.response.PublishQueueCounts;
import com.gentics.contentnode.rest.model.response.admin.ObjectCount;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.db.SQLExecutorWrapper;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of the publish queue
 */
public class PublishQueue {
	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(PublishQueue.class);

	/**
	 * Batch Size for deleting finished entries from the publish queue
	 */
	protected static int batchSize = 100;

	/**
	 * ExecutorService that will remove objects from the publishqueue in batches
	 */
	protected static ExecutorService removerService = null;

	/**
	 * Threadlocal map for collecting data for "fast dependency dirting".
	 * The map is constructed as
	 * channelId -> Map(objectType -> Map(Ids -> Set(attribute names)))
	 */
	protected static ThreadLocal<Map<Integer, Map<Integer, Map<Integer, Set<String>>>>> dependencyDirting = new ThreadLocal<>();

	protected final static List<Action> REMOVING_ACTIONS = Arrays.asList(Action.DELETE, Action.REMOVE, Action.OFFLINE, Action.HIDE);

	/**
	 * Initialize fast dependency dirting by preparing the threadlocal dependencyDirting map
	 */
	public static void initFastDependencyDirting() {
		if (dependencyDirting.get() == null) {
			logger.info("Initializing fast dependency dirting");
			dependencyDirting.set(new HashMap<Integer, Map<Integer, Map<Integer, Set<String>>>>());
		}
	}

	/**
	 * Finish fast dependency dirting
	 */
	public static void finishFastDependencyDirting() throws NodeException {
		Map<Integer, Map<Integer, Map<Integer, Set<String>>>> dirtMap = dependencyDirting.get();
		if (dirtMap == null) {
			return;
		}

		logger.info("Finishing fast dependency dirting");

		for (Map.Entry<Integer, Map<Integer, Map<Integer, Set<String>>>> channelEntry : dirtMap.entrySet()) {
			final int channelId = channelEntry.getKey();
			Map<Integer, Map<Integer, Set<String>>> dirtMapForChannel = channelEntry.getValue();

			for (Map.Entry<Integer, Map<Integer, Set<String>>> typeEntry : dirtMapForChannel.entrySet()) {
				final int objType = typeEntry.getKey();
				Map<Integer, Set<String>> objIdMap = typeEntry.getValue();

				if (objIdMap.isEmpty()) {
					continue;
				}

				// get possibly existing publishqueue entries
				final List<Entry> existingEntries = new ArrayList<PublishQueue.Entry>(1);

				// check which of the entries already exists
				DBUtils.executeMassStatement("SELECT * FROM publishqueue WHERE obj_type = ? AND action = ? AND channel_id = ? AND obj_id IN", new ArrayList<>(
						objIdMap.keySet()), 4, new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt)
							throws SQLException {
						stmt.setInt(1, objType); // obj_type = ?
						stmt.setString(2, Action.DEPENDENCY.toString()); // action = ?
						stmt.setInt(3, channelId); // channel_id = ?
					}

					@Override
					public void handleResultSet(ResultSet rs)
							throws SQLException, NodeException {
						while (rs.next()) {
							existingEntries.add(new Entry(rs));
						}
					}
				});

				Set<Integer> undelayIds = new HashSet<Integer>();
				for (Entry entry : existingEntries) {
					setAttributes(entry.id, true, objIdMap.get(entry.getObjId()));
					objIdMap.remove(entry.getObjId());
					if (entry.isDelayed()) {
						undelayIds.add(entry.getObjId());
					}
				}

				if (!undelayIds.isEmpty()) {
					DBUtils.executeMassStatement("UPDATE publishqueue SET delay = ? WHERE obj_type = ? AND channel_id = ? AND obj_id IN", null,
							new ArrayList<Integer>(undelayIds), 4, new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt)
								throws SQLException {
							stmt.setInt(1, 0); // delay = ?
							stmt.setInt(2, objType); // obj_type = ?
							stmt.setInt(3, channelId); // channel_id = ?
						}
					}, Transaction.UPDATE_STATEMENT);
				}

				if (!objIdMap.isEmpty()) {
					Transaction t = TransactionManager.getCurrentTransaction();

					// increase the dirt counter
					DirtCounter counter = DependencyManager.getDirtCounter();

					List<Integer> entryIds = new ArrayList<>();
					List<Set<String>> attributes = new ArrayList<>();
					List<Object[]> argsList = new ArrayList<>();

					for (Map.Entry<Integer, Set<String>> entry : objIdMap.entrySet()) {
						argsList.add(new Object[] {objType, entry.getKey(), Action.DEPENDENCY.toString(), channelId, t.getUnixTimestamp()});
						attributes.add(entry.getValue());
						if (counter != null) {
							counter.inc();
						}
					}
					DBUtils.executeBatchStatement("INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) VALUES (?, ?, ?, ?, ?)",
							Transaction.INSERT_STATEMENT, argsList, s -> {
								ResultSet keys = s.getGeneratedKeys();
								while (keys.next()) {
									entryIds.add(keys.getInt(1));
								}
							});
					if (entryIds.size() != attributes.size()) {
						throw new NodeException("Error while fast dependency dirting: expected " + attributes.size() + " entries, but inserted "
								+ entryIds.size() + " instead");
					}
					for (int i = 0; i < entryIds.size(); i++) {
						setAttributes(entryIds.get(i), false, attributes.get(i));
					}
				}
			}
		}

		logger.info("Finished fast dependency dirting");

		dependencyDirting.remove();
	}

	/**
	 * Cancel fast dependency dirting by just disposing the prepared map
	 */
	public static void cancelFastDependencyDirting() {
		if (dependencyDirting.get() != null) {
			logger.info("Cancelling fast dependency dirting");
			dependencyDirting.remove();
		}
	}

	/**
	 * Prepare the object for fast dependency dirting if that was prepared before
	 * @param objectType object type
	 * @param objId object id
	 * @param channelId channel id
	 * @param attributes dirted attributes (may be empty)
	 * @return true if object was prepared for fast dependency dirting, false if not
	 */
	protected static boolean doFastDependencyDirting(int objectType, int objId, int channelId, Set<String> attributes) {
		Map<Integer, Map<Integer, Map<Integer, Set<String>>>> dirtMap = dependencyDirting.get();
		if (dirtMap == null) {
			return false;
		}

		dirtMap.computeIfAbsent(channelId, key -> new HashMap<>()).computeIfAbsent(objectType, key -> new HashMap<>())
				.computeIfAbsent(objId, key -> new HashSet<>()).addAll(attributes);

		return true;
	}

	/**
	 * Start the publish process. Mark the current publishqueue entries to be handled by the publish process.
	 * Publishqueue entries will not be published if they
	 * <ol>
	 * <li>Belong to nodes/channels that are not listed in publishedNodes</li>
	 * <li>Are marked as delayed</li>
	 * </ol>
	 * The sql statement will be performed in a separate transaction, which is committed, so that the changes will be available immediately.
	 * @param publishedNodes list of published nodes
	 * @return A Map containing counters for folders, pages
	 *		and files which are to be published. Outer keys are the node Ids, "inner" keys are the numeric object types
	 * @throws NodeException
	 */
	public static Map<Integer, Map<Integer, Integer>> startPublishProcess(final List<Node> publishedNodes) throws NodeException {
		Map<Integer, Map<Integer, Integer>> objectsToPublishCount = new HashMap<>();

		if (ObjectTransformer.isEmpty(publishedNodes)) {
			return objectsToPublishCount;
		}

		removerService = Executors.newSingleThreadExecutor();

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				StringBuffer sql = new StringBuffer("UPDATE publishqueue SET publish_flag = ? WHERE delay = ? AND channel_id IN (");

				sql.append(StringUtils.repeat("?", publishedNodes.size(), ","));
				sql.append(")");
				List<Object> params = new ArrayList<Object>();

				params.add(1); // set publish flag to...
				params.add(0); // delay = ...
				for (Node node : publishedNodes) {
					params.add(node.getId()); // channel_id IN ...
				}

				DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
			}
		});

		// get IDs of all nodes, which publish files into a content repository
		int[] fileNodeIds = publishedNodes.stream().filter(Node::doPublishContentMapFiles).filter(n -> {
			try {
				return n.getContentRepository() != null;
			} catch (NodeException e) {
				return false;
			}
		}).mapToInt(Node::getId).toArray();

		// get IDs of all nodes, which publish folders into a content repository
		int[] folderNodeIds = publishedNodes.stream().filter(Node::doPublishContentMapFolders).filter(n -> {
			try {
				return n.getContentRepository() != null;
			} catch (NodeException e) {
				return false;
			}
		}).mapToInt(Node::getId).toArray();

		// get IDs of all nodes, which publish pages
		int[] pageNodeIds = publishedNodes.stream().mapToInt(Node::getId).toArray();

		// get IDs of all nodes, which publish forms into a content repository
		int[] formNodeIds = publishedNodes.stream().filter(Node::doPublishContentmap).filter(n -> {
			try {
				return n.getContentRepository() != null;
			} catch (NodeException e) {
				return false;
			}
		}).mapToInt(Node::getId).toArray();

		Map<Integer, Integer> folderCountPerNode = countDirtedObjectsPerNode(Folder.class, true, false, folderNodeIds);
		Map<Integer, Integer> pageCountPerNode = countDirtedObjectsPerNode(Page.class, true, false, pageNodeIds);
		Map<Integer, Integer> fileCountPerNode = countDirtedObjectsPerNode(File.class, true, false, fileNodeIds);
		Map<Integer, Integer> formCountPerNode = countDirtedObjectsPerNode(Form.class, true, false, formNodeIds);

		for (Node node : publishedNodes) {
			int nodeId = node.getId();
			Map<Integer, Integer> nodeCounts = new HashMap<>();
			nodeCounts.put(Folder.TYPE_FOLDER_INTEGER, folderCountPerNode.getOrDefault(nodeId, 0));
			nodeCounts.put(Page.TYPE_PAGE_INTEGER, pageCountPerNode.getOrDefault(nodeId, 0));
			nodeCounts.put(File.TYPE_FILE_INTEGER, fileCountPerNode.getOrDefault(nodeId, 0));
			nodeCounts.put(Form.TYPE_FORM, formCountPerNode.getOrDefault(nodeId, 0));

			objectsToPublishCount.put(nodeId, nodeCounts);
		}

		return objectsToPublishCount;
	}

	/**
	 * Finalize the publish process
	 * <ol>
	 * <li>Remove all publishqueue entries that have the publish_flag set</li>
	 * </ol>
	 * This is done in a new transaction
	 */
	public static void finalizePublishProcess() {
		try {
			// first make sure, all objects, that have been marked before are rmoved
			HandledObject.markObjectsFinished();

			if (removerService != null) {
			removerService.shutdown();
			if (!removerService.awaitTermination(1, TimeUnit.HOURS)) {
				throw new NodeException("Timeout when waiting for publiqueue entries to be removed");
			}
			}

			TransactionManager.execute(new TransactionManager.Executable() {
				public void execute() throws NodeException {
					DBUtils.executeUpdate("DELETE FROM publishqueue WHERE publish_flag = ?", new Object[] { 1});
				}
			});
		} catch (NodeException | InterruptedException e) {
			logger.error("Error while finalizing the publish process", e);
		} finally {
			HandledObject.clear();
		}
	}

	/**
	 * Unmark the publishqueue entries handled by this publish process
	 *
	 * @return A Map containing counters for folders, pages
	 *		and files that were <em>not</em> published. The keys of the
	 *		Map are the numeric object codes.
	 */
	public static Map<Integer, Integer> handleFailedPublishProcess() {
		@SuppressWarnings("serial")
		HashMap<Integer, Integer> notPublishedCount = new HashMap<Integer, Integer>(3) {
			{
				put(Folder.TYPE_FOLDER_INTEGER, 0);
				put(Page.TYPE_PAGE_INTEGER, 0);
				put(File.TYPE_FILE_INTEGER, 0);
				put(Form.TYPE_FORM, 0);
			}
		};

		try {
			// first make sure, all objects, that have been marked before are rmoved
			HandledObject.markObjectsFinished();

			if (removerService != null) {
			removerService.shutdown();
			if (!removerService.awaitTermination(1, TimeUnit.HOURS)) {
				throw new NodeException("Timeout when waiting for publiqueue entries to be removed");
			}
			}

			notPublishedCount.put(
				Folder.TYPE_FOLDER_INTEGER,
				getDirtedObjectIds(Folder.class, true, null).size());

			notPublishedCount.put(
				Page.TYPE_PAGE_INTEGER,
				getDirtedObjectIds(Page.class, true, null).size());

			notPublishedCount.put(
				File.TYPE_FILE_INTEGER,
				getDirtedObjectIds(File.class, true, null).size());

			notPublishedCount.put(
					Form.TYPE_FORM,
					getDirtedObjectIds(Form.class, true, null).size());

			TransactionManager.execute(new TransactionManager.Executable() {
				public void execute() throws NodeException {
					DBUtils.executeUpdate("UPDATE publishqueue SET publish_flag = ?", new Object[] { 0 });
				}
			});
		} catch (NodeException | InterruptedException e) {
			logger.error("Error while handling a failed publish process", e);
		} finally {
			HandledObject.clear();
		}

		return notPublishedCount;
	}

	/**
	 * Get the ids of the dirted objects of given class for the given node.
	 * When fetching the ids for a channel, the result may contain IDs of objects that belong to the same channelset. When getting the objects for the channel,
	 * this will result in duplicates in the list of objects.
	 * To avoid this, use the method {@link #getDirtedObjects(Class, Node)}, which will remove duplicates.
	 * @param clazz object class
	 * @param forPublish true if only objects marked for the publish run shall be fetched
	 * @param node node
	 * @param omit optional list of Actions, that shall be omitted (in addition to the "removing" actions {@link Action#DELETE}, {@link Action#REMOVE}, {@link Action#OFFLINE} and {@link Action#HIDE})
	 * @return list of object ids
	 * @throws NodeException
	 */
	public static <T extends NodeObject> List<Integer> getDirtedObjectIds(final Class<T> clazz, final boolean forPublish, final Node node, final Action...omit) throws NodeException {
		final List<Integer> objIds = new ArrayList<Integer>();

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();
				final int objType = t.getTType(clazz);
				StringBuilder sql = new StringBuilder("SELECT DISTINCT obj_id FROM publishqueue WHERE obj_type = ? AND action NOT IN (");
				sql.append(StringUtils.repeat("?", omit.length + 4, ",")).append(")");

				if (forPublish) {
					sql.append(" AND publish_flag = ?");
				}
				if (node != null) {
					sql.append(" AND channel_id = ?");
				}
				DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						int pCounter = 1;

						stmt.setInt(pCounter++, objType); // obj_type = ?

						stmt.setString(pCounter++, Action.DELETE.toString()); // action NOT IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.REMOVE.toString()); // action NOT IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.OFFLINE.toString()); // action NOT IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.HIDE.toString()); // action NOT IN (?, ?, ?, ?)

						for (Action action : omit) {
							stmt.setString(pCounter++, action.toString()); // action NOT IN (?, ?, ?, ?, ...)
						}

						if (forPublish) {
							stmt.setInt(pCounter++, 1); // publish_flag = ?
						}
						if (node != null) {
							stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0)); // channel_id = ?
						}
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							objIds.add(rs.getInt("obj_id"));
						}
					}
				});
			}
		});
		return objIds;
	}

	/**
	 * Get the dirted objects of given class for the given node, that are marked for the publish process.
	 * Access to the publishqueue will be done in a separate transaction.
	 * The result will not contain duplicate entries
	 * @param clazz object class
	 * @param node node
	 * @return list of dirted objects
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> List<NodeObjectWithAttributes<T>> getDirtedObjects(Class<T> clazz, final Node node) throws NodeException {
		if (node == null) {
			return Collections.emptyList();
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		List<Integer> objIds = getDirtedObjectIds(clazz, true, node);

		Set<T> objects = new LinkedHashSet<T>();

		try (ChannelTrx cTrx = new ChannelTrx(node)) {
			objects.addAll(t.getObjects(clazz, objIds));
		}

		List<NodeObjectWithAttributes<T>> objectsWithAttributes = new ArrayList<>();
		// filter the objects, that are not visible in the given node
		for (Iterator<T> i = objects.iterator(); i.hasNext();) {
			T o = i.next();

			try {
				if (!MultichannellingFactory.isVisibleInNode(node, (LocalizableNodeObject<? extends NodeObject>) o)) {
					continue;
				}
			} catch (InconsistentDataException e) {
				// it may happen that e.g. the folder of the object no longer exists (because it was deleted)
				// in such cases we also remove this object (because it is supposed to also be deleted)
				continue;
			}

			objectsWithAttributes.add(new NodeObjectWithAttributes<T>(o));
		}

		return objectsWithAttributes;
	}

	/**
	 * Variant of {@link #getDirtedObjectIds(Class, boolean, Node, Action...)},
	 * which will return a map. Keys are the object IDs, values are sets of
	 * dirted attribute names (null if whole object is dirted)
	 *
	 * @param clazz object class
	 * @param forPublish true if only objects marked for the publish run shall be fetched
	 * @param node node
	 * @param omit optional list of Actions, that shall be omitted (in addition to the "removing" actions {@link Action#DELETE}, {@link Action#REMOVE}, {@link Action#OFFLINE} and {@link Action#HIDE})
	 * @return map of dirted object IDs with attributes
	 * @throws NodeException
	 */
	public static <T extends NodeObject> Map<Integer, Set<String>> getDirtedObjectIdsWithAttributes(final Class<T> clazz, final boolean forPublish,
			final Node node, final Action... omit) throws NodeException {
		final Map<Integer, Set<String>> objIds = new HashMap<>();

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();
				final int objType = t.getTType(clazz);
				StringBuilder sql = new StringBuilder(
						"SELECT pq.obj_id, pqa.name FROM publishqueue pq LEFT JOIN publishqueue_attribute pqa ON pq.id = pqa.publishqueue_id WHERE pq.obj_type = ? AND pq.action NOT IN (");
				sql.append(StringUtils.repeat("?", omit.length + 4, ",")).append(")");

				if (forPublish) {
					sql.append(" AND pq.publish_flag = ?");
				}
				if (node != null) {
					sql.append(" AND pq.channel_id = ?");
				}
				DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						int pCounter = 1;

						stmt.setInt(pCounter++, objType); // obj_type = ?

						stmt.setString(pCounter++, Action.DELETE.toString()); // action NOT IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.REMOVE.toString()); // action NOT IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.OFFLINE.toString()); // action NOT IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.HIDE.toString()); // action NOT IN (?, ?, ?, ?)

						for (Action action : omit) {
							stmt.setString(pCounter++, action.toString()); // action NOT IN (?, ?, ?, ?, ...)
						}

						if (forPublish) {
							stmt.setInt(pCounter++, 1); // publish_flag = ?
						}
						if (node != null) {
							stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0)); // channel_id = ?
						}
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							int objId = rs.getInt("obj_id");
							String name = rs.getString("name");
							if (name == null) {
								// dirt entries without attribute name dirt the
								// whole object (overwriting attribute specific
								// dirts, if any were found before)
								objIds.put(objId, null);
							} else if (!objIds.containsKey(objId)) {
								// new dirt entry with an attribute name: dirt the single attribute
								objIds.put(objId, new HashSet<>(Arrays.asList(name)));
							} else {
								// the object is already dirted, if it was dirted without attribute name (as a whole)
								// we leave it that way (ignore the current entry)
								// otherwise, we add the dirted attribute to the attributes dirted before
								Set<String> set = objIds.get(objId);
								if (set != null) {
									set.add(name);
								}
							}
						}
					}
				});
			}
		});
		return objIds;
	}

	/**
	 * Variant of {@link #getDirtedObjects(Class, Node)} which will return a
	 * map. Keys are the dirted objects, values are sets of dirted attributes
	 * (null if whole object is dirted)
	 *
	 * @param clazz
	 *            object class
	 * @param node
	 *            node
	 * @return list of dirted objects
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> List<NodeObjectWithAttributes<T>> getDirtedObjectsWithAttributes(Class<T> clazz, final Node node) throws NodeException {
		if (node == null) {
			return Collections.emptyList();
		}
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<Integer, Set<String>> objIds = getDirtedObjectIdsWithAttributes(clazz, true, node);
		Map<Integer, NodeObjectWithAttributes<T>> objectMap = new HashMap<>();

		try (ChannelTrx cTrx = new ChannelTrx(node)) {
			for (Map.Entry<Integer, Set<String>> entry : objIds.entrySet()) {
				T object = t.getObject(clazz, entry.getKey());
				if (object != null) {
					// omit objects, which are not online
					if (object instanceof PublishableNodeObject) {
						if (!((PublishableNodeObject) object).isOnline()) {
							continue;
						}
					}

					Integer objectId = object.getId();

					// This can happen because two different object IDs from
					// getDirtedObjectIdsWithAttributes can resolve to the same
					// object in the channel transaction (e.g. when a master object
					// is unhidden in a channel, and the localized copy also has
					// entries in he publishqueue.
					if (objectMap.containsKey(objectId)) {
						objectMap.get(objectId).mergeAttributes(entry.getValue());
					} else {
						objectMap.put(objectId, new NodeObjectWithAttributes<T>(object, entry.getValue()));
					}
				}
			}
		}

		Collection<NodeObjectWithAttributes<T>> objects = objectMap.values();

		if (LocalizableNodeObject.class.isAssignableFrom(clazz)) {
			// filter the objects, that are not visible in the given node
			for (Iterator<NodeObjectWithAttributes<T>> i = objects.iterator(); i.hasNext();) {
				NodeObjectWithAttributes<T> o = i.next();

				try {
					if (!MultichannellingFactory.isVisibleInNode(node, (LocalizableNodeObject<? extends NodeObject>) o.object)) {
						i.remove();
					}
				} catch (InconsistentDataException e) {
					// it may happen that e.g. the folder of the object no longer exists (because it was deleted)
					// in such cases we also remove this object (because it is supposed to also be deleted)
					i.remove();
				}
			}
		}

		return new ArrayList<>(objects);
	}

	/**
	 * Get the ids of objects, that need to be removed from the published node.
	 * This includes
	 * <ul>
	 * <li>Objects that have been <i>deleted</i> (completely removed from the system)</li>
	 * <li>Objects that have been <i>removed</i> (moved to another node)</li>
	 * <li>Objects that have been taken <i>offline</i></li>
	 * </ul>
	 * @param clazz object class
	 * @param forPublish true to get objects, that were marked for the publish process, false to get objects not marked for the publish process, null to get all objects
	 * @param node node
	 * @return list of ids
	 * @throws NodeException
	 */
	public static <T extends NodeObject> List<Integer> getRemovedObjectIds(final Class<T> clazz, final Boolean forPublish, final Node node) throws NodeException {
		if (node == null) {
			return Collections.emptyList();
		}

		final List<Integer> objIds = new ArrayList<Integer>();

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();
				final int objType = t.getTType(clazz);

				StringBuilder sql = new StringBuilder("SELECT DISTINCT obj_id FROM publishqueue WHERE obj_type = ? AND channel_id = ?");
				if (forPublish != null) {
					sql.append(" AND publish_flag = ?");
				}
				sql.append(" AND action IN (?, ?, ?, ?)");

				DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						int pCounter = 1;
						stmt.setInt(pCounter++, objType); // obj_type = ?
						stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0)); // channel_id = ?
						if (forPublish != null) {
							stmt.setBoolean(pCounter++, forPublish.booleanValue()); // publish_flag = ?
						}
						stmt.setString(pCounter++, Action.DELETE.toString()); // action IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.REMOVE.toString()); // action IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.OFFLINE.toString()); // action IN (?, ?, ?, ?)
						stmt.setString(pCounter++, Action.HIDE.toString()); // action IN (?, ?, ?, ?)
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							objIds.add(rs.getInt("obj_id"));
						}
					}
				});
			}
		});
		return objIds;
	}

	/**
	 * Get the dirted objects with attributes
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param forPublish true to get objects marked for the publish process
	 * @param node node, for which the objects are dirted
	 * @param action list of dirt actions to include
	 * @return map of object IDs to sets of attributes
	 * @throws NodeException
	 */
	public static <T extends NodeObject> Map<Integer, Set<String>> getObjectIdsWithAttributes(final Class<T> clazz, final boolean forPublish,
			final Node node, final Action... action) throws NodeException {
		final Map<Integer, Set<String>> objIds = new HashMap<>();

		TransactionManager.execute(new TransactionManager.Executable() {
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();
				final int objType = t.getTType(clazz);
				StringBuilder sql = new StringBuilder(
						"SELECT pq.obj_id, pqa.name FROM publishqueue pq LEFT JOIN publishqueue_attribute pqa ON pq.id = pqa.publishqueue_id WHERE pq.obj_type = ?");
				if (action.length > 0) {
					sql.append(" AND pq.action IN (").append(StringUtils.repeat("?", action.length, ",")).append(")");
				}

				if (forPublish) {
					sql.append(" AND pq.publish_flag = ?");
				}
				if (node != null) {
					sql.append(" AND pq.channel_id = ?");
				}
				DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						int pCounter = 1;

						stmt.setInt(pCounter++, objType); // obj_type = ?

						if (action.length > 0) {
							for (Action a : action) {
								stmt.setString(pCounter++, a.toString()); // action IN (?)
							}
						}

						if (forPublish) {
							stmt.setInt(pCounter++, 1); // publish_flag = ?
						}
						if (node != null) {
							stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0)); // channel_id = ?
						}
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							int objId = rs.getInt("obj_id");
							String name = rs.getString("name");
							if (name == null) {
								// dirt entries without attribute name dirt the
								// whole object (overwriting attribute specific
								// dirts, if any were found before)
								objIds.put(objId, null);
							} else if (!objIds.containsKey(objId)) {
								// new dirt entry with an attribute name: dirt the single attribute
								objIds.put(objId, new HashSet<>(Arrays.asList(name)));
							} else {
								// the object is already dirted, if it was dirted without attribute name (as a whole)
								// we leave it that way (ignore the current entry)
								// otherwise, we add the dirted attribute to the attributes dirted before
								Set<String> set = objIds.get(objId);
								if (set != null) {
									set.add(name);
								}
							}
						}
					}
				});
			}
		});
		return objIds;
	}

	/**
	 * Get the offline pages that are handled in this publish run
	 * @param executor SQLExecutor instance
	 * @throws NodeException
	 */
	public static void getOfflinePages(final SQLExecutor executor) throws NodeException {
		final SQLExecutor exe = new SQLExecutorWrapper(executor) {

			/*
			 * (non-Javadoc)
			 *
			 * @see com.gentics.lib.db.SQLExecutorWrapper#prepareStatement(java.sql.PreparedStatement)
			 */
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, Page.TYPE_PAGE); // obj_type = ?
				stmt.setInt(2, 1); // publish_flag = ?
				stmt.setString(3, Action.OFFLINE.toString()); // action = ?
			}
		};

		TransactionManager.execute(
				new TransactionManager.Executable() {
			public void execute() throws NodeException {
				DBUtils.executeStatement(
						"SELECT DISTINCT obj_type o_type, obj_id o_id, channel_id node_id FROM "
								+ "publishqueue WHERE obj_type = ? AND publish_flag = ? AND action = ?",
								exe);
			}
		});
	}

	/**
	 * Get the deleted objects that are handled in this publish run
	 * @param objType object type
	 * @param executor SQLExecutor instance
	 * @throws NodeException
	 */
	public static void getDeletedObjects(final int objType, final SQLExecutor executor) throws NodeException {
		final SQLExecutor exe = new SQLExecutorWrapper(executor) {

			/*
			 * (non-Javadoc)
			 *
			 * @see com.gentics.lib.db.SQLExecutorWrapper#prepareStatement(java.sql.PreparedStatement)
			 */
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, objType); // obj_type = ?
				stmt.setInt(2, 1); // publish_flag = ?
				stmt.setString(3, Action.DELETE.toString()); // action = ?
			}
		};

		TransactionManager.execute(
				new TransactionManager.Executable() {
			public void execute() throws NodeException {
				DBUtils.executeStatement(
						"SELECT DISTINCT obj_type o_type, obj_id o_id, channel_id node_id FROM "
								+ "publishqueue WHERE obj_type = ? AND publish_flag = ? AND action = ?",
								exe);
			}
		});
	}

	/**
	 * Dirt the given object for the channel id
	 * @param object object to dirt
	 * @param action dirting action
	 * @param channelId channel id (0 means: for all channels)
	 * @param attributes optional list of attributes to be dirted
	 * @return stored entry
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Entry> dirtObject(NodeObject object, Action action, int channelId, String...attributes) throws NodeException {
		if (object == null) {
			throw new NodeException("Cannot dirt null object");
		}

		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = null;

		if (channelId != 0) {
			node = t.getObject(Node.class, channelId);
			// omit dirting of objects for channels, in which they are not visible
			if ((action == Action.DEPENDENCY || action == Action.MODIFY ) && object instanceof LocalizableNodeObject<?>) {
				LocalizableNodeObject<NodeObject> locObject = (LocalizableNodeObject<NodeObject>)object;
				if (!MultichannellingFactory.isVisibleInNode(node, locObject)) {
					return Collections.emptyList();
				}
			}
		}

		if (object instanceof Page) {
			Page page = (Page) object;

			// omit dirting of pages, which are generally excluded from publishing
			if (action != Action.DELETE && page.getTemplate().getMarkupLanguage().isExcludeFromPublishing()) {
				return Collections.emptyList();
			}

			// omit dirting offline pages (DEPENDENCY, MODIFY or MOVE)
			if (action == Action.DEPENDENCY || action == Action.MOVE || action == Action.MODIFY) {
				// omit offline pages
				if (!page.isOnline()) {
					return Collections.emptyList();
				} else if (node != null) {
					if (!MultichannellingFactory.isVisibleInNode(node, page)) {
						return Collections.emptyList();
					}
				}
			}
		}

		// omit dirting offline objects (DEPENDENCY, MODIFY or MOVE)
		if (object instanceof PublishableNodeObject
				&& (action == Action.DEPENDENCY || action == Action.MOVE || action == Action.MODIFY)) {
			PublishableNodeObject pon = (PublishableNodeObject) object;
			if (!pon.isOnline()) {
				return Collections.emptyList();
			}
		}

		Collection<Integer> channelIds = new ArrayList<Integer>();

		if (channelId == 0) {
			if (object instanceof LocalizableNodeObject<?>) {
				channelIds.addAll(MultichannellingFactory.getNodeIds((LocalizableNodeObject<NodeObject>) object, true));
			} else if (object instanceof NodeObjectInFolder) {
				NodeObjectInFolder tmp = (NodeObjectInFolder) object;
				channelIds.add(tmp.getOwningNode().getId());
			}
		} else {
			channelIds.add(channelId);
		}

		// when an object is removed from a node, it is also removed from all channels, where the object is not visible in (any more)
		if (action == Action.REMOVE) {
			Collection<Node> channels = node.getAllChannels();

			for (Node channel : channels) {
				channelIds.add(ObjectTransformer.getInt(channel.getId(), 0));
			}

			if (object instanceof LocalizableNodeObject<?>) {
				Node objectChannel = ((LocalizableNodeObject<?>) object).getChannel();
				if (objectChannel == null) {
					objectChannel = ((LocalizableNodeObject<?>) object).getOwningNode();
				}
				if (objectChannel != null) {
					channelIds.remove(ObjectTransformer.getInt(objectChannel.getId(), 0));
					for (Node channel : objectChannel.getAllChannels()) {
						channelIds.remove(ObjectTransformer.getInt(channel.getId(), 0));
					}
				}
			}
		}

		Collection<Entry> entries = new ArrayList<Entry>();

		for (Integer cId : channelIds) {
			int objType = ObjectTransformer.getInt(object.getTType(), 0);

			if (objType == ImageFile.TYPE_IMAGE) {
				objType = File.TYPE_FILE;
			}

			if (attributes.length == 0 && REMOVING_ACTIONS.contains(action)) {
				// when objects are deleted or taken offline, we need to know, which language the object had, in order to remove the correct language variant from mesh.
				// exception: when the object was REMOVEd (means: moved to another node), we want to remove all language variants from mesh.
				if (MeshPublisher.supportsAlternativeLanguages(object) || action == Action.REMOVE) {
					attributes = new String[] { "uuid:" + MeshPublisher.getMeshUuid(object) };
				} else {
					attributes = new String[] { "uuid:" + MeshPublisher.getMeshUuid(object),
							"language:" + MeshPublisher.getMeshLanguage(object) };
				}
			}

			Entry entry = dirtObject(objType, ObjectTransformer.getInt(object.getId(), 0), action, cId, true, attributes);
			if (entry != null) {
				entries.add(entry);
			}
		}
		return entries;
	}

	/**
	 * Dirt all published pages
	 * @param nodeId node/channel ids (for restricting to nodes/channels). If null, no restriction will be done (all pages will be republished)
	 * @param rangeProperty range property to restrict to a specified range (null for no restriction)
	 * @param rangeStart start value of the range restriction (if rangeProperty not null)
	 * @param rangeEnd end value of the range restriction (if rangeProperty not null)
	 * @param dirtAction dirt action
	 * @param attributes optional attributes to dirt
	 * @throws NodeException
	 */
	public static void dirtPublishedPages(int[] nodeId, String rangeProperty, int rangeStart, int rangeEnd, Action dirtAction, String... attributes)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean filterCreationDate = StringUtils.isEqual(rangeProperty, "cdate");

		// get template IDs to exclude
		Set<Integer> excludedTemplateIds = DBUtils.select(
				"SELECT template.id FROM template LEFT JOIN ml ON template.ml_id = ml.id WHERE ml.exclude_from_publishing = ?",
				ps -> {
					ps.setBoolean(1, true);
				}, DBUtils.IDS);

		// get the nodes
		Collection<Node> nodes = getNodes(nodeId);

		for (Node node : nodes) {
			int channelId = ObjectTransformer.getInt(node.getId(), 0);

			if (node.isChannel()) {
				t.setChannelId(node.getId());
				try {
					// get all online pages
					List<Page> pages = node.getFolder().getPages(PageSearch.create().setRecursive(true).setOnline(true));

					for (Page page : pages) {
						if (page.getTemplate().getMarkupLanguage().isExcludeFromPublishing()) {
							continue;
						}
						if (filterCreationDate) {
							// range restriction
							int cdate = page.getCDate().getIntTimestamp();

							if (cdate < rangeStart || cdate > rangeEnd) {
								continue;
							}
						}
						dirtObject(Page.TYPE_PAGE, ObjectTransformer.getInt(page.getId(), 0), dirtAction, channelId, false, attributes);
					}
				} finally {
					t.resetChannel();
				}
			} else {
				// for non-channels, dirting all pages is easier
				StringBuffer sql = new StringBuffer(
						"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) SELECT ?, page.id, ?, ?, ? FROM page LEFT JOIN folder ON page.folder_id = folder.id WHERE page.online = ? AND folder.node_id = ? AND page.channel_id = ?");
				List<Object> params = new ArrayList<Object>();

				params.add(Page.TYPE_PAGE); // first SELECT ?
				params.add(dirtAction.toString()); // second SELECT ?
				params.add(channelId); // third SELECT ?
				params.add(t.getUnixTimestamp()); // fourth SELECT ?
				params.add(1); // page.online = ?
				params.add(channelId); // folder.node_id = ?
				params.add(0); // page.channel_id = ?

				// add range restriction
				if (!StringUtils.isEmpty(rangeProperty)) {
					sql.append(" AND page.").append(rangeProperty).append(" >= ? AND page.").append(rangeProperty).append(" <= ?");
					params.add(rangeStart);
					params.add(rangeEnd);
				}

				// add template restriction
				if (!excludedTemplateIds.isEmpty()) {
					sql.append(" AND page.template_id NOT IN (" + StringUtils.repeat("?", excludedTemplateIds.size(), ",") + ")");
					params.addAll(excludedTemplateIds);
				}

				if (!ObjectTransformer.isEmpty(attributes) && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING)) {
					Set<Integer> ids = new HashSet<>();
					DBUtils.executeStatement(sql.toString(), Transaction.INSERT_STATEMENT, st -> {
						int counter = 1;
						st.setInt(counter++, Page.TYPE_PAGE);
						st.setString(counter++, dirtAction.toString());
						st.setInt(counter++, channelId);
						st.setInt(counter++, t.getUnixTimestamp());
						st.setInt(counter++, 1);
						st.setInt(counter++, channelId);
						st.setInt(counter++, 0);
						if (!StringUtils.isEmpty(rangeProperty)) {
							st.setInt(counter++, rangeStart);
							st.setInt(counter++, rangeEnd);
						}
						if (!excludedTemplateIds.isEmpty()) {
							for (int id : excludedTemplateIds) {
								st.setInt(counter++, id);
							}
						}
					}, null, st -> {
						ResultSet keys = st.getGeneratedKeys();
						while (keys.next()) {
							ids.add(keys.getInt(1));
						}
					});

					for (Integer id : ids) {
						setAttributes(id, false, attributes);
					}
				} else {
					DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
				}
			}
		}
	}

	/**
	 * Delay publishing of the dirted objects of given type
	 * @param nodeId node/channel ids (for restricting). If null, all objects of given type will be delayed.
	 * @param delay true to delay, false to "undelay"
	 * @param objType object type
	 * @param rangeProperty range property to restrict to a specified range (null for no restriction)
	 * @param rangeStart start value of the range restriction (if rangeProperty not null)
	 * @param rangeEnd end value of the range restriction (if rangeProperty not null)
	 * @throws NodeException
	 */
	public static void delayDirtedObjects(int[] nodeId, boolean delay, int objType, String rangeProperty, int rangeStart, int rangeEnd) throws NodeException {
		String table = null;

		switch (objType) {
		case Page.TYPE_PAGE:
			table = "page";
			break;

		case File.TYPE_FILE:
		case ImageFile.TYPE_IMAGE:
			table = "contentfile";
			objType = File.TYPE_FILE;
			break;

		case Folder.TYPE_FOLDER:
			table = "folder";
			break;

		case Form.TYPE_FORM:
			table = "form";
			break;

		default:
			throw new NodeException("Cannot delay publishing of objects of type " + objType);
		}

		StringBuffer sql = new StringBuffer("UPDATE publishqueue pq");
		List<Object> params = new ArrayList<Object>();

		if (!StringUtils.isEmpty(rangeProperty)) {
			sql.append(" LEFT JOIN ").append(table).append(" t ON pq.obj_id = t.id");
		}

		sql.append(" SET pq.delay = ? WHERE pq.obj_type = ? AND pq.action != ?");
		params.add(delay ? 1 : 0);
		params.add(objType);
		params.add(Action.DELETE.toString());

		if (!StringUtils.isEmpty(rangeProperty)) {
			sql.append(" AND t.").append(rangeProperty).append(" >= ? AND t.").append(rangeProperty).append(" <= ?");
			params.add(rangeStart);
			params.add(rangeEnd);
		}

		// add optional restriction to node id's
		if (!ObjectTransformer.isEmpty(nodeId)) {
			sql.append(" AND pq.channel_id IN (");
			sql.append(StringUtils.repeat("?", nodeId.length, ","));
			sql.append(")");
			for (int id : nodeId) {
				params.add(id);
			}
		}

		DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
	}

	/**
	 * Remove objects of given type from publish queue
	 * @param nodeId node/channel ids (for restricting). If null, all objects of given type will be handled.
	 * @param objType object type
	 * @param rangeProperty range property to restrict to a specified range (null for no restriction)
	 * @param rangeStart start value of the range restriction (if rangeProperty not null)
	 * @param rangeEnd end value of the range restriction (if rangeProperty not null)
	 * @throws NodeException
	 */
	public static void undirtObjects(int[] nodeId, int objType, String rangeProperty, int rangeStart, int rangeEnd) throws NodeException {
		String table = null;

		switch (objType) {
		case Page.TYPE_PAGE:
			table = "page";
			break;

		case File.TYPE_FILE:
		case ImageFile.TYPE_IMAGE:
			table = "contentfile";
			objType = File.TYPE_FILE;
			break;

		case Folder.TYPE_FOLDER:
			table = "folder";
			break;

		case Form.TYPE_FORM:
			table = "form";
			break;

		default:
			throw new NodeException("Cannot undirt publishing of objects of type " + objType);
		}

		StringBuffer sql = new StringBuffer("DELETE pq FROM publishqueue pq");
		List<Object> params = new ArrayList<Object>();

		if (!StringUtils.isEmpty(rangeProperty)) {
			sql.append(" LEFT JOIN ").append(table).append(" t ON pq.obj_id = t.id");
		}

		sql.append(" WHERE pq.obj_type = ? AND pq.action != ?");
		params.add(objType);
		params.add(Action.DELETE.toString());

		if (!StringUtils.isEmpty(rangeProperty)) {
			sql.append(" AND t.").append(rangeProperty).append(" >= ? AND t.").append(rangeProperty).append(" <= ?");
			params.add(rangeStart);
			params.add(rangeEnd);
		}

		// add optional restriction to node id's
		if (!ObjectTransformer.isEmpty(nodeId)) {
			sql.append(" AND pq.channel_id IN (");
			sql.append(StringUtils.repeat("?", nodeId.length, ","));
			sql.append(")");
			for (int id : nodeId) {
				params.add(id);
			}
		}

		DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
	}

	/**
	 * Dirt all images and files
	 * @param nodeId node/channel ids (for restricting to nodes/channels). If null, no restriction will be done (all images/files will be republished)
	 * @param rangeProperty range property to restrict to a specified range (null for no restriction)
	 * @param rangeStart start value of the range restriction (if rangeProperty not null)
	 * @param rangeEnd end value of the range restriction (if rangeProperty not null)
	 * @param dirtAction dirt action
	 * @param attributes optional attributes to dirt
	 * @throws NodeException
	 */
	public static void dirtImagesAndFiles(int[] nodeId, String rangeProperty, int rangeStart, int rangeEnd, Action dirtAction, String... attributes)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean filterCreationDate = StringUtils.isEqual(rangeProperty, "cdate");

		// get the nodes
		Collection<Node> nodes = getNodes(nodeId);

		for (Node node : nodes) {
			int channelId = ObjectTransformer.getInt(node.getId(), 0);

			if (node.isChannel()) {
				t.setChannelId(node.getId());
				try {
					// get all images/files
					List<File> files = node.getFolder().getFilesAndImages(FileSearch.create().setRecursive(true));

					for (File file : files) {
						if (filterCreationDate) {
							// range restriction
							int cdate = file.getCDate().getIntTimestamp();

							if (cdate < rangeStart || cdate > rangeEnd) {
								continue;
							}
						}

						dirtObject(File.TYPE_FILE, ObjectTransformer.getInt(file.getId(), 0), dirtAction, channelId, false, attributes);
					}
				} finally {
					t.resetChannel();
				}
			} else {
				// for non-channels, dirting all images/files is easier
				StringBuffer sql = new StringBuffer(
						"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) SELECT ?, contentfile.id, ?, ?, ? FROM contentfile LEFT JOIN folder ON contentfile.folder_id = folder.id WHERE contentfile.deleted = 0 AND folder.node_id = ? AND contentfile.channel_id = ?");
				List<Object> params = new ArrayList<Object>();

				params.add(File.TYPE_FILE);
				params.add(dirtAction.toString());
				params.add(channelId);
				params.add(t.getUnixTimestamp());
				params.add(channelId);
				params.add(0);

				// add range restriction
				if (!StringUtils.isEmpty(rangeProperty)) {
					sql.append(" AND contentfile.").append(rangeProperty).append(" >= ? AND contentfile.").append(rangeProperty).append(" <= ?");
					params.add(rangeStart);
					params.add(rangeEnd);
				}

				if (!ObjectTransformer.isEmpty(attributes) && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING)) {
					Set<Integer> ids = new HashSet<>();
					DBUtils.executeStatement(sql.toString(), Transaction.INSERT_STATEMENT, st -> {
						st.setInt(1, File.TYPE_FILE);
						st.setString(2, dirtAction.toString());
						st.setInt(3, channelId);
						st.setInt(4, t.getUnixTimestamp());
						st.setInt(5, channelId);
						st.setInt(6, 0);
						if (!StringUtils.isEmpty(rangeProperty)) {
							st.setInt(7, rangeStart);
							st.setInt(8, rangeEnd);
						}
					}, null, st -> {
						ResultSet keys = st.getGeneratedKeys();
						while (keys.next()) {
							ids.add(keys.getInt(1));
						}
					});

					for (Integer id : ids) {
						setAttributes(id, false, attributes);
					}
				} else {
					DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
				}
			}
		}
	}

	/**
	 * Dirt all folders
	 * @param nodeId node/channel ids (for restricting to nodes/channels). If null, no restriction will be done (all folders will be republished)
	 * @param rangeProperty range property to restrict to a specified range (null for no restriction)
	 * @param rangeStart start value of the range restriction (if rangeProperty not null)
	 * @param rangeEnd end value of the range restriction (if rangeProperty not null)
	 * @param dirtAction dirt action
	 * @param attributes optional attributes to dirt
	 * @throws NodeException
	 */
	public static void dirtFolders(int[] nodeId, String rangeProperty, int rangeStart, int rangeEnd, Action dirtAction, String... attributes)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		boolean filterCreationDate = StringUtils.isEqual(rangeProperty, "cdate");

		// get the nodes
		Collection<Node> nodes = getNodes(nodeId);

		for (Node node : nodes) {
			final int channelId = ObjectTransformer.getInt(node.getId(), 0);

			if (node.isChannel()) {
				t.setChannelId(node.getId());
				try {
					recursiveDirtFolder(node.getFolder(), channelId, filterCreationDate, rangeStart, rangeEnd, dirtAction, attributes);
				} finally {
					t.resetChannel();
				}
			} else {
				// for non-channels, dirting all folders is easier
				StringBuffer sql = new StringBuffer(
						"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) SELECT ?, id, ?, ?, ? FROM folder WHERE deleted = 0 AND node_id = ? AND channel_id = ?");
				List<Object> params = new ArrayList<Object>();

				params.add(Folder.TYPE_FOLDER);
				params.add(dirtAction.toString());
				params.add(channelId);
				params.add(t.getUnixTimestamp());
				params.add(channelId);
				params.add(0);

				// add range restriction
				if (!StringUtils.isEmpty(rangeProperty)) {
					sql.append(" AND folder.").append(rangeProperty).append(" >= ? AND folder.").append(rangeProperty).append(" <= ?");
					params.add(rangeStart);
					params.add(rangeEnd);
				}

				if (!ObjectTransformer.isEmpty(attributes) && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING)) {
					Set<Integer> ids = new HashSet<>();
					DBUtils.executeStatement(sql.toString(), Transaction.INSERT_STATEMENT, st -> {
						st.setInt(1, Folder.TYPE_FOLDER);
						st.setString(2, dirtAction.toString());
						st.setInt(3, channelId);
						st.setInt(4, t.getUnixTimestamp());
						st.setInt(5, channelId);
						st.setInt(6, 0);
						if (!StringUtils.isEmpty(rangeProperty)) {
							st.setInt(7, rangeStart);
							st.setInt(8, rangeEnd);
						}
					}, null, st -> {
						ResultSet keys = st.getGeneratedKeys();
						while (keys.next()) {
							ids.add(keys.getInt(1));
						}
					});

					for (Integer id : ids) {
						setAttributes(id, false, attributes);
					}
				} else {
					DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
				}
			}
		}
	}

	/**
	 * Dirt all forms
	 * @param nodeId node/channel ids (for restricting to nodes/channels). If null, no restriction will be done (all forms will be republished)
	 * @param rangeProperty range property to restrict to a specified range (null for no restriction)
	 * @param rangeStart start value of the range restriction (if rangeProperty not null)
	 * @param rangeEnd end value of the range restriction (if rangeProperty not null)
	 * @param dirtAction dirt action
	 * @param attributes optional attributes to dirt
	 * @throws NodeException
	 */
	public static void dirtForms(int[] nodeId, String rangeProperty, int rangeStart, int rangeEnd, Action dirtAction, String... attributes)
			throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the nodes
		Collection<Node> nodes = getNodes(nodeId);

		for (Node node : nodes) {
			final int channelId = ObjectTransformer.getInt(node.getId(), 0);

			StringBuffer sql = new StringBuffer(
					"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) SELECT ?, form.id, ?, ?, ? FROM form LEFT JOIN folder ON form.folder_id = folder.id WHERE form.deleted = ? AND form.online = ? AND folder.node_id = ?");
			List<Object> params = new ArrayList<Object>();

			params.add(Form.TYPE_FORM);
			params.add(dirtAction.toString());
			params.add(channelId);
			params.add(t.getUnixTimestamp());
			params.add(0); // form.deleted = ?
			params.add(1); // form.online = ?
			params.add(channelId); // folder.node_id = ?

			// add range restriction
			if (!StringUtils.isEmpty(rangeProperty)) {
				sql.append(" AND form.").append(rangeProperty).append(" >= ? AND form.").append(rangeProperty).append(" <= ?");
				params.add(rangeStart);
				params.add(rangeEnd);
			}

			if (!ObjectTransformer.isEmpty(attributes) && t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING)) {
				Set<Integer> ids = new HashSet<>();
				DBUtils.executeStatement(sql.toString(), Transaction.INSERT_STATEMENT, st -> {
					st.setInt(1, Form.TYPE_FORM);
					st.setString(2, dirtAction.toString());
					st.setInt(3, channelId);
					st.setInt(4, t.getUnixTimestamp());
					st.setInt(5, 0); // form.deleted = ?
					st.setInt(6, 1); // form.online = ?
					st.setInt(7, channelId);
					if (!StringUtils.isEmpty(rangeProperty)) {
						st.setInt(8, rangeStart);
						st.setInt(9, rangeEnd);
					}
				}, null, st -> {
					ResultSet keys = st.getGeneratedKeys();
					while (keys.next()) {
						ids.add(keys.getInt(1));
					}
				});

				for (Integer id : ids) {
					setAttributes(id, false, attributes);
				}
			} else {
				DBUtils.executeUpdate(sql.toString(), (Object[]) params.toArray(new Object[params.size()]));
			}
		}
	}

	/**
	 * Count the number of dirted objects
	 * @param clazz object class
	 * @param forPublish true if only objects marked for the publish run shall be counted
	 * @param node optional node to restrict counting for a node
	 * @return number of dirted objects
	 * @throws NodeException
	 */
	public static <T extends NodeObject> int countDirtedObjects(final Class<T> clazz, final boolean forPublish, final Node node) throws NodeException {
		if (node != null) {
			return countDirtedObjects(clazz, forPublish, false, node.getId());
		} else {
			return countDirtedObjects(clazz, forPublish, false);
		}
	}

	/**
	 * Count the number of dirted objects
	 * @param clazz object class
	 * @param forPublish true if only objects marked for the publish run shall be counted
	 * @param delayed true to count delayed objects, false to count objects, which are not delayed
	 * @param nodeIds optional list of node IDs for restriction
	 * @return number of dirted objects
	 * @throws NodeException
	 */
	public static <T extends NodeObject> int countDirtedObjects(final Class<T> clazz, final boolean forPublish, boolean delayed, int... nodeIds)
			throws NodeException {
		final int[] result = new int[] { 0};

		TransactionManager.execute(
				new TransactionManager.Executable() {
			public void execute() throws NodeException {
				Transaction t = TransactionManager.getCurrentTransaction();
				final int objType = t.getTType(clazz);
				StringBuffer sql = new StringBuffer(
						"SELECT COUNT(DISTINCT pq.obj_id, pq.channel_id) c FROM publishqueue pq WHERE pq.action NOT IN (?, ?, ?) AND pq.obj_type = ? AND delay = ?");

				if (forPublish) {
					sql.append(" AND publish_flag = ?");
				}
				if (nodeIds.length > 0) {
					sql.append(" AND channel_id IN (").append(StringUtils.repeat("?", nodeIds.length, ",")).append(")");
				}
				DBUtils.executeStatement(sql.toString(), new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						int pCounter = 1;

						stmt.setString(pCounter++, Action.DELETE.toString()); // pq.action NOT IN (?, ?, ?)
						stmt.setString(pCounter++, Action.HIDE.toString()); // pq.action NOT IN (?, ?, ?)
						stmt.setString(pCounter++, Action.REMOVE.toString()); // pq.action NOT IN (?, ?, ?)
						stmt.setInt(pCounter++, objType); // pq.obj_type = ?
						stmt.setInt(pCounter++, delayed ? 1 : 0); // delay = ?
						if (forPublish) {
							stmt.setInt(pCounter++, 1); // publish_flag = ?
						}
						for (int id : nodeIds) {
							stmt.setInt(pCounter++, id); // channel_id IN (?)
						}
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						if (rs.next()) {
							result[0] = rs.getInt("c");
						}
					}
				});
			}
		});
		return result[0];
	}

	/**
	 * Count dirted objects of given class per node
	 * @param <T> type of the given object class
	 * @param clazz object class
	 * @param forPublish true if only objects marked for the publish run shall be counted
	 * @param delayed true to count delayed objects, false to count objects, which are not delayed
	 * @param nodeIds optional list of node IDs for restriction
	 * @return map with counts per node ID
	 * @throws NodeException
	 */
	public static <T extends NodeObject> Map<Integer, Integer> countDirtedObjectsPerNode(final Class<T> clazz,
			boolean forPublish, boolean delayed, int... nodeIds) throws NodeException {
		return TransactionManager.execute(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			final int objType = t.getTType(clazz);

			StringBuilder sql = new StringBuilder();
			sql.append(
					"SELECT COUNT(DISTINCT pq.obj_id) c, channel_id FROM publishqueue pq WHERE pq.action NOT IN (?, ?, ?, ?) AND pq.obj_type = ? AND delay = ?");
			if (forPublish) {
				sql.append(" AND publish_flag = ?");
			}
			if (nodeIds.length > 0) {
				sql.append(" AND channel_id IN (").append(StringUtils.repeat("?", nodeIds.length, ",")).append(")");
			}
			sql.append(" GROUP BY channel_id");
			return DBUtils.select(sql.toString(), stmt -> {
				int index = 1;
				stmt.setString(index++, PublishQueue.Action.DELETE.toString()); // pq.action NOT IN (?, ?, ?, ?)
				stmt.setString(index++, PublishQueue.Action.HIDE.toString()); // pq.action NOT IN (?, ?, ?, ?)
				stmt.setString(index++, PublishQueue.Action.REMOVE.toString()); // pq.action NOT IN (?, ?, ?, ?)
				stmt.setString(index++, PublishQueue.Action.OFFLINE.toString()); // pq.action NOT IN (?, ?, ?, ?)
				stmt.setInt(index++, objType); // pq.obj_type = ?
				stmt.setBoolean(index++, delayed);// delay = ?
				if (forPublish) {
					stmt.setBoolean(index++, forPublish); // publish_flag = ?
				}
				if (nodeIds.length > 0) {
					for (int nodeId : nodeIds) {
						stmt.setInt(index++, nodeId); // channel_id in ()
					}
				}
			}, rs -> {
				Map<Integer, Integer> map = new HashMap<>();

				while (rs.next()) {
					int id = rs.getInt("channel_id");
					int count = rs.getInt("c");
					map.put(id, count);
				}
				return map;
			});
		});
	}

	/**
	 * Count dirted objects of all types for the given nodes
	 * @param forPublish true to count objects marked for the publish run, false to count objects not marked, and null to not consider flag
	 * @param nodeIds optional list of node IDs for restriction
	 * @return map of node ID to publish queue counts (containing counts for dirted and delayed objects)
	 * @throws NodeException
	 */
	public static Map<Integer, PublishQueueCounts> countDirtedObjectsPerNode(Boolean forPublish, int... nodeIds) throws NodeException {
		return TransactionManager.execute(() -> {
			StringBuilder sql = new StringBuilder();
			sql.append(
					"SELECT COUNT(DISTINCT pq.obj_id) c, channel_id, obj_type, delay FROM publishqueue pq WHERE pq.action != ? AND pq.action != ? AND pq.action != ? AND pq.action != ?");
			if (nodeIds.length > 0) {
				sql.append(" AND channel_id IN (").append(StringUtils.repeat("?", nodeIds.length, ",")).append(")");
			}
			if (forPublish != null) {
				sql.append(" AND publish_flag = ?");
			}
			sql.append(" GROUP BY channel_id, obj_type, delay");
			return DBUtils.select(sql.toString(), stmt -> {
				int index = 1;
				stmt.setString(index++, PublishQueue.Action.DELETE.toString());
				stmt.setString(index++, PublishQueue.Action.HIDE.toString());
				stmt.setString(index++, PublishQueue.Action.REMOVE.toString());
				stmt.setString(index++, PublishQueue.Action.OFFLINE.toString());
				if (nodeIds.length > 0) {
					for (int nodeId : nodeIds) {
						stmt.setInt(index++, nodeId);
					}
				}
				if (forPublish != null) {
					stmt.setBoolean(index++, forPublish);
				}
			}, rs -> {
				Map<Integer, PublishQueueCounts> map = new HashMap<>();

				while (rs.next()) {
					int id = rs.getInt("channel_id");
					int type = rs.getInt("obj_type");
					int count = rs.getInt("c");
					boolean delayed = rs.getBoolean("delay");

					PublishQueueCounts counts = map.computeIfAbsent(id, k -> new PublishQueueCounts()
						.setFiles(new ObjectCount())
						.setFolders(new ObjectCount())
						.setForms(new ObjectCount())
						.setPages(new ObjectCount()));
					ObjectCount objectCounts = null;
					switch (type) {
					case Page.TYPE_PAGE:
						objectCounts = counts.getPages();
						break;
					case Folder.TYPE_FOLDER:
						objectCounts = counts.getFolders();
						break;
					case File.TYPE_FILE:
						objectCounts = counts.getFiles();
						break;
					case Form.TYPE_FORM:
						objectCounts = counts.getForms();
						break;
					}
					if (objectCounts != null) {
						if (delayed) {
							objectCounts.setDelayed(count);
						} else {
							objectCounts.setToPublish(count);
						}
					}
				}
				return map;
			});
		});
	}

	/**
	 * Initiate the given publish action on the given object in the given channel.
	 * This means that the publish process is about to perform the given action.
	 * When all initiated actions are successfully performed on an object, it will be immediately removed from the publish queue
	 * @param objType object type
	 * @param objId object id
	 * @param channelId channel id
	 * @param action publish action
	 * @throws NodeException
	 */
	public synchronized static void initiatePublishAction(int objType, int objId, int channelId, PublishAction action) throws NodeException {
		if (logger.isDebugEnabled()) {
			logger.debug("Initiate Publish Action " + action + " for object " + objType + "." + objId + " in channel " + channelId);
		}
		HandledObject.get(objType, objId, channelId, true).initiatePublishAction(action);
	}

	/**
	 * Report the publish action as "done" on the given object/channel
	 * @param objType object type
	 * @param objId object id
	 * @param channelId channel id
	 * @param action publish action
	 * @throws NodeException
	 */
	public synchronized static void reportPublishActionDone(int objType, int objId, int channelId, PublishAction action) throws NodeException {
		if (logger.isDebugEnabled()) {
			logger.debug("Report Publish Action " + action + " for object " + objType + "." + objId + " in channel " + channelId + " done");
		}
		HandledObject obj = HandledObject.get(objType, objId, channelId, false);
		if (obj == null) {
			throw new NodeException("Cannot mark action " + action + " as finished for object " + objType + "." + objId + " in channel " + channelId
					+ ", because it was never initiated");
		}

		obj.publishActionDone(action);
	}

	/**
	 * Recursively dirt the folder in the given channel
	 * @param folder folder
	 * @param channelId channel
	 * @param filterCreationDate true if folders shall be filtered by creation date
	 * @param rangeStart start value of the filtered range
	 * @param rangeEnd end value of the filtered range
	 * @param dirtAction dirt action
	 * @param attributes optional list of attributes
	 * @throws NodeException
	 */
	protected static void recursiveDirtFolder(Folder folder, int channelId, boolean filterCreationDate, int rangeStart, int rangeEnd, Action dirtAction,
			String... attributes) throws NodeException {
		if (filterCreationDate) {
			// range restriction
			int cdate = folder.getCDate().getIntTimestamp();

			if (rangeStart <= cdate && cdate <= rangeEnd) {
				dirtObject(Folder.TYPE_FOLDER, ObjectTransformer.getInt(folder.getId(), 0), dirtAction, channelId, false, attributes);
			}
		} else {
			dirtObject(Folder.TYPE_FOLDER, ObjectTransformer.getInt(folder.getId(), 0), dirtAction, channelId, false, attributes);
		}
		for (Folder child : folder.getChildFolders()) {
			recursiveDirtFolder(child, channelId, filterCreationDate, rangeStart, rangeEnd, dirtAction, attributes);
		}
	}

	/**
	 * Get the selected nodes
	 * @param nodeId node/channel ids (for restricting to nodes/channels). If null, no restriction will be done
	 * @return collection of nodes
	 * @throws NodeException
	 */
	protected static Collection<Node> getNodes(int[] nodeId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		final Collection<Integer> selectedNodeIds = new ArrayList<Integer>();

		if (nodeId == null) {
			DBUtils.executeStatement("SELECT id FROM node", new SQLExecutor() {
				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						selectedNodeIds.add(rs.getInt("id"));
					}
				}
			});
		} else {
			for (int id : nodeId) {
				selectedNodeIds.add(id);
			}
		}

		return t.getObjects(Node.class, selectedNodeIds);
	}

	/**
	 * Dirt the given object for the channel id
	 * @param objType object type
	 * @param objId object id
	 * @param action dirting action
	 * @param channelId channel id (0 means: for all channels)
	 * @param checkExisting when true, this method first checks whether the object is already dirted
	 * @param attributes optional list of attributes
	 * @return stored entry or null if the entry will be stored later
	 * @throws NodeException
	 */
	protected static Entry dirtObject(final int objType, final int objId, final Action action, final int channelId, boolean checkExisting, String... attributes)
			throws NodeException {
		if (action == null) {
			throw new NodeException("Cannot dirt object without action");
		}

		if (checkExisting && action == Action.DEPENDENCY && doFastDependencyDirting(objType, objId, channelId, new HashSet<>(Arrays.asList(attributes)))) {
			return null;
		}

		Transaction t = TransactionManager.getCurrentTransaction();

		if (checkExisting) {
			// get possibly existing publishqueue entries
			final List<Entry> savedEntries = new ArrayList<PublishQueue.Entry>(1);

			DBUtils.executeStatement("SELECT * FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND action = ? AND channel_id = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, objType);
					stmt.setInt(2, objId);
					stmt.setString(3, action.toString());
					stmt.setInt(4, channelId);
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						savedEntries.add(new Entry(rs));
					}
				}
			});

			if (!savedEntries.isEmpty()) {
				Entry entry = savedEntries.get(0);

				// Check if the object is delayed
				if (entry.isDelayed()) {
					// Clear the delay
					DBUtils.executeUpdate("UPDATE publishqueue SET delay = 0 WHERE obj_type = ? AND obj_id = ? AND channel_id = ?",
							new Object[] { entry.getObjType(), entry.getObjId(), entry.getChannelId() });
				}

				setAttributes(entry.id, true, attributes);
				return entry;
			}
		}

		// create a new entry
		Entry entry = new Entry(objType, objId, action, channelId, false, t.getUnixTimestamp());

		// store the entry
		List<Integer> ids = DBUtils.executeInsert("INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) VALUES (?, ?, ?, ?, ?)",
				new Object[] { objType, objId, action.toString(), channelId, t.getUnixTimestamp()});

		if (ids.size() == 0) {
			throw new NodeException("Error while dirting object " + objType + "." + objId + ": could not extract generated key from publishqueue");
		}

		// set the id
		entry.id = ids.get(0);

		// when action was DELETE, we can remove all other publishqueue entries for that object
		if (action == Action.DELETE || action == Action.OFFLINE) {
			DBUtils.executeUpdate("DELETE FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND id != ? AND action NOT IN (?, ?, ?)",
					new Object[] { objType, objId, entry.id, Action.DELETE.toString(), Action.OFFLINE.toString(), Action.REMOVE.toString()});
		}

		// when action was REMOVE or OFFLINE or HIDE, we can remove all other publishqueue entries for the object in the channel
		if (action == Action.REMOVE || action == Action.OFFLINE || action == Action.HIDE) {
			DBUtils.executeUpdate("DELETE FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND channel_id = ? AND id != ?",
					new Object[] { objType, objId, channelId, entry.id});
		}

		// when action was MOVE, was can remove all REMOVE entries for the object in the channel
		if (action == Action.MOVE) {
			DBUtils.executeUpdate("DELETE FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND channel_id = ? AND action = ?",
					new Object[] { objType, objId, channelId, Action.REMOVE.toString()});
		}

		// when action was UNHIDE, we can remove all HIDE entries for the object in the channel
		if (action == Action.UNHIDE) {
			DBUtils.executeUpdate("DELETE FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND channel_id = ? AND action = ?",
					new Object[] { objType, objId, channelId, Action.HIDE.toString()});
		}

		// when action was CREATE, we must remove all DELETE entries for the object in the channel
		// This fixes situations were objects are restored from wastebin right after they were put into the wastebin
		if (action == Action.CREATE) {
			DBUtils.executeUpdate("DELETE FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND channel_id = ? AND action = ?", new Object[] { objType,
					objId, channelId, Action.DELETE.toString() });
		}

		// increase the dirt counter
		DirtCounter counter = DependencyManager.getDirtCounter();

		if (counter != null) {
			counter.inc();
		}

		setAttributes(entry.id, false, attributes);

		// return the entry
		return entry;
	}

	/**
	 * Other variant of {@link #setAttributes(int, boolean, String...)}
	 * @param entryId entry ID
	 * @param checkExisting true to check for existing entry
	 * @param attributes set of attributes
	 * @throws NodeException
	 */
	protected static void setAttributes(int entryId, boolean checkExisting, Collection<String> attributes) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// nothing to do if either feature is not activated or the entry is new and no attributes should be stored
		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING) || (!checkExisting && ObjectTransformer.isEmpty(attributes))) {
			return;
		}

		List<Object[]> argsList = new ArrayList<>();
		if (checkExisting && ObjectTransformer.isEmpty(attributes)) {
			// entry is old and object shall be dirted as a whole -> remove any existing attributes
			DBUtils.executeStatement("DELETE FROM publishqueue_attribute WHERE publishqueue_id = ?", Transaction.DELETE_STATEMENT, p -> p.setInt(1, entryId));
		} else if (checkExisting) {
			// entry is old, check which attributes are already stored and add the rest
			Set<String> existing = new HashSet<>();
			DBUtils.executeStatement("SELECT name FROM publishqueue_attribute WHERE publishqueue_id = ?", Transaction.SELECT_STATEMENT,
					p -> p.setInt(1, entryId), r -> {
						while (r.next()) {
							existing.add(r.getString("name"));
						}
					});
			if (existing.isEmpty()) {
				return;
			}

			for (String attribute : attributes) {
				if (existing.contains(attribute)) {
					continue;
				}
				argsList.add(new Object[] {entryId, attribute});
			}
		} else {
			// entry is new, store all attributes
			for (String attribute : attributes) {
				argsList.add(new Object[] {entryId, attribute});
			}
		}

		if (!argsList.isEmpty()) {
			DBUtils.executeBatchInsert("INSERT INTO publishqueue_attribute (publishqueue_id, name) VALUES (?, ?)", argsList);
		}
	}

	/**
	 * If the Feature {@link Feature#ATTRIBUTE_DIRTING} is set, add the optional list of attributes to the publishqueue entry.
	 * When the entry already exists, but has no attributes set, nothing will be changed (because "no attributes" means dirting everything and setting attributes would restrict dirting)
	 * @param entryId publishqueue entry ID
	 * @param checkExisting true to check for already existing attributes
	 * @param attributes optional list of attributes to add
	 * @throws NodeException
	 */
	protected static void setAttributes(int entryId, boolean checkExisting, String...attributes) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (!t.getNodeConfig().getDefaultPreferences().isFeature(Feature.ATTRIBUTE_DIRTING) || (!checkExisting && ObjectTransformer.isEmpty(attributes))) {
			return;
		}

		setAttributes(entryId, checkExisting, Arrays.asList(attributes));
	}

	/**
	 * Get the action when the event mask for {@link #getAction} contains {@link Events#EVENT_CN_PAGESTATUS}.
	 *
	 * @param page page to check
	 * @param property The event properties
	 * @return The necessary <code>Action</code> or <code>null</code> if no action could be determined
	 * @throws NodeException On errors
	 */
	private static Action getCnPageStatusAction(Page page, String[] property) throws NodeException {
		if (!ObjectTransformer.isEmpty(property)) {
			if ("online".equals(property[0])) {
				return page.isOnline() ? Action.ONLINE : Action.OFFLINE;
			}
		}

		if (page.isOnline()) {
			return Action.MODIFY;
		}

		return null;
	}

	/**
	 * Get the action when the event mask for {@link #getAction} contains {@link Events#EVENT_CN_PAGESTATUS} for an instance of {@link PublishableNodeObjectInFolder}
	 * @param object object to check
	 * @param property event properties
	 * @return The necessary <code>Action</code> or <code>null</code> if no action could be determined
	 * @throws NodeException
	 */
	private static Action getStatusAction(PublishableNodeObjectInFolder object, String[] property)
			throws NodeException {
		if (!ObjectTransformer.isEmpty(property)) {
			if ("online".equals(property[0])) {
				return object.isOnline() ? Action.ONLINE : Action.OFFLINE;
			}
		}

		if (object.isOnline()) {
			return Action.MODIFY;
		}

		return null;
	}

	/**
	 * Get the action for the given event mask
	 * @param object object that has to be dirted
	 * @param eventMask event mask
	 * @param property event properties
	 * @return action or null
	 */
	public static Action getAction(NodeObject object, int eventMask, String[] property) throws NodeException {
		int tType = ObjectTransformer.getInt(object.getTType(), 0);

		switch (tType) {
		case File.TYPE_FILE:
		case ImageFile.TYPE_IMAGE:
		case Folder.TYPE_FOLDER:
			if (Events.isEvent(eventMask, Events.HIDE)) {
				return Action.HIDE;
			}

			if (Events.isEvent(eventMask, Events.REVEAL)) {
				return Action.UNHIDE;
			}

			if (Events.isEvent(eventMask, Events.UPDATE)) {
				return Action.MODIFY;
			}

			if (Events.isEvent(eventMask, Events.CREATE)) {
				return Action.CREATE;
			}

			if (Events.isEvent(eventMask, Events.DELETE)) {
				return Action.DELETE;
			}

			if (Events.isEvent(eventMask, Events.MOVE)) {
				return Action.MOVE;
			}

			return null;

		case Page.TYPE_PAGE:
			if (Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS) && object instanceof Page) {
				Action ret;

				if ((ret = getCnPageStatusAction((Page) object, property)) != null) {
					return ret;
				}
			}

			if (Events.isEvent(eventMask, Events.HIDE)) {
				return Action.HIDE;
			}

			if (Events.isEvent(eventMask, Events.REVEAL)) {
				return Action.UNHIDE;
			}

			if (Events.isEvent(eventMask, Events.UPDATE) && object instanceof Page) {
				Page page = (Page) object;
				boolean isOnline = page.isOnline();

				if (isOnline) {
					return Action.MODIFY;
				}
			}

			if (Events.isEvent(eventMask, Events.DELETE)) {
				return Action.DELETE;
			}

			if (Events.isEvent(eventMask, Events.MOVE) && object instanceof Page) {
				Page page = (Page) object;
				boolean onlineAndNotModified = page.isOnline() && !page.isModified();

				if (onlineAndNotModified) {
					return Action.MOVE;
				}
			}

			break;
		case Form.TYPE_FORM:
			if (Events.isEvent(eventMask, Events.EVENT_CN_PAGESTATUS) && object instanceof Form) {
				Action ret;

				if ((ret = getStatusAction((Form) object, property)) != null) {
					return ret;
				}
			}

			if (Events.isEvent(eventMask, Events.DELETE)) {
				return Action.DELETE;
			}

			break;
		}

		return null;
	}

	/**
	 * Update the publish_flag to 0 for the given object in the given channel.
	 * This is done in a separate transaction
	 * @param object object
	 * @param node node
	 * @throws NodeException
	 */
	public static void requeueObject(NodeObject object, Node node) throws NodeException {
		if (object == null || node == null) {
			return;
		}
		TransactionManager.execute(() -> {
			DBUtils.executeStatement("UPDATE publishqueue SET publish_flag = ? WHERE obj_type = ? AND obj_id = ? AND channel_id = ? AND publish_flag = ?",
				Transaction.UPDATE_STATEMENT, (st) -> {
					st.setInt(1, 0);
					int objType = ObjectTransformer.getInt(object.getTType(), 0);
					if (objType == ImageFile.TYPE_IMAGE) {
						objType = File.TYPE_FILE;
					}
					st.setInt(2, objType);
					st.setInt(3, object.getId());
					st.setInt(4, node.getId());
					st.setInt(5, 1);
				});
		});
	}

	/**
	 * Enumeration of possible publish queue actions
	 */
	public static enum Action {

		/**
		 * Object was created
		 */
		CREATE, /**
		 * Object (Page) was taken online
		 */ ONLINE, /**
		 * Object was modified
		 */ MODIFY, /**
		 * Object was deleted (completely)
		 */ DELETE, /**
		 * Object (Page) was taken offline
		 */ OFFLINE, /**
		 * Object was moved to the node
		 */ MOVE, /**
		 * Object was moved from the node to another node
		 */ REMOVE, /**
		 * A dependency of the object was changed
		 */ DEPENDENCY, /**
		 * A former inherited object is no longer vixible, because it is hidden by a localized copy now
		 */ HIDE, /**
		 * A inherited object that was hidden by a localized version is unhidden, because the localized version was removed
		 */ UNHIDE;
	}

	/**
	 * Implementation of a publish queue entry
	 */
	public static class Entry {

		/**
		 * Internal ID of publish queue entries
		 */
		protected int id = -1;

		/**
		 * Type of the dirted object
		 */
		protected int objType;

		/**
		 * ID of the dirted object
		 */
		protected int objId;

		/**
		 * Publish action
		 */
		protected Action action;

		/**
		 * Channel ID
		 */
		protected int channelId;

		/**
		 * Delay
		 */
		protected boolean delay;

		/**
		 * Creation timestamp
		 */
		protected int timestamp;

		/**
		 * Create a new instance (without an id)
		 * @param objType object type
		 * @param objId object id
		 * @param action action
		 * @param channelId channel id
		 * @param timestamp timestamp
		 */
		protected Entry(int objType, int objId, Action action, int channelId, boolean delay, int timestamp) {
			this.objType = objType;
			this.objId = objId;
			this.action = action;
			this.channelId = channelId;
			this.delay = delay;
			this.timestamp = timestamp;
		}

		/**
		 * Create an instance for the data from the resultset
		 * @param rs resultset
		 * @throws SQLException
		 */
		protected Entry(ResultSet rs) throws SQLException {
			this(rs.getInt("obj_type"), rs.getInt("obj_id"), Action.valueOf(rs.getString("action")), rs.getInt("channel_id"), rs.getBoolean("delay"),
					rs.getInt("timestamp"));

			this.id = rs.getInt("id");
		}

		/**
		 * @return the id
		 */
		public int getId() {
			return id;
		}

		/**
		 * @param id the id to set
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * @return the objType
		 */
		public int getObjType() {
			return objType;
		}

		/**
		 * @param objType the objType to set
		 */
		public void setObjType(int objType) {
			this.objType = objType;
		}

		/**
		 * @return the objId
		 */
		public int getObjId() {
			return objId;
		}

		/**
		 * @param objId the objId to set
		 */
		public void setObjId(int objId) {
			this.objId = objId;
		}

		/**
		 * @return the channelId
		 */
		public int getChannelId() {
			return channelId;
		}

		/**
		 * @param channelId the channelId to set
		 */
		public void setChannelId(int channelId) {
			this.channelId = channelId;
		}

		/**
		 * Get if the object is delayed
		 * @return the delay
		 */
		public boolean isDelayed() {
			return delay;
		}

		/**
		 * Set if the object should be delayed
		 * @param delay the delay to set
		 */
		public void setDelayed(boolean delay) {
			this.delay = delay;
		}
	}

	/**
	 * Possible Publish Actions, that can be reported to the PublishQueue
	 */
	public static enum PublishAction {
		/**
		 * Update the publish table (pages only)
		 */
		UPDATE_PUBLISH_TABLE,

		/**
		 * Write the object into the cr
		 */
		WRITE_CR
	}

	/**
	 * Class for encapsulating nodes and attributes
	 */
	public static class NodeObjectWithAttributes<T extends NodeObject> {
		/**
		 * Object
		 */
		protected T object;

		/**
		 * Attributes
		 */
		protected Set<String> attributes;

		/**
		 * Create an instance without attributes
		 * @param object object
		 */
		public NodeObjectWithAttributes (T object) {
			this(object, null);
		}

		/**
		 * Create an instance
		 * @param object object
		 * @param attributes attributes
		 */
		public NodeObjectWithAttributes (T object, Set<String> attributes) {
			this.object = object;
			this.attributes = attributes;
		}

		/**
		 * Get the object
		 * @return object
		 */
		public T getObject() {
			return object;
		}

		/**
		 * Get the attributes
		 * @return attributes
		 */
		public Set<String> getAttributes() {
			return attributes;
		}

		/**
		 * Set the attributes
		 * @param attributes
		 */
		public void setAttributes(Set<String> attributes) {
			this.attributes = attributes;
		}

		/**
		 * Adds the given set of attributes to this object's attributes.
		 *
		 * If either this object's or the given attributes are
		 * <code>null</code> this objects attributes will be
		 * <code>null</code> after this method call, because this
		 * means that all attributes are affected.
		 *
		 * @param otherAttributes The set of attributes that should be added
		 *		to this one.
		 */
		public void mergeAttributes(Set<String> otherAttributes) {
			if (attributes == null) {
				return;
			}

			if (otherAttributes == null) {
				attributes = null;
			} else {
				attributes.addAll(otherAttributes);
			}
		}

		@Override
		public String toString() {
			// TODO add info about attributes (if set)
			return object.toString();
		}
	}

	/**
	 * Class for encapsulating object IDs and attributes
	 */
	public static class NodeObjectIdWithAttributes {
		/**
		 * Object ID
		 */
		protected int id;

		/**
		 * Attributes
		 */
		protected Set<String> attributes;

		/**
		 * Create an instance
		 * @param objId object ID
		 * @param attributes set of attributes
		 */
		public NodeObjectIdWithAttributes(int objId, Set<String> attributes) {
			this.id = objId;
			this.attributes = attributes;
		}

		/**
		 * Create an instance from an object with attributes
		 * @param object object with attributes
		 */
		public NodeObjectIdWithAttributes(NodeObjectWithAttributes<? extends NodeObject> object) {
			this(object.object.getId(), object.attributes);
		}

		/**
		 * Get the ID
		 * @return ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * Get the attributes
		 * @return attributes
		 */
		public Set<String> getAttributes() {
			return attributes;
		}

		@Override
		public String toString() {
			// TODO add info about attributes (if set)
			return Integer.toString(id);
		}
	}

	/**
	 * Inner helper class to store, which publish actions have been initiated
	 * and which have been reported "done" for objects during a publish process
	 */
	protected static class HandledObject {
		/**
		 * Map of objects handled during publish process
		 */
		protected static Map<String, HandledObject> objectsMap = new HashMap<String, PublishQueue.HandledObject>();

		/**
		 * List of handled objects, that are marked done
		 */
		protected static List<HandledObject> done = new ArrayList<HandledObject>();

		/**
		 * Key of the published object. The key is constructed as "obj_type.obj_id.channel_id"
		 */
		protected String key;

		/**
		 * object type
		 */
		protected int objType;

		/**
		 * object id
		 */
		protected int objId;

		/**
		 * channel id
		 */
		protected int channelId;

		/**
		 * Map containing all initiated actions. When an action is initiated, the value is false, if it
		 * is reported "done", the value is changed to "true"
		 */
		protected Map<PublishAction, Boolean> publishActions = new HashMap<PublishAction, Boolean>();

		/**
		 * Clear the map of handled objects
		 */
		public static void clear() {
			objectsMap.clear();
		}

		/**
		 * Get the instance for the given object
		 * @param objType object type
		 * @param objId object id
		 * @param channelId channel id
		 * @param create true if the instance shall be created if not yet done
		 * @return the instance, this may return null if create is false and the instance was not created before
		 */
		protected static synchronized HandledObject get(int objType, int objId, int channelId, boolean create) {
			String key = createKey(objType, objId, channelId);

			// get the object (create, if not existent)
			HandledObject handledObject = objectsMap.get(key);
			if (handledObject == null && create) {
				handledObject = new HandledObject(key, objType, objId, channelId);
				objectsMap.put(key, handledObject);
			}

			return handledObject;
		}

		/**
		 * Create the key for the given object/channel
		 * @param objType object type
		 * @param objId object id
		 * @param channelId channel id
		 * @return key
		 */
		protected static String createKey(int objType, int objId, int channelId) {
			// generate the key for the object/channel
			StringBuilder keyBuilder = new StringBuilder();
			keyBuilder.append(objType).append(".").append(objId).append(".").append(channelId);
			return keyBuilder.toString();
		}

		/**
		 * Create an instance with given key
		 * @param key object key
		 * @param objType object type
		 * @param objId object id
		 * @param channelId channel id
		 */
		protected HandledObject(String key, int objType, int objId, int channelId) {
			this.key = key;
			this.objType = objType;
			this.objId = objId;
			this.channelId = channelId;
		}

		/**
		 * Initiate the publish action on the object
		 * @param action publish action
		 * @throws NodeException if the publish action was already initiated
		 */
		protected synchronized void initiatePublishAction(PublishAction action) throws NodeException {
			if (publishActions.containsKey(action)) {
				throw new NodeException("Action " + action + " was initiated for object " + key + " twice");
			}
			publishActions.put(action, false);
		}

		/**
		 * Mark a publish action "done"
		 * @param action publish action
		 * @throws NodeException if the publish action was not initiated
		 */
		protected synchronized void publishActionDone(PublishAction action) throws NodeException {
			if (!publishActions.containsKey(action)) {
				throw new NodeException("Cannot mark action " + action + " as finished for object " + key + ", because it was never initiated");
			}
			publishActions.put(action, true);

			boolean allFinished = true;
			for (Boolean actionFinished : publishActions.values()) {
				allFinished &= actionFinished;
			}

			if (allFinished) {
				markObjectFinished();

				// remove from the map
				objectsMap.remove(key);
			}
		}

		/**
		 * Mark the object "finished" in the publish queue
		 * @throws NodeException
		 */
		protected void markObjectFinished() throws NodeException {
			synchronized (done) {
				done.add(this);

				if (done.size() >= batchSize) {
					markObjectsFinished();
				}
			}
		}

		/**
		 * Remove all objects currently listed as {@link #done} from the publishqueue.
		 * This will add a new {@link Callable} to the {@link PublishQueue#removerService}.
		 * @throws NodeException
		 */
		public static void markObjectsFinished() throws NodeException {
			if (!done.isEmpty() && removerService != null) {
				removerService.submit(new BatchedRemover(new ArrayList<HandledObject>(done)));
				done.clear();
			}
		}
	}

	/**
	 * Callable implementation that removes a batch of objects from the publishqueue
	 */
	protected static class BatchedRemover implements Callable<Integer> {
		/**
		 * Batch to be removed
		 */
		protected List<HandledObject> batch;

		/**
		 * Create an instance for the given batch
		 * @param batch batch
		 * @throws TransactionException
		 */
		public BatchedRemover(List<HandledObject> batch) throws TransactionException {
			this.batch = batch;
		}

		@Override
		public Integer call() throws Exception {
			Trx.operate(t -> {
				PreparedStatement stmt = null;

				try {
					stmt = t.prepareDeleteStatement("DELETE FROM publishqueue WHERE obj_type = ? AND obj_id = ? AND channel_id = ? AND publish_flag = ?");
					for (HandledObject obj : batch) {
						stmt.setInt(1, obj.objType);
						stmt.setInt(2, obj.objId);
						stmt.setInt(3, obj.channelId);
						stmt.setInt(4, 1);
						stmt.addBatch();
					}

					stmt.executeBatch();
				} catch (SQLException e) {
					throw new NodeException("Error while removing publishqueue entries", e);
				} finally {
					t.closeStatement(stmt);
				}
			});

			return batch.size();
		}
	}
}
