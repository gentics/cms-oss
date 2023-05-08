package com.gentics.contentnode.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.ChannelStructure;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.TestFileProvider;

@GCNFeature(set = { Feature.MULTICHANNELLING })
public class CNGenticsImageStoreMCTest {

	private static final String IMAGE_1_2_MD5_HASH = "10259e346f391d5fd740e4b694212b47f55e784e";

	private static int transactionCounter = 1;
	@Rule
	public DBTestContext testContext = new DBTestContext(false);

	public Transaction newTransaction() throws NodeException {
		return testContext.startTransaction(transactionCounter++);
	}
	/**
	 * Set up a nodes & a template to trigger the imagestore
	 */
	ChannelStructure channels;
	Template tpl;
	Folder masterRootFolder;

	private Construct construct;

	private ImageFile img1;

	private ImageFile img2;

	private File publishDir;

	private File imageStoreDir;
	/**
	 * Set up a channel structure, some images & a template.
	 * @throws Exception
	 */
	@Before
	public void init() throws Exception {
		publishDir = new File(ConfigurationValue.PUBLISH_PATH.get(), "pub");
		imageStoreDir = new File(ConfigurationValue.GIS_PATH.get());

		Transaction t = TransactionManager.getCurrentTransaction();
		DBUtils.executeUpdate("UPDATE node SET publish_fs = 0, disable_publish = 1", new Object[]{});
		channels = new ChannelStructure();
		DBUtils.executeUpdate("UPDATE node SET publish_fs = 1, publish_fs_files = 1, publish_fs_pages = 1 where disable_publish = 0", new Object[]{});
		t.commit();
		t = newTransaction();
		channels.reloadObjects();

		masterRootFolder = channels.getNode(Channel.MASTER).getFolder();
		Part textPart = Creator.createTextPartUnsaved("text", 1, 1, "");
		construct = Creator.createConstruct("imgstoretest", "blah", "imgstoretest", Arrays.asList(textPart));
		Template tpl = Creator.createTemplate("imgstoretpl", "<node imgstoretext>", masterRootFolder);
		TemplateTag tt = t.createObject(TemplateTag.class);
		tt.setConstructId(construct.getId());
		tt.setPublic(true);
		tt.setEnabled(1);
		tt.setName("imgstoretext");
		tpl.getTemplateTags().put("imgstoretext", tt);
		tpl.save();
		this.tpl = tpl;
		img1 = Creator.createImage(channels.getNode(Channel.MASTER).getFolder(), "test.jpg", null);
		img2 = Creator.createImage(channels.getNode(Channel.MASTER).getFolder(), "test2.jpg", channels.getNode(Channel.CHANNEL), IOUtils.toByteArray(TestFileProvider.getTestJPG2()));
		t.commit();
		t = newTransaction();
		testContext.publish(true);
		testContext.getContext().setFeature(Feature.TAG_IMAGE_RESIZER, true);
		t.commit();
	}

	/**
	 * Publishes pages with imagestore links, removes an image, an imagestore link and publishes again
	 * @throws Exception
	 */
	@Test
	public void testPublishAndDelete() throws Exception {
		final String image_2_2_md5_hash = "cc19beb0063699acb1048bdd9a90f093dde24bd7";
		final String image_3_2_md5_hash = "88e32189322530f5dc1efd96cfff6cff93586d16";
		final String image_cropandresize_md5_hash = "528729209aca488cb3b240a155f252ec40232fb3";

		Transaction t = newTransaction();
		Page testPage = Creator.createPage("testpage", channels.getNode(Channel.MASTER).getFolder(), tpl);
		ContentTag ct = testPage.getContentTag("imgstoretext");
		Value v = (Value) ct.getValues().get("text");
		
		v.setValueText("/GenticsImageStore/1/2/smart/Content.Node/test.jpg\n" +
				"http://sidechannel/GenticsImageStore/2/2/force/Content.Node/test.jpg\n" +
				"<![CDATA[/GenticsImageStore/3/2/smart/Content.Node/test2.jpg]]>");
		assertTrue("The page must have changes", testPage.save());
		testPage.publish();

		Page localizedTestPage = (Page) testPage.copy();

		localizedTestPage.setChannelInfo(channels.getNode(Channel.SIDECHANNEL).getId(), testPage.getChannelSetId());
		localizedTestPage.save();
		Value v2 = (Value) localizedTestPage.getContentTag("imgstoretext").getValues().get("text");
		v2.setValueText("/GenticsImageStore/4/4/cropandresize/force/1/1/2/2/Content.Node/test.jpg");
		localizedTestPage.save();
		localizedTestPage.publish();
		t.commit();
		testContext.getContext().publish(false, true, (transactionCounter++)*1000L);
		t = newTransaction();

		// assert hashed images
		assertImgStoreFile(image_2_2_md5_hash, true);
		assertImgStoreFile(image_3_2_md5_hash, true);
		assertImgStoreFile(image_cropandresize_md5_hash, true);
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, true);

		// assert published images
		assertImgStorePubFile("sidechannel/GenticsImageStore/2/2/force/Content.Node/test.jpg", true);
		assertImgStorePubFile("sidechannel/GenticsImageStore/4/4/cropandresize/force/1/1/2/2/Content.Node/test.jpg", true);
		assertImgStorePubFile("sidechannel/Content.Node/test.jpg", true);
		assertImgStorePubFile("master/Content.node/home/test.jpg", true);
		assertImgStorePubFile("subchannel/GenticsImageStore/1/2/smart/Content.Node/test.jpg", true);
		assertImgStorePubFile("subchannel/GenticsImageStore/3/2/smart/Content.Node/test2.jpg", true);
		assertImgStorePubFile("subchannel/Content.Node/test.jpg", true);
		assertImgStorePubFile("subchannel/Content.Node/test2.jpg", true);
		assertImgStorePubFile("channel/GenticsImageStore/1/2/smart/Content.Node/test.jpg", true);
		assertImgStorePubFile("channel/GenticsImageStore/3/2/smart/Content.Node/test2.jpg", true);
		assertImgStorePubFile("channel/Content.Node/test.jpg", true);
		assertImgStorePubFile("channel/Content.Node/test2.jpg", true);

		// remove an image and a link
		img2.delete();

		localizedTestPage = t.getObject(Page.class, localizedTestPage.getId(), true, false, true);
		v2 = (Value) localizedTestPage.getContentTag("imgstoretext").getValues().get("text");
		v2.setValueText("a nice page without /GenticsImageStore/urls!");
		localizedTestPage.save();
		localizedTestPage.publish();

		// publish
		t.commit();
		testContext.getContext().publish(false, true, (transactionCounter++)*1000L);
		t = newTransaction();

		// assert hashed images
		assertImgStoreFile(image_2_2_md5_hash, true);
		assertImgStoreFile(image_3_2_md5_hash, false);
		assertImgStoreFile(image_cropandresize_md5_hash, false);
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, true);

		// assert published images
		assertImgStorePubFile("sidechannel/GenticsImageStore/2/2/force/Content.Node/test.jpg", true);
		assertImgStorePubFile("sidechannel/GenticsImageStore/4/4/cropandresize/force/1/1/2/2/Content.Node/test.jpg", false);
		assertImgStorePubFile("sidechannel/Content.Node/test.jpg", true);
		assertImgStorePubFile("master/Content.node/home/test.jpg", true);
		assertImgStorePubFile("subchannel/GenticsImageStore/1/2/smart/Content.Node/test.jpg", true);
		assertImgStorePubFile("subchannel/GenticsImageStore/3/2/smart/Content.Node/test2.jpg", false);
		assertImgStorePubFile("subchannel/Content.Node/test.jpg", true);
		assertImgStorePubFile("subchannel/Content.Node/test2.jpg", false);
		assertImgStorePubFile("channel/GenticsImageStore/1/2/smart/Content.Node/test.jpg", true);
		assertImgStorePubFile("channel/GenticsImageStore/3/2/smart/Content.Node/test2.jpg", false);
		assertImgStorePubFile("channel/Content.Node/test.jpg", true);
		assertImgStorePubFile("channel/Content.Node/test2.jpg", false);
		t.commit();
	}

	/**
	 * Test recreation of GIS Files (if they somehow get lost)
	 * @throws Exception
	 */
	@Test
	public void testRecreateGISFile() throws Exception {
		// create a page using a GIS image
		try (Trx trx = new Trx()) {
			Page testPage = Creator.createPage("testpage", channels.getNode(Channel.MASTER).getFolder(), tpl);
			Value v = testPage.getContentTag("imgstoretext").getValues().getByKeyname("text");
			v.setValueText("/GenticsImageStore/1/2/smart/Content.Node/test.jpg");
			assertTrue("The page must have changes", testPage.save());
			testPage.publish();
			trx.success();
		}

		// publish run
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// imagestore file must exist now
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, true);

		// delete the imagestore file
		assertTrue("ImageStore File must be deleted", deleteImgStoreFile(IMAGE_1_2_MD5_HASH));
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, false);

		// another publish run
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// the imagestore file must be recreated
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, true);
	}

	/**
	 * Test recreation of GIS Files with different md5
	 * @throws Exception
	 */
	@Test
	public void testRecreateGISFileDifferentMD5() throws Exception {
		// create a page using a GIS image
		try (Trx trx = new Trx()) {
			Page testPage = Creator.createPage("testpage", channels.getNode(Channel.MASTER).getFolder(), tpl);
			Value v = testPage.getContentTag("imgstoretext").getValues().getByKeyname("text");
			v.setValueText("/GenticsImageStore/1/2/smart/Content.Node/test.jpg");
			assertTrue("The page must have changes", testPage.save());
			testPage.publish();
			trx.success();
		}

		// publish run
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// imagestore file must exist now
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, true);

		// delete the imagestore file
		assertTrue("ImageStore File must be deleted", deleteImgStoreFile(IMAGE_1_2_MD5_HASH));
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, false);

		Set<Integer> imageStoreImageEntries = Trx.supply(() -> DBUtils.select("SELECT id FROM imagestoreimage WHERE hash = ?", st ->{
			st.setString(1, IMAGE_1_2_MD5_HASH);
		}, DBUtils.IDS));
		assertThat(imageStoreImageEntries).as("Ids of imagestoreimage entries").hasSize(1);

		// change the md5 sum stored in the DB
		Trx.operate(() -> {
			assertEquals("Expected number of imagestoreimage entries changed.", 1, DBUtils.executeUpdate("UPDATE imagestoreimage SET hash = hash_orig WHERE hash = ?",
					new Object[] { IMAGE_1_2_MD5_HASH }));
		});

		// another publish run
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// the imagestore file must be recreated
		assertImgStoreFile(IMAGE_1_2_MD5_HASH, true);

		// and the imagestoreimage entry must also be updated
		assertThat(Trx.supply(() -> DBUtils.select("SELECT id FROM imagestoreimage WHERE hash = ?", st -> {
			st.setString(1, IMAGE_1_2_MD5_HASH);
		}, DBUtils.IDS))).containsOnlyElementsOf(imageStoreImageEntries);
	}

	private void assertImgStorePubFile(String name, boolean shouldExist) {
		assertTrue(name + (shouldExist ? " should exist" : " should not exist"), new File(publishDir, name).exists() ^ !shouldExist);
	}
	private void assertImgStoreFile(String hash, boolean shouldExist) {
		String filename = hash.substring(0,2) + "/" + hash;
		assertTrue(filename + (shouldExist ? " should exist" : " should not exist"), new File(imageStoreDir, filename).exists() ^ !shouldExist);
	}

	/**
	 * Delete the imagestore file
	 * @param hash file hash
	 * @return true iff the file was deleted
	 */
	private boolean deleteImgStoreFile(String hash) {
		String filename = hash.substring(0,2) + "/" + hash;
		File file = new File(imageStoreDir, filename);
		return file.delete();
	}
}
