package com.gentics.contentnode.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.rules.ExternalResource;

import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;

/**
 * {@link ExternalResource} implementation, which will start and stop the {@link OSSRunner}. This has to be used together with a {@link DBTestContext} like this:
 * <pre>
	private static DBTestContext testContext = new DBTestContext();

	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(ossRunnerContext);
 * </pre>
 */
public class OSSRunnerContext extends ExternalResource {
	/**
	 * Maximum time in ms to wait for the OSSRunner to get started
	 */
	public final static long MAX_WAIT_MS = 60_000L;

	/**
	 * Poll interval in ms for testing, whether the OSSRunner is started
	 */
	public final static long WAIT_INTERVAL_MS = 1_000L;

	/**
	 * Logger
	 */
	protected static final NodeLogger logger = NodeLogger.getNodeLogger(OSSRunnerContext.class);

	/**
	 * Thread of the OSSRunner
	 */
	protected Thread ossRunnerThread;

	@Override
	protected void before() throws Throwable {
		ossRunnerThread = new Thread(() -> {
			OSSRunner.main(null);
		});

		ossRunnerThread.start();

		boolean serverStarted = false;
		long startWait = System.currentTimeMillis();

		while (!serverStarted && (System.currentTimeMillis() - startWait) < MAX_WAIT_MS) {
			assertThat(OSSRunner.isServerFailed()).as("Server startup has failed").isFalse();
			serverStarted = OSSRunner.isServerStarted();
			if (!serverStarted) {
				Thread.sleep(WAIT_INTERVAL_MS);
			}
		}

		assertThat(serverStarted).as(String.format("Server has been started within %d ms", MAX_WAIT_MS)).isTrue();
	}

	@Override
	protected void after() {
		try {
			OSSRunner.stop();
			if (ossRunnerThread != null) {
				ossRunnerThread.interrupt();
			}

			boolean serverStopped = false;
			long startWait = System.currentTimeMillis();
			
			while (!serverStopped && (System.currentTimeMillis() - startWait) < MAX_WAIT_MS) {
				serverStopped = OSSRunner.isServerStopped();
				if (!serverStopped) {
					Thread.sleep(WAIT_INTERVAL_MS);
				}
			}
		} catch (Exception e) {
			logger.warn("Error while stopping OSSRunnerContext", e);
		}
	}
}
