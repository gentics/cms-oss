package com.gentics.contentnode.tests.publish.multithreading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.FactoryHandle;
import com.gentics.contentnode.factory.MulticonnectionTransactionImpl;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Test cases for the handling of broken read connections in the {@link MulticonnectionTransactionImpl} (SUP-20293).
 *
 * When the database server closes the read connections (e.g. during a database outage), a publish worker thread
 * gets a closed connection from the connection pool. The connection must not stay borrowed after the failed request,
 * otherwise the pool is exhausted after 10 failed requests and every further request blocks for the
 * maximum wait time of the pool (10 minutes) before failing with "Timeout waiting for idle object".
 */
public class MulticonnectionTransactionConnectionLeakTest {
	/**
	 * Number of read connections opened by the {@link MulticonnectionTransactionImpl}
	 */
	private final static int CONNECTION_COUNT = 10;

	/**
	 * Maximum time (in seconds) a request for a read connection may take. This is far below the maximum wait time of the pool (10 minutes).
	 */
	private final static int MAX_REQUEST_SECONDS = 30;

	/**
	 * Flag, which simulates whether the read connections have been closed by the database server
	 */
	private AtomicBoolean readConnectionsClosed = new AtomicBoolean(false);

	/**
	 * Static mock of the runtime configuration
	 */
	private MockedStatic<NodeConfigRuntimeConfiguration> runtimeConfigMock;

	/**
	 * Single thread, which simulates the publish worker thread (connections are assigned per thread)
	 */
	private ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "Publishing worker thread 2"));

	/**
	 * Tested transaction
	 */
	private MulticonnectionTransactionImpl transaction;

	@Before
	public void setup() throws Exception {
		List<Connection> connections = new ArrayList<>();
		// the first connection is the writable connection of the transaction
		connections.add(mockConnection(new AtomicBoolean(false)));
		// the other connections are the read connections, which will be put into the pool
		for (int i = 0; i < CONNECTION_COUNT; i++) {
			connections.add(mockConnection(readConnectionsClosed));
		}

		NodeConfig nodeConfig = mock(NodeConfig.class);
		when(nodeConfig.getConnection(false)).thenReturn(connections.get(0), connections.subList(1, connections.size()).toArray(new Connection[0]));

		NodeConfigRuntimeConfiguration runtimeConfig = mock(NodeConfigRuntimeConfiguration.class);
		when(runtimeConfig.getNodeConfig()).thenReturn(nodeConfig);

		runtimeConfigMock = mockStatic(NodeConfigRuntimeConfiguration.class);
		runtimeConfigMock.when(NodeConfigRuntimeConfiguration::getDefault).thenReturn(runtimeConfig);

		transaction = new MulticonnectionTransactionImpl(1, mock(FactoryHandle.class), false);
	}

	@After
	public void tearDown() throws Exception {
		worker.shutdownNow();
		if (transaction != null && transaction.isOpen()) {
			try {
				transaction.rollback();
			} catch (Exception ignored) {
			}
		}
		if (runtimeConfigMock != null) {
			runtimeConfigMock.close();
		}
	}

	/**
	 * Test that requests for read connections do not block after the database server closed all read connections
	 * @throws Exception
	 */
	@Test
	public void testNoBlockingAfterClosedConnections() throws Throwable {
		// precondition: as long as the connections are open, they are returned to the pool after usage
		for (int i = 0; i < 2 * CONNECTION_COUNT; i++) {
			assertThat(requestReadConnection()).as("Request #%d with open connections", i + 1).isNull();
		}

		// the database server closes all read connections
		readConnectionsClosed.set(true);

		// every pooled connection is handed out once and found to be closed
		for (int i = 0; i < CONNECTION_COUNT; i++) {
			assertThat(requestReadConnection()).as("Request #%d with closed connections", i + 1).isInstanceOf(RuntimeException.class)
					.hasMessageContaining("has been closed already");
		}

		// the next request must not wait for the pool's maximum wait time, because no connection is in use anymore
		try {
			requestReadConnection();
		} catch (TimeoutException e) {
			fail(String.format(
					"Request for a read connection still blocked after %d s, although no connection is in use. The failed requests did not give their connections back to the pool.",
					MAX_REQUEST_SECONDS));
		}
	}

	/**
	 * Let the worker thread prepare and close a reading statement (which will request and release a read connection)
	 * @return the exception thrown while preparing the statement or null, if the statement could be prepared
	 * @throws TimeoutException if the request did not finish within {@link #MAX_REQUEST_SECONDS}
	 * @throws Exception
	 */
	private Throwable requestReadConnection() throws Exception {
		Future<Throwable> future = worker.submit(() -> catchThrowable(() -> {
			PreparedStatement statement = transaction.prepareStatement("SELECT 1");
			transaction.closeStatement(statement);
		}));
		try {
			return future.get(MAX_REQUEST_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw e;
		}
	}

	/**
	 * Create a mocked connection
	 * @param closed flag, which determines whether the connection is closed
	 * @return mocked connection
	 * @throws SQLException
	 */
	private Connection mockConnection(AtomicBoolean closed) throws SQLException {
		Connection connection = mock(Connection.class);
		when(connection.isClosed()).thenAnswer(invocation -> closed.get());
		when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
			PreparedStatement statement = mock(PreparedStatement.class);
			when(statement.getConnection()).thenReturn(connection);
			return statement;
		});
		return connection;
	}
}
