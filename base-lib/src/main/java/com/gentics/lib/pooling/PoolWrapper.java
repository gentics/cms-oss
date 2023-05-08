/*
 * @author norbert
 * @date 14.11.2005
 * @version $Id: PoolWrapper.java,v 1.2 2008-01-09 09:11:19 norbert Exp $
 */
package com.gentics.lib.pooling;

import org.apache.commons.pool.impl.GenericObjectPool;

import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of the PoolInterface that wraps around the jakarta-commons
 * pooling implementation
 * @author norbert
 */
public class PoolWrapper implements PoolInterface {

	/**
	 * wrapped object pool
	 */
	protected GenericObjectPool wrappedPool;

	/**
	 * name of the pool
	 */
	protected String poolName;

	/**
	 * logger (for logging, what else)
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * constant for the default maximum of idle objects
	 */
	protected final static int DEFAULT_MAX_IDLE = 10;

	/**
	 * constant for the default maximum number of objects
	 */
	protected final static int DEFAULT_MAX_OBJECTS = 30;

	/**
	 * create an instance of the wrapper
	 * @param name pool name
	 * @param factory object creation factory
	 */
	public PoolWrapper(String name, PoolFactoryInterface factory) {
		this(name, DEFAULT_MAX_IDLE, DEFAULT_MAX_OBJECTS, factory, false);
	}

	/**
	 * create an instance of the wrapper
	 * @param name pool name
	 * @param factory object creation factory
	 * @param blockOverflow true an object requestor shall wait for a free
	 *        object and not fail, when pool is exhausted
	 */
	public PoolWrapper(String name, PoolFactoryInterface factory, boolean blockOverflow) {
		this(name, DEFAULT_MAX_IDLE, DEFAULT_MAX_OBJECTS, factory, blockOverflow);
	}

	/**
	 * create an instance of the wrapper
	 * @param name pool name
	 * @param maxIdle maximum allowed idle objects
	 * @param maxObjects maximum number of objects in the pool
	 * @param factory object creation factory
	 */
	public PoolWrapper(String name, int maxIdle, int maxObjects, PoolFactoryInterface factory) {
		this(name, maxIdle, maxObjects, factory, false);
	}

	/**
	 * create an instance of the wrapper
	 * @param name pool name
	 * @param maxIdle maximum allowed idle objects
	 * @param maxObjects maximum number of objects in the pool
	 * @param factory object creation factory
	 * @param blockOverflow true an object requestor shall wait for a free
	 *        object and not fail, when pool is exhausted
	 */
	public PoolWrapper(String name, int maxIdle, int maxObjects, PoolFactoryInterface factory,
			boolean blockOverflow) {
		super();
		poolName = name;
		wrappedPool = new GenericObjectPool(new PoolFactoryWrapper(factory));
		wrappedPool.setMaxIdle(maxIdle);
		wrappedPool.setMaxActive(maxObjects);
		wrappedPool.setWhenExhaustedAction(blockOverflow ? GenericObjectPool.WHEN_EXHAUSTED_BLOCK : GenericObjectPool.WHEN_EXHAUSTED_FAIL);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.pooling.PoolInterface#getInstance()
	 */
	public Poolable getInstance() throws PoolEmptyException, PoolingException,
				IllegalAccessException {
		try {
			Object object = wrappedPool.borrowObject();

			if (!(object instanceof Poolable)) {
				throw new PoolingException("pooled object must implement " + Poolable.class.getName());
			}
			if (logger.isInfoEnabled()) {
				infoOutput("object borrowed");
			}
			return (Poolable) object;
		} catch (PoolingException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new PoolingException("Error while fetching object from pool", ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.pooling.PoolInterface#releaseInstance(com.gentics.lib.pooling.Poolable)
	 */
	public void releaseInstance(Poolable PooledObject) throws NotPoolObjectException {
		try {
			wrappedPool.returnObject(PooledObject);
			if (logger.isInfoEnabled()) {
				infoOutput("object returned");
			}
		} catch (Exception ex) {
			throw new NotPoolObjectException();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.pooling.PoolInterface#belongsToPool(com.gentics.lib.pooling.Poolable)
	 */
	public boolean belongsToPool(Poolable Object) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.pooling.PoolInterface#removeAll()
	 */
	public void removeAll() {
		wrappedPool.clear();
	}

	/**
	 * Get the wrapped generic object pool
	 * @return Returns the wrappedPool.
	 */
	public GenericObjectPool getWrappedPool() {
		return wrappedPool;
	}

	/**
	 * Generate some informational output to the logger (this method should only
	 * be called when the logger has at least debug level of
	 * {@link org.apache.log4j.Level#INFO})
	 * @param message message to display
	 */
	protected void infoOutput(String message) {
		logger.info(
				poolName + " - " + message + " - idle/maxidle active/maxactive: " + wrappedPool.getNumIdle() + "/" + wrappedPool.getMaxIdle() + " "
				+ wrappedPool.getNumActive() + "/" + wrappedPool.getMaxActive());
	}
}
