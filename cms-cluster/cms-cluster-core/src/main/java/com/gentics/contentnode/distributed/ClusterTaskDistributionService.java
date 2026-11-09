package com.gentics.contentnode.distributed;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.contentnode.cluster.ClusterSupport;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Cluster specific implementation of {@link TaskDistributionService}
 */
public class ClusterTaskDistributionService implements TaskDistributionService {
	@Override
	public Boolean isTaskExecutionAllowed() {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.CLUSTER)) {
			return null;
		}
		return ClusterSupport.getSingleton().isMaster();
	}

	@Override
	public <T> Pair<Boolean, T> call(Callable<T> task, boolean single) throws InterruptedException, Exception {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.CLUSTER)) {
			return Pair.of(false, null);
		}

		if (single) {
			return Pair.of(true, ClusterSupport.getSingleton().callOnMaster(task));
		} else {
			return Pair.of(true, ClusterSupport.getSingleton().callOnAllMembers(task));
		}
	}

	@Override
	public void executeOther(Runnable task) {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.CLUSTER)) {
			ClusterSupport.getSingleton().executeOnOtherMembers(task);
		}
	}
}
