package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertTrue;

import java.lang.Thread.State;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.contentnode.publish.CnMapPublishException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.events.Events;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.rest.model.request.StartpageRequest;
import com.gentics.contentnode.tests.cnmappublishhandler.LogHandler;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;

/**
 * Test cases for instant publishing
 */
public class InstantPublishingSandboxTest extends AbstractInstantPublishingTestSandboxTest {

	static final String NEW_OBJECT_NAME = "NewName";

	/**
	 * Node
	 */
	protected static Node node;

	/**
	 * Setup the sandbox 
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpOnce() throws Exception {
		// activate the feature instant publishing
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("instant_cr_publishing", true);
	}

	@Before
	public void setUp() throws Exception {
		restoreSnapshot();
		testContext.getContext().startTransaction();
		node = createNode("instant", "Instant Publishing Node", true, false);

		// publish once to get the cr structure up to date
		testContext.getContext().startTransaction();
		testContext.publish(false);
	}

	/**
	 * Destroy datasources
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Test moving an unpublished page (page must not be instant published)
	 * @throws Exception
	 */
	@Test
	public void testMoveUnpublishedPage() throws Exception {
		// get the root folder of the node
		Folder root = node.getFolder();

		// create a template
		Template template = createTemplate(root);

		// create a page
		Page page = createPage(root, template, "Page to move");

		// check that the page is not published
		assertInContentrepository(node, page, false);

		// create another folder
		Folder folder = createFolder(root, "Other Folder");

		// move the page to the other folder
		page.setFolderId(folder.getId());
		page.save();
		Transaction t = TransactionManager.getCurrentTransaction();

		// trigger the move event on the page
		t.addTransactional(new TransactionalTriggerEvent(page, null, Events.MOVE));
		t.commit(false);

		// check that the page still is not published
		assertInContentrepository(node, page, false);
	}

	/**
	 * Test localizing a published page. The former inherited page must be removed from the content repository
	 * @throws Exception
	 */
	@Test
	public void testLocalizePage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a channel
		Node channel = createNode("channel", "Channel", true, node);

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a template
		Template template = createTemplate(root);

		// create a page
		Page page = createPage(root, template, "Page to localize");

		// publish the page
		page.publish();
		t.commit(false);

		// page must now exist in master, but not in channel (because instant publishing will not handle inherited pages)
		assertInContentrepository(node, page, true);
		assertInContentrepository(channel, page, false);

		// run a publish process
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// page must now exist in master and channel
		assertInContentrepository(node, page, true);
		assertInContentrepository(channel, page, true);

		// now localize the page
		Page localizedPage = (Page) page.copy();

		localizedPage.setChannelInfo(channel.getId(), page.getChannelSetId());
		localizedPage.save();
		localizedPage.unlock();
		t.commit(false);
		assertTrue("Page failed to be localized", ObjectTransformer.getInt(page.getId(), 0) != ObjectTransformer.getInt(localizedPage.getId(), 0));

		// localized page is not yet published, so it is not instant published
		assertInContentrepository(node, page, true);
		assertInContentrepository(channel, page, true);
		assertInContentrepository(node, localizedPage, false);
		assertInContentrepository(channel, localizedPage, false);

		// publish the localized page
		localizedPage.publish();
		t.commit(false);

		// localized page must now replace the page in the channel's CR
		assertInContentrepository(node, page, true);
		assertInContentrepository(channel, page, false);
		assertInContentrepository(node, localizedPage, false);
		assertInContentrepository(channel, localizedPage, true);
	}

	/**
	 * Test unlocalizing a published page
	 * @throws Exception
	 */
	@Test
	public void testUnlocalizePage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a channel
		Node channel = createNode("channel", "Channel", true, node);

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a template
		Template template = createTemplate(root);

		// create a page
		Page page = createPage(root, template, "Page to localize");

		// publish the page
		page.publish();
		t.commit(false);

		// now localize the page
		Page localizedPage = (Page) page.copy();

		localizedPage.setChannelInfo(channel.getId(), page.getChannelSetId());
		localizedPage.save();
		localizedPage.unlock();
		localizedPage.publish();
		t.commit(false);
		assertTrue("Page failed to be localized", ObjectTransformer.getInt(page.getId(), 0) != ObjectTransformer.getInt(localizedPage.getId(), 0));

		// run a publish process
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// page must exist in the node and localized page in the channel
		assertInContentrepository(node, page, true);
		assertInContentrepository(channel, page, false);
		assertInContentrepository(node, localizedPage, false);
		assertInContentrepository(channel, localizedPage, true);

		// start a new transaction
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// remove the localized page
		localizedPage.delete();
		t.commit(false);

		// inherited page must now replace the localized page in the channel's CR
		assertInContentrepository(node, page, true);
		assertInContentrepository(channel, page, true);
		assertInContentrepository(node, localizedPage, false);
		assertInContentrepository(channel, localizedPage, false);
	}

	/**
	 * Test localizing a file
	 * @throws Exception
	 */
	@Test
	public void testLocalizeFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a channel
		Node channel = createNode("channel", "Channel", true, node);

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a file
		File file = createFile(root, "file-to-localize.txt", new byte[] { 1, 2, 3});

		// file must now exist in master, but not in channel (because instant publishing will not handle inherited files)
		assertInContentrepository(node, file, true);
		assertInContentrepository(channel, file, false);

		// run a publish process
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// file must now exist in master and channel
		assertInContentrepository(node, file, true);
		assertInContentrepository(channel, file, true);

		// now localize the file
		File localizedFile = (File) file.copy();

		localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
		localizedFile.save();
		t.commit(false);
		assertTrue("File failed to be localized", ObjectTransformer.getInt(file.getId(), 0) != ObjectTransformer.getInt(localizedFile.getId(), 0));

		// localized file must replace the inherited file in the channel
		assertInContentrepository(node, file, true);
		assertInContentrepository(channel, file, false);
		assertInContentrepository(node, localizedFile, false);
		assertInContentrepository(channel, localizedFile, true);
	}

	/**
	 * Test unlocalizing a file
	 * @throws Exception
	 */
	@Test
	public void testUnlocalizeFile() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a channel
		Node channel = createNode("channel", "Channel", true, node);

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a file
		File file = createFile(root, "file-to-localize.txt", new byte[] { 1, 2, 3});

		// now localize the file
		File localizedFile = (File) file.copy();

		localizedFile.setChannelInfo(channel.getId(), file.getChannelSetId());
		localizedFile.save();
		t.commit(false);
		assertTrue("File failed to be localized", ObjectTransformer.getInt(file.getId(), 0) != ObjectTransformer.getInt(localizedFile.getId(), 0));

		// run a publish process
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// file must exist in the node and localized file in the channel
		assertInContentrepository(node, file, true);
		assertInContentrepository(channel, file, false);
		assertInContentrepository(node, localizedFile, false);
		assertInContentrepository(channel, localizedFile, true);

		// start a new transaction
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// remove the localized file
		localizedFile.delete();
		t.commit(false);

		// inherited file must now replace the localized file in the channel's CR
		assertInContentrepository(node, file, true);
		assertInContentrepository(channel, file, true);
		assertInContentrepository(node, localizedFile, false);
		assertInContentrepository(channel, localizedFile, false);
	}

	/**
	 * Test localizing a folder
	 * @throws Exception
	 */
	@Test
	public void testLocalizeFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a channel
		Node channel = createNode("channel", "Channel", true, node);

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a folder
		Folder folder = createFolder(root, "Folder to Localize");

		// folder must now exist in master, but not in channel (because instant publishing will not handle inherited folders)
		assertInContentrepository(node, folder, true);
		assertInContentrepository(channel, folder, false);

		// run a publish process
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// page must now exist in master and channel
		assertInContentrepository(node, folder, true);
		assertInContentrepository(channel, folder, true);

		// now localize the folder
		Folder localizedFolder = (Folder) folder.copy();

		localizedFolder.setChannelInfo(channel.getId(), folder.getChannelSetId());
		localizedFolder.save();
		t.commit(false);
		assertTrue("Folder failed to be localized", ObjectTransformer.getInt(folder.getId(), 0) != ObjectTransformer.getInt(localizedFolder.getId(), 0));

		// localized folder must replace the inherited folder in the channel
		assertInContentrepository(node, folder, true);
		assertInContentrepository(channel, folder, false);
		assertInContentrepository(node, localizedFolder, false);
		assertInContentrepository(channel, localizedFolder, true);
	}

	/**
	 * Test unlocalizing a folder
	 * @throws Exception
	 */
	@Test
	public void testUnlocalizeFolder() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// create a channel
		Node channel = createNode("channel", "Channel", true, node);

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a folder
		Folder folder = createFolder(root, "Folder to Localize");

		// now localize the folder
		Folder localizedFolder = (Folder) folder.copy();

		localizedFolder.setChannelInfo(channel.getId(), folder.getChannelSetId());
		localizedFolder.save();
		t.commit(false);
		assertTrue("Folder failed to be localized", ObjectTransformer.getInt(folder.getId(), 0) != ObjectTransformer.getInt(localizedFolder.getId(), 0));

		// run a publish process
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// folder must exist in the node and localized folder in the channel
		assertInContentrepository(node, folder, true);
		assertInContentrepository(channel, folder, false);
		assertInContentrepository(node, localizedFolder, false);
		assertInContentrepository(channel, localizedFolder, true);

		// start a new transaction
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// remove the localized folder
		localizedFolder.delete();
		t.commit(false);

		// inherited folder must now replace the localized folder in the channel's CR
		assertInContentrepository(node, folder, true);
		assertInContentrepository(channel, folder, true);
		assertInContentrepository(node, localizedFolder, false);
		assertInContentrepository(channel, localizedFolder, false);
	}

	/**
	 * Test setting a startpage for a new created folder
	 * @throws Exception
	 */
	@Test
	public void testSetStartpage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a folder
		Folder folder = createFolder(root, "Folder for startpage");

		// create the startpage tagmap entry
		createStartpageObjectProperty();
		createStartpageTagmapEntry(node);

		// run a publish process to synchronize the tagmap entries
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// create a template
		Template template = createTemplate(folder);

		// create and publish a page
		Page startpage = createPage(folder, template, "Startpage");

		startpage.publish();
		t.commit(false);

		// start a new transaction
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// set the startpage
		StartpageRequest request = new StartpageRequest();

		request.setPageId(ObjectTransformer.getInt(startpage.getId(), 0));
		ContentNodeRESTUtils.assertResponseOK(ContentNodeRESTUtils.getFolderResource().setStartpage(ObjectTransformer.getString(folder.getId(), null), request));
		t.commit(false);

		// do the assertion
		assertInContentrepository(node, folder, "startpage", Page.TYPE_PAGE + "." + startpage.getId());
	}

	/**
	 * Test changing the startpage for a folder
	 * @throws Exception
	 */
	@Test
	public void testChangeStartpage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// get the root folder of the node
		Folder root = node.getFolder();

		// create a folder
		Folder folder = createFolder(root, "Folder for startpage");

		// create the startpage tagmap entry
		createStartpageObjectProperty();
		createStartpageTagmapEntry(node);

		// create a template
		Template template = createTemplate(folder);

		// create and publish a page
		Page startpage = createPage(folder, template, "Startpage");

		startpage.publish();
		t.commit(false);

		// create an publish a second page
		Page otherStartpage = createPage(root, template, "Other startpage");

		otherStartpage.publish();
		t.commit(false);

		// start a new transaction
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// set the startpage
		StartpageRequest request = new StartpageRequest();

		request.setPageId(ObjectTransformer.getInt(startpage.getId(), 0));
		ContentNodeRESTUtils.assertResponseOK(ContentNodeRESTUtils.getFolderResource().setStartpage(ObjectTransformer.getString(folder.getId(), null), request));
		t.commit(false);

		// run a publish process to synchronize the tagmap entries
		assertTrue("Publish process failed", testContext.getContext().publish(false).getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);

		// start a new transaction
		testContext.getContext().startTransaction();
		t = TransactionManager.getCurrentTransaction();

		// now change the startpage
		request = new StartpageRequest();
		request.setPageId(ObjectTransformer.getInt(otherStartpage.getId(), 0));
		ContentNodeRESTUtils.assertResponseOK(ContentNodeRESTUtils.getFolderResource().setStartpage(ObjectTransformer.getString(folder.getId(), null), request));
		t.commit(false);

		// do the assertion
		assertInContentrepository(node, folder, "startpage", Page.TYPE_PAGE + "." + otherStartpage.getId());
	}

	/**
	 * Tests instant publishing of a page during a normal publish run.
	 *
	 * This test uses the {@link InstantPublishHandler} to provoke
	 * a modification and therefore an instant publish action while a
	 * regular publication is running.
	 */
	@Test
	public void testInstantPublishFileDuringPublishRun() throws Exception {
		createNameTagmapEntry(node, File.TYPE_FILE, "file");

		Folder folder = Creator.createFolder(node.getFolder(), "Test folder", "/", node);
		File file = Creator.createFile(folder, "test.txt", "Test".getBytes(), node);

		file.setForceOnline(true);
		file.save();

		InstantPublishWorker worker = InstantPublishWorker.getInstance();

		worker.reset(testContext);

		testContext.getContext().startTransaction();
		testContext.publish(true);

		testContext.getDBSQLUtils().executeQueryManipulation(
			"INSERT INTO cr_publish_handler (name, contentrepository_id, javaclass, properties) "
			+ "VALUES ('TestHandler', " + node.getContentrepositoryId()
				+ ", '" + InstantPublishHandler.class.getName()
				+ "', 'starton=" + Folder.TYPE_FOLDER + "." + folder.getId()
				+ "\nmodify=" + File.TYPE_FILE + "." + file.getId() + "')");

		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(folder, Action.DEPENDENCY, 0);
			PublishQueue.dirtObject(file, Action.DEPENDENCY, 0);

			trx.success();
		}

		testContext.getContext().publish(true);
		worker.joinThread();

		assertInContentrepository(node, file, "name", NEW_OBJECT_NAME);
	}

	/**
	 * Tests instant publishing of a page during a normal publish run.
	 *
	 * This test uses the {@link InstantPublishHandler} to provoke
	 * a modification and therefore an instant publish action while a
	 * regular publication is running.
	 */
	@Test
	public void testInstantPublishPageDuringPublishRun() throws Exception {
		createNameTagmapEntry(node, Page.TYPE_PAGE, "page");

		Folder folder = Creator.createFolder(node.getFolder(), "Test folder", "/", node);
		Page page = Creator.createPage("Test page", folder, Creator.createTemplate("Test template", "", folder));

		page.publish();

		InstantPublishWorker worker = InstantPublishWorker.getInstance();

		worker.reset(testContext);

		testContext.getContext().startTransaction();
		testContext.publish(true);

		testContext.getDBSQLUtils().executeQueryManipulation(
			"INSERT INTO cr_publish_handler (name, contentrepository_id, javaclass, properties) "
			+ "VALUES ('TestHandler', " + node.getContentrepositoryId()
				+ ", '" + InstantPublishHandler.class.getName()
				+ "', 'starton=" + Folder.TYPE_FOLDER + "." + folder.getId()
				+ "\nmodify=" + Page.TYPE_PAGE + "." + page.getId() + "')");

		try (Trx trx = new Trx()) {
			PublishQueue.dirtObject(folder, Action.DEPENDENCY, 0);
			PublishQueue.dirtObject(page, Action.DEPENDENCY, 0);

			trx.success();
		}

		testContext.getContext().publish(false);
		worker.joinThread();

		assertInContentrepository(node, page, "name", NEW_OBJECT_NAME);
	}

	/**
	 * A singleton runnable which will modify the object
	 * specified in {@link #startThread}.
	 *
	 * The modification has to happen in a separate thread,
	 * because it has to happen in a different transaction than
	 * the publish process.
	 *
	 * This should be used in a publish handler, during
	 * a regular publish run, to provoke an instant publish
	 * action.
	 *
	 * To prevent timing issues, this thread should be allowed
	 * to reach the commit of the transaction to make sure,
	 * the modification happens before the regular publish
	 * run processes the object (this would lead to a false
	 * positive test, because the instant publish action would
	 * occur <em>after</em> the regular publication. 
	 */
	public static class InstantPublishWorker implements Runnable {
		/** The singleton instance of this class. */
		private volatile static InstantPublishWorker instance = null;

		/** The logger. */
		private final NodeLogger logger = NodeLogger.getNodeLogger(getClass());

		/** The test context to use. */
		private DBTestContext testContext = null;
		/** The worker thread. */
		private Thread thread;
		/** The type of the object to be modified. */
		private int objType;
		/** The ID of the object to be modified. */
		private int objId;

		/**
		 * Get the instance of the instant publish worker.
		 *
		 * @return The singleton instance of the worker.
		 */
		public static InstantPublishWorker getInstance() {
			if (instance == null) {
				instance = new InstantPublishWorker();
			}

			return instance;
		}

		/**
		 * Private constructor to prevent instantiation.
		 */
		private InstantPublishWorker() {
		}

		/**
		 * Checks if the instance is in a state that allows
		 * calls to {@link #reset} and {@link #startThread}.
		 *
		 * @throws IllegalStateException When the worker thread
		 *		is currently running.
		 */
		private void checkState() throws IllegalStateException {
			if (thread != null && thread.isAlive()) {
				throw new IllegalStateException(
					"Cannot reset or started InstantPublishWorker while thread is still running");
			}
		}

		/**
		 * Checks if the worker thread is running.
		 *
		 * @return <code>true</code> if the worker thread is
		 *		running, and <code>false</code> otherwise.
		 */
		public boolean isRunning() {
			return (thread != null && thread.isAlive());
		}

		public boolean isBlocked() {
			if (thread == null || !thread.isAlive()) {
				return false;
			}

			return thread.getState() != State.RUNNABLE;
		}

		/**
		 * Resets the instance of the worker.
		 *
		 * @param testContext The test context to use.
		 */
		public void reset(DBTestContext testContext) {
			checkState();

			this.testContext = testContext;

			thread = new Thread(this, "InstantPublishWorker");
			objType = objId = 0;
		}

		/**
		 * Starts the worker thread modifying the specified object.
		 *
		 * @param objType The type of the object to modify.
		 * @param objId The ID of the object to modify.
		 * @throws InterruptedException
		 */
		public void startThread(int objType, int objId) {
			checkState();

			if (testContext == null) {
				throw new IllegalStateException("Cannot start InstantPublishWorker thread without test context");
			}

			this.objType = objType;
			this.objId = objId;

			thread.start();
		}

		/**
		 * Waits for the worker thread to finish.
		 *
		 * @throws InterruptedException When the waiting was interrupted.
		 */
		public void joinThread() throws InterruptedException {
			if (thread == null) {
				throw new IllegalStateException("Thread was not started yet");
			}

			if (thread.isAlive()) {
				thread.join();
			}
		}

		/**
		 * Loads the specified object to modify and sets a new name,
		 * causing instant publication of the object.
		 */
		@Override
		public void run() {
			try {
				testContext.getContext().startTransaction();
				Transaction t = TransactionManager.getCurrentTransaction();

				switch (objType) {
				case File.TYPE_FILE:
					File file = t.getObject(File.class, objId, true);

					if (logger.isDebugEnabled()) {
						logger.debug("Changing " + file + "'s name to " + NEW_OBJECT_NAME);
					}
					file.setName(NEW_OBJECT_NAME);
					file.save();
					break;

				case Page.TYPE_PAGE:
					Page page = t.getObject(Page.class, objId, true);

					if (logger.isDebugEnabled()) {
						logger.debug("Changing " + page + "'s name to " + NEW_OBJECT_NAME);
					}
					page.setName(NEW_OBJECT_NAME);
					page.save();
					page.publish();
					break;

				default:
					throw new Exception("Unexpected object type " + objType);
				}

				t.commit();
			} catch (Exception e) {
				throw new Error("Could not modify object to provoke instant publishing", e);
			}
		}
	}

	/**
	 * Publish handler, which triggers an object modification
	 * and therefore instant publishing of that object during
	 * a normal publish run.
	 */
	public static class InstantPublishHandler extends LogHandler {
		/** The type of the object which should trigger the instant publishing. */
		private int startObjType = 0;
		/** The ID of the object which should trigger the instant publishing. */
		private int startObjId = 0;
		/** The type of the object to be modified. */
		private int modifyObjType = 0;
		/** The ID of the object to be modified. */
		private int modifyObjId = 0;

		/**
		 * Initializes this handler.
		 *
		 * The two required parameters are
		 * <ul>
		 *   <li>starton, and</li>
		 *   <li>modify</li>
		 * </ul>
		 * and both must have the form <code>OBJECT_TYPE.OBJECT_ID</code>.
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void init(Map parameters) throws CnMapPublishException {
			String parts[];

			for (Map.Entry<Object, Object> parameter : ((Map<Object, Object>) parameters).entrySet()) {
				switch (parameter.getKey().toString()) {
					case "starton":
						parts = parameter.getValue().toString().split("\\.");
						startObjType = Integer.parseInt(parts[0]);
						startObjId = Integer.parseInt(parts[1]);
						break;

					case "modify":
						parts = parameter.getValue().toString().split("\\.");
						modifyObjType = Integer.parseInt(parts[0]);
						modifyObjId = Integer.parseInt(parts[1]);
						break;

					default:
						continue;
				}
			}

			if (startObjType == 0 || startObjId == 0 || modifyObjType == 0 || modifyObjId == 0) {
				throw new CnMapPublishException("Not all necessary parameters provided (starton, modify)");
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Initialised new InstantPublishHandler starting on "
					+ startObjType + "." + startObjId + " and modifying "
					+ modifyObjType + "." + modifyObjId);
			}
		}

		/**
		 * Starts an instant publishing worker when a certain object
		 * is published.
		 *
		 * The worker will modify the object with content ID
		 * "{@link #modifyObjType}.{@link #modifyObjId}" and start the
		 * thread once it encounters "{@link #startObjType}.{@link #startObjId}".
		 *
		 * This method will wait for the worker thread to try a commit, to make
		 * sure it will get the lock for the content map, directly after the
		 * triggering object was processed. (See also the comments on
		 * {@link InstantPublishWorker}.)
		 */
		@Override
		public void updateObject(Resolvable object) throws CnMapPublishException {
			int objType = Integer.parseInt(object.get("obj_type").toString());
			int objId = Integer.parseInt(object.get("obj_id").toString());
			InstantPublishWorker worker = InstantPublishWorker.getInstance();

			if (worker.isRunning() || objType != startObjType || objId != startObjId) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring object " + objType + "." + objId
						+ " (worker: " + (worker.isRunning() ? "" : "not ") + "running, startObj: "
						+ startObjType + "." + startObjId + ")");
				}

				return;
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Starting worker thread for " + modifyObjType + "." + modifyObjId);
			}

			try {
				Transaction t = TransactionManager.getCurrentTransaction();

				t.getObject(t.getClass(modifyObjType), modifyObjId);
			} catch (NodeException e) {
				throw new CnMapPublishException(e);
			}

			worker.startThread(modifyObjType, modifyObjId);

			// Wait until the worker thread tries to commit the transaction,
			// to make sure the publisher thread does not reach the modified
			// object before the worker thread.
			while (!worker.isBlocked()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// Doesn't matter, we will continue sleeping.
				}
			}
		}
	}
}
