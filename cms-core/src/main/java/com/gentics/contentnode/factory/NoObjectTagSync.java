package com.gentics.contentnode.factory;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.object.TagFactory;

/**
 * AutoCloseable that temporarily disables synchronization of object tags
 */
public class NoObjectTagSync implements AutoCloseable {
	protected boolean oldSyncSetting;

	public NoObjectTagSync() throws TransactionException {
		Transaction t = TransactionManager.getCurrentTransaction();
		oldSyncSetting = ObjectTransformer.getBoolean(t.getAttributes().get(TagFactory.SYNC_RUNNING_ATTRIBUTENAME), false);
		if (!oldSyncSetting) {
			t.getAttributes().put(TagFactory.SYNC_RUNNING_ATTRIBUTENAME, true);
		}
	}

	@Override
	public void close() throws TransactionException {
		if (!oldSyncSetting) {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.getAttributes().remove(TagFactory.SYNC_RUNNING_ATTRIBUTENAME);
		}
	}
}
