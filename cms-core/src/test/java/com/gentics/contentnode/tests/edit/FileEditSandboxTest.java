package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.convertStreamToString;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.generateDataFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequestWrapper;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.i18n.I18nString;
import com.gentics.contentnode.etc.PropertyTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FileFactory;
import com.gentics.contentnode.factory.object.FileSizeException;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.response.FileUploadResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.i18n.CNI18nString;


/**
 * Test case for testing editing of files
 *
 * @author johannes2
 */
public class FileEditSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * id of the file which is edited
	 */
	public final static int CONTENTFILE_ID = 3;

	/**
	 * File that has foreign dependencies
	 */
	public final static int DIRTTEST_FILE_CONTENTFILE_ID = 1;

	/**
	 * Folder id for creation of new files
	 */
	public final static int FOLDER_ID = 7;

	/**
	 * Test editing meta data of a file
	 *
	 * @throws Exception
	 */
	@Test
	public void testEditingMetaData() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		String newName = "The_edited_Name";
		String fileType = "application/octet-stream";
		String newDescription = "The edited description";
		int newFilesize = 9991;

		// get the transaction timestamp (which will be used as edate)
		int transactionTimestamp = t.getUnixTimestamp();

		// get the file for editing
		File file = (ContentFile) t.getObject(File.class, CONTENTFILE_ID, true);

		// change some metadata
		file.setName(newName);
		file.setFiletype(fileType);
		file.setDescription(newDescription);
		file.setFilesize(newFilesize);

		// save the file
		file.save();

		// commit the transaction
		t.commit(false);

		// now read the file again and check whether it has the modified meta
		// data
		File readFile = (File) t.getObject(File.class, CONTENTFILE_ID);

		assertEquals("Check name of file", newName, readFile.getName());
		assertEquals("Check description of file", newDescription, readFile.getDescription());
		assertEquals("Check size of the file", newFilesize, readFile.getFilesize());

		// check whether the edate and editor have been set
		assertEquals("Check editor id of the file", DBTestContext.USER_WITH_PERMS, readFile.getEditor().getId());
		assertEquals("Check edate of the file", transactionTimestamp, readFile.getEDate().getIntTimestamp());

		// also check the data directly in the database
		ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM contentfile WHERE id = " + CONTENTFILE_ID);

		if (res.next()) {
			assertEquals("Check name of file", newName, res.getString("name"));
			assertEquals("Check description of file", newDescription, res.getString("description"));
			assertEquals("Check size of the file", newFilesize, res.getInt("filesize"));
		} else {
			fail("Did not find the file in the database");
		}
		res.close();
	}

	/**
	 * Will test the suggestNewFileName method
	 * New files with the same parent folder and the same filename as existing files should be renamed
	 * @throws Exception
	 */
	@Test
	public void testSuggestNewFileName()throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		File file = (File) t.createObject(File.class);

		file.setName("image-dpi72x72-res250x188-alpha.png");
		file.setFiletype("image/gif");
		file.setFolderId(FOLDER_ID);

		String oldFileName = file.getName();
		FileFactory.suggestNewFilename(file);

		assertFalse("Both filenames should not be the same.", file.getName().equals(oldFileName));
	}

	/**
	 * Will test the suggestNewFilename method
	 * Existing files that have no conflict with other files should get the same name.
	 */
	@Test
	public void testSuggestNewFileName2() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		File file = (ContentFile) t.getObject(File.class, CONTENTFILE_ID, true);
		String oldFilename = file.getName();
		FileFactory.suggestNewFilename(file);

		assertEquals("Both filenames should have the same name.", oldFilename, file.getName());
	}

	/**
	 * Will test the suggestNewFilename method
	 * New files with a different parent folder (that has a different pubdir path) and the same filename as existing files should not renamed
	 * @throws Exception
	 */
	@Test
	public void testSuggestNewFileName3() throws Exception {

		Transaction t = testContext.startTransactionWithPermissions(true);
		File file = (File) t.createObject(File.class);

		file.setName("textimage1.1.gif");
		file.setFiletype("image/gif");
		file.setFolderId(FOLDER_ID - 2);

		String oldFileName = file.getFilename();
		FileFactory.suggestNewFilename(file);

		assertEquals("Both filenames should be the same", oldFileName, file.getName());

	}

	/**
	 * Test if the selected file can be copied and the suggestion code is also working there as expected.
	 * @throws Exception
	 */
	@Test
	public void testCopyFile() throws Exception {

		Transaction t = testContext.startTransactionWithPermissions(true);
		File file = (ContentFile) t.getObject(File.class, CONTENTFILE_ID, true);
		FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);
		File newFile = fileFactory.copyFile(file);

		assertNull("The id should be null after creating the copy.", newFile.getId());
		newFile.save();
		assertNotNull("After saving the new file should contain a valid fileId.", newFile.getId());
		assertFalse("Filenames should not be the same.", file.getName().equalsIgnoreCase(newFile.getName()));
	}

	/**
	 * Test if the selected file can be copied and the suggestion code is also working there as expected.
	 * @throws Exception
	 */
	@Test
	public void testCopyFileWithNewFilename() throws Exception {

		String newFilename = "Test1234";
		Transaction t = testContext.startTransactionWithPermissions(true);
		File file = (ContentFile) t.getObject(File.class, CONTENTFILE_ID, true);
		FileFactory fileFactory = (FileFactory) t.getObjectFactory(File.class);
		File newFile = fileFactory.copyFile(file, newFilename);

		assertNull("The id should be null after creating the copy.", newFile.getId());
		newFile.save();
		assertNotNull("After saving the new file should contain a valid fileId.", newFile.getId());
		assertFalse("Filenames should not be the same.", file.getName().equalsIgnoreCase(newFile.getName()));
		assertEquals("The newfile should have the filename {" + newFilename + "}", newFilename, newFile.getName());
	}

	/**
	 * Test that edit of the .name dirts the correct pages
	 */
	@Test
	public void testDirtPageByFilename() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		File file = (ContentFile) t.getObject(File.class, DIRTTEST_FILE_CONTENTFILE_ID, true);

		file.setName("Blablabal");
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();
		testContext.checkDirtedPages(dirtedPagesBeforeModification, new int[] { 2, 4, 7});
	}

	/**
	 * Test that edit of the .description dirts the correct pages
	 */
	@Test
	public void testDirtPageByDescription() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		File file = (ContentFile) t.getObject(File.class, DIRTTEST_FILE_CONTENTFILE_ID, true);

		file.setDescription("Blablabal");
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 7: Target[Garbage.data].editdate
		int[] pageIds = {7, 9};

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of the .folder dirts the correct pages
	 */
	@Test
	public void testDirtPageByFolder() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		File file = (ContentFile) t.getObject(File.class, DIRTTEST_FILE_CONTENTFILE_ID, true);

		file.setFolderId(11);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 6: Target[Garbage.data].folder
		// 7: Target[Garbage.data].editdate
		// 4: Target[Garbage.data].url
		int[] pageIds = { 6, 7, 4};

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of the .type dirts the correct pages
	 */
	@Test
	public void testDirtPageByFiletype() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		File file = (ContentFile) t.getObject(File.class, DIRTTEST_FILE_CONTENTFILE_ID, true);

		file.setFiletype("image/png");
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 7: Target[Garbage.data].editdate
		// 5: Target[Garbage.data].type
		// 10: Target[Garbage.data].isimage
		int[] pageIds = { 7, 5, 10 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test that edit of the .size dirts the correct pages
	 */
	@Test
	public void testDirtPageBySize() throws Exception {
		// republish everything to build dependencies
		testContext.publish(true);

		// Edit and Save our file
		Transaction t = testContext.startTransactionWithPermissions(true);

		int dirtedPagesBeforeModification = PublishQueue.countDirtedObjects(Page.class, false, null);

		// get the file for editing
		File file = (ContentFile) t.getObject(File.class, DIRTTEST_FILE_CONTENTFILE_ID, true);

		file.setFilesize(99999);
		file.save();
		t.commit(false);

		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		testContext.waitForDirtqueueWorker();

		// 3: Target[Garbage.data].size
		// 7: Target[Garbage.data].editdate
		int[] pageIds = { 3, 7 };

		testContext.checkDirtedPages(dirtedPagesBeforeModification, pageIds);
	}

	/**
	 * Test whether the file caches behave like expected while editing a file
	 * @throws Exception
	 */
	@Test
	public void testFileCaches() throws Exception {
		String newName = "This_is_the_new_filename";

		// start a first transaction
		Transaction startFirstFetchLater = testContext.startTransactionWithPermissions(true);
		Transaction preEdit = testContext.startTransactionWithPermissions(false);
		// get the original page
		File preEditFile = (ContentFile) preEdit.getObject(File.class, CONTENTFILE_ID);
		String oldName = preEditFile.getName();

		// start a new transaction for editing
		Transaction edit = testContext.startTransactionWithPermissions(false);
		// get the page for editing
		File editFile = (ContentFile) edit.getObject(File.class, CONTENTFILE_ID, true);

		// now start a concurrent transaction
		Transaction concurrent = testContext.startTransactionWithPermissions(false);
		File concurrentFile = (ContentFile) concurrent.getObject(File.class, CONTENTFILE_ID);

		assertEquals("Check file name before editing", oldName, concurrentFile.getName());

		// now edit the file
		editFile.setName(newName);

		// check names in other transactions
		assertEquals("Check file name for preedit after editing", oldName, ((ContentFile) preEdit.getObject(File.class, CONTENTFILE_ID)).getName());
		assertEquals("Check file name for concurrent after editing", oldName, ((ContentFile) concurrent.getObject(File.class, CONTENTFILE_ID)).getName());

		// save the file
		TransactionManager.setCurrentTransaction(edit);
		editFile.save();

		// check names in other transactions
		assertEquals("Check file name for preedit after saving", oldName, ((ContentFile) preEdit.getObject(File.class, CONTENTFILE_ID)).getName());
		assertEquals("Check file name for concurrent after saving", oldName, ((ContentFile) concurrent.getObject(File.class, CONTENTFILE_ID)).getName());

		// commit the edit transaction
		edit.commit();

		// start a final transaction
		Transaction postEdit = testContext.startTransactionWithPermissions(false);

		// check names in other transactions
		assertEquals("Check file name for preedit after commit", oldName, ((ContentFile) preEdit.getObject(File.class, CONTENTFILE_ID)).getName());
		assertEquals("Check file name for concurrent after commit", oldName, ((ContentFile) concurrent.getObject(File.class, CONTENTFILE_ID)).getName());
		assertEquals("Check file name for postedit after commit", newName, ((ContentFile) postEdit.getObject(File.class, CONTENTFILE_ID)).getName());
		assertEquals("Check file name for startFirstFetchLater after commit", newName,
				((ContentFile) startFirstFetchLater.getObject(File.class, CONTENTFILE_ID)).getName());

		startFirstFetchLater.commit();
		preEdit.commit();
		concurrent.commit();
		postEdit.commit();
	}

	/**
	 * Tests the creation of a file with the same name and same folder id as an existing file
	 * @throws Exception
	 */
	@Test
	public void testCreateNewTwice() throws Exception {

		String fileName = "newFile.jpg";
		String fileDescription = "Some new file";
		String fileType = "application/octet-stream";
		String fileContent = "HalloWelt";
		int fileSize = 9999;

		Transaction t = testContext.startTransactionWithPermissions(true); // Phase #1 - Create a new file
		{

			File newFile = (File) t.createObject(File.class);

			newFile.setName(fileName);
			newFile.setFiletype(fileType);
			newFile.setFilesize(fileSize);
			newFile.setDescription(fileDescription);
			newFile.setFileStream(generateDataFile(fileContent));
			newFile.setFolderId(FOLDER_ID);
			assertNull("Check whether the new file has no id", newFile.getId());

			// save the file
			newFile.save();
			t.commit();
		} // Phase #2 - Create the second file with the same specs as the first one
		{
			t = testContext.startTransactionWithPermissions(false);
			File newFile = (File) t.createObject(File.class);

			newFile.setName(fileName);
			newFile.setFiletype(fileType);
			newFile.setFilesize(fileSize);
			newFile.setDescription(fileDescription);
			newFile.setFileStream(generateDataFile(fileContent));
			newFile.setFolderId(FOLDER_ID);
			assertNull("Check whether the new file has no id", newFile.getId());
			String oldFileName = newFile.getName();
			String oldFileExtension = newFile.getExtension();
			assertEquals("Check if the file extension is correct", oldFileExtension, "jpg");

			newFile.save();
			t.commit();
			String newFileName = newFile.getName();
			String newFileExtension = newFile.getExtension();

			assertFalse("Both names should be different.", newFileName.equals(oldFileName));
			assertTrue("Both file extensions should be the same.", newFileExtension.equals(oldFileExtension) );

		}

	}

	/**
	 * Test creation of files with filenames that only differ in case
	 * @throws Exception
	 */
	@Test
	public void testCreateCaseSensitive() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);

		File file1 = t.createObject(File.class);
		file1.setFolderId(FOLDER_ID);
		file1.setName("file.bin");
		file1.setFileStream(new ByteArrayInputStream("content".getBytes()));
		file1.save();
		t.commit(false);

		File file2 = t.createObject(File.class);
		file2.setFolderId(FOLDER_ID);
		file2.setName("FILE.BIN");
		file2.setFileStream(new ByteArrayInputStream("CONTENT".getBytes()));
		file2.save();
		t.commit(false);

		file1 = t.getObject(File.class, file1.getId());
		file2 = t.getObject(File.class, file2.getId());

		assertFalse("Filenames of " + file1 + " and " + file2 + " must differ in more than just the case", file1.getName().equalsIgnoreCase(file2.getName()));
	}

	/**
	 * Test the update behavior of a file.
	 * @throws Exception
	 */
	@Test
	public void testCreateAndUpdate() throws Exception {

		String fileName = "newFile.jpg";
		String fileDescription = "Some new file";
		String fileType = "application/octet-stream";
		String fileHash = "c391cdd78c2d778f302b34d5539a08e0";
		String fileContent = "HalloWelt";
		int fileSize = 9999;
		int newFileId = -1;

		testContext.startTransactionWithPermissions(true);
		Transaction t = TransactionManager.getCurrentTransaction();
		// get the transaction timestamp (which will be used as edate)
		int transactionTimestamp = t.getUnixTimestamp(); // Phase #1 - Create a new file
		{

			File newFile = (File) t.createObject(File.class);

			newFile.setName(fileName);
			newFile.setFiletype(fileType);
			newFile.setFilesize(fileSize);
			newFile.setDescription(fileDescription);
			newFile.setFileStream(generateDataFile(fileContent));
			newFile.setFolderId(FOLDER_ID);
			assertNull("Check whether the new file has no id", newFile.getId());

			// save the file
			newFile.save();
			t.commit();

			newFileId = ObjectTransformer.getInt(newFile.getId(), 0);

			// // Check dirtqueue
			// ResultSet res = dbUtils.executeQuery("SELECT * FROM dirtqueue where sid = '" + t.getSessionId() +"'" );
			// if(res.next()){
			// assertEquals("Dirtqueue obj_type does not match.",Folder.TYPE_FOLDER, res.getInt("obj_type"));
			// assertEquals("Dirtqueue property does not match.", "files,images", res.getString("property"));
			// assertEquals("Dirtqueue events does not match.", 2, res.getInt("events"));
			// assertEquals("Dirtqueue obj_id does not match.", FOLDER_ID, res.getInt("obj_id"));
			// }
			// else {
			// fail("Could not find the needed dirtqueue entry within database");
			// }
			//
			// if(res.next()){
			// assertEquals("Dirtqueue obj_type does not match.",File.TYPE_FILE, res.getInt("obj_type"));
			// assertEquals("Dirtqueue property does not match.", null , res.getObject("property"));
			// assertEquals("Dirtqueue events does not match.", 1, res.getInt("events"));
			// assertEquals("Dirtqueue obj_id does not match.", newFileId, res.getInt("obj_id"));
			// }
			// else {
			// fail("Could not find the needed dirtqueue entry within database");
			// }
			// //res.close();

			// Check logcmd
			ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM logcmd where user_id = " + DBTestContext.USER_WITH_PERMS);

			if (res.next()) {
				assertEquals("Logcmd timestamp does not match.", transactionTimestamp, res.getInt("timestamp"));
				assertEquals("Logcmd cmd_desc_id does not match.", 338, res.getInt("cmd_desc_id"));
				assertEquals("Logcmd o_type does not match.", File.TYPE_FILE, res.getInt("o_type"));
				assertEquals("Logcmd o_id does not match.", newFileId, res.getInt("o_id"));
				assertEquals("Logcmd o_id2 does not match.", FOLDER_ID, res.getInt("o_id2"));
				assertEquals("Logcmd user_id does not match.", DBTestContext.USER_WITH_PERMS.intValue(), res.getInt("user_id"));
				assertEquals("Logcmd info does not match.", "cmd_file_create-java", res.getString("info"));
			} else {
				fail("Could not find the needed Logcmd entry within database");
			}

			if (res.next()) {
				assertEquals("Logcmd timestamp does not match.", transactionTimestamp, res.getInt("timestamp"));
				assertEquals("Logcmd cmd_desc_id does not match.", 339, res.getInt("cmd_desc_id"));
				assertEquals("Logcmd o_type does not match.", File.TYPE_FILE, res.getInt("o_type"));
				assertEquals("Logcmd o_id does not match.", newFileId, res.getInt("o_id"));
				assertEquals("Logcmd o_id2 does not match.", FOLDER_ID, res.getInt("o_id2"));
				assertEquals("Logcmd user_id does not match.", DBTestContext.USER_WITH_PERMS.intValue(), res.getInt("user_id"));
				assertEquals("Logcmd info does not match.", "cmd_file_data-java", res.getString("info"));
			} else {
				fail("Could not find the needed Logcmd entry within database");
			}
			res.close();

		} // Phase #2 - Get the stored file and update it
		{
			t = testContext.startTransactionWithPermissions(false);
			transactionTimestamp = t.getUnixTimestamp();
			// get the file for editing
			File file = (ContentFile) t.getObject(File.class, newFileId, true);

			System.out.println(convertStreamToString(file.getFileStream()));

			file.setFileStream(generateDataFile(fileContent + "CHANGED"));

			// save the file
			file.save();
			t.commit();

			assertTrue("Check whether the new file has a file id after saving", newFileId != 0);

			ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM logcmd where user_id = " + testContext.USER_WITH_PERMS + " limit 2,2");

			if (res.next()) {
				assertEquals("Logcmd timestamp does not match.", transactionTimestamp, res.getInt("timestamp"));
				assertEquals("Logcmd cmd_desc_id does not match.", 339, res.getInt("cmd_desc_id"));
				assertEquals("Logcmd o_type does not match.", File.TYPE_FILE, res.getInt("o_type"));
				assertEquals("Logcmd o_id does not match.", newFileId, res.getInt("o_id"));
				assertEquals("Logcmd o_id2 does not match.", FOLDER_ID, res.getInt("o_id2"));
				assertEquals("Logcmd user_id does not match.", DBTestContext.USER_WITH_PERMS.intValue(), res.getInt("user_id"));
				assertEquals("Logcmd info does not match.", "cmd_file_data-java", res.getString("info"));
			} else {
				fail("Could not find the needed Logcmd entry within database");
			}

			if (res.next()) {
				assertEquals("Logcmd timestamp does not match.", transactionTimestamp, res.getInt("timestamp"));
				assertEquals("Logcmd cmd_desc_id does not match.", 339, res.getInt("cmd_desc_id"));
				assertEquals("Logcmd o_type does not match.", File.TYPE_FILE, res.getInt("o_type"));
				assertEquals("Logcmd o_id does not match.", newFileId, res.getInt("o_id"));
				assertEquals("Logcmd o_id2 does not match.", FOLDER_ID, res.getInt("o_id2"));
				assertEquals("Logcmd user_id does not match.", DBTestContext.USER_WITH_PERMS.intValue(), res.getInt("user_id"));
				assertEquals("Logcmd info does not match.", "cmd_file_update-java", res.getString("info"));
			} else {
				fail("Could not find the needed Logcmd entry within database");
			}
			res.close();

		} // Phase #3 - Get the file again and check its content
		{
			t = testContext.startTransactionWithPermissions(false);
			File file = (File) t.getObject(File.class, newFileId);

			System.out.println("Reading contents of: " + newFileId);
			String liveFileContent = convertStreamToString(file.getFileStream());

			assertEquals("Changed file content does not match.", fileContent + "CHANGED", liveFileContent);
		}

	}

	/**
	 * Test creation of a new file
	 * @throws Exception
	 */
	@Test
	public void testCreateNewFile() throws Exception {
		testContext.startTransactionWithPermissions(true);

		String fileName = "newFile.jpg";
		String fileDescription = "Some new file";
		String fileHash = "476a5533998c2b31c81c2d56a25b83a7";
		String fileType = "application/octet-stream";
		String fileContent = "HalloWelt";
		int fileSize = 9999;

		Transaction t = TransactionManager.getCurrentTransaction();
		File newFile = (File) t.createObject(File.class);
		int transactionTimestamp = t.getUnixTimestamp();

		// set some attributes
		newFile.setName(fileName);
		newFile.setFiletype(fileType);
		newFile.setFilesize(fileSize);
		newFile.setDescription(fileDescription);
		newFile.setFileStream(generateDataFile(fileContent));
		newFile.setFolderId(FOLDER_ID);

		assertNull("Check whether the new file has no id", newFile.getId());

		// save the file
		newFile.save();
		t.commit();

		t = testContext.startTransactionWithPermissions(false);

		int newFileId = ObjectTransformer.getInt(newFile.getId(), 0);

		assertTrue("Check whether the new file has a file id after saving", newFileId != 0);

		// now load the file
		File file = (File) t.getObject(File.class, newFileId);

		assertNotNull("Check that the file now really exists", file);
		assertEquals("The md5 of the file does not match.", fileHash, file.getMd5());
		assertEquals("The description of the file does not match", fileDescription, file.getDescription());

		// check whether the edate and editor have been set
		assertEquals("Check editor id of the file", DBTestContext.USER_WITH_PERMS, file.getEditor().getId());
		assertEquals("Check edate of the file", transactionTimestamp, file.getEDate().getIntTimestamp());
		assertEquals("Check creator id of the file", DBTestContext.USER_WITH_PERMS, file.getCreator().getId());

		// check existence of file
		ResultSet res = testContext.getDBSQLUtils().executeQuery("SELECT * FROM contentfile where id = " + newFileId);

		if (res.next()) {
			assertNotNull("Check id in the database", res.getObject("id"));
			assertNotNull("Check md5 in the database", res.getObject("md5"));
		} else {
			fail("Could not find complete file data in the database");
		}

		// check that the channelset_id of the file was set
		assertTrue("Channelset ID must be set", ObjectTransformer.getInt(file.getChannelSetId(), 0) != 0);
	}

	/**
	 * Tests filename sanitize.
	 * @throws Exception
	 */
	@Test
	public void testSanitizeName() throws Exception {
		File file = createAndSaveFile("äüï.jpg");
		assertEquals("aeuei.jpg", file.getName());
	}

	/**
	 * Tests leading/trailing spaces removal.
	 * @throws Exception
	 */
	@Test
	public void testLeadingTrailingSpaces() throws Exception {
		File file = createAndSaveFile("  leadingtrailing  .jpg  ");
		assertEquals("leadingtrailing-.jpg", file.getName());
	}

	/**
	 * Creates and saves a file.
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	private File createAndSaveFile(String fileName) throws Exception {
		testContext.startTransactionWithPermissions(true);

		String fileDescription = "Some new file";
		String fileType = "application/octet-stream";
		String fileContent = "HalloWelt";
		int fileSize = 9999;

		Transaction t = TransactionManager.getCurrentTransaction();
		File newFile = (File) t.createObject(File.class);

		// set some attributes
		newFile.setName(fileName);
		newFile.setFiletype(fileType);
		newFile.setFilesize(fileSize);
		newFile.setDescription(fileDescription);
		newFile.setFileStream(generateDataFile(fileContent));
		newFile.setFolderId(FOLDER_ID);

		// save the file
		newFile.save();
		t.commit();

		t = testContext.startTransactionWithPermissions(false);

		int newFileId = ObjectTransformer.getInt(newFile.getId(), 0);

		// now load the file
		return t.getObject(File.class, newFileId);
	}

	/**
	 * Test creation of a new file
	 * @throws Exception
	 */
	@Test
	public void testCreateNewFileSizeLimit() throws Exception {
		testContext.startTransactionWithPermissions(true);

		String fileName = "newFile.jpg";
		String fileDescription = "Some new file";
		String fileType = "application/octet-stream";

		Transaction t = TransactionManager.getCurrentTransaction();
		File newFile = (File) t.createObject(File.class);

		// set some attributes
		newFile.setName(fileName);
		newFile.setFiletype(fileType);
		newFile.setDescription(fileDescription);
		newFile.setFileStream(generateDataFile(1025));
		newFile.setFolderId(FOLDER_ID);

		assertNull("Check whether the new file has no id", newFile.getId());

		// save the file
		try {
			newFile.save();
		} catch (NodeException e) {
			I18nString  i18nMessage = new CNI18nString("rest.file.upload.limit_reached");

			i18nMessage.setParameter("0", "1 MB");
			i18nMessage.setParameter("1", "1 KB");
			assertEquals(i18nMessage.toString(), e.getMessage());
			return;
		}
		fail("This test should fail with a specific exception.");
	}

	/**
	 * Test disable max filesize for specific groups
	 * @throws Exception
	 */
	@Test
	public void testOmitNewFileSizeLimit() throws Exception {
		int nodeUserId = 3;
		int editorUserId = 26;
		String nodeGroupName = "Node Super Admin";

		try (Trx trx = new Trx()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			SystemUser nodeUser = t.getObject(SystemUser.class, nodeUserId);
			assertNotNull("Node User must exist", nodeUser);

			assertTrue("Node User must be member of group '" + nodeGroupName + "'",
					nodeUser.getUserGroups().stream().anyMatch(g -> nodeGroupName.equals(g.getName())));

			SystemUser editorUser = t.getObject(SystemUser.class, editorUserId);
			assertNotNull("Editor User must exist", editorUser);

			assertFalse("Editor User must not be member of group '" + nodeGroupName + "'",
					editorUser.getUserGroups().stream().anyMatch(g -> nodeGroupName.equals(g.getName())));

			trx.success();
		}

		try (Trx trx = new Trx(null, nodeUserId);
				PropertyTrx propTrx = new PropertyTrx("no_max_filesize", new String[] { nodeGroupName })) {
			Transaction t = TransactionManager.getCurrentTransaction();
			File newFile = t.createObject(File.class);
			newFile.setName("newfile.bin");
			newFile.setFileStream(generateDataFile(1025));
			newFile.setFolderId(FOLDER_ID);
			newFile.save();

			trx.success();
		}

		try (Trx trx = new Trx(null, editorUserId);
				PropertyTrx propTrx = new PropertyTrx("no_max_filesize", new String[] { nodeGroupName })) {
			Transaction t = TransactionManager.getCurrentTransaction();
			File newFile = t.createObject(File.class);
			newFile.setName("newfile.bin");
			newFile.setFileStream(generateDataFile(1025));
			newFile.setFolderId(FOLDER_ID);
			try {
				newFile.save();
				fail("FileSizeException should have been thrown");
			} catch(FileSizeException e) {
			}

			trx.success();
		}
	}

	/**
	 * Test if the file extension is empty if the file has no extension in its name
	 * @throws NodeException
	 */
	@Test
	public void testEmptyExtension() throws NodeException{
		String fileName = "myfile";

		Transaction t = TransactionManager.getCurrentTransaction();
		File newFile = (File) t.createObject(File.class);

		newFile.setName(fileName);
		String extension = newFile.getExtension();
		assertEquals("Extension should be empty","",extension);
	}

	/**
	 * Test overwriting file with the same file name in a specific order with the create method
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testMultiPartCreateOverwrite() throws Exception {
		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(), true)) {
			FileResourceImpl fileResource = new FileResourceImpl();

			Node node = ContentNodeTestDataUtils.createNode();
			Folder folder = node.getFolder();

			Integer fileId = null;
			Integer newFileId = null;
			MultiPart multiPart = null;
			FileUploadResponse fileUploadResponse = null;

			try {
				multiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart(
						"blah.txt", folder.getId(), node.getId(), "", true, "testcontent");
				fileUploadResponse = fileResource.create(multiPart);
				ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
				fileId = fileUploadResponse.getFile().getId();
			} finally {
				if (multiPart != null) {
					multiPart.cleanup();
				}
			}

			try {
				multiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart(
						"blah.txt", folder.getId(), node.getId(), "", true, "testcontent2");
				fileUploadResponse = fileResource.create(multiPart);;
				ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
				newFileId = fileUploadResponse.getFile().getId();

				assertEquals("The file ID of the new uploaded file must match the first", fileId, newFileId);
			} finally {
				if (multiPart != null) {
					multiPart.cleanup();
				}
			}


			try {
				// Upload the file with the same folder ID and name again and create a new one this time
				multiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart(
						"blah.txt", folder.getId(), node.getId(), "", false, "testcontent2");
				fileUploadResponse = fileResource.create(multiPart);;
				ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
				newFileId = fileUploadResponse.getFile().getId();

				assertNotEquals("The file ID of the new uploaded file must not match the first", fileId, newFileId);
			} finally {
				if (multiPart != null) {
					multiPart.cleanup();
				}
			}

			try {
				// Upload the file without specifying whether to overwrite, should also create a new file
				multiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart(
						"blah.txt", folder.getId(), node.getId(), "", null, "testcontent2");
				fileUploadResponse = fileResource.create(multiPart);;
				ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
				newFileId = fileUploadResponse.getFile().getId();

				assertNotEquals("The file ID of the new uploaded file must not match the first", fileId, newFileId);
			} finally {
				if (multiPart != null) {
					multiPart.cleanup();
				}
			}
		}
	}

	/**
	 * Test overwriting a file with the same file name in a specific order with the createSimple method
	 * @throws Exception
	 */
	@Test
	public void testMultiPartCreateSimpleOverwrite() throws Exception {
		HttpServletRequestWrapper httpServletRequest;
		FileUploadResponse fileUploadResponse;
		Node node;
		Folder folder;
		Integer fileId;

		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(), true)) {
			FileResourceImpl fileResource = new FileResourceImpl();

			node = ContentNodeTestDataUtils.createNode();
			folder = node.getFolder();

			httpServletRequest = mock(HttpServletRequestWrapper.class);
			when(httpServletRequest.getInputStream()).thenReturn(StringtoServletInputStream("content"));

			fileUploadResponse = fileResource.createSimple(httpServletRequest, folder.getId(), node.getId(), "binary", "blah.txt", "", true);
			ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
			fileId = fileUploadResponse.getFile().getId();
		}

		try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(), true)) {
			FileResourceImpl fileResource = new FileResourceImpl();

			// Upload the file iwth the same folder ID and name again to replace it
			when(httpServletRequest.getInputStream()).thenReturn(StringtoServletInputStream("content2"));
			fileUploadResponse = fileResource.createSimple(httpServletRequest, folder.getId(), node.getId(), "binary", "blah.txt", "", true);
			ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
			Integer newFileId = fileUploadResponse.getFile().getId();

			assertEquals("The file ID of the new uploaded file must match the first", fileId, newFileId);

			// Upload the file iwth the same folder ID and name again and create a new one this time
			when(httpServletRequest.getInputStream()).thenReturn(StringtoServletInputStream("content3"));
			fileUploadResponse = fileResource.createSimple(
					httpServletRequest, folder.getId(), node.getId(), "binary", "blah.txt", "", false);
			ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
			newFileId = fileUploadResponse.getFile().getId();

			assertNotEquals("The file ID of the new uploaded file must not match the first", fileId, newFileId);
		}
	}

	/**
	 * Test creating 10 files with the same filename in parallel. The rest endpoint should create a lock
	 * on the filename and ensure that the file names for each file are unique.
	 * @throws Exception
	 */
	@Test
	public void testMultiPartCreateConcurrency() throws Exception {
		Node node = null;
		Folder folder = null;

		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode();
			folder = node.getFolder();
			trx.success();
		}

		ExecutorService service = Executors.newCachedThreadPool();

		List<Callable<FileUploadResponse>> callables = new ArrayList<Callable<FileUploadResponse>>();

		for (int i = 0; i < 10; i++) {
			callables.add(new TestMultiPartCreateConcurrency(folder));
		}

		List<Future<FileUploadResponse>> fileUploadResponseFutures = service.invokeAll(callables);

		List<String> filenames = new ArrayList<String>();
		for (Future<FileUploadResponse> fileUploadResponseFuture : fileUploadResponseFutures) {
			FileUploadResponse fileUploadResponse = fileUploadResponseFuture.get();
			ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);

			filenames.add(fileUploadResponse.getFile().getName());
		}

		List<String> duplicates = filenames.stream().distinct().filter(
				entry -> Collections.frequency(filenames, entry) > 1).collect(Collectors.toList());
		assertEquals("There must not be any duplicate filenames", 0, duplicates.size());
	}

	/**
	 * The Callable that creates a file in a new transaction for the
	 * test testMultiPartCreateConcurrency
	 */
	public static class TestMultiPartCreateConcurrency
			implements Callable<FileUploadResponse> {
		/**
		 * The folder where to create the file in
		 */
		private Folder folder;

		/**
		 * @param folder The folder where to create the file in
		 */
		public TestMultiPartCreateConcurrency(Folder folder) {
			this.folder = folder;
		}

		/**
		 * Invoke the file creation thread
		 */
		public FileUploadResponse call() throws Exception {
			MultiPart multiPart = null;
			FileUploadResponse fileUploadResponse = null;

			try (Trx trx = new Trx(ContentNodeTestDataUtils.createSession(), true)) {
				FileResourceImpl fileResource = new FileResourceImpl();
				fileResource.setTransaction(trx.getTransaction());

				try {
					Random rand = new Random();
					String filename = "abcdefghijklmnopqrstuvxyzabcdefghijklmnopqrstuvxyzabcdefghijklmnopqrstuvxyz.txt";
					multiPart = ContentNodeTestDataUtils.createRestFileUploadMultiPart(
							filename, folder.getId(), folder.getNode().getId(), "", false, "testcontent");
					fileUploadResponse = fileResource.create(multiPart);
					ContentNodeTestUtils.assertResponseCodeOk(fileUploadResponse);
				} finally {
					if (multiPart != null) {
						multiPart.cleanup();
					}
				}

				trx.success();
			}

			return fileUploadResponse;
		}
	}

	/**
	 * Converts a stringt to a ServletInputStream
	 * @param value A string
	 * @return A ServletInputStream
	 * @throws IOException
	 */
	public ServletInputStream StringtoServletInputStream(String value) throws IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
		ServletInputStream servletInputStream=new ServletInputStream(){
			public int read() throws IOException {
				return byteArrayInputStream.read();
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
			}
		};
		byteArrayInputStream.close();

		return servletInputStream;
	}
}
