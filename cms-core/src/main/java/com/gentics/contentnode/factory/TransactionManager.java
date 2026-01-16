/*
 * @author norbert
 * @date 19.12.2006
 * @version $Id: TransactionManager.java,v 1.35.2.6 2011-04-06 11:45:11 tobiassteiner Exp $
 */
package com.gentics.contentnode.factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.i18n.Language;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.etc.Timing;
import com.gentics.contentnode.factory.TransactionStatistics.Item;
import com.gentics.contentnode.i18n.CNDictionary;
import com.gentics.contentnode.jmx.MBeanRegistry;
import com.gentics.contentnode.jmx.TransactionInfoMBean;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.NodeObjectInfo;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.publish.InstantPublisher.Result;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.lib.db.Connector;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.PoolConnection;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/**
 * TransactionManager is a static utility class to generate and manage transactions.
 */
public final class TransactionManager {

	/**
	 * Keepalive interval (in ms). Default is 1 hour
	 */
	public final static int KEEPALIVE_INTERVAL = 60 * 60 * 1000;

	/**
	 * sequence for transaction ids
	 */
	private static long transactionIDSequence = 0;

	/**
	 * list of currently open transactions (ids)
	 */
	private static List<Long> openTransactions = new Vector<Long>();

	/**
	 * logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(TransactionManager.class);

	/**
	 * threadlocal curren transaction
	 */
	private static ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();

	public static final Object syncObject = new Object();

	/**
	 * JMX Bean
	 */
	private static TransactionInfo transactionInfoMBean = null;

	/**
	 * hidden constructor for a utility class
	 */
	private TransactionManager() {// this will never be called anyway
	}

	static {
		try {
			transactionInfoMBean = new TransactionInfo();
		} catch (NotCompliantMBeanException e) {
			logger.error("Error while registering JMX Bean", e);
		}
	}

	/**
	 * Get the current (tread-local) transaction or null if none found
	 * @return current transaction or null
	 */
	public static Transaction getCurrentTransactionOrNull() {
		return currentTransaction.get();
	}

	/**
	 * Get the current (thread-local) transaction
	 * @return current transaction
	 * @throws TransactionException if no current transaction found
	 */
	public static Transaction getCurrentTransaction() throws TransactionException {
		Transaction current = currentTransaction.get();

		if (current != null) {
			return current;
		} else {
			throw new TransactionException("No current transaction found");
		}
	}

	/**
	 * Set the current (thread-local) transaction
	 * @param transaction current transaction to set
	 */
	public static void setCurrentTransaction(Transaction transaction) {
		currentTransaction.set(transaction);
	}

	/**
	 * Get a new transaction with the same configuration as the given
	 * transaction. Note that transactions retrieved with this method must be
	 * stopped by calling either {@link Transaction#commit()} or
	 * {@link Transaction#rollback()}.
	 * @param transaction
	 * @return
	 * @throws TransactionException
	 */
	public static Transaction getTransaction(Transaction transaction) throws TransactionException, InvalidSessionIdException {
		return getTransaction(transaction, null);
	}

	/**
	 * Get a new transaction with the same configuration as the given
	 * transaction. Note that transactions retrieved with this method must be
	 * stopped by calling either {@link Transaction#commit()} or
	 * {@link Transaction#rollback()}.
	 * @param transaction
	 * @param useConnectionPool true if connection pooling shall be used
	 * @return
	 * @throws TransactionException
	 */
	public static Transaction getTransaction(Transaction transaction, Boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		if (transaction instanceof TransactionImpl) {
			TransactionImpl t = (TransactionImpl) transaction;
			boolean connPool = ObjectTransformer.getBoolean(useConnectionPool, t.useConnectionPool);

			return new TransactionImpl(t.sessionId, t.getUserId(), getTransactionID(), t.factoryHandle, connPool, t.getPermHandler());
		} else {
			throw new TransactionException("Error while getting transaction: given transaction was not provided by the TransactionManager");
		}
	}

	/**
	 * Get a new transaction using the given configuration and db connection.
	 * Note that transactions retrieved with this method must be stopped by
	 * calling either {@link Transaction#commit()} or
	 * {@link Transaction#rollback()}.<br/> Note on connection pooling: for
	 * long running transactions (e.g. publishing process) it is STRONGLY
	 * recommended, to not use connection pooling, since the memory consumption
	 * would be much higher.
	 * @param factoryHandle factory handle
	 * @param useConnectionPool flag to set whether db connections shall be
	 *        pooled or not.
	 * @return a new open transaction
	 * @throws TransactionException when no transaction could be started
	 */
	public static Transaction getTransaction(FactoryHandle factoryHandle,
			boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		return new TransactionImpl(getTransactionID(), factoryHandle, useConnectionPool);
	}

	/**
	 * Get a new transaction using the given configuration and db connection.
	 * Note that transactions retrieved with this method must be stopped by
	 * calling either {@link Transaction#commit()} or
	 * {@link Transaction#rollback()}.<br/>
	 * Note on connection pooling: for long running transactions (e.g.
	 * publishing process) it is STRONGLY recommended, to not use connection
	 * pooling, since the memory consumption would be much higher.
	 * @param sessionId sessionId for the user that is specified in the parameter userId
	 * @param userId userId of the user who should be associated with this transaction
	 * @param factoryHandle factory handle
	 * @param useConnectionPool flag to set whether db connections shall be
	 *        pooled or not.
	 * @return a new open transaction
	 * @throws TransactionException when no transaction could be started
	 */
	public static Transaction getTransaction(String sessionId, Integer userId, FactoryHandle factoryHandle,
			boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		return new TransactionImpl(sessionId, userId, getTransactionID(), factoryHandle, useConnectionPool, null);
	}

	/**
	 * Get a new transaction using the given configuration and db connection.
	 * Note that transactions retrieved with this method must be stopped by
	 * calling either {@link Transaction#commit()} or
	 * {@link Transaction#rollback()}.<br/> Note on connection pooling: for
	 * long running transactions (e.g. publishing process) it is STRONGLY
	 * recommended, to not use connection pooling, since the memory consumption
	 * would be much higher.
	 * @param factoryHandle factory handle
	 * @param useConnectionPool flag to set whether db connections shall be
	 *        pooled or not.
	 * @param multiconnection this is only needed if you want to access the transaction from multiple threads
	 * @return a new open transaction
	 * @throws TransactionException when no transaction could be started
	 */
	public static Transaction getTransaction(FactoryHandle factoryHandle,
			boolean useConnectionPool, boolean multiconnection) throws TransactionException, InvalidSessionIdException {
		if (multiconnection) {
			return new MulticonnectionTransactionImpl(getTransactionID(), factoryHandle, useConnectionPool);
		} else {
			return getTransaction(factoryHandle, useConnectionPool);
		}
	}

	/**
	 * Get a new transaction using the given configuration and db connection.
	 * Note that transactions retrieved with this method must be stopped by
	 * calling either {@link Transaction#commit()} or
	 * {@link Transaction#rollback()}.<br/>
	 * Note on connection pooling: for long running transactions (e.g.
	 * publishing process) it is STRONGLY recommended, to not use connection
	 * pooling, since the memory consumption would be much higher.
	 * @param sessionId sessionId of the user which should be associated with
	 *        the transaction. If the sessionId is null the System user is
	 *        associated with the Transaction.
	 * @param factoryHandle factory handle
	 * @param useConnectionPool flag to set whether db connections shall be
	 *        pooled or not.
	 * @return a new open transaction
	 * @throws TransactionException when no transaction could be started
	 */
	public static Transaction getTransaction(String sessionId, FactoryHandle factoryHandle,
			boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
		return new TransactionImpl(sessionId, getTransactionID(), factoryHandle, useConnectionPool);
	}

	/**
	 * Check whether the transaction with given id is still running
	 * @param id id of the transaction in question
	 * @return true when the transaction is running, false if not
	 */
	public static boolean isTransactionRunning(long id) {
		synchronized (openTransactions) {
			int foundAt = Collections.binarySearch(openTransactions, new Long(id));

			return foundAt >= 0;
		}
	}

	/**
	 * Execute the given executable instance in a new - temporary - transaction, that will be created as copy of the current transaction.
	 * @param exec executable
	 * @throws TransactionException if there is no current transaction or the executable threw an error.
	 */
	public static void execute(Executable exec) throws NodeException {
		execute(exec, null);
	}

	/**
	 * Execute the given executable instance in a new - temporary - transaction, that will be created as copy of the current transaction.
	 * @param exec executable
	 * @param useConnectionPool True to use connection pooling, false to not use it, null to use, if the current transaction did
	 * @throws TransactionException if there is no current transaction or the executable threw an error.
	 */
	public static void execute(Executable exec, Boolean useConnectionPool) throws NodeException {
		execute(exec, useConnectionPool, 0);
	}

	/**
	 * Execute the given executable instance in a new - temporary - transaction, that will be created as copy of the current transaction.
	 * @param exec executable
	 * @param useConnectionPool True to use connection pooling, false to not use it, null to use, if the current transaction did
	 * @param retries number of retries (0 for none), if the executable fails with a {@link SQLTransientException}}
	 * @throws TransactionException if there is no current transaction or the executable threw an error.
	 */
	public static void execute(Executable exec, Boolean useConnectionPool, int retries) throws NodeException {
		Transaction old = getCurrentTransaction();

		int attempt = 0;
		boolean doRetry = false;

		do {
			doRetry = false;
			attempt++;

			Transaction tmp = getTransaction(old, useConnectionPool);
			tmp.setTimestamp(old.getTimestamp());
			setCurrentTransaction(tmp);

			try {
				exec.execute();
				tmp.commit();
			} catch (Exception e) {
				try {
					tmp.rollback();
				} catch (TransactionException ignored) {
				}
				if ((e instanceof SQLTransientException || e.getCause() instanceof SQLTransientException)
						&& attempt <= retries) {
					doRetry = true;
				} else if (e instanceof NodeException) {
					throw (NodeException) e;
				} else {
					throw new NodeException(e);
				}
			} finally {
				setCurrentTransaction(old);
			}
		} while (doRetry);
	}

	/**
	 * Execute the given executable instance in a new - temporary - transaction, that will be created as copy of the current transaction.
	 * @param exec executable
	 * @return return value
	 * @throws TransactionException if there is no current transaction or the executable threw an error.
	 */
	public static <R> R execute(ReturnValueExecutable<R> exec) throws NodeException {
		return execute(exec, null);
	}

	/**
	 * Execute the given executable instance in a new - temporary - transaction, that will be created as copy of the current transaction.
	 * @param exec executable
	 * @param useConnectionPool True to use connection pooling, false to not use it, null to use, if the current transaction did
	 * @return return value
	 * @throws TransactionException if there is no current transaction or the executable threw an error.
	 */
	public static <R> R execute(ReturnValueExecutable<R> exec, Boolean useConnectionPool) throws NodeException {
		Transaction old = getCurrentTransaction();
		Transaction tmp = getTransaction(old, useConnectionPool);
		tmp.setTimestamp(old.getTimestamp());
		setCurrentTransaction(tmp);

		try {
			R result = exec.execute();
			tmp.commit();
			return result;
		} catch (Exception e) {
			try {
				tmp.rollback();
			} catch (TransactionException ignored) {}
			if (e instanceof RuntimeException && e.getCause() instanceof NodeException) {
				throw (NodeException) e.getCause();
			} else if (e instanceof NodeException) {
				throw (NodeException) e;
			} else {
				throw new NodeException(e);
			}
		} finally {
			setCurrentTransaction(old);
		}
	}

	/**
	 * Register a new open transaction
	 * @param id transaction id
	 */
	protected static void registerTransaction(Long id) {
		synchronized (openTransactions) {
			int foundAt = Collections.binarySearch(openTransactions, id);

			if (foundAt < 0) {
				openTransactions.add(-foundAt - 1, id);
				if (transactionInfoMBean != null) {
					transactionInfoMBean.inc();
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Registered transaction id " + id + " at manager.");
				}
			}
		}
	}

	/**
	 * Deregister the transaction (it is no longer open)
	 * @param id transaction id
	 */
	protected static void deregisterTransaction(Long id) {
		synchronized (openTransactions) {
			// when the transaction to be deregistered is the current
			// transaction, we set the current to null
			Transaction current = getCurrentTransactionOrNull();

			if (current != null && id.longValue() == current.getId()) {
				setCurrentTransaction(null);
			}
			if (Collections.binarySearch(openTransactions, id) >= 0) {
				openTransactions.remove(id);
				if (transactionInfoMBean != null) {
					transactionInfoMBean.dec();
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Deregistered transaction id " + id + " from manager.");
				}
			}
		}
	}

	/**
	 * Get the ids of the currently registered open transactions
	 * @return array of transaction ids
	 */
	protected static long[] getOpenTransactions() {
		synchronized (openTransactions) {
			long[] ids = new long[openTransactions.size()];

			for (int i = 0; i < openTransactions.size(); ++i) {
				ids[i] = openTransactions.get(i).longValue();
			}

			return ids;
		}
	}

	/**
	 * Get the last transaction ID
	 * @return last transaction ID
	 */
	public static long getLastTransactionID() {
		return transactionIDSequence;
	}

	/**
	 * Get a new unique transaction id from the sequence
	 * @return transaction id
	 */
	private static synchronized long getTransactionID() {
		return ++transactionIDSequence;
	}

	/**
	 * Interface for implementations that execute actions in a new - temporary - transaction
	 */
	public static interface Executable {

		/**
		 * Execute something in a temporary transaction, using the method {@link TransactionManager#execute(Executable)}.
		 * The temporary transaction has already been created. If this method returns normally, the transaction will be committed.
		 * If this method throws an exception, the transaction will be rolled back and the method {@link TransactionManager#execute(Executable)} will also throw the exception.
		 * @throws NodeException
		 */
		void execute() throws NodeException;
	}

	/**
	 * Interface for implementations that execute actions, which return something in a new - temporary - transaction
	 */
	public static interface ReturnValueExecutable<R> {
		/**
		 * Execute something in a temporary transaction, using the method {@link TransactionManager#execute(Executable)}.
		 * The temporary transaction has already been created. If this method returns normally, the transaction will be committed.
		 * If this method throws an exception, the transaction will be rolled back and the method {@link TransactionManager#execute(Executable)} will also throw the exception.
		 * @return return value
		 * @throws NodeException
		 */
		R execute() throws NodeException;
	}

	/**
	 * Implementation of a transaction
	 */
	protected static class TransactionImpl implements Transaction {

		/**
		 * transaction id
		 */
		protected Long id;

		/**
		 * status of this transaction (true when open)
		 */
		protected boolean open = false;

		/**
		 * interrupt status of the transaction
		 */
		protected boolean interrupted;

		/**
		 * ids of the parallel open transactions
		 */
		protected long[] parallelOpenTransactions;

		/**
		 * db connection
		 */
		protected Connection connection;

		/**
		 * dbhandle containing the connection
		 */
		protected DBHandle dbHandle;

		/**
		 * Flag to mark whether the connection uses a connection pool or not
		 */
		protected boolean useConnectionPool;

		/**
		 * timestamp of the transaction start
		 */
		protected long startTimestamp;

		/**
		 * factory handle for privileged access to the NodeFactory
		 */
		protected FactoryHandle factoryHandle;

		/**
		 * PermHandler for this transaction
		 */
		protected PermHandler permHandler;

		/**
		 * render type for this transaction
		 */
		protected ThreadLocal<RenderType> renderType = new ThreadLocal<RenderType>();

		/**
		 * "level2" cache (implemented as simple map)
		 */
		protected Map<String, Map<Object, Object>> level2Cache = new ConcurrentHashMap<String, Map<Object, Object>>();

		/**
		 * Flag to enable/disable the level2 Cache
		 */
		protected boolean useLevel2Cache = true;

		private RenderResult renderResult;

		/**
		 * The session ID that was used to construct this instance with.
		 */
		protected String sessionId;

		/**
		 * Holds the session variables for this transaction.
		 * This variable may be null, if this transaction is not associated
		 * with a session.
		 */
		protected Session session;

		/**
		 * ID of the user
		 */
		protected Integer userId;

		/**
		 * Flag to mark, whether transactionals are run or not
		 */
		protected boolean runTransactionals = false;

		/**
		 * list of transactionals that are called during the commit process
		 */
		protected List<Transactional> transactionals = Collections.synchronizedList(new Vector<Transactional>());

		/**
		 * Counter that counts the amount of transactionals
		 */
		protected Map<Class<?>, Integer> transactionalsCounter = Collections.synchronizedMap(new HashMap<Class<?>, Integer>());

		/**
		 * Transaction attributes
		 */
		protected Map<String, Object> attributes;

		/**
		 * Stack of "disable multichannelling flag"s
		 */
		protected ThreadLocal<Stack<Boolean>> disableMultichannellingFlag = new ThreadLocal<Stack<Boolean>>();

		/**
		 * Disable Versioned Publishing Flag
		 */
		protected ThreadLocal<Boolean> disableVersionedPublishing = new ThreadLocal<Boolean>();

		/**
		 * Threadlocal to enable/disable the publish cache
		 */
		protected ThreadLocal<Boolean> enablePublishCache = new ThreadLocal<Boolean>();

		/**
		 * Stack of channels
		 */
		protected ThreadLocal<Stack<Node>> channel = new ThreadLocal<Stack<Node>>();

		/**
		 * Threadlocal flag for checking permissions in ANY channel
		 */
		protected ThreadLocal<Boolean> checkAnyChannel = new ThreadLocal<>();

		/**
		 * The channel that is currently being published.
		 */
		protected ThreadLocal<Integer> publishedNode = new ThreadLocal<>();

		/**
		 * Flag to enable/disable instant publishing
		 */
		protected boolean instantPublishing = true;

		/**
		 * Map of languages (dictionaries)
		 */
		protected Map<Integer, Language> languages = new HashMap<Integer, Language>(2);

		/**
		 * Timestamp this transaction was last checked. This is initialized with the current system time, so the first keepalive dummy statement will be issued in
		 * {@link TransactionManager#KEEPALIVE_INTERVAL} ms.
		 */
		protected long lastCheck = System.currentTimeMillis();

		/**
		 * Transaction statistics
		 */
		protected TransactionStatistics stats;

		/**
		 * Prepared NodeStructure
		 */
		protected PublishData nodeStructure;

		/**
		 * Wastebin filter of the transaction
		 */
		protected ThreadLocal<Wastebin> wastebinFilter = ThreadLocal.withInitial(() -> Wastebin.getDefault());

		/**
		 * Collected instant publishing results. Keys are object types, values are maps of object ID -> result
		 */
		protected Map<Integer, Map<Integer, Result>> instantPublishingResults = new HashMap<>();

		/**
		 * Create a new transaction instance and start it.
		 * The system user will be associated with this transaction.
		 * @param id transaction id
		 * @param factoryHandle factory handle
		 * @param useConnectionPool whether a connection pool shall be used or not
		 */
		public TransactionImpl(long id, FactoryHandle factoryHandle, boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
			initTransaction(null, null, id, factoryHandle, useConnectionPool, null);
		}

		/**
		 * Create a new transaction instance and start it.
		 * @param sessionId sessionId of the user which should be associated with the transaction.
		 * @param id transaction id
		 * @param factoryHandle factory handle
		 * @param useConnectionPool whether a connection pool shall be used or not
		 */
		public TransactionImpl(String sessionId, long id, FactoryHandle factoryHandle, boolean useConnectionPool) throws TransactionException, InvalidSessionIdException {
			initTransaction(sessionId, null, id, factoryHandle, useConnectionPool, null);
		}

		/**
		 * Create a new transaction instance and start it.
		 * @param sessionId sessionId of the user which should be associated with the transaction
		 * @param userId id of the user who is associated with the session
		 * @param id transaction id
		 * @param factoryHandle factory handle
		 * @param useConnectionPool whether a connection pool shall be used or not
		 * @param preInitializedPermHandler if not null, use this PermHandler
		 * instead of initializing a new one.
		 */
		public TransactionImpl(String sessionId, Integer userId, long id, FactoryHandle factoryHandle, boolean useConnectionPool, PermHandler preInitializedPermHandler) throws TransactionException, InvalidSessionIdException {
			initTransaction(sessionId, userId, id, factoryHandle, useConnectionPool, preInitializedPermHandler);
		}

		/**
		 * Initializes a new Transaction and starts it
		 *
		 * @param sessionId sessionId of the user which should be associated with the Transaction.
		 *   Session variables like userId an languageId will be loaded from
		 *   the session using this ID. If the given ID is non-null and
		 *   doesn't identify a valid session, a
		 *   {@link TransactionException} is thrown.
		 *   This parameter may be null, in which case the session will not
		 *   have an associated user or session.
		 * @param id transaction id
		 * @param factoryHandle factory handle
		 * @param useConnectionPool whether a connection pool shall be used or not
		 * @param preInitializedPermHandler if not null, use this PermHandler
		 * instead of initializing a new one.
		 */
		protected void initTransaction(String sessionId, Integer user, long id, FactoryHandle factoryHandle, boolean useConnectionPool, PermHandler preInitializedPermHandler) throws TransactionException, InvalidSessionIdException {
			// will get initialized by initTransactionUnsafe() and must be closed if a exception is thrown
			connection = null;
			try {
				initTransactionUnsafe(sessionId, user, id, factoryHandle, useConnectionPool, preInitializedPermHandler);
			} catch (TransactionException e) {
				try {
					// clear the level2 cache
					clearLevel2Cache();
					if (null != connection) {
						// rollback the db connection
						connection.rollback();
					}
				} catch (SQLException e1) {
					logger.warn("Error while rolling back connection", e1);
				} finally {
					stopTransaction();
				}
				throw e;
			} catch (InvalidSessionIdException e) {
				try {
					// clear the level2 cache
					clearLevel2Cache();
					if (null != connection) {
						// rollback the db connection
						connection.rollback();
					}
				} catch (SQLException e1) {
					logger.warn("Error while rolling back connection", e1);
				} finally {
					stopTransaction();
				}
				throw e;
			}
		}

		/**
		 * If this function fails and throws an exception, it will not clean
		 * up after itself. The caller is responsible for catching any
		 * exceptions and making sure all resources are freed. In particular,
		 * not closing the database connection (if established by this
		 * method), will result in dangling database connections.
		 * @param preInitializedPermHandler if not null, use this PermHandler
		 * instead of initializing a new one.
		 * @see #initTransaction(String, Integer, long, String, FactoryHandle, boolean, PermHandler)
		 */
		private void initTransactionUnsafe(String sessionId, Integer user, long id, FactoryHandle factoryHandle, boolean useConnectionPool, PermHandler preInitializedPermHandler) throws TransactionException, InvalidSessionIdException {
			// begin profiling mark for any kind of transaction
			RuntimeProfiler.beginMark(JavaParserConstants.TRANSACTION);

			this.sessionId = sessionId;
			this.userId = user;
			this.useConnectionPool = useConnectionPool;

			this.id = new Long(id);
			parallelOpenTransactions = TransactionManager.getOpenTransactions();
			this.factoryHandle = factoryHandle;
			this.renderResult = new RenderResult();

			// get the configuration
			NodeConfig config = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();

			if (config == null) {
				throw new TransactionException("Could not start " + this + ": No configuration found");
			}

			// get the db connection
			try {
				connection = config.getConnection(useConnectionPool);
			} catch (NodeException e) {
				throw new TransactionException(
						"Could not start " + this + ": Could not establish connection", e);
			}

			if (connection == null) {
				throw new TransactionException(
						"Could not start " + this + ": Could not establish connection");
			}

			// create a new TransactionConnector instance and add to DB. Store the returned handle
			dbHandle = DB.addConnector(new TransactionConnector());

			if (sessionId != null) {

				SessionToken token = new SessionToken(sessionId);

				this.sessionId = String.valueOf(token.getSessionId());
				this.session = new Session(token.getSessionId(), this);
				ContentNodeHelper.setLanguageId(this.session.getLanguageId());
			} else {
				// this Transaction isn't associated with a session.
				this.session = null;
			}

			if (preInitializedPermHandler!=null) {
				permHandler=preInitializedPermHandler;
			} else {
				// init the permHandler by refreshing it
				refreshPermHandler();
			}

			// start the transaction
			try {
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				throw new TransactionException("Could not start " + this + ": Error while starting db transaction.", e);
			}
			this.open = true;
			TransactionManager.registerTransaction(this.id);

			// mark the transaction starttime (for logging transaction durations)
			this.startTimestamp = System.currentTimeMillis();

			if (logger.isDebugEnabled()) {
				logger.debug(this + " started @ " + startTimestamp);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#commit()
		 */
		public void commit() throws TransactionException {
			this.commit(true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#commit()
		 */
		public void commit(boolean stopTransaction) throws TransactionException {
			// check whether the transaction is running
			if (!isOpen()) {
				throw new TransactionException("Error while committing " + this + ": transaction is not open");
			}

			try {
				// flush all factories
				Transaction current = TransactionManager.getCurrentTransactionOrNull();

				try (Timing commit = Timing.subLog("Flush")) {
					TransactionManager.setCurrentTransaction(this);
					factoryHandle.flushAll();
				} finally {
					TransactionManager.setCurrentTransaction(current);
				}

				try (Timing commit = Timing.subLog("Clear Cache")) {
					// clear the level2 cache
					clearLevel2Cache();
				}

				runTransactionals = true;
				try (Timing commit = Timing.subLog("On DB Commit")) {
					// call the transactionals onDBCommit()
					for (Transactional t : transactionals) {
						t.onDBCommit(this);
					}
				}

				try (Timing commit = Timing.subLog("Connection Commit")) {
					// commit the db connection
					connection.commit();
				}

				boolean commitAgain = false;

				try (Timing commit = Timing.subLog("On Trx Commit")) {
					// call the transactionals onTransactionCommit()
					for (Transactional t : transactionals) {
						commitAgain |= t.onTransactionCommit(this);
					}
				}

				if (commitAgain) {
					try (Timing commit = Timing.subLog("Connection Commit")) {
						connection.commit();
					}
				}
				runTransactionals = false;

				try (Timing commit = Timing.subLog("Remove Delete Lists")) {
					removeDeleteLists();
				}
				transactionals = new Vector<Transactional>();

				if (stopTransaction) {
					try (Timing commit = Timing.subLog("Stop")) {
						stopTransaction();
					}
				}
			} catch (Exception e) {
				try {
					this.rollback();
				} catch (TransactionException te) {
					logger.error("Error while rolling back transaction.", te);
				}
				throw new TransactionException("Error while commiting " + this, e);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#rollback()
		 */
		public void rollback() throws TransactionException {
			rollback(true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#rollback(boolean)
		 */
		public void rollback(boolean stopTransaction) throws TransactionException {
			// check whether the transaction is running
			if (!isOpen()) {
				throw new TransactionException("Error while rolling back " + this + ": transaction is not open");
			}

			removeDeleteLists();

			try {
				// run the transactionals
				runTransactionals = true;
				for (Transactional t : transactionals) {
					t.onTransactionRollback(this);
				}

				// clear the level2 cache
				clearLevel2Cache();
				// rollback the db connection
				connection.rollback();

				for (Transactional t : transactionals) {
					t.afterTransactionRollback(this);
				}

				transactionals = new Vector<Transactional>();
				resetTransactionalCounter();
			} catch (SQLException e) {
				throw new TransactionException("Error while rolling back " + this, e);
			} finally {
				runTransactionals = false;
				if (stopTransaction) {
					stopTransaction();
				}
			}
		}

		/**
		 * Returns the current transactional counter for the given type
		 */
		protected int getTransactionalCounter(Transactional t) {
			int count = transactionalsCounter.containsKey(t.getClass()) ? transactionalsCounter.get(t.getClass()) : 0;
			return count;
		}
		/**
		 * Increment the transactional counter by one
		 *
		 * @param t
		 */
		synchronized protected void incrementTransactionalCounter(Transactional t) {
			int count = getTransactionalCounter(t);
			transactionalsCounter.put(t.getClass(), count + 1);
		}

		/**
		 * Decrement the transactionals counter by one
		 *
		 * @param t
		 */
		synchronized protected void decrementTransactionalCounter(Transactional t) {
			int count = getTransactionalCounter(t);
			transactionalsCounter.put(t.getClass(), count - 1);
		}

		/**
		 * Resets the {@link Transactional} counter for the given type of transactionals
		 *
		 * @param t
		 */
		protected void resetTransactionalCounter(Transactional t) {
			transactionalsCounter.put(t.getClass(), 0);
		}

		/**
		 * Resets the {@link Transactional} counter for all types
		 */
		synchronized protected void resetTransactionalCounter() {
			transactionalsCounter.clear();
		}

		/**
		 * Removes all deleteLists for this transaction
		 */
		private void removeDeleteLists() {
			factoryHandle.removeDeleteLists(this);
		}

		/**
		 * Stop this transaction, deregister the transaction from the
		 * transaction manager and give back the connection.
		 *
		 * Attention: Please note that any cleanup that is done here should
		 *            also be applied to any class that inherits this class.
		 *            e.g.: @see MulticonnectionTransactionImpl
		 */
		protected void stopTransaction() {
			this.open = false;
			deregisterTransaction(this.id);
			NodeConfig config = NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();

			// clean the transactionals
			transactionals = new Vector<Transactional>();
			resetTransactionalCounter();

			// deregister the connector from the DB
			if (dbHandle != null) {
				DB.closeConnector(dbHandle);
			}

			if (config == null) {
				// log an error here (but this will rarely happen)
				logger.error("Error while stopping " + this + ": could not find configuration, db connection will be closed.");
				if (connection != null) {
					try {
						connection.close();
					} catch (SQLException e) {
						logger.warn("Error while closing connection in " + this + "", e);
					}
				}
			} else {
				config.returnConnection(connection);
				if (logger.isDebugEnabled()) {
					logger.debug("Returned db connection of " + this + " to connection pool.");
				}
			}

			connection = null;

			if (logger.isDebugEnabled()) {
				long stopTimestamp = System.currentTimeMillis();

				logger.debug("Stopped " + this + " @ " + stopTimestamp + " (transaction runtime: " + (stopTimestamp - startTimestamp) + "ms)");
			}

			// profiling end any transactions, commited or rolledback. has been started in constructor of transaction.
			RuntimeProfiler.endMark(JavaParserConstants.TRANSACTION);

		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#removeTransactional(com.gentics.lib.base.factory.Transactional)
		 */
		public void removeTransactional(Transactional t) {
			if (!runTransactionals) {
				transactionals.remove(t);
				decrementTransactionalCounter(t);
			}
		}

		/**
		 * Checks whether the given transactional should be replaced by a generic transactional. This method will check whether a transactional specific threshold is
		 * exceeded. All transactionals of the same type will be replaced by a generic transactional. Additional this check will not add the given transactional when a
		 * generic transactional of the matching type is detected within the list of transactionals.
		 *
		 * @return true when the transactional could be replaced by a generic transactional or when a generic transactional for the type of the given transactional could
		 *         be found, otherwise false.
		 */
		private boolean handleTransactionalThreshold(Transactional t) {
			Transactional singleton = t.getSingleton(this);

			// Check whether this transactional got a generic singleton implementation.
			if (singleton != null) {
				boolean isSingletonListed = transactionals.contains(singleton);
				// Check whether the singleton is already listed. In this case we can skip the rest and ignore the give transaction since its purpose will be handled by
				// the singleton in a single step
				if (isSingletonListed) {
					if (logger.isDebugEnabled()) {
						logger.debug("The singleton transactional {" + singleton.getClass() + "} is already listed. No need to continue");
					}
					return true;
				} else {
					int threshold = t.getThreshold(this);
					int nTransactionalsListed = getTransactionalCounter(t);
					// determine whether threshold was exceeded
					if (threshold >= 0 && nTransactionalsListed >= threshold) {
						if (logger.isDebugEnabled()) {
							logger.debug("The threshold of {" + threshold + "} was exceeded for a Transactional of type {" + t.getClass() + "}");
						}
						// replace transactionals with the singleton
						ListIterator<Transactional> iter = transactionals.listIterator();
						int removedTransactionals = 0;
						while (iter.hasNext()) {
							Transactional currentTransactional = iter.next();
							boolean isTransactionalOfSameType = t.getClass().equals(currentTransactional.getClass());
							if (isTransactionalOfSameType) {
								iter.remove();
								removedTransactionals++;
							}
						}

						transactionals.add(singleton);
						resetTransactionalCounter(t);
						logger.debug("Replaced {" + removedTransactionals + "} transactionals with singleton of type {" + singleton.getClass() + "}");
						return true;
					} else {
						return false;
					}
				}
			} else {
				return false;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#addTransactional(com.gentics.lib.base.factory.Transactional)
		 */
		synchronized public void addTransactional(Transactional t) {

			// if transactionals are performed right now
			if (!runTransactionals) {

				// Check whether this transactional will trigger the
				// threshold for transactionals. We don't need to add
				// the transactional if this check succeeds.
				if (handleTransactionalThreshold(t)) {
					return;
				}

				// only add transactionals once
				if (!transactionals.contains(t)) {
					transactionals.add(t);
					incrementTransactionalCounter(t);
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isRunning()
		 */
		public boolean isOpen() {
			return open;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#interrupt()
		 */
		public void interrupt() {
			this.interrupted = true;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isInterrupted()
		 */
		public boolean isInterrupted() {
			return interrupted;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getId()
		 */
		public long getId() {
			return id.longValue();
		}

		/*
		 * @see com.gentics.lib.base.factory.Transaction#getSessionId()
		 */
		public String getSessionId() {
			return sessionId;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getUserId()
		 */
		public int getUserId() {
			// this transaction may not be associated with a session
			if (null == this.session) {
				return ObjectTransformer.getInt(userId, 0);
			} else {
				return session.getUserId();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getPermHandler()
		 */
		public PermHandler getPermHandler() {
			return permHandler;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getGroupPermHandler(int)
		 */
		public PermHandler getGroupPermHandler(int groupId) throws NodeException {
			PermHandler groupPermHandler = new PermHandler();
			groupPermHandler.initForGroup(groupId);

			return groupPermHandler;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isParallelRunning(long)
		 */
		public boolean isParallelOpen(long transId) {
			for (int i = 0; i < parallelOpenTransactions.length; i++) {
				if (transId == parallelOpenTransactions[i]) {
					return true;
				}
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "Transaction {" + this.getClass().getName() + "}, {" + id + "}, status: "
					+ (isOpen() ? "open" : "closed");
		}

		/**
		 * Get the threadlocal stack of disable multichannelling flags
		 * @return stack of disable multichannelling flags
		 */
		protected Stack<Boolean> getDisableMultichannellingFlagStack() {
			Stack<Boolean> result = disableMultichannellingFlag.get();

			if (result == null) {
				result = new Stack<Boolean>();
				disableMultichannellingFlag.set(result);
			}
			return result;
		}

		/**
		 * Get the threadlocal stack of channels
		 * @return stack of channels
		 */
		protected Stack<Node> getChannelStack() {
			Stack<Node> result = channel.get();

			if (result == null) {
				result = new Stack<Node>();
				channel.set(result);
			}
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#finalize()
		 */
		protected void finalize() throws Throwable {
			if (!getDisableMultichannellingFlagStack().empty()) {
				logger.error("disableMultichannellingFlag stack is not empty while " + this + " is garbage collected");
			}

			if (!getChannelStack().empty()) {
				logger.error("channel stack is not empty while " + this + " is garbage collected");
			}

			if (isOpen()) {
				// transaction is still running while garbage collected
				logger.error(this + " is still open while garbage collected. Stopping it now.");
				stopTransaction();
			}

		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getRenderType()
		 */
		public RenderType getRenderType() {
			return renderType.get();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#setRenderType(com.gentics.lib.render.RenderType)
		 */
		public void setRenderType(RenderType renderType) {
			this.renderType.set(renderType);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObject(java.lang.Class, java.lang.Integer)
		 */
		public <T extends NodeObject> T getObject(Class<T> clazz, Integer id) throws NodeException {
			return getObject(clazz, id, false, true, true);
		}

		public <T extends NodeObject> T getCurrentObject(Class<T> clazz, Integer id) throws NodeException {
			try {
				setDisableVersionedPublish(true);
				return getObject(clazz, id, false, true, true);
			} finally {
				setDisableVersionedPublish(false);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObject(java.lang.Class,
		 *      java.lang.Integer, int, boolean)
		 */
		public <T extends NodeObject> T getObject(Class<T> clazz, Integer id,
				int versionTimestamp, boolean multichannelFallback) throws NodeException {
			// set this as current transaction
			TransactionManager.setCurrentTransaction(this);
			try {
				startObjectLoad();
				// forward the call to the factory handle
				return getWastebinFilter().filter(factoryHandle.getObject(clazz, id, false, versionTimestamp, multichannelFallback, true));
			} finally {
				endObjectLoad(clazz);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObject(java.lang.Class, java.lang.Object, int)
		 */
		public <T extends NodeObject> T getObject(Class<T> clazz, Integer id,
				int versionTimestamp) throws NodeException {
			return getObject(clazz, id, versionTimestamp, true);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObject(java.lang.Class, java.lang.Integer, boolean)
		 */
		public <T extends NodeObject> T getObject(Class<T> clazz, Integer id, boolean forUpdate) throws NodeException, ReadOnlyException {
			return getObject(clazz, id, forUpdate, true, true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObject(java.lang.Class, java.lang.Integer, boolean, boolean, boolean)
		 */
		public <T extends NodeObject> T getObject(Class<T> clazz, Integer id,
				boolean forUpdate, boolean multichannelFallback, boolean logErrorIfNotFound) throws NodeException, ReadOnlyException {
			TransactionManager.setCurrentTransaction(this);
			try {
				startObjectLoad();
				return getWastebinFilter().filter(factoryHandle.getObject(clazz, id, forUpdate, -1, multichannelFallback, logErrorIfNotFound));
			} finally {
				endObjectLoad(clazz);
			}
		}

		@Override
		public <T extends NodeObject> T getObject(T object) throws NodeException {
			return getObject(object, false, true);
		}

		@Override
		public <T extends NodeObject> T getObject(T object, boolean forUpdate) throws NodeException {
			return getObject(object, forUpdate, true);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends NodeObject> T getObject(T object, boolean forUpdate, boolean multichannelFallback) throws NodeException {
			if (object == null) {
				return null;
			}
			return (T)getObject(object.getObjectInfo().getObjectClass(), object.getId(), forUpdate, multichannelFallback, true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObjects(java.lang.Class, java.util.Collection)
		 */
		public <T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids) throws NodeException {
			return getObjects(clazz, ids, false, true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObjects(java.lang.Class, java.util.Collection, int)
		 */
		public <T extends NodeObject> List<T> getObjects(Class<T> clazz,
				Collection<Integer> ids, int versionTimestamp) throws NodeException {
			// set this as current transaction
			TransactionManager.setCurrentTransaction(this);
			// forward the call to the factory handle
			try {
				startObjectLoad();
				RuntimeProfiler.beginMark(JavaParserConstants.TRANSACTION_GETOBJECTS);
				List<T> objects = factoryHandle.getObjects(clazz, ids, false, versionTimestamp, true);
				getWastebinFilter().filter(objects);
				return objects;
			} finally {
				RuntimeProfiler.endMark(JavaParserConstants.TRANSACTION_GETOBJECTS);
				endObjectLoad(clazz);
			}
		}

		@Override
		public <T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids,
				boolean forUpdate) throws NodeException, ReadOnlyException {
			return getObjects(clazz, ids, forUpdate, true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObjects(java.lang.Class, java.util.Collection, boolean)
		 */
		public <T extends NodeObject> List<T> getObjects(Class<T> clazz, Collection<Integer> ids, boolean forUpdate, boolean allowMultichannelingFallback)
				throws NodeException, ReadOnlyException {
			// set this as current transaction
			TransactionManager.setCurrentTransaction(this);
			// forward the call to the factory handle
			try {
				startObjectLoad();
				RuntimeProfiler.beginMark(JavaParserConstants.TRANSACTION_GETOBJECTS);
				List<T> objects = factoryHandle.getObjects(clazz, ids, forUpdate, -1, allowMultichannelingFallback);
				getWastebinFilter().filter(objects);
				return objects;
			} finally {
				RuntimeProfiler.endMark(JavaParserConstants.TRANSACTION_GETOBJECTS);
				endObjectLoad(clazz);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#createObject(java.lang.Class)
		 */
		public <T extends NodeObject> T createObject(Class<T> clazz) throws NodeException {
			TransactionManager.setCurrentTransaction(this);
			return factoryHandle.createObject(clazz);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#renderObject(com.gentics.lib.render.GCNRenderable, com.gentics.lib.render.RenderResult)
		 */
		public String renderObject(GCNRenderable object, RenderResult result) throws NodeException {
			// set this as current transaction
			TransactionManager.setCurrentTransaction(this);
			// let the object render itself
			return object.render(result);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isCached(java.lang.Class, java.lang.Object)
		 */
		public boolean isCached(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
			// TODO implement this
			return false;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#createObjectInfo(java.lang.Class)
		 */
		public NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz) throws NodeException {
			return factoryHandle.createObjectInfo(clazz, false);
		}

		@Override
		public NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, int versionTimestamp) throws NodeException {
			return factoryHandle.createObjectInfo(clazz, versionTimestamp);
		}

		@Override
		public NodeObjectInfo createObjectInfo(Class<? extends NodeObject> clazz, boolean editable) throws NodeException {
			return factoryHandle.createObjectInfo(clazz, editable);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#putObject(java.lang.Class, com.gentics.lib.base.object.NodeObject)
		 */
		public <T extends NodeObject> T putObject(Class<T> clazz, NodeObject obj) throws NodeException {
			// TODO implement this
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getObjectFactory(java.lang.Class)
		 */
		public ObjectFactory getObjectFactory(Class<? extends NodeObject> clazz) {
			return factoryHandle.getObjectFactory(clazz);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getClass(int)
		 */
		public Class<? extends NodeObject> getClass(int objType) {
			return factoryHandle.getClass(objType);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getClass(java.lang.String)
		 */
		public Class<? extends NodeObject> getClass(String tableName) {
			return factoryHandle.getClass(tableName);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getTable(java.lang.Class)
		 */
		public String getTable(Class<? extends NodeObject> clazz) {
			return factoryHandle.getTable(clazz);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getTType(java.lang.Class)
		 */
		public int getTType(Class<? extends NodeObject> clazz) {
			return factoryHandle.getTType(clazz);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#dirtObjectCache(java.lang.Class, java.lang.Object)
		 */
		public void dirtObjectCache(Class<? extends NodeObject> clazz, Integer id) throws NodeException {
			dirtObjectCache(clazz, id, true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#dirtObjectCache(java.lang.Class, java.lang.Object, boolean)
		 */
		public void dirtObjectCache(Class<? extends NodeObject> clazz,
				Integer id, boolean atCommit) throws NodeException {
			// set this as current transaction
			TransactionManager.setCurrentTransaction(this);
			factoryHandle.dirtObjectCache(clazz, id);
			clearLevel2Cache();
			if (atCommit) {
				addTransactional(new TransactionalDirtCache(clazz, id));
			}
		}

		@Override
		public void clearCache(Class<? extends NodeObject> clazz, Set<Integer> ids) throws NodeException {
			factoryHandle.getFactory().clear(clazz, ids);
		}

		/**
		 * Clears the NodeFactory object cache
		 *
		 * @throws NodeException
		 */
		public void clearNodeObjectCache() throws NodeException {
			factoryHandle.getFactory().clear();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getTimestamp()
		 */
		public long getTimestamp() {
			return startTimestamp;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.i18n.LanguageProvider#getLanguage()
		 */
		public synchronized Language getLanguage() {
			try {
				int langId = getLanguageId();

				if (0 == langId) {
					// fallback to a default language.
					// this case has been observed during page-import (do 11004)
					langId = 1;
				}
				if (languages.containsKey(langId)) {
					return languages.get(langId);
				} else {
					Locale locale = ContentNodeHelper.getLocaleForLanguageId(langId, this);
					Language language = new Language(Integer.toString(langId), locale, new CNDictionary(langId));

					languages.put(langId, language);
					return language;
				}
			} catch (NodeException e) {
				logger.error("Error while getting language", e);
				return null;
			}
		}

		/**
		 * @return the language ID this transaction should use. this is always
		 *   a valid ID (0 < id).
		 * @throws NodeException if neither the thread nor this transaction
		 *   has a language id set.
		 */
		private int getLanguageId() throws NodeException {
			try {
				// TODO: actually, we should always and only use the
				// session's languageId. Calling
				// ContentNodeHelper.getLanguageId() is done for backwards
				// compatibility, so that we don't change any current
				// behaviour. This will actually throw expected exceptions because
				// ContentNodeHelper.getLanguageId() isn't used by
				// ContentNodeResource. To fix this, one should remove the
				// get/setLanguageId() from ContentNodeHelper at some point.
				// Another point: this transaction may actually be for user=0
				// and sessionId=null (that happens when viewing a page
				// it seems). In that case, ContentNodeHelper.getLanguageId()
				// may still have a valid languageId - it should be checked whether
				// that is so, because then, one _has_ to use
				// ContentNodeHelper.getLanguageId() since the languageId can't
				// be retrieved if the sessionId is null.
				int threadLangId = ContentNodeHelper.getLanguageId();

				if (0 < threadLangId) {
					return threadLangId;
				}
			} catch (NodeException e) {
				// exception is thrown if there is no languageId() for the
				// current thread. if the transaction doesn't have a
				// languageId either, we emulate the previous behaviour
				// and just pass the exception on.
				if (null == this.session) {
					throw e;
				}
			}
			// the transaction may not be associated with a session
			if (null != this.session) {
				return this.session.getLanguageId();
			}
			logger.error("Transaction doesn't have a valid language ID");
			return 0;
		}

		@Override
		public boolean enableLevel2Cache(boolean enabled) {
			boolean wasEnabled = useLevel2Cache;
			useLevel2Cache = enabled;
			return wasEnabled;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#clearLevel2Cache()
		 */
		public void clearLevel2Cache() {
			level2Cache.clear();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getFromLevel2Cache(com.gentics.lib.base.object.NodeObject, java.lang.Object)
		 */
		public Object getFromLevel2Cache(NodeObject nodeObject, Object key) {
			return useLevel2Cache ? getNodeObjectLevel2Cache(nodeObject).get(key) : null;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#putIntoLevel2Cache(com.gentics.lib.base.object.NodeObject, java.lang.Object, java.lang.Object)
		 */
		public void putIntoLevel2Cache(NodeObject nodeObject, Object key, Object object) {
			if (!useLevel2Cache) {
				return;
			}
			Map<Object, Object> cache = getNodeObjectLevel2Cache(nodeObject);

			if (cache != Collections.EMPTY_MAP) {
				getNodeObjectLevel2Cache(nodeObject).put(key, object);
			}
		}

		/**
		 * Get the nodeobject specific level2 cache (create one if not yet done)
		 * @param nodeObject node object
		 * @return node object cache
		 */
		protected Map<Object, Object> getNodeObjectLevel2Cache(NodeObject nodeObject) {
			if (nodeObject == null || ObjectTransformer.getInt(nodeObject.getId(), -1) <= 0) {
				return Collections.emptyMap();
			}
			String cacheKey = nodeObject.getId() + nodeObject.getObjectInfo().getHashKey();
			Map<Object, Object> cache = level2Cache.get(cacheKey);

			if (cache == null) {
				cache = new HashMap<Object, Object>();
				level2Cache.put(cacheKey, cache);
			}

			return cache;
		}

		public void setTimestamp(long timestamp) {
			this.startTimestamp = timestamp;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getUnixTimestamp()
		 */
		public int getUnixTimestamp() {
			return (int) (getTimestamp() / 1000);
		}

		public RenderResult getRenderResult() {
			return renderResult;
		}

		public void setRenderResult(RenderResult renderResult) {
			this.renderResult = renderResult;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#prepareStatement(java.lang.String)
		 */
		public PreparedStatement prepareStatement(String sql) throws SQLException, TransactionException {
			return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#prepareStatement(java.lang.String, int, int)
		 */
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			if (logger.isDebugEnabled()) {
				logger.debug("prepareStatement(" + sql + ")");
			}
			return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}

		public PreparedStatement prepareStatement(String sql, int type) throws SQLException, TransactionException {
			switch (type) {
			case Transaction.INSERT_STATEMENT:
				return prepareInsertStatement(sql);

			case Transaction.UPDATE_STATEMENT:
				return prepareUpdateStatement(sql);

			case Transaction.DELETE_STATEMENT:
				return prepareDeleteStatement(sql);

			default:
				// select and everything else
				return prepareStatement(sql);
			}
		}

		public PreparedStatement prepareUpdateStatement(String sql) throws SQLException, TransactionException {
			return prepareStatement(sql);
		}

		public PreparedStatement prepareDeleteStatement(String sql) throws SQLException, TransactionException {
			return prepareStatement(sql);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#prepareInsertStatement(java.lang.String)
		 */
		public PreparedStatement prepareInsertStatement(String sql) throws SQLException, TransactionException {
			// waitForConnection();
			return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		}

		@Override
		public PreparedStatement prepareSelectForUpdate(String sql) throws SQLException, TransactionException {
			return prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#closeStatement(java.sql.PreparedStatement)
		 */
		public void closeStatement(PreparedStatement statement) {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					logger.warn("Error while closing the statement", e);
				}
			}
			// wakeUp();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#closeResultSet(java.sql.ResultSet)
		 */
		public void closeResultSet(ResultSet resultSet) {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					logger.warn("Error while closing the resultset", e);
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#closeStatement(java.sql.Statement)
		 */
		public void closeStatement(Statement statement) {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					logger.warn("Error while closing the statement", e);
				}
			}
			// wakeUp();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getStatement()
		 */
		public Statement getStatement() throws SQLException {
			// waitForConnection();
			return connection.createStatement();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getNodeConfig()
		 */
		public NodeConfig getNodeConfig() {
			return NodeConfigRuntimeConfiguration.getDefault().getNodeConfig();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getDBHandle()
		 */
		public DBHandle getDBHandle() {
			return dbHandle;
		}

		/**
		 * Connector implementation which contains the connection of the
		 * transaction. When the transaction is started, an instance of the
		 * connector is added to {@link DB}. And when the transaction is
		 * stopped, the connector is removed from the {@link DB}.
		 */
		protected class TransactionConnector implements Connector {
			/**
			 * Create an instance of the TransactionConnector.
			 */
			public TransactionConnector() {
			}

			/* (non-Javadoc)
			 * @see com.gentics.lib.db.Connector#close()
			 */
			public void close() throws SQLException {
			}

			/* (non-Javadoc)
			 * @see com.gentics.lib.db.Connector#getConnection()
			 */
			public PoolConnection getConnection() throws SQLException {
				return new PoolConnection((int) getId(), TransactionImpl.this.getConnection(), true);
			}

			/* (non-Javadoc)
			 * @see com.gentics.lib.db.Connector#releaseConnection(com.gentics.lib.db.PoolConnection)
			 */
			public void releaseConnection(PoolConnection c) throws SQLException {
				TransactionImpl.this.releaseConnection(c.getConnection());
			}
		}

		/**
		 * refreshed the perm handler for the current user
		 */
		public void refreshPermHandler() throws TransactionException {
			permHandler = new PermHandler();
			try {
				permHandler.initForUser(getUserId());
			} catch (NodeException e) {
				throw new TransactionException("Could not initialze PermHandler for user with id {" + getUserId() + "}", e);
			}
		}

		public Session getSession() {
			return session;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getAttributes()
		 */
		public Map<String, Object> getAttributes() {
			if (attributes == null) {
				attributes = new HashMap<String, Object>();
			}

			return attributes;
		}

		/**
		 * @see com.gentics.contentnode.factory.Transaction#getConnection()
		 */
		public Connection getConnection() {
			return connection;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#releaseConnection(java.sql.Connection)
		 */
		public void releaseConnection(Connection c) {
			// empty implementation, since this transaction implementation is
			// only used from a single thread and the connection is not locked.
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#setDisableMultichannellingFlag(boolean)
		 */
		public void setDisableMultichannellingFlag(boolean flag) {
			getDisableMultichannellingFlagStack().push(flag);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#resetDisableMultichannellingFlag()
		 */
		public void resetDisableMultichannellingFlag() {
			getDisableMultichannellingFlagStack().pop();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isDisableMultichannellingFlag()
		 */
		public boolean isDisableMultichannellingFlag() {
			if (getDisableMultichannellingFlagStack().empty()) {
				return false;
			} else {
				return getDisableMultichannellingFlagStack().peek();
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#setChannel(java.lang.Object)
		 */
		public void setChannelId(Integer channelId) {
			Node channel = null;
			if (channelId != null) {
				try {
					channel = getObject(Node.class, channelId, -1, false);
				} catch (NodeException e) {
					logger.error("Error while setting channel to " + channelId, e);
				}
				if (channel == null) {
					logger.warn("Error while setting channel to " + channelId + ": channel does not exist");
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Set channel ID to {" + channelId + "}");
			}
			getChannelStack().push(channel);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#resetChannel()
		 */
		public void resetChannel() {
			getChannelStack().pop();
			if (logger.isDebugEnabled()) {
				logger.debug("Reset channel ID to {" + getChannelId() + "}.");
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getChannel()
		 */
		public Integer getChannelId() {
			if (getChannelStack().empty()) {
				return null;
			} else {
				Node channel = getChannelStack().peek();
				if (channel == null) {
					return null;
				} else {
					return channel.getId();
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getChannel()
		 */
		public Node getChannel() {
			if (getChannelStack().empty()) {
				return null;
			} else {
				return getChannelStack().peek();
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.factory.Transaction#getPublishedNodeId()
		 */
		public Integer getPublishedNodeId() {
			return publishedNode.get();
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.contentnode.factory.Transaction#setPublishedNodeId(java.lang.Integer)
		 */
		public void setPublishedNodeId(Integer nodeId) {
			publishedNode.set(nodeId);
		}

		@Override
		public PublishedNodeTrx initPublishedNode(Node node) throws NodeException {
			if (getPublishedNodeId() == null && node != null) {
				return new PublishedNodeTrx(node);
			} else {
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canView(com.gentics.lib.base.object.NodeObject)
		 */
		public boolean canView(NodeObject object) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canView(object);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canEdit(com.gentics.lib.base.object.NodeObject)
		 */
		public boolean canEdit(NodeObject object) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canEdit(object);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canPublish(com.gentics.lib.base.object.NodeObject)
		 */
		public boolean canPublish(NodeObject object) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canPublish(object);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canDelete(com.gentics.lib.base.object.NodeObject)
		 */
		public boolean canDelete(NodeObject object) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canDelete(object);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canView(com.gentics.contentnode.object.Folder, java.lang.Class)
		 */
		public boolean canView(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canView(f, clazz, language);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canCreate(com.gentics.contentnode.object.Folder, java.lang.Class)
		 */
		public boolean canCreate(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canCreate(f, clazz, language);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canEdit(com.gentics.contentnode.object.Folder, java.lang.Class)
		 */
		public boolean canEdit(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canEdit(f, clazz, language);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canDelete(com.gentics.contentnode.object.Folder, java.lang.Class)
		 */
		public boolean canDelete(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canDelete(f, clazz, language);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#canPublish(com.gentics.contentnode.object.Folder, java.lang.Class)
		 */
		public boolean canPublish(Folder f, Class<? extends NodeObject> clazz, ContentLanguage language) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canPublish(f, clazz, language);
			}
		}

		@Override
		public boolean canWastebin(Node n) throws NodeException {
			// when no perm handler is set, we allow everything
			if (permHandler == null) {
				return true;
			} else {
				return permHandler.canWastebin(n);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getFieldData(com.gentics.lib.base.object.NodeObject)
		 */
		public Map<String, Object> getFieldData(NodeObject object) throws NodeException {
			return factoryHandle.getFieldData(object);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#setFieldData(com.gentics.lib.base.object.NodeObject, java.util.Map)
		 */
		public void setFieldData(NodeObject object, Map<String, Object> dataMap) throws NodeException {
			factoryHandle.setFieldData(object, dataMap);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#setInstantPublishingEnabled(boolean)
		 */
		public void setInstantPublishingEnabled(boolean instantPublishing) {
			this.instantPublishing = instantPublishing;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isInstantPublishingEnabled()
		 */
		public boolean isInstantPublishingEnabled() {
			return this.instantPublishing;
		}

		@Override
		public boolean usePublishablePages() {
			// with this feature, it is possible to disable the versioned rendering
			// of pages while publishing
			if (getNodeConfig().getDefaultPreferences().getFeature("disable_versioned_publishing")) {
				return false;
			}
			RenderType renderType = getRenderType();

			if (renderType == null) {
				return false;
			}
			if (renderType.getEditMode() != RenderType.EM_PUBLISH) {
				return false;
			}

			return !isDisableVersionedPublish() && isPublishCacheEnabled();
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#setDisableVersionedPublish(boolean)
		 */
		public void setDisableVersionedPublish(boolean flag) {
			disableVersionedPublishing.set(flag);

		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#isDisableVersionedPublish()
		 */
		public boolean isDisableVersionedPublish() {
			return ObjectTransformer.getBoolean(disableVersionedPublishing.get(), false);

		}

		@Override
		public void setPublishCacheEnabled(boolean flag) {
			enablePublishCache.set(flag);
		}

		@Override
		public boolean isPublishCacheEnabled() {
			return getNodeConfig().getDefaultPreferences().isFeature(Feature.PUBLISH_CACHE) && ObjectTransformer.getBoolean(enablePublishCache.get(), true);
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#keepAlive()
		 */
		public synchronized void keepAlive() {
			long now = System.currentTimeMillis();

			if (now - lastCheck > KEEPALIVE_INTERVAL) {
				if (logger.isDebugEnabled()) {
					logger.debug("Last access to " + this + " was @" + lastCheck + ", keeping connection alive now.");
				}
				keepConnectionAlive();
				lastCheck = now;
			}
		}

		/**
		 * Internal method to keep the connection alive
		 */
		protected void keepConnectionAlive() {
			PreparedStatement pst = null;

			try {
				DBHandle handle = getDBHandle();
				String dummyStatement = handle.getDummyStatement();

				if (dummyStatement != null) {
					// we deliberately declare this statement as insert statement to get the writable connection
					pst = prepareStatement(dummyStatement, Transaction.INSERT_STATEMENT);
					pst.execute();
				} else {
					logger.warn("Could not keep connection alive, because no dummy statement was found");
				}
			} catch (Exception e) {
				logger.warn("Error while keeping connection alive", e);
			} finally {
				closeStatement(pst);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#prepareVersionedObjects(java.lang.Class, java.lang.Class, java.util.Map)
		 */
		public <T extends NodeObject> Set<T> prepareVersionedObjects(Class<T> clazz, Class<? extends NodeObject> mainClazz, Map<Integer, Integer> timestamps) throws NodeException {
			ObjectFactory objectFactory = factoryHandle.getObjectFactory(clazz);
			if (objectFactory instanceof BatchObjectFactory) {
				return ((BatchObjectFactory) objectFactory).batchLoadVersionedObjects(factoryHandle, clazz, mainClazz, timestamps);
			} else {
				return Collections.emptySet();
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#enableStatistics(boolean)
		 */
		public void enableStatistics(boolean enable) {
			if (enable && stats == null) {
				stats = new TransactionStatistics();
			} else if (!enable) {
				stats = null;
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getStatistics()
		 */
		public TransactionStatistics getStatistics() {
			return stats;
		}

		/**
		 * Start measuring object load (if enabled)
		 */
		protected void startObjectLoad() {
			if (stats != null) {
				stats.get(Item.FETCH_OBJECT).start();
			}
		}

		/**
		 * End measuring object load (if enabled)
		 * @param clazz object class
		 */
		protected void endObjectLoad(Class<? extends NodeObject> clazz) {
			if (stats != null) {
				if (clazz != null) {
					stats.get(Item.FETCH_OBJECT).stop(1, clazz.getSimpleName());
				} else {
					stats.get(Item.FETCH_OBJECT).stop();
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#prepareNodeStructure()
		 */
		public void preparePublishData() throws NodeException {
			// do not prepare publish data, if we are not publishing
			// or versioned publishing is disabled
			RenderType type = getRenderType();
			if (type == null) {
				return;
			}
			if (getNodeConfig().getDefaultPreferences().getFeature("disable_versioned_publishing")) {
				return;
			}
			if (type.getEditMode() != RenderType.EM_PUBLISH) {
				return;
			}
			if (nodeStructure == null) {
				nodeStructure = new PublishData();
			}
		}

		@Override
		public void setPublishData(PublishData publishData) {
			this.nodeStructure = publishData;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#getNodeStructure()
		 */
		public PublishData getPublishData() {
			return nodeStructure;
		}

		/* (non-Javadoc)
		 * @see com.gentics.lib.base.factory.Transaction#resetNodeStructure()
		 */
		public void resetPublishData() {
			nodeStructure = null;
		}

		@Override
		public <T extends NodeObject> T getObject(Class<T> clazz, String id) throws NodeException {
			return getObject(clazz, id, false);
		}

		@Override
		public <T extends NodeObject> T getObject(Class<T> clazz, String id, boolean forUpdate) throws NodeException, ReadOnlyException {
			return getObject(clazz, id, forUpdate, true);
		}

		@Override
		public <T extends NodeObject> T getObject(Class<T> clazz, String id, boolean forUpdate, boolean multichannelFallback) throws NodeException {
			if (GlobalId.isGlobalId(id)) {
				return getObject(clazz, new GlobalId(id).getLocalId(clazz), forUpdate, multichannelFallback, true);
			} else {
				return getObject(clazz, ObjectTransformer.getInteger(id, null), forUpdate, multichannelFallback, true);
			}
		}

		@Override
		public <T extends NodeObject> T getObject(Class<T> clazz, GlobalId id, boolean forUpdate) throws NodeException, ReadOnlyException {
			if (id == null) {
				return null;
			} else {
				return getObject(clazz, id.getLocalId(clazz), forUpdate);
			}
		}

		@Override
		public <T extends NodeObject> T getObject(Class<T> clazz, GlobalId id) throws NodeException, ReadOnlyException {
			return getObject(clazz, id, false);
		}

		@Override
		public <T extends NodeObject> List<T> getObjectsByStringIds(Class<T> clazz, Collection<String> ids) throws NodeException {
			List<Integer> transformedIds = new ArrayList<Integer>();
			if (ids == null) {
				return null;
			}
			for (String currentId : ids) {
				if (GlobalId.isGlobalId(currentId)) {
					GlobalId objectId = new GlobalId(currentId);
					transformedIds.add(objectId.getLocalId(clazz));
				} else {
					Integer objectId = ObjectTransformer.getInteger(currentId, null);
					transformedIds.add(objectId);
				}
			}
			return getObjects(clazz, transformedIds);
		}

		@Override
		public Wastebin setWastebinFilter(Wastebin filter) {
			Wastebin oldFilter = getWastebinFilter();
			wastebinFilter.set(filter != null ? filter : Wastebin.getDefault());
			return oldFilter;
		}

		@Override
		public Wastebin getWastebinFilter() {
			return wastebinFilter.get();
		}

		@Override
		public boolean isCheckAnyChannel() {
			return ObjectTransformer.getBoolean(checkAnyChannel.get(), false);
		}

		@Override
		public void setCheckAnyChannel(boolean flag) {
			checkAnyChannel.set(flag ? flag : null);
		}

		@Override
		public void addInstantPublishingResult(NodeObject object, Result result) {
			Integer objType = object.getTType();
			Integer objId = object.getId();
			instantPublishingResults.computeIfAbsent(objType, k -> new HashMap<>()).put(objId, result);
		}

		@Override
		public Result getInstantPublishingResult(int objType, int objId) {
			return instantPublishingResults.getOrDefault(objType, Collections.emptyMap()).get(objId);
		}
	}

	/**
	 * JMX Bean implementation
	 */
	protected static class TransactionInfo extends StandardMBean implements TransactionInfoMBean {
		/**
		 * Total Transactions
		 */
		private long total = 0;

		/**
		 * Open Transactions
		 */
		private int open = 0;

		/**
		 * Maximum number of open transactions
		 */
		private int maxOpen = 0;

		public TransactionInfo() throws NotCompliantMBeanException {
			super(TransactionInfoMBean.class);
			MBeanRegistry.registerMBean(this, "System", "TrxInfo");
		}

		@Override
		public long getTotal() {
			return total;
		}

		@Override
		public int getOpen() {
			return open;
		}

		@Override
		public int getMaxOpen() {
			return maxOpen;
		}

		/**
		 * Increase number of open transactions
		 */
		void inc() {
			total++;
			open++;
			maxOpen = Math.max(maxOpen, open);
		}

		/**
		 * Decrease number of open transactions
		 */
		void dec() {
			open--;
		}
	}
}
