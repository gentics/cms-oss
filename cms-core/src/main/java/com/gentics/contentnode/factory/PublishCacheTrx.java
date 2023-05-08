package com.gentics.contentnode.factory;

import com.gentics.api.lib.exception.NodeException;

/**
 * AutoClosable to temporarily enable/disable the publish cache
 */
public class PublishCacheTrx implements AutoCloseable {
	/**
	 * Original value
	 */
	protected boolean publishCacheEnabled;

	/**
	 * Enable/disable the publish cache (depending on the given flag).
	 * Enabling only works, if the Feature is also enabled
	 * @param enable true to enable the publish cache, false to disable
	 * @throws NodeException
	 */
	public PublishCacheTrx(boolean enable) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		publishCacheEnabled = t.isPublishCacheEnabled();
		t.setPublishCacheEnabled(enable);
	}

	@Override
	public void close() throws NodeException {
		TransactionManager.getCurrentTransaction().setPublishCacheEnabled(publishCacheEnabled);
	}
}
