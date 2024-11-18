package com.gentics.contentnode.tests.dirting;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectTagDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.resource.impl.ObjectPropertyResourceImpl;
import com.gentics.contentnode.tests.assertj.GCNAssertions;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for changing object property definitions
 */
public class ObjectTagConsistencyTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static int constructId;

	private static int otherConstructId;

	private static List<Integer> defaultObjectPropertyIds;

	final String TAG_NAME = "copyright_tag";

	private ObjectTagDefinition tagDefinition;

	private Folder folder1;

	private Folder folder2;

	@BeforeClass
	public static void setupOnce() throws NodeException, IOException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());

		constructId = supply(() -> createConstruct(node, HTMLPartType.class, "html", "html"));
		otherConstructId = supply(() -> createConstruct(node, HTMLPartType.class, "otherhtml", "html"));

		defaultObjectPropertyIds = supply(() -> DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDLIST));
	}

	@Before
	public void setup() throws NodeException {
		operate(t -> {
			clear(node);

			List<Integer> objectPropertyIds = new ArrayList<>(DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDLIST));
			objectPropertyIds.removeAll(defaultObjectPropertyIds);

			for (ObjectTagDefinition objectTagDefinition : t.getObjects(ObjectTagDefinition.class, objectPropertyIds)) {
				objectTagDefinition.delete(true);
			}
		});

		tagDefinition = supply(() -> createObjectTagDefinition(TAG_NAME, Folder.TYPE_FOLDER, constructId));
		folder1 = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder1"));
		folder2 = supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Folder2"));
	}

	@Test
	public void testChangeRequired() throws NodeException {
		ObjectTag objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		ObjectTag objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);

		supply(() -> new ObjectPropertyResourceImpl().update(Integer.toString(tagDefinition.getId()), new ObjectProperty().setRequired(true)));

		objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", true)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", true)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
	}

	@Test
	public void testChangeInheritable() throws NodeException {
		ObjectTag objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		ObjectTag objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);

		supply(() -> new ObjectPropertyResourceImpl().update(Integer.toString(tagDefinition.getId()), new ObjectProperty().setInheritable(true)));

		objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", true)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", true)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
	}

	@Test
	public void testChangeConstructId() throws NodeException {
		ObjectTag objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		ObjectTag objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);

		supply(() -> new ObjectPropertyResourceImpl().update(Integer.toString(tagDefinition.getId()), new ObjectProperty().setConstructId(otherConstructId)));

		objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", otherConstructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", otherConstructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
	}

	@Test
	public void testTryChangeType() throws NodeException {
		ObjectTag objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		ObjectTag objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);

		supply(() -> new ObjectPropertyResourceImpl().update(Integer.toString(tagDefinition.getId()), new ObjectProperty().setType(Page.TYPE_PAGE)));

		objectTag1 = supply(() -> folder1.getObjectTag(TAG_NAME));
		objectTag2 = supply(() -> folder2.getObjectTag(TAG_NAME));

		assertThat(objectTag1).as("Object Tag of Folder1")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
		assertThat(objectTag2).as("Object Tag of Folder2")
			.isNotNull()
			.hasFieldOrPropertyWithValue("required", false)
			.hasFieldOrPropertyWithValue("inheritable", false)
			.hasFieldOrPropertyWithValue("constructId", constructId)
			.hasFieldOrPropertyWithValue("objType", Folder.TYPE_FOLDER);
	}
}
