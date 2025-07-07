package com.gentics.contentnode.testutils.mesh;

import static org.assertj.core.api.Assertions.fail;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Timing;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.ImageManipulationMode;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.ProtocolVersion;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.OkHttpClient.Builder;

/**
 * Mesh Context that starts a Mesh container
 */
public class MeshContext extends GenericContainer<MeshContext> {
	/**
	 * Currently tested Mesh Version
	 */
	public final static String TESTED_MESH_VERSION = "2.1.10";

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

	private boolean useFilesystem = false;

	private ImageManipulationMode imageManipulationMode = ImageManipulatorOptions.DEFAULT_IMAGE_MANIPULATION_MODE;

	/**
	 * Create an instance, using the Mesh version of the MeshRestClient
	 */
	public MeshContext() {
		super("docker.gentics.com/gentics/mesh:" + TESTED_MESH_VERSION);
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

		if (!useFilesystem) {
			addEnv("MESH_GRAPH_DB_DIRECTORY", "null");
		}

		addEnv(MeshOptions.MESH_INITIAL_ADMIN_PASSWORD_ENV, "admin");
		addEnv(MeshOptions.MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET_ENV, "false");
		addEnv(ImageManipulatorOptions.MESH_IMAGE_MANIPULATION_MODE_ENV, String.valueOf(imageManipulationMode));
	
		exposedPorts.add(8080);
		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer));
		setStartupAttempts(1);
	
		ArrayList<Consumer<OutputFrame>> consumers = new ArrayList<>(getLogConsumers());
		consumers.add(logBuffer);
		setLogConsumers(consumers);
	}

	@Override
	public void start() {
		super.start();

		// we build our own okhttp client, because we want to increase the timeout to 2 minutes
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequestsPerHost(64);

		Builder builder = new OkHttpClient.Builder()
			.callTimeout(Duration.ofMinutes(2))
			.connectTimeout(Duration.ofMinutes(2))
			.writeTimeout(Duration.ofMinutes(2))
			.readTimeout(Duration.ofMinutes(2))
			.protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE))
			.dispatcher(dispatcher);

		client = MeshRestClient.create(new MeshRestClientConfig.Builder()
				.setHost(getContainerIpAddress())
				.setPort(getMappedPort(8080))
				.setSsl(false)
				.setProtocolVersion(ProtocolVersion.HTTP_2)
				.build(), builder.build());
		client.setLogin("admin", "admin");

		try (Timing tim = Timing.get(-1, duration -> {
			System.out.println("Initial login took %d ms".formatted(duration));
		})) {
			client.login().blockingGet();
		} catch (NodeException e) {
			fail("Failed when doing initial login", e);
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

	/**
	 * Run the mesh server with file system persisting enabled.
	 * 
	 * @return
	 */
	public MeshContext withFilesystem() {
		this.useFilesystem = true;
		return this;
	}

	public MeshContext withImageManipulationMode(ImageManipulationMode mode) {
		this.imageManipulationMode = mode;
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
