package com.gentics.contentnode.factory;

import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import de.jkeylockmanager.manager.ReturnValueLockCallback;

/**
 * Lock manager implementation, that will lock access to the given callback by the given key.
 * Calls to the callback will be done in a new temporary transaction
 *
 * @param <R> class of the return value
 */
public class TransactionLockManager<R> {
	/**
	 * Internally used lock manager
	 */
	private final KeyLockManager lockManager = KeyLockManagers.newLock();

	/**
	 * Execute the given callback (in a separate temporary transaction)
	 * @param key lock key
	 * @param callback callback
	 * @return return value
	 * @throws Exception
	 */
	public R execute(Object key, ReturnValueLockCallback<R> callback) throws Exception {
		CallbackWrapper wrapper = new CallbackWrapper(callback);
		R result = lockManager.executeLocked(key, wrapper);
		if (wrapper.e != null) {
			throw wrapper.e;
		}

		return result;
	}

	/**
	 * Callback wrapper, that will wrap the call to the callback in a new transaction
	 */
	protected class CallbackWrapper implements ReturnValueLockCallback<R> {
		/**
		 * Exception, that was thrown while calling the callback or while handling the transaction
		 */
		protected Exception e;

		/**
		 * Wrapped callback
		 */
		protected ReturnValueLockCallback<R> wrappedCallback;

		public CallbackWrapper(ReturnValueLockCallback<R> wrappedCallback) {
			this.wrappedCallback = wrappedCallback;
		}

		/* (non-Javadoc)
		 * @see de.jkeylockmanager.manager.ReturnValueLockCallback#doInLock()
		 */
		public R doInLock() {
			try {
				return TransactionManager.execute(() -> wrappedCallback.doInLock());
			} catch (Exception e) {
				this.e = e;
				return null;
			}
		}
	}
}
