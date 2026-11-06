package com.gentics.contentnode.tests.publish.mccr;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for the publish process with localized objects
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = {Feature.MULTICHANNELLING, Feature.INSTANT_CR_PUBLISHING, Feature.MCCR}, unset = {Feature.TAG_IMAGE_RESIZER})
public class LocalizedObjectsPublishingSandboxTest extends AbstractLocalizedObjectsPublishingTest {

	/**
	 * Object type setting
	 */
	protected ObjectType objectType;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: objectType {0}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}


	@Before 
	public void setUp() throws Exception {
		setUpTestData(false, false);
	}

	/**
	 * Create a parameterized instance of the test case
	 * 
	 * @param objectType
	 *            {@link ObjectType} settings
	 */
	public LocalizedObjectsPublishingSandboxTest(ObjectType objectType) {
		this.objectType = objectType;
	}

	/**
	 * Publishing test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Transaction t = null;
		Node master = getMaster();
		Node channel = getChannel();

		switch (objectType) {
		case PAGE:
			// Create objects
			Page page = createPage();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertPageInContentrepository(master, page, true);
			assertPageInContentrepository(channel, page, true);

			// Check if the objects have different content IDs and attributes
			assertPageEquality(page, false, false);

			// Localize the page
			ContentNodeTestDataUtils.localizePage(channel, page);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertPageInContentrepository(master, page, true);
			assertPageInContentrepository(channel, page, true);

			// Check if the objects have the same content IDs and different attributes
			assertPageEquality(page, true, false);

			// Change localized page name (and content which is based on the name)
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			Page localizedPage = t.getObject(Page.class, page.getId(), true);

			localizedPage.setName("page_different_name");
			localizedPage.save();
			localizedPage.publish();
			t.commit(false);

			runPublishProcess();

			// Check if the objects have the same content IDs and the same attributes
			assertPageEquality(page, true, true);

			// Unlocalize the object
			unlocalizePage(localizedPage);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertPageInContentrepository(master, page, true);
			assertPageInContentrepository(channel, page, true);

			// Check if the objects have different content IDs and attributes
			assertPageEquality(page, false, false);

			break;

		case FILE:
			File file = createFile();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true);
			assertFileInContentrepository(channel, file, true);

			// Check if the objects have different content IDs and attributes
			assertFileEquality(file, false, false);

			// Localize the file
			ContentNodeTestDataUtils.localizeFile(channel, file);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true);
			assertFileInContentrepository(channel, file, true);

			// Check if the objects have the same content IDs and different attributes
			assertFileEquality(file, true, false);

			// Change localized file name and binary content
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			File localizedFile = t.getObject(File.class, file.getId(), true);

			localizedFile.setName("file_different_name.txt");
			localizedFile.setFileStream(new ByteArrayInputStream("This is different file content".getBytes("UTF-8")));
			localizedFile.save();
			t.commit(false);

			runPublishProcess();

			// Check if the objects have the same content IDs and the same attributes
			assertFileEquality(file, true, true);

			// Unlocalize the object
			unlocalizeFile(localizedFile);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true);
			assertFileInContentrepository(channel, file, true);

			// Check if the objects have different content IDs and attributes
			assertFileEquality(file, false, false);
			break;

		case IMAGE:
			ImageFile image = createImage();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true);
			assertFileInContentrepository(channel, image, true);

			// Check if the objects have different content IDs and attributes
			assertFileEquality(image, false, false);

			// Localize the image
			ContentNodeTestDataUtils.localizeImage(channel, image);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true);
			assertFileInContentrepository(channel, image, true);

			// Check if the objects have the same content IDs and different attributes
			assertFileEquality(image, true, false);

			// Change localized image name and binary content
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			ImageFile localizedImage = t.getObject(ImageFile.class, image.getId(), true);

			localizedImage.setName("image_ifferent_name.jpg");
			localizedImage.setFileStream(GenericTestUtils.getPictureResource("bild2.jpg"));
			localizedImage.save();
			t.commit(false);

			runPublishProcess();

			// Check if the objects have the same content IDs and the same attributes
			assertFileEquality(image, true, true);

			// Unlocalize the object
			unlocalizeImage(localizedImage);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true);
			assertFileInContentrepository(channel, image, true);

			// Check if the objects have different content IDs and attributes
			assertFileEquality(image, false, false);
			break;

		case FOLDER:
			Folder folder = createFolder();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true);
			assertFolderInContentrepository(channel, folder, true);

			// Check if the objects have different content IDs and attributes
			assertFolderEquality(folder, false, false);

			// Localize the folder
			ContentNodeTestDataUtils.localizeFolder(channel, folder);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true);
			assertFolderInContentrepository(channel, folder, true);

			// Check if the objects have the same content IDs and different attributes
			assertFolderEquality(folder, true, false);

			// Change localized folder name
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			Folder localizedFolder = t.getObject(Folder.class, folder.getId(), true);

			localizedFolder.setName("folder_different_name");
			localizedFolder.save();
			t.commit(false);

			runPublishProcess();

			// Check if the objects have the same content IDs and the same attributes
			assertFolderEquality(folder, true, true);

			// Unlocalize the object
			unlocalizeFolder(localizedFolder);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true);
			assertFolderInContentrepository(channel, folder, true);

			// Check if the objects have different content IDs and attributes
			assertFolderEquality(folder, false, false);
			break;

		default:
			break;
		}
	}

}
