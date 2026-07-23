package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getAdminResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPermResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.PermResponse;
import com.gentics.contentnode.rest.model.response.admin.CustomTool;
import com.gentics.contentnode.rest.model.response.admin.ToolsResponse;
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for getting custom tools over the REST API
 */
@RunWith(Parameterized.class)
public class CustomToolsTest {
	public final static String ADMIN_LOGIN = "tooladmin";

	public final static String ADMIN_PASSWORD = "tooladmin";

	public final static String EDITOR_LOGIN = "tooleditor";

	public final static String EDITOR_PASSWORD = "tooleditor";

	public final static int formGeneratorToolId = 1;

	public final static int superToolId = 2;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(CustomToolsTest.class.getResource("custom_tools.yml").getFile());

	/**
	 * Reference tool "supertool"
	 */
	public static CustomTool superTool = new CustomTool().setId(superToolId).setName(nameMap("Supertool")).setKey("supertool").setNewtab(false);

	/**
	 * Reference tool "formgenerator"
	 */
	public static CustomTool formGeneratorTool = getFormGeneratorTool();

	private static SystemUser system;

	private static Auth systemAuth;

	private static SystemUser admin;

	private static Auth adminAuth;

	private static SystemUser editor;

	private static Auth editorAuth;

	/**
	 * Get the reference tool "formgenerator"
	 * @return reference tool
	 */
	private static CustomTool getFormGeneratorTool() {
		return new CustomTool().setId(formGeneratorToolId).setName(nameMap("Formgenerator", "Formulargenerator")).setKey("formgenerator")
				.setToolUrl("http://www.orf.at/").setIconUrl("http://workstation/DEV/ws/burrito.png").setNewtab(true);
	}

	@BeforeClass
	public static void beforeClass() throws NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup adminGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		assertThat(adminGroup).as("Admin group").isNotNull();

		system = supply(t -> t.getObject(SystemUser.class, 1));
		systemAuth = new Auth(system);

		admin = Trx.supply(() -> Creator.createUser(ADMIN_LOGIN, ADMIN_PASSWORD, "test", "test", "", Arrays.asList(adminGroup)));
		adminAuth = new Auth(admin);

		UserGroup editorGroup = Trx.supply(() -> Creator.createUsergroup("editor", "", adminGroup));
		Trx.operate(() -> PermHandler.setPermissions(PermHandler.TYPE_CUSTOM_TOOL, superToolId, Arrays.asList(editorGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		editor = Trx.supply(() -> Creator.createUser(EDITOR_LOGIN, EDITOR_PASSWORD, "editor", "editor", "", Arrays.asList(editorGroup)));
		editorAuth = new Auth(editor);
	}

	/**
	 * Helper method to return a name map for the given name (in english and german)
	 * @param name tool name
	 * @return name map
	 */
	public static Map<String, String> nameMap(String name) {
		return nameMap(name, name);
	}

	/**
	 * Helper method to return a name map for the given names
	 * @param english english name
	 * @param german german name
	 * @return name map
	 */
	public static Map<String, String> nameMap(String english, String german) {
		Map<String, String> namesMap = new HashMap<>();
		namesMap.put("en", english);
		namesMap.put("de", german);
		return namesMap;
	}

	@Parameters(name = "{index}: auth: {0}")
	public static Collection<Object[]> data() {
		return Stream.of(AuthType.LOGIN, AuthType.TOKEN).map(type -> new Object[] {type}).collect(Collectors.toList());
	}

	@Parameter(0)
	public AuthType authType;

	/**
	 * Test loading the custom tools
	 * @throws NodeException
	 */
	@Test
	public void testLoad() throws NodeException {
		ToolsResponse toolsResponse = systemAuth.withAuth(authType, () -> assertSuccess(() -> getAdminResource().tools(), null));
		assertThat(toolsResponse.getTools()).as("Tools list").usingRecursiveFieldByFieldElementComparator().containsExactly(superTool, formGeneratorTool);
	}

	/**
	 * Test getting tools for restricted user
	 * @throws Exception
	 */
	@Test
	public void testViewPermission() throws Exception {
		ToolsResponse toolsResponse = editorAuth.withAuth(authType, () -> assertSuccess(() -> getAdminResource().tools(), null));
		assertThat(toolsResponse.getTools()).as("Tools list").usingRecursiveFieldByFieldElementComparator().containsExactly(superTool);
	}

	/**
	 * Test checking the view permission for an admin
	 * @throws Exception
	 */
	@Test
	public void testCheckPermissionAdmin() throws Exception {
		adminAuth.withAuth(authType, () -> {
			for (int id : Arrays.asList(formGeneratorToolId, superToolId)) {
				PermResponse permResponse = assertSuccess(() -> getPermResource().getObjectPermission(Permission.view, TypePerms.customtool.name(), id, 0), null);
				assertThat(permResponse.isGranted()).as("Granted").isTrue();
			}
		});
	}

	/**
	 * Test checking the view permission for a restricted user
	 * @throws Exception
	 */
	@Test
	public void testCheckPermission() throws Exception {
		editorAuth.withAuth(authType, () -> {
			for (int id : Arrays.asList(formGeneratorToolId, superToolId)) {
				PermResponse permResponse = assertSuccess(() -> getPermResource().getObjectPermission(Permission.view, TypePerms.customtool.name(), id, 0), null);
				if (id == superToolId) {
					assertThat(permResponse.isGranted()).as("Granted").isTrue();
				} else {
					assertThat(permResponse.isGranted()).as("Granted").isFalse();
				}
			}
		});
	}
}
