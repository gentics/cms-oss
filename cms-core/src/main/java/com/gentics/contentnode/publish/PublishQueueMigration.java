package com.gentics.contentnode.publish;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.MultiChannellingFallbackList;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.DummyObject;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * Migration tool for the new dirt mechanism
 */
public class PublishQueueMigration {

	/**
	 * Migrate the objects, that were dirted the old way (pages with status, all objects with logcmd entries) to the publishqueue
	 * 
	 * @throws NodeException
	 */
	public static void migrateDirtedObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get all pages in status 1
		final List<Integer> pageIds = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT id FROM page WHERE status = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, 1); // status = ?
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					pageIds.add(rs.getInt("id"));
				}
			}
		});
		List<Page> pagesToDirt = t.getObjects(Page.class, pageIds);

		// dirt them (for all channels they are visible in)
		for (Page page : pagesToDirt) {
			PublishQueue.dirtObject(page, Action.DEPENDENCY, 0);
		}

		// get all nodes
		final List<Integer> nodeIds = new ArrayList<Integer>();

		DBUtils.executeStatement("SELECT id FROM node", new SQLExecutor() {
			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					nodeIds.add(rs.getInt("id"));
				}
			}
		});
		List<Node> nodes = t.getObjects(Node.class, nodeIds);
		List<? extends NodeObject> objects = null;

		int minTimestamp = Integer.MAX_VALUE;

		// iterate over the nodes
		for (Node node : nodes) {
			// get the channel id
			int channelId = ObjectTransformer.getInt(node.getId(), 0);

			// get the last publish timestamp for the node
			int lastPublishTimestamp = node.getLastPublishTimestamp();

			minTimestamp = Math.min(minTimestamp, lastPublishTimestamp);

			// get all modified files from the logcmd table (since the last publish timestamp)
			objects = getModifiedFiles(node, lastPublishTimestamp);
			// and dirt them
			for (NodeObject file : objects) {
				PublishQueue.dirtObject(file, Action.DEPENDENCY, 0);
			}

			// get all modified folders from the logcmd table (since the last publish timestamp)
			objects = getModifiedFolders(node, lastPublishTimestamp);
			// and dirt them
			for (NodeObject folder : objects) {
				PublishQueue.dirtObject(folder, Action.DEPENDENCY, 0);
			}

			// dirt the moved objects
			objects = getMovedObjects(Page.class, node, lastPublishTimestamp);
			for (NodeObject page : objects) {
				PublishQueue.dirtObject(page, Action.REMOVE, channelId);
				PublishQueue.dirtObject(page, Action.MOVE, ObjectTransformer.getInt(((Page) page).getOwningNode().getId(), 0));
			}
			objects = getMovedObjects(Folder.class, node, lastPublishTimestamp);
			for (NodeObject folder : objects) {
				PublishQueue.dirtObject(folder, Action.REMOVE, channelId);
			}
			objects = getMovedObjects(File.class, node, lastPublishTimestamp);
			for (NodeObject file : objects) {
				PublishQueue.dirtObject(file, Action.REMOVE, channelId);
			}

			if (node.isChannel()) {
				// dirt the hidden objects
				objects = getHiddenObjects(node, lastPublishTimestamp);
				for (NodeObject object : objects) {
					PublishQueue.dirtObject(object, Action.HIDE, channelId);
				}

				// dirt the unhidden objects
				objects = getUnHiddenObjects(node, lastPublishTimestamp);
				for (NodeObject object : objects) {
					PublishQueue.dirtObject(object, Action.UNHIDE, channelId);
				}
			}

			// dirt the deleted objects
			dirtDeletedObjects(node);
		}

		// dirt the pages taken offline
		objects = getOfflinePages(minTimestamp);
		for (NodeObject page : objects) {
			PublishQueue.dirtObject(page, Action.OFFLINE, 0);
		}

		// last update all pages from status 2 -> 1
		DBUtils.executeStatement("UPDATE page SET status = ? WHERE status = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, 1); // SET status = ?
				stmt.setInt(2, 2); // WHERE status = ?
			}
		});
	}

	/**
	 * Get the modified files that are published into the given node (check in the logcmd table)
	 * @param node node
	 * @param lastPublished last publish timestamp
	 * @return list of modified files
	 * @throws NodeException
	 */
	private static List<File> getModifiedFiles(Node node, int lastPublished) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement st = null;
		ResultSet res = null;

		int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(lastPublished);

		// flag to mark whether multichannelling must be considered when getting the files
		boolean multiChannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
		List<Node> masterNodes = node.getMasterNodes();
		Object nodeId = node.getId();

		if (multiChannelling && masterNodes.size() > 0) {
			nodeId = masterNodes.get(masterNodes.size() - 1).getId();
		}

		try {
			int[] objectChangingCommands = ActionLogger.getObjectChangingCommands();

			if (lastPublished >= 0) {
				StringBuffer sql = new StringBuffer();

				sql.append("SELECT ");
				// when multichannelling is considered, we need more information
				if (multiChannelling) {
					sql.append("contentfile.id id, contentfile.channelset_id channelset_id, contentfile.channel_id channel_id");
				} else {
					sql.append("contentfile.id id");
				}
				sql.append(" FROM contentfile, logcmd, folder, node ");
				sql.append("WHERE logcmd.o_id=contentfile.id AND ");
				sql.append("folder.id = contentfile.folder_id AND ");
				sql.append("( logcmd.timestamp >= ? ");
				if (dbTimestamp != -1) {
					sql.append("OR (logcmd.timestamp < ? && unix_timestamp(insert_timestamp) >= ?) ");
				}
				sql.append(") AND logcmd.o_type in (?, ?) AND ");
				if (multiChannelling) {
					sql.append("(logcmd.cmd_desc_id IN (");
					sql.append(StringUtils.repeat("?", objectChangingCommands.length, ","));
					sql.append(") OR (logcmd.cmd_desc_id = ? AND logcmd.o_id2 IN (0, ?))) ");
				} else {
					sql.append("logcmd.cmd_desc_id IN (");
					sql.append(StringUtils.repeat("?", objectChangingCommands.length, ","));
					sql.append(") ");
				}
				sql.append(" AND node.id = folder.node_id AND ");
				sql.append("node.publish_contentmap = ? AND ");
				sql.append("node.id = ? ");
				if (multiChannelling) {
					sql.append("AND contentfile.channel_id IN (");
					sql.append(StringUtils.repeat("?", masterNodes.size() + 2, ","));
					sql.append(") ");
				}
				sql.append("GROUP BY contentfile.id");

				st = t.prepareStatement(sql.toString());
				int pCounter = 1;

				st.setInt(pCounter++, lastPublished);
				if (dbTimestamp != -1) {
					st.setInt(pCounter++, lastPublished);
					st.setInt(pCounter++, dbTimestamp);
				}
				st.setInt(pCounter++, ContentFile.TYPE_FILE);
				st.setInt(pCounter++, ContentFile.TYPE_IMAGE);

				if (multiChannelling) {
					for (int i = 0; i < objectChangingCommands.length; i++) {
						st.setInt(pCounter++, objectChangingCommands[i]);
					}
					st.setInt(pCounter++, ActionLogger.MC_UNHIDE);
					st.setObject(pCounter++, node.getId());
				} else {
					for (int i = 0; i < objectChangingCommands.length; i++) {
						st.setInt(pCounter++, objectChangingCommands[i]);
					}
				}
				st.setInt(pCounter++, 1);
				st.setObject(pCounter++, nodeId);

				if (multiChannelling) {
					// for multichannelling, we must consider this node
					st.setObject(pCounter++, node.getId());
					// and all master nodes as well
					for (Node master : masterNodes) {
						st.setObject(pCounter++, master.getId());
					}
					// and of course all objects without channel_id
					st.setObject(pCounter++, 0);
				}
			} else {
				StringBuffer sql = new StringBuffer();

				sql.append("SELECT ");
				// when multichannelling is considered, we need more information
				if (multiChannelling) {
					sql.append("contentfile.id id, contentfile.channelset_id channelset_id, contentfile.channel_id channel_id, contentfile.mc_exclude, contentfile_disinherit.channel_id disinherited_node");
				} else {
					sql.append("contentfile.id id");
				}
				sql.append(" FROM contentfile, folder, node ");
				if (multiChannelling) {
					sql.append(" LEFT JOIN contentfile_disinherit on contentfile_disinherit.page_id = contentfile.id ");
				}
				sql.append("WHERE folder.id = contentfile.folder_id AND node.id = folder.node_id ");
				sql.append("AND node.publish_contentmap = ? AND ");
				sql.append("node.id = ? ");
				if (multiChannelling) {
					sql.append("AND contentfile.channel_id IN (");
					sql.append(StringUtils.repeat("?", masterNodes.size() + 2, ","));
					sql.append(") ");
				}

				st = t.prepareStatement(sql.toString());

				int pCounter = 1;

				st.setInt(pCounter++, 1);
				st.setObject(pCounter++, nodeId);
				if (multiChannelling) {
					// for multichannelling, we must consider this node
					st.setObject(pCounter++, node.getId());
					// and all master nodes as well
					for (Node master : masterNodes) {
						st.setObject(pCounter++, master.getId());
					}
					// and of course objects without channel_id
					st.setObject(pCounter++, 0);
				}
			}

			res = st.executeQuery();

			List<Integer> fileIds = null;

			if (multiChannelling) {
				// use a multichannel fallback list, which will take care
				// about object inheritance and local copies
				MultiChannellingFallbackList objectList = new MultiChannellingFallbackList(node);

				while (res.next()) {
					objectList.addObject(res.getInt("id"), res.getInt("channelset_id"), res.getInt("channel_id"), res.getBoolean("mc_exclude"), res.getInt("disinherited_node"));
				}

				fileIds = objectList.getObjectIds();
			} else {
				fileIds = new ArrayList<Integer>();
				while (res.next()) {
					fileIds.add(res.getInt("id"));
				}
			}

			return t.getObjects(File.class, fileIds);
		} catch (SQLException e) {
			throw new NodeException("Error while getting timestamp of last publish process for {" + node + "}", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(st);
		}
	}

	/**
	 * Get the list of modified folders published into the given node (check in logcmd table)
	 * @param node node
	 * @param lastPublished last publish timestamp
	 * @return list of modified folders
	 * @throws NodeException
	 */
	private static List<Folder> getModifiedFolders(Node node, int lastPublished) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement st = null;
		ResultSet res = null;

		int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(lastPublished);

		// by default we would fetch the folders for this node
		Object nodeId = node.getId();
		List<Object> nodeIds = new ArrayList<Object>();

		nodeIds.add(nodeId);

		boolean multichannelling = t.getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTICHANNELLING);
		List<Object> channelIds = new ArrayList<Object>();

		// if this node is a channel, we also fetch the folders from the
		// master nodes, but we will restrict the channel ids
		if (multichannelling) {
			List<Node> masterNodes = node.getMasterNodes();

			for (Node master : masterNodes) {
				nodeIds.add(master.getId());
				channelIds.add(master.getId());
			}

			channelIds.add(0);
			channelIds.add(nodeId);
		}

		try {
			List<Integer> folderIds = new ArrayList<Integer>();
			int[] objectChangingCommands = ActionLogger.getObjectChangingCommands();

			if (lastPublished >= 0) {
				StringBuffer sql = new StringBuffer();

				sql.append("SELECT folder.id id, folder.channel_id, folder.channelset_id FROM folder, logcmd, node ");
				sql.append("WHERE logcmd.o_id = folder.id AND ");
				sql.append("( logcmd.timestamp >= ? ");
				if (dbTimestamp != -1) {
					sql.append("OR (logcmd.timestamp < ? && unix_timestamp(insert_timestamp) >= ?) ");
				}
				sql.append(") AND logcmd.o_type = ? AND ");
				if (multichannelling) {
					sql.append("(logcmd.cmd_desc_id IN (");
					sql.append(StringUtils.repeat("?", objectChangingCommands.length, ","));
					sql.append(") OR (logcmd.cmd_desc_id = ? AND logcmd.o_id2 IN (0, ?))) ");
				} else {
					sql.append("logcmd.cmd_desc_id IN (");
					sql.append(StringUtils.repeat("?", objectChangingCommands.length, ","));
					sql.append(") ");
				}
				sql.append(" AND node.id = folder.node_id AND ");
				sql.append("node.publish_contentmap = ? AND ");
				sql.append("folder.node_id IN (");
				sql.append(StringUtils.repeat("?", nodeIds.size(), ","));
				sql.append(") ");
				if (multichannelling) {
					// restrict the folders to candidates for multichannel fallback
					sql.append("AND folder.channel_id IN (");
					sql.append(StringUtils.repeat("?", channelIds.size(), ","));
					sql.append(") ");
				}
				sql.append("GROUP BY folder.id");

				st = t.prepareStatement(sql.toString());
				int pCounter = 1;

				// set the last published timestamp
				st.setInt(pCounter++, lastPublished);
				if (dbTimestamp != -1) {
					st.setInt(pCounter++, lastPublished);
					st.setInt(pCounter++, dbTimestamp);
				}
				// set the type
				st.setInt(pCounter++, Folder.TYPE_FOLDER);

				if (multichannelling) {
					// set the cmd ids
					for (int i = 0; i < objectChangingCommands.length; i++) {
						st.setInt(pCounter++, objectChangingCommands[i]);
					}
					st.setInt(pCounter++, ActionLogger.MC_UNHIDE);
					st.setObject(pCounter++, node.getId());
				} else {
					// set the cmd ids
					for (int i = 0; i < objectChangingCommands.length; i++) {
						st.setInt(pCounter++, objectChangingCommands[i]);
					}
				}
				// set 1 (publish_contentmap)
				st.setInt(pCounter++, 1);
				// set the node ids
				for (Object nId : nodeIds) {
					st.setObject(pCounter++, nId);
				}

				// set the channel ids
				if (multichannelling) {
					for (Object cId : channelIds) {
						st.setObject(pCounter++, cId);
					}
				}
			} else {
				StringBuffer sql = new StringBuffer();

				sql.append("SELECT folder.id id, folder.channel_id, folder.channelset_id, folder.mc_exclude, folder_disinherit.channel_id disinherited_node FROM folder, node ");
				sql.append("LEFT JOIN folder_disinherit on folder_disinherit.folder_id = id ");
				sql.append("WHERE node.id = folder.node_id AND node.publish_contentmap = ? AND ");
				sql.append("node.id IN (");
				sql.append(StringUtils.repeat("?", nodeIds.size(), ","));
				sql.append(") ");
				if (multichannelling) {
					// restrict the folders to candidates for multichannel fallback
					sql.append("AND folder.channel_id IN (");
					sql.append(StringUtils.repeat("?", channelIds.size(), ","));
					sql.append(") ");
				}

				st = t.prepareStatement(sql.toString());
				int pCounter = 1;

				// set 1 (publish_contentmap)
				st.setInt(pCounter++, 1);
				// set the node ids
				for (Object nId : nodeIds) {
					st.setObject(pCounter++, nId);
				}

				// set the channel ids
				if (multichannelling) {
					for (Object cId : channelIds) {
						st.setObject(pCounter++, cId);
					}
				}
			}

			res = st.executeQuery();

			if (multichannelling) {
				// for multichannelling, we need to do the channel fallback
				MultiChannellingFallbackList fbList = new MultiChannellingFallbackList(node);

				while (res.next()) {
					fbList.addObject(res.getInt("id"), res.getInt("channelset_id"), res.getInt("channel_id"), res.getBoolean("mc_exclude"), res.getInt("disinherited_node"));
				}

				folderIds.addAll(fbList.getObjectIds());
			} else {
				while (res.next()) {
					folderIds.add(res.getInt("id"));
				}
			}

			return t.getObjects(Folder.class, folderIds);
		} catch (SQLException e) {
			throw new NodeException("Error while getting timestamp of last publish process for {" + node + "}", e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(st);
		}
	}

	/**
	 * Get the objects of given class that were (possibly) moved out of the given node since the given timestamp
	 * @param clazz object class
	 * @param node node
	 * @param timestamp timestamp
	 * @return list of (possibly) moved objects
	 * @throws NodeException
	 */
	private static <T extends NodeObject> List<T> getMovedObjects(Class<T> clazz, final Node node, final int timestamp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		String typeStr;

		final int type = t.getTType(clazz);

		switch (type) {
		case Page.TYPE_PAGE:
			typeStr = "page";
			break;

		case Folder.TYPE_FOLDER:
			typeStr = "folder";
			break;

		case ContentFile.TYPE_FILE:
			typeStr = "contentfile";
			break;

		default:
			throw new NodeException("Invalid type: {" + type + "}");
		}
		// get all pages/files or folders that were moved (eventually a parent folder was moved) into another node
		String sql;
		final int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(timestamp);

		if (type == Folder.TYPE_FOLDER) {
			sql = "select distinct logcmd.o_type, logcmd.o_id from " + typeStr + ", logcmd where " + typeStr + ".id = logcmd.o_id AND logcmd.o_type = ? AND "
					+ "folder.node_id != ? AND (" + "logcmd.timestamp >= ?"
					+ (dbTimestamp > 0 ? " OR (logcmd.timestamp < ? AND unix_timestamp(logcmd.insert_timestamp) >= ?)" : "") + ") AND logcmd.cmd_desc_id = ?";
		} else {
			sql = "select distinct logcmd.o_type, logcmd.o_id from " + typeStr + ", logcmd, folder where " + typeStr
					+ ".id = logcmd.o_id AND logcmd.o_type = ? AND " + typeStr + ".folder_id = folder.id AND " + "folder.node_id != ? AND (" + "logcmd.timestamp >= ?"
					+ (dbTimestamp > 0 ? " OR (logcmd.timestamp < ? AND unix_timestamp(logcmd.insert_timestamp) >= ?)" : "") + ") AND logcmd.cmd_desc_id = ?";
		}

		final List<Integer> objectIds = new ArrayList<Integer>();

		DBUtils.executeStatement(sql, new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int pCounter = 1;

				stmt.setInt(pCounter++, type); // logcmd.o_type = ?
				stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0)); // folder.node_id != ?
				stmt.setInt(pCounter++, timestamp); // logcmd.timestamp >= ?
				if (dbTimestamp > 0) {
					stmt.setInt(pCounter++, timestamp); // logcmd.timestamp < ?
					stmt.setInt(pCounter++, dbTimestamp); // unix_timestamp(logcmd.insert_timestamp) >= ?
				}
				stmt.setInt(pCounter++, ActionLogger.MOVE); // logcmd.cmd_desc_id = ?
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					objectIds.add(rs.getInt("o_id"));
				}
			}
		});

		return t.getObjects(clazz, objectIds);
	}

	/**
	 * Get the pages which were taken offline since the given timestamp
	 * @param timestamp timestamp
	 * @return list of pages taken offline
	 * @throws NodeException
	 */
	private static List<Page> getOfflinePages(final int timestamp) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		final int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(timestamp);
		final List<Integer> pageIds = new ArrayList<Integer>();
		SQLExecutor exe = new SQLExecutor() {

			/*
			 * (non-Javadoc)
			 * @see com.gentics.lib.db.SQLExecutorWrapper#prepareStatement(java.sql.PreparedStatement)
			 */
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				if (dbTimestamp > 0) {
					stmt.setInt(1, Page.TYPE_PAGE);
					stmt.setInt(2, ActionLogger.PAGEOFFLINE);
					stmt.setInt(3, ActionLogger.PAGETIME);
					stmt.setInt(4, timestamp);
					stmt.setInt(5, timestamp);
					stmt.setInt(6, dbTimestamp);
					stmt.setInt(7, Page.STATUS_OFFLINE);
					stmt.setInt(8, Page.STATUS_TIMEMANAGEMENT);
				} else {
					stmt.setInt(1, Page.TYPE_PAGE);
					stmt.setInt(2, ActionLogger.PAGEOFFLINE);
					stmt.setInt(3, ActionLogger.PAGETIME);
					stmt.setInt(4, timestamp);
					stmt.setInt(5, Page.STATUS_OFFLINE);
					stmt.setInt(6, Page.STATUS_TIMEMANAGEMENT);
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					pageIds.add(rs.getInt("o_id"));
				}
			}
		};

		// use ActionLogger class to query logs
		if (dbTimestamp > 0) {
			DBUtils.executeStatement(
					"SELECT DISTINCT logcmd.o_type, logcmd.o_id, folder.node_id FROM " + "page, logcmd, folder WHERE page.id=logcmd.o_id "
					+ "AND logcmd.o_type= ? AND logcmd.cmd_desc_id IN (?, ?) "
					+ " AND (logcmd.timestamp >= ? OR (logcmd.timestamp < ? AND unix_timestamp(logcmd.insert_timestamp) >= ?)) AND page.status IN (?, ?)"
					+ " AND folder.id = page.folder_id",
					exe);
		} else {
			DBUtils.executeStatement(
					"SELECT DISTINCT logcmd.o_type, logcmd.o_id, folder.node_id FROM " + "page, logcmd, folder WHERE page.id=logcmd.o_id "
					+ "AND logcmd.o_type= ? AND logcmd.cmd_desc_id IN (?, ?) " + " AND logcmd.timestamp >= ? AND page.status IN (?, ?)"
					+ " AND folder.id = page.folder_id",
					exe);
		}

		return t.getObjects(Page.class, pageIds);
	}

	/**
	 * Get the deleted objects for the given node
	 * @param node node
	 * @throws NodeException
	 */
	private static void dirtDeletedObjects(final Node node) throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();
		final int timestamp = node.getLastPublishTimestamp();
		final int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(timestamp);
		final Action action = Action.DELETE;
		SQLExecutor exe = new SQLExecutor() {

			/*
			 * (non-Javadoc)
			 * @see com.gentics.lib.db.SQLExecutor#prepareStatement(java.sql.PreparedStatement)
			 */
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int pCounter = 1;

				if (dbTimestamp > 0) {
					stmt.setString(pCounter++, action.toString());
					stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0));
					stmt.setInt(pCounter++, t.getUnixTimestamp());
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, dbTimestamp);
					stmt.setInt(pCounter++, ActionLogger.DEL);
				} else {
					stmt.setString(pCounter++, action.toString());
					stmt.setInt(pCounter++, ObjectTransformer.getInt(node.getId(), 0));
					stmt.setInt(pCounter++, t.getUnixTimestamp());
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, ActionLogger.DEL);
				}
			}
		};
		StringBuffer sql = new StringBuffer(
				"INSERT INTO publishqueue (obj_type, obj_id, action, channel_id, timestamp) SELECT DISTINCT o_type, o_id, ?, ?, ? FROM logcmd WHERE ");

		if (dbTimestamp > 0) {
			sql.append("(timestamp >= ? OR (timestamp < ? AND unix_timestamp(insert_timestamp) >= ?)) AND ");
			sql.append("cmd_desc_id = ?");
		} else {
			sql.append("timestamp >= ? AND ");
			sql.append("cmd_desc_id = ? ");
		}
		DBUtils.executeUpdateStatement(sql.toString(), exe);
	}

	/**
	 * Get the hidden objects
	 * @param node node
	 * @param timestamp timestamp
	 * @return hidden objects
	 * @throws NodeException
	 */
	private static List<NodeObject> getHiddenObjects(final Node node, final int timestamp) throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();
		final int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(timestamp);
		final List<NodeObject> objects = new ArrayList<NodeObject>();
		SQLExecutor exe = new SQLExecutor() {

			/*
			 * (non-Javadoc)
			 * @see com.gentics.lib.db.SQLExecutor#prepareStatement(java.sql.PreparedStatement)
			 */
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int pCounter = 1;

				if (dbTimestamp > 0) {
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, dbTimestamp);
					stmt.setInt(pCounter++, ActionLogger.MC_HIDE);
					stmt.setObject(pCounter++, node.getId());
				} else {
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, ActionLogger.MC_HIDE);
					stmt.setObject(pCounter++, node.getId());
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Class<? extends NodeObject> objClazz = t.getClass(rs.getInt("o_type"));

					if (objClazz != null) {
						objects.add(new DummyObject(rs.getInt("o_id"), t.createObjectInfo(objClazz)));
					}
				}
			}
		};
		StringBuffer sql = new StringBuffer("SELECT DISTINCT o_type, o_id FROM logcmd WHERE ");

		if (dbTimestamp > 0) {
			sql.append("(timestamp >= ? OR (timestamp < ? AND unix_timestamp(insert_timestamp) >= ?)) AND ");
			sql.append("cmd_desc_id = ? AND o_id2 = ?");
		} else {
			sql.append("timestamp >= ? AND ");
			sql.append("cmd_desc_id = ? AND o_id2 = ?");
		}
		DBUtils.executeStatement(sql.toString(), exe);

		return objects;
	}

	/**
	 * Get the unhidden objects
	 * @param node node
	 * @param timestamp timestamp
	 * @return hidden objects
	 * @throws NodeException
	 */
	private static List<NodeObject> getUnHiddenObjects(final Node node, final int timestamp) throws NodeException {
		final Transaction t = TransactionManager.getCurrentTransaction();
		final int dbTimestamp = PublishUtils.getDatabaseStartTimeOfPublish(timestamp);
		final List<NodeObject> objects = new ArrayList<NodeObject>();
		SQLExecutor exe = new SQLExecutor() {

			/*
			 * (non-Javadoc)
			 * @see com.gentics.lib.db.SQLExecutor#prepareStatement(java.sql.PreparedStatement)
			 */
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				int pCounter = 1;

				if (dbTimestamp > 0) {
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, dbTimestamp);
					stmt.setInt(pCounter++, ActionLogger.MC_UNHIDE);
					stmt.setObject(pCounter++, node.getId());
				} else {
					stmt.setInt(pCounter++, timestamp);
					stmt.setInt(pCounter++, ActionLogger.MC_UNHIDE);
					stmt.setObject(pCounter++, node.getId());
				}
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					Class<? extends NodeObject> objClazz = t.getClass(rs.getInt("o_type"));

					if (objClazz != null) {
						objects.add(new DummyObject(rs.getInt("o_id"), t.createObjectInfo(objClazz)));
					}
				}
			}
		};
		StringBuffer sql = new StringBuffer("SELECT DISTINCT o_type, o_id FROM logcmd WHERE ");

		if (dbTimestamp > 0) {
			sql.append("(timestamp >= ? OR (timestamp < ? AND unix_timestamp(insert_timestamp) >= ?)) AND ");
			sql.append("cmd_desc_id = ? AND o_id2 = ?");
		} else {
			sql.append("timestamp >= ? AND ");
			sql.append("cmd_desc_id = ? AND o_id2 = ?");
		}
		DBUtils.executeStatement(sql.toString(), exe);

		return objects;
	}
}
