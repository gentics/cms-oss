package com.gentics.contentnode.testutils.mesh;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.ProtocolVersion;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.testdbmanager.ManagerResponse;

/**
 * Mesh Context that starts a Mesh container
 */
public class MeshContext extends GenericContainer<MeshContext> {

	public static final String DOCKER_NET = "DOCKER_NET";
	public static final String DOCKER_MESH_IMAGE = "DOCKER_MESH_IMAGE";

	/**
	 * Currently tested Mesh Version
	 */
	public final static String TESTED_MESH_VERSION = "3.0.0";

	public static final String MESH_DATABASE_SUFFIX = "_mesh";

	protected LogBuffer logBuffer = new LogBuffer();

	private static final Logger log = LoggerFactory.getLogger(MeshContext.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private MeshRestClient client;

	/**
	 * Name of the node. Default: dummy
	 */
	private String nodeName = "dummy";

	private int waitTimeout = 300;

	private Integer debugPort;

	private String extraOpts;

	private ImageManipulationMode imageManipulationMode = ImageManipulatorOptions.DEFAULT_IMAGE_MANIPULATION_MODE;

	private SQLUtils dbUtils = null;

	private boolean useTestContextDatabase = false;

	/**
	 * Create an instance, using the Mesh version of the MeshRestClient
	 */
	public MeshContext() {
		super(StringUtils.isNotBlank(System.getenv(DOCKER_MESH_IMAGE)) ? System.getenv(DOCKER_MESH_IMAGE) : "docker.gentics.com/gentics/mesh:" + TESTED_MESH_VERSION);

		String dockerNet = System.getenv(DOCKER_NET);
		if (StringUtils.isNotBlank(dockerNet)) {
			withNetworkMode(dockerNet);
		}
		setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*" + Pattern.quote(MeshEvent.STARTUP.address) + ".*")
				.withStartupTimeout(Duration.of(waitTimeout, ChronoUnit.SECONDS)));
	}

	/**
	 * Get the log buffer
	 * @return log buffer
	 */
	public LogBuffer getLogBuffer() {
		return logBuffer;
	}

	@Override
	protected void configure() {
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv("NODENAME", nodeName);
		String javaOpts = null;
	
		if (debugPort != null) {
			javaOpts = "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n ";
			exposedPorts.add(8000);
			setPortBindings(Arrays.asList("8000:8000"));
		}
	
		if (extraOpts != null) {
			if (javaOpts == null) {
				javaOpts = "";
			}
			javaOpts += extraOpts + " ";
		}
	
		if (javaOpts != null) {
			addEnv("JAVAOPTS", javaOpts);
		}

		// We don't use ES
		addEnv(ElasticSearchOptions.MESH_ELASTICSEARCH_URL_ENV, "null");

		if (useTestContextDatabase) {
			CompletableFuture<ManagerResponse> future = new CompletableFuture<>();
			Executors.newSingleThreadExecutor().submit(() -> {
				DBTestContext.requestGCNDB(future);
			});

			try {
				ManagerResponse dbConnectionResponse = future.get(DBTestContext.DEFAULT_MAX_WAIT, TimeUnit.SECONDS);
				addEnv("MESH_DATABASE_ADDRESS", dbConnectionResponse.getHostname() + ":" + dbConnectionResponse.getPort());
				addEnv("MESH_JDBC_CONNECTION_URL_EXTRA_PARAMS", "?characterEncoding=UTF8");
				addEnv("MESH_JDBC_DATABASE_NAME", dbConnectionResponse.getName() + MESH_DATABASE_SUFFIX);
				addEnv("MESH_JDBC_CONNECTION_USERNAME", dbConnectionResponse.getUser());
				addEnv("MESH_JDBC_CONNECTION_PASSWORD", "");
				addEnv("MESH_DB_CONNECTOR_CLASSPATH", "/connector");
				addFileSystemBind(new java.io.File("target/connector").getAbsolutePath(), "/connector", BindMode.READ_ONLY);

				Properties properties = dbConnectionResponse.toProperties();
				properties.setProperty("url", properties.getProperty("url") + MESH_DATABASE_SUFFIX);
				dbUtils = SQLUtilsFactory.getSQLUtils(properties);
				dbUtils.connectDatabase();
				//dbUtils.createDatabase();
				dbUtils.executeQueryManipulation("CREATE DATABASE IF NOT EXISTS `" + dbConnectionResponse.getName() + MESH_DATABASE_SUFFIX + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;");
			} catch (TimeoutException e) {
				throw new IllegalStateException("Waited too long for the connection properties from gcn-testdb-manager");
			} catch (InterruptedException e) {
				throw new IllegalStateException("Getting the connection from gcn-testdb-manager has been interrupted");
			} catch (Throwable e) {
				throw new IllegalStateException("Getting the connection from gcn-testdb-manager has failed", e);
		}
		}

		addEnv(MeshOptions.MESH_INITIAL_ADMIN_PASSWORD_ENV, "admin");
		addEnv(MeshOptions.MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET_ENV, "false");
		addEnv(ImageManipulatorOptions.MESH_IMAGE_MANIPULATION_MODE_ENV, String.valueOf(imageManipulationMode));
	
		exposedPorts.add(8080);
		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer, logBuffer));
		setStartupAttempts(1);
	}

	@Override
	public void start() {
		super.start();
		client = MeshRestClient.create(new MeshRestClientConfig.Builder()
				.setHost(getContainerIpAddress())
				.setPort(getMappedPort(8080))
				.setSsl(false)
				.setProtocolVersion(ProtocolVersion.HTTP_2)
				.build());
		client.setLogin("admin", "admin");
		client.login().blockingGet();
	}

	@Override
	public void stop() {
		super.stop();
		try {
			if (dbUtils != null) {
				dbUtils.removeDatabase();
			}
		} catch (SQLUtilException e) {
			log.error("Could not drop Mesh database", e);
		}
	}

	public MeshRestClient client() {
		return client;
	}

	/**
	 * Expose the debug port to connect to.
	 * 
	 * @param debugPort
	 *            JNLP debug port. No debugging is enabled when set to null.
	 * @return Fluent API
	 */
	public MeshContext withDebug(int debugPort) {
		this.debugPort = debugPort;
		return this;
	}

	/**
	 * Use the provided JVM arguments.
	 * 
	 * @param opts
	 *            Additional JVM options
	 * @return
	 */
	public MeshContext withExtraOpts(String opts) {
		extraOpts = opts;
		return this;
	}

	/**
	 * Set the name of the node.
	 * 
	 * @param name
	 * @return
	 */
	public MeshContext withNodeName(String name) {
		this.nodeName = name;
		return this;
	}

	public MeshContext withImageManipulationMode(ImageManipulationMode mode) {
		this.imageManipulationMode = mode;
		return this;
	}

	public MeshContext withTestContextDatabase(boolean useTestContextDatabase) {
		this.useTestContextDatabase = useTestContextDatabase;
		return this;
	}

	@Override
	public String getContainerIpAddress() {
		String containerHost = System.getenv("CONTAINER_HOST");
		if (containerHost != null) {
			return containerHost;
		} else {
			return super.getContainerIpAddress();
		}
	}

	public String getHost() {
		String containerHost = System.getenv("CONTAINER_HOST");
		if (containerHost != null) {
			return containerHost;
		} else {
			return "localhost";
		}
	}
}
