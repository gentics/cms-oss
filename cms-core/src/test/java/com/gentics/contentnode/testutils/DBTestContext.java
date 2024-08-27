package com.gentics.contentnode.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.ResolverContextHandler;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.SessionToken;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.init.InitJob;
import com.gentics.contentnode.init.MigrateTimeManagement;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.resolving.NodeObjectResolverContext;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlets.UdateChecker;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.lib.log.NodeLogger;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.JDBCSettings;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.infrastructure.TestEnvironment;
import com.gentics.testutils.sandbox.GCNSandboxHelper;
import com.gentics.testutils.testdbmanager.ManagerResponse;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

/**
 * Test Database Context
 *
 * Please note that this class will modify the java.io.tmpdir path temporarily.
 */
public class DBTestContext extends TestWatcher {

	protected static final NodeLogger logger = NodeLogger.getNodeLogger(DBTestContext.class);

	private ExecutorService es;

	/**
	 * SQLUtils for the cms database
	 */
	private SQLUtils dbUtils;

	/**
	 * Id of a user with permissions
	 */
	public final static Integer USER_WITH_PERMS = 26;

	/**
	 * Id of a user without permissions
	 */
	public final static Integer USER_WITHOUT_PERMS = 27;

	/**
	 * Timeout in (ms) for waiting on the dirtqueue worker
	 */
	public final static Integer DIRTQUEUEWORKER_WAITTIMEOUT = 60000;

	/**
	 * Name of the default test configuration file
	 */
	public final static String DEFAULT_CONFIG_NAME = "default_test_config.yml";

	public final int DEFAULT_MAX_WAIT = 240;

	/**
	 * Name of the environment variable, which could contain a comma separated list
	 * of features, that need to be activated when running tests
	 */
	public final static String ENV_TEST_FEATURES = "CMS_TEST_FEATURES";

	protected ContentNodeTestContext context;

	protected java.io.File pubDir;

	/**
	 * Flag that is used to determine whether dbfiles should be copied or not.
	 * Skipping dbfile download can speedup test execution.
	 */
	private boolean skipDBFileDownload = false;

	/**
	 * Whether to start the DirtQueueWorker thread
	 */
	private boolean startDirtQueueWorker = false;

	private Properties connectionProperties = new Properties();

	private File gcnBasePath;

	protected int maxWait = DEFAULT_MAX_WAIT;

	/**
	 * List of init jobs to do
	 */
	private List<Class<? extends InitJob>> initJobs = new ArrayList<>(Arrays.asList(MigrateTimeManagement.class));

	/**
	 * Optional configuration modificator
	 */
	private Consumer<MapPreferences> configModificator;

	static {
		System.setProperty("com.gentics.contentnode.testmode", "true");
		GenericTestUtils.initConfigPathForCache();
		// Modify the java.io.tmpdir to ensure that there are no conflicts in
		// between multithreaded tests
		final File newTmpDir = new File(System.getProperty("java.io.tmpdir"), "random_" + TestEnvironment.getRandomHash(10));
		newTmpDir.mkdirs();
		System.setProperty("java.io.tmpdir", newTmpDir.getAbsolutePath());
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FileUtils.deleteDirectory(newTmpDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Request a GCN Database
	 * @param future future, that is completed when the gcn database is available
	 */
	public static void requestGCNDB(final CompletableFuture<ManagerResponse> future) {
		try {
			WebSocket websocket = new WebSocketFactory().createSocket("ws://" + GCNSandboxHelper.getHostname() + ":" + GCNSandboxHelper.getPort() + "/create").addListener(new WebSocketAdapter() {
				@Override
				public void onTextMessage(WebSocket ws, String message) {
					try {
						ManagerResponse response = GCNSandboxHelper.gson.fromJson(message, ManagerResponse.class);

						Properties databaseProperties = response.toProperties();

						// Force the MariaDB JDBC driver
						databaseProperties.setProperty(JDBCSettings.DRIVERNAME_PROPERTY_KEY, "org.mariadb.jdbc.Driver");
						String jdbcUrl = databaseProperties.getProperty(JDBCSettings.URL_PROPERTY_KEY);
						databaseProperties.setProperty(JDBCSettings.URL_PROPERTY_KEY, jdbcUrl.replaceFirst("jdbc:mysql://", "jdbc:mariadb://"));

						future.complete(response);
					} catch (Exception e) {
						future.completeExceptionally(e);
					}
				}
			}).connect();
			int MAX_WAIT = 4 * 60 * 60;
			for (int i = 0; i < MAX_WAIT; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					websocket.disconnect();
				}
			}
			System.err.println("Timeout of {" + MAX_WAIT + "} reached. Disconnecting websocket to DB manager.");
			websocket.disconnect();
			throw new RuntimeException("Timeout of test reached");
		} catch (WebSocketException | IOException e) {
			future.completeExceptionally(e);
		}
	}

	public DBTestContext() {
		this(true, false);
	}

	/**
	 * Setup a dbtest context with the given setup flags.
	 *
	 * @param skipDBFileDownload
	 *            When true it will omit the download of dbfiles
	 */
	public DBTestContext(boolean skipDBFileDownload) {
		this(skipDBFileDownload, false);
	}

	/**
	 * Setup a dbtest context with the given setup flags.
	 *
	 * @param skipDBFileDownload
	 *            When true it will omit the download of dbfiles
	 * @param startDirtQueueWorker
	 *            When true it will start the dirt queue worker thread
	 */
	public DBTestContext(boolean skipDBFileDownload, boolean startDirtQueueWorker) {
		URL defaultTestConfigUrl = getClass().getResource(DEFAULT_CONFIG_NAME);
		if (defaultTestConfigUrl != null && StringUtils.isEqual(defaultTestConfigUrl.getProtocol(), "file")) {
			System.setProperty(ConfigurationValue.CONF_FILES.getSystemPropertyName(), defaultTestConfigUrl.getPath());
		}
		this.skipDBFileDownload = skipDBFileDownload;
		this.startDirtQueueWorker = startDirtQueueWorker;

		ResolverContextHandler.registerContext("contentnode-test-resolver", new NodeObjectResolverContext());
	}

	@Override
	protected void starting(Description description) {
		try {
			before();
			setFeatures(description);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Setup the test context
	 * @throws Throwable
	 */
	protected void before() throws Throwable {
		logger.debug("Before invoked. Setup starting..");
		String prefixHash = TestEnvironment.getRandomHash(10);
		gcnBasePath = new File(System.getProperty("java.io.tmpdir"), "random_" + prefixHash);
		gcnBasePath.mkdirs();

		logger.debug("Starting gcn setup script thread.");
		es = Executors.newSingleThreadExecutor();
		CompletableFuture<ManagerResponse> future = new CompletableFuture<>();
		es.submit(() -> {
			requestGCNDB(future);
		});

		try {
			connectionProperties = future.get(this.maxWait, TimeUnit.SECONDS).toProperties();
		} catch (TimeoutException e) {
			throw new NodeException("Waited too long for the connection properties from gcn-testdb-manager");
		}

		connectionProperties.setProperty("username", "root");
		connectionProperties.setProperty("passwd", "finger");
		setupGCN(gcnBasePath, connectionProperties);

		if (startDirtQueueWorker) {
			getContext().getContentNodeFactory().startDirtQueueWorker();
		}

		logger.debug("Setup done.");
	}

	/**
	 * Set Features according to the method annotations
	 *
	 * @param description
	 * @throws NodeException
	 */
	protected void setFeatures(Description description) throws NodeException {
		Class<?> testClass = description.getTestClass();
		if (testClass != null) {
			List<Class<?>> classes = new ArrayList<>();
			while (testClass != null) {
				classes.add(testClass);
				testClass = testClass.getSuperclass();
			}
			Collections.reverse(classes);
			for (Class<?> clazz : classes) {
				setFeatures(clazz.getAnnotation(GCNFeature.class));
			}
		}
		setFeatures(description.getAnnotation(GCNFeature.class));
	}

	/**
	 * If the given featureAnnotation is not null, set the annotated features to
	 * the test context
	 *
	 * @param featureAnnotation
	 *            feature annotation (may be null)
	 * @throws NodeException
	 */
	protected void setFeatures(GCNFeature featureAnnotation) throws NodeException {
		NodePreferences prefs = getContext().getNodeConfig().getDefaultPreferences();
		Optional.ofNullable(System.getenv(ENV_TEST_FEATURES)).ifPresent(env -> {
			for (String name : env.split(",")) {
				Feature feature = Feature.getByName(org.apache.commons.lang3.StringUtils.trim(name));
				if (feature != null) {
					prefs.setFeature(feature, true);
				}
			}
		});

		if (featureAnnotation != null) {
			for (Feature feature : featureAnnotation.set()) {
				if (!feature.isAvailable()) {
					throw new NodeException(String.format("Feature %s is not available", feature.getName()));
				}
				prefs.setFeature(feature, true);
			}
			for (Feature feature : featureAnnotation.unset()) {
				prefs.setFeature(feature, false);
			}

		}
	}

	@Override
	protected void finished(Description description) {
		logger.debug("After invoked. Cleanup starting..");
		PermissionStore.reset();
		LanguageProviderFactory.reset();
		try {
			if (gcnBasePath.exists()) {
				FileUtils.deleteDirectory(gcnBasePath);
			}
		} catch (IOException e) {
			logger.error("Error while cleaning gcnBasePath {" + gcnBasePath + "}.", e);
		}
		try {
			clearNodeCache();
		} catch (Exception e) {
			logger.error("Error while cleaning nodeCache", e);
		}
		try {
			getContext().getContentNodeFactory().stopDirtQueueWorker();
		} catch (NodeException e) {
			logger.error("Error while stopping dirtqueue worker.", e);
		}
		try {
			Transaction t = TransactionManager.getCurrentTransactionOrNull();
			if (t != null) {
				t.rollback();
				TransactionManager.setCurrentTransaction(null);
			}
			getContext().getNodeConfig().close();
		} catch (Exception e) {
			logger.debug("Error while cleaning up db connections/transaction.", e);
		}

		es.shutdownNow();

		if (dbUtils != null) {
			try {
				dbUtils.disconnectDatabase();
			} catch (SQLUtilException e) {
				logger.error("Error while disconnecting from database", e);
			}
		}

		logger.debug("Cleanup done.");
	}

	/**
	 * Properties that contain information about the database settings
	 *
	 * @return
	 */
	public Properties getConnectionProperties() {
		return connectionProperties;
	}

	/**
	 * Sets the given CR for the CR with id 1 in the cms database
	 *
	 * @param resource
	 * @throws JDBCMalformedURLException
	 * @throws SQLUtilException
	 */
	public void updateCRReference(ContentRepositoryResource resource) throws JDBCMalformedURLException, SQLUtilException {
		TestDatabase crTestDB = resource.getCRTestUtils().getTestDatabase();

		// make sure that the configured contentrepository uses the correct
		// connection data
		getDBSQLUtils().executeQueryManipulation("UPDATE contentrepository SET username = '" + crTestDB.getUsername() + "', password = '" + crTestDB.getPassword() + "', url = '"
				+ crTestDB.getJDBCUrl() + "' WHERE id = 1");
	}

	/**
	 * Setup various stuff for gcn. This method will also setup a text context
	 * and
	 *
	 * @param basePath
	 * @param cmsDatabaseSettings
	 * @throws NodeException
	 * @throws PortalCacheException
	 * @throws SQLUtilException
	 * @throws JDBCMalformedURLException
	 * @throws SQLException
	 * @throws IOException
	 * @throws JSchException
	 */
	private void setupGCN(File basePath, Properties cmsDatabaseSettings)
			throws NodeException, PortalCacheException, SQLUtilException, JDBCMalformedURLException, IOException, SQLException {

		Map<String, Object> overwriteConfig = new HashMap<>();
		MapPreferences mapPrefs = new MapPreferences(overwriteConfig);

		System.setProperty(AlohaRenderer.BUILD_TIMESTAMP, "TEST");
		System.setProperty(AlohaRenderer.ALOHA_EDITOR_BASE_URL_PARAM, String.format("/alohaeditor/%s", "TEST"));
		System.setProperty(ConfigurationValue.DBFILES_PATH.getSystemPropertyName(), new File(basePath, "dbfiles").getAbsolutePath());
		System.setProperty(ConfigurationValue.CACHE_PATH.getSystemPropertyName(), new File(basePath, "cache").getAbsolutePath());
		System.setProperty(ConfigurationValue.GIS_PATH.getSystemPropertyName(), new File(basePath, "gis").getAbsolutePath());
		System.setProperty(ConfigurationValue.CONTENT_PACKAGES_PATH.getSystemPropertyName(), new File(basePath, "content-packages").getAbsolutePath());
		System.setProperty(ConfigurationValue.LOGS_PATH.getSystemPropertyName(), new File(basePath, "logs").getAbsolutePath());
		System.setProperty(ConfigurationValue.PACKAGES_PATH.getSystemPropertyName(), new File(basePath, "packages").getAbsolutePath());
		System.setProperty(ConfigurationValue.PUBLISH_PATH.getSystemPropertyName(), new File(basePath, "publish").getAbsolutePath());

		System.setProperty(ConfigurationValue.NODE_DB_URL.getSystemPropertyName(),
				cmsDatabaseSettings.getProperty("url")
						+ "?characterEncoding=UTF8&includeInnodbStatusInDeadlockExceptions=true&useSSL=false");
		System.setProperty(ConfigurationValue.NODE_DB_USER.getSystemPropertyName(), cmsDatabaseSettings.getProperty("username"));
		System.setProperty(ConfigurationValue.NODE_DB_PASSWORD.getSystemPropertyName(), cmsDatabaseSettings.getProperty("passwd"));
		this.setLicenseKeySystemProperty();

		mapPrefs.set("contentnode.maxfilesize", "1048576");
		mapPrefs.set("contentnode.feature.symlink_files", "False");
		mapPrefs.set("contentnode.feature.persistentscheduler", "True");

		mapPrefs.set("mailhost", "mail.gentics.com");
		mapPrefs.set("contentnode.feature.inbox_to_email_optional", "false");
		mapPrefs.set("contentnode.feature.inbox_to_email", "false");
		mapPrefs.set("contentnode.feature.disable_versioned_publishing", "false");
		mapPrefs.set("contentnode.feature.tag_image_resizer", "false");

		mapPrefs.set("contentrepository_driverclass.mysql", "org.mariadb.jdbc.Driver");
		mapPrefs.set("contentrepository_driverclass.hsql", "org.hsqldb.jdbcDriver");

		mapPrefs.set("contentrepository_dummyquery.mysql", "SELECT 1");
		mapPrefs.set("contentrepository_dummyquery.oracle", "SELECT 1 FROM dual");
		mapPrefs.set("contentrepository_dummyquery.mssql", "SELECT 1");

		if (configModificator != null) {
			configModificator.accept(mapPrefs);
		}

		// clear the node caches
		PortalCache cache = PortalCache.getCache(NodeFactory.CACHEREGION);
		if (cache != null) {
			cache.clear();
		}

		dbUtils = SQLUtilsFactory.getSQLUtils(cmsDatabaseSettings);
		dbUtils.connectDatabase();

		// create the test context
		NodeConfigRuntimeConfiguration.overwrite(overwriteConfig);
		NodeConfigRuntimeConfiguration.reset();
		context = new ContentNodeTestContext(true, true);

		// check triggers, functions, stored procedures
		UdateChecker.check();

		// set the current language
		ContentNodeHelper.setLanguageId(1);

		NodePreferences prefs = context.getTransaction().getNodeConfig().getDefaultPreferences();

		java.io.File dbFilesDir = new java.io.File(ConfigurationValue.DBFILES_PATH.get());
		dbFilesDir.mkdirs();
		if (skipDBFileDownload) {
			logger.debug("Skipping dbfiles download since flag to skip this step was enabled.");
		} else {
			logger.debug("Adding dbfiles..");
			GCNSandboxHelper.downloadDBFiles(dbFilesDir);
			logger.debug("Adding dbfiles completed");
		}

		// get the pubdir
		pubDir = new java.io.File(ConfigurationValue.PUBLISH_PATH.get(), "pub");
		getContext().getContentNodeFactory().reloadConfiguration();

		for (Class<? extends InitJob> jobClass : initJobs) {
			try {
				jobClass.newInstance().execute();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new NodeException(e);
			}
		}
	}

	private void setLicenseKeySystemProperty() {
		URL licenseResource = getClass().getResource("license.key");

		if (licenseResource != null) {
			File licenseKeyFile = new File(licenseResource.getFile());
			System.setProperty(ConfigurationValue.KEYS_PATH.getSystemPropertyName(),
					licenseKeyFile.getParentFile().toString());
		}
	}

	/**
	 * Start a new transaction for the system user The transaction will be set
	 * as current transaction
	 *
	 * @return transaction
	 * @throws Exception
	 */
	public Transaction startSystemUserTransaction() throws NodeException {
		String SESSION_SECRET = "sidforsystem012";

		// create a dummy session for the systemuser
		int sessionId;
		try {
			sessionId = getDBSQLUtils().executeQueryInsert("INSERT INTO systemsession (secret, user_id, ip, agent, cookie, since, language, val) VALUES ('" + SESSION_SECRET + "', "
					+ 1 + ", 'localhost', 'JUnit Test', 0, unix_timestamp(), 1, '')");
			String SESSION_TOKEN = new SessionToken(sessionId, SESSION_SECRET).toString();
			// now we create a new transaction for the user
			Transaction t = context.getContentNodeFactory().startTransaction(SESSION_TOKEN, true);
			context.setTransaction(t);
			return t;
		} catch (SQLException e) {
			throw new NodeException("Error while starting systemuser transaction", e);
		}
	}

	/**
	 * Start a systemuser transaction with the given timestamp
	 *
	 * @param timestamp
	 *            timestamp
	 * @param stopCurrent
	 *            true to stop the current transaction
	 * @return transaction
	 * @throws Exception
	 */
	public Transaction startSystemUserTransaction(int timestamp, boolean stopCurrent) throws NodeException {
		if (stopCurrent) {
			Transaction current = TransactionManager.getCurrentTransactionOrNull();
			if (current != null) {
				current.commit();
			}
		}
		Transaction t = startSystemUserTransaction();
		context.setTransaction(t);
		t.setTimestamp(timestamp * 1000L);
		return t;
	}

	/**
	 * Start a new transaction with a user that has permissions
	 *
	 * @throws Exception
	 */
	public Transaction startTransactionWithPermissions(boolean closeCurrent) throws Exception {
		String SESSION_SECRET = "sidwithperms012";

		// create a dummy session for the user
		int sessionId = getDBSQLUtils().executeQueryManipulation("INSERT INTO systemsession (secret, user_id, ip, agent, cookie, since, language, val) VALUES ('" + SESSION_SECRET
				+ "', " + USER_WITH_PERMS + ", 'localhost', 'JUnit Test', 0, unix_timestamp(), 1, '')");
		String SESSION_TOKEN = new SessionToken(sessionId, SESSION_SECRET).toString();
		if (closeCurrent) {
			Transaction currentTransaction = TransactionManager.getCurrentTransactionOrNull();
			if (currentTransaction != null) {
				currentTransaction.commit();
			}
		}
		// now we create a new transaction for the user
		Transaction t = context.getContentNodeFactory().startTransaction(SESSION_TOKEN, true);
		context.setTransaction(t);

		RenderType renderType = RenderType.getDefaultRenderType(context.getNodeConfig().getDefaultPreferences(), RenderType.EM_LIVEPREVIEW, t.getSessionId(), 0);

		renderType.setRenderUrlFactory(new StaticUrlFactory(RenderUrl.LINK_AUTO, RenderUrl.LINK_AUTO, null));
		t.setRenderType(renderType);

		return TransactionManager.getCurrentTransaction();
	}

	/**
	 * Start a new transaction with a user that has NO permissions
	 *
	 * @throws Exception
	 */
	public Transaction startTransactionWithoutPermissions(boolean closeCurrent) throws Exception {
		int SESSION_ID = 1234;
		String SESSION_SECRET = "sidwithoutperms";

		// create a dummy session for the user
		getDBSQLUtils().executeQueryManipulation("DELETE FROM systemsession where id = " + SESSION_ID);
		getDBSQLUtils().executeQueryManipulation("INSERT INTO systemsession (id, secret, user_id, ip, agent, cookie, since, language, val) VALUES (" + SESSION_ID + ", '"
				+ SESSION_SECRET + "', " + USER_WITHOUT_PERMS + ", 'localhost', 'JUnit Test', 0, unix_timestamp(), 1, '')");
		if (closeCurrent) {
			Transaction currentTransaction = TransactionManager.getCurrentTransactionOrNull();
			if (currentTransaction != null) {
				context.getTransaction().commit();
			}
		}
		// now we create a new transaction for the user
		Transaction t = context.getContentNodeFactory().startTransaction(new SessionToken(SESSION_ID, SESSION_SECRET).toString(), true);
		context.setTransaction(t);

		return t;
	}

	/**
	 * Returns the sql utils for the node db
	 *
	 * @return
	 */
	public SQLUtils getDBSQLUtils() {
		return dbUtils;
	}

	public File getPubDir() {
		return pubDir;
	}

	/**
	 * Returns the content node testcontext for this test
	 *
	 * @return
	 * @throws NodeException
	 */
	public ContentNodeTestContext getContext() throws NodeException {
		if (context == null) {
			throw new NodeException("Couldn't get ContentNodeTestContext (Database not reachable?)");
		}

		return context;
	}

	/**
	 * Wait for the dirtqueue worker to do it's job (handle all dirtqueue
	 * entries). This method fails, when we have to wait longer than
	 * {@link #DIRTQUEUEWORKER_WAITTIMEOUT} ms.
	 *
	 * @throws NodeException
	 */
	public void waitForDirtqueueWorker() throws NodeException {
		// wait until no more entry in dirtqueue exists (all events have been
		// handled)
		context.startDirtQueueWorker();
		try {
			int numDirtQueueEntries = 0;
			long waitingStart = System.currentTimeMillis();

			while ((System.currentTimeMillis() - waitingStart) < DIRTQUEUEWORKER_WAITTIMEOUT && (numDirtQueueEntries = dbUtils.getNumRows("SELECT * FROM dirtqueue")) > 0) {
				// wait a bit
				Thread.sleep(100);
			}

			// now check whether there were still some dirtqueue entries left
			assertEquals("Check number of dirtqueue entries after waiting " + (System.currentTimeMillis() - waitingStart) + " ms:", 0, numDirtQueueEntries);
		} catch (SQLUtilException | InterruptedException e) {
			throw new NodeException(e);
		} finally {
			context.stopDirtQueueWorker();
		}
	}

	/**
	 * Truncates the given table.
	 *
	 * @param tableName
	 *            Name of the table that should be truncated.
	 * @throws SQLException
	 * @throws NodeException
	 */
	public void truncateTable(String tableName) throws SQLException, NodeException {
		Connection conn = getContext().getTransaction().getConnection();
		Statement st = null;

		try {
			st = conn.createStatement();
			st.executeUpdate("truncate " + tableName);
		} catch (SQLException e) {
			throw e;
		} finally {
			if (st != null) {
				st.close();
			}
		}
	}

	/**
	 * Marks all pages as published
	 *
	 * @throws SQLException
	 * @throws NodeException
	 */
	public void markAllPagesAsPublished() throws SQLException, NodeException {
		DBUtils.update("UPDATE page SET online = ?", 1);
	}

	/**
	 * Run a publish process with the given timestamp (in s)
	 *
	 * @param timestamp
	 *            timestamp in s
	 * @throws Exception
	 */
	public void publish(int timestamp) throws Exception {
		// run a publish process
		PublishInfo info = getContext().publish(false, true, timestamp * 1000L);
		assertTrue("Publish process failed", info.getReturnCode() == PublishInfo.RETURN_CODE_SUCCESS);
	}

	/**
	 * Run a publish process
	 *
	 * @param dirtAll
	 *            true when everything shall be dirted before, false if not
	 * @throws Exception
	 */
	public void publish(boolean dirtAll) throws Exception {
		assertEquals("Check publish status", PublishInfo.RETURN_CODE_SUCCESS, context.publish(dirtAll).getReturnCode());
	}

	/**
	 * Clears the Content.Node cache
	 *
	 * @throws IOException
	 * @throws NodeException
	 */
	protected void clearNodeCache() throws NodeException, IOException {
		if (context != null) {
			context.clearNodeObjectCache();
		}
	}

	/**
	 * Start a new transaction with the given timestamp. Any currently open
	 * transaction will be committed and closed first. The new transaction will
	 * be set as the current transaction.
	 *
	 * @param timestamp
	 *            transaction timestamp (in s)
	 * @return transaction
	 * @throws NodeException
	 */
	public Transaction startTransaction(int timestamp) throws NodeException {
		getContext().startTransaction();
		Transaction t = TransactionManager.getCurrentTransaction();
		context.setTransaction(t);
		t.setTimestamp(timestamp * 1000L);
		t.setPublishCacheEnabled(false);
		return t;
	}

	/**
	 * Check if the correct amount of pages were dirted
	 *
	 * @throws SQLUtilException
	 */
	public void checkDirtedPages(int dirtedPagesBeforeModification, int[] ids) throws Exception {
		List<Integer> dirtedPageIds = PublishQueue.getDirtedObjectIds(Page.class, false, null);

		for (int id : ids) {
			assertTrue("Check that correct pages were dirted. Expected: {" + dirtedPageIds + "}, to contain {" + id + "}", dirtedPageIds.contains(id));
		}

		assertEquals("Check # of dirted pages", ids.length, dirtedPageIds.size() - dirtedPagesBeforeModification);
	}

	/**
	 * Set maximum wait time in seconds.
	 * The default value is {@link DBTestContext#DEFAULT_MAX_WAIT}
	 * @param maxWait maximum wait time in seconds
	 * @return fluent API
	 */
	public DBTestContext setMaxWait(int maxWait) {
		this.maxWait = maxWait;
		return this;
	}

	/**
	 * Omit execution of the given init job
	 * @param initJobClass init job class
	 * @return fluent API
	 */
	public DBTestContext omit(Class<? extends InitJob> initJobClass) {
		initJobs.remove(initJobClass);
		return this;
	}

	/**
	 * Set a configuration modificator. The given consumer will be called with an instance of {@link MapPreferences}, which can be modified
	 * @param configModificator modificator instance
	 * @return fluent API
	 */
	public DBTestContext config(Consumer<MapPreferences> configModificator) {
		this.configModificator = configModificator;
		return this;
	}

	/**
	 * Add the given file to the list of config files. The config files will be read after the default config from {@link #DEFAULT_CONFIG_NAME} and in the order,
	 * they were added. Configurations will be merged.
	 * @param filePath path to the file
	 * @return fluent API
	 */
	public DBTestContext config(String filePath) {
		if (!StringUtils.isEmpty(filePath)) {
			String propertyName = ConfigurationValue.CONF_FILES.getSystemPropertyName();
			String current = System.getProperty(propertyName, "");
			if (StringUtils.isEmpty(current)) {
				System.setProperty(propertyName, filePath);
			} else {
				System.setProperty(propertyName, String.format("%s, %s", current, filePath));
			}
		}
		return this;
	}

	/**
	 * Get the base path for the gcn installation
	 * @return base path
	 */
	public File getGcnBasePath() {
		return gcnBasePath;
	}

	/**
	 * Update the configuration from the given file (which can optionally be modified with the given modified)
	 * @param file config file
	 * @param configModifier optional config modifier
	 * @throws IOException
	 * @throws NodeException
	 */
	public void updateConfig(File file, Consumer<MapPreferences> configModifier) throws IOException, NodeException {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setDefaultMergeable(true);
		@SuppressWarnings("unchecked")
		MapPreferences data = new MapPreferences(mapper.readValue(file, Map.class));
		if (configModifier != null) {
			configModifier.accept(data);
		}

		File configFile = new File(getGcnBasePath(), file.getName());
		FileUtils.write(configFile, mapper.writeValueAsString(data.toMap()));

		System.setProperty(ConfigurationValue.CONF_FILES.getSystemPropertyName(), configFile.getAbsolutePath());
		NodeConfigRuntimeConfiguration.getDefault().reloadConfiguration();
	}
}
