/*
 * PoolImpl.java
 *
 * Created on 26. August 2004, 23:56
 */

package com.gentics.lib.pooling;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

import com.gentics.lib.log.NodeLogger;

/**
 * @author Dietmar
 */
public class SimpleObjectPool implements PoolInterface {

	protected java.util.Stack freeObjects = null;

	protected java.util.HashSet pooledObjects = null;

	protected PoolFactoryInterface poolableCreationFactoryInterface = null;

	protected LinkedHashSet queuedObjects = null;

	protected int currentCount = 0;

	protected int minimum = 0;

	protected int maximum = 0;

	protected boolean blockOverflow = false;

	protected String name = "";

	protected int stepSize = 0;

	private final static int MINIMUMSTEPSIZE = 3;

	private final static int STANDARDSTEPCOUNT = 5;

	public SimpleObjectPool(PoolFactoryInterface creationFactory) throws PoolingException, IllegalAccessException {
		this(10, 20, creationFactory, false);
	}

	public SimpleObjectPool(PoolFactoryInterface creationFactory, boolean blockOverflow) throws PoolingException, IllegalAccessException {
		this(10, 20, creationFactory, blockOverflow);
	}

	public SimpleObjectPool(int minimalsize, int maximalsize,
			PoolFactoryInterface creationFactoryInterface) throws PoolingException,
				IllegalAccessException {
		this(minimalsize, maximalsize, creationFactoryInterface, false);
	}

	/** Creates a new instance of PoolImpl */
	
	/**
	 * @param minimalsize
	 * @param maximalsize
	 * @param creationFactoryInterface
	 * @param blockOverflow if true, the pool will lock and wait if there are no
	 *        more free objects, if false, it will simply throw an exception
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public SimpleObjectPool(int minimalsize, int maximalsize,
			PoolFactoryInterface creationFactoryInterface, boolean blockOverflow) throws PoolingException, IllegalAccessException {
		minimum = minimalsize;
		maximum = maximalsize;
		this.blockOverflow = blockOverflow;
		// check if the object class implements poolable
		pooledObjects = new HashSet(minimum);
		freeObjects = new java.util.Stack();
		poolableCreationFactoryInterface = creationFactoryInterface;
		queuedObjects = new LinkedHashSet(maximum);
		// initialize minimal objects
		appendObjects(minimalsize);
		currentCount = minimalsize;

		// create step size
		stepSize = (minimum - maximum) / STANDARDSTEPCOUNT;
		if (stepSize < MINIMUMSTEPSIZE) {
			stepSize = MINIMUMSTEPSIZE;
		}
	}

	private synchronized void appendObjects(int minimalsize) throws PoolingException,
				IllegalAccessException {
		for (int i = 0; i < minimalsize; i++) {
			freeObjects.push(poolableCreationFactoryInterface.createObject());
		}
	}

	/**
	 * we try to be at least some kind of fair by queueing
	 * waiting-for-non-empty-pool threads in a fifo. this does not avoid another
	 * thread luckily stealing a recently freed object, if the first-in-fifo
	 * object isn't fast enough to catch it itself. Life is hard ;)
	 * @return
	 */
	private synchronized Object getSyncQueueObject() {
		Object ret = new Object();

		queuedObjects.add(ret);
		return ret;
	}

	private synchronized void releaseSyncQueueObject(Object sObject) {
		queuedObjects.remove(sObject);
	}

	private synchronized void notifyOfFreedObject() {
		if (queuedObjects.isEmpty()) {
			return;
		}
		Object sync = queuedObjects.iterator().next();

		synchronized (sync) {
			sync.notify();
		}
	}

	public Poolable getInstance() throws PoolEmptyException, PoolingException,
				IllegalAccessException {
		return getInstance(0);
	}

	public Poolable getInstance(long maxWait) throws PoolEmptyException, PoolingException,
				IllegalAccessException {
		if (!this.blockOverflow) {
			return getFreeInstance();
		} else {
			Object sync = getSyncQueueObject();
			Poolable ret = null;
			long waitTime = 0;

			synchronized (sync) {
				while (ret == null) {
					try {
						ret = getFreeInstance();
					} catch (PoolEmptyException ex) {
						try {
							// if the pool is empty, wait for our place in queue
							// to be notified
							NodeLogger.getLogger(getClass()).warn("Pool [" + name + "] is empty. Waiting for free object. Consider raising poolsize.");
							final long time1 = System.currentTimeMillis();
							long nowMaxWait;

							if (maxWait != 0) {
								nowMaxWait = maxWait - waitTime;
							} else {
								nowMaxWait = 0;
							}
							if (nowMaxWait >= 0) {
								sync.wait(nowMaxWait);
							} else {
								throw ex;
							}
							waitTime += System.currentTimeMillis() - time1;
						} catch (InterruptedException e) {
							throw new PoolEmptyException("Could not wait for free Object. Interrupted! Original message: " + e.getMessage());
						}
					}
				}
			}
			releaseSyncQueueObject(sync);
			return ret;
		}
	}

	private synchronized Poolable getFreeInstance() throws PoolEmptyException,
				PoolingException, IllegalAccessException {
		Poolable ret = null;

		if (currentCount > 0) {
			ret = (Poolable) freeObjects.pop();
			currentCount--;
			// add to pooledobjects
			pooledObjects.add(ret);
		} else {
			// check wheter or not i can create more elements
			if (this.pooledObjects.size() == maximum) {
				// the pool is on maximum - no more objects possible
				throw new PoolEmptyException();
			} else {
				// the pool is not on maximum - create X objects into the pool
				// and return
				// calculate difference to maximum
				int difference = (maximum - pooledObjects.size());

				// if there are more slots left then one step would cost
				if (difference >= stepSize) {
					// use stepsize
					appendObjects(stepSize);
					currentCount += stepSize;
				} else {
					// use difference
					appendObjects(difference);
					currentCount += difference;
				}
				// get an instance
				ret = (Poolable) freeObjects.pop();
				currentCount--;
				// add to pooledobjects
				pooledObjects.add(ret);
			}
		}
		return ret;
	}

	/**
	 * @deprecated cause of changed interfaces
	 */
	public Class getPooledClass() {
		return Object.class;
	}

	public synchronized void releaseInstance(Poolable pooledObject) throws NotPoolObjectException {
		// check if this is a object that we have had in this pool
		if (pooledObjects.contains(pooledObject)) {
			// reset
			pooledObject.reset();
			pooledObjects.remove(pooledObject);
			currentCount++;
			this.freeObjects.push(pooledObject);
			try {
				poolableCreationFactoryInterface.reinitObject(pooledObject);
			} catch (PoolingException e) {
				NodeLogger.getNodeLogger(getClass()).error("Error while releasing instance", e);
			}
			notifyOfFreedObject();
		} else {
			throw new NotPoolObjectException();
		}
	}

	public boolean belongsToPool(Poolable object) {
		boolean ret = false;

		// only pooled objects can be available outside
		if (pooledObjects.contains(object)) {
			ret = true;
		}
		return ret;
	}

	public synchronized void removeAll() {
		Iterator it = pooledObjects.iterator();

		while (it.hasNext()) {
			Poolable poolable = (Poolable) it.next();

			try {
				this.poolableCreationFactoryInterface.destroyObject(poolable);
			} catch (PoolingException e) {
				NodeLogger.getNodeLogger(getClass()).error("Error while removing all instances", e);
			}
		}
		pooledObjects.clear();
		freeObjects.clear();
		queuedObjects.clear();

		currentCount = 0;
	}
}
