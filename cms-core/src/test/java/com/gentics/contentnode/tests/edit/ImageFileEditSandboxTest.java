package com.gentics.contentnode.tests.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.ResultSet;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test case for testing editing of node object ImageFile
 * 
 * @author johannes2
 * 
 */
public class ImageFileEditSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext(false);

	/**
	 * blume.jpg DPI: 600x600 Resolution: 1160x1376
	 */
	final String jpgImage2 = "blume.jpg";

	/**
	 * garbage.data DPI: - Resolution: -
	 */
	final String garbageImage = "garbage.data";

	/**
	 * id of the file which is edited image-dpi66x44-res311x211.jpg
	 */
	public final static int CONTENTFILE_ID = 4;

	/**
	 * Image that has foreign dependencies
	 */
	public final static int DIRTTEST_IMAGEFILE_CONTENTFILE_ID = 3;

	/**
	 * File that has foreign dependencies
	 */
	public final static int DIRTTEST_FILE_CONTENTFILE_ID = 1;

	/**
	 * Folder id for creation of new files
	 */
	public final static int FOLDER_ID = 7;

	/**
	 * Test editing meta data of a image
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEditingMetaData() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		String newName = "The_edited_Name";
		String newDescription = "The edited description";
		int fileSize = 9999;
		int imageDpiX = 99;
		int imageDpiY = 88;
		int imageSizeX = 77;
		int imageSizeY = 66;
		float imageFpX= 0.8f;
		float imageFpY= 0.6f;

		// get the transaction timestamp (which will be used as edate)
		int transactionTimestamp = t.getUnixTimestamp(); /**
		 * Get an existing image and change some attributes
		 */ {

			// get the image for editing
			ImageFile imagefile = (ImageFile) t.getObject(File.class, CONTENTFILE_ID, true);

			// Check the meta data before we change it
			assertEquals("Check dpix of the image", 66, imagefile.getDpiX());
			assertEquals("Check dpiy of the image", 44, imagefile.getDpiY());
			assertEquals("Check sizex of the image", 311, imagefile.getSizeX());
			assertEquals("Check sizey of the image", 211, imagefile.getSizeY());
			assertEquals("Check fpx default value of the image", 0.5f, imagefile.getFpX(), 0);
			assertEquals("Check fpy default value of the image", 0.5f, imagefile.getFpY(), 0);

			// change some metadata
			imagefile.setName(newName);
			imagefile.setDescription(newDescription);
			imagefile.setFilesize(fileSize);
			imagefile.setDpiX(imageDpiX);
			imagefile.setDpiY(imageDpiY);
			imagefile.setSizeX(imageSizeX);
			imagefile.setSizeY(imageSizeY);
			imagefile.setFpX(imageFpX);
			imagefile.setFpY(imageFpY);

			// save the image
			imagefile.save();

			// commit the transaction
			t.commit(false);
		} /**
		 * now read the image again and check whether it has the modified meta data
		 */ {
			// Spaces will be replaced by '_' due to filename sanitizing
			String newNameAfterSave = "The_edited_Name";
			ImageFile readImageFile = (ImageFile) t.getObject(File.class, CONTENTFILE_ID);

			assertEquals("Check name of image", newNameAfterSave, readImageFile.getName());
			assertEquals("Check description of image", newDescription, readImageFile.getDescription());
			assertEquals("Check size of the image", fileSize, readImageFile.getFilesize());
			assertEquals("Check dpix of the image", imageDpiX, readImageFile.getDpiX());
			assertEquals("Check dpiy of the image", imageDpiY, readImageFile.getDpiY());
			assertEquals("Check dpix of the image", imageDpiX, readImageFile.getDpiX());
			assertEquals("Check fpy of the image", imageFpX, readImageFile.getFpX(), 0);
			assertEquals("Check fpy of the image", imageFpY, readImageFile.getFpY(), 0);
			assertEquals("Check sizey of the image", imageSizeY, readImageFile.getSizeY());

			// check whether the edate and editor have been set
			assertEquals("Check editor id of the file", DBTestContext.USER_WITH_PERMS, readImageFile.getEditor().getId());
			assertEquals("Check edate of the file", transactionTimestamp, readImageFile.getEDate().getIntTimestamp());

			// also check the data directly in the database
			ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM contentfile WHERE id = " + CONTENTFILE_ID);

			if (res.next()) {
				assertEquals("Check name of file", newName, res.getString("name"));
				assertEquals("Check description of file", newDescription, res.getString("description"));
				assertEquals("Check size of the file", fileSize, res.getInt("filesize"));
				assertEquals("Check dpix of the image", imageDpiX, res.getInt("dpix"));
				assertEquals("Check dpiy of the image", imageDpiY, res.getInt("dpiy"));
				assertEquals("Check fpx of the image", imageFpX, res.getFloat("fpx"), 0);
				assertEquals("Check fpy of the image", imageFpY, res.getFloat("fpy"), 0);
				assertEquals("Check sizex of the image", imageSizeX, res.getInt("sizex"));
				assertEquals("Check sizey of the image", imageSizeY, res.getInt("sizey"));
			} else {
				fail("Did not find the image in the database");
			}
		}
	}

	/**
	 * Test creation of a new image
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateNewImage() throws Exception {
		testContext.startTransactionWithPermissions(true);

		String fileName = "newimage.jpg";
		String fileHash = "A2D3FE21";
		String fileType = "image/jpeg";
		String fileDescription = "Some new imagefile";

		int fileSize = 9999;
		int imageDpiX = 99;
		int imageDpiY = 88;
		int imageSizeX = 77;
		int imageSizeY = 66;

		Transaction t = TransactionManager.getCurrentTransaction();
		ImageFile newImageFile = (ImageFile) t.createObject(File.class);

		assertNull("Check whether the new file has no id", newImageFile.getId());

		// set some attributes
		newImageFile.setFolderId(FOLDER_ID);
		newImageFile.setMd5(fileHash);
		newImageFile.setName(fileName);
		newImageFile.setFiletype(fileType);
		newImageFile.setFilesize(fileSize);
		newImageFile.setDescription(fileDescription);
		newImageFile.setDpiX(imageDpiX);
		newImageFile.setDpiY(imageDpiY);
		newImageFile.setSizeX(imageSizeX);
		newImageFile.setSizeY(imageSizeY);

		// save the image
		newImageFile.save();
		t.commit();

		t = testContext.startTransactionWithPermissions(false);

		int newFileId = ObjectTransformer.getInt(newImageFile.getId(), 0);

		assertTrue("Check whether the new file has a file id after saving", newFileId != 0);

		// now load the image
		ImageFile imagefile = (ImageFile) t.getObject(ImageFile.class, newFileId);

		assertNotNull("Check that the file now really exists", imagefile);
		assertEquals("The md5 of the file does not match.", fileHash, imagefile.getMd5());
		assertEquals("The description of the file does not match", fileDescription, imagefile.getDescription());
		assertEquals("The default value of fpx should be 0.5f", 0.5f, imagefile.getFpX(), 0);
		assertEquals("The default value of fpx should be 0.5f", 0.5f, imagefile.getFpY(), 0);

		// check existence of image

		ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM contentfile where id = " + newFileId);

		if (res.next()) {
			assertNotNull("Check id in the database", res.getObject("id"));
			assertNotNull("Check md5 in the database", res.getObject("md5"));
		} else {
			fail("Could not find complete file data in the database");
		}
	}
	
	/**
	 * Tests filename sanitize.
	 * @throws Exception
	 */
	@Test
	public void testSanitizeName() throws Exception {
		ImageFile file = createAndSaveImage("casa äüï.jpg");
		
		assertEquals("casa-aeuei.jpg", file.getName());
	}

	/**
	 * Tests leading/trailing spaces removal.
	 * @throws Exception
	 */
	@Test
	public void testLeadingTrailingSpace() throws Exception {
		ImageFile file = createAndSaveImage("  leadingtrailing.jpg  ");
		
		assertEquals("leadingtrailing.jpg", file.getName());
	}

	/**
	 * Creates and saves image.
	 * @param string
	 * @return
	 * @throws Exception 
	 */
	private ImageFile createAndSaveImage(String fileName) throws Exception {
		testContext.startTransactionWithPermissions(true);

		String fileHash = "A2D3FE21";
		String fileType = "image/jpeg";
		String fileDescription = "Some new imagefile";

		int fileSize = 9999;
		int imageDpiX = 99;
		int imageDpiY = 88;
		int imageSizeX = 77;
		int imageSizeY = 66;

		Transaction t = TransactionManager.getCurrentTransaction();
		ImageFile newImageFile = (ImageFile) t.createObject(File.class);

		// set some attributes
		newImageFile.setFolderId(FOLDER_ID);
		newImageFile.setMd5(fileHash);
		newImageFile.setName(fileName);
		newImageFile.setFiletype(fileType);
		newImageFile.setFilesize(fileSize);
		newImageFile.setDescription(fileDescription);
		newImageFile.setDpiX(imageDpiX);
		newImageFile.setDpiY(imageDpiY);
		newImageFile.setSizeX(imageSizeX);
		newImageFile.setSizeY(imageSizeY);

		// save the image
		newImageFile.save();
		t.commit();

		t = testContext.startTransactionWithPermissions(false);

		int newFileId = ObjectTransformer.getInt(newImageFile.getId(), 0);

		// now load the image
		return (ImageFile) t.getObject(ImageFile.class, newFileId);
	}

	/**
	 * Test that the binaryupdate of an existing image dirts the correct pages
	 */
	@Test
	public void testDirtPageByBinaryupdate() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		ImageFile file = (ImageFile) t.getObject(File.class, DIRTTEST_IMAGEFILE_CONTENTFILE_ID, true);
		InputStream ins = GenericTestUtils.getPictureResource(jpgImage2);

		file.setFileStream(ins);
		file.setName(jpgImage2);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 19: Target[image-dpi66x44-res311x211.png].dpi
		// 20: Target[image-dpi66x44-res311x211.png].dpix
		// 21: Target[image-dpi66x44-res311x211.png].dpiy
		// 22: Target[image-dpi66x44-res311x211.png].editdate
		// 24: Target[image-dpi66x44-res311x211.png].height
		// 26: Target[image-dpi66x44-res311x211.png].name
		// 27: Target[image-dpi66x44-res311x211.png].size
		// 28: Target[image-dpi66x44-res311x211.png].type
		// 29: Target[image-dpi66x44-res311x211.png].url
		// 30: Target[image-dpi66x44-res311x211.png].width
		int[] pageIds = { 19, 20, 21, 22, 24, 26, 27, 28, 29, 30 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of dpix dirts the correct pages
	 */
	@Test
	public void testDirtPageByDpiX() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		ImageFile file = (ImageFile) t.getObject(File.class, DIRTTEST_IMAGEFILE_CONTENTFILE_ID, true);

		file.setDpiX(99999);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 22: Target[image-dpi66x44-res311x211.png].editdate
		// 19: Target[image-dpi66x44-res311x211.png].dpi
		// 20: Target[image-dpi66x44-res311x211.png].dpix
		int[] pageIds = { 22, 19, 20 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of dpiy dirts the correct pages
	 */
	@Test
	public void testDirtPageByDpiY() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		ImageFile file = (ImageFile) t.getObject(File.class, DIRTTEST_IMAGEFILE_CONTENTFILE_ID, true);

		file.setDpiY(99999);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 22: Target[image-dpi66x44-res311x211.png].editdate
		// 19: Target[image-dpi66x44-res311x211.png].dpi
		// 21: Target[image-dpi66x44-res311x211.png].dpiy
		int[] pageIds = { 22, 19, 21 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of height dirts the correct pages
	 */
	@Test
	public void testDirtPageByHeight() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		ImageFile file = (ImageFile) t.getObject(File.class, DIRTTEST_IMAGEFILE_CONTENTFILE_ID, true);

		file.setSizeY(99999);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been handled)
		testContext.waitForDirtqueueWorker();

		// 22: Target[image-dpi66x44-res311x211.png].editdate
		// 24: Target[image-dpi66x44-res311x211.png].height
		int[] pageIds = { 22, 24 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of width dirts the correct pages
	 */
	@Test
	public void testDirtPageByWidth() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		ImageFile file = (ImageFile) t.getObject(File.class, DIRTTEST_IMAGEFILE_CONTENTFILE_ID, true);

		file.setSizeX(99999);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 22: Target[image-dpi66x44-res311x211.png].editdate
		// 30: Target[image-dpi66x44-res311x211.png].width
		int[] pageIds = { 22, 30 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that the binary update of a file with image data dirts the correct pages
	 */
	@Test
	public void testDirtPageByBinaryupdate2() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);
		
		// get the file for editing
		ImageFile file = (ImageFile) t.getObject(File.class, DIRTTEST_FILE_CONTENTFILE_ID, true);
		InputStream ins = GenericTestUtils.getPictureResource(jpgImage2);

		file.setFileStream(ins);
		file.setFiletype("image/jpg");
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 7: Target[Garbage.data].editdate
		// 13: Target[Garbage.data].width
		// 12: Target[Garbage.data].height
		// 15: Target[Garbage.data].dpi
		// 8: Target[Garbage.data].dpix
		// 11: Target[Garbage.data].dpiy
		// 10: Target[Garbage.data].isimage
		// 5: Target[Garbage.data].type
		// 3: Target[Garbage.data].size
		int[] pageIds = { 7, 13, 12, 15, 8, 11, 10, 5, 3 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Tests the creation of a image nodeobject by using binary garbage
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateNewBrokenImage() throws Exception {

		String fileName = "newFile.jpg";
		String fileType = "image/jpeg";
		String fileDescription = "Some new file";
		int fileSize = 9999;
		int newFileId = -1;

		testContext.startTransactionWithPermissions(true);
		Transaction t = TransactionManager.getCurrentTransaction(); // Phase #1 - Create a new imagefile
		{
			ImageFile newImageFile = (ImageFile) t.createObject(File.class);
			assertFalse("The image should not be an image because it contains garbage content.",newImageFile.isImage());
			newImageFile.setName(fileName);
			newImageFile.setFiletype(fileType);
			newImageFile.setFilesize(fileSize);
			newImageFile.setDescription(fileDescription);

			InputStream ins = GenericTestUtils.getPictureResource(garbageImage);

			newImageFile.setFileStream(ins);
			assertNull("Check whether the new file has no id", newImageFile.getId());

			// set the folder id
			newImageFile.setFolderId(FOLDER_ID);

			// save the file
			newImageFile.save();
			t.commit();
			newFileId = ObjectTransformer.getInt(newImageFile.getId(), 0);
		} // Phase #2 - Check that the failover worked
		{
			t = testContext.startTransactionWithPermissions(false);

			// now load the image
			ImageFile imagefile = (ImageFile) t.getObject(ImageFile.class, newFileId);

			assertNotNull("Check that the file now really exists", imagefile);

			// The mimetype detector should fall back to extension based mime type recongition,
			// however the sizeX and sizey properties will be both 0.
			assertEquals("Filetype does not match.", "image/jpeg", imagefile.getFiletype());

			assertEquals("DpiX does not match.", 0, imagefile.getDpiX());
			assertEquals("DpiY does not match.", 0, imagefile.getDpiY());

			assertEquals("SizeX does not match.", 0, imagefile.getSizeX());
			assertEquals("SizeY does not match.", 0, imagefile.getSizeY());

		}

	}

	/**
	 * Test the update behavior of a image (image-dpi66x44-res311x211.jpg)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateAndUpdate() throws Exception {

		String fileName = "newFile.jpg";
		String fileType = "image/jpeg";
		String fileDescription = "Some new file";
		int fileSize = 9999;
		int newFileId = -1;

		testContext.startTransactionWithPermissions(true);
		Transaction t = TransactionManager.getCurrentTransaction(); // Phase #1 - Create a new imagefile
		{
			ImageFile newImageFile = (ImageFile) t.createObject(File.class);

			// int transactionTimestamp = t.getUnixTimestamp();
			newImageFile.setName(fileName);
			newImageFile.setFiletype(fileType);
			newImageFile.setFilesize(fileSize);
			newImageFile.setDescription(fileDescription);

			InputStream ins = GenericTestUtils.getPictureResource(jpgImage2);

			newImageFile.setFileStream(ins);
			assertNull("Check whether the new file has no id", newImageFile.getId());

			// set the folder id
			newImageFile.setFolderId(FOLDER_ID);

			// save the file
			newImageFile.save();
			t.commit();
			System.out.println(newImageFile.getDpiX());
			newFileId = ObjectTransformer.getInt(newImageFile.getId(), 0);
		} // Phase #2 - Get the stored imagefile and update it
		{
			t = testContext.startTransactionWithPermissions(false);
			// get the file for editing
			ImageFile file = (ImageFile) t.getObject(File.class, newFileId, true);

			// Check current image attributes
			assertEquals("SizeX does not match.", 1160, file.getSizeX());
			assertEquals("SizeY does not match.", 1376, file.getSizeY());
			assertEquals("DpiX does not match.", 600, file.getDpiX());
			assertEquals("DpiY does not match.", 600, file.getDpiY());
			assertEquals("MD5 does not match.", "c86685ec353ff08a97e56909659d0cbc", file.getMd5());

			InputStream ins = GenericTestUtils.getPictureResource("image-dpi66x44-res311x211.jpg");

			file.setFileStream(ins);

			// save the imagefile
			file.save();
			t.commit();

			assertTrue("Check whether the new file has a file id after saving", newFileId != 0);
		} // Phase #3 - Get the imagefile again and check its updated content
		{
			t = testContext.startTransactionWithPermissions(false);
			ImageFile file = (ImageFile) t.getObject(File.class, newFileId);

			// Check updated image attributes
			assertEquals("SizeX does not match.", 311, file.getSizeX());
			assertEquals("SizeY does not match.", 211, file.getSizeY());
			assertEquals("DpiX does not match.", 66, file.getDpiX());
			assertEquals("DpiY does not match.", 44, file.getDpiY());
			assertEquals("MD5 does not match.", "0a368bb0a61161ed035639a9b1b82596", file.getMd5());

		}

	}

}
