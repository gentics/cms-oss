/*
 * @author herbert
 * @date Feb 8, 2008
 * @version $Id: CNInvokerQueue.java,v 1.6 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.servlet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.distributed.DistributionUtil;
import com.gentics.contentnode.factory.ContentNodeFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.servlet.queue.ErrorQueueEntry;
import com.gentics.contentnode.servlet.queue.InvokerQueueEntry;
import com.gentics.contentnode.servlet.queue.NodeCopyQueueEntry;
import com.gentics.contentnode.servlet.queue.SleepingQueueEntry;
import com.gentics.lib.db.SQLExecutor;
import com.gentics.lib.log.NodeLogger;

/**
 * A simple queue which allows queueing of structure copy,
 * import/export invoke commands.
 * 
 * @author herbert
 */
public class CNInvokerQueue implements Runnable {

	private static CNInvokerQueue instance = null;
	private static NodeLogger logger = NodeLogger.getNodeLogger(CNInvokerQueue.class);

	private static InvokerQueueEntry[] entryTypes = new InvokerQueueEntry[] {
		new SleepingQueueEntry(), new ErrorQueueEntry(), new NodeCopyQueueEntry()};

	private InvokerQueueEntry currentlyRunningQueueEntry = null;
	private ContentNodeFactory factory;
	private Thread thread;

	/**
	 * Flag to mark whether the thread should be stopped
	 */
	private boolean running = true;

	/**
	 * Create instance and start the background thread
	 * @param factory factory
	 */
	protected CNInvokerQueue(ContentNodeFactory factory) {
		// Start event processor ..
		this.factory = factory;
		this.running = true;
		this.thread = new Thread(this, "CNInvokerQueue");
		// The thread is never closed in a nice way so it has to be a deamon. Otherwise Tomcat is not able to stop. 
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Get the instance, if the instance does not exist, create one
	 * @param factory factory
	 * @return instance
	 */
	public static synchronized CNInvokerQueue getDefault(ContentNodeFactory factory) {
		if (instance == null) {
			instance = new CNInvokerQueue(factory);
		} else {
			instance.assureRunningThread();
		}
		return instance;
	}

	/**
	 * Shutdown the instance
	 */
	public static synchronized void shutdown() {
		if (instance != null) {
			instance.stopThread();
			instance = null;
		}
	}

	/**
	 * Add a queue entry
	 * @param entry entry
	 * @throws DuplicateEntryException
	 */
	public void addQueueEntry(final InvokerQueueEntry entry) throws DuplicateEntryException {
		synchronized (this) {
			// First check if it is already in the queue ..
			final boolean[] exists = new boolean[] { false };

			try {
				startTransaction(null);
				DBUtils.executeStatement("SELECT count(id) as c FROM invokerqueue WHERE type = ? AND idparam = ?", new SQLExecutor() {

					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setString(1, entry.getType());
						stmt.setString(2, entry.getIdParameter());
					}

					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						rs.next();
						int c = rs.getInt("c");

						if (logger.isDebugEnabled()) {
							logger.debug("adding queue entry ... found {" + c + "} entries matching query.");
						}
						exists[0] = c > 0;
					}
				});

				if (exists[0]) {
					throw new DuplicateEntryException("Item is already in the queue.");
				}

				DBUtils.executeUpdate("INSERT INTO invokerqueue (type, idparam, additionalparams, date) VALUES (?,?,?,?)",
						new Object[] { entry.getType(), entry.getIdParameter(), entry.getAdditionalParameters(), new Long(System.currentTimeMillis() / 1000)});
			} catch (DuplicateEntryException e) {
				throw e;
			} catch (NodeException e) {
				throw new RuntimeException(e);
			} finally {
				stopTransaction();
			}

			assureRunningThread();

			// Notify the processor ...
			this.notify();
		}
	}

	/**
	 * Make sure, that the thread is still alive. If not, create a new thread and start it
	 */
	public void assureRunningThread() {
		// make sure the thread is still running
		if (!thread.isAlive()) {
			logger.error("It seems the CNInvokerQueue thread has died - trying to restart it.");
			thread = new Thread(this, "CNInvokerQueue");
			thread.setDaemon(true);
			thread.start();
		}
	}

	@Override
	public void run() {
		// wait for new invoker entries ..
		// TODO make sure this thread can be killed.
		final InvokerQueueEntry[] currentQueueEntry = new InvokerQueueEntry[1];
		final int[] currentQueueEntryId = new int[1];

		while (running) {
			try {
				synchronized (this) {
					currentQueueEntry[0] = null;

					if (DistributionUtil.isTaskExecutionAllowed()) {
						// Search for next queue entry ..
						try {
							logger.debug("Searching for next queue entry");
							startTransaction(null);
							DBUtils.executeStatement("SELECT id, type, idparam, additionalparams FROM invokerqueue ORDER BY date LIMIT 1", new SQLExecutor() {

								public void prepareStatement(PreparedStatement stmt) throws SQLException {}

								public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
									while (rs.next()) {
										currentQueueEntryId[0] = rs.getInt("id");
										String type = rs.getString("type");
										String idparam = rs.getString("idparam");
										String additionalparams = rs.getString("additionalparams");
										InvokerQueueEntry entry = getQueueEntryOfType(type);

										if (entry == null) {
											throw new RuntimeException("Unknown queue entry type {" + type + "}");
										}
										InvokerQueueEntry queueEntry = entry.createQueueEntry(type, idparam, additionalparams);

										currentQueueEntry[0] = queueEntry;
									}
								}
							});
						} catch (NodeException e) {
							throw new RuntimeException(e);
						} finally {
							stopTransaction();
						}
					} else {
						logger.debug("Instance does not allow task execution");
					}

					if (currentQueueEntry[0] != null) {
						// Set the curerntly running queue entry ...
						currentlyRunningQueueEntry = currentQueueEntry[0];
					} else {
						// There is no item in the queue - waiting
						try {
							logger.debug("No queue entry found, waiting ...");
							this.wait(15 * 60 * 1000);
						} catch (InterruptedException e) {
							if (running) {
								logger.error("Error while waiting for new queue entries.", e);
							}
						}
					}
				}
				if (currentQueueEntry[0] != null) {
					logger.debug("Invoking queue entry ...");
					try {
						startTransaction(ObjectTransformer.getInteger(currentlyRunningQueueEntry.getParameter("userId"), null));
						currentlyRunningQueueEntry.invoke();
						// Delete it from the database
						// TODO make sure one entry is deleted ..
						DBUtils.executeUpdate("DELETE FROM invokerqueue WHERE id = ?", new Object[] { new Integer(currentQueueEntryId[0])});
					} finally {
						stopTransaction();
						currentlyRunningQueueEntry.cleanUp();
						currentlyRunningQueueEntry = null;
					}
				}
			} catch (Throwable e) {
				logger.fatal("Error while processing invoker queue", e);
				try {
					synchronized (this) {
						// avoid endless loops with 100% CPU usage by waiting a second ..
						this.wait(1000);
					}
				} catch (InterruptedException e1) {
					if (running) {
						logger.error("error while waiting ...", e);
					}
				}
			}
		}

		logger.info("Thread was stopped");
	}

	/**
	 * Stop the current transaction
	 */
	private void stopTransaction() {
		Transaction transaction;

		try {
			transaction = TransactionManager.getCurrentTransaction();
			transaction.commit(true);
			TransactionManager.setCurrentTransaction(null);
		} catch (TransactionException e) {
			logger.error("Error while stopping transaction", e);
		}
	}

	/**
	 * Start the transaction for the user
	 * @param userId user ID
	 */
	private void startTransaction(Integer userId) {
		Transaction transaction;

		try {
			transaction = factory.startTransaction(null, null, true);
			TransactionManager.setCurrentTransaction(transaction);
		} catch (NodeException e) {
			logger.fatal("Error while starting transaction", e);
		}
	}

	public static InvokerQueueEntry getQueueEntryOfType(String type) {
		for (int i = 0; i < entryTypes.length; i++) {
			if (type.equals(entryTypes[i].getType())) {
				return entryTypes[i];
			}
		}
		return null;
	}

	public static class DuplicateEntryException extends NodeException {
		private static final long serialVersionUID = -1580023959197100427L;

		public DuplicateEntryException(String message) {
			super(message);
		}
	}

	/**
	 * returns the position in the queue for the given type/idparam.
	 * @return -1: Not found / 0: In Progress / or the position in the queue
	 */
	public int getQueuePosition(final String type, final String idparam) throws NodeException {
		// select count(id) from invokerqueue where date < (select date from invokerqueue where type = 'sleep' and idparam = 'd');
		final int[] ret = new int[1];

		synchronized (this) {
			startTransaction(null);
			try {
				DBUtils.executeStatement("SELECT COUNT(id) AS c FROM invokerqueue WHERE date <= (SELECT date FROM invokerqueue WHERE type = ? AND idparam = ?)",
						new SQLExecutor() {

					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setString(1, type);
						stmt.setString(2, idparam);
					}

					public void handleResultSet(ResultSet rs) throws SQLException, NodeException {
						rs.next();
						int c = rs.getInt("c");

						ret[0] = c - 1;
					}
				});
			} finally {
				stopTransaction();
			}

			if (ret[0] == 0) {
				if (currentlyRunningQueueEntry != null) {
					// if it is 0 it should be running right now
					if (!type.equals(currentlyRunningQueueEntry.getType()) || !idparam.equals(currentlyRunningQueueEntry.getIdParameter())) {
						// ok .. this is weird...
						logger.warn("queue position 0 is not running, but instead: {" + currentlyRunningQueueEntry.toString() + "}");
					}
				}
			}
		}
		return ret[0];
	}

	/**
	 * @return -2 if the specified type/idparam is not currently running, -1 if not implemented, 0-100 otherwise
	 */
	public int getProgress(String type, String idparam) {
		synchronized (this) {
			if (currentlyRunningQueueEntry == null || !type.equals(currentlyRunningQueueEntry.getType())
					|| !idparam.equals(currentlyRunningQueueEntry.getIdParameter())) {
				return -2;
			}
			return currentlyRunningQueueEntry.getProgress();
		}
	}

	/**
	 * Cancel the running queue entry
	 * @param type
	 * @param idparam
	 */
	public void cancel(String type, String idparam) {
		synchronized (this) {
			if (logger.isInfoEnabled()) {
				logger.info("Cancelling queue entry {" + type + "} {" + idparam + "}.");
			}
			if (currentlyRunningQueueEntry == null || !type.equals(currentlyRunningQueueEntry.getType())
					|| !idparam.equals(currentlyRunningQueueEntry.getIdParameter())) {
				return;
			}
			thread.interrupt();
		}
	}

	/**
	 * Stop the queue thread
	 */
	private void stopThread() {
		running = false;
		if (thread.isAlive()) {
			thread.interrupt();
		}
		thread = null;
	}
}
