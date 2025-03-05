package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;

public class SchedulerScheduleAssert extends AbstractNodeObjectAssert<SchedulerScheduleAssert, SchedulerSchedule> {

	protected SchedulerScheduleAssert(SchedulerSchedule actual) {
		super(actual, SchedulerScheduleAssert.class);
	}

	public SchedulerScheduleAssert hasName(String name) {
		assertThat(actual.getName()).as(descriptionText() + " name").isEqualTo(name);
		return myself;
	}

	public SchedulerScheduleAssert has(SchedulerTask task) throws NodeException {
		assertThat(actual.getSchedulerTask()).as(descriptionText() + " task").isEqualTo(task);
		return myself;
	}
}
