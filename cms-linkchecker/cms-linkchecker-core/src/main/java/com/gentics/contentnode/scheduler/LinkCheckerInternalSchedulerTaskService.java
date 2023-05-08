package com.gentics.contentnode.scheduler;

import java.util.Arrays;
import java.util.List;

/**
 * Service for the internal link checker scheduler tasks
 */
public class LinkCheckerInternalSchedulerTaskService implements InternalSchedulerTaskService {
	@Override
	public List<InternalSchedulerTask> tasks() {
		return Arrays.asList(LinkCheckerInternalSchedulerTask.values());
	}
}
