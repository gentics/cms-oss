package com.gentics.contentnode.etc;

import static com.gentics.contentnode.runtime.ConfigurationValue.NODE_DB_DRIVER_CLASS;
import static com.gentics.contentnode.runtime.ConfigurationValue.NODE_DB_PASSWORD;
import static com.gentics.contentnode.runtime.ConfigurationValue.NODE_DB_URL;
import static com.gentics.contentnode.runtime.ConfigurationValue.NODE_DB_USER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.changelog.ChangeLogEntry;
import com.gentics.contentnode.changelog.ChangeLogHandler;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.factory.object.UserLanguageFactory;
import com.gentics.contentnode.i18n.ContentNodeLanguageProviderWrapper;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.publish.PublishQueueStats;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RendererFactory;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.scheduler.PersistentSchedulerProperties;
import com.gentics.contentnode.scheduler.SchedulerUtils;
import com.gentics.contentnode.scheduler.SimpleScheduler;
import com.gentics.contentnode.servlets.UdateChecker;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DB.DBTrx;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.genericexceptions.UnavailableException;
import com.gentics.lib.i18n.LanguageProviderFactory;
import com.gentics.lib.io.FileRemover;
import com.gentics.lib.log.NodeLogger;

/**
 * Node configuration based on the properties config returned by
 * Gentics Content.Node do=24
 * 
 * @author herbert
 */
public class PropertyNodeConfig implements NodeConfig {
	/**
	 * ServiceLoader for available {@link LifecycleService}s
	 */
	protected final static ServiceLoader<LifecycleService> lifecycleServiceLoader = ServiceLoader
			.load(LifecycleService.class);

	private NodeLogger logger = NodeLogger.getNodeLogger(PropertyNodeConfig.class);

	private Map<String, DataSource> datasources = new HashMap<String, DataSource>();
	private Map<String, GenericObjectPool<Object>> connectionPools = new HashMap<>();

	private NodePreferences defPrefs;

	private Map<String, SQLHandle> sqlHandles = new HashMap<String, SQLHandle>();

	/**
	 * Last SQLException, which was thrown while getting a connection
	 */
	private SQLException lastException;

	/**
	 * Counter for consecutive thrown identical SQLExceptions
	 */
	private int counter;

	/**
	 * Timestamp (in ms), when the exception was thrown the first time
	 */
	private long timestamp;

	/**
	 * Default value for connection pool: {@value #DEFAULT_MAX_POOLSIZE}
	 */
	public final static int DEFAULT_MAX_POOLSIZE = 100;

	/**
	 * Default wait time in ms for connection pool: {@value #DEFAULT_MAX_WAIT}
	 */
	public final static int DEFAULT_MAX_WAIT = -1;

	/**
	 * Default max idle connections in pool: {@value #DEFAULT_MAX_IDLE}
	 */
	public final static int DEFAULT_MAX_IDLE = 10;

	/**
	 * How many consecutively thrown exceptions will be logged in full (including stack trace)
	 */
	public final static int FULL_LOG_COUNT = 10;

	/**
	 * Internal key for the pooled connections to the backend databse
	 */
	public static final String NODE_DB_POOLED_KEY = "nodedb";

	/**
	 * Internal key for the non-pooled connections to the backend databse
	 */
	public static final String NODE_DB_KEY = "nodedb|nonpooled";

	/**
	 * Common prefix for all db related settings
	 */
	public static final String SETTINGS_PREFIX = "db.settings.";

	public static final String DB_SETTINGS_POOL_WHEN_EXHAUSTED_ACTION = SETTINGS_PREFIX + "pool_whenExhaustedAction";

	public static final String DB_SETTINGS_POOL_MAX_IDLE = SETTINGS_PREFIX + "pool_maxIdle";

	public static final String DB_SETTINGS_POOL_MAX_WAIT = SETTINGS_PREFIX + "pool_maxWait";

	public static final String DB_SETTINGS_POOL_SIZE_MAX = SETTINGS_PREFIX + "pool_size_max";

	private StdSchedulerFactory schedulerFactory = null;

	/**
	 * Constructor for the PropertyNodeConfig
	 * When using this constructor the PersistentScheduler will be initialized.
	 * @param props Properties for the NodeConfig
	 */
	public PropertyNodeConfig(Map<String, Object> props) throws NodeException {
		setProperties(props);
	}

	/**
	 * Initialize
	 * @throws NodeException
	 */
	public void init() throws NodeException {
		// do initial setup of the database (initial dump, changelog)
		tryConnectToDatabase(1_000, ObjectTransformer.getInt(ConfigurationValue.NODE_DB_INIT_TIMEOUT.get(), 60_000));
		createDatabaseIfEmpty();
		executeChangelog();

		Operator.start(ObjectTransformer.getInt(defPrefs.getProperty("background_thread_poolsize"), 10));

		/*
		 * Because reloading the configuration stops the simple scheduler, these jobs have to be restarted:
		 * 	1. Scheduler factory.
		 * 	2. Cleaning old sessions.
		 */
		Optional<SchedulerFactory> factory = Optional.ofNullable(ContentNodeFactory.getInstance())
			.map(ContentNodeFactory::getFactory)
			.map(f -> f.getObjectFactory(SchedulerSchedule.class))
			.map(o -> (SchedulerFactory) o);

		// Restart the scheduler.
		if (factory.isPresent()) {
			factory.get().startScheduler();
		}

		// Restart the cleaning of old session.s
		Session.scheduleSessionCleaning();

		PublishQueueStats.get().init(ObjectTransformer.getLong(defPrefs.getProperty("publish_queue_stats.refresh_delay"), 60_000));

		initializePersistentScheduler();
		registerLanguageProvider();

		// initialize the RendererFactory here
		RendererFactory.initRenderers(getDefaultPreferences());

		// fix required permissions on activated features
		fixRequiredFeaturePermissions();

		// initialize periodic synchronization for devtools
		if (getDefaultPreferences().isFeature(Feature.DEVTOOLS)) {
			Synchronizer.start();
		} else {
			Synchronizer.stop();
		}

		// initialize the PermissionStore
		Trx.operate(() -> PermissionStore.initialize());

		// initialize lifecycleServices
		lifecycleServiceLoader.forEach(LifecycleService::start);
	}

	/**
	 * Try to connect to backend database. Retry, if connection fails
	 * @param waitMs wait time in milliseconds before re-trying
	 * @param maxWaitMs maximum wait time in milliseconds, before giving up.
	 * @throws NodeException
	 */
	protected void tryConnectToDatabase(int waitMs, int maxWaitMs) throws NodeException {
		DBHandle handle = getSQLHandle(true).getDBHandle();

		long start = System.currentTimeMillis();
		boolean connectionEstablished = false;

		logger.info("Trying to establish connection to backend database");
		do {
			try (DBTrx trx = new DBTrx(handle)) {
				connectionEstablished = true;
			} catch (SQLException e) {
				long waited = System.currentTimeMillis() - start;
				if (waited > maxWaitMs) {
					throw new NodeException(String.format("Failed to establish connection to backend database in %d ms", maxWaitMs), e);
				} else {
					try {
						logger.debug(String.format("Failed to establish connection to backend database. Waiting %d ms before re-try", waitMs));
						Thread.sleep(waitMs);
					} catch (InterruptedException e1) {
						throw new NodeException("Interrupted while trying to establish connection to backend database", e1);
					}
				}
			}
		} while(!connectionEstablished);
	}

	/**
	 * Create the initial database structure, if the database does not contain any tables
	 * @throws NodeException
	 */
	protected void createDatabaseIfEmpty() throws NodeException {
		DBHandle handle = getSQLHandle(true).getDBHandle();

		boolean tablesFound = false;
		try (DBTrx trx = new DBTrx(handle)) {
			DatabaseMetaData metaData = trx.getConnection().getMetaData();
			try (ResultSet rs = metaData.getTables(null, handle.getDbSchema(), null, null)) {
				while (rs.next()) {
					logger.debug(String.format("Found table %s", rs.getString("TABLE_NAME")));
					tablesFound = true;
				}
			}

			if (!tablesFound) {
				NodeConfigRuntimeConfiguration.runtimeLog.info("No tables found in database, creating initial table structure");
				StringBuilder cmd = new StringBuilder();
				String line = null;

				try (InputStream in = UdateChecker.class.getResourceAsStream("/dumps/backend.sql");
						InputStreamReader inReader = new InputStreamReader(in, "UTF-8");
						BufferedReader bufferedReader = new BufferedReader(inReader)) {

					// disable foreign key checks
					DB.update(handle, "SET FOREIGN_KEY_CHECKS = 0");

					try {
						while ((line = bufferedReader.readLine()) != null) {
							cmd.append(line);

							if (cmd.toString().endsWith(";")) {
								DB.update(handle, cmd.toString());
								cmd = new StringBuilder();
							}
						}

						// check for final command, which was not yet executed
						if (cmd.length() != 0) {
							DB.update(handle, cmd.toString());
						}
					} finally {
						// enable foreign key checks
						DB.update(handle, "SET FOREIGN_KEY_CHECKS = 1");
					}
				} catch (IOException e) {
					throw new NodeException("Error while reading db dump file", e);
				} catch (SQLException e) {
					throw new NodeException("Error while creating initial table structure", e);
				}
			}
		} catch (SQLException e) {
			throw new NodeException("Error while checking database", e);
		}
	}

	/**
	 * Execute the changelog
	 * @throws NodeException
	 */
	protected void executeChangelog() throws NodeException {
		DBHandle handle = getSQLHandle(true).getDBHandle();
		List<ChangeLogEntry> entries = null;

		try (DBTrx trx = new DBTrx(handle)) {
			entries = ChangeLogHandler.getSQLQueriesFromChangeLog(new String[] {""}, handle);
		} catch (SQLException e) {
			throw new NodeException("Error while getting changelog", e);
		}

		for (ChangeLogEntry change : entries) {
			try (DBTrx trx = new DBTrx(handle)) {
				change.apply(handle);
			} catch (SQLException e) {
				throw new NodeException(String.format("Error while executing change {%s}", change), e);
			}
		}
	}

	/**
	 * Fix required permissions for activated features
	 */
	protected void fixRequiredFeaturePermissions() {
		for (Feature f : Feature.values()) {
			if (getDefaultPreferences().isFeature(f)) {
				try {
					f.fixRequiredPermissions();
				} catch (NodeException e) {
					logger.error("Error while fixing required permission for feature " + f, e);
				}
			}
		}
	}

	/**
	 * Set the configuration properties
	 * @param props properties as nested maps
	 */
	public void setProperties(Map<String, Object> props) {
		defPrefs = new ThreadLocalPropertyPreferences(new MapPreferences(props));
	}

	/**
	 * Initializes the Persistent Scheduler
	 */
	public void initializePersistentScheduler() throws UnavailableException {
		schedulerFactory = new StdSchedulerFactory();
		try {
			PersistentSchedulerProperties schedulerProps = new PersistentSchedulerProperties(defPrefs);

			schedulerFactory.initialize(schedulerProps);
			schedulerFactory.getScheduler().start();
		} catch (SchedulerException e) {
			throw new UnavailableException("Could not initialize scheduler", e);
		}
	}

	/**
	 * Register the Content.Node specific language provider wrapper that is used the retrieve the current language provider.
	 * 
	 * @throws NodeException
	 */
	public void registerLanguageProvider() throws NodeException {
		// initialize ui languages
		Trx.operate(() -> UserLanguageFactory.init());

		LanguageProviderFactory.reset();
		LanguageProviderFactory.getInstance().registerProviderWrapper(new ContentNodeLanguageProviderWrapper());
		logger.debug("Registered Content.Node specific LanguageProviderWrapper");
	}

	/**
	 * Get the scheduler which uses a JDBCJobstore to save all the jobstate in the database
	 * @return The Scheduler or null if non started
	 * @throws SchedulerException
	 */
	public Scheduler getPersistentScheduler() throws NodeException {
		if (schedulerFactory == null) {
			return null;
		}
		try {
			return schedulerFactory.getScheduler();
		} catch (SchedulerException e) {
			throw new NodeException("Could not get Scheduler from factory", e);
		}
	}

	/**
	 * Called when reloading the configuration before creating a new PropertyNodeConfig. The close call will also close all connections in the datasource pool.
	 */
	public void close() throws NodeException {
		// stop all lifecycle services
		lifecycleServiceLoader.forEach(LifecycleService::stop);

		MeshPublisher.shutdown();
		Synchronizer.terminate();
		FileRemover.shutdown();
		Operator.shutdown();
		PublishQueueStats.get().shutdown();
		SimpleScheduler.shutdown();
		SchedulerUtils.forceShutdown(getPersistentScheduler());
		schedulerFactory = null;
		datasources.clear();
		for (GenericObjectPool<Object> pool : connectionPools.values()) {
			try {
				pool.close();
			} catch (Exception e) {
				logger.debug("Error while closing connection pool {" + pool + "}");
			}
		}
		connectionPools.clear();

		for (SQLHandle handle : sqlHandles.values()) {
			handle.close();
		}
		sqlHandles.clear();
	}

	@Override
	public Connection getConnection(boolean useConnectionPool) throws NodeException {
		return createConnectionWithPrefix(getSQLHandle(useConnectionPool).getDBHandle(), useConnectionPool);
	}

	@Override
	public synchronized SQLHandle getSQLHandle(boolean useConnectionPool) {
		String keyName = useConnectionPool ? NODE_DB_POOLED_KEY : NODE_DB_KEY;

		if (!sqlHandles.containsKey(keyName)) {
			SQLHandle handle = createSQLHandle(useConnectionPool);

			if (handle != null) {
				sqlHandles.put(keyName, handle);
			}
		}

		return sqlHandles.get(keyName);
	}

	/**
	 * Create the sql handle with the given configuration name
	 * @param name config name
	 * @param useConnectionPool flag for usage of database connection pooling
	 * @return sql handle
	 */
	private SQLHandle createSQLHandle(boolean useConnectionPool) {
		Map<String, String> handleProperties = new HashMap<String, String>();

		// check for existence of configuration values
		String url = NODE_DB_URL.get();

		handleProperties.put("url", url);
		handleProperties.put("driverClass", NODE_DB_DRIVER_CLASS.get());
		handleProperties.put("username", NODE_DB_USER.get());
		handleProperties.put("passwd", NODE_DB_PASSWORD.get());
		if (useConnectionPool) {
			handleProperties.put("type", "jdbc");
		} else {
			handleProperties.put("type", "nonpoolingjdbc");
		}

		SQLHandle handle = new SQLHandle(NODE_DB_KEY);

		handle.init(handleProperties);
		return handle;
	}

	/**
	 * Get the contentrepository configuration from the database.
	 * @return map of contentrepositories
	 * @throws NodeException
	 */
	private synchronized Map<Integer, ContentMap> getMapConfiguration() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Map<Integer, ContentMap> mapConfiguration = new HashMap<Integer, ContentMap>();

		List<ContentRepository> crs = t.getObjects(ContentRepository.class,
				DBUtils.select(
						"SELECT c.id FROM contentrepository c LEFT JOIN node n ON c.id = n.contentrepository_id WHERE c.crtype IN ('cr', 'mccr') AND n.publish_contentmap = 1 AND n.disable_publish = 0 GROUP BY c.id",
						DBUtils.IDS));

		for (ContentRepository cr : crs) {
			switch (cr.getCheckStatus()) {
			case ContentRepository.DATACHECK_STATUS_ERROR:
				// the last check on this contentrepository failed: print a warning
				t.getRenderResult().warn(ContentMap.class,
						"ContentRepository {" + cr.getName() + "} (id " + cr.getId() + ") configuration had an error when it was checked the last time");
				break;

			case ContentRepository.DATACHECK_STATUS_UNCHECKED:
				// contentrepository is unchecked: print a warning
				t.getRenderResult().warn(ContentMap.class, "Using unchecked ContentRepository {" + cr.getName() + "} (id " + cr.getId() + ") for publishing");
				break;

			default:
				break;
			}
			ContentMap map = cr.getContentMap();
			mapConfiguration.put(cr.getId(), map);
		}
		PortalConnectorFactory.unregisterUnusedHandles();

		return mapConfiguration;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.etc.NodeConfig#getContentMaps()
	 */
	public Collection<ContentMap> getContentMaps() throws NodeException {
		// get the contentmaps from the database
		Map<Integer, ContentMap> mapConfiguration = getMapConfiguration();

		return mapConfiguration.values();
		// if(contentMaps == null) { parseMapConfiguration(); }
		// return contentMaps.values();
	}

	/**
	 * Creates a new JDBC connection based on the settings of the specifies
	 * properties.
	 * @param dbHandle DBHandle used to get a dummy statement
	 * @param useConnectionPool true when connection pooling shall be used,
	 *        false when a new connection shall be created upon every invocation
	 * @return a new jdbc connection.
	 * @throws NodeException 
	 */
	private Connection createConnectionWithPrefix(DBHandle dbHandle, boolean useConnectionPool) throws NodeException {
		String url = null;
		String driverClass = null;

		try {
			if (useConnectionPool) {
				DataSource ds = (DataSource) datasources.get(NODE_DB_POOLED_KEY);

				if (ds != null) {
					Connection c = ds.getConnection();
					resetException();
					return c;
				}
			}

			url = NODE_DB_URL.get();
			driverClass = NODE_DB_DRIVER_CLASS.get();

			String login = NODE_DB_USER.get();
			String passwd = NODE_DB_PASSWORD.get();
			int poolSize = ObjectTransformer.getInt(defPrefs.getProperty(DB_SETTINGS_POOL_SIZE_MAX), DEFAULT_MAX_POOLSIZE);
			byte whenExhaustedAction = parseWhenExhaustedPoolAction(defPrefs);
			int maxWait = ObjectTransformer.getInt(defPrefs.getProperty(DB_SETTINGS_POOL_MAX_WAIT), DEFAULT_MAX_WAIT);
			int maxIdle = ObjectTransformer.getInt(defPrefs.getProperty(DB_SETTINGS_POOL_MAX_IDLE), DEFAULT_MAX_IDLE);

			try {
				Class.forName(driverClass);
			} catch (RuntimeException e) {
				logger.fatal("Error while initialising driver class {" + driverClass + "}", e);
				return null;
			}

			Connection c = null;
			if (useConnectionPool) {
				GenericObjectPool<Object> connectionPool = new GenericObjectPool<>(null, poolSize, whenExhaustedAction,
						maxWait, maxIdle);
				connectionPool.setTestOnBorrow(true);
				connectionPools.put(NODE_DB_POOLED_KEY, connectionPool);
				ConnectionFactory connectionFactory = null;
				connectionFactory = new DriverManagerConnectionFactory(url, login, passwd);

				// create the connection factory
				new PoolableConnectionFactory(connectionFactory, connectionPool, null, dbHandle.getDummyStatement(), false, true);

				// and the datasource
				PoolingDataSource poolingds = new PoolingDataSource(connectionPool);

				// Connection conn = DriverManager.getConnection(url, login, passwd);
				datasources.put(NODE_DB_POOLED_KEY, poolingds);
				c = poolingds.getConnection();
			} else {
				c = DriverManager.getConnection(url, login, passwd);
			}

			resetException();
			return c;
		} catch (SQLException e) {
			boolean logShort = handleException(e);

			if (logShort) {
				logger.fatal(
						"Error while trying to create connection to backend database with url {" + url + "}.");
				logger.fatal(String.format("Error repeated %d times (first time %s ago)", counter,
						Duration.ofMillis(System.currentTimeMillis() - timestamp).toString()));
			} else {
				logger.fatal(
						"Error while trying to create connection to backend database with url {" + url + "}",
						e);
			}
			Transaction t = TransactionManager.getCurrentTransaction();

			if (t != null) {
				RenderResult result = t.getRenderResult();

				if (result != null) {
					result.fatal(PropertyNodeConfig.class, "Error while trying to create connection to backend database with url {" + url + "}", e);
				}
			}
			return null;
		} catch (ClassNotFoundException e) {
			logger.fatal("Unable to load driver class. {" + driverClass + "}", e);
			return null;
		}
	}

	/**
	 * Returns the matching byte value for the whenExhaustedAction setting. Default value is WHEN_EXHAUSTED_GROW
	 *
	 * @param preferences
	 * @return the parsed connection pool action value
	 */
	private byte parseWhenExhaustedPoolAction(NodePreferences preferences) {
		String actionString =  preferences.getProperty(DB_SETTINGS_POOL_WHEN_EXHAUSTED_ACTION);
		if ("GROW".equalsIgnoreCase(actionString)) {
			return GenericObjectPool.WHEN_EXHAUSTED_GROW;
		} else if ("BLOCK".equalsIgnoreCase(actionString)) {
			return GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
		} else if ("FAIL".equalsIgnoreCase(actionString)) {
			return GenericObjectPool.WHEN_EXHAUSTED_FAIL;
		}
		return GenericObjectPool.WHEN_EXHAUSTED_GROW;
	}

	/**
	 * Reset the last caught exception (called when a connection could be created successfully)
	 */
	private synchronized void resetException() {
		lastException = null;
		counter = 0;
		timestamp = 0L;
	}

	/**
	 * Count the number of identical (same SQLState and ErrorCode) exceptions, which are thrown consecutively
	 * @param e sql exception
	 * @return true if the number of exceptions exceeds {@link PropertyNodeConfig#FULL_LOG_COUNT}
	 */
	private synchronized boolean handleException(SQLException e) {
		boolean logShort = false;
		boolean exceptionFound = false;
		if (lastException != null) {
			if (lastException.getErrorCode() == e.getErrorCode()
					&& StringUtils.equals(lastException.getSQLState(), e.getSQLState())) {
				exceptionFound = true;
				// exception was caught again
				counter++;

				if (counter > FULL_LOG_COUNT) {
					logShort = true;
				}
			}
		}

		if (!exceptionFound) {
			lastException = e;
			counter = 1;
			timestamp = System.currentTimeMillis();
		}

		return logShort;
	}

	public void returnConnection(Connection connection) {
		if (connection == null) {
			return;
		}
		try {
			connection.close();
		} catch (SQLException e) {
			logger.error("Error while closing connection");
		}
	}

	public NodePreferences getDefaultPreferences() {
		return defPrefs;
	}

	public NodePreferences getUserPreferences(int userId) {
		return defPrefs;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodeConfig#overwriteDefaultPreferences(com.gentics.lib.etc.NodePreferences)
	 */
	public void overwriteDefaultPreferences(NodePreferences nodePreferences) {
		this.defPrefs = nodePreferences;
	}
}
