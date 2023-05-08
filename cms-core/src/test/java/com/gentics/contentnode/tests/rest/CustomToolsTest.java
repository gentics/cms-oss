package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.request.Permission;
import com.gentics.contentnode.rest.model.response.PermResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.admin.CustomTool;
import com.gentics.contentnode.rest.model.response.admin.ToolsResponse;
import com.gentics.contentnode.rest.resource.AdminResource;
import com.gentics.contentnode.rest.resource.impl.AdminResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PermResourceImpl;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for getting custom tools over the REST API
 */
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
	 * Reference tool "formgenerator" (without SID replacement)
	 */
	public static CustomTool formGeneratorTool = getFormGeneratorTool(0);

	private static SystemUser admin;

	private static SystemUser editor;

	/**
	 * Get the reference tool "formgenerator" with the given sid
	 * @param sid sid
	 * @return reference tool
	 */
	private static CustomTool getFormGeneratorTool(int sid) {
		String addedSid = "";
		if (sid > 0) {
			addedSid = Integer.toString(sid);
		}
		return new CustomTool().setId(formGeneratorToolId).setName(nameMap("Formgenerator", "Formulargenerator")).setKey("formgenerator")
				.setToolUrl(String.format("http://www.orf.at/?sid=%s", addedSid)).setIconUrl("http://workstation/DEV/ws/burrito.png").setNewtab(true);
	}

	@BeforeClass
	public static void beforeClass() throws NodeException {
		UserGroup adminGroup = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		assertThat(adminGroup).as("Admin group").isNotNull();

		admin = Trx.supply(() -> Creator.createUser(ADMIN_LOGIN, ADMIN_PASSWORD, "test", "test", "", Arrays.asList(adminGroup)));

		UserGroup editorGroup = Trx.supply(() -> Creator.createUsergroup("editor", "", adminGroup));
		Trx.operate(() -> PermHandler.setPermissions(PermHandler.TYPE_CUSTOM_TOOL, superToolId, Arrays.asList(editorGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		editor = Trx.supply(() -> Creator.createUser(EDITOR_LOGIN, EDITOR_PASSWORD, "editor", "editor", "", Arrays.asList(editorGroup)));
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

	/**
	 * Test loading the custom tools
	 * @throws NodeException
	 */
	@Test
	public void testLoad() throws NodeException {
		ToolsResponse toolsResponse = Trx.supply(() -> {
			AdminResource res = new AdminResourceImpl();
			try {
				return res.tools();
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});

		assertThat(toolsResponse).as("Response").hasCode(ResponseCode.OK);
		assertThat(toolsResponse.getTools()).as("Tools list").usingFieldByFieldElementComparator().containsExactly(superTool, formGeneratorTool);
	}

	/**
	 * Test SID replacement
	 * @throws Exception
	 */
	@Test
	public void testSIDReplacement() throws Exception {
		try (Trx trx = new Trx(admin)) {
			int sid = trx.getTransaction().getSession().getSessionId();
			AdminResource res = new AdminResourceImpl();
			ToolsResponse toolsResponse = res.tools();

			assertThat(toolsResponse).as("Response").hasCode(ResponseCode.OK);
			assertThat(toolsResponse.getTools()).as("Tools list").usingFieldByFieldElementComparator().containsExactly(superTool, getFormGeneratorTool(sid));
		}
	}

	/**
	 * Test getting tools for restricted user
	 * @throws Exception
	 */
	@Test
	public void testViewPermission() throws Exception {
		try (Trx trx = new Trx(editor)) {
			AdminResource res = new AdminResourceImpl();
			ToolsResponse toolsResponse = res.tools();

			assertThat(toolsResponse).as("Response").hasCode(ResponseCode.OK);
			assertThat(toolsResponse.getTools()).as("Tools list").usingFieldByFieldElementComparator().containsExactly(superTool);
		}
	}

	/**
	 * Test checking the view permission for an admin
	 * @throws Exception
	 */
	@Test
	public void testCheckPermissionAdmin() throws Exception {
		try (Trx trx = new Trx(admin)) {
			PermResourceImpl res = new PermResourceImpl();
			for (int id : Arrays.asList(formGeneratorToolId, superToolId)) {
				PermResponse permResponse = res.getObjectPermission(Permission.view, TypePerms.customtool.name(), id, 0);
				assertThat(permResponse).as("Response").hasCode(ResponseCode.OK);
				assertThat(permResponse.isGranted()).as("Granted").isTrue();
			}
		}
	}

	/**
	 * Test checking the view permission for a restricted user
	 * @throws Exception
	 */
	@Test
	public void testCheckPermission() throws Exception {
		try (Trx trx = new Trx(editor)) {
			PermResourceImpl res = new PermResourceImpl();
			for (int id : Arrays.asList(formGeneratorToolId, superToolId)) {
				PermResponse permResponse = res.getObjectPermission(Permission.view, TypePerms.customtool.name(), id, 0);
				assertThat(permResponse).as("Response").hasCode(ResponseCode.OK);
				if (id == superToolId) {
					assertThat(permResponse.isGranted()).as("Granted").isTrue();
				} else {
					assertThat(permResponse.isGranted()).as("Granted").isFalse();
				}
			}
		}
	}
}
