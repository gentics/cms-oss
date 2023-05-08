package com.gentics.contentnode.tests.perm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;

/**
 * Test cases for the PermissionStore
 * 	37 Task
	38 Task Template
	39 Job
OK	40 ObjectProperty Element
	109 Role

	137 Export
	138 Import

OK	10001 Node
OK	10002 Folder
OK	10023 Language
OK	10027 Datasource
OK	10033 Channel
OK	10208 ContentRepository

 */
public class PermissionStoreTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Id of the Node Group
	 */
	public final static int GROUP_ID = 2;

	/**
	 * Id of the construct to be used for the object property
	 */
	public final static int CONSTRUCT_ID = 1;

	/**
	 * Perm String with all bits set
	 */
	public final static String FULL_PERM = StringUtils.repeat("1", 32);

	@Before
	public void setUp() throws Exception {
		testContext.getContext().setFeature(Feature.MULTICHANNELLING, true);
	}

	/**
	 * Test refreshing the permissions after a new group has been created
	 * @throws Exception
	 */
	@Test
	public void testCreateGroup() throws Exception {
		// check whether the group permissions for the initial group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();

		// create a new group
		List<Integer> groupIds = DBUtils.executeInsert("INSERT INTO usergroup (name, mother) VALUES (?, ?)", new Object[] { "Testgroup", GROUP_ID });
		assertEquals("Check that a group was created", 1, groupIds.size());
		int newGroupId = groupIds.get(0);

		// duplicate the permissions
		DBUtils.executeUpdate("INSERT INTO perm SELECT ?, o_type, o_id, perm FROM perm WHERE usergroup_id = ?", new Object[] { newGroupId, GROUP_ID });

		// let the permission store refresh the permissions
		PermissionStore.getInstance().refreshGroup(newGroupId);

		// check whether the group permissions for the new group can be fetched from the PermissionStore
		assertGroupPermissions(newGroupId);
		assertPermissionConsistency();
	}

	/**
	 * Test refreshing the permissions after a group has been removed
	 * @throws Exception
	 */
	@Test
	public void testRemoveGroup() throws Exception {
		// check whether the group permissions for the initial group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();

		// create a new group
		List<Integer> groupIds = DBUtils.executeInsert("INSERT INTO usergroup (name, mother) VALUES (?, ?)", new Object[] { "Testgroup", GROUP_ID });
		assertEquals("Check that a group was created", 1, groupIds.size());
		int newGroupId = groupIds.get(0);

		// duplicate the permissions
		DBUtils.executeUpdate("INSERT INTO perm SELECT ?, o_type, o_id, perm FROM perm WHERE usergroup_id = ?", new Object[] { newGroupId, GROUP_ID });

		// let the permission store refresh the permissions
		PermissionStore.getInstance().refreshGroup(newGroupId);

		// check whether the group permissions for the new group can be fetched from the PermissionStore
		assertGroupPermissions(newGroupId);
		assertPermissionConsistency();

		// delete the group and its permissions
		DBUtils.executeUpdate("DELETE FROM usergroup WHERE id = ?", new Object[] {newGroupId});
		DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] {newGroupId});

		// let the permission store refresh permissions
		PermissionStore.getInstance().removeGroup(newGroupId);

		// check the number of permissions
		assertPermissionConsistency();
	}

	/**
	 * Test refreshing the permissions after a group permission has been changed (for an object)
	 * @throws Exception
	 */
	@Test
	public void testChangeGroupPermission() throws Exception {
		// check whether the group permissions for the initial group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();

		final Map<Integer, String> folderPerm = new HashMap<Integer, String>();
		// get the permissions for a single folder
		DBUtils.executeStatement("SELECT o_id, perm FROM perm WHERE usergroup_id = ? AND o_type = ? LIMIT 1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, GROUP_ID);
				stmt.setInt(2, Folder.TYPE_FOLDER);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					folderPerm.put(rs.getInt("o_id"), rs.getString("perm"));
				}
			}
		});
		assertFalse("Did not find a single folder permission", folderPerm.isEmpty());

		// modify all found folder perms (by inverting every bit)
		for (Map.Entry<Integer, String> entry : folderPerm.entrySet()) {
			String newPerm = entry.getValue().replaceAll("0", "X").replaceAll("1", "0").replaceAll("X", "1");
			DBUtils.executeUpdate("UPDATE perm SET perm = ? WHERE usergroup_id = ? AND o_type = ? AND o_id = ?", new Object[] {newPerm, GROUP_ID, Folder.TYPE_FOLDER, entry.getKey()});

			// let the permission store refresh the permissions
			PermissionStore.getInstance().refreshObject(Folder.TYPE_FOLDER, entry.getKey());
		}

		// check whether the group permissions for the group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();
	}

	/**
	 * Test refreshing the permissions after a group permission has been added (for an object)
	 * @throws Exception
	 */
	@Test
	public void testAddGroupPermission() throws Exception {
		// check whether the group permissions for the initial group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();

		final List<Integer> folderId = new ArrayList<Integer>();
		// get the first folder id, which does not have a permission set for the group
		DBUtils.executeStatement("SELECT MAX(o_id) + 1 folder_id FROM perm WHERE usergroup_id = ? AND o_type = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, GROUP_ID);
				stmt.setInt(2, Folder.TYPE_FOLDER);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					folderId.add(rs.getInt("folder_id"));
				}
			}
		});
		assertFalse("Did not find a free folder_id", folderId.isEmpty());

		// add the group permission
		DBUtils.executeUpdate("INSERT INTO perm (usergroup_id, o_type, o_id, perm) VALUES (?, ?, ?, ?)",
				new Object[] { GROUP_ID, Folder.TYPE_FOLDER, folderId.get(0), StringUtils.repeat("10", 16) });

		// let the permission store refresh the permissions
		PermissionStore.getInstance().refreshObject(Folder.TYPE_FOLDER, folderId.get(0));

		// check whether the group permissions for the group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();
	}

	/**
	 * Test refreshing the permissions after a group permission has been removed (for an object)
	 * @throws Exception
	 */
	@Test
	public void testRemoveGroupPermission() throws Exception {
		// check whether the group permissions for the initial group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();

		final Map<Integer, String> folderPerm = new HashMap<Integer, String>();
		// get the permissions for a single folder
		DBUtils.executeStatement("SELECT o_id, perm FROM perm WHERE usergroup_id = ? AND o_type = ? AND perm != ? LIMIT 1", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, GROUP_ID);
				stmt.setInt(2, Folder.TYPE_FOLDER);
				stmt.setString(3, StringUtils.repeat("0", 32));
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					folderPerm.put(rs.getInt("o_id"), rs.getString("perm"));
				}
			}
		});
		assertFalse("Did not find a single folder permission", folderPerm.isEmpty());

		// remove all found folder perms
		for (Map.Entry<Integer, String> entry : folderPerm.entrySet()) {
			DBUtils.executeUpdate("DELETE FROM perm WHERE usergroup_id = ? AND o_type = ? AND o_id = ?", new Object[] {GROUP_ID, Folder.TYPE_FOLDER, entry.getKey()});

			// let the permission store refresh the permissions
			PermissionStore.getInstance().refreshObject(Folder.TYPE_FOLDER, entry.getKey());

			// check that the permission is really gone
			assertEquals("Check removed permission", StringUtils.repeat("0", 32),
					PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), Folder.TYPE_FOLDER, entry.getKey(), -1, -1)
							.getGroupPermissions().toString());
		}

		// check whether the group permissions for the group can be fetched from the PermissionStore
		assertGroupPermissions(GROUP_ID);
		assertPermissionConsistency();
	}

	/**
	 * Test creation of a node
	 * @throws Exception
	 */
	@Test
	public void testCreateNode() throws Exception {
		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a new node
		Node node = createNode("www.node.test", "Test node");

		// check whether permissions have been set
		assertTrue("Node must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(node));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting a node
	 * @throws Exception
	 */
	@Test
	public void testDeleteNode() throws Exception {
		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a new node
		Node node = createNode("www.node.test", "Test node");
		int rootFolderId = ObjectTransformer.getInt(node.getFolder().getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		Transaction t = TransactionManager.getCurrentTransaction();
		// delete the node
		node.delete();
		t.commit(false);

		// check whether permissions have been removed
		assertEquals("No permissions should be set on the deleted node", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), Node.TYPE_NODE, rootFolderId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test creation of a channel
	 * @throws Exception
	 */
	@Test
	public void testCreateChannel() throws Exception {
		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a new node
		Node node = createNode("www.node.test", "Test node");

		// create a new channel
		Node channel = createChannel("www.channel.test", "Test channel", node);

		// check whether permissions have been set
		assertTrue("Channel must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(channel));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting a channel
	 * @throws Exception
	 */
	@Test
	public void testDeleteChannel() throws Exception {
		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a new node
		Node node = createNode("www.node.test", "Test node");

		// create a new channel
		Node channel = createChannel("www.channel.test", "Test channel", node);
		int rootFolderId = ObjectTransformer.getInt(channel.getFolder().getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		Transaction t = TransactionManager.getCurrentTransaction();
		// delete the channel
		channel.delete();
		t.commit(false);

		// check whether permissions have been removed
		assertEquals("No permissions should be set on the deleted channel", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), Node.TYPE_CHANNEL, rootFolderId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test creation of a folder
	 * @throws Exception
	 */
	@Test
	public void testCreateFolder() throws Exception {
		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a new node
		Node node = createNode("www.node.test", "Test node");
		// create a folder
		Folder folder = createFolder(node.getFolder(), "Test Folder", "/");

		// check whether permissions have been set
		assertTrue("Folder must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(folder));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting a folder
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolder() throws Exception {
		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a new node
		Node node = createNode("www.node.test", "Test node");
		Folder folder = createFolder(node.getFolder(), "Test folder", "/");
		int folderId = ObjectTransformer.getInt(folder.getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		Transaction t = TransactionManager.getCurrentTransaction();
		// delete the folder
		folder.delete();
		t.commit(false);

		// check whether permissions have been removed
		assertEquals("No permissions should be set on the deleted folder", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), Folder.TYPE_FOLDER, folderId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test creating a contentrepository
	 * @throws Exception
	 */
	@Test
	public void testCreateContentrepository() throws Exception {
		// set permissions to view contentrepositories
		Transaction t = TransactionManager.getCurrentTransaction();
		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(ContentRepository.TYPE_CONTENTREPOSITORIES, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a CR
		ContentRepository cr = createCR("Test CR");

		// check whether permissions have been set
		assertTrue("CR must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(cr));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting a contentrepository
	 * @throws Exception
	 */
	@Test
	public void deleteContentrepository() throws Exception {
		// set permissions to view contentrepositories
		Transaction t = TransactionManager.getCurrentTransaction();
		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(ContentRepository.TYPE_CONTENTREPOSITORIES, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a CR
		ContentRepository cr = createCR("Test CR");
		int crId = ObjectTransformer.getInt(cr.getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		t = TransactionManager.getCurrentTransaction();
		// delete the cr
		cr.delete();
		t.commit(false);

		// check whether the permissions have been removed
		assertEquals("No permissions should be set on the deleted cr", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), ContentRepository.TYPE_CONTENTREPOSITORY, crId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test creating a content language
	 * @throws Exception
	 */
	@Test
	public void testCreateContentLanguage() throws Exception {
		// set permissions to view content languages
		Transaction t = TransactionManager.getCurrentTransaction();
		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(ContentLanguage.TYPE_CONTENTLANGUAGE, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a content language
		ContentLanguage language = createLanguage("Klingon", "kl");

		// check whether permissions have been set
		assertTrue("ContentLanguage must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(language));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting a content language
	 * @throws Exception
	 */
	@Test
	public void testDeleteContentLanguage() throws Exception {
		// set permissions to view content languages
		Transaction t = TransactionManager.getCurrentTransaction();
		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(ContentLanguage.TYPE_CONTENTLANGUAGE, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a content language
		ContentLanguage language = createLanguage("Klingon", "kl");
		int langId = ObjectTransformer.getInt(language.getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		t = TransactionManager.getCurrentTransaction();
		// delete the content language
		language.delete();
		t.commit(false);

		// check whether the permissions have been removed
		assertEquals("No permissions should be set on the deleted content language", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), ContentLanguage.TYPE_CONTENTLANGUAGE, langId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test creating a datasource, when the feature to have permissions per datasource is off
	 * @throws Exception
	 */
	@Test
	public void testCreateDatasourceWithoutFeature() throws Exception {
		doDatasourceCreateTest(false);
	}

	/**
	 * Test creating a datasource, when the feature to have permissions per datasource is on
	 * @throws Exception
	 */
	@Test
	public void testCreateDatasourceWithFeature() throws Exception {
		doDatasourceCreateTest(true);
	}

	/**
	 * Internal function to do the datasource create test
	 * @param feature true if the feature to have permissions per datasource instance is on
	 * @throws Exception
	 */
	protected void doDatasourceCreateTest(boolean feature) throws Exception {
		// set permissions to view datasources
		Transaction t = TransactionManager.getCurrentTransaction();
		t.getNodeConfig().getDefaultPreferences().setFeature(Feature.DATASOURCE_PERM.toString().toLowerCase(), feature);

		// clean the datasource permissions
		DBUtils.executeUpdate("DELETE FROM perm WHERE o_type = ? AND o_id NOT IN (SELECT id FROM datasource)", new Object[] {Datasource.TYPE_SINGLE_DATASOURCE});
		PermissionStore.initialize(true);

		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(Datasource.TYPE_DATASOURCE, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a datasource
		Datasource datasource = createDatasource("Test DS");

		// check whether permissions have been set
		assertTrue("Datasource must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(datasource));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting a datasource, when the feature to have permissions per datasource is off
	 * @throws Exception
	 */
	@Test
	public void testDeleteDatasourceWithoutFeature() throws Exception {
		doDatasourceDeleteTest(false);
	}

	/**
	 * Test deleting a datasource, when the feature to have permissions per datasource is on
	 * @throws Exception
	 */
	@Test
	public void testDeleteDatasourceWithFeature() throws Exception {
		doDatasourceDeleteTest(true);
	}

	/**
	 * Internal method to do the datasource delete test
	 * @param feature true if the feature to have permissions per datasource instance is on
	 * @throws Exception
	 */
	protected void doDatasourceDeleteTest(boolean feature) throws Exception {
		// set permissions to view datasources
		Transaction t = TransactionManager.getCurrentTransaction();
		t.getNodeConfig().getDefaultPreferences().setFeature(Feature.DATASOURCE_PERM.toString().toLowerCase(), feature);

		// clean the datasource permissions
		DBUtils.executeUpdate("DELETE FROM perm WHERE o_type = ? AND o_id NOT IN (SELECT id FROM datasource)", new Object[] {Datasource.TYPE_SINGLE_DATASOURCE});
		PermissionStore.initialize(true);

		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(Datasource.TYPE_DATASOURCE, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create a datasource
		Datasource datasource = createDatasource("Test DS");
		int dsId = ObjectTransformer.getInt(datasource.getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		t = TransactionManager.getCurrentTransaction();
		// delete the datasource
		datasource.delete();
		t.commit(false);

		// check whether the permissions have been removed
		assertEquals("No permissions should be set on the deleted datasource", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), Datasource.TYPE_SINGLE_DATASOURCE, dsId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test creating an object property definition
	 * @throws Exception
	 */
	@Test
	public void testCreateObjectProperty() throws Exception {
		// set permissions to view object property definitions
		Transaction t = TransactionManager.getCurrentTransaction();
		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(ObjectTagDefinition.TYPE_OBJTAG_DEFS, GROUP_ID, FULL_PERM);
		setPermission(ObjectTagDefinition.TYPE_OBJTAG_DEF_FOR_TYPE, Folder.TYPE_FOLDER, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create an object property definition
		ObjectTagDefinition objProp = createObjectProperty(Folder.TYPE_FOLDER, "object.test", CONSTRUCT_ID);

		// check whether permissions have been set
		assertTrue("Object Property must be visible to the user", TransactionManager.getCurrentTransaction().getPermHandler().canView(objProp));
		assertPermissionConsistency();
	}

	/**
	 * Test deleting an object property definition
	 * @throws Exception
	 */
	@Test
	public void testDeleteObjectProperty() throws Exception {
		// set permissions to view object property definitions
		Transaction t = TransactionManager.getCurrentTransaction();
		setPermission(PermHandler.TYPE_ADMIN, GROUP_ID, FULL_PERM);
		setPermission(PermHandler.TYPE_CONADMIN, GROUP_ID, FULL_PERM);
		setPermission(ObjectTagDefinition.TYPE_OBJTAG_DEFS, GROUP_ID, FULL_PERM);
		setPermission(ObjectTagDefinition.TYPE_OBJTAG_DEF_FOR_TYPE, Folder.TYPE_FOLDER, GROUP_ID, FULL_PERM);
		t.commit(false);
		assertPermissionConsistency();

		int userId = getNodeUserId();
		testContext.getContext().startTransaction(userId);

		// create an object proeprty definition
		ObjectTagDefinition objProp = createObjectProperty(Folder.TYPE_FOLDER, "object.test", CONSTRUCT_ID);
		int objPropId = ObjectTransformer.getInt(objProp.getId(), -1);

		// start a new transaction
		testContext.getContext().startTransaction(userId);
		t = TransactionManager.getCurrentTransaction();
		// delete the object property
		objProp.delete();
		t.commit(false);

		// check whether the permissions have been removed
		assertEquals("No permissions should be set on the deleted object property", StringUtils.repeat("0", 32),
				PermissionStore.getInstance().getMergedPermissions(Arrays.asList(GROUP_ID), ObjectTagDefinition.TYPE_OBJTAG_DEF, objPropId, -1, -1).getGroupPermissions().toString());
		assertPermissionConsistency();
	}

	/**
	 * Test the view, edit and delete permissions on a template that is linked
	 * to folders in wastebin only
	 * 
	 * @throws NodeException
	 */
	@Test
	public void testPermissionOnTemplateLinkedToFolderInWastebin() throws Exception {
		try (FeatureClosure featureClosure = new FeatureClosure(Feature.WASTEBIN, true)) {
			Template template = null;

			try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
				Transaction t = trx.getTransaction();
		
				Node node         = ContentNodeTestDataUtils.createNode();
				Folder folder     = ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder");
				template          = ContentNodeTestDataUtils.createTemplate(folder, "Template");

				folder.delete();
				trx.success();
			}

			try (Trx trx = new Trx(null, DBTestContext.USER_WITH_PERMS)) {
				Transaction t = trx.getTransaction();
				template      = t.getObject(template);

				assertTrue("Template must be viewable",  t.getPermHandler().canView(template));
				assertTrue("Template must be editable",  t.getPermHandler().canEdit(template));
				assertTrue("Template must be deletable", t.getPermHandler().canDelete(template));
			}
		}
	}

	/**
	 * Get the id of a node user (user in the Node group)
	 * @return id of the user
	 * @throws Exception
	 */
	protected int getNodeUserId() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		UserGroup nodeGroup = t.getObject(UserGroup.class, GROUP_ID);
		assertNotNull("Node group must exist", nodeGroup);
		List<SystemUser> nodeGroupMembers = nodeGroup.getMembers();
		assertFalse("List of members of node group must not be null", nodeGroupMembers.isEmpty());

		// start a new transaction with a member of the node group
		return ObjectTransformer.getInt(nodeGroupMembers.get(0).getId(), -1);
	}

	/**
	 * Assert group permissions for the given group
	 * @param groupId group id
	 * @throws Exception
	 */
	protected void assertGroupPermissions(final int groupId) throws Exception {
		DBUtils.executeStatement("SELECT o_type, o_id, perm FROM perm WHERE usergroup_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, groupId);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				boolean permFound = false;
				while (rs.next()) {
					permFound = true;
					int objType = rs.getInt("o_type");
					int objId = rs.getInt("o_id");
					String perm = rs.getString("perm");
					PermissionPair p = PermissionStore.getInstance().getMergedPermissions(Arrays.asList(groupId), objType, objId, -1, -1);
					assertEquals("Check permission bits for " + objType + "." + objId, perm, p.getGroupPermissions().toString());
				}
				assertTrue("Permissions must exist for group " + groupId, permFound);
			}
		});
	}

	/**
	 * Assert the consistency of the PermissionStore
	 * @throws Exception
	 */
	protected void assertPermissionConsistency() throws Exception {
		StringBuilder output = new StringBuilder();
		boolean consistent = PermissionStore.getInstance().checkConsistency(true, output, false);
		assertTrue(output.toString(), consistent);
	}

	/**
	 * Create a new Node
	 * 
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @return node
	 * @throws Exception
	 */
	protected Node createNode(String hostName, String name) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		Folder rootFolder = t.createObject(Folder.class);

		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		node.save();
		t.commit(false);

		return t.getObject(Node.class, node.getId());
	}

	/**
	 * Create a new channel
	 * @param hostName hostname
	 * @param name name
	 * @param master master node
	 * @return channel
	 * @throws Exception
	 */
	protected Node createChannel(String hostName, String name, Node master) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node channel = t.createObject(Node.class);

		channel.setHostname(hostName);
		channel.setPublishDir("/");
		Folder rootFolder = t.createObject(Folder.class);

		channel.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		Folder masterRootFolder = master.getFolder();

		rootFolder.setChannelMaster(masterRootFolder);

		channel.save();
		t.commit(false);

		return t.getObject(Node.class, channel.getId());
	}

	/**
	 * Create a new folder
	 * @param mother mother folder
	 * @param name folder name
	 * @param pubDir folder publish directory
	 * @return folder
	 * @throws Exception
	 */
	protected Folder createFolder(Folder mother, String name, String pubDir) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.createObject(Folder.class);

		folder.setMotherId(mother.getId());
		folder.setName(name);
		folder.setPublishDir(pubDir);

		folder.save();
		t.commit(false);

		return t.getObject(Folder.class, folder.getId());
	}

	/**
	 * Create a new contentrepository
	 * @param name name
	 * @return contentrepository
	 * @throws Exception
	 */
	protected ContentRepository createCR(String name) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);

		cr.save();
		t.commit(false);

		return cr;
	}

	/**
	 * Create a content language
	 * @param name name
	 * @param code code
	 * @return content language
	 * @throws Exception
	 */
	protected ContentLanguage createLanguage(String name, String code) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentLanguage language = t.createObject(ContentLanguage.class);

		language.setName(name);
		language.setCode(code);

		language.save();
		t.commit(false);

		return language;
	}

	/**
	 * Create a datasource
	 * @param name name
	 * @return datasource
	 * @throws Exception
	 */
	protected Datasource createDatasource(String name) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Datasource datasource = t.createObject(Datasource.class);

		datasource.setName(name);
		datasource.setSourceType(SourceType.staticDS);

		datasource.save();
		t.commit(false);

		return datasource;
	}

	/**
	 * Create an object property definition
	 * @param type type
	 * @param name name
	 * @param constructId construct id
	 * @return object property definition
	 * @throws Exception
	 */
	protected ObjectTagDefinition createObjectProperty(int type, String name, int constructId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		ObjectTagDefinition objProp = t.createObject(ObjectTagDefinition.class);

		objProp.setName(name, 1);
		objProp.setTargetType(type);
		objProp.getObjectTag().setObjType(type);
		objProp.getObjectTag().setConstructId(constructId);

		objProp.save();
		t.commit(false);

		return objProp;
	}

	/**
	 * Set the given permission to the given object type in the tree
	 * @param objType object type
	 * @param groupId group id
	 * @param perm permission
	 * @throws Exception
	 */
	protected void setPermission(final int objType, final int groupId, final String perm) throws Exception {
		final List<Integer> treeId = new ArrayList<Integer>(1);
		DBUtils.executeStatement("SELECT id FROM tree WHERE type_id = ?", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, objType);
			}

			@Override
			public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
				while (rs.next()) {
					treeId.add(rs.getInt("id"));
				}
			}
		});

		assertEquals("Check number of tree items for type " + objType, 1, treeId.size());

		setPermission(objType, treeId.get(0), groupId, perm);
	}

	/**
	 * Set the given permission to the given object
	 * @param objType object type
	 * @param objId object id
	 * @param groupId group id
	 * @param perm permission
	 * @throws Exception
	 */
	protected void setPermission(final int objType, final int objId, final int groupId, final String perm) throws Exception {
		Map<String, Object> id = new HashMap<String, Object>();
		id.put("o_type", objType);
		id.put("o_id", objId);
		id.put("usergroup_id", groupId);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("perm", perm);
		DBUtils.updateOrInsert("perm", id, data);

		PermissionStore.getInstance().refreshObject(objType, objId);
	}
}
