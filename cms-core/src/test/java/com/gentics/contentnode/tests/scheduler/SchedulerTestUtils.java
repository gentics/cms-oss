package com.gentics.contentnode.tests.scheduler;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import java.time.OffsetDateTime;
import java.util.List;


public class SchedulerTestUtils {


	/**
	 * Start an execution of the given schedule with the specified timestamp.
	 *
	 * @param schedule  The schedule to start an execution for.
	 * @param startTime The start timestamp for the execution.
	 * @return The execution ID.
	 */
	public static int startExecution(
			SchedulerSchedule schedule, OffsetDateTime startTime) throws NodeException {
		return Trx.supply(() -> {
			long startTimestamp = startTime.toEpochSecond();
			List<Integer> executionIds = DBUtils.executeInsert(
					"INSERT INTO scheduler_execution (scheduler_schedule_id, starttime) VALUES (?, ?)",
					new Object[]{schedule.getId(), startTimestamp});
			int id = executionIds.get(0);

			DBUtils.update(
					"UPDATE scheduler_schedule SET scheduler_execution_id = ? WHERE id = ?",
					new Object[]{id, schedule.getId()});

			return id;
		});
	}

	/**
	 * Finish the execution with the given ID.
	 *
	 * @param executionId The ID of the execution to be finished.
	 * @param endTime     The end time to set.
	 * @param duration    The duration to set.
	 * @param status      The result status of the execution.
	 */
	public static void finishExecution(int executionId, OffsetDateTime endTime, int duration,
			boolean status) throws NodeException {
		Trx.operate(() -> DBUtils.update(
				"UPDATE scheduler_execution SET endtime = ?, duration = ?, result = ? WHERE id = ?",
				new Object[]{endTime.toEpochSecond(), duration, status ? 0 : 1, executionId}));
	}

}
