package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.SYSTEM_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static com.gentics.contentnode.testutils.Creator.createUsergroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.gentics.contentnode.rest.model.perm.RoleItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;
import com.gentics.contentnode.rest.model.perm.TypePermissions;
import com.gentics.contentnode.rest.model.request.TypePermissionRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.TypePermissionList;
import com.gentics.contentnode.rest.model.response.TypePermissionResponse;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for reading/setting group permissions
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class GroupPermissionsTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();
	private static UserGroup nodeGroup;
	private static UserGroup testGroup;
	private static UserGroup subGroup;
	private static UserGroup subSubGroup;
	private static UserGroup sideGroup;
	private static SystemUser testUser;
	private static Node node;
	private static Node channel;
	private static Folder inheritedFolder;
	private static Folder inheritedSubFolder;
	private static Folder localizedMasterFolder;
	private static Folder localizedChannelFolder;
	private static Folder localFolder;
	private static Folder localSubFolder;

	@BeforeClass
	public static void setupData() throws NodeException {
		nodeGroup = supply(t -> t.getObject(UserGroup.class, NODE_GROUP_ID));
		testGroup = supply(() -> create(UserGroup.class, group -> {
			group.setName("Testgroup");
			group.setMotherId(NODE_GROUP_ID);
		}));
		subGroup = supply(() -> createUsergroup("Subgroup", "", testGroup));
		subSubGroup = supply(() -> createUsergroup("Subsubgroup", "", subGroup));
		sideGroup = supply(() -> createUsergroup("Sidegroup", "", nodeGroup));

		testUser = supply(() -> createSystemUser("Test", "Test", null, "tester", "tester", Arrays.asList(testGroup)));

		// delete all nodes
		operate(t -> {
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete(true);
			}
		});

		node = supply(testUser, () -> createNode());
		channel = supply(testUser, () -> createChannel(node, "Channel", "channel", "/"));
		inheritedFolder = supply(testUser, () -> createFolder(node.getFolder(), "Inherited"));
		inheritedSubFolder = supply(testUser, () -> createFolder(inheritedFolder, "Inherited Subfolder"));
		localizedMasterFolder = supply(testUser, () -> createFolder(node.getFolder(), "Localized (Master)"));
		localizedChannelFolder = supply(testUser, () -> update(localize(localizedMasterFolder, channel), folder -> folder.setName("Localized (Channel)")));
		localFolder = supply(testUser, () -> createFolder(node.getFolder(), "Local", channel));
		localSubFolder = supply(testUser, () -> createFolder(localFolder, "Local Subfolder", channel));
	}

	/**
	 * Get test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: test {0}, adminview {1}, groupsview {2}, groupsperm {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedGroup tested : TestedGroup.values()) {
			for (boolean adminView : Arrays.asList(true, false)) {
				for (boolean groupsView : Arrays.asList(true, false)) {
					for (boolean groupsPerm : Arrays.asList(true, false)) {
						if (!groupsView && groupsPerm) {
							continue;
						}
						data.add(new Object[] { tested, adminView, groupsView, groupsPerm });
					}
				}
			}
		}
		return data;
	}

	@Rule
	public ExceptionChecker exceptionRule = new ExceptionChecker();

	@Parameter(0)
	public TestedGroup tested;

	@Parameter(1)
	public boolean adminView;

	@Parameter(2)
	public boolean groupsView;

	@Parameter(3)
	public boolean groupsPerm;

	protected UserGroup testedGroup;

	@Before
	public void setupPermissions() throws NodeException {
		// set permissions on test group
		Permission adminPerm = new Permission(PermHandler.EMPTY_PERM);
		if (adminView) {
			adminPerm.mergeBits(new Permission(PermHandler.PERM_VIEW).toString());
		}
		operate(() -> setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(testGroup), adminPerm.toString()));

		Permission groupPerm = new Permission(PermHandler.EMPTY_PERM);
		if (groupsView) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_VIEW).toString());
		}
		if (groupsPerm) {
			groupPerm.mergeBits(new Permission(PermHandler.PERM_CHANGE_GROUP_PERM).toString());
		}
		operate(() -> setPermissions(UserGroup.TYPE_GROUPADMIN, Arrays.asList(testGroup), groupPerm.toString()));

		// remove all permissions on other groups
		operate(() -> DBUtils.update("DELETE FROM perm WHERE usergroup_id NOT IN (?, ?, ?)", SYSTEM_GROUP_ID, NODE_GROUP_ID, testGroup.getId()));
		operate(() -> PermissionStore.initialize(true));

		testedGroup = getTestedGroup();
	}

	/**
	 * Test reading root level permissions
	 * @throws NodeException
	 */
	@Test
	public void testReadRoot() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		TypePermissionList response = supply(testUser, () -> {
			return new GroupResourceImpl().getPerms(Integer.toString(testedGroup.getId()), null, null, null, null);
		});
		assertResponseCodeOk(response);
		assertThat(response.getItems()).as("Types list").usingElementComparatorOnFields("type").containsExactly(
				new TypePermissions().setType("setting"),
				new TypePermissions().setType("inbox"),
				new TypePermissions().setType("pubqueue"),
				new TypePermissions().setType("admin"),
				new TypePermissions().setType("content"));
	}

	/**
	 * Test reading a single type
	 * @throws NodeException
	 */
	@Test
	public void testReadTypePerm() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		TypePermissionResponse response = supply(testUser, () -> {
			return new GroupResourceImpl().getTypePerms(Integer.toString(testedGroup.getId()), "inbox");
		});
		assertResponseCodeOk(response);
		boolean editable = tested.isSub() && groupsPerm;
		boolean value = !tested.isSub();
		assertThat(response.getPerms()).as("Type perms").usingElementComparatorOnFields("type", "value", "editable").containsExactly(
				new TypePermissionItem().setType(PermType.read).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.setperm).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.instantmessages).setValue(value).setEditable(editable));
	}

	/**
	 * Test reading for a type with instance ID
	 * @throws NodeException
	 */
	@Test
	public void testReadTypePermWithId() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		} else {
			exceptionRule.expect(RestMappedException.class, "Berechtigungen für Typ 'admin (1)' können nicht auf Objekte vergeben werden.");
		}
		supply(testUser, () -> {
			return new GroupResourceImpl().getInstancePerms(Integer.toString(testedGroup.getId()), "admin", 1);
		});
	}

	/**
	 * Test setting type permission
	 * @throws NodeException
	 */
	@Test
	public void testSetTypePerm() throws NodeException {
		if (!tested.isSub() || !groupsView || !adminView || !groupsPerm) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		// grant all permissions
		GenericResponse response = supply(testUser, () -> {
			return new GroupResourceImpl().setTypePerms(Integer.toString(testedGroup.getId()), "pubqueue", 0,
					new TypePermissionRequest().setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true),
							new TypePermissionItem().setType(PermType.setperm).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.pubqueue.type(), null, -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("11000000000000000000000000000000");
		});

		// revoke single permission
		response = supply(testUser, () -> {
			return new GroupResourceImpl().setTypePerms(Integer.toString(testedGroup.getId()), "pubqueue", 0,
					new TypePermissionRequest().setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.setperm).setValue(false))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.pubqueue.type(), null, -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("10000000000000000000000000000000");
		});
	}

	/**
	 * Test reading an instance perm
	 * @throws NodeException
	 */
	@Test
	public void testReadInstancePerm() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		TypePermissionResponse response = supply(testUser, () -> {
			return new GroupResourceImpl().getInstancePerms(Integer.toString(testedGroup.getId()), TypePerms.node.name(), node.getFolder().getId());
		});
		assertResponseCodeOk(response);
		boolean editable = tested.isSub() && groupsPerm;
		boolean value = !tested.isSub();
		assertThat(response.getPerms()).as("Type perms").usingElementComparatorOnFields("type", "value", "editable").containsExactly(
				new TypePermissionItem().setType(PermType.read).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.setperm).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.create).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updatefolder).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.deletefolder).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.readitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.createitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updateitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.deleteitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.importitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.publishpages).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.readtemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.createtemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updatetemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.deletetemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.linktemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updateconstructs).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updateinheritance).setValue(value).setEditable(editable));
		assertThat(response.getRoles())
			.as("Type roles")
			.usingElementComparatorOnFields("id", "label", "editable")
			.containsExactly(new RoleItem().setId(1).setLabel("English").setEditable(editable));

		// read again with feature CHANNELSYNC
		response = supply(testUser, () -> {
			try (FeatureClosure fc = new FeatureClosure(Feature.CHANNELSYNC, true)) {
				return new GroupResourceImpl().getInstancePerms(Integer.toString(testedGroup.getId()), TypePerms.node.name(), node.getFolder().getId());
			}
		});
		assertResponseCodeOk(response);
		assertThat(response.getPerms()).as("Type perms").usingElementComparatorOnFields("type", "value", "editable").containsExactly(
				new TypePermissionItem().setType(PermType.read).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.setperm).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.create).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updatefolder).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.deletefolder).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.readitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.createitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updateitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.deleteitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.importitems).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.publishpages).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.readtemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.createtemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updatetemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.deletetemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.linktemplates).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updateconstructs).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.channelsync).setValue(value).setEditable(editable),
				new TypePermissionItem().setType(PermType.updateinheritance).setValue(value).setEditable(editable));
		assertThat(response.getRoles())
			.as("Type roles")
			.usingElementComparatorOnFields("id", "label", "editable")
			.containsExactly(new RoleItem().setId(1).setLabel("English").setEditable(editable));
	}

	/**
	 * Test reading for an instance without instance ID
	 * @throws NodeException
	 */
	@Test
	public void testReadInstancePermWithoutId() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		} else {
			exceptionRule.expect(RestMappedException.class, "Berechtigungen für Typ 'folder (10002)' können nur auf Objekte vergeben werden.");
		}
		supply(testUser, () -> {
			return new GroupResourceImpl().getTypePerms(Integer.toString(testedGroup.getId()), TypePerms.folder.name());
		});
	}

	/**
	 * Test setting instance permission
	 * @throws NodeException
	 */
	@Test
	public void testSetInstancePerm() throws NodeException {
		if (!tested.isSub() || !groupsView || !adminView || !groupsPerm) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		// grant permissions
		GenericResponse response = supply(testUser, () -> {
			return new GroupResourceImpl().setInstancePerms(Integer.toString(testedGroup.getId()), TypePerms.folder.name(), inheritedFolder.getId(), 0,
					new TypePermissionRequest().setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true),
							new TypePermissionItem().setType(PermType.setperm).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.folder.type(), inheritedFolder.getId(), -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("11000000000000000000000000000000");
		});

		// change some permissions
		response = supply(testUser, () -> {
			return new GroupResourceImpl().setInstancePerms(Integer.toString(testedGroup.getId()), TypePerms.folder.name(), inheritedFolder.getId(), 0,
					new TypePermissionRequest().setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.setperm).setValue(false),
							new TypePermissionItem().setType(PermType.readitems).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.folder.type(), inheritedFolder.getId(), -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("10000000000100000000000000000000");
		});
	}

	/**
	 * Test setting permissions on subobjects
	 * @throws NodeException
	 */
	@Test
	public void testSetSubobjects() throws NodeException {
		if (!tested.isSub() || !groupsView || !adminView || !groupsPerm) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		// grant permissions (not for subobjects)
		GenericResponse response = supply(testUser, () -> {
			return new GroupResourceImpl().setInstancePerms(Integer.toString(testedGroup.getId()), TypePerms.node.name(), node.getFolder().getId(), 0,
					new TypePermissionRequest().setSubObjects(false).setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true),
							new TypePermissionItem().setType(PermType.updateconstructs).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.node.type(), node.getFolder().getId(), -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits for node").isEqualTo("10000000000000000000100000000000");

			pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.folder.type(), inheritedFolder.getId(), -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits for folder").isEqualTo("00000000000000000000000000000000");
		});

		// grant permissions (also for subobjects)
		response = supply(testUser, () -> {
			return new GroupResourceImpl().setInstancePerms(Integer.toString(testedGroup.getId()), TypePerms.node.name(), node.getFolder().getId(), 0,
					new TypePermissionRequest().setSubObjects(true).setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true),
							new TypePermissionItem().setType(PermType.updateconstructs).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.node.type(), node.getFolder().getId(), -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits for node").isEqualTo("10000000000000000000100000000000");

			pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.folder.type(), inheritedFolder.getId(), -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits for folder").isEqualTo("10000000000000000000000000000000");
		});
	}

	/**
	 * Test setting permissions for subgroups
	 * @throws NodeException
	 */
	@Test
	public void testSetSubgroups() throws NodeException {
		if (!tested.isSub() || !groupsView || !adminView || !groupsPerm) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}

		// set permission on group only
		GenericResponse response = supply(testUser, () -> {
			return new GroupResourceImpl().setTypePerms(Integer.toString(testedGroup.getId()), TypePerms.autoupdate.name(), 0,
					new TypePermissionRequest().setSubGroups(false).setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.autoupdate.type(), null, -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("10000000000000000000000000000000");

			pair = t.getGroupPermHandler(subSubGroup.getId()).getPermissions(TypePerms.autoupdate.type(), null, -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("00000000000000000000000000000000");
		});

		// set permission on subgroups
		response = supply(testUser, () -> {
			return new GroupResourceImpl().setTypePerms(Integer.toString(testedGroup.getId()), TypePerms.autoupdate.name(), 0,
					new TypePermissionRequest().setSubGroups(true).setPerms(Arrays.asList(new TypePermissionItem().setType(PermType.read).setValue(true))));
		});
		assertResponseCodeOk(response);
		operate(t -> {
			PermissionPair pair = t.getGroupPermHandler(testedGroup.getId()).getPermissions(TypePerms.autoupdate.type(), null, -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("10000000000000000000000000000000");

			pair = t.getGroupPermHandler(subSubGroup.getId()).getPermissions(TypePerms.autoupdate.type(), null, -1, -1);
			assertThat(pair.getGroupPermissions().toString()).as("Permission bits").isEqualTo("10000000000000000000000000000000");
		});
	}

	/**
	 * Test getting permissions on nodes/channels
	 * @throws NodeException
	 */
	@Test
	public void testNodes() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}
		TypePermissionList response = supply(testUser, () -> {
			return new GroupResourceImpl().getPerms(Integer.toString(testedGroup.getId()), TypePerms.content.name(), null, null, null);
		});
		assertResponseCodeOk(response);
		boolean editable = tested.isSub() && groupsPerm;

		operate(() -> {
			assertThat(response.getItems()).as("Types list").usingElementComparatorOnFields("type", "id", "label", "editable", "channelId").containsExactly(
					node(channel, false, true),
					node(node, editable, true));
		});
	}

	/**
	 * Test getting permissions on folders in node
	 * @throws NodeException
	 */
	@Test
	public void testNodeFolders() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}

		boolean editable = tested.isSub() && groupsPerm;
		assertFolders(node, folder(inheritedFolder, node.getId(), editable, true), folder(localizedMasterFolder, node.getId(), editable, false));
		assertFolders(inheritedFolder, node.getId(), folder(inheritedSubFolder, node.getId(), editable, false));
		assertFolders(inheritedSubFolder, node.getId());
		assertFolders(localizedMasterFolder, node.getId());
	}

	/**
	 * Test getting permissions on folders in channel
	 * @throws NodeException
	 */
	@Test
	public void testChannelFolders() throws NodeException {
		if (!tested.isOwnOrSub() || !groupsView || !adminView) {
			exceptionRule.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", testedGroup.getId()));
		}

		boolean editable = tested.isSub() && groupsPerm;
		assertFolders(channel, folder(inheritedFolder, channel.getId(), false, true), folder(localFolder, channel.getId(), editable, true),
				folder(localizedChannelFolder, channel.getId(), false, false));
		assertFolders(inheritedFolder, channel.getId(), folder(inheritedSubFolder, channel.getId(), false, false));
		assertFolders(inheritedSubFolder, channel.getId());
		assertFolders(localFolder, channel.getId(), folder(localSubFolder, channel.getId(), editable, false));
		assertFolders(localSubFolder, channel.getId());
		assertFolders(localizedChannelFolder, channel.getId());
	}

	/**
	 * Get the tested group
	 * @return tested group
	 */
	protected UserGroup getTestedGroup() {
		switch (tested) {
		case superGroup:
		default:
			return nodeGroup;
		case ownGroup:
			return testGroup;
		case subgroup:
			return subGroup;
		case sideGroup:
			return sideGroup;
		}
	}

	/**
	 * Assert expected type permissions under the node
	 * @param node node
	 * @param expected expected type permissions
	 * @throws NodeException
	 */
	protected void assertFolders(Node node, TypePermissions...expected) throws NodeException {
		TypePermissionList response = supply(testUser, () -> {
			return new GroupResourceImpl().getPerms(Integer.toString(testedGroup.getId()), TypePerms.node.name(), null, node.getFolder().getId(), null);
		});
		assertResponseCodeOk(response);

		operate(() -> {
			assertThat(response.getItems()).as("Types list").usingElementComparatorOnFields("type", "id", "label", "editable", "channelId", "children")
					.containsExactly(expected);
		});
	}

	/**
	 * Assert expected type permissions under the folder in the channel
	 * @param folder folder
	 * @param channelId channel ID
	 * @param expected expected type permissions
	 * @throws NodeException
	 */
	protected void assertFolders(Folder folder, Integer channelId, TypePermissions...expected) throws NodeException {
		TypePermissionList response = supply(testUser, () -> {
			return new GroupResourceImpl().getPerms(Integer.toString(testedGroup.getId()), TypePerms.folder.name(), null, folder.getId(), channelId);
		});
		assertResponseCodeOk(response);

		operate(() -> {
			assertThat(response.getItems()).as("Types list").usingElementComparatorOnFields("type", "id", "label", "editable", "channelId", "children")
					.containsExactly(expected);
		});
	}

	/**
	 * Create reference instance for node
	 * @param node node
	 * @param editable true for editable
	 * @param children true when node has children
	 * @return reference instance
	 * @throws NodeException
	 */
	protected TypePermissions node(Node node, boolean editable, boolean children) throws NodeException {
		return new TypePermissions().setType("node").setId(node.getFolder().getId()).setLabel(node.getFolder().getName()).setEditable(editable)
				.setChildren(children);
	}

	/**
	 * Create reference instance for folder
	 * @param folder folder
	 * @param channelId channel ID
	 * @param editable true for editable
	 * @param children true when folder has children
	 * @return reference instance
	 * @throws NodeException
	 */
	protected TypePermissions folder(Folder folder, Integer channelId, boolean editable, boolean children) throws NodeException {
		return new TypePermissions().setType("folder").setId(folder.getId()).setLabel(folder.getName()).setChannelId(channelId).setEditable(editable)
				.setChildren(children);
	}

	/**
	 * Possible tested groups
	 */
	public static enum TestedGroup {
		superGroup(false, false),
		ownGroup(true, false),
		subgroup(false, true),
		sideGroup(false, false);

		private boolean own;

		private boolean sub;

		TestedGroup(boolean own, boolean sub) {
			this.own = own;
			this.sub = sub;
		}

		/**
		 * Return true, if the group is a subgroup of the user's group
		 * @return true for subgroups
		 */
		public boolean isSub() {
			return sub;
		}

		/**
		 * Return true for the user's own group
		 * @return true for own group
		 */
		public boolean isOwn() {
			return own;
		}

		/**
		 * Return true, if the group is the user's own group or a subgroup
		 * @return true for own and subgroups
		 */
		public boolean isOwnOrSub() {
			return own || sub;
		}
	}
}
