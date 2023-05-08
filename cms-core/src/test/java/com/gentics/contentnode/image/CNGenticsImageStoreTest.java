package com.gentics.contentnode.image;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.testutils.GenericTestUtils;

public class CNGenticsImageStoreTest {

	/**
	 * Folder id for creation of new files
	 */
	public final static int FOLDER_ID = 7;

	/**
	 * File id of an existing image file
	 */
	public final static int CONTENTFILE_ID = 4;

	private final static String IMG_NAME_ORIG = "konzept.jpg";
	private final static String IMG_NAME_NEW = "blume.jpg";
	private final static String TAG_NAME = "imgstoretest";
	private final static String PART_NAME = "text";

	private int transactionCounter = 1;

	@Rule
	public DBTestContext testContext = new DBTestContext(false);

	/**
	 * Test getting the edate for files.
	 * The edate must change after updating it's content.
	 * @throws Exception
	 */
	@Test
	public void testEdateFromFileId() throws Exception {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		CNGenticsImageStore gis = new CNGenticsImageStore(config.getNodeConfig());
		Transaction t = testContext.startTransactionWithPermissions(true);

		// get the image for editing
		ImageFile imageFile = (ImageFile) t.getObject(File.class, CONTENTFILE_ID, true);

		// ensure edate > 0
		int origEdate = gis.getEDateFromFileId(String.valueOf(CONTENTFILE_ID));
		assertTrue("An existing Contentfile must have edate > 0", origEdate > 0);

		InputStream ins = GenericTestUtils.getPictureResource("blume.jpg");
		imageFile.setFileStream(ins);
		imageFile.setFiletype("image/jpg");
		assertTrue(imageFile.save());
		t.commit(true);
		int newEdate= gis.getEDateFromFileId(String.valueOf(CONTENTFILE_ID));
		assertTrue("A File must have a bigger edate after it's updated", origEdate < newEdate);

		//Misc Tests for wrong input
		assertTrue("A wrong input must return number <= 0", gis.getEDateFromFileId("") <= 0);
		assertTrue("A wrong input must return number <= 0", gis.getEDateFromFileId("sdgasdh") <= 0);
		assertTrue("A wrong input must return number <= 0", gis.getEDateFromFileId("-1") <= 0);
		assertTrue("A wrong input must return number <= 0", gis.getEDateFromFileId("2352135123") <= 0);
		assertTrue("A wrong input must return number <= 0", gis.getEDateFromFileId("144.12") <= 0);
	}

	/**
	 * Test creating the cache key for files.
	 * The cache key must vary after the content of a file is updated.
	 *
	 * @throws Exception
	 */
	@Test
	public void testCreateCacheKeyAfterUpdate() throws Exception {
		NodeConfigRuntimeConfiguration config = NodeConfigRuntimeConfiguration.getDefault();
		CNGenticsImageStore gis = new CNGenticsImageStore(config.getNodeConfig());
		Transaction t = testContext.startTransactionWithPermissions(true);

		String mode ="bla";
		String width = "320";
		String height = "320";
		String topx = "0";
		String topy = "0";
		String cwidth = "320";
		String cheight = "320";
		String fileId = String.valueOf(CONTENTFILE_ID);
		// get the image for editing
		ImageFile imageFile = (ImageFile) t.getObject(File.class, CONTENTFILE_ID, true);
		String origCropCacheKey = (String) gis.createCropCacheKey(fileId, mode, width, height, topx, topy, cwidth, cheight);
		String origCacheKey = (String) gis.createCacheKey(fileId, mode, cwidth, cheight);

		InputStream ins = GenericTestUtils.getPictureResource("blume.jpg");
		imageFile.setFileStream(ins);
		imageFile.setFiletype("image/jpg");
		assertTrue(imageFile.save());
		t.commit(true);

		String newCropCacheKey = (String) gis.createCropCacheKey(fileId, mode, width, height, topx, topy, cwidth, cheight);
		String newCacheKey = (String) gis.createCacheKey(fileId, mode, cwidth, cheight);
		assertNotEquals("CacheKey must be different after the file is updated", origCacheKey, newCacheKey);
		assertNotEquals("CacheKey must be different after the file is updated", origCropCacheKey, newCropCacheKey);
	}

	/**
	 * Test if the entries in the imagestoreimage are updated during the publish
	 * process when the binary data of an images has changed.
	 */
	@Test
	public void testChangeBinaryData() throws Exception {
		testContext.getContext().setFeature(Feature.TAG_IMAGE_RESIZER, true);

		Transaction t = testContext.startTransactionWithPermissions(true);
		TemplateTag tt = t.createObject(TemplateTag.class);
		Construct construct = Creator.createConstruct(
			TAG_NAME,
			"img",
			TAG_NAME,
			Arrays.asList(Creator.createTextPartUnsaved(PART_NAME, 1, 1, "")));

		tt.setConstructId(construct.getId());
		tt.setPublic(true);
		tt.setEnabled(1);
		tt.setName(TAG_NAME);

		String pubDir = "/Content.Node";
		Node node = Creator.createNode("testnode", "testhost", pubDir, "/", null);
		Template template = Creator.createTemplate("imgstoretpl", "<node " + TAG_NAME + ">", node.getFolder());

		node.setPublishFilesystem(true);
		node.save();
		template.getTemplateTags().put(TAG_NAME, tt);
		template.save();

		t.commit();

		t = testContext.startTransaction(++transactionCounter);
		String imageFilename = "test.jpg";
		ImageFile image = Creator.createImage(node.getFolder(), imageFilename, node);

		image.setFileStream(GenericTestUtils.getPictureResource(IMG_NAME_ORIG));
		image.setFiletype("image/jpg");
		image.save();

		t.commit(false);

		Page page = Creator.createPage("testpage", node.getFolder(), template);
		ContentTag tag = page.getContentTag(TAG_NAME);
		Value v = (Value) tag.getValues().get(PART_NAME);

		v.setValueText("/GenticsImageStore/100/100/smart/" + pubDir + "/" + imageFilename);

		assertTrue("The page must have changes", page.save());
		page.publish();

		t = testContext.startTransaction(++transactionCounter);

		testContext.publish(true);

		Map<String, String> imagestoreData = new HashMap<>();
		List<String> imagestoreDataFields = Arrays.asList("edate", "hash", "hash_orig");

		DBUtils.executeStatement(
			"SELECT * FROM imagestoreimage WHERE contentfile_id = " + image.getId(),
			new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					assertTrue("Image must be in imagestoreimage table", rs.next());

					for (String key : imagestoreDataFields) {
						imagestoreData.put(key, rs.getString(key));
					}
				}
			});

		t = testContext.startTransaction(++transactionCounter);

		image.setFileStream(GenericTestUtils.getPictureResource(IMG_NAME_NEW));
		image.save();

		t = testContext.startTransaction(++transactionCounter);
		testContext.publish(false);

		DBUtils.executeStatement(
			"SELECT * FROM imagestoreimage WHERE contentfile_id = " + image.getId(),
			new SQLExecutor() {
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					assertTrue("Image must still be in imagestoreimage table", rs.next());

					for (String key : imagestoreDataFields) {
						assertFalse(
							"The " + key + " field must have changed",
							imagestoreData.get(key).equals(rs.getString(key)));
					}
				}
			});
	}
}
