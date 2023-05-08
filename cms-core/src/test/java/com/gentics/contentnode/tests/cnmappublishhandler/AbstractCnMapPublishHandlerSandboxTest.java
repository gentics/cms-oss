package com.gentics.contentnode.tests.cnmappublishhandler;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.contentnode.publish.CnMapPublishHandler;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.TestAppender;
import com.gentics.lib.content.GenticsContentAttribute;
import com.gentics.lib.datasource.mccr.MCCRCacheHelper;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;

/**
 * Sandbox test for the CnMapPublishHandlers
 */
abstract public class AbstractCnMapPublishHandlerSandboxTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Flag whether to use an mccr
	 */
	protected boolean mccr;

	/**
	 * Flag whether to use instant publishing
	 */
	protected boolean instantPublishing;

	/**
	 * Flag whether to use differential delete
	 */
	protected boolean diffDelete;

	public TestAppender testAppender;

	protected Node node;

	protected MCCRDatasource mccrDs;

	protected Datasource ds;

	protected Template template;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	public static Collection<Object[]> data(boolean mccr) {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Boolean instantPublishing : Arrays.asList(true, false)) {
			for (Boolean diffDelete : Arrays.asList(true, false)) {
				data.add(new Object[] {mccr, instantPublishing, diffDelete});
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param mccr true for mccr
	 * @param instantPublishing true for instant publishing
	 * @param diffDelete true for differential delete
	 */
	public AbstractCnMapPublishHandlerSandboxTest(boolean mccr, boolean instantPublishing, boolean diffDelete) {
		this.mccr = mccr;
		this.instantPublishing = instantPublishing;
		this.diffDelete = diffDelete;
	}

	/**
	 * Restore the snapshot
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		// prepare the logger
		NodeLogger logger = NodeLogger.getNodeLogger(LogHandler.class.getPackage().getName());

		logger.setLevel(Level.INFO);
		logger.removeAllAppenders();
		testAppender = new TestAppender();
		logger.addAppender(testAppender);

		try (Trx trx = new Trx(null, 1)) {
			NodePreferences prefs = TransactionManager.getCurrentTransaction().getNodeConfig().getDefaultPreferences();
			prefs.setFeature("symlink_files", false);
			prefs.setFeature("instant_cr_publishing", true);
			prefs.setFeature("multichannelling", true);
			prefs.setFeature("mccr", true);

			node = createNode("bla", "Bla Node");
			mccrDs = node.getContentMap().getMCCRDatasource();
			ds = node.getContentMap().getDatasource();
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node page.name>", "Template using page");

			trx.success();
		}

		publish();
	}

	/**
	 * Destroy datasource
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (node != null) {
			try (Trx trx = new Trx()) {
				node.delete();
				trx.success();
			}
		}
		LanguageProviderFactory.reset();
		if (mccrDs != null) {
			MCCRCacheHelper.clear(mccrDs, true);
		}

		PortalConnectorFactory.destroy();
	}

	/**
	 * Test an empty publish process (whether the publish run is ok and the CnMapPublishHandler was used)
	 * @throws Exception
	 */
	@Test
	public void testEmptyPublish() throws Exception {
		// do a first publish run
		publish();

		cleanLogCmd();
		// no do an empty publish run
		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()				// sync object types
				.open(mccr).commit(mccr).close(mccr)	// sync channel structure
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single, new page
	 * @throws Exception
	 */
	@Test
	public void testCreatePage() throws Exception {
		// do a first publish run
		publish();

		cleanLogCmd();
		testAppender.reset();
		// create a new page
		Page page = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			page = t.getObject(Page.class, ContentNodeTestDataUtils.createPage(node.getFolder(), template, "New page").getId(), true);
			// publish the page
			page.publish();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().create(page).commit().close()	// write page to CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean mtPublishing = testContext.getContext().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTITHREADED_PUBLISHING);
			String expected = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr)							// sync channel structure
				.open().commit().close()										// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open(mtPublishing).commit(mtPublishing).close(mtPublishing)	// prepare publish data
				.open().update(page).commit().close()							// write page to CR
				.open().commit().close()										// Transactions for get/setLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().create(page).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single modified page
	 * @throws Exception
	 */
	@Test
	public void testModifyPage() throws Exception {
		// create a new page
		Page page = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			page = t.getObject(Page.class, ContentNodeTestDataUtils.createPage(node.getFolder(), template, "New page").getId(), true);
			page.publish();
			trx.success();
		}
		publish();

		cleanLogCmd();
		testAppender.reset();

		// modify the page
		try (Trx trx = new Trx(null, 1)) {
			page.setName("Modified page");
			page.save();
			page.publish();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().update(page).commit().close()	// write page to CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the page be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean mtPublishing = testContext.getContext().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTITHREADED_PUBLISHING);
			String expected = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr)							// sync channel structure
				.open(mtPublishing).commit(mtPublishing).close(mtPublishing)	// prepare publish data
				.open().commit().close()										// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().update(page).commit().close()							// write page to CR
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().update(page).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a removed page
	 * @throws Exception
	 */
	@Test
	public void testDeletePage() throws Exception {
		// create a new page
		Page page = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			page = t.getObject(Page.class, ContentNodeTestDataUtils.createPage(node.getFolder(), template, "New page").getId(), true);
			page.publish();
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// delete the page
		try (Trx trx = new Trx(null, 1)) {
			page.delete();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().delete(page).commit().close()	// remove page from CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the page be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean delete = diffDelete && !mccr;
			Result result = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr);							// sync channel structure

			if (!mccr) {
				result = result
					.open().commit().close()									// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			result = result
				.open(delete).delete(page, delete).commit(delete).close(delete)	// remove page from CR
				.open().commit().close();

			if (mccr) {
				result = result
					.open().commit().close()									// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			String expected = result.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().delete(page).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test taking a page offline
	 * @throws Exception
	 */
	@Test
	public void testTakePageOffline() throws Exception {
		// do a first publish run
		publish();

		cleanLogCmd();
		// create a new page
		Page page = null;

		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			page = t.getObject(Page.class, ContentNodeTestDataUtils.createPage(node.getFolder(), template, "New page").getId(), true);
			// publish the page
			page.publish();
			trx.success();
		}
		publish();

		cleanLogCmd();
		testAppender.reset();

		// take the page offline
		try (Trx trx = new Trx(null, 1)) {
			page.takeOffline();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().delete(page).commit().close()	// remove page from CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the page be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean delete = diffDelete && !mccr;
			Result result = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr);							// sync channel structure

			if (!mccr) {
				result = result
					.open().commit().close()									// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			result = result
				.open(delete).delete(page, delete).commit(delete).close(delete)	// delete page from CR
				.open().commit().close();

			if (mccr) {
				result = result
					.open().commit().close()									// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			String expected = result.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
					.open().delete(page).commit().close()
					.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single, new image
	 * @throws Exception
	 */
	@Test
	public void testCreateImage() throws Exception {
		// do a first publish run
		publish();

		cleanLogCmd();
		testAppender.reset();

		// create a new image
		ImageFile image = null;
		try (Trx trx = new Trx(null, 1)) {
			image = createImage(node.getFolder());
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().create(image).commit().close()	// write image into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()				// sync object types
				.open(mccr).commit(mccr).close(mccr)	// sync channel structure
				.open().commit().close()				// Transactions for getLastMapUpdate()
				.open().commit().close()
				.open().update(image).commit()			// write image into CR
				.close()
				.open().commit().close()				// Transactions for getLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().create(image).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single modified image
	 * @throws Exception
	 */
	@Test
	public void testModifyImage() throws Exception {
		// create a new image
		ImageFile image = null;
		try (Trx trx = new Trx(null, 1)) {
			image = createImage(node.getFolder());
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// modify the image
		try (Trx trx = new Trx(null, 1)) {
			image.setName("mod_" + image.getName());
			image.save();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().update(image).commit().close()	// write image into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the image be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()				// sync object types
				.open(mccr).commit(mccr).close(mccr)	// sync channel structure
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().update(image).commit().close()	// write image into CR
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
					.open().update(image).commit().close()
					.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a removed image
	 * @throws Exception
	 */
	@Test
	public void testDeleteImage() throws Exception {
		// create a new image
		ImageFile image = null;
		try (Trx trx = new Trx(null, 1)) {
			image = createImage(node.getFolder());
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// delete the image
		try (Trx trx = new Trx(null, 1)) {
			image.delete();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().delete(image).commit().close()	// delete image from CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and run the publish process again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean delete = diffDelete && !mccr;
			String expected = Result.instance()
				.open().commit().close()											// sync object types
				.open(mccr).commit(mccr).close(mccr)								// sync channel structure
				.open().commit().close()
				.open().commit().close()
				.open(delete).delete(image, delete).commit(delete).close(delete)	// delete image from CR
				.open().commit().close()											// Transactions for get/setLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().delete(image).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single, new file
	 * @throws Exception
	 */
	@Test
	public void testCreateFile() throws Exception {
		// do a first publish run
		publish();

		cleanLogCmd();
		testAppender.reset();

		// create a new file
		com.gentics.contentnode.object.File file = null;
		try (Trx trx = new Trx(null, 1)) {
			file = createFile(node.getFolder());
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().create(file).commit().close()	// write file into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()				// sync object types
				.open(mccr).commit(mccr).close(mccr)	// sync channel structure
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().update(file).commit().close()	// write file into CR
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().create(file).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single modified file
	 * @throws Exception
	 */
	@Test
	public void testModifyFile() throws Exception {
		// create a new file
		com.gentics.contentnode.object.File file = null;
		try (Trx trx = new Trx(null, 1)) {
			file = createFile(node.getFolder());
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// modify the file
		try (Trx trx = new Trx(null, 1)) {
			file.setName("mod_" + file.getName());
			file.save();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
					.open().update(file).commit().close()	// write file into CR
					.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the file be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()				// sync object types
				.open(mccr).commit(mccr).close(mccr)	// sync channel structure
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().update(file).commit().close()	// write file into CR
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().update(file).commit().close()	// write file into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a removed file
	 * @throws Exception
	 */
	@Test
	public void testDeleteFile() throws Exception {
		// create a new file
		com.gentics.contentnode.object.File file = null;
		try (Trx trx = new Trx(null, 1)) {
			file = createFile(node.getFolder());
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// delete the file
		try (Trx trx = new Trx(null, 1)) {
			file.delete();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().delete(file).commit().close()	// delete file from CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and run the publish process again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean delete = diffDelete && !mccr;
			Result result = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr);							// sync channel structure

			if (!mccr) {
				result = result
					.open().commit().close()									// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			result = result
				.open(delete).delete(file, delete).commit(delete).close(delete)	// delete file from CR
				.open().commit().close();										// Transactions for get/setLastMapUpdate()

			if (mccr) {
				result = result
					.open().commit().close()
					.open().commit().close();
			}

			String expected = result.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().delete(file).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single, new folder
	 * @throws Exception
	 */
	@Test
	public void testCreateFolder() throws Exception {
		// do a first publish run
		publish();

		cleanLogCmd();
		testAppender.reset();

		// create a new folder
		Folder folder = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createFolder(node.getFolder(), "New Folder").getId(), true);
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().create(folder).commit().close()	// write folder into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		testAppender.reset();

		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()					// sync object types
				.open(mccr).commit(mccr).close(mccr)		// sync channel structure
				.open().commit().close().open().commit().close()	// Transactions for getLastMapUpdate()
				.open().update(folder).commit().close()		// write folder into CR
				.open().commit().close()					// Transaction for setLastMapUPdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().create(folder).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a single modified folder
	 * @throws Exception
	 */
	@Test
	public void testModifyFolder() throws Exception {
		// create a new folder
		Folder folder = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createFolder(node.getFolder(), "New Folder").getId(), true);
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// modify the folder
		try (Trx trx = new Trx(null, 1)) {
			folder.setName("Modified Folder");
			folder.save();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().update(folder).commit().close()		// write folder into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the file be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()					// sync object types
				.open(mccr).commit(mccr).close(mccr)		// sync channel structure
				.open().commit().close()					// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().update(folder).commit().close()		// write folder into CR
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().update(folder).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing of a removed folder
	 * @throws Exception
	 */
	@Test
	public void testDeleteFolder() throws Exception {
		// create a new folder
		Folder folder = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createFolder(node.getFolder(), "New Folder").getId(), true);
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// delete the folder
		try (Trx trx = new Trx(null, 1)) {
			folder.delete();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().delete(folder).commit().close()		// delete folder from CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else{
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and run the publish process again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean delete = diffDelete && !mccr;
			Result result =
			Result.instance()
				.open().commit().close()											// sync object types
				.open(mccr).commit(mccr).close(mccr);								// sync channel structure

			if (!mccr) {
				result = result
					.open().commit().close()											// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			result = result
				.open(delete).delete(folder, delete).commit(delete).close(delete)	// delete folder from CR
				.open().commit().close();

			if (mccr) {
				result = result
					.open().commit().close()											// Transactions for get/setLastMapUpdate()
					.open().commit().close();
			}

			String expected = result.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().delete(folder).commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing multiple changes at once
	 * @throws Exception
	 */
	@Test
	public void testMultipleChanges() throws Exception {
		// create some objects
		Folder folder = null;
		ImageFile image = null;
		com.gentics.contentnode.object.File file = null;
		Page page = null;

		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createFolder(node.getFolder(), "New Folder").getId(), true);
			image = createImage(node.getFolder());
			file = createFile(node.getFolder());
			page = t.getObject(Page.class, ContentNodeTestDataUtils.createPage(node.getFolder(), template, "New Page").getId(), true);
			page.publish();
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// now modify objects
		try (Trx trx = new Trx(null, 1)) {
			folder.setName("Modified folder");
			folder.save();
			image.setName("mod_" + image.getName());
			image.save();
			file.setName("mod_" + file.getName());
			file.save();
			page.setName("Modified Page");
			page.save();
			page.publish();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().update(folder).commit().close()		// write folder into CR
				.open().update(image).commit().close()		// write image into CR
				.open().update(file).commit().close()		// write file into CR
				.open().update(page).commit().close()		// write page into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		testAppender.reset();
		publish();

		if (instantPublishing) {
			boolean mtPublishing = testContext.getContext().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTITHREADED_PUBLISHING);
			String expected = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr)							// sync channel structure
				.open().commit().close()										// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().update(folder).commit().close()							// write folder into CR
				.open().update(image).commit().close()							// write image into CR
				.open().update(file).commit().close()							// write file into CR
				.open(mtPublishing).commit(mtPublishing).close(mtPublishing)	// prepare publish data
				.open().update(page).commit().close()							// write page into CR
				.open().commit().close()										// Transactions for get/setLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			// all objects are expected to be published in a single transaction
			String expected = Result.instance()
				.open()
				.update(folder)
				.update(image)
				.update(file)
				.update(page)
				.commit()
				.close()
				.toString();

			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test modifying the publish handler
	 * @throws Exception
	 */
	@Test
	public void testModifiedPublishHandler() throws Exception {
		// do a publish run
		publish();

		// modify the publish handler
		try (Trx trx = new Trx(null, 1)) {
			DBUtils.executeUpdate("UPDATE cr_publish_handler SET properties = 'test1=modified1\ntest2=value2'", null);
			trx.success();
		}

		cleanLogCmd();
		// publish again
		testAppender.reset();
		publish();

		// the old instance must be destroyed and the new one initialized
		String dsId = ds != null ? ds.getId() : mccrDs.getId();
		if (instantPublishing) {
			String expected = Result.instance()
				.init(Result.map(CnMapPublishHandler.DS_ID, dsId, "test2", "value2", "test1", "modified1"))
				.destroy()
				.open().commit().close()
				.open(mccr).commit(mccr).close(mccr)
				.open().commit().close()				// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.init(Result.map(CnMapPublishHandler.DS_ID, dsId, "test2", "value2", "test1", "modified1"))
				.destroy()
				.open().commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing when the handler fails (throws an exception)
	 * @throws Exception
	 */
	@Test
	public void testFailingHandler() throws Exception {
		publish();

		cleanLogCmd();
		// use the failing handler now
		try (Trx trx = new Trx(null, 1)) {
			DBUtils.executeUpdate("UPDATE cr_publish_handler SET javaclass = ?", new Object[] {FailingHandler.class.getName()});
			trx.success();
		}

		NodeLogger logger = NodeLogger.getNodeLogger(FailingHandler.class);

		logger.setLevel(Level.INFO);
		logger.removeAllAppenders();
		testAppender = new TestAppender();
		logger.addAppender(testAppender);

		testAppender.reset();

		// create a new page
		Page page = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			page = t.getObject(Page.class, ContentNodeTestDataUtils.createPage(node.getFolder(), template, "New page").getId(), true);

			// publish the page
			page.publish();
			trx.success();
		}

		String dsId = ds != null ? ds.getId() : mccrDs.getId();
		if (instantPublishing) {
			String expected = Result.instance()
				.init(Result.map(CnMapPublishHandler.DS_ID, dsId, "test2", "value2", "test1", "value1"))
				.open()
				.create(page)
				.rollback()
				.close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		testAppender.reset();

		// publish process must fail now
		publish(false);

		// the new publish handler will also be initialized, because it is new
		if (instantPublishing) {
			boolean mtPublishing = testContext.getContext().getNodeConfig().getDefaultPreferences().isFeature(Feature.MULTITHREADED_PUBLISHING);
			String expected = Result.instance()
				.open().commit().close()										// sync object types
				.open(mccr).commit(mccr).close(mccr)							// sync channel structure
				.open(mtPublishing).commit(mtPublishing).close(mtPublishing)	// prepare publish data
				.open().commit().close()										// Transactions for get/setLastMapUpdate()
				.open().commit().close()
				.open().create(page)											// try writing page
				.rollback().close()												// roll back and close
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.init(Result.map(CnMapPublishHandler.DS_ID, dsId, "test2", "value2", "test1", "value1"))
				.open()
				.create(page)
				.rollback()
				.close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Test publishing, when the publish process fails when initializing the contentrepository
	 * @throws Exception
	 */
	@Test
	public void testFailingPublisher1() throws Exception {
		publish();

		cleanLogCmd();
		// we provoke a publish error by modifying the url of the contentrepository
		Trx.operate(t -> update(node.getContentRepository(), cr -> {
			cr.setUrl("bogus");
		}));

		testAppender.reset();
		// publish process must fail now
		publish(false);

		assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
	}

	/**
	 * Test publishing with an incorrect handler
	 * @throws Exception
	 */
	@Test
	public void testIncorrectHandler() throws Exception {
		// set a bogus javaclass
		try (Trx trx = new Trx(null, 1)) {
			DBUtils.executeUpdate("UPDATE cr_publish_handler SET javaclass = ?", new Object[] {"this.is.a.bogus.javaclass"});
			trx.success();
		}

		// publish process must fail
		publish(false);
	}

	/**
	 * Test publishing with a handler, that accesses the datasource itself (to fetch the original object)
	 * @throws Exception
	 */
	@Test
	public void testDsAccessingHandler() throws Exception {
		// use the ds accessing handler now
		try (Trx trx = new Trx(null, 1)) {
			DBUtils.executeUpdate("UPDATE cr_publish_handler SET javaclass = ?", new Object[] {DsAccessingHandler.class.getName()});
			trx.success();
		}

		// create a new folder
		Folder folder = null;
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			folder = t.getObject(Folder.class, ContentNodeTestDataUtils.createFolder(node.getFolder(), "New Folder").getId(), true);
			trx.success();
		}

		publish();

		cleanLogCmd();
		testAppender.reset();

		// modify the folder
		try (Trx trx = new Trx(null, 1)) {
			folder.setName("Modified Folder");
			folder.save();
			trx.success();
		}

		if (instantPublishing) {
			String expected = Result.instance()
				.open().update(folder).change("New Folder", "Modified Folder").commit().close()		// write folder into CR
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			assertEquals("Check for calls to CnMapPublishHandler", "", testAppender.toString());
		}

		// now reset the test appender and let the file be published again
		testAppender.reset();
		publish();

		if (instantPublishing) {
			String expected = Result.instance()
				.open().commit().close()															// sync object types
				.open(mccr).commit(mccr).close(mccr)												// sync channel structure
				.open().commit().close()															// Transactions for getLastMapUpdate()
				.open().commit().close()
				.open().update(folder).change("Modified Folder", "Modified Folder").commit().close()// write folder into CR
				.open().commit().close()															// Transactions for getLastMapUpdate()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		} else {
			String expected = Result.instance()
				.open().update(folder).change("New Folder", "Modified Folder").commit().close()
				.toString();
			assertEquals("Check for calls to CnMapPublishHandler", expected, testAppender.toString());
		}
	}

	/**
	 * Run the publish process
	 * @throws Exception
	 */
	public void publish() throws Exception {
		publish(true);
	}

	/**
	 * Run the publish process
	 * @param expectSuccess true if we expect the publish process to succeed, false if we expect it to fail
	 * @throws Exception
	 */
	public void publish(boolean expectSuccess) throws Exception {
		try (Trx trx = new Trx(null, 1)) {
			assertEquals("Check publish status", expectSuccess ? PublishInfo.RETURN_CODE_SUCCESS : PublishInfo.RETURN_CODE_ERROR,
					testContext.getContext().publish(false, true, System.currentTimeMillis(), expectSuccess).getReturnCode());
			trx.success();
		}
	}

	/**
	 * Create a new Node
	 *
	 * @param hostName
	 *            hostname
	 * @param name
	 *            node name
	 * @return node
	 * @throws Exception
	 */
	protected Node createNode(String hostName, String name) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node node = t.createObject(Node.class);

		node.setHostname(hostName);
		node.setPublishDir("/");
		node.setPublishFilesystem(true);
		Folder rootFolder = t.createObject(Folder.class);

		node.setFolder(rootFolder);
		rootFolder.setName(name);
		rootFolder.setPublishDir("/");

		node.save();
		t.commit(false);

		// create a datasource
		Map<String, String> handleProperties = createDatasource(node);
		ContentRepository cr = t.createObject(ContentRepository.class);

		cr.setName(name);

		cr.setDbType("hsql");
		cr.setCrType(Type.cr);
		cr.setMultichannelling(mccr);
		cr.setUrl(handleProperties.get("url"));
		cr.setUsername("sa");
		cr.setPassword("");
		cr.setDiffDelete(diffDelete);
		cr.setInstantPublishing(instantPublishing);

		cr.save();

		// activate publishing into the contentrepository
		node.setPublishContentmap(true);
		node.setContentrepositoryId(cr.getId());
		node.save();
		t.commit(false);

		ContentNodeTestDataUtils.addTagmapEntryAllTypes(cr, GenticsContentAttribute.ATTR_TYPE_TEXT, "node.id", "node_id", null, false,
				false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntryAllTypes(cr, GenticsContentAttribute.ATTR_TYPE_TEXT, "name", "name", null, false,
				false, true, -1, null, null);
		ContentNodeTestDataUtils.addTagmapEntry(cr,  Page.TYPE_PAGE, GenticsContentAttribute.ATTR_TYPE_TEXT_LONG, "", "content",
				null, false, false, true, -1, null, null);

		// add the publish handler
		DBUtils.executeUpdate("INSERT INTO cr_publish_handler (name, contentrepository_id, javaclass, properties) VALUES (?, ?, ?, ?)",
				new Object[] { "TestHandler", cr.getId(), LogHandler.class.getName(), "test1=value1\ntest2=value2" });

		return t.getObject(Node.class, node.getId());
	}

	/**
	 * Create a datasource for the given node
	 * @param node node
	 * @return handle properties
	 * @throws Exception
	 */
	protected Map<String, String> createDatasource(Node node) throws Exception {
		Map<String, String> handleProps = new HashMap<String, String>();

		handleProps.put("type", "jdbc");
		handleProps.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProps.put("url", "jdbc:hsqldb:mem:" + node.getHostname());
		handleProps.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> dsProps = new HashMap<String, String>();

		dsProps.put("sanitycheck2", "true");
		dsProps.put("autorepair2", "true");

		if (mccr) {
			mccrDs = (MCCRDatasource)PortalConnectorFactory.createMultichannellingDatasource(handleProps, dsProps);
			assertNotNull("Check whether datasource was created", mccrDs);
		} else {
			ds = PortalConnectorFactory.createDatasource(handleProps, dsProps);
			assertNotNull("Check whether datasource was created", ds);
		}

		return handleProps;
	}

	/**
	 * Create an image
	 * @param folder folder
	 * @return image
	 * @throws Exception
	 */
	protected ImageFile createImage(Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ImageFile image = t.createObject(ImageFile.class);

		image.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));
		image.setName("blume_.jpg");
		image.setFolderId(folder.getId());
		image.save();

		return image;
	}

	/**
	 * Create a file
	 * @param folder folder
	 * @return file
	 * @throws Exception
	 */
	protected com.gentics.contentnode.object.File createFile(Folder folder) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		ImageFile file = t.createObject(ImageFile.class);

		file.setFileStream(new ByteArrayInputStream("This is the file contents".getBytes("UTF-8")));
		file.setName("textfile.txt");
		file.setFolderId(folder.getId());
		file.save();

		return file;
	}

	/**
	 * Clean the logcmd table (to make sure we will not publish the root folder again)
	 */
	protected void cleanLogCmd() throws NodeException {
		try (Trx trx = new Trx(null, 1)) {
			DBUtils.executeUpdate("DELETE FROM logcmd WHERE o_type = 10002", null);
			trx.success();
		}
	}

	/**
	 * Implementation to construct the expected result
	 */
	public static class Result {
		/**
		 * StringBuilder
		 */
		protected StringBuilder expected = new StringBuilder();

		/**
		 * Generate string represenation of a TreeMap containing the given data
		 * @param data key1, value1, key2, value2, ...
		 * @return string representation
		 */
		protected static String map(String... data) {
			Map<String, String> map = new TreeMap<>();
			for (int i = 0; i < data.length - 1; i += 2) {
				map.put(data[i], data[i + 1]);
			}
			return map.toString();
		}

		/**
		 * Create a new Result instance
		 * @return new Result instance
		 */
		public static Result instance() {
			return new Result();
		}

		/**
		 * Open call
		 * @return instance
		 */
		public Result open() {
			return open(true);
		}

		/**
		 * Open call
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result open(boolean flag) {
			if (flag) {
				expected.append("open(timestamp)\n");
			}
			return this;
		}

		/**
		 * Close call
		 * @return instance
		 */
		public Result close() {
			return close(true);
		}

		/**
		 * Close call
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result close(boolean flag) {
			if (flag) {
				expected.append("close()\n");
			}
			return this;
		}

		/**
		 * Commit call
		 * @return instance
		 */
		public Result commit() {
			return commit(true);
		}

		/**
		 * Commit call
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result commit(boolean flag) {
			if (flag) {
				expected.append("commit()\n");
			}
			return this;
		}

		/**
		 * Rollback call
		 * @return instance
		 */
		public Result rollback() {
			expected.append("rollback()\n");
			return this;
		}

		/**
		 * Create call
		 * @param object created object
		 * @return instance
		 */
		public Result create(NodeObject object) {
			return create(object, true);
		}

		/**
		 * Create call
		 * @param object created object
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result create(NodeObject object, boolean flag) {
			if (flag) {
				expected.append("createObject(");
				append(object);
				expected.append(")\n");
			}
			return this;
		}

		/**
		 * Update call
		 * @param object updated object
		 * @return instance
		 */
		public Result update(NodeObject object) {
			return update(object, true);
		}

		/**
		 * Update call
		 * @param object updated object
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result update(NodeObject object, boolean flag) {
			if (flag) {
				expected.append("updateObject(");
				append(object);
				expected.append(")\n");
			}
			return this;
		}

		/**
		 * Change call
		 * @param from old value
		 * @param to new value
		 * @return instance
		 */
		public Result change(String from, String to) {
			expected.append("change ").append(from).append(" to ").append(to).append("\n");
			return this;
		}

		/**
		 * Delete call
		 * @param object deleted object
		 * @return instance
		 */
		public Result delete(NodeObject object) {
			return delete(object, true);
		}

		/**
		 * Delete call
		 * @param object deleted object
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result delete(NodeObject object, boolean flag) {
			if (flag) {
				expected.append("deleteObject(");
				append(object);
				expected.append(")\n");
			}
			return this;
		}

		/**
		 * Init call
		 * @param init init parameters
		 * @return instance
		 */
		public Result init(String init) {
			return init(init, true);
		}

		/**
		 * Init call
		 * @param init init parameters
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result init(String init, boolean flag) {
			if (flag) {
				expected.append("init(").append(init).append(")\n");
			}
			return this;
		}

		/**
		 * Destroy call
		 * @return instance
		 */
		public Result destroy() {
			return destroy(true);
		}

		/**
		 * Destroy call
		 * @param flag true if the call is expected, false if not
		 * @return instance
		 */
		public Result destroy(boolean flag) {
			if (flag) {
				expected.append("destroy()\n");
			}
			return this;
		}

		@Override
		public String toString() {
			return expected.toString();
		}

		/**
		 * Append the given object
		 * @param object object
		 * @return StringBuilder instance
		 */
		protected void append(NodeObject object) {
			int ttype = ObjectTransformer.getInt(object.getTType(), 0);
			if (ttype == ImageFile.TYPE_IMAGE) {
				ttype = File.TYPE_FILE;
			}
			expected.append(ttype).append(".").append(object.getId());
		}
	}

}
