/**
 * 
 */
package com.gentics.contentnode.tests.rest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
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
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.response.ResponseCode;
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

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * ID of the permission group
	 */
	protected SystemUser systemUser;

	/**
	 * User group to use for the tests
	 */
	protected UserGroup userGroup;

	/**
	 * Node that will be created for testing
	 */
	protected Node node;

	/**
	 * Construct that will be created for testing
	 */
	protected Construct construct;

	/**
	 * The object type to run the tests for
	 */
	@Parameter
	public int objectType;



	/**
	 * Get the test variation data
	 * @return test variation data
	 */
	@Parameters(name= "Object type: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ Folder.TYPE_FOLDER }, { Page.TYPE_PAGE },
				{ File.TYPE_FILE }, { ImageFile.TYPE_IMAGE },
				// Templates are not tested, because loading new object
				// tags over the Rest API doesn't work properly yet.
		});
	}

	@Before 
	public void setup() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		this.node       = ContentNodeTestDataUtils.createNode("master", "ObjectTagPermissionsSandboxTest", PublishTarget.NONE);
		int constructId = ContentNodeTestDataUtils.createConstruct(node, HTMLPartType.class, "construct", "part");
		this.construct  = t.getObject(Construct.class, constructId);

		this.userGroup = ContentNodeTestDataUtils.createUserGroup("test group", ContentNodeTestDataUtils.NODE_GROUP_ID);
		this.systemUser = ContentNodeTestDataUtils.createSystemUser("Tester", "Tester", "", "Tester", "", Arrays.asList(userGroup));

		testContext.getContext().startTransaction(ObjectTransformer.getInt(systemUser.getId(), 0));
		t = TransactionManager.getCurrentTransaction();

		// Permissions are important
		int rootFolderId = ObjectTransformer.getInt(this.node.getFolder().getId(), 0);
		PermHandler.setPermissions(Node.TYPE_NODE, rootFolderId, Arrays.asList(this.userGroup),
				new Permission(PermHandler.FULL_PERM).toString());
		PermHandler.setPermissions(Folder.TYPE_FOLDER, rootFolderId, Arrays.asList(this.userGroup),
				new Permission(PermHandler.FULL_PERM).toString());
		t.commit(false);
	}

	/**
	 * Test if the "view" permission on object properties is respected
	 * @throws Exception
	 */
	@Test
	public void testViewPermission() throws Exception {
		ObjectTagDefinition objectTagDefinition = createObjectPropertyDefinition(this.objectType, null);

		// Create the node object after the definition, or it will not be in
		// there automatically
		NodeObject nodeObject = ContentNodeTestDataUtils.createNodeObject(this.objectType, this.node.getFolder(), "test");

		// Test view permission: yes
		setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.PERM_VIEW));
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				objectTagDefinition.getObjectTag().getName(), true);

		// Test view permission: no
		setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.EMPTY_PERM));
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				objectTagDefinition.getObjectTag().getName(), false);
	}

	/**
	 * Test if the "edit" permission on object properties is respected
	 * @throws Exception
	 */
	@Test
	public void testEditPermission() throws Exception {
		ObjectTagDefinition objectTagDefinition = createObjectPropertyDefinition(this.objectType, null);

		// Create the node object after the definition, or it will not be in
		// there automatically
		NodeObject nodeObject = ContentNodeTestDataUtils.createNodeObject(this.objectType, this.node.getFolder(), "test");

		Map<String, Tag> restTags = ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(
				this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				objectTagDefinition.getObjectTag().getName(), true);

		// Test update permission: no, tags changed: no
		setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.EMPTY_PERM));
		ContentNodeTestDataUtils.saveRestNodeObjectPropertyTagsAndAssert(this.objectType,
				ObjectTransformer.getInteger(nodeObject.getId(), 0), restTags, ResponseCode.OK);

		// Modify values
		for(Map.Entry<String, Tag> entry : restTags.entrySet()) {
			Tag tag = entry.getValue();
			Map<String, Property> properties = tag.getProperties();
			for(Map.Entry<String, Property> propertyEntry : properties.entrySet()) {
				propertyEntry.getValue().setStringValue("test123");
			}
		}

		// Test update permission: no, tags changed: yes
		ContentNodeTestDataUtils.saveRestNodeObjectPropertyTagsAndAssert(this.objectType,
				ObjectTransformer.getInteger(nodeObject.getId(), 0), restTags, ResponseCode.PERMISSION);

		// Test update permission: yes, tags changed: yes
		setObjectPropertyDefinitionPermissions(objectTagDefinition, new Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE));
		ContentNodeTestDataUtils.saveRestNodeObjectPropertyTagsAndAssert(this.objectType,
				ObjectTransformer.getInteger(nodeObject.getId(), 0), restTags, ResponseCode.OK);
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

		ObjectTagDefinition pageObjectTagDefinition = createObjectPropertyDefinition(this.objectType, null);
		setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM));

		ObjectTagDefinition folderObjectTagDefinition = createObjectPropertyDefinition(Folder.TYPE_FOLDER, null);
		NodeObject nodeObject = ContentNodeTestDataUtils.createNodeObject(this.objectType, this.node.getFolder(), "test_folder_inheritance");

		// NodeObject: view permission: no, Template: view permission: no
		setObjectPropertyDefinitionPermissions(folderObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM));
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				pageObjectTagDefinition.getObjectTag().getName(), false);

		// NodeObject: view permission: no, Template: view permission: yes --> view permission granted
		setObjectPropertyDefinitionPermissions(folderObjectTagDefinition, new Permission(PermHandler.PERM_VIEW));
		boolean inherits = ContentNodeTestUtils.doesObjectTypeInheritObjectPropertyPermissionsFromObjectType(this.objectType, Folder.TYPE_FOLDER);
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				pageObjectTagDefinition.getObjectTag().getName(), inherits);
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

		ObjectTagDefinition pageObjectTagDefinition = createObjectPropertyDefinition(this.objectType, null);
		setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM));

		ObjectTagDefinition templateObjectTagDefinition = createObjectPropertyDefinition(Template.TYPE_TEMPLATE, null);
		NodeObject nodeObject = ContentNodeTestDataUtils.createNodeObject(
				this.objectType, node.getFolder(), "testTemplatePermissionInheritance");

		// NodeObject: view permission: no, Template: view permission: no
		setObjectPropertyDefinitionPermissions(templateObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM));
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				templateObjectTagDefinition.getObjectTag().getName(), false);

		// NodeObject: view permission: no, Template: view permission: yes --> view permission granted
		setObjectPropertyDefinitionPermissions(templateObjectTagDefinition, new Permission(PermHandler.PERM_VIEW));
		boolean inherits = ContentNodeTestUtils.doesObjectTypeInheritObjectPropertyPermissionsFromObjectType(this.objectType, Template.TYPE_TEMPLATE);
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				templateObjectTagDefinition.getObjectTag().getName(), inherits);
	}

	/**
	 * Test if the permission inheritance works with nested object tags (they don't have definitions)
	 * @throws Exception
	 */
	@Test
	public void testInTagObjectTagPermissionInheritance() throws Exception {
		ObjectTagDefinition pageObjectTagDefinition = createObjectPropertyDefinition(this.objectType, "parenttag");
		setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.PERM_VIEW));
		NodeObject nodeObject = ContentNodeTestDataUtils.createNodeObject(
				this.objectType, node.getFolder(), "testInTagObjectTagPermissionInheritance");

		ObjectTag parentTag = null;
		switch (this.objectType) {
		case Folder.TYPE_FOLDER:
			parentTag = ((Folder)nodeObject).getObjectTag("parenttag");
			break;
		case Template.TYPE_TEMPLATE:
			parentTag = ((Template)nodeObject).getObjectTag("parenttag");
			break;
		case Page.TYPE_PAGE:
			parentTag = ((Page)nodeObject).getObjectTag("parenttag");
			break;
		case File.TYPE_FILE:
			parentTag = ((File)nodeObject).getObjectTag("parenttag");
			break;
		case ImageFile.TYPE_IMAGE:
			parentTag = ((ImageFile)nodeObject).getObjectTag("parenttag");
			break;
		}

		Assert.assertNotNull("ObjectTag parenttag must exist in node object " + nodeObject, parentTag);

		ObjectTag inTag = ContentNodeTestDataUtils.createInTagObjectTag(parentTag, ObjectTransformer.getInt(construct.getId(), 0), "intag", nodeObject);
		testContext.getContext().clearNodeObjectCache();
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				inTag.getName(), true);

		// Add another sub object tag to test the permission bubbling
		ObjectTag inIntag = ContentNodeTestDataUtils.createInTagObjectTag(inTag, ObjectTransformer.getInt(construct.getId(), 0), "inIntag", nodeObject);
		testContext.getContext().clearNodeObjectCache();
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				inIntag.getName(), true);

		// Test again without view permission on the parent object tag
		setObjectPropertyDefinitionPermissions(pageObjectTagDefinition, new Permission(PermHandler.EMPTY_PERM));
		ContentNodeTestDataUtils.loadRestNodeObjectAndCheckIfTagExists(this.objectType, ObjectTransformer.getInteger(nodeObject.getId(), 0),
				inIntag.getName(), false);
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
				Arrays.asList(this.userGroup),
				permission.toString());
		TransactionManager.getCurrentTransaction().commit(false);
	}

	/**
	 * @param type
	 * @return
	 * @throws Exception
	 */
	protected ObjectTagDefinition createObjectPropertyDefinition(int type, String keyword) throws Exception {
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
