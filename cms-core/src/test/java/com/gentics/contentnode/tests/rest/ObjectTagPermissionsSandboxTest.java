/**
 * 
 */
package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.db.DBUtils.IDS;
import static com.gentics.contentnode.db.DBUtils.select;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createInTagObjectTag;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNodeObject;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.saveRestNodeObjectPropertyTags;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.doesObjectTypeInheritObjectPropertyPermissionsFromObjectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.exceptions.InsufficientPrivilegesException;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test the view and save permission on object tags
 * in the Rest API resources.
 */
@RunWith(value = Parameterized.class)
public class ObjectTagPermissionsSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * test user
	 */
	protected static SystemUser systemUser;

	protected static Auth systemUserAuth;

	/**
	 * User group to use for the tests
	 */
	protected static UserGroup userGroup;

	/**
	 * Node that will be created for testing
	 */
	protected static Node node;

	/**
	 * Construct that will be created for testing
	 */
	protected static Construct construct;

	private static Set<Integer> objectTagIds;

	private static Set<Integer> objectPropertyIds;

	/**
	 * The object type to run the tests for
	 */
	@Parameter(0)
	public int objectType;

	@Parameter(1)
	public AuthType authType;

	/**
	 * Get the test variation data
	 * @return test variation data
	 */
	@Parameters(name= "Object type: {0}, auth: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		// Templates are not tested, because loading new object
		// tags over the Rest API doesn't work properly yet.
		for (int objectType : List.of(Folder.TYPE_FOLDER, Page.TYPE_PAGE, File.TYPE_FILE, ImageFile.TYPE_IMAGE)) {
			for (AuthType authType : List.of(AuthType.LOGIN, AuthType.TOKEN)) {
				data.add(new Object[] {objectType, authType});
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("master", "ObjectTagPermissionsSandboxTest", PublishTarget.NONE));
		int constructId = supply(() -> createConstruct(node, HTMLPartType.class, "construct", "part"));
		construct  = supply(t -> t.getObject(Construct.class, constructId));

		userGroup = supply(() -> createUserGroup("test group", ContentNodeTestDataUtils.NODE_GROUP_ID));
		systemUser = supply(() -> createSystemUser("Tester", "Tester", "", "Tester", "", Arrays.asList(userGroup)));
		systemUserAuth = new Auth(systemUser);

		operate(() -> {
			// Permissions are important
			int rootFolderId = ObjectTransformer.getInt(node.getFolder().getId(), 0);
			PermHandler.setPermissions(Node.TYPE_NODE, rootFolderId, Arrays.asList(userGroup),
					new Permission(PermHandler.FULL_PERM).toString());
			PermHandler.setPermissions(Folder.TYPE_FOLDER, rootFolderId, Arrays.asList(userGroup),
					new Permission(PermHandler.FULL_PERM).toString());
		});

		objectTagIds = supply(() -> select("SELECT id FROM objtag WHERE obj_id != 0", IDS));
		objectPropertyIds = supply(() -> select("SELECT id FROM objtag WHERE obj_id = 0", IDS));
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> clear(node));

		operate(t -> {
			Set<Integer> allTagIds = select("SELECT id FROM objtag WHERE obj_id != 0", IDS);
			allTagIds.removeAll(objectTagIds);

			for (ObjectTag tag : t.getObjects(ObjectTag.class, allTagIds)) {
				tag.delete(true);
			}
		});

		operate(t -> {
			Set<Integer> allPropertyIds = select("SELECT id FROM objtag WHERE obj_id = 0", IDS);
			allPropertyIds.removeAll(objectPropertyIds);

			for (ObjectTagDefinition prop : t.getObjects(ObjectTagDefinition.class, allPropertyIds)) {
				prop.delete(true);
			}
		});
	}

	/**
	 * Test if the "view" permission on object properties is respected
	 * @throws Exception
	 */
	@Test
	public void testViewPermission() throws Exception {
		ObjectTagDefinition objectTagDefinition = supply(() -> createObjectPropertyDefinition(this.objectType, null));
		String name = execute(o -> o.getObjectTag().getName(), objectTagDefinition);

		// Create the node object after the definition, or it will not be in
		// there automatically
		NodeObject nodeObject = supply(() -> createNodeObject(this.objectType, node.getFolder(), "test"));

		// Test view permission: yes
		operate(() -> setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.PERM_VIEW)));

		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					name, true);
		});

		// Test view permission: no
		operate(() -> setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));
		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					name, false);
		});
	}

	/**
	 * Test if the "edit" permission on object properties is respected
	 * @throws Exception
	 */
	@Test
	public void testEditPermission() throws Exception {
		ObjectTagDefinition objectTagDefinition = supply(() -> createObjectPropertyDefinition(this.objectType, null));
		String name = execute(o -> o.getObjectTag().getName(), objectTagDefinition);

		// Create the node object after the definition, or it will not be in
		// there automatically
		NodeObject nodeObject = supply(() -> createNodeObject(this.objectType, node.getFolder(), "test"));
		consume(NodeObject::unlock, nodeObject);

		Map<String, Tag> restTags = loadRestNodeObjectAndCheckIfTagExists(
				this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				name, true);

		// Test update permission: no, tags changed: no
		operate(() -> setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));
		systemUserAuth.withAuth(authType, () -> {
			assertSuccess(() -> saveRestNodeObjectPropertyTags(this.objectType,
					ObjectTransformer.getInteger(nodeObject.getId(), 0), restTags), null);
		});

		// Modify values
		for(Map.Entry<String, Tag> entry : restTags.entrySet()) {
			Tag tag = entry.getValue();
			Map<String, Property> properties = tag.getProperties();
			for(Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
				propertyEntry.getValue().setStringValue("test123");
			}
		}

		// Test update permission: no, tags changed: yes
		systemUserAuth.withAuth(authType, () -> {
			assertError(() -> saveRestNodeObjectPropertyTags(this.objectType,
					ObjectTransformer.getInteger(nodeObject.getId(), 0), restTags), InsufficientPrivilegesException.class, ResponseCode.PERMISSION, null);
		});

		// Test update permission: yes, tags changed: yes
		operate(() -> setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE)));

		systemUserAuth.withAuth(authType, () -> {
			assertSuccess(() -> saveRestNodeObjectPropertyTags(this.objectType,
					ObjectTransformer.getInteger(nodeObject.getId(), 0), restTags), null);
		});
	}

	/**
	 * Test if permissions are correctly inherited from folders and templates to pages and files
	 * @throws Exception
	 */
	@Test
	public void testFolderPermissionInheritance() throws Exception {
		if (this.objectType == Folder.TYPE_FOLDER) {
			// This wouldn't make sense
			return;
		}

		ObjectTagDefinition pageObjectTagDefinition = supply(() -> createObjectPropertyDefinition(this.objectType, null));
		String pageOEName = execute(o -> o.getObjectTag().getName(), pageObjectTagDefinition);
		operate(() -> setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));

		ObjectTagDefinition folderObjectTagDefinition = supply(() -> createObjectPropertyDefinition(Folder.TYPE_FOLDER, null));
		NodeObject nodeObject = supply(() -> ContentNodeTestDataUtils.createNodeObject(this.objectType, node.getFolder(), "test_folder_inheritance"));

		// NodeObject: view permission: no, Template: view permission: no
		operate(() -> setObjectPropertyDefinitionPermissions(folderObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));
		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					pageOEName, false);
		});

		// NodeObject: view permission: no, Template: view permission: yes --> view permission granted
		operate(() -> setObjectPropertyDefinitionPermissions(folderObjectTagDefinition, new Permission(PermHandler.PERM_VIEW)));
		boolean inherits = doesObjectTypeInheritObjectPropertyPermissionsFromObjectType(this.objectType, Folder.TYPE_FOLDER);

		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					pageOEName, inherits);
		});
	}

	/**
	 * Test the object property permission inheritance from templates to any node object
	 * @throws Exception
	 */
	@Test
	public void testTemplatePermissionInheritance() throws Exception {
		if (this.objectType == Template.TYPE_TEMPLATE) {
			// This wouldn't make sense
			return;
		}

		ObjectTagDefinition pageObjectTagDefinition = supply(() -> createObjectPropertyDefinition(this.objectType, null));
		operate(() -> setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));

		ObjectTagDefinition templateObjectTagDefinition = supply(() -> createObjectPropertyDefinition(Template.TYPE_TEMPLATE, null));
		String templateOEName = execute(o -> o.getObjectTag().getName(), templateObjectTagDefinition);
		NodeObject nodeObject = supply(() -> createNodeObject(
				this.objectType, node.getFolder(), "testTemplatePermissionInheritance"));

		// NodeObject: view permission: no, Template: view permission: no
		operate(() -> setObjectPropertyDefinitionPermissions(templateObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));
		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					templateOEName, false);
		});

		// NodeObject: view permission: no, Template: view permission: yes --> view permission granted
		operate(() -> setObjectPropertyDefinitionPermissions(templateObjectTagDefinition, new Permission(PermHandler.PERM_VIEW)));
		boolean inherits = ContentNodeTestUtils.doesObjectTypeInheritObjectPropertyPermissionsFromObjectType(this.objectType, Template.TYPE_TEMPLATE);

		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					templateOEName, inherits);
		});
	}

	/**
	 * Test if the permission inheritance works with nested object tags (they don't have definitions)
	 * @throws Exception
	 */
	@Test
	public void testInTagObjectTagPermissionInheritance() throws Exception {
		ObjectTagDefinition pageObjectTagDefinition = supply(() -> createObjectPropertyDefinition(this.objectType, "parenttag"));
		operate(() -> setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.PERM_VIEW)));
		NodeObject nodeObject = supply(
				() -> createNodeObject(this.objectType, node.getFolder(), "testInTagObjectTagPermissionInheritance"));

		ObjectTag parentTag = null;
		switch (this.objectType) {
		case Folder.TYPE_FOLDER:
			parentTag = supply(() -> ((Folder)nodeObject).getObjectTag("parenttag"));
			break;
		case Template.TYPE_TEMPLATE:
			parentTag = supply(() -> ((Template)nodeObject).getObjectTag("parenttag"));
			break;
		case Page.TYPE_PAGE:
			parentTag = supply(() -> ((Page)nodeObject).getObjectTag("parenttag"));
			break;
		case File.TYPE_FILE:
			parentTag = supply(() -> ((File)nodeObject).getObjectTag("parenttag"));
			break;
		case ImageFile.TYPE_IMAGE:
			parentTag = supply(() -> ((ImageFile)nodeObject).getObjectTag("parenttag"));
			break;
		}

		Assert.assertNotNull("ObjectTag parenttag must exist in node object " + nodeObject, parentTag);

		ObjectTag inTag = execute(pt -> createInTagObjectTag(pt, ObjectTransformer.getInt(construct.getId(), 0), "intag", nodeObject), parentTag);
		testContext.getContext().clearNodeObjectCache();
		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					inTag.getName(), true);
		});

		// Add another sub object tag to test the permission bubbling
		ObjectTag inIntag = supply(() -> createInTagObjectTag(inTag, ObjectTransformer.getInt(construct.getId(), 0), "inIntag", nodeObject));
		testContext.getContext().clearNodeObjectCache();
		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					inIntag.getName(), true);
		});

		// Test again without view permission on the parent object tag
		operate(() -> setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM)));
		systemUserAuth.withAuth(authType, () -> {
			loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
					inIntag.getName(), false);
		});
	}

	/**
	 * @param ObjectTagDefinition
	 * @param permission
	 * @throws NodeException
	 */
	protected void setObjectPropertyDefinitionPermissions(
			ObjectTagDefinition objectTagDefinition, Permission permission) throws NodeException {
		PermHandler.setPermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF,
				ObjectTransformer.getInt(objectTagDefinition.getId(), 0),
				Arrays.asList(userGroup),
				permission.toString());
	}

	/**
	 * @param type
	 * @return
	 * @throws NodeException
	 */
	protected ObjectTagDefinition createObjectPropertyDefinition(int type, String keyword) throws NodeException {
		String name = keyword;
		if (keyword == null) {
			String className = getClass().getName();
			name = className.substring(className.lastIndexOf(".") + 1).toLowerCase();
		}

		// We simply set the object tag name and keyword to the current test class name
		return ContentNodeTestDataUtils.createObjectPropertyDefinition(
				type, ObjectTransformer.getInt(construct.getId(), 0), name, name);
	}
}
