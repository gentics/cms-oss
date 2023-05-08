/*
 * @author norbert
 * @date Dec 16, 2009
 * @version $Id: AbstractSandboxEditTest.java,v 1.4.2.3 2011-02-16 16:48:21 tobiassteiner Exp $
 */
package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.Rule;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.Permissions;
import com.gentics.contentnode.publish.Publisher;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.testutils.database.SQLUtilException;

/**
 * Abstract Superclass that sets up the test environment for the edit tests.
 * 
 * @author norbert
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public abstract class AbstractMultichannellingSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext(true);

	/**
	 * Array containing false and true
	 */
	public final static boolean[] FALSE_TRUE = { false, true };

	/**
	 * Array containing null, false and true
	 */
	public final static Boolean[] NULL_FALSE_TRUE = { null, Boolean.FALSE, Boolean.TRUE };

	/**
	 * Id of the master node
	 */
	public final static int MASTER_ID = 10;

	/**
	 * Id of the channel
	 */
	protected int channelId = -1;

	/**
	 * Id of the subchannel
	 */
	protected int subChannelId = -1;

	/**
	 * Id of the side channel
	 */
	protected int sideChannelId;

	public final static String CRDB_PROPERTIES_FILENAME = "node4_objectedit_cr.properties";

	/**
	 * Reference descriptors to check
	 */
	protected static List<ReferenceDescriptor> referenceDescriptors = Arrays.asList(new ReferenceDescriptor("folder", "mother", "folder"),
			new ReferenceDescriptor("page", "folder_id", "folder"), new ReferenceDescriptor("contentfile", "folder_id", "folder"));

	/**
	 * Test user
	 */
	protected SystemUser user;

	/**
	 * Run a publish process
	 * 
	 * @param dirtAll
	 *            true when everything shall be dirted before, false if not
	 * @throws Exception
	 */
	public void publish(boolean dirtAll) throws Exception {
		// (if requested)
		if (dirtAll) {
			// dirt all pages
			testContext.getDBSQLUtils().executeQueryManipulation(
					"UPDATE page, folder, node SET status = 1, delay_publish = 0 WHERE page.status = 2 AND page.folder_id = folder.id AND folder.node_id = node.id AND node.disable_publish = 0");
			// dirt images and files
			testContext.getDBSQLUtils().executeQueryManipulation(
					"INSERT INTO logcmd (user_id, cmd_desc_id, o_type, o_id, timestamp) SELECT 1, 339, 10008, contentfile.id, unix_timestamp() from contentfile");
			// dirt folders
			testContext.getDBSQLUtils().executeQueryManipulation(
					"INSERT INTO logcmd (user_id, cmd_desc_id, o_type, o_id, timestamp) SELECT 1, 339, 10002, id, unix_timestamp() from folder");
		}

		// publish everything
		Publisher pub = new Publisher(false);

		pub.setTimestamp(new Date().getTime());
		pub.run();

		assertEquals("Check status of publish process: ", "OK", pub.getRenderResult().getReturnCode());
	}

	@Before
	public void setUp() throws Exception {

		testContext.startTransactionWithPermissions(false);

		Transaction t = TransactionManager.getCurrentTransaction();
		user = t.getObject(SystemUser.class, DBTestContext.USER_WITH_PERMS);
		UserGroup group = user.getUserGroups().get(0);

		int rootFolderId = ObjectTransformer.getInt(t.getObject(Node.class, MASTER_ID).getFolder().getId(), 0);
		Permissions oldPerms = PermissionStore.getInstance().getMergedPermissions(Arrays.asList(ObjectTransformer.getInt(group.getId(), 0)), Node.TYPE_NODE, rootFolderId, -1, -1).getGroupPermissions();
		Permissions newPerms = Permissions.merge(Arrays.asList(oldPerms, Permissions.get(PermHandler.PERM_CHANNEL_SYNC)));
		PermHandler.setPermissions(Node.TYPE_NODE, rootFolderId, Arrays.asList(group), newPerms.toString());
		t.commit(false);

		// create the channel
		channelId = createChannel(MASTER_ID, "Test Channel", "This is the test channel", "channel", "/Content.Node");
		// create the subchannel
		subChannelId = createChannel(channelId, "Test Subchannel", "This is the test subchannel", "subchannel", "/Content.Node");
		// create a side channel so we have a tree of channels
		sideChannelId = createChannel(MASTER_ID, "Test Sidechannel", "This is the test side channel", "sidechannel", "/Content.Node");
	}

	/**
	 * Check if the correct amount of pages were dirted
	 * 
	 * @throws SQLUtilException
	 */
	protected void checkDirtedPages(int[] ids) throws SQLUtilException, SQLException {
		ResultSet rs = testContext.getDBSQLUtils().executeQuery("SELECT id,name FROM page WHERE status = 1");
		String dirted = "Pages dirted: ";
		int nPagesDirted = 0;

		while (rs.next()) {
			nPagesDirted++;
			dirted += "[" + rs.getInt("id") + "]:" + rs.getString("name") + ",";
		}

		String set = "";

		for (int i = 0; i < ids.length; i++) {
			set += ids[i] + ",";
		}
		set = set.substring(0, set.length() - 1);

		assertEquals("Check that the correct pages were dirted. I expected pages: " + set + ". " + dirted, ids.length,
				testContext.getDBSQLUtils().getNumRows("SELECT id FROM page WHERE id IN (" + set + ") AND status = 1"));
		assertEquals("Check the correct amount of pages were dirted. " + dirted, ids.length, nPagesDirted);
	}

	/**
	 * Create a channel of the given master TODO: migrate this to the NodeObject functions, when creating nodes and channels is implemented
	 * 
	 * @param masterId
	 *            master node id
	 * @param name
	 *            name of the channel
	 * @param description
	 *            of the channel
	 * @param host
	 *            hostname of the channel
	 * @param pubDir
	 *            pubdir of the channel
	 * @return id of the channel
	 * @throws NodeException
	 */
	protected int createChannel(int masterId, String name, String description, String host, String pubDir) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// TODO use java api to create channels
		Node masterNode = t.getObject(Node.class, masterId);
		Folder rootFolder = masterNode.getFolder();
		Object channelSetId = rootFolder.getChannelSetId();

		// create the node
		List<Integer> keys = DBUtils.executeInsert("INSERT INTO node (host, pub_dir, pub_dir_bin, utf8, publish_fs) VALUES (?, ?, ?, ?, ?)",
				new Object[] { host, pubDir, pubDir, true, true });

		assertEquals("Check number of generated rows", 1, keys.size());
		int channelId = keys.get(0);

		// create the root folder
		List<Integer> folderKeys = DBUtils.executeInsert(
				"INSERT INTO folder (name, description, creator, cdate, editor, edate, node_id, type_id, mother, pub_dir, master_id, channel_id, channelset_id, is_master) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				new Object[] {
			name, description, t.getUserId(), t.getUnixTimestamp(), t.getUserId(), t.getUnixTimestamp(), channelId, Node.TYPE_CHANNEL, 0, pubDir,
			rootFolder.getId(), channelId, channelSetId, 0 });

		assertEquals("Check number of generated rows", 1, folderKeys.size());
		int folderId = folderKeys.get(0);

		DBUtils.executeUpdate("UPDATE node SET folder_id = ? WHERE id = ?", new Object[] { folderId, channelId });

		// get all groups and parent groups of the user
		SystemUser user = t.getObject(SystemUser.class, t.getUserId());
		List<UserGroup> allParents = new Vector<UserGroup>();
		List<UserGroup> userGroups = user.getUserGroups();

		allParents.addAll(userGroups);

		for (UserGroup userGroup : userGroups) {
			List<UserGroup> parents = userGroup.getParents();

			for (UserGroup parent : parents) {
				if (!allParents.contains(parent)) {
					allParents.add(parent);
				}
			}
		}
		t.commit(false);
		return channelId;
	}

	/**
	 * Assert the response
	 * 
	 * @param response
	 *            response
	 * @throws Exception
	 *             if the response is not OK
	 */
	protected void assertResponseCode(GenericResponse response) throws Exception {
		assertTrue("Response returned with code " + response.getResponseInfo().getResponseCode() + " (" + response.getResponseInfo().getResponseMessage() + ")",
				ResponseCode.OK == response.getResponseInfo().getResponseCode());
	}

	/**
	 * Assert reference integrity in the DB. No object, that is not a master, may be referenced in the DB
	 * 
	 * @throws Exception
	 */
	protected void assertReferenceIntegrity() throws Exception {
		for (ReferenceDescriptor referenceDescriptor : referenceDescriptors) {
			ResultSet checkResult = testContext.getDBSQLUtils().executeQuery(referenceDescriptor.getCheckSQL());

			if (checkResult.next()) {
				fail(
						"Record " + checkResult.getObject("sourceid") + " of " + referenceDescriptor.getSourceTable() + " references non-master record "
						+ checkResult.getObject("targetid") + " of table " + referenceDescriptor.getTargetTable() + " via field "
						+ referenceDescriptor.getSourceField());
			}
		}
	}

	/**
	 * Restrict all assignments of this user to groups to the given channel
	 * @param userId user id
	 * @param channelId channel id
	 * @throws NodeException
	 */
	protected void restrictUserToChannel(final int userId, final int channelId) throws NodeException {
		final List<Integer> userGroupIds = new ArrayList<Integer>();
		DBUtils.executeStatement("SELECT id FROM user_group WHERE user_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, userId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					userGroupIds.add(rs.getInt("id"));
				}
			}
		});

		for (Integer userGroupId : userGroupIds) {
			DBUtils.executeInsert("INSERT INTO user_group_node (user_group_id, node_id) VALUES (?, ?)", new Object[] {userGroupId, channelId});
		}
	}

	/**
	 * Reference descriptor
	 */
	public static class ReferenceDescriptor {

		/**
		 * Name of the source table
		 */
		protected String sourceTable;

		/**
		 * Name of the source field
		 */
		protected String sourceField;

		/**
		 * Name of the target table
		 */
		protected String targetTable;

		/**
		 * SQL Statement that fetched reference failures
		 */
		protected String checkSQL;

		/**
		 * Create an instance
		 * 
		 * @param sourceTable
		 *            source table
		 * @param sourceField
		 *            source field
		 * @param targetTable
		 *            target table
		 */
		public ReferenceDescriptor(String sourceTable, String sourceField, String targetTable) {
			this.sourceTable = sourceTable;
			this.sourceField = sourceField;
			this.targetTable = targetTable;
			this.checkSQL = "SELECT source.id sourceid, target.id targetid FROM " + sourceTable + " source LEFT JOIN " + targetTable + " target ON source."
					+ sourceField + " = target.id WHERE " + "target.is_master != 1";
		}

		/**
		 * Get the check sql
		 * 
		 * @return check sql
		 */
		public String getCheckSQL() {
			return checkSQL;
		}

		/**
		 * Get source table name
		 * 
		 * @return source table name
		 */
		public String getSourceTable() {
			return sourceTable;
		}

		/**
		 * Get source field name
		 * 
		 * @return source field name
		 */
		public String getSourceField() {
			return sourceField;
		}

		/**
		 * Get target table name
		 * 
		 * @return target table name
		 */
		public String getTargetTable() {
			return targetTable;
		}
	}
}
