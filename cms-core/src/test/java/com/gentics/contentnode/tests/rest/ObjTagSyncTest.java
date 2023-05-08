package com.gentics.contentnode.tests.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for synchronizing object tags when saving unchanged object instances
 */
public class ObjTagSyncTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	@BeforeClass
	public static void setupOnce() throws Exception {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
	}

	/**
	 * Test whether saving an empty page adds the object tag for a new definition
	 * @throws Exception
	 */
	@Test
	public void testNewObjPropPage() throws Exception {
		// create a page
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage"));

		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "page_new", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Page New", "page_new"));

		// reload page
		page = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), page);

		// assert that the page does not have the tag
		Trx.consume((o, d) -> {
			assertNull(o + " must not contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, page, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, page);

		// reload page
		page = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), page);

		// assert that page does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, page, def);
	}

	/**
	 * Test for updated object property definition
	 * @throws Exception
	 */
	@Test
	public void testUpdatedObjPropPage() throws Exception {
		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "page_update", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(Page.TYPE_PAGE, constructId, "Page Update", "page_update"));

		// create a page
		Page page = Trx.supply(() -> ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Testpage"));

		// assert that page does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, page, def);

		// update the construct (add new part)
		updateConstruct(constructId);

		// reload page
		page = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), page);

		// assert that the tag does not have the new part
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			assertNull(tag + " must not have new value", tag.getValues().getByKeyname("newtext"));
		}, page, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, page);

		// reload page
		page = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), page);

		// assert that tag has new part now (with default value)
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			Value value = tag.getValues().getByKeyname("newtext");
			assertNotNull(tag + " must not have new value", value);
			assertEquals("Check value", "Default text", value.getValueText());
		}, page, def);
	}

	/**
	 * Test whether saving an empty folder adds the object tag for a new definition
	 * @throws Exception
	 */
	@Test
	public void testNewObjPropFolder() throws Exception {
		// create a folder
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Testfolder"));

		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "folder_new", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Folder New", "folder_new"));

		// reload folder
		folder = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), folder);

		// assert that the folder does not have the tag
		Trx.consume((o, d) -> {
			assertNull(o + " must not contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, folder, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, folder);

		// reload folder
		folder = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), folder);

		// assert that folder does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, folder, def);
	}

	/**
	 * Test for updated object property definition
	 * @throws Exception
	 */
	@Test
	public void testUpdatedObjPropFolder() throws Exception {
		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "folder_update", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Folder", "folder_update"));

		// create a folder
		Folder folder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Testfolder"));

		// assert that folder does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, folder, def);

		// update the construct (add new part)
		updateConstruct(constructId);

		// reload folder
		folder = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), folder);

		// assert that the tag does not have the new part
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			assertNull(tag + " must not have new value", tag.getValues().getByKeyname("newtext"));
		}, folder, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, folder);

		// reload
		folder = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), folder);

		// assert that tag has new part now (with default value)
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			Value value = tag.getValues().getByKeyname("newtext");
			assertNotNull(tag + " must not have new value", value);
			assertEquals("Check value", "Default text", value.getValueText());
		}, folder, def);
	}

	/**
	 * Test whether saving an empty template adds the object tag for a new definition
	 * @throws Exception
	 */
	@Test
	public void testNewObjPropTemplate() throws Exception {
		// create a template
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Testtemplate"));

		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "template_new", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(Template.TYPE_TEMPLATE, constructId, "Template New", "template_new"));

		// reload
		template = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), template);

		// assert that the object does not have the tag
		Trx.consume((o, d) -> {
			assertNull(o + " must not contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, template, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save(false);
		}, template);

		// reload
		template = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), template);

		// assert that object does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, template, def);
	}

	/**
	 * Test for updated object property definition
	 * @throws Exception
	 */
	@Test
	public void testUpdatedObjPropTemplate() throws Exception {
		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "template_update", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(Template.TYPE_TEMPLATE, constructId, "Template", "template_update"));

		// create a template
		Template template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(node.getFolder(), "Testtemplate"));

		// assert that object does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, template, def);

		// update the construct (add new part)
		updateConstruct(constructId);

		// reload
		template = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), template);

		// assert that the tag does not have the new part
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			assertNull(tag + " must not have new value", tag.getValues().getByKeyname("newtext"));
		}, template, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save(false);
		}, template);

		// reload
		template = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), template);

		// assert that tag has new part now (with default value)
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			Value value = tag.getValues().getByKeyname("newtext");
			assertNotNull(tag + " must not have new value", value);
			assertEquals("Check value", "Default text", value.getValueText());
		}, template, def);
	}

	/**
	 * Test whether saving an empty file adds the object tag for a new definition
	 * @throws Exception
	 */
	@Test
	public void testNewObjPropFile() throws Exception {
		// create a file
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", "Contents".getBytes()));

		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "file_new", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(File.TYPE_FILE, constructId, "File New", "file_new"));

		// reload
		file = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), file);

		// assert that the object does not have the tag
		Trx.consume((o, d) -> {
			assertNull(o + " must not contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, file, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, file);

		// reload
		file = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), file);

		// assert that object does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, file, def);
	}

	/**
	 * Test for updated object property definition
	 * @throws Exception
	 */
	@Test
	public void testUpdatedObjPropFile() throws Exception {
		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "file_update", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(File.TYPE_FILE, constructId, "File Update", "file_update"));

		// create a file
		File file = Trx.supply(() -> ContentNodeTestDataUtils.createFile(node.getFolder(), "testfile.txt", "Contents".getBytes()));

		// assert that object does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, file, def);

		// update the construct (add new part)
		updateConstruct(constructId);

		// reload
		file = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), file);

		// assert that the tag does not have the new part
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			assertNull(tag + " must not have new value", tag.getValues().getByKeyname("newtext"));
		}, file, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, file);

		// reload
		file = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), file);

		// assert that tag has new part now (with default value)
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			Value value = tag.getValues().getByKeyname("newtext");
			assertNotNull(tag + " must not have new value", value);
			assertEquals("Check value", "Default text", value.getValueText());
		}, file, def);
	}

	/**
	 * Test whether saving an empty image the object tag for a new definition
	 * @throws Exception
	 */
	@Test
	public void testNewObjPropImage() throws Exception {
		// create an image
		ImageFile image = Trx.supply(() -> {
			try {
				return (ImageFile)ContentNodeTestDataUtils.createImage(node.getFolder(), "blume.jpg", IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg")));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		});

		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "image_new", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(ImageFile.TYPE_IMAGE, constructId, "Image New", "image_new"));

		// reload
		image = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), image);

		// assert that the object does not have the tag
		Trx.consume((o, d) -> {
			assertNull(o + " must not contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, image, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, image);

		// reload
		image = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), image);

		// assert that object does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, image, def);
	}

	/**
	 * Test for updated object property definition
	 * @throws Exception
	 */
	@Test
	public void testUpdatedObjPropImage() throws Exception {
		// create new construct
		Integer constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "image_update", "text"));

		// create new object property definition
		ObjectTagDefinition def = Trx.supply(() -> ContentNodeTestDataUtils
				.createObjectPropertyDefinition(ImageFile.TYPE_IMAGE, constructId, "Image Update", "image_update"));

		// create an image
		ImageFile image = Trx.supply(() -> {
			try {
				return (ImageFile)ContentNodeTestDataUtils.createImage(node.getFolder(), "blume.jpg", IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg")));
			} catch (IOException e) {
				throw new NodeException(e);
			}
		});

		// assert that object does have the tag now
		Trx.consume((o, d) -> {
			assertNotNull(o + " must contain a tag for " + d, o.getObjectTag(d.getObjectTag().getName().substring("object.".length())));
		}, image, def);

		// update the construct (add new part)
		updateConstruct(constructId);

		// reload
		image = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), image);

		// assert that the tag does not have the new part
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			assertNull(tag + " must not have new value", tag.getValues().getByKeyname("newtext"));
		}, image, def);

		// save unchanged
		Trx.consume(o -> {
			TransactionManager.getCurrentTransaction().getObject(o, true).save();
		}, image);

		// reload
		image = Trx.execute(o -> TransactionManager.getCurrentTransaction().getObject(o), image);

		// assert that tag has new part now (with default value)
		Trx.consume((o, d) -> {
			ObjectTag tag = o.getObjectTag(d.getObjectTag().getName().substring("object.".length()));
			assertNotNull(o + " must contain a tag for " + d, tag);
			Value value = tag.getValues().getByKeyname("newtext");
			assertNotNull(tag + " must not have new value", value);
			assertEquals("Check value", "Default text", value.getValueText());
		}, image, def);
	}

	/**
	 * Update the construct by adding a new text part "newtext" with default value "Default text"
	 * @param constructId construct ID
	 * @throws NodeException
	 */
	protected void updateConstruct(int constructId) throws NodeException {
		Trx.consume((cId) -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Construct construct = t.getObject(Construct.class, cId, true);

			Part part = t.createObject(Part.class);
			part.setEditable(1);
			part.setHidden(false);
			part.setKeyname("newtext");
			part.setName("newtext", 1);
			part.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(ShortTextPartType.class));
			part.setDefaultValue(t.createObject(Value.class));
			part.getDefaultValue().setValueText("Default text");

			construct.getParts().add(part);
			construct.save();
		}, constructId);
	}
}
