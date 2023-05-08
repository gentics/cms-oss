package com.gentics.contentnode.distributed;

/**
 * Simple sync guard to avoid endless distribution of calls (publishing of messages)
 */
public class DistributionSync {
	/**
	 * Threadlocal flag
	 */
	private ThreadLocal<Boolean> flag = ThreadLocal.withInitial(() -> true);

	/**
	 * Get autoclosable {@link DistributionSync#Trx} instance that will set the internal threadlocal flag to false in its
	 * constructor, and will set it back to true when closed
	 * @return sync instance
	 */
	public Trx get() {
		return new Trx();
	}

	/**
	 * Check whether the threadlocal flag is currently true
	 * @return state of the threadlocal flag
	 */
	public boolean allow() {
		return flag.get();
	}

	/**
	 * Autocloseable class that clears the flag upon construction and sets it again when closed
	 */
	public class Trx implements AutoCloseable {
		protected Trx() {
			flag.set(false);
		}

		@Override
		public void close() {
			flag.set(true);
		}
	}
}
