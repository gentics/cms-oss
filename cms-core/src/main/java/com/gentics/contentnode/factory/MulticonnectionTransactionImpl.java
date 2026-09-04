/*
 * @author jan
 * @date Jun 5, 2008
 * @version $Id: MulticonnectionTransactionImpl.java,v 1.8 2010-09-28 17:01:32 norbert Exp $
 */
package com.gentics.contentnode.factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.AsynchronousJob;
import com.gentics.contentnode.etc.AsynchronousWorker;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.TransactionManager.TransactionImpl;
import com.gentics.contentnode.publish.PublishThreadInfo;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.DB;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of a multiconnection transaction, which can be used for multithreaded publishing
 * The multiconnection transaction will open
 * <ol>
 * <li>a single connection that is used for writing</li>
 * <li>10 connections that are used for reading</li>
 * </ol>
 * Access to the writable connection is synchronized. When a thread calls one of the methods
 * {@link #prepareInsertStatement(String)}, {@link #prepareUpdateStatement(String)} or {@link #prepareDeleteStatement(String)}, the thread will lock the writable connection until all
 * prepared statements are closed by calling {@link #closeStatement(PreparedStatement)}.
 */
public class MulticonnectionTransactionImpl extends TransactionImpl implements MulticonnectionTransaction {

	/**
	 * The transaction will use 10 parallel connections for reading.
	 */
	// TODO: some performance tests to see how many connections are optimal and/or make this configurable
	private static final int CONNECTION_COUNT = 10;

	private NodeConfig config;

	private NodeLogger logger = NodeLogger.getNodeLogger(MulticonnectionTransactionImpl.class);

	/**
	 * List of connections for reading, which were generated in
	 * {@link #startMulticonnection()}, but not yet "delivered" from the
	 * {@link PoolableConnectionFactory} instance
	 */
	private List<Connection> connections = new Vector<Connection>();

	/**
	 * Map of all connections in the pool (values are ordinal numbers)
	 */
	private Map<Connection, Integer> allConnections = new HashMap<Connection, Integer>();

	/**
	 * Flag to mark whether the {@link #writableConnection} is currently locked by a thread.
	 */
	private boolean writeConnectionLock;

	/**
	 * Pool for the connections
	 */
	private GenericObjectPool<Connection> connectionPool;

	/**
	 * Connection that is assigned to the current thread
	 */
	private ThreadLocal<Connection> assignedConnection = new ThreadLocal<Connection>();

	/**
	 * Threadlocal counter for how often the reading connection was requested by the thread.
	 */
	private ThreadLocal<Integer> assignmentStack = new ThreadLocal<Integer>();

	/**
	 * Threadlocal counter for how often the writable connection was requested by the thread
	 */
	private ThreadLocal<Integer> writableConnectionAssignmentStack = new ThreadLocal<Integer>();

	/**
	 * Asynchronous worker, that will perform jobs
	 */
	private AsynchronousWorker asynchronousWorker;

	/**
	 * Thread currently owning the writable connection
	 */
	private Thread writableConnectionOwner = null;

	/**
	 * Stores an exception that contains the stacktrace for the call that lead to the writableConnectionOwner assignment.
	 */
	private Exception writableConnectionOwnerStacktraceException = null;

	/**
	 * List of threads currently waiting on the writable connection
	 */
	private List<Thread> waitingThreads = new Vector<Thread>();

	/**
	 * A list of publishThreadInfo objects - this allows you to get a summary about the publishThreadInfos of all threads
	 */
	private List<PublishThreadInfo> publishThreadInfos = new Vector<PublishThreadInfo>();

	/**
	 * In this threadlocal we store the timing information about current thread (time working, time waiting for db etc.)
	 */
	private ThreadLocal<PublishThreadInfo> publishThreadInfo = new ThreadLocal<PublishThreadInfo>();

	/**
	 * Create a new transaction for use in multithreaded publishing. This transaction instance is shared between all publishing threads.
	 * @param id Transaction ID
	 * @param factoryHandle factory handle
	 * @param useConnectionPool true if a connection pool shall be used
	 * @throws TransactionException
	 * @throws InvalidSessionIdException
	 */
	public MulticonnectionTransactionImpl(long id, 
			FactoryHandle factoryHandle, boolean useConnectionPool) throws TransactionException,
				InvalidSessionIdException {
		super(id, factoryHandle, useConnectionPool);
		config = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();

		// open the connections
		startMulticonnection();

		// create new connection pool
		// The apache pooling library lacks some features we would need. You  cannot initiate the pool
		// with a fixed set of objects in it. The objects are created dynamically every time a client requests
		// a new object from the pool until WHEN_EXHAUSTED_BLOCK. 
		// (This happens in PoolableConnectionFactory.makeObject()).

		// However in this case we cannot create the objects dynamically - all connections have to be started
		// at once. In startMulticonnection() we open the connections to the DB and store the objects
		// in the "List connections". 

		// At the begining the pool is empty. When some requests a connection PoolableConnectionFactory.makeObject()
		// is called. Then we take (and remove) a connection from the connections list.
		connectionPool = new GenericObjectPool<Connection>(new PoolableConnectionFactory());
		connectionPool.setMaxActive(CONNECTION_COUNT);
		connectionPool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
		connectionPool.setMaxIdle(-1);
		connectionPool.setMaxWait(Duration.of(10, ChronoUnit.MINUTES).toMillis());
		// we want to test idle connections. Testing will be done every TransactionManager.KEEPALIVE_INTERVAL ms (1 hour)
		// and will call the validate() method of the PoolableConnectionFactory, which will issue a dummy statement to keep the connection alive
		connectionPool.setTestWhileIdle(true);
		// don't evict any objects due to idle time
		connectionPool.setMinEvictableIdleTimeMillis(-1);
		// run the evictor every second.
		connectionPool.setTimeBetweenEvictionRunsMillis(1000L);

		// now we get all connections from the pool and return them. this will
		// ensure that all the connections really exist in the pool and will be
		// kept alive by the object evictor (which will prevent connection timeouts)
		List<Connection> borrowedObjects = new ArrayList<Connection>(CONNECTION_COUNT);
		for (int i = 0; i < CONNECTION_COUNT; i++) {
			try {
				borrowedObjects.add(connectionPool.borrowObject());
			} catch (Exception e) {
				throw new TransactionException("Error while initializing connection pool", e);
			}
		}
		// return all the connections to the pool
		for (Connection borrowed : borrowedObjects) {
			try {
				connectionPool.returnObject(borrowed);
			} catch (Exception e) { // ignored
			}
		}

		// TODO: do we want onFailExit??
		asynchronousWorker = new AsynchronousWorker("Worker for Multithreaded Publishing", true);
		asynchronousWorker.setLogJobs(false);
		asynchronousWorker.start();
	}

	@Override
	public void setRenderResult(RenderResult renderResult) {
		super.setRenderResult(renderResult);
		asynchronousWorker.setRenderResult(renderResult);
	}

	/**
	 * Start {@link #CONNECTION_COUNT} connections for reading and store them in {@link #connections}.
	 * Start a single connection for writing.
	 * Create an instance of {@link AsynchronousWorker}.
	 * 
	 * @throws TransactionException
	 */
	public void startMulticonnection() throws TransactionException {
		try {
			// create CONNECTION_COUNT connections for reading.
			for (int i = 0; i < CONNECTION_COUNT; i++) {
				if (logger.isDebugEnabled()) {
					logger.debug("Getting connection: " + i);
				}
				Connection c = config.getConnection(false);

				if (logger.isDebugEnabled()) {
					logger.debug("disabling autocommit for: " + i);
				}
				c.setAutoCommit(false);
				connections.add(c);
				// add the connections to the list holding all connections
				allConnections.put(c, i);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Getting writable connection.");
			}

		} catch (SQLException e) {
			throw new TransactionException("Could not start new multiconnection transaction.", e);
		} catch (NodeException e) {
			throw new TransactionException("Could not start new multiconnection transaction.", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#commit(boolean)
	 */
	public void commit(boolean stopTransaction) throws TransactionException {
		if (stopTransaction) {
			// wait until the asynchronous worker has processed all pending jobs
			asynchronousWorker.flush();
			// stop the asynchronous worker
			asynchronousWorker.stop();
			asynchronousWorker.join();
		}
		super.commit(stopTransaction);
	}

	/**
	 * Stop the transaction
	 */
	protected void stopTransaction() {
		stopTransaction(false);
	}

	/**
	 * Stop the transaction
	 * @param force true if the connection shall be stopped by force
	 */
	private void stopTransaction(boolean force) {
		if (logger.isInfoEnabled()) {
			logger.info("Stopping thread for DB write jobs.");
		}

		// mark the transaction closed
		this.open = false;
		// deregister transaction
		TransactionManager.deregisterTransaction(this.id);

		// reset the transactionals
		transactionals = new Vector<Transactional>();
		resetTransactionalCounter();

		// shutdown all asynchronous workers
		if (!force) {
			asynchronousWorker.flush();
			asynchronousWorker.stop();
		} else {
			asynchronousWorker.abort();
		}

		asynchronousWorker.join();

		// deregister the connector from the DB
		if (dbHandle != null) {
			DB.closeConnector(dbHandle);
		}

		// close the writable connection
		try {
			connection.close();
		} catch (SQLException e2) {
			logger.warn("Could not close writable connection of multiconnection transaction.", e2);
		}
		connection = null;

		// close the pool. this must be done before the connections are closed.
		// otherwise, the evictor thread of the pool might validate connections that were already closed, which
		// would log an error
		try {
			connectionPool.close();
		} catch (Exception e) {
			logger.warn("Error while closing connection pool", e);
		}

		for (Connection c : allConnections.keySet()) {
			try {
				// no need to commit since this connection should't be used for
				// writing anyway;
				c.close();
			} catch (SQLException e) {
				logger.warn("Error while closing readable connection for multiconnection transaction.", e);
			}
		}
		allConnections.clear();
		connections.clear();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#rollback()
	 */
	public void rollback() throws TransactionException {
		if (logger.isInfoEnabled()) {
			logger.info("Rolling back multiconnection transaction.");
		}

		// check whether the transaction is running
		if (!isOpen()) {
			throw new TransactionException("Error while rolling back " + this + ": transaction is not open");
		}

		try {
			// clear the level2 cache
			clearLevel2Cache();
			// rollback the db connection
			connection.rollback();
		} catch (SQLException e) {
			throw new TransactionException("Error while rolling back " + this, e);
		} finally {
			stopTransaction(true);
		}
	}

	/**
	 * Gets a connection for the current thread for the current thread.
	 * If a connection is already assigned to this thread, this connection will be returned, and the threadlocal counter
	 * will be increased.
	 * 
	 * @return A connection to the DB that can be used for reading statements.
	 *         Don't use this connection for writing statements. The reading of
	 *         data is transactional but the connection will not be committed.
	 *         All written data will be lost.
	 * @throws SQLException
	 */
	private Connection getConnectionForCurrentThread() throws SQLException {
		// Unfortunately the apache pooling library doesn't support threading:
		// If one thread requests and gets an object from the pool and then 
		// later requests another one (before returning the first one) it should
		// get the same object as in the first request. The object won't be
		// returned to the pool until all requests are returned.

		// Example: you request 4 objects from the pool, get 4 times the same object
		// and then have to return it 4 times.

		// E.g. you request a connection, make a statement and while processing
		// the result you want to make another statement. It is perfectly ok
		// to reuse the same connection. However you cannot return it to the pool
		// until *all* statements are closed.

		String threadName = Thread.currentThread().getName();

		if (logger.isDebugEnabled()) {
			logger.debug("Thread {" + threadName + "} wants a connection, current stack is: {" + assignedConnection.get() + "}");
		}

		// this thread has already a connection so we can reuse it
		// just increase the stackcount
		if (assignedConnection.get() != null) {
			Connection c = assignedConnection.get();

			// we use a threadlocal to store how many times the current thread has requested the connection.
			Integer i = assignmentStack.get();

			assignmentStack.set(i.intValue() + 1);

			return c;
		} else {
			// the current thread has no assigned connection at the moment
			long waitStart = System.currentTimeMillis();
			Connection c = null;

			try {
				// this will block if the pool is empty
				c = connectionPool.borrowObject();
			} catch (Exception e) {
				String msg = "Worker thread {" + threadName + "} could not get a connection from multiconnection pool.";

				logger.fatal(msg, e);
				throw new SQLException(msg, e);
			}

			getPublishThreadInfo().increaseTimeWaitingDB((System.currentTimeMillis() - waitStart));

			if (c == null) {
				throw new SQLException("Worker thread {" + threadName + "} did not get a connection from multiconnection pool.");
			} else if (c.isClosed()) {
				throw new SQLException("Worker thread {" + threadName + "} requested a connection from the pool, but it has been closed already.");
			}

			assignedConnection.set(c);
			assignmentStack.set(1);
			return c;
		}
	}

	/**
	 * Get a connection that can be used for reading and writing.
	 * @return A connection to the DB the that can be used for writing (and
	 *         reading). This connection will be commited.
	 * @throws TransactionException
	 */
	private Connection getWriteConnection() throws TransactionException {
		long startWait = System.currentTimeMillis();
		try {
			if (connection.isClosed()) {
				throw new TransactionException("Writable connection has been closed");
			}
		} catch (SQLException e) {
			throw new TransactionException("Error while checking connection", e);
		}

		waitForConnection();
		getPublishThreadInfo().increaseTimeWaitingDB((System.currentTimeMillis() - startWait));
		return connection;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#prepareInsertStatement(java.lang.String)
	 */
	public PreparedStatement prepareInsertStatement(String sql) throws SQLException, TransactionException {
		if (logger.isDebugEnabled()) {
			logger.debug("prepareInsertStatement(" + sql + ")");
		}
		return getWriteConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#prepareDeleteStatement(java.lang.String)
	 */
	public PreparedStatement prepareDeleteStatement(String sql)  throws SQLException, TransactionException {
		if (logger.isDebugEnabled()) {
			logger.debug("prepareDeleteStatement(" + sql + ")");
		}
		return getWriteConnection().prepareStatement(sql);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#prepareUpdateStatement(java.lang.String)
	 */
	public PreparedStatement prepareUpdateStatement(String sql)  throws SQLException, TransactionException {
		if (logger.isDebugEnabled()) {
			logger.debug("prepareUpdateStatement(" + sql + ")");
		}
		return getWriteConnection().prepareStatement(sql);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#getStatement()
	 */
	public Statement getStatement() throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("getStatement()");
		}
		return getConnectionForCurrentThread().createStatement();
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#prepareStatement(java.lang.String)
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException, TransactionException {
		if (logger.isDebugEnabled()) {
			logger.debug("prepareStatement(" + sql + ")");
		}
		if (sql.startsWith("INSERT") || sql.startsWith("insert") || sql.startsWith("UPDATE") || sql.startsWith("update") || sql.startsWith("DELETE")
				|| sql.startsWith("delete")) {
			String firstWord = sql.substring(0, 1).toUpperCase() + sql.substring(1, 6).toLowerCase();

			throw new TransactionException(
					"Trying to prepare a readable statement for {" + sql + "}, which will modify the database! Need to use prepare" + firstWord
					+ "Statement() to prepare the statement");
		}
		return getConnectionForCurrentThread().prepareStatement(sql);
	}

	@Override
	public PreparedStatement prepareSelectForUpdate(String sql) throws SQLException, TransactionException {
		return getWriteConnection().prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
	}

	/**
	 * A worker thread can request a connection over and over. It will always get
	 * the same connection. However we have to remember how many times it was 
	 * requested so we know when it can be returned to the connection pool.
	 * 
	 * This function decreases the counter and if 0, returns the connection to the pool.
	 */
	private void popAssignmentStack() {
		// now pop the stack and (if 0) release the connection and put it back to the pool
		Integer stackcount = assignmentStack.get();
		int i = 0;

		try {
			i = stackcount.intValue();
		} catch (NullPointerException e1) {
			logger.warn("Trying to pop connection stack that is null.", e1);
		}
		i--;

		// if i>0 we have not reached the stack bottom yet, just store the decreased stackcount
		if (i > 0) {
			assignmentStack.set(i);
		} else {
			assignmentStack.set(0);
			// we have reached the bottom of the stack
			// this thread doesn't need the connection anymore
			Connection c = assignedConnection.get();

			try {
				connectionPool.returnObject(c);
			} catch (Exception e) {
				logger.warn("Could not return connection to pool: " + e.getMessage(), e);
			}
			assignedConnection.set(null);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#closeStatement(java.sql.Statement)
	 */
	public void closeStatement(Statement statement) {

		if (statement != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Thread {" + Thread.currentThread().getName() + "} is closing statement {" + statement.toString() + "}.");
			}
			// if this was a write statement we must handle it differently
			try {
				if (statement.getConnection() != null) {
					if (statement.getConnection().equals(connection)) {
						wakeUp();
						statement.close();
						return;
					}
				} else {
					logger.warn("Thread {%s} returned statement without connection".formatted(Thread.currentThread().getName()));
				}
			} catch (SQLException e1) {
				logger.warn("Error while closing writing statement.", e1);
				return;
			}

			try {
				statement.close();
			} catch (SQLException e) {
				logger.warn("Error while closing the statement", e);
			}
			popAssignmentStack();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#closeResultSet(java.sql.ResultSet)
	 */
	public void closeResultSet(ResultSet resultSet) {
		if (resultSet != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Thread {" + Thread.currentThread().getName() + "} is closing ResultSet {" + resultSet.toString() + "}.");
			}
			try {
				resultSet.close();
			} catch (SQLException e) {
				logger.warn("Error while closing the resultset", e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.TransactionManager.TransactionImpl#closeStatement(java.sql.PreparedStatement)
	 */
	public void closeStatement(PreparedStatement statement) {
		closeStatement((Statement) statement);
	}

	/**
	 * Internal implementation of a {@link PoolableObjectFactory}.
	 * An instance of this class will be used by the connection pool to "generate" the connections
	 * which actually are generated 
	 */
	private class PoolableConnectionFactory implements PoolableObjectFactory<Connection> {

		/**
		 * Time for every connection, when it was kept alive the last time
		 */
		protected Map<Connection, Long> keepAliveTimes = new HashMap<Connection, Long>();

		@Override
		public void activateObject(Connection c) throws Exception {}

		@Override
		public void destroyObject(Connection c) throws Exception {}

		@Override
		public void passivateObject(Connection c) throws Exception {
			if (c == null) {
				logger.warn("A null connection was passed for returning to the pool");
			} else {
				if (c.isClosed()) {
					logger.warn("A closed connection was passed for returning to the pool.");
				}
			}
		}

		@Override
		public boolean validateObject(Connection c) {
			if (c == null) {
				return false;
			}
			try {
				boolean isOk = !c.isClosed();

				if (!isOk) {
					logger.warn("An already closed connection got into the pool.");
				} else {
					// keep the connection alive by issuing a dummy statement
					keepAliveCheck(c);
				}
				return isOk;
			} catch (SQLException e) {
				logger.warn("Encountered an invalid connection, it will be removed from the pool.");
				return false;
			}
		}

		@Override
		public Connection makeObject() throws Exception {
			// In startMulticonnection() we have opened CONNECTION_COUNT connections and stored them in the list.
			// The connection pool is configured to use max. CONNECTION_COUNT active objects.
			// That means that this function gets called max. CONNECTION_COUNT times

			// we return one of the connections and remove it from the list
			if (CollectionUtils.isEmpty(connections)) {
				throw new TransactionException("Could not get connection from the pool, since none are left.");
			}
			return connections.remove(0);
		}

		/**
		 * Check whether the connection needs to be kept alive, and if yes, keep it alive
		 * @param c connection
		 */
		protected void keepAliveCheck(Connection c) {
			long lastKeepAlive = ObjectTransformer.getLong(this.keepAliveTimes.get(c), 0);
			int connNumber = ObjectTransformer.getInt(allConnections.get(c), -1);

			if (logger.isDebugEnabled()) {
				if (lastKeepAlive == 0) {
					logger.debug("Connection #" + connNumber + " was never kept alive before");
				} else {
					logger.debug("Connection #" + connNumber + " was kept alive @" + lastKeepAlive);
				}
			}
			long now = System.currentTimeMillis();

			if ((now - lastKeepAlive) > TransactionManager.KEEPALIVE_INTERVAL) {
				if (logger.isDebugEnabled()) {
					logger.debug("Keeping connection #" + connNumber + " alive now");
				}
				// keep alive now
				keepAlive(c);
				// mark the time, when the connection was kept alive the last time
				this.keepAliveTimes.put(c, now);
			}
		}

		/**
		 * Keep the connection alive by issuing a dummy SQL statement
		 * @param c connection to keep alive
		 */
		protected void keepAlive(Connection c) {
			// keep the connection alive by issuing a dummy statement
			PreparedStatement pst = null;

			try {
				pst = c.prepareStatement("SELECT 1");
				pst.execute();
			} catch (SQLException e) {
				logger.warn("Error while keeping connection alive", e);
			} finally {
				if (pst != null) {
					try {
						pst.close();
					} catch (SQLException ignored) {}
				}
			}
		}
	}

	/**
	 * We have only one writable connection and only one thread can have it at a time. 
	 * There is no pool so we have to make our own block&wait.
	 */
	private void waitForConnection() {
		synchronized (connection) {
			if (logger.isDebugEnabled()) {
				logger.debug("Thread {" + Thread.currentThread().getName() + "} wants a writable connection.");
			}
			while (writeConnectionLock) {
				if (writableConnectionOwner.equals(Thread.currentThread())) {
					break;
				}
				try {
					waitingThreads.add(Thread.currentThread());
					if (logger.isInfoEnabled()) {
						logger.info("Thread " + Thread.currentThread() + " wants writing connection which is blocked by {" + writableConnectionOwner + "}. The appeneded stacktrace shows the call that lead to the writableConnectionOwner assignment.", writableConnectionOwnerStacktraceException);
					}
					connection.wait();
				} catch (InterruptedException e) {
					if (Thread.interrupted()) {

						// the thread that had locked the writable connection is finished and woke us up.
						if (!writeConnectionLock) {
							break;
						} else {
							continue;
						}
					}
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Granting connection to thread {" + Thread.currentThread().getName() + "}.");
			}
			writeConnectionLock = true;
			writableConnectionOwnerStacktraceException = new NodeException("The writableConnectionOwner assignment occured at {"
					+ System.currentTimeMillis() + "}");
			writableConnectionOwner = Thread.currentThread();
			Integer i = writableConnectionAssignmentStack.get();

			if (i == null) {
				i = 1;
				writableConnectionAssignmentStack.set(i);
			} else {
				int iint = i.intValue();

				writableConnectionAssignmentStack.set(iint + 1);
			}
		}
	}

	/**
	 * A thread can request a writable connection over and over. When all statements on it are closed
	 * then the writable connection free and another thread can use it (if there is another thread
	 * waiting for it).
	 */
	private void wakeUp() {
		synchronized (connection) {
			Integer i = writableConnectionAssignmentStack.get();

			if (i == null) {
				logger.error("Error while retrieving assignment stack for writable connection.");
				return;
			}
			int iint = i.intValue() - 1;

			if (logger.isDebugEnabled()) {
				logger.debug("Thread {" + Thread.currentThread().getName() + "} released writable connection. New stack: " + iint);
			}
			writableConnectionAssignmentStack.set(iint);

			// if counter == 0 then the last statement on this connection was closed
			// the connection is now available to other threads.
			if (iint == 0) {
				writeConnectionLock = false;
				writableConnectionOwner = null;
				writableConnectionOwnerStacktraceException = null;
				if (logger.isDebugEnabled()) {
					logger.debug("Thread {" + Thread.currentThread().getName() + "} released writable connection. Waking up next waiting thread (if any there).");
				}

				// wake up the next waiting thread (which is blocked in waitForConnection())
				Iterator<Thread> threadIterator = waitingThreads.iterator();

				if (threadIterator.hasNext()) {
					Thread t = threadIterator.next();

					t.interrupt();
					threadIterator.remove();

					if (logger.isDebugEnabled()) {
						logger.debug("Woke up thread {" + t.getName() + "}.");
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.MulticonnectionTransaction#addAsynchronousJob(com.gentics.lib.etc.AsynchronousJob)
	 */
	public void addAsynchronousJob(AsynchronousJob job) throws NodeException {
		asynchronousWorker.addAsynchronousJob(job);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.MulticonnectionTransaction#waitForAsynchronousJobs()
	 */
	public void waitForAsynchronousJobs() throws NodeException {
		asynchronousWorker.flush();
		asynchronousWorker.throwExceptionOnFailure();
	}

	/**
	 * @return the publishThreadInfos
	 */
	public List<PublishThreadInfo> getPublishThreadInfos() {
		return publishThreadInfos;
	}

	/**
	 * @return the publishThreadInfo
	 */
	public PublishThreadInfo getPublishThreadInfo() {
		PublishThreadInfo pti = publishThreadInfo.get();

		if (pti == null) {
			pti = new PublishThreadInfo(Thread.currentThread());
			setPublishThreadInfo(pti);
		}
		return pti;
	}

	/**
	 * @param publishThreadInfo the publishThreadInfo to set
	 */
	public void setPublishThreadInfo(PublishThreadInfo publishThreadInfo) {
		this.publishThreadInfo.set(publishThreadInfo);
		publishThreadInfos.add(publishThreadInfo);
	}

	@Override
	public Connection getConnection() throws TransactionException {
		return getWriteConnection();
	}

	@Override
	public void releaseConnection(Connection c) {
		if (c != null && c.equals(connection)) {
			wakeUp();
		}
	}
}
