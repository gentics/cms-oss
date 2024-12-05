package com.gentics.contentnode.cluster;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class ClusterTask implements Callable<String>, Serializable {
	private static final long serialVersionUID = -5543283456937178204L;

	@Override
	public String call() throws Exception {
		return Thread.currentThread().getName();
	}
}
