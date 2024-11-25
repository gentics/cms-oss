/*
 * @author norbert
 * @date 09.10.2006
 * @version $Id: RoundRobinHandlePool.java,v 1.4 2009-12-16 16:12:08 herbert Exp $
 */
package com.gentics.lib.datasource;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.datasource.DatasourceDefinition;
import com.gentics.api.lib.datasource.DatasourceHandle;
import com.gentics.api.lib.datasource.HandlePool;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.log.NodeLogger;

/**
 * RoundRobin handlepool with alive-checking of handles
 */
public class RoundRobinHandlePool implements HandlePool {

	/**
	 * last used handle index
	 */
	private int handleIndex = 0;

	/**
	 * handles (with availability information)
	 */
	private HandleWithTimeout[] handles;

	/**
	 * type id
	 */
	private String typeId;

	/**
	 * string representation of the handle pool
	 */
	private String stringRepresentation;

	/**
	 * logger
	 */
	protected final static NodeLogger logger = NodeLogger.getNodeLogger(RoundRobinHandlePool.class);

	/**
	 * array of timeouts to be used for handles that are marked unavailable (1 minute, 5 minutes, 10 minutes)
	 */
	private final static int[] UNAVAILABLE_TIMEOUTS = new int[] { 60 * 1000, 5 * 60 * 1000, 10 * 60 * 1000};

	/**
	 * default value for the background validation interval (10 mins)
	 */
	public final static int BACKGROUND_VALIDATION_INTERVAL_DEFAULT = 10 * 60 * 1000;

	/**
	 * interval for the background validation (in ms)
	 */
	protected int backgroundValidationInterval = BACKGROUND_VALIDATION_INTERVAL_DEFAULT;

	/**
	 * background validation job
	 */
	protected ValidationJob validationJob;

	/**
	 * whether background validation shall be done
	 */
	protected boolean backgroundValidation = false;

	/**
	 * Create instance of the round robin handle pool
	 * @param handlesList handles list
	 */
	public RoundRobinHandlePool(LinkedList handlesList, boolean backgroundValidation, int backgroundValidationInterval) {
		this((DatasourceHandle[]) handlesList.toArray(new DatasourceHandle[handlesList.size()]), backgroundValidation, backgroundValidationInterval);
	}

	/**
	 * Create instance of the round robin handle pool
	 * @param dshandles handles list
	 */
	public RoundRobinHandlePool(DatasourceHandle[] dshandles, boolean backgroundValidation, int backgroundValidationInterval) {
		this.backgroundValidationInterval = backgroundValidationInterval;
		this.backgroundValidation = backgroundValidation;
		if (ObjectTransformer.isEmpty(dshandles)) {
			throw new IllegalArgumentException("Cannot create HandlePool with empty list of handles");
		}

		handles = new HandleWithTimeout[dshandles.length];
		for (int i = 0; i < dshandles.length; i++) {
			handles[i] = new HandleWithTimeout(dshandles[i]);
		}

		DatasourceDefinition def = dshandles[0].getDatasourceDefinition();

		if (def != null) {
			typeId = def.getID();
		}

		if (dshandles.length < 2 && this.backgroundValidation) {
			this.backgroundValidation = false;
			if (logger.isInfoEnabled()) {
				logger.info("Handle pool for datasouce {" + typeId + "} only contains one handle, not starting background validation job");
			}
		}

		if (this.backgroundValidation) {
			// create and start the background validation job
			validationJob = new ValidationJob();
			validationJob.start();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.HandlePool#getHandle()
	 */
	public DatasourceHandle getHandle() {
		DBHandle currentHandle = DB.getCurrentHandle();
		if (currentHandle != null) {
			SQLHandle sqlHandle = currentHandle.getSqlHandle();

			if (sqlHandle != null) {
				for (HandleWithTimeout handle : handles) {
					if (sqlHandle == handle.handle) {
						return sqlHandle;
					}
				}
			}
		}

		return getNextWorkingHandle();
	}

	/**
	 * Get the next handle (round robin)
	 * @return handle
	 */
	private DatasourceHandle getNextWorkingHandle() {
		// get the next handle index (round robin)
		int startIndex = getNextIndex();
		int currentIndex = startIndex;
		List triedHandles = new Vector();

		// first get a free, working handle
		do {
			if (handles[currentIndex].willValidate() && handles[currentIndex].canTry()) {
				try {
					triedHandles.add(handles[currentIndex]);
					if (handles[currentIndex].isAlive(false)) {
						return handles[currentIndex].getHandle();
					}
				} finally {
					handles[currentIndex].releaseTry();
				}
			}

			// the latest handle is tried by another thread or is not available
			currentIndex++;
			if (currentIndex >= handles.length) {
				currentIndex = 0;
			}
		} while (currentIndex != startIndex);

		if (logger.isDebugEnabled()) {
			logger.debug("Not found an active handle in the first round, trying again");
		}

		// could not find a free, available handle
		// now check the handles that were not checked before (but should revalidate)
		do {
			if (handles[currentIndex].willValidate() && !triedHandles.contains(handles[currentIndex])) {
				triedHandles.add(handles[currentIndex]);
				if (handles[currentIndex].isAlive(false)) {
					return handles[currentIndex].getHandle();
				}
			}

			// get the next index
			currentIndex++;
			if (currentIndex >= handles.length) {
				currentIndex = 0;
			}
		} while (currentIndex != startIndex);

		if (logger.isDebugEnabled()) {
			logger.debug("Not found an active handle in the second round, trying again (force revalidation)");
		}

		// last ressort, force retrying of handles
		do {
			if (!triedHandles.contains(handles[currentIndex])) {
				if (handles[currentIndex].isAlive(true)) {
					return handles[currentIndex].getHandle();
				}
			}

			// get the next index
			currentIndex++;
			if (currentIndex >= handles.length) {
				currentIndex = 0;
			}
		} while (currentIndex != startIndex);

		return null;
	}

	/**
	 * Get the next index (round robin)
	 * @return next index
	 */
	private synchronized int getNextIndex() {
		handleIndex++;
		if (handleIndex >= handles.length) {
			handleIndex = 0;
		}

		return handleIndex;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.HandlePool#close()
	 */
	public void close() {
		for (int i = 0; i < handles.length; i++) {
			handles[i].getHandle().close();
		}

		if (validationJob != null) {
			validationJob.interrupt();
			validationJob = null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.datasource.HandlePool#getTypeID()
	 */
	public String getTypeID() {
		return typeId;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		synchronized (this) {
			if (stringRepresentation == null) {
				StringBuffer buffer = new StringBuffer();

				for (int i = 0; i < handles.length; i++) {
					if (i != 0) {
						buffer.append("-");
					}
					buffer.append(handles[i]);
				}
				stringRepresentation = buffer.toString();
			}
			return stringRepresentation;
		}
	}

	/**
	 * Helper class for handles with availability marking
	 */
	private class HandleWithTimeout {

		/**
		 * flag to mark handles that are currently tried
		 */
		protected boolean currentlyTried = false;

		/**
		 * the handle
		 */
		protected DatasourceHandle handle;

		/**
		 * timestamp after which this handle might be tried again, set to -1 for
		 * handles marked as available
		 */
		protected long tryAgainAfter = -1;

		/**
		 * next used timeout step
		 */
		protected int timeoutStep = 0;

		/**
		 * timestamp of the last time this handle was checked (to prevent
		 * checking it too often)
		 */
		protected long lastChecked = 0;

		/**
		 * Create an instance of the handle
		 * @param handle handle
		 */
		public HandleWithTimeout(DatasourceHandle handle) {
			this.handle = handle;
		}
 
		/**
		 * Check whether the handle may be tried. When this method returns true,
		 * the flag {@link #currentlyTried} will be set to true and following
		 * calls (by other threads) will return false, until {@link #releaseTry()} is called
		 * @return true when the handle is currently free for trying, false if
		 *         not
		 */
		public synchronized boolean canTry() {
			if (currentlyTried) {
				return false;
			} else {
				currentlyTried = true;
				return true;
			}
		}

		/**
		 * Check whether this handle will be validated on a call to
		 * {@link #isAlive(boolean)} with forceRevalidate set to false.
		 * @return true when this handle will be validated, false if not
		 */
		public boolean willValidate() {
			boolean willValidate = tryAgainAfter < System.currentTimeMillis();

			if (!willValidate && logger.isDebugEnabled()) {
				logger.debug(
						"Handle " + handle + " will next be validated @ " + tryAgainAfter + " (in " + (tryAgainAfter - System.currentTimeMillis()) / 1000 + " s)");
			}
			return willValidate;
		}

		/**
		 * Check whether the handle is currently tried
		 * @return true when the handle is currently tried
		 */
		public boolean isTried() {
			return currentlyTried;
		}

		/**
		 * release this handle from trying
		 *
		 */
		public synchronized void releaseTry() {
			currentlyTried = false;
		}

		/**
		 * Check whether the server connection defined by this handle is
		 * alive or not.
		 * @param forceRevalidate true when handles marked as unavailable before
		 *        shall be checked anyway
		 * @return true when the handle is alive, false if not or may not yet be checked again
		 */
		public boolean isAlive(boolean forceRevalidate) {
			long now = System.currentTimeMillis();

			// in any case prevent rechecking of the same handle with less than 5 secs in between
			if ((!forceRevalidate || (now < lastChecked + 5000)) && tryAgainAfter > now) {
				if (logger.isDebugEnabled()) {
					logger.debug("Handle " + handle + " will next be validated @ " + tryAgainAfter + " (in " + (tryAgainAfter - now) / 1000 + " s)");
				}
				// this handle is marked unavailable and may not yet be
				// revalidated
				return false;
			} else {
				lastChecked = now;
				// check whether the handle is available
				if (handle.isAlive()) {
					// the handle is alive
					tryAgainAfter = -1;
					timeoutStep = 0;
					return true;
				} else {
					// handle is not alive, so mark it as unavailable
					if (!backgroundValidation) {
						tryAgainAfter = now + UNAVAILABLE_TIMEOUTS[timeoutStep];
					} else {
						// when background validation is done, set the interval
						// so high that normally no productive query would test
						// this handle again
						tryAgainAfter = now + backgroundValidationInterval * 2;
					}

					if (logger.isDebugEnabled()) {
						logger.debug(
								"Handle " + handle + " is marked unavailable and will be rechecked @ " + tryAgainAfter + " (in " + (tryAgainAfter - now) / 1000
								+ " s)");
					}

					if (!backgroundValidation && timeoutStep < UNAVAILABLE_TIMEOUTS.length - 1) {
						timeoutStep++;
					}
					return false;
				}
			}
		}

		/**
		 * Get the handle
		 * @return the handle
		 */
		public DatasourceHandle getHandle() {
			return handle;
		}

		/**
		 * Check whether the handle is currently marked unavailable
		 * @return true when the handle is marked unavailable, false if not
		 */
		public boolean isMarkedUnavailable() {
			return tryAgainAfter >= 0;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return handle.toString();
		}
	}

	/**
	 * Internal class for the background validation job
	 */
	private class ValidationJob extends Thread {

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			if (logger.isInfoEnabled()) {
				logger.info("Background validation job for handle pool {" + RoundRobinHandlePool.this.toString() + "} started.");
			}

			boolean interrupted = false;

			while (!interrupted) {
				// do the handle check here
				for (int i = 0; i < handles.length; i++) {
					// do not check handles that are currently checked by someone else
					if (handles[i].canTry()) {
						if (logger.isInfoEnabled()) {
							logger.info("Checking handle {" + handles[i] + "} now");
						}
						try {
							boolean wasAlive = !handles[i].isMarkedUnavailable();
							// force checking of the handle (marks unavailable handles)
							boolean handleAlive = handles[i].isAlive(true);

							if (logger.isInfoEnabled()) {
								if (!handleAlive && wasAlive) {
									Exception e = handles[i].handle.getLastException();

									logger.warn(
											"Handle {" + handles[i] + "} is not alive and temporarily taken out of service. Will be checked again in "
											+ backgroundValidationInterval + " ms",
											e);
								} else if (!handleAlive && !wasAlive) {
									logger.warn("Handle {" + handles[i] + "} still not alive, will check again in " + backgroundValidationInterval + " ms");
								} else if (!wasAlive) {
									logger.info("Handle {" + handles[i] + "} was unavailable and is now available again.");
								}
							}
						} finally {
							handles[i].releaseTry();
						}
					}
				}

				// sleep for the defined time period
				try {
					Thread.sleep(backgroundValidationInterval);
				} catch (InterruptedException e) {
					// when the sleeping was interrupted, we stop the thread
					interrupted = true;
					if (logger.isInfoEnabled()) {
						logger.info("Background validation job for handle pool {" + RoundRobinHandlePool.this.toString() + "} stopped.");
					}
				}
			}
		}
	}
}
