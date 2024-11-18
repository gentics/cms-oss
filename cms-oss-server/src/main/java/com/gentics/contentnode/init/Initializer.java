package com.gentics.contentnode.init;

import static com.gentics.contentnode.factory.Trx.operate;

import com.gentics.api.lib.cache.PortalCacheException;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.ResolverContextHandler;
import com.gentics.contentnode.cache.GCMSCache;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.job.FixPageVersionsJob;
import com.gentics.contentnode.parser.ContentRenderer;
import com.gentics.contentnode.parser.function.RenderFunction;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishHandlerStore;
import com.gentics.contentnode.resolving.NodeObjectResolverContext;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.servlet.CNInvokerQueue;
import com.gentics.contentnode.servlets.UdateChecker;
import com.gentics.contentnode.validation.ValidatorFactory;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.expressionparser.functions.FunctionRegistry;
import com.gentics.lib.expressionparser.functions.FunctionRegistryException;
import com.gentics.lib.jaxb.JAXBHelper;
import com.gentics.lib.log.NodeLogger;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang3.StringUtils;
import org.apache.jcs.engine.control.CompositeCacheManager;

/**
 * Static initializer
 */
public class Initializer {
	/**
	 * logger
	 */
	protected static NodeLogger logger = null;


	/**
	 * Singleton instance
	 */
	protected static Initializer instance = new Initializer();

	static {
		String nodelogPath = ConfigurationValue.CONF_PATH.get() + "nodelog.yml";
		IOException caught = null;
		try {
			createConfigurationFileFromResourceIfMissing(new File(nodelogPath), "/nodelog.yml");
		} catch (IOException e) {
			// store the caught exception, because we cannot log it before the logger is initialized
			caught = e;
		}
		// configure the configuration factory and file
		System.setProperty("log4j2.configurationFactory", CustomConfigFactory.class.getName());
		System.setProperty("log4j2.configurationFile", nodelogPath);
		logger = NodeLogger.getNodeLogger(Initializer.class);

		// if an exception was caught before, log it now
		if (caught != null) {
			logger.error(String.format("Error while writing logger configuration to file %s", nodelogPath), caught);
		}
	}

	/**
	 * Get the singleton instance
	 * @return initializer instance
	 */
	public static Initializer get() {
		return instance;
	}


	/**
	 * Create the configuration file, if it is missing
	 * @param file configuration file
	 * @param initialContent initial content of the created file
	 * @throws IOException
	 */
	private static void createConfigurationFileIfMissing(File file, String initialContent) throws IOException {
		if (!file.exists() && !StringUtils.isBlank(initialContent)) {
			FileUtils.write(file, initialContent, "UTF-8");
		}
	}


	/**
	 * Create the configuration file, if it is missing
	 * @param file configuration file
	 * @param resource classpath resource which shall be used as initial content
	 * @throws IOException
	 */
	private static void createConfigurationFileFromResourceIfMissing(File file, String resource) throws IOException {
		if (!file.exists()) {
			URL url = Initializer.class.getResource(resource);
			if (url != null) {
				file.getParentFile().mkdirs();
				try (InputStream in = url.openStream();
						InputStreamReader reader = new InputStreamReader(in, "UTF-8");
						FileWriter out = new FileWriter(file, Charset.forName("UTF-8"))) {
					IOUtils.copy(in, out);
				}
			}
		}
	}

	/**
	 * Create instance
	 */
	private Initializer() {
	}

	/**
	 * Initialize GCMS. This will
	 * <ol>
	 *   <li>Make the license check (and do autoactivation, if necessary and configured)</li>
	 *   <li>Initialize the cache</li>
	 *   <li>Initialize the key provider (generating a keypair, if necessary)</li>
	 *   <li>Setup the database (ensure that connection can be made and database exists, install initial dump, if database is empty, apply changelog, create/fix triggers)</li>
	 *   <li>Initialize velocity rendering engine</li>
	 *   <li>Initialize JAXB</li>
	 *   <li>Register renderers</li>
	 *   <li>Check the dictionary</li>
	 *   <li>Register object factories</li>
	 *   <li>Start background threads</li>
	 *   <li>Generate/set the node user password (if not set)</li>
	 *   <li>Run the initialization jobs</li>
	 * </ol>
	 */
	public void init() {
		try {
			FunctionRegistry.getInstance().registerFunction(RenderFunction.class.getName());
		} catch (FunctionRegistryException e) {
			logger.error("Error while registering expression parser functions", e);
		}

		// This should be refactored some time, so that it doesn't implicitly
		// start Quartz, because it is not yet safe to.



		try {
			String cacheConfigurationPath = ConfigurationValue.CACHE_CONFIG_PATH.get();
			try {
				createConfigurationFileFromResourceIfMissing(new File(cacheConfigurationPath), "/cache.ccf");
			} catch (IOException e) {
				logger.error(String.format("Error while writing cache configuration to file %s", cacheConfigurationPath), e);
			}

			// before loading the cache configuration, we make sure that the path to the directory holding disk-based caches is set as system property,
			// because that system property should be referenced in the cache configuration
			String cachePath = ConfigurationValue.CACHE_PATH.get();
			System.setProperty(ConfigurationValue.CACHE_PATH.getSystemPropertyName(), cachePath);

			GCMSCache.initialize(cacheConfigurationPath);
		} catch (PortalCacheException e) {
			logger.error("Error while initializing the cache", e);
		}

		try {
			KeyProvider.init(ConfigurationValue.KEYS_PATH.get());
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}

		try {
			UdateChecker.ensureGlobalPrefix();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			NodeConfigRuntimeConfiguration.initVelocity();
		} catch (Exception e) {
			throw new RuntimeException("Error while initializing Velocity Engine", e);
		}

		// this is optional, but we rather preload the validation API on startup
		// than waiting for the first user-input to happen. Loading the default
		// configuration takes a while (the AntiSamy policy in particular).
		ValidatorFactory.preload();

		// required, for example, for reading the XNL configuration.
		try {
			JAXBHelper.init(null);
		} catch (JAXBException e) {
			// everything except XNL function seem to work if the helper
			// isn't successfully initialized.
			logger.error("Error while initializing the JAXBHelper", e);
		}

		ContentRenderer.registerRenderer();

		UdateChecker.check();

		try {
			operate(() -> CNDictionary.ensureConsistency());
		} catch (NodeException e) {
			throw new RuntimeException("Error while checking for translation consistency", e);
		}

		// Make sure invoker queue and dirt queue worker threads are running ...
		ContentNodeFactory factory = ContentNodeFactory.getInstance();
		CNInvokerQueue.getDefault(factory);
		try {
			factory.startDirtQueueWorker();
		} catch (NodeException e) {
			logger.error("Error while starting dirt queue worker thread", e);
		}

		// set the language id (because background jobs will need it)
		ContentNodeHelper.setLanguageId(1);

		startFixPageVersions();

		try {
			GenerateNodeUserPassword generateNodeUserPassword  = new GenerateNodeUserPassword();
			generateNodeUserPassword.execute();
		} catch (NodeException e) {
			logger.error("Error while generating a new password for the user \"node\"", e);
		}

		// start further initialization jobs
		try {
			InitializationJobs.start();
		} catch (NodeException e) {
			logger.error("Error while starting initialization jobs", e);
		}

		ResolverContextHandler.registerContext("contentnode-resolver", new NodeObjectResolverContext());
	}

	/**
	 * Shutdown
	 */
	public void shutdown() {
		logger.info("Shutdown JCS Cache");
		CompositeCacheManager.getInstance().shutDown();

		logger.info("Checking for running publish process.");
		if (PublishController.isRunningLocally()) {
			logger.info("Publish process is currently running. stopping it.");
			PublishController.stopPublishLocally(true);
		}

		logger.info("Destroying all publish handler instances");
		PublishHandlerStore.destroyAllPublishHandlers();
		logger.info("Destroyed all publish handler instances");

		ContentNodeFactory factory = ContentNodeFactory.getInstance();

		logger.info("Stopping CNInvokerQueue");
		CNInvokerQueue.shutdown();

		try {
			logger.info("Stopping DirtqueueWorker");
			factory.stopDirtQueueWorker();
		} catch (NodeException e) {
			logger.error("Error while stopping DirtqueueWorker", e);
		}

		try {
			logger.info("Shutting down Persistent Scheduler.");
			NodeConfigRuntimeConfiguration.shutdown();
		} catch (NodeException e) {
			logger.error("Error while freeing resources", e);
		}
	}

	/**
	 *  start a FixPageVersionsJob if necessary
	 */
	private void startFixPageVersions() {
		final MutableBoolean startFixPageVersions = new MutableBoolean(false);
		try {
			Trx.operate(() -> {
				DBUtils.executeStatement("SELECT name FROM nodesetup WHERE name IN (?)", new SQLExecutor() {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setString(1, FixPageVersionsJob.NODESETUP);
					}

					@Override
					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						while (rs.next()) {
							startFixPageVersions.setValue(true);
						}
					}
				});
			});

			if (startFixPageVersions.booleanValue()) {
				FixPageVersionsJob job = new FixPageVersionsJob();

				job.execute(-1, TimeUnit.SECONDS);
				DBUtils.executeUpdate("DELETE FROM nodesetup WHERE name = ?", new Object[] { FixPageVersionsJob.NODESETUP });
			}
		} catch (NodeException e) {
			logger.error("Error while starting background jobs upon startup", e);
		}
	}
}
