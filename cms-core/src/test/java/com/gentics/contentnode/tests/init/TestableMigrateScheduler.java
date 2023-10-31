package com.gentics.contentnode.tests.init;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.init.MigrateScheduler;

/**
 * Variant of {@link MigrateScheduler} that exposes {@link #migrate(OldTask)} for testing
 */
public class TestableMigrateScheduler extends MigrateScheduler {
	@Override
	public int migrate(OldTask oldTask) throws NodeException {
		return super.migrate(oldTask);
	}
}
