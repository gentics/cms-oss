package com.gentics.contentnode.tests.perm;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.cleanGroups;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.cleanUsers;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createDatasource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getSystemUsers;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Arrays;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Datasource;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.request.SetPermsRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PermBitsResponse;
import com.gentics.contentnode.rest.model.response.PermResponse;
import com.gentics.contentnode.rest.resource.PermResource;
import com.gentics.contentnode.rest.resource.impl.PermResourceImpl;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.tests.utils.Expected;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the {@link PermResource}
 */
public class PermResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Set<Integer> systemUsers;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private static Node node;

	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		systemUsers = getSystemUsers();

		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Before
	public void setup() throws NodeException {
		cleanUsers(systemUsers);
		cleanGroups();
		Trx.operate(Transaction::clearNodeObjectCache);
	}

	/**
	 * Test setting permissions on a type
	 * @throws NodeException
	 */
	@Test
	public void testSet() throws NodeException {
		UserGroup group = create();

		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.constructadmin.name(), -1,
					new SetPermsRequest().setPerm("100").setGroupId(g.getId()));
			assertResponseCodeOk(response);
		}, group);

		String perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.constructadmin.type());
		}, DBUtils.firstString("perm")), group);

		assertThat(perm).as("Changed permissions").isEqualTo("10000000000000000000000000000000");
	}

	/**
	 * Test setting permission on subgroups
	 * @throws NodeException
	 */
	@Test
	public void testSetSubgroups() throws NodeException {
		UserGroup group = create();
		UserGroup subgroup = create(group.getId());

		// update permissions (without subgroup)
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.constructadmin.name(), -1,
					new SetPermsRequest().setPerm("100").setGroupId(g.getId()));
			assertResponseCodeOk(response);
		}, group);

		String perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.constructadmin.type());
		}, DBUtils.firstString("perm")), subgroup);

		assertThat(perm).as("Original permissions").isNull();

		// update again (with subgroup)
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.constructadmin.name(), -1,
					new SetPermsRequest().setPerm("100").setGroupId(g.getId()).setSubGroups(true));
			assertResponseCodeOk(response);
		}, group);

		perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.constructadmin.type());
		}, DBUtils.firstString("perm")), subgroup);

		assertThat(perm).as("Changed permissions").isEqualTo("10000000000000000000000000000000");
	}

	/**
	 * Test setting permissions on subobjects/subtypes
	 * @throws NodeException
	 */
	@Test
	public void testSetSubobjects() throws NodeException {
		UserGroup group = create();

		// update permissions (without subobjects)
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1,
					new SetPermsRequest().setPerm("100").setGroupId(g.getId()));
			assertResponseCodeOk(response);
		}, group);

		String perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.groupadmin.type());
		}, DBUtils.firstString("perm")), group);
		assertThat(perm).as("Original permissions").isNull();

		// update permissions (with subobjects)
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1,
					new SetPermsRequest().setPerm("100").setGroupId(g.getId()).setSubObjects(true));
			assertResponseCodeOk(response);
		}, group);

		perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.groupadmin.type());
		}, DBUtils.firstString("perm")), group);
		assertThat(perm).as("Original permissions").isEqualTo("10000000000000000000000000000000");
	}

	/**
	 * Test that only the supported bits will be set on types
	 * @throws NodeException
	 */
	@Test
	public void testSetBitFiltering() throws NodeException {
		UserGroup group = create();

		// update permissions (without subobjects)
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1,
					new SetPermsRequest().setPerm("1001").setGroupId(g.getId()));
			assertResponseCodeOk(response);
		}, group);

		String perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.admin.type());
		}, DBUtils.firstString("perm")), group);
		assertThat(perm).as("Changed permissions").isEqualTo("10000000000000000000000000000000");
	}

	/**
	 * Test that while setting permission bits to subobjects, no bits are overwritten that are not supported by the root type
	 * @throws NodeException
	 */
	@Test
	public void testSetBitFilteringSubobjects() throws NodeException {
		UserGroup group = create();

		// update permissions on subobject
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.groupadmin.name(), -1,
					new SetPermsRequest().setPerm("11111111111111111111111111111111").setGroupId(g.getId()));
			assertResponseCodeOk(response);
		}, group);
		String perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.groupadmin.type());
		}, DBUtils.firstString("perm")), group);
		assertThat(perm).as("Changed permissions").isEqualTo("11000000111111000000000000000000");

		// update permissions (with subobjects)
		consume(g -> {
			GenericResponse response = new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1,
					new SetPermsRequest().setPerm("010").setGroupId(g.getId()).setSubObjects(true));
			assertResponseCodeOk(response);
		}, group);

		perm = execute(g -> DBUtils.select("SELECT perm FROM perm WHERE usergroup_id = ? AND o_type = ?", st -> {
			st.setInt(1, g.getId());
			st.setInt(2, TypePerms.groupadmin.type());
		}, DBUtils.firstString("perm")), group);
		assertThat(perm).as("Changed permissions").isEqualTo("01000000111111000000000000000000");
	}

	/**
	 * Test setting permissions where the user has no permission on the type
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = InsufficientPrivilegesException.class, message = "Keine Berechtigung zur Vergabe von Berechtigungen auf Typ 'admin (1)'.")
	public void testSetNoTypePermission() throws NodeException {
		UserGroup editingGroup = supply(() -> createUserGroup("Editor Group", NODE_GROUP_ID));
		SystemUser editor = supply(() -> createSystemUser("tester", "tester", null, "tester", "tester", Arrays.asList(editingGroup)));
		UserGroup group = create(editingGroup.getId());

		operate(() -> PermHandler.setPermissions(TypePerms.admin.type(), Arrays.asList(editingGroup), "101"));

		consume(editor, g -> new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1, new SetPermsRequest().setPerm("010").setGroupId(g.getId())),
				group);
	}

	/**
	 * Test setting permissions where the user has no permission on the group
	 * @throws NodeException
	 */
	@Test
	public void testSetNoGroupPermission() throws NodeException {
		UserGroup editingGroup = supply(() -> createUserGroup("Editor Group", NODE_GROUP_ID));
		SystemUser editor = supply(() -> createSystemUser("tester", "tester", null, "tester", "tester", Arrays.asList(editingGroup)));
		UserGroup group = create(editingGroup.getId());

		operate(() -> PermHandler.setPermissions(TypePerms.groupadmin.type(), Arrays.asList(editingGroup), ".............0"));
		exceptionChecker.expect(InsufficientPrivilegesException.class, String.format("Keine Berechtigung für die Gruppe %d.", group.getId()));

		consume(editor, g -> new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1, new SetPermsRequest().setPerm("010").setGroupId(g.getId())),
				group);
	}

	/**
	 * Test setting permission on an invalid type
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Unbekannter Typ 'bogus'.")
	public void testSetInvalidType() throws NodeException {
		Trx.operate(() -> {
			new PermResourceImpl().setPermissions("bogus", -1, new SetPermsRequest().setPerm("1"));
		});
	}

	/**
	 * Test setting permission on an invalid group
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = EntityNotFoundException.class, message = "Die Gruppe mit der ID 4711 konnte nicht gefunden werden.")
	public void testSetInvalidGroup() throws NodeException {
		Trx.operate(() -> {
			new PermResourceImpl().setPermissions(TypePerms.admin.name(), -1, new SetPermsRequest().setPerm("1").setGroupId(4711));
		});
	}

	/**
	 * Test checking view permission on template
	 * @throws NodeException
	 */
	@Test
	public void testReadingTemplatePermission() throws NodeException {
		PermResponse response = supply(() -> new PermResourceImpl().getObjectPermission(Permission.view, "10006", template.getId(), 0));
		assertResponseOK(response);
		assertThat(response.isGranted()).as("Permission granted").isTrue();
	}

	/**
	 * Test checking view permission on inexistent template
	 * @throws NodeException
	 */
	@Test
	public void testReadingInexistentTemplatePermission() throws NodeException {
		PermResponse response = supply(() -> new PermResourceImpl().getObjectPermission(Permission.view, "10006", 4711, 0));
		assertResponseOK(response);
		assertThat(response.isGranted()).as("Permission granted").isFalse();
	}

	/**
	 * Test checking view permission on custom tool by type string
	 * @throws NodeException
	 */
	@Test
	public void testReadingToolPermissionByStringType() throws NodeException {
		PermResponse response = supply(() -> new PermResourceImpl().getObjectPermission(Permission.view, TypePerms.customtool.name(), 1, 0));
		assertResponseOK(response);
		assertThat(response.isGranted()).as("Permission granted").isTrue();
	}

	/**
	 * Test checking view permission on custom tool by type int
	 * @throws NodeException
	 */
	@Test
	public void testReadingToolPermissionByIntType() throws NodeException {
		PermResponse response = supply(() -> new PermResourceImpl().getObjectPermission(Permission.view, "90001", 1, 0));
		assertResponseOK(response);
		assertThat(response.isGranted()).as("Permission granted").isTrue();
	}

	@Test
	public void testDatasourcePermissionsFallback() throws NodeException {
		UserGroup group = create();
		SystemUser user = supply(() -> createSystemUser("Test", "User", null, "tester", "tester", Arrays.asList(group)));

		// give the user read permission on the admin (and all subtypes)
		supply(() -> new PermResourceImpl().setPermissions(TypePerms.admin.name(), 0, new SetPermsRequest()
				.setGroupId(group.getId()).setSubObjects(true).setPerm("10000000000000000000000000000000")));

		// create a datasource
		Datasource datasource = supply(() -> createDatasource("Test Datasource", Arrays.asList("one", "two", "three")));

		// check permission for the user, when no datasource instance permissions are used (user should have read on datasourceadmin and the datasource itself)
		try (FeatureClosure fc = new FeatureClosure(Feature.DATASOURCE_PERM, false)) {
			PermBitsResponse response = supply(user,
					() -> new PermResourceImpl().getPermissions(TypePerms.datasourceadmin.name(), true));
			assertResponseOK(response);
			assertThat(response.getPerm()).as("Datasource Admin Permissions")
					.isEqualTo("10000000000000000000000000000000");
			assertThat(response.getPermissionsMap()).as("Datasource Admin Permissions Map").isNotNull();
			assertThat(response.getPermissionsMap().getPermissions()).as("Datasource Admin Permissions Map").isNotNull()
					.containsOnly(entry(PermType.read, true), entry(PermType.setperm, false));

			response = supply(user, () -> new PermResourceImpl().getPermissions(TypePerms.datasource.name(),
					datasource.getId(), -1, -1, -1, true));
			assertResponseOK(response);
			assertThat(response.getPerm()).as("Datasource Instance Permissions")
					.isEqualTo("10000000000000000000000000000000");
			assertThat(response.getPermissionsMap()).as("Datasource Instance Permissions Map").isNotNull();
			assertThat(response.getPermissionsMap().getPermissions()).as("Datasource Instance Permissions Map").isNotNull()
					.containsOnly(entry(PermType.read, true), entry(PermType.setperm, false));
		}

		// check permission for the user, when datasource instance permissions are used (user should have read on datasourceadmin but no permission on the datasource itself)
		try (FeatureClosure fc = new FeatureClosure(Feature.DATASOURCE_PERM, true)) {
			PermBitsResponse response = supply(user,
					() -> new PermResourceImpl().getPermissions(TypePerms.datasourceadmin.name(), true));
			assertResponseOK(response);
			assertThat(response.getPerm()).as("Datasource Admin Permissions")
					.isEqualTo("10000000000000000000000000000000");
			assertThat(response.getPermissionsMap()).as("Datasource Admin Permissions Map").isNotNull();
			assertThat(response.getPermissionsMap().getPermissions()).as("Datasource Admin Permissions Map").isNotNull()
					.containsOnly(entry(PermType.read, true), entry(PermType.setperm, false));

			response = supply(user, () -> new PermResourceImpl().getPermissions(TypePerms.datasource.name(),
					datasource.getId(), -1, -1, -1, true));
			assertResponseOK(response);
			assertThat(response.getPerm()).as("Datasource Instance Permissions")
					.isEqualTo("00000000000000000000000000000000");
			assertThat(response.getPermissionsMap()).as("Datasource Instance Permissions Map").isNotNull();
			assertThat(response.getPermissionsMap().getPermissions()).as("Datasource Instance Permissions Map").isNotNull()
					.containsOnly(entry(PermType.read, false), entry(PermType.setperm, false));
		}
	}

	/**
	 * Create subgroup of "node" group without any permission set
	 * @return created group
	 * @throws NodeException
	 */
	protected UserGroup create() throws NodeException {
		return create(NODE_GROUP_ID);
	}

	/**
	 * Create subgroup of the given group without any permission set
	 * @param parentGroupId parent group ID
	 * @return created group
	 * @throws NodeException
	 */
	protected UserGroup create(int parentGroupId) throws NodeException {
		UserGroup group = supply(() -> createUserGroup("Testgroup", parentGroupId));
		consume(g -> {
			update("DELETE FROM perm WHERE usergroup_id = ?", g.getId());
			PermissionStore.getInstance().refreshGroup(g.getId());
		}, group);
		return group;
	}
}
