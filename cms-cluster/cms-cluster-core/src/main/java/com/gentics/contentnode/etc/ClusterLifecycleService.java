package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.cluster.ClusterSupport;

/**
 * Implementation of {@link LifecycleService} which starts/stops clustering
 */
public class ClusterLifecycleService implements LifecycleService {
	@Override
	public void start() {
		try {
			ClusterSupport.getSingleton().init();
		} catch (NodeException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void stop() {
		ClusterSupport.getSingleton().stop();
	}
}
