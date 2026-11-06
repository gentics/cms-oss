package com.gentics.contentnode.etc;

import com.gentics.contentnode.object.search.Indexer;

/**
 * Implementation of {@link LifecycleService} that starts and stops the {@link Indexer}
 */
public class IndexerLifecycleService implements LifecycleService {
	@Override
	public void start() {
		Indexer.init();
	}

	@Override
	public void stop() {
		Indexer.shutdown();
	}
}
