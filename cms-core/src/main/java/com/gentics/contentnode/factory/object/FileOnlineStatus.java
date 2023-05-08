package com.gentics.contentnode.factory.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

/**
 * Static helper class for managing the online/offline status of files.
 * 
 * Every access to the table contentfile_online is done in a separate transaction (which is immediately committed), because when
 * data is inserted into this table, the foreign key to contentfile.id would lock the referenced contentfile record, which would possibly cause
 * a database lock if another transaction tries to update the channelset_id.
 */
public class FileOnlineStatus {

	/**
	 * Logger
	 */
	public final static NodeLogger logger = NodeLogger.getNodeLogger(FileOnlineStatus.class);

	/**
	 * Indexed status attributes
	 */
	protected final static String[] INDEXED_STATUS_ATTRIBUTES = {"online"};

	/**
	 * Check whether the given file is online for its node
	 * @param file file to check
	 * @return true if the file is online
	 * @throws NodeException
	 */
	public static boolean isOnline(File file) throws NodeException {
		if (file == null) {
			throw new NodeException("Cannot check online status for file null");
		}
		return isOnline(file, file.getFolder().getNode());
	}

	/**
	 * Check whether the given file is online for the given channel.
	 * If this method is called for a large number of files and the same channel,
	 * it should be considered to have the data prepared by calling {@link #prepareForNode(Node)} first,
	 * and then checking on the returned instance of {@link FileListForNode}.
	 * @param file file to check
	 * @param channel channel
	 * @return true if the file is online in the given channel, false if not
	 * @throws NodeException
	 */
	public static boolean isOnline(File file, Node channel) throws NodeException {
		if (file == null) {
			throw new NodeException("Cannot check online status for file null");
		}
		if (channel == null) {
			throw new NodeException("Cannot check online status for channel null");
		}

		// we will check for the master
		file = file.getMaster();

		Node fileNode = file.getFolder().getNode();

		// check whether the file belongs to the channel
		if (channel.equals(fileNode) || channel.isChannelOf(fileNode)) {
			final boolean[] result = new boolean[1];
			final Integer fileId = file.getId();
			final Integer channelId = channel.getId();

			TransactionManager.execute(new TransactionManager.Executable() {

				/* (non-Javadoc)
				 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
				 */
				public void execute() throws NodeException {
					result[0] = checkOnlineStatus(fileId, channelId);
				}
			}, true);
			return result[0];
		} else {
			return false;
		}
	}

	/**
	 * Prepare the file list for the given channel
	 * @param channel channel
	 * @return file list object
	 * @throws NodeException
	 */
	public static FileListForNode prepareForNode(Node channel) throws NodeException {
		return new FileListForNode(channel);
	}

	/**
	 * Set the file to be online/offline for its node
	 * @param file file
	 * @param online true to set the file online, false for setting offline
	 * @throws NodeException
	 */
	public static void setOnline(File file, boolean online) throws NodeException {
		if (file == null) {
			throw new NodeException("Cannot set online status for file null");
		}
		setOnline(file, file.getFolder().getNode(), online);
	}

	/**
	 * Set the file to be online/offline for the given channel
	 * @param file file
	 * @param channel channel
	 * @param online true to set the file online, false for setting offline
	 * @throws NodeException
	 */
	public static void setOnline(File file, Node channel, final boolean online) throws NodeException {
		if (file == null) {
			throw new NodeException("Cannot set online status for file null");
		}
		if (channel == null) {
			throw new NodeException("Cannot set online status for channel null");
		}

		// we will set for the master
		file = file.getMaster();

		Node fileNode = file.getFolder().getNode();

		// check whether the file belongs to the channel
		if (channel.equals(fileNode) || channel.isChannelOf(fileNode)) {
			Transaction old = TransactionManager.getCurrentTransaction();
			Transaction tmp = null;

			try {
				tmp = TransactionManager.getTransaction(old, true);
				TransactionManager.setCurrentTransaction(tmp);
				setOnlineStatus(file.getId(), channel.getId(), online);
			} finally {
				if (tmp != null) {
					tmp.commit();
				}
				TransactionManager.setCurrentTransaction(old);
			}
		}
	}

	/**
	 * Check the online status for the given fileId and nodeId
	 * @param fileId file id
	 * @param nodeId node id
	 * @return true if online, false if offline
	 * @throws NodeException
	 */
	protected static boolean checkOnlineStatus(Object fileId, Object nodeId) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;
		ResultSet res = null;

		try {
			pst = t.prepareStatement("SELECT * FROM contentfile_online WHERE contentfile_id = ? AND node_id = ?");
			pst.setObject(1, fileId);
			pst.setObject(2, nodeId);

			res = pst.executeQuery();

			return res.next();
		} catch (SQLException e) {
			throw new NodeException("Error while checking online status for fileId " + fileId + ", nodeId " + nodeId, e);
		} finally {
			t.closeResultSet(res);
			t.closeStatement(pst);
		}
	}

	/**
	 * Set the online status for the given fileId and nodeId
	 * @param fileId file id
	 * @param nodeId node id
	 * @param online true if online, false if offline
	 * @throws NodeException
	 */
	public static void setOnlineStatus(Integer fileId, Integer nodeId, boolean online) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		PreparedStatement pst = null;

		try {
			if (online) {
				if (!checkOnlineStatus(fileId, nodeId)) {
					pst = t.prepareInsertStatement("INSERT INTO contentfile_online (contentfile_id, node_id) VALUES (?, ?)");
					pst.setObject(1, fileId);
					pst.setObject(2, nodeId);
					pst.executeUpdate();
					t.addTransactional(new TransactionalTriggerEvent(File.class, fileId, INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
				}
			} else {
				pst = t.prepareDeleteStatement("DELETE FROM contentfile_online WHERE contentfile_id = ? AND node_id = ?");
				pst.setObject(1, fileId);
				pst.setObject(2, nodeId);
				pst.executeUpdate();
				t.addTransactional(new TransactionalTriggerEvent(File.class, fileId, INDEXED_STATUS_ATTRIBUTES, Events.NOTIFY));
			}
		} catch (SQLException e) {
			if (e.getErrorCode() == 1452) {
				logger.warn(
						"File online status for the file with ID " + fileId + " could not be set because the file no longer "
						+ "exists in the database. The statement was ignored, no further action is necessary.",
						e);
			} else {
				throw new NodeException("Error while setting online status for fileId " + fileId + ", nodeId " + nodeId, e);
			}
		} finally {
			t.closeStatement(pst);
		}
	}

	/**
	 * Inner helper class for quicker decisions on the online status of files When an instance is created for a node, all online file ids are stored in an internal list.
	 * The decision (by invoking the method {@link FileListForNode#isOnline(File)} will be done by doing binary searches, which should be quicker than doing SQL
	 * statements.
	 */
	public static class FileListForNode {

		/**
		 * Sorted list of ids of the online files
		 */
		protected List<Integer> onlineFileIds;

		/**
		 * Channel
		 */
		protected Node channel;

		/**
		 * Create an instance containing the data of the node
		 * @param node node
		 * @throws NodeException
		 */
		protected FileListForNode(Node node) throws NodeException {
			onlineFileIds = new Vector<Integer>();
			this.channel = node;
			TransactionManager.execute(new TransactionManager.Executable() {

				/* (non-Javadoc)
				 * @see com.gentics.lib.base.factory.TransactionManager.Executable#execute()
				 */
				public void execute() throws NodeException {
					DBUtils.executeStatement("SELECT contentfile_id FROM contentfile_online WHERE node_id = ? ORDER BY contentfile_id ASC", new SQLExecutor() {
						@Override
						public void prepareStatement(PreparedStatement stmt) throws SQLException {
							stmt.setObject(1, channel.getId());
						}

						@Override
						public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
							while (rs.next()) {
								onlineFileIds.add(rs.getInt("contentfile_id"));
							}
						}
					});
				}
			}, true);
		}

		/**
		 * Check whether the file is online
		 * @param file file to check
		 * @return true if the file is online, false if not
		 * @throws NodeException
		 */
		public boolean isOnline(File file) throws NodeException {
			if (file == null) {
				throw new NodeException("Cannot check online status for file null");
			}

			// we will check for the master
			file = file.getMaster();

			Node fileNode = file.getFolder().getNode();

			// check whether the file belongs to the channel
			if (channel.equals(fileNode) || channel.isChannelOf(fileNode)) {
				return checkOnlineStatus(file.getId());
			} else {
				return false;
			}
		}

		/**
		 * Check the online status of the file by doing a binary search in the prepared list
		 * @param fileId file id
		 * @return true if the file is online, false if not
		 */
		protected boolean checkOnlineStatus(Object fileId) {
			int iFileId = ObjectTransformer.getInt(fileId, 0);

			if (iFileId == 0) {
				return false;
			} else {
				return Collections.binarySearch(onlineFileIds, iFileId) >= 0;
			}
		}
	}
}
