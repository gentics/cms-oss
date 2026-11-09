package com.gentics.contentnode.tests.perm;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler.ObjectPermission;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.db.SQLExecutor;

/**
 * Abstract test cases for channel specific permissions
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = Feature.MULTICHANNELLING)
public abstract class AbstractChannelPermissionTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * User with all permissions
	 */
	protected static SystemUser nodeUser;

	/**
	 * User with restricted permissions
	 */
	protected static SystemUser restrictedUser;

	/**
	 * Tested class
	 */
	protected Class<? extends NodeObject> clazz;

	/**
	 * Tested permission
	 */
	protected ObjectPermission perm;

	/**
	 * Setting for the folder
	 */
	protected ChannelSetting folderSetting;

	/**
	 * Setting for the object
	 */
	protected ChannelSetting objectSetting;

	/**
	 * Master Node
	 */
	protected static Node master;

	/**
	 * Channel
	 */
	protected static Node channel;

	/**
	 * Subchannel
	 */
	protected static Node subChannel;

	/**
	 * Sidechannel
	 */
	protected static Node sideChannel;

	/**
	 * Create a test instance
	 * @param clazz tested class
	 * @param perm tested permission
	 * @param folderSetting folder setting
	 * @param objectSetting object setting
	 */
	public AbstractChannelPermissionTest(Class<? extends NodeObject> clazz, ObjectPermission perm, ChannelSetting folderSetting, ChannelSetting objectSetting) {
		this.clazz = clazz;
		this.perm = perm;
		this.folderSetting = folderSetting;
		this.objectSetting = objectSetting;
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		nodeUser = supply(t -> t.getObject(SystemUser.class, ContentNodeTestDataUtils.getNodeUserId()));
		master = supply(nodeUser, () -> ContentNodeTestDataUtils.createNode("Node", "hode", "/", null, false, false));
		channel = supply(nodeUser, () -> ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/"));
		subChannel = supply(nodeUser, () -> ContentNodeTestDataUtils.createChannel(channel, "SubChannel", "subchannel", "/"));
		sideChannel = supply(nodeUser, () -> ContentNodeTestDataUtils.createChannel(master, "SideChannel", "sidechannel", "/"));

		final UserGroup userGroup = supply(() -> ContentNodeTestDataUtils.createUserGroup("test group", ContentNodeTestDataUtils.NODE_GROUP_ID));
		restrictedUser = supply(() -> ContentNodeTestDataUtils.createSystemUser("Tester", "Tester", "", "Tester", "", new ArrayList<UserGroup>()));

		final int[] userGroupId = new int[1];
		// assign to group
		operate(() -> DBUtils.executeStatement("INSERT INTO user_group (user_id, usergroup_id) VALUES (?, ?)", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, restrictedUser.getId());
				stmt.setInt(2, ObjectTransformer.getInt(userGroup.getId(), 0));
			}

			@Override
			public void handleStatment(PreparedStatement stmt) throws SQLException, NodeException {
				ResultSet keys = stmt.getGeneratedKeys();
				if (keys.next()) {
					userGroupId[0] = keys.getInt(1);
				}
			}
		}, Transaction.INSERT_STATEMENT));

		// restrict to channel
		operate(() -> DBUtils.executeStatement("INSERT INTO user_group_node (user_group_id, node_id) VALUES (?, ?)", new SQLExecutor() {
			@Override
			public void prepareStatement(PreparedStatement stmt) throws SQLException {
				stmt.setInt(1, userGroupId[0]);
				stmt.setInt(2, ObjectTransformer.getInt(channel.getId(), 0));
			}
		}, Transaction.INSERT_STATEMENT));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			for (Node n : Arrays.asList(sideChannel, subChannel, channel, master)) {
				ContentNodeTestDataUtils.clear(n);
			}
		});
	}

	/**
	 * Do the permission check test
	 * @param folder folder
	 * @throws Exception
	 */
	protected void doPermissionCheck(Folder folder) throws Exception {
		// data change operations will always be done in the scope of the object's owning channel
		boolean dataChange = perm == ObjectPermission.edit || perm == ObjectPermission.delete || perm == ObjectPermission.publish;
		boolean allowDataChange = objectSetting == ChannelSetting.LOCAL;

		switch (perm) {
		case delete:
		case edit:
		case publish:
		case view:
			NodeObject testObject = supply(nodeUser, () -> createTestObject(folder));

			operate(nodeUser, () -> {
				// when the object is created locally, we do not check permissions
				// in the master or side channel, because the object does not exist
				// there
				if (objectSetting != ChannelSetting.LOCAL) {
					checkObjectPermission(master, testObject, true);
				}
				checkObjectPermission(channel, testObject, true);
				checkObjectPermission(subChannel, testObject, true);
				if (objectSetting != ChannelSetting.LOCAL) {
					checkObjectPermission(sideChannel, testObject, true);
				}
			});

			operate(restrictedUser, () -> {
				if (objectSetting != ChannelSetting.LOCAL) {
					checkObjectPermission(master, testObject, dataChange ? allowDataChange : false);
				}
				checkObjectPermission(channel, testObject, dataChange ? allowDataChange : true);
				checkObjectPermission(subChannel, testObject, dataChange ? allowDataChange : false);
				if (objectSetting != ChannelSetting.LOCAL) {
					checkObjectPermission(sideChannel, testObject, dataChange ? allowDataChange : false);
				}
			});
		case create:
			if (perm == ObjectPermission.create || !clazz.equals(Folder.class)) {
				operate(nodeUser, () -> {
					if (objectSetting != ChannelSetting.LOCAL) {
						checkClassPermission(master, folder, true);
					}
					checkClassPermission(channel, folder, true);
					checkClassPermission(subChannel, folder, true);
					if (objectSetting != ChannelSetting.LOCAL) {
						checkClassPermission(sideChannel, folder, true);
					}
				});

				operate(restrictedUser, () -> {
					if (objectSetting != ChannelSetting.LOCAL) {
						checkClassPermission(master, folder, false);
					}
					checkClassPermission(channel, folder, true);
					checkClassPermission(subChannel, folder, false);
					if (objectSetting != ChannelSetting.LOCAL) {
						checkClassPermission(sideChannel, folder, false);
					}
				});
			}
			break;
		}
	}

	/**
	 * Check the class permission in the given channel
	 * @param channel channel
	 * @param folder folder on which to check
	 * @param expected expected result
	 * @throws NodeException
	 */
	protected void checkClassPermission(Node channel, Folder folder, boolean expected) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		try {
			t.setChannelId(channel.getId());
			assertEquals("Check permission " + perm + " on " + clazz + " in channel " + channel, expected, perm.checkClass(folder, clazz, null));
		} finally {
			t.resetChannel();
		}
	}

	/**
	 * Check the object permission in the given channel
	 * @param channel channel
	 * @param testObject object to check
	 * @param expected expected result
	 * @throws NodeException
	 */
	protected void checkObjectPermission(Node channel, NodeObject testObject, boolean expected) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		try {
			t.setChannelId(channel.getId());
			assertEquals("Check permission " + perm + " on " + testObject + " in channel " + channel, expected, perm.checkObject(testObject));
		} finally {
			t.resetChannel();
		}
	}

	/**
	 * Create the test folder
	 * @return test folder
	 * @throws NodeException
	 */
	protected Folder createTestFolder() throws NodeException {
		Folder folder = null;
		switch (folderSetting) {
		case INHERITED:
			folder = createFolder(null, master.getFolder(), "Home");
			break;
		case LOCALIZED:
			folder = createFolder(null, master.getFolder(), "Home");
			localize(channel, folder);
			break;
		case LOCAL:
			folder = createFolder(channel, master.getFolder(), "Home");
			break;
		}
		return folder;
	}

	/**
	 * Create the test object
	 * @param folder folder where the test object shall be created
	 * @return test object
	 * @throws NodeException
	 */
	protected NodeObject createTestObject(Folder folder) throws NodeException {
		Node node = null;
		if (objectSetting == ChannelSetting.LOCAL) {
			node = channel;
		}

		LocalizableNodeObject<? extends NodeObject> testObject = null;
		if (clazz.equals(Page.class)) {
			Template template = createTemplate(null, folder, "Template", "this is the template");
			testObject = createPage(node, folder, "Page", "page.html", template);
		} else if (clazz.equals(Template.class)) {
			testObject = createTemplate(node, folder, "Template", "this is the template");
		} else if (clazz.equals(File.class)) {
			testObject = createFile(node, folder, "file.bin", "application/octetstream", new byte[] {1, 2, 3, 4});
		} else if (clazz.equals(Folder.class)) {
			testObject = createFolder(node, folder, "Testfolder");
		}

		if (objectSetting == ChannelSetting.LOCALIZED) {
			localize(channel, testObject);
		}

		return testObject;
	}

	/**
	 * Create a new folder
	 * @param channel channel, if the folder shall be a channel local object, null for a master object in the node
	 * @param mother mother folder
	 * @param name folder name
	 * @return new folder
	 * @throws NodeException
	 */
	protected Folder createFolder(Node channel, Folder mother, String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Folder folder = t.createObject(Folder.class);
		folder.setMotherId(mother.getId());
		folder.setName(name);
		folder.setPublishDir("/");
		if (channel != null) {
			folder.setChannelInfo(channel.getId(), null);
		}
		folder.save();
		t.commit(false);
		return folder;
	}

	/**
	 * Create a template
	 * @param channel channel, if the template shall be a channel local object, null for a master object in the node
	 * @param folder folder
	 * @param name name
	 * @param source source
	 * @return template
	 * @throws NodeException
	 */
	protected Template createTemplate(Node channel, Folder folder, String name, String source) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);

		template.setFolderId(folder.getId());
		template.setMlId(1);
		template.setName(name);
		template.setSource(source);
		if (channel != null) {
			template.setChannelInfo(channel.getId(), null);
		}
		template.save();
		t.commit(false);

		return template;
	}

	/**
	 * Create a page
	 * @param channel channel, if the page shall be a channel local object, null for a master object in the node
	 * @param folder folder
	 * @param name name
	 * @param filename filename
	 * @param template template
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage(Node channel, Folder folder, String name, String filename, Template template) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);

		page.setFilename(filename);
		page.setName(name);
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());

		if (channel != null) {
			page.setChannelInfo(channel.getId(), null);
		}

		// save the page
		page.save();
		t.commit(false);

		return page;
	}

	/**
	 * Create a new file
	 * @param channel channel, if the file shall be a channel local object, null for a master object in the node
	 * @param folder folder
	 * @param name name
	 * @param type file type
	 * @param data file data
	 * @return file
	 * @throws NodeException
	 */
	protected ContentFile createFile(Node channel, Folder folder, String name, String type, byte[] data) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		ContentFile file = (ContentFile)t.createObject(File.class);

		file.setName(name);
		file.setFiletype(type);
		file.setFilesize(data.length);
		file.setFileStream(new ByteArrayInputStream(data));
		file.setFolderId(folder.getId());

		if (channel != null) {
			file.setChannelInfo(channel.getId(), null);
		}

		// save the file
		file.save();
		t.commit(false);

		return file;
	}

	/**
	 * Localize the given object
	 * @param channel channel
	 * @param object object to localize
	 * @return localized object
	 * @throws NodeException
	 */
	protected <T extends NodeObject> T localize(Node channel, LocalizableNodeObject<T> object) throws NodeException {
		@SuppressWarnings("unchecked")
		LocalizableNodeObject<T> localized = (LocalizableNodeObject<T>)object.copy();
		localized.setChannelInfo(channel.getId(), object.getChannelSetId());
		localized.save();
		TransactionManager.getCurrentTransaction().commit(false);
		return localized.getObject();
	}

	/**
	 * Possible values for localization settings
	 */
	public static enum ChannelSetting {
		INHERITED,
		LOCALIZED,
		LOCAL
	}
}
