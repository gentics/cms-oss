package com.gentics.contentnode.etc;

import java.util.concurrent.TimeUnit;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.cluster.ClusterSupport;

/**
 * Cluster specific implementation of {@link LockService}
 */
public class ClusterLockService implements LockService {

	@Override
	public void acquire(String key) throws InterruptedException {
		ClusterSupport.acquireLock(key);
	}

	@Override
	public void acquire(String key, long timeout, TimeUnit unit) throws NodeException, InterruptedException {
		ClusterSupport.acquireLock(key, timeout, unit);
	}

	@Override
	public void release(String key) {
		ClusterSupport.releaseLock(key);
	}
}
