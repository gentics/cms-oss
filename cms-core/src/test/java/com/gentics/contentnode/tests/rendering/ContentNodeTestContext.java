/*
 * @author herbert
 * @date 22.01.2007
 * @version $Id: ContentNodeTestContext.java,v 1.8 2010-09-02 08:02:47 johannes2 Exp $
 */
package com.gentics.contentnode.tests.rendering;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.Level;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentMap;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.init.BcryptPasswords;
import com.gentics.contentnode.msg.NodeMessage;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.jaxb.JAXBHelper;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;

/**
 * Simple class holding context information for the tests.
 * (Currently only DB connection.)
 * 
 * @author herbert
 */
public class ContentNodeTestContext {

	/**
	 * a path to a property file containing local preferences which could be used for development.
	 * These overwrite the settings which are retrieved from {@link #CONFIG_URL_SYSTEMPROPERTY}.
	 */
	public static final String CONFIG_LOCAL_SYSTEMPROPERTY = "com.gentics.contentnode.config.local";

	/**
	 * list of id's of pages to include in the test, if not set, all (test) pages will be rendered
	 */
	public final static String CONFIG_INCLUDED_PAGES = "com.gentics.contentnode.page";
    
	/**
	 * The loaded properties can be overwritten by using the properties from this file within the constructor.  
	 */
	private final static String CONFIG_OVERWRITES_FILE = "default_config_overwrites.properties";

	/**
	 * timeout in (ms) for waiting on the dirtqueue worker
	 */
	public final static int DIRTQUEUEWORKER_WAITTIMEOUT = 60000;

	private Transaction transaction;
	private List includedPages;

	private ContentNodeFactory factory;

	/**
	 * maximum number of bytes in the filedata buffer
	 */
	public final static int FILEDATA_BUFFER_SIZE = 4096;

	private static NodeLogger logger = NodeLogger.getNodeLogger(ContentNodeTestContext.class);

	/**
	 * Whether the passwords job has been run once.
	 * This will be set to true when calling the login method.
	 */
	private boolean passwordHashJobrun = false;

	/**
	 * Create new ContentNodeTestContext and overload the properties loaded by
	 * these custom ones
	 * 
	 * @throws NodeException 
	 * @throws IOException
	 */
	public ContentNodeTestContext() throws NodeException {
		this(true, false);
	}

	/**
	 * Create an instance of the test context
	 * @param startTransaction true to start a transaction
	 * @param force true to force re-initialization of node config (e.g. when database connection was changed)
	 * @throws NodeException
	 */
	public ContentNodeTestContext(boolean startTransaction, boolean force) throws NodeException {
		System.setProperty("com.gentics.contentnode.dirtqueue.wait", "100");
		// set the system property "testmode", so that the dirtqueue worker will not be started immediately
		System.setProperty("com.gentics.contentnode.testmode", "true");
		init(startTransaction, force);
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	/**
	 * Get the NodeConfig
	 * @return NodeConfig
	 */
	public NodeConfig getNodeConfig() {
		return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
	}

	/**
	 * Invoke a cacheclear for the nodefactory caches
	 * @throws NodeException
	 * @throws IOException 
	 */
	public void clearNodeObjectCache() throws NodeException, IOException {
		NodeFactory.getInstance().clear();
	}

	/**
	 * Initialize the test context
	 * @param startTransaction true to start a transaction
	 * @param force true to force re-initialization of node config (e.g. when database connection was changed)
	 * @throws NodeException
	 */
	protected void init(boolean startTransaction, boolean force) throws NodeException {
		try {
			ContentRenderer.registerRenderer();

			NodeConfig nodeConfig = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
			NodePreferences nodePreferences = nodeConfig.getDefaultPreferences();

			// initialize velocity
			NodeConfigRuntimeConfiguration.initVelocity();

			factory = ContentNodeFactory.getInstance();

			// initialize the permission store
			startTransaction();
			PermissionStore.initialize(true);
			TransactionManager.getCurrentTransaction().commit();
			transaction = null;

			if (startTransaction) {
				startTransaction();
			}

			JAXBHelper.init(null);

			String includedPagesConf = System.getProperty(CONFIG_INCLUDED_PAGES, null);

			if (includedPagesConf != null) {
				includedPages = new Vector();
				String[] pageIds = StringUtils.splitString(includedPagesConf, ',');

				for (int i = 0; i < pageIds.length; i++) {
					includedPages.add(pageIds[i].trim());
				}
			}

		} catch (Exception e) {
			throw new RuntimeException("Error during initialization of test context.", e);
		}
	}

	public ContentNodeFactory getContentNodeFactory() {
		return this.factory;
	}

	public void setCurrentTransaction() {
		TransactionManager.setCurrentTransaction(transaction);
	}

	/**
	 * Method to start a new transaction for the systemuser. If a transaction is open, it is closed first.
	 * @throws NodeException
	 */
	public void startTransaction() throws NodeException {
		startTransaction(1);
	}

	/**
	 * Start a new transaction for the given user. If a transaction is open, it is closed first.
	 * @param userId ID of the systemuser
	 * @throws NodeException
	 */
	public void startTransaction(int userId) throws NodeException {
		if (transaction != null && transaction.isOpen()) {
			transaction.commit();
		}
		transaction = factory.startTransaction(null, userId, true);
		RenderType renderType = RenderType.getDefaultRenderType(getNodeConfig().getDefaultPreferences(), RenderType.EM_PUBLISH, null, 0);
		renderType.setHandleDependencies(false);
		transaction.setRenderType(renderType);
		transaction.setPublishCacheEnabled(false);
		ContentNodeHelper.setLanguageId(1);
	}

	/**
	 * Perform login with the given user credentials.
	 * Starts a new transaction for the logged in user
	 * @param login login
	 * @param password password
	 * @return A session ID
	 * @throws NodeException
	 * @throws JobExecutionException 
	 */
	public String login(String login, String password) throws NodeException {
		if (transaction != null && transaction.isOpen()) {
			transaction.commit();
		}

		String sid = null;
		try (Trx trx = new Trx()) {
			if (!passwordHashJobrun) {
				BcryptPasswords bcryptPasswords = new BcryptPasswords();
				bcryptPasswords.execute();
				passwordHashJobrun = true;
			}
			sid = ContentNodeRESTUtils.login(login, password);

			trx.success();
		}

		transaction = factory.startTransaction(sid, true);
		transaction.setRenderType(RenderType.getDefaultRenderType(getNodeConfig().getDefaultPreferences(), RenderType.EM_PUBLISH, null, 0));
		ContentNodeHelper.setLanguageId(1);

		return sid;
	}

	/**
	 * Check whether the page shall be tested or not
	 * @param page page to test
	 * @return true when the page shall be tested, false if not
	 */
	public boolean doTestPage(Page page) {
		if (page == null) {
			return false;
		} else if (includedPages == null) {
			return true;
		} else {
			return includedPages.contains(page.getId().toString());
		}
	}

	/**
	 * Create an instance of {@link SQLUtils} which is configured for the node db.
	 * This instance can be used to generate, query or manipulate the node db
	 * @return instance of SQLUtils
	 * @throws JDBCMalformedURLException 
	 * @throws SQLUtilException 
	 */
	public SQLUtils createNodeDBSQLUtils() throws JDBCMalformedURLException, SQLUtilException {
		// get the configuration
		NodeConfig config = getNodeConfig();

		// prepare the connection properties
		Properties dbProps = new Properties();

		dbProps.setProperty("url", config.getDefaultPreferences().getProperty("contentnode.db.settings.url"));
		dbProps.setProperty("driverClass", config.getDefaultPreferences().getProperty("contentnode.db.settings.driverClass"));
		dbProps.setProperty("username", config.getDefaultPreferences().getProperty("contentnode.db.settings.login"));
		dbProps.setProperty("passwd", config.getDefaultPreferences().getProperty("contentnode.db.settings.pw"));

		// create an return the instance of SQLUtils
		return SQLUtilsFactory.getSQLUtils(dbProps);
	}

	/**
	 * Create the instances of {@link SQLUtils} for all contentrepositories configured in the node db, keys are the names of the cr's
	 * @return Map of {name} -&gt; {SQLUtil}
	 * @throws Exception
	 */
	public Map<String, SQLUtils> createCRDBSQLUtils() throws Exception {
		Map<String, SQLUtils> crdbSQLUtils = new HashMap<String, SQLUtils>();
		NodeConfig config = getNodeConfig();
		Statement st = null;
		ResultSet crConfigs = null;

		try {
			st = transaction.getStatement();
			crConfigs = st.executeQuery("SELECT * from contentrepository");
			while (crConfigs.next()) {
				Properties dbProps = new Properties();

				dbProps.putAll(ContentMap.getHandleParameters(crConfigs, config));
				SQLUtils utils = SQLUtilsFactory.getSQLUtils(dbProps);

				crdbSQLUtils.put(crConfigs.getString("name"), utils);
			}
		} finally {
			if (transaction != null) {
				transaction.closeResultSet(crConfigs);
				transaction.closeStatement(st);
			}
		}

		return crdbSQLUtils;
	}

	/**
	 * Get the publish directory
	 * @return publish directory
	 */
	public File getPubDir() {
		return new File(ConfigurationValue.PUBLISH_PATH.get(), "pub");
	}

	/**
	 * Run a publish process
	 * @param dirtAll true when everything shall be dirted before, false if not
	 * @throws Exception
	 */
	public PublishInfo publish(boolean dirtAll) throws Exception {
		return publish(dirtAll, true, System.currentTimeMillis());
	}

	/**
	 * Run a publish process
	 * @param dirtAll true when everything shall be dirted before, false if not
	 * @param wait true when this thread shall wait for the publisher thread to die, false if not
	 * @throws Exception
	 */
	public PublishInfo publish(boolean dirtAll, boolean wait) throws Exception {
		return publish(dirtAll, wait, System.currentTimeMillis());
	}

	/**
	 * Run a publish process
	 * @param dirtAll true when everything shall be dirted before, false if not
	 * @param wait true when this thread shall wait for the publisher thread to die, false if not
	 * @param timestamp timestamp
	 * @throws Exception
	 */
	public PublishInfo publish(boolean dirtAll, boolean wait, long timestamp) throws Exception {
		return publish(dirtAll, wait, timestamp, true);
	}

	/**
	 * Run a publish process
	 * @param dirtAll true when everything shall be dirted before, false if not
	 * @param wait true when this thread shall wait for the publisher thread to die, false if not
	 * @param timestamp timestamp
	 * @param shouldSucceed Whether the publishing is expected to succeed (log will be output otherwise)
	 * @throws Exception
	 */
	public PublishInfo publish(boolean dirtAll, boolean wait, long timestamp, boolean succeed) throws Exception {
		if (dirtAll) {
			// get ids of nodes, that have publish not disabled
			final List<Integer> nodeIdsList = new ArrayList<Integer>();

			DBUtils.executeStatement("SELECT id FROM node WHERE disable_publish = ?", new SQLExecutor() {
				@Override
				public void prepareStatement(PreparedStatement stmt) throws SQLException {
					stmt.setInt(1, 0); // disable_publish = ?
				}

				@Override
				public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
					while (rs.next()) {
						nodeIdsList.add(rs.getInt("id"));
					}
				}
			});

			int[] nodeIds = new int[nodeIdsList.size()];

			for (int i = 0; i < nodeIdsList.size(); ++i) {
				nodeIds[i] = nodeIdsList.get(i);
			}
			// dirt all pages
			PublishQueue.dirtPublishedPages(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			// dirt images and files
			PublishQueue.dirtImagesAndFiles(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			// dirt folders
			PublishQueue.dirtFolders(nodeIds, null, 0, 0, PublishQueue.Action.DEPENDENCY);
			if (transaction.isOpen()) {
				transaction.commit(false);
			}
		}

		// start the dirt queue worker
		startDirtQueueWorker();

		try {
			PublishController.startPublish(true, timestamp);

			if (wait) {
				// ... and wait for it to die 
				PublishController.joinPublisherLocally();
			}
        	
			if (succeed && PublishController.getPublishInfo().getReturnCode() != PublishInfo.RETURN_CODE_SUCCESS) {
				File publishLog = PublishController.getPublishLog();

				if (publishLog != null) {
					System.out.println(FileUtil.file2String(publishLog));
				}
				Collection<NodeMessage> messages = PublishController.getPublishInfo().getMessages();

				for (NodeMessage message : messages) {
					if (message.getLevel().isMoreSpecificThan(Level.ERROR)) {
						if (message.getThrowable() != null) {
							logger.error(message.getMessage(), message.getThrowable());
						} else {
							logger.error(message.getMessage());
						}
					}
				}
			}
			if (transaction.isOpen()) {
				transaction.commit(false);
			}
			// return the publish info
			return PublishController.getPublishInfo();
		} finally {
			if (wait) {
				stopDirtQueueWorker();
			}
		}
	}

	/**
	 * Wait for the dirtqueue worker to do it's job (handle all dirtqueue
	 * entries). This method fails, when we have to wait longer than
	 * {@link #DIRTQUEUEWORKER_WAITTIMEOUT} ms.
	 * 
	 * @throws Exception
	 */
	public void waitForDirtqueueWorker(SQLUtils dbUtils) throws Exception {
		// first start the dirt queue worker
		startDirtQueueWorker();

		try {
			// wait until no more entry in dirtqueue exists (all events have been
			// handled)
			int numDirtQueueEntries = 0;
			long waitingStart = System.currentTimeMillis();

			while ((System.currentTimeMillis() - waitingStart) < DIRTQUEUEWORKER_WAITTIMEOUT
					&& (numDirtQueueEntries = dbUtils.getNumRows("SELECT * FROM dirtqueue")) > 0) {
				// wait 1 second
				Thread.sleep(1000);
			}
    		
			// now check whether there were still some dirtqueue entries left
			assertEquals("Check number of dirtqueue entries after waiting " + (System.currentTimeMillis() - waitingStart) + " ms:", 0, numDirtQueueEntries);
		} finally {
			// finally stop the dirt queue worker
			stopDirtQueueWorker();
		}
	}

	/**
	 * Start the dirt queue worker
	 * @throws NodeException
	 */
	public void startDirtQueueWorker() throws NodeException {
		factory.startDirtQueueWorker();
	}

	/**
	 * Stop the dirt queue worker
	 * @throws NodeException
	 */
	public void stopDirtQueueWorker() throws NodeException {
		factory.stopDirtQueueWorker();
	}

	/**
	 * Activate/deactivate the given feature
	 * @param feature feature
	 * @param active true to activate, false to deactivate
	 */
	public void setFeature(Feature feature, boolean active) {
		getNodeConfig().getDefaultPreferences().setFeature(feature.toString().toLowerCase(), active);
	}
}
