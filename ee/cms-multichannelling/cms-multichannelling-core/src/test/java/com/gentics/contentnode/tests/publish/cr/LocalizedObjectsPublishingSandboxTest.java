package com.gentics.contentnode.tests.publish.cr;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.testutils.GCNFeature;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for the publish process with localized objects
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MCCR})
public class LocalizedObjectsPublishingSandboxTest extends AbstractLocalizedObjectsPublishingTest {

	/**
	 * Object type setting
	 */
	protected ObjectType objectType;

	/**
	 * True for instant publishing, false if not
	 */
	protected boolean instant;

	/**
	 * Template
	 */
	protected Template template;

	/**
	 * Get the test parameters
	 * 
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: objectType {0}, instant {1}")
	public static Collection<Object[]> data() {
		return getTestParameters();
	}

	@Before
	public void setUp() throws Exception {
		// activate the feature instant publishing
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("instant_cr_publishing", true);
		// deactivate the GIS
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("tag_image_resizer", false);
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("multichannelling", true);
		setUpTestData(true, false);

		template = ContentNodeTestDataUtils.createTemplate(master.getFolder(), "Template");
	}

	/**
	 * Create a parameterized instance of the test case
	 * 
	 * @param objectType
	 *            {@link ObjectType} settings
	 * @param instant
	 *            true for instant publishing
	 */
	public LocalizedObjectsPublishingSandboxTest(ObjectType objectType, boolean instant) {
		this.objectType = objectType;
		this.instant = instant;
	}

	/**
	 * Publishing test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Transaction t = null;
		setInstant(instant);
		Node master = getMaster();
		Node channel = getChannel();

		switch (objectType) {
		case PAGE:
			// Create objects
			Page page = createPage();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertPageInContentrepository(master, page, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertPageInContentrepository(channel, page, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));

			// Localize the page
			Page localizedPage = localizePage(channel, page);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertPageInContentrepository(master, page, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertPageInContentrepository(channel, page, false);
			assertPageInContentrepository(master, localizedPage, false);
			assertPageInContentrepository(channel, localizedPage, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Change localized page name (and content which is based on the
			// name)
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			localizedPage = t.getObject(Page.class, page.getId(), true);

			localizedPage.setName("page_different_name");
			localizedPage.save();
			localizedPage.publish();
			t.commit(false);

			runPublishProcess();

			assertPageInContentrepository(master, page, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertPageInContentrepository(channel, page, false);
			assertPageInContentrepository(master, localizedPage, false);
			assertPageInContentrepository(channel, localizedPage, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Unlocalize the object
			unlocalizePage(localizedPage);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertPageInContentrepository(master, page, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertPageInContentrepository(channel, page, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));
			assertPageInContentrepository(master, localizedPage, false);
			assertPageInContentrepository(channel, localizedPage, false);

			break;

		case FILE:
			File file = createFile();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, file, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));

			// Localize the file
			File localizedFile = localizeFile(channel, file);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, file, false);
			assertFileInContentrepository(master, localizedFile, false);
			assertFileInContentrepository(channel, localizedFile, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Change localized file name and binary content
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			localizedFile = t.getObject(File.class, file.getId(), true);

			localizedFile.setName("file_different_name.txt");
			localizedFile.setFileStream(new ByteArrayInputStream("This is different file content".getBytes("UTF-8")));
			localizedFile.save();
			t.commit(false);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, file, false);
			assertFileInContentrepository(master, localizedFile, false);
			assertFileInContentrepository(channel, localizedFile, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Unlocalize the object
			unlocalizeFile(localizedFile);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, file, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, file, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));
			assertFileInContentrepository(master, localizedFile, false);
			assertFileInContentrepository(channel, localizedFile, false);

			break;

		case IMAGE:
			ImageFile image = createImage();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, image, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));

			// Localize the image
			ImageFile localizedImage = localizeImage(channel, image);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, image, false);
			assertFileInContentrepository(master, localizedImage, false);
			assertFileInContentrepository(channel, localizedImage, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Change localized image name and binary content
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			localizedImage = t.getObject(ImageFile.class, image.getId(), true);

			localizedImage.setName("image_ifferent_name.jpg");
			localizedImage.setFileStream(GenericTestUtils.getPictureResource("bild2.jpg"));
			localizedImage.save();
			t.commit(false);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, image, false);
			assertFileInContentrepository(master, localizedImage, false);
			assertFileInContentrepository(channel, localizedImage, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Unlocalize the object
			unlocalizeImage(localizedImage);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFileInContentrepository(master, image, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFileInContentrepository(channel, image, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));
			assertFileInContentrepository(master, localizedImage, false);
			assertFileInContentrepository(channel, localizedImage, false);

			break;

		case FOLDER:
			Folder folder = createFolder();

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFolderInContentrepository(channel, folder, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));

			// Localize the folder
			Folder localizedFolder = localizeFolder(channel, folder);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFolderInContentrepository(channel, folder, false);
			assertFolderInContentrepository(master, localizedFolder, false);
			assertFolderInContentrepository(channel, localizedFolder, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Change localized folder name
			t = TransactionManager.getCurrentTransaction();
			t.setChannelId(channel.getId());
			localizedFolder = t.getObject(Folder.class, folder.getId(), true);

			localizedFolder.setName("folder_different_name");
			localizedFolder.save();
			t.commit(false);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFolderInContentrepository(channel, folder, false);
			assertFolderInContentrepository(master, localizedFolder, false);
			assertFolderInContentrepository(channel, localizedFolder, true, CRAttribute.get("ismaster", 0), CRAttribute.get("inherited", 0));

			// Unlocalize the object
			unlocalizeFolder(localizedFolder);

			runPublishProcess();

			// Check if object exists in the master and channel
			assertFolderInContentrepository(master, folder, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 0));
			assertFolderInContentrepository(channel, folder, true, CRAttribute.get("ismaster", 1), CRAttribute.get("inherited", 1));
			assertFolderInContentrepository(master, localizedFolder, false);
			assertFolderInContentrepository(channel, localizedFolder, false);

			break;
		default:
			break;
		}
	}

	/**
	 * Publishing test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishAfterLocalizeMotherFolder() throws Exception {
		Node channel = getChannel();
		CNDatasource ds = datasources.get(ObjectTransformer.getInt(channel.getId(), 0));

		// Create a folder in the master node
		Folder folder = createFolder();

		// Create the object
		NodeObject subObject;
		int ttype = 0;
		switch (objectType) {
		case FOLDER:
			subObject = ContentNodeTestDataUtils.createFolder(folder, "Subfolder");
			ttype = Folder.TYPE_FOLDER;
			break;
		case PAGE:
			subObject = update(ContentNodeTestDataUtils.createPage(folder, template, "Page"), p -> p.publish());
			ttype = Page.TYPE_PAGE;
			break;
		case FILE:
			subObject = ContentNodeTestDataUtils.createFile(folder, "testfile.txt", "Testfile Contents".getBytes());
			ttype = File.TYPE_FILE;
			break;
		case IMAGE:
			try (InputStream inputStream = GenericTestUtils.getPictureResource("blume.jpg")) {
				byte[] data = IOUtils.toByteArray(inputStream);
				subObject = ContentNodeTestDataUtils.createImage(folder, "blume.jpg", data);
			}
			ttype = File.TYPE_FILE;
			break;
		default:
			fail(String.format("Cannot test type %s", objectType));
			return;
		}

		// Run the publish run
		runPublishProcess();

		Resolvable crSubObject = PortalConnectorFactory.getContentObject(String.format("%d.%d", ttype, subObject.getId()), ds);
		assertNotNull(String.format("%s must exist in CR", subObject), crSubObject);

		assertTrue("Subobject must have the master folder as mother folder", crSubObject.getProperty("mother_obj_id").equals(folder.getId()));

		// Localize the mother in the channel
		Folder localizedFolder = localizeFolder(channel, folder);

		// Run the publish run again
		runPublishProcess();

		crSubObject = PortalConnectorFactory.getContentObject(String.format("%d.%d", ttype, subObject.getId()), ds);
		assertNotNull(String.format("%s must exist in CR", subObject), crSubObject);

		// The mother of the subobject should now be the localized folder in the
		// channel
		assertTrue("Subobject must have the localized channel folder as mother folder",
				crSubObject.getProperty("mother_obj_id").equals(localizedFolder.getId()));
	}
}
