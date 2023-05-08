package com.gentics.contentnode.factory;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.PageFactory;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.lib.log.NodeLogger;

/**
 * Transactional that dirts cache on DB commit
 */
public class TransactionalDirtCache extends AbstractTransactional {

	/**
	 * Class of the object
	 */
	private Class<? extends NodeObject> clazz;

	/**
	 * Default threshold for a global cache clear.
	 */
	public static final int DEFAULT_CACHE_CLEAR_THRESHOLD = 10000;

	/**
	 *  Key for the global cache clear threshold parameter
	 */
	public static final String GLOBAL_CACHE_CLEAR_THRESHOLD_PARAM = "contentnode.globalCacheClearTransactionalThreshold";

	/**
	 * Id of the object
	 */
	private Integer id;

	/**
	 * Logger
	 */
	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * The global cache clear transactional can be used to clear the whole node object cache. This implementation will be used when the amount of stored transactionals
	 * exceed the threshold of {@link TransactionalDirtCache#getThreshold(Transaction)}
	 */
	private static Transactional globalCacheClearTransactional = new AbstractTransactional() {

		private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

		public void onDBCommit(Transaction t) throws NodeException {
		}

		/**
		 * Clears the whole node object cache
		 */
		public boolean onTransactionCommit(Transaction t) {
			try {
				t.clearNodeObjectCache();
				logger.debug("The global cache clear was invoked.");
			} catch (NodeException e) {
				logger.error("Could not clear node object cache or page status cache.", e);
			}
			return false;
		}

	};

	/**
	 * Create an instance
	 * @param clazz class
	 * @param id id
	 */
	public TransactionalDirtCache(Class<? extends NodeObject> clazz, Integer id) {
		this.clazz = clazz;
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onDBCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public void onDBCommit(Transaction t) throws NodeException {}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.factory.Transactional#onTransactionCommit(com.gentics.lib.base.factory.Transaction)
	 */
	public boolean onTransactionCommit(Transaction t) {
		dirtCache(t);
		return false;
	}

	@Override
	public void onTransactionRollback(Transaction t) {
		dirtCache(t);
	}

	/**
	 * @See {@link AbstractTransactional#getThreshold(Transaction)}
	 */
	@Override
	public int getThreshold(Transaction t) {
		int threshold = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty(GLOBAL_CACHE_CLEAR_THRESHOLD_PARAM), DEFAULT_CACHE_CLEAR_THRESHOLD);
		return threshold;
	}

	/**
	 * @See {@link AbstractTransactional#getSingleton(Transaction)}
	 */
	@Override
	public Transactional getSingleton(Transaction t) {
		return globalCacheClearTransactional;
	}

	/**
	 * Dirt the object's cache
	 * @param t transaction
	 */
	protected void dirtCache(Transaction t) {
		try {
			// dirt the object cache immediately (don't forget to pass "false" to third parameter, otherwise we would have an endless loop)
			t.dirtObjectCache(clazz, id, false);
		} catch (NodeException e) {
			logger.error("Error while dirting cache", e);
		}
	}
}
