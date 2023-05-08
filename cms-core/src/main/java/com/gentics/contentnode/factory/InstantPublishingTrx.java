package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;

/**
 * AutoCloseable that enables/disables instant publishing
 */
public class InstantPublishingTrx implements AutoCloseable {
	/**
	 * Current Transaction
	 */
	private Transaction t;

	/**
	 * Flag whether instant publishing was enabled or disabled
	 */
	private boolean wasInstantPublishing;

	/**
	 * Create an instance, switch instant publishing on/off
	 * @param on true to switch on
	 * @throws NodeException
	 */
	public InstantPublishingTrx(boolean on) throws NodeException {
		t = TransactionManager.getCurrentTransaction();
		wasInstantPublishing = t.isInstantPublishingEnabled();
		t.setInstantPublishingEnabled(on);
	}

	@Override
	public void close() throws NodeException {
		t.setInstantPublishingEnabled(wasInstantPublishing);
	}
}
