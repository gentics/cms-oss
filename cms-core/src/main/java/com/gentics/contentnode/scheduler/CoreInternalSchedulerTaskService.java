package com.gentics.contentnode.scheduler;

import java.util.Arrays;
import java.util.List;

/**
 * Service of the core internal tasks
 */
public class CoreInternalSchedulerTaskService implements InternalSchedulerTaskService {
	@Override
	public List<InternalSchedulerTask> tasks() {
		return Arrays.asList(CoreInternalSchedulerTask.values());
	}
}
