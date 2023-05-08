package com.gentics.contentnode.tests.rest.role;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.attribute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
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
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.FilePrivileges;
import com.gentics.contentnode.rest.model.PagePrivileges;
import com.gentics.contentnode.rest.model.Privilege;
import com.gentics.contentnode.rest.model.RoleModel;
import com.gentics.contentnode.rest.model.RolePermissionsModel;
import com.gentics.contentnode.rest.model.response.role.RoleListResponse;
import com.gentics.contentnode.rest.model.response.role.RolePermResponse;
import com.gentics.contentnode.rest.model.response.role.RoleResponse;
import com.gentics.contentnode.rest.resource.impl.RoleResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for roles
 */
@RunWith(value = Parameterized.class)
public class RoleEditTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	public final static int NODE_GROUP = 2;

	/**
	 * Set of all existing language IDs
	 */
	private static Set<Integer> languageIds;

	private static UserGroup nodeGroup;

	private static UserGroup testGroup;

	private static SystemUser testUser;

	@Parameter(0)
	public boolean perm;

	@Rule
	public ExceptionChecker exceptionRule = new ExceptionChecker();

	@Parameters(name = "{index}: perm {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean perm : Arrays.asList(true, false)) {
			data.add(new Object[] { perm });
		}
		return data;
	}

	/**
	 * Check consistency by executing a select statement (selecting ids of inconsistent records).
	 * @param description Description (what is checked)
	 * @param sql sql statement
	 * @throws NodeException
	 */
	protected static void checkConsistency(String description, String sql) throws NodeException {
		Set<Integer> ids = Trx.supply(() -> DBUtils.select(sql, DBUtils.IDS));
		assertThat(ids).as(description).isEmpty();
	}

	/**
	 * Create a map containing the german and english strings (if not null)
	 * @param german german string
	 * @param english english string
	 * @return map
	 */
	protected static Map<String, String> i18nMap(String german, String english) {
		Map<String, String> map = new HashMap<>();
		if (german != null) {
			map.put("de", german);
		}
		if (english != null) {
			map.put("en", english);
		}
		return map;
	}

	/**
	 * Load language IDs. Create user without permission
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		languageIds = Trx.supply(() -> {
			return DBUtils.select("SELECT id FROM contentgroup", DBUtils.IDS);
		});

		nodeGroup = Trx.supply(t -> t.getObject(UserGroup.class, NODE_GROUP));
		testGroup = Trx.supply(() -> Creator.createUsergroup("Test Group", "", nodeGroup));
		testUser = Trx.supply(() -> Creator.createUser("tester", "tester", "Test", "er", "", Arrays.asList(testGroup)));
		Trx.operate(() -> PermHandler.setPermissions(PermHandler.TYPE_ADMIN, Arrays.asList(testGroup), new Permission(PermHandler.PERM_VIEW).toString()));
	}

	/**
	 * Clean test data
	 * @throws NodeException
	 */
	@Before
	public void cleanData() throws NodeException {
		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM roleperm_obj", null);
			DBUtils.executeUpdate("DELETE FROM roleperm", null);
			DBUtils.executeUpdate("DELETE FROM role_usergroup_assignment", null);
			DBUtils.executeUpdate("DELETE FROM role_usergroup", null);
			DBUtils.executeUpdate("DELETE FROM role", null);
			PermissionStore.initialize(true);
		});
		if (perm) {
			Trx.operate(() -> PermHandler.setPermissions(Role.TYPE_ROLE, Arrays.asList(testGroup), new Permission(PermHandler.PERM_VIEW).toString()));
		} else {
			Trx.operate(() -> PermHandler.setPermissions(Role.TYPE_ROLE, Arrays.asList(testGroup), PermHandler.EMPTY_PERM));
			exceptionRule.expect(InsufficientPrivilegesException.class, "Keine Berechtigung für Rollen.");
		}
	}

	/**
	 * Check consistency after the test
	 * @throws NodeException
	 */
	@After
	public void checkConsistency() throws NodeException {
		Trx.operate(() -> {
			checkConsistency("Dangling roleperm_obj entries",
					"SELECT rpo.id FROM roleperm_obj rpo LEFT JOIN roleperm rp ON rpo.roleperm_id = rp.id WHERE rp.id IS NULL");
			checkConsistency("Dangling roleperm entries", "SELECT rp.id FROM roleperm rp LEFT JOIN role r ON rp.role_id = r.id WHERE r.id IS NULL");
			checkConsistency("Dangling role_usergroup_assignment entries (missing role_usergroup)",
					"SELECT rua.id FROM role_usergroup_assignment rua LEFT JOIN role_usergroup ru ON rua.role_usergroup_id = ru.id WHERE ru.id IS NULL");
			checkConsistency("Dangling role_usergroup_assignment entries (missing folder)",
					"SELECT rua.id FROM role_usergroup_assignment rua LEFT JOIN folder f ON rua.obj_id = f.id AND rua.obj_type = 10002 WHERE f.id IS NULL");
			checkConsistency("Dangling role_usergroup_assignment entries (missing node)",
					"SELECT rua.id FROM role_usergroup_assignment rua LEFT JOIN folder f ON rua.obj_id = f.id AND rua.obj_type = 10001 WHERE f.id IS NULL");
			checkConsistency("Dangling role_usergroup entries (missing role)",
					"SELECT ru.id FROM role_usergroup ru LEFT JOIN role r ON ru.role_id = r.id WHERE r.id IS NULL");
			checkConsistency("Dangling role_usergroup entries (missing usergroup)",
					"SELECT ru.id FROM role_usergroup ru LEFT JOIN usergroup ug ON ru.usergroup_id = ug.id WHERE ug.id IS NULL");

			StringBuilder output = new StringBuilder();
			boolean consistent = PermissionStore.getInstance().checkConsistency(true, output, false);
			assertThat(consistent).as(output.toString()).isTrue();
		});
	}

	/**
	 * Test creating a role
	 * @throws NodeException
	 */
	@Test
	public void testCreate() throws NodeException {
		String germanName = "Rolle";
		String englishName = "Role";
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(testUser, () -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap(germanName, englishName)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		assertThat(created).as("Created role").isNotNull().has(attribute("nameI18n", i18nMap(germanName, englishName)));
	}

	/**
	 * Test creating a role with only english name
	 * @throws NodeException
	 */
	@Test
	public void testCreateEnglish() throws NodeException {
		String englishName = "Role";
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(testUser, () -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap(null, englishName)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		assertThat(created).as("Created role").isNotNull().has(attribute("name", englishName));
		assertThat(created).as("Created role").isNotNull().has(attribute("nameI18n", i18nMap(englishName, englishName)));
	}

	/**
	 * Test creating roles with duplicate names
	 * @throws NodeException
	 */
	@Test
	public void testCreateDuplicate() throws NodeException {
		String name = "Role";

		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel first = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap(null, name)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		RoleModel second = Trx.supply(testUser, () -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap(null, name)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		List<String> englishNames = Stream.of(first, second).map(role -> role.getNameI18n().get("en")).collect(Collectors.toList());
		assertThat(englishNames).as("English role names").doesNotHaveDuplicates().doesNotContain("").doesNotContainNull();
		List<String> germanNames = Stream.of(first, second).map(role -> role.getNameI18n().get("de")).collect(Collectors.toList());
		assertThat(germanNames).as("German role names").doesNotHaveDuplicates().doesNotContain("").doesNotContainNull();
	}

	/**
	 * Test updating a role
	 * @throws NodeException
	 */
	@Test
	public void testUpdate() throws NodeException {
		String originalName = "Role";
		String originalDescription = "Description of the Role";
		String newName = "Updated Role";
		String newDescription = "New Description";
		RoleResourceImpl resource = new RoleResourceImpl();

		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap(originalName, null)).setDescriptionI18n(i18nMap(originalDescription, null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		RoleModel updated = Trx.supply(testUser, () -> {
			RoleResponse response = resource.update(String.valueOf(created.getId()),
					new RoleModel().setNameI18n(i18nMap(null, newName)).setDescriptionI18n(i18nMap(null, newDescription)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		assertThat(updated).as("Updated Role").has(attribute("nameI18n", i18nMap(originalName, newName)))
				.has(attribute("descriptionI18n", i18nMap(originalDescription, newDescription)));
	}

	/**
	 * Test deleting a role
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		setAllPermissions(created);

		List<RoleModel> roles = Trx.supply(() -> {
			RoleListResponse response = resource.list(null, null, null, null);
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getItems();
		});

		assertThat(roles).as("Roles before deleting").usingFieldByFieldElementComparator().contains(created);

		Trx.operate(testUser, () -> {
			resource.delete(String.valueOf(created.getId()));
		});

		roles = Trx.supply(() -> {
			RoleListResponse response = resource.list(null, null, null, null);
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getItems();
		});

		assertThat(roles).as("Roles after deleting").usingFieldByFieldElementComparator().doesNotContain(created);
	}

	/**
	 * Test reading role permissions
	 * @throws NodeException
	 */
	@Test
	public void testReadPermissions() throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			RolePermResponse response = resource.getPerm(String.valueOf(created.getId()));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
	}

	/**
	 * Test granting a single page permission
	 * @throws NodeException
	 */
	@Test
	public void testGrantPagePermission() throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			RolePermResponse response = resource.updatePerm(String.valueOf(created.getId()),
					new RolePermissionsModel().setPage(new PagePrivileges().setCreatepage(true)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
		assertThat(perms.getPage()).as("Page permissions").grantsOnly(Privilege.createpage);
		assertThat(perms.getFile()).as("File permissions").grantsOnly();
		for (PagePrivileges langPriv : perms.getPageLanguages().values()) {
			assertThat(langPriv).as("Language permissions").grantsOnly();
		}
	}

	/**
	 * Test granting a single file permission
	 * @throws NodeException
	 */
	@Test
	public void testGrantFilePermission() throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			RolePermResponse response = resource.updatePerm(String.valueOf(created.getId()),
					new RolePermissionsModel().setFile(new FilePrivileges().setDeletefile(true)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
		assertThat(perms.getPage()).as("Page permissions").grantsOnly();
		assertThat(perms.getFile()).as("File permissions").grantsOnly(Privilege.deletefile);
		for (PagePrivileges langPriv : perms.getPageLanguages().values()) {
			assertThat(langPriv).as("Language permissions").grantsOnly();
		}
	}

	/**
	 * Test granting a single page language permission
	 * @throws NodeException
	 */
	@Test
	public void testGrantPageLanguagePermission() throws NodeException {
		int languageId = languageIds.iterator().next();

		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			Map<Integer, PagePrivileges> map = new HashMap<>();
			map.put(languageId, new PagePrivileges().setTranslatepage(true));
			RolePermResponse response = resource.updatePerm(String.valueOf(created.getId()),
					new RolePermissionsModel().setPageLanguages(map));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
		assertThat(perms.getPage()).as("Page permissions").grantsOnly();
		assertThat(perms.getFile()).as("File permissions").grantsOnly();
		for (Map.Entry<Integer, PagePrivileges> entry : perms.getPageLanguages().entrySet()) {
			PagePrivileges langPriv = entry.getValue();
			if (entry.getKey().equals(languageId)) {
				assertThat(langPriv).as("Language permissions").grantsOnly(Privilege.translatepage);
			} else {
				assertThat(langPriv).as("Language permissions").grantsOnly();
			}
		}
	}

	/**
	 * Test revoking a single page permission
	 * @throws NodeException
	 */
	@Test
	public void testRevokePagePermission() throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		setAllPermissions(created);

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			RolePermResponse response = resource.updatePerm(String.valueOf(created.getId()),
					new RolePermissionsModel().setPage(new PagePrivileges().setPublishpage(false)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
		assertThat(perms.getPage()).as("Page permissions").grantsAllBut(Privilege.publishpage);
		assertThat(perms.getFile()).as("File permissions").grantsAll();
		for (PagePrivileges langPriv : perms.getPageLanguages().values()) {
			assertThat(langPriv).as("Language permissions").grantsAll();
		}
	}

	/**
	 * Test revoking a single file permission
	 * @throws NodeException
	 */
	@Test
	public void testRevokeFilePermission() throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		setAllPermissions(created);

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			RolePermResponse response = resource.updatePerm(String.valueOf(created.getId()),
					new RolePermissionsModel().setFile(new FilePrivileges().setUpdatefile(false)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
		assertThat(perms.getPage()).as("Page permissions").grantsAll();
		assertThat(perms.getFile()).as("File permissions").grantsAllBut(Privilege.updatefile);
		for (PagePrivileges langPriv : perms.getPageLanguages().values()) {
			assertThat(langPriv).as("Language permissions").grantsAll();
		}
	}

	/**
	 * Test revoking a single page language permission
	 * @throws NodeException
	 */
	@Test
	public void testRevokePageLanguagePermission() throws NodeException {
		int languageId = languageIds.iterator().next();

		RoleResourceImpl resource = new RoleResourceImpl();
		RoleModel created = Trx.supply(() -> {
			RoleResponse response = resource.create(new RoleModel().setNameI18n(i18nMap("Role", null)));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getRole();
		});

		setAllPermissions(created);

		RolePermissionsModel perms = Trx.supply(testUser, () -> {
			Map<Integer, PagePrivileges> map = new HashMap<>();
			map.put(languageId, new PagePrivileges().setViewpage(false));

			RolePermResponse response = resource.updatePerm(String.valueOf(created.getId()),
					new RolePermissionsModel().setPageLanguages(map));
			ContentNodeRESTUtils.assertResponseOK(response);
			return response.getPerm();
		});

		assertThat(perms).as("Role permissions").isConsistent(languageIds);
		assertThat(perms.getPage()).as("Page permissions").grantsAll();
		assertThat(perms.getFile()).as("File permissions").grantsAll();
		for (Map.Entry<Integer, PagePrivileges> entry : perms.getPageLanguages().entrySet()) {
			PagePrivileges langPriv = entry.getValue();
			if (entry.getKey().equals(languageId)) {
				assertThat(langPriv).as("Language permissions").grantsAllBut(Privilege.viewpage);
			} else {
				assertThat(langPriv).as("Language permissions").grantsAll();
			}
		}
	}

	/**
	 * Set all privileges to the role
	 * @param role role
	 * @throws NodeException
	 */
	protected void setAllPermissions(RoleModel role) throws NodeException {
		RoleResourceImpl resource = new RoleResourceImpl();
		PagePrivileges fullPage = new PagePrivileges().setCreatepage(true).setDeletepage(true).setPublishpage(true).setTranslatepage(true).setUpdatepage(true)
				.setViewpage(true);
		FilePrivileges fullFile = new FilePrivileges().setCreatefile(true).setDeletefile(true).setUpdatefile(true).setViewfile(true);

		Trx.operate(() -> {
			Map<Integer, PagePrivileges> map = languageIds.stream().collect(Collectors.toMap(Function.identity(), k -> fullPage));
			RolePermResponse response = resource.updatePerm(String.valueOf(role.getId()),
					new RolePermissionsModel().setPageLanguages(map).setPage(fullPage).setFile(fullFile));
			ContentNodeRESTUtils.assertResponseOK(response);
		});
	}
}
