package com.gentics.contentnode.tests.scheduler;

import static com.gentics.contentnode.tests.scheduler.SchedulerTestUtils.finishExecution;
import static com.gentics.contentnode.tests.scheduler.SchedulerTestUtils.startExecution;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.scheduler.IntervalUnit;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleFollow;
import com.gentics.contentnode.rest.model.scheduler.ScheduleInterval;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for determination of due schedules.
 */
public class SchedulingTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static SchedulerFactory factory;
	private static SchedulerTask dummyTask;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		factory = (SchedulerFactory) testContext.getContext().getContentNodeFactory().getFactory().getObjectFactory(SchedulerSchedule.class);
		// The tests only get the schedules via getDueSchedules(true). The
		// scheduler should not actually execute the schedules, so we suspend it.
		factory.suspend(Collections.emptySet());
		testContext.getContext().getTransaction().commit();
		dummyTask = Builder.create(
			SchedulerTask.class,
			task -> {
				task.setName("Dummy task");
				task.setCommand("dummy");
			})
			.build();
	}

	@Before
	public void setup() throws NodeException {
		// Delete all schedules and executions.
		Trx.operate(t -> {
			DBUtils.executeUpdate("DELETE FROM scheduler_execution;", new Object[0]);

			List<SchedulerSchedule> schedules = t.getObjects(SchedulerSchedule.class, DBUtils.select("SELECT id FROM scheduler_schedule", DBUtils.IDS));

			for (SchedulerSchedule schedule : schedules) {
				schedule.delete();
			}
		});
	}

	/**
	 * Check that schedules with start times in the future or end times in the
	 * past are not executed.
	 */
	@Test
	public void testIntervalOutside() throws NodeException {
		SchedulerSchedule tooSoon = createSchedule(
			"Interval schedule - too soon",
			prepareIntervalData(IntervalUnit.minute)
				.setStartTimestamp(Integer.MAX_VALUE));
		SchedulerSchedule tooLate = createSchedule(
			"Interval schedule - too late",
			prepareIntervalData(IntervalUnit.minute).setEndTimestamp(1));

		List<SchedulerSchedule> schedules = getDueSchedules(OffsetDateTime.now());

		assertThat(schedules)
			.as("Due schedules")
			.doesNotContain(tooSoon, tooLate);
	}

	/**
	 * Check that interval schedules are not executed, when the last execution
	 * was started less than the specified interval type ago.
	 *
	 * <p>
	 *     Creates a schedule for every interval type, and then successively
	 *     adds executions in the last month, week, day, hour and minute. The
	 *     due schedules must decrease accordingly.
	 * </p>
	 */
	@Test
	public void testIntervalTypes() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule everyMinute = createSchedule("Interval minute", prepareIntervalData(IntervalUnit.minute));
		SchedulerSchedule everyHour = createSchedule("Interval hour", prepareIntervalData(IntervalUnit.hour));
		SchedulerSchedule everyDay = createSchedule("Interval day", prepareIntervalData(IntervalUnit.day));
		SchedulerSchedule everyWeek = createSchedule("Interval week", prepareIntervalData(IntervalUnit.week));
		SchedulerSchedule everyMonth = createSchedule("Interval month", prepareIntervalData(IntervalUnit.month));
		List<SchedulerSchedule> allSchedules = getDueSchedules(now);

		assertThat(allSchedules)
			.as("Due schedules (no executions)")
			.containsExactlyInAnyOrder(everyMinute, everyHour, everyDay, everyWeek, everyMonth);

		addExecutions(allSchedules, now.minusMonths(1), true);

		List<SchedulerSchedule> dueSchedules = getDueSchedules(now);

		assertThat(dueSchedules)
			.as("Due schedules (last execution: one month)")
			.containsExactlyInAnyOrder(everyMinute, everyHour, everyDay, everyWeek);

		addExecutions(allSchedules, now.minusWeeks(1), true);

		dueSchedules = getDueSchedules(now);

		assertThat(dueSchedules)
			.as("Due schedules (last execution: one week)")
			.containsExactlyInAnyOrder(everyMinute, everyHour, everyDay);

		addExecutions(allSchedules, now.minusDays(1), true);

		dueSchedules = getDueSchedules(now);

		assertThat(dueSchedules)
			.as("Due schedules (last execution: one day)")
			.containsExactlyInAnyOrder(everyMinute, everyHour);

		addExecutions(allSchedules, now.minusHours(1), true);

		dueSchedules = getDueSchedules(now);

		assertThat(dueSchedules)
			.as("Due schedules (last execution: one hour)")
			.containsExactlyInAnyOrder(everyMinute);

		addExecutions(allSchedules, now.minusMinutes(1), true);

		dueSchedules = getDueSchedules(now);

		assertThat(dueSchedules)
			.as("Due schedules (last execution: one minute)")
			.isEmpty();
	}

	/**
	 * Check that interval schedules are not executed when the last execution is
	 * less than the configured time amount ago.
	 */
	@Test
	public void testIntervalValues() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule everyMinute = createSchedule("Interval minute", prepareIntervalData(IntervalUnit.minute, 2));
		List<SchedulerSchedule> allSchedules = getDueSchedules(now);

		assertThat(allSchedules)
			.as("Due schedules (no executions)")
			.containsExactly(everyMinute);

		addExecutions(allSchedules, now.minusMinutes(3), true);

		assertThat(getDueSchedules(now))
			.as("Due schedules (3 minutes)")
			.containsExactly(everyMinute);

		addExecutions(allSchedules, now.minusMinutes(2), true);

		assertThat(getDueSchedules(now))
			.as("Due schedules (2 minutes)")
			.isEmpty();

		addExecutions(allSchedules, now.minusMinutes(1), true);

		assertThat(getDueSchedules(now))
			.as("Due schedules (1 minute)")
			.isEmpty();
	}

	/**
	 * Check simple follow up rule that jobs are scheduled, when their followed
	 * job was executed.
	 *
	 * <p>
	 *     The test checks with followup schedules that require the previous
	 *     execution to be a success, and one that is always executed.
	 * </p>
	 */
	@Test
	public void testFollowup() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule initialSchedule = createSchedule("Once", new ScheduleData().setType(ScheduleType.once), now.minusMinutes(3).toEpochSecond());
		SchedulerSchedule followUpOnSuccessSchedule = createSchedule("FollowUp Once OnSuccess", prepareFollowUpData(true, initialSchedule));
		SchedulerSchedule followUpAlways = createSchedule("Follow-Up Once Always", prepareFollowUpData(false, initialSchedule));

		assertThat(getDueSchedules(now))
			.as("Due schedules: initial")
			.containsOnly(initialSchedule);

		addExecutions(initialSchedule, now.minusMinutes(2), false);

		assertThat(getDueSchedules(now))
			.as("Due schedules: after failed initial")
			.containsExactly(followUpAlways);

		// The last execution of of the followed schedule must have started
		// later than the last execution of the following schedule.
		addExecutions(initialSchedule, now.minusMinutes(1).plusSeconds(1), true);
		addExecutions(followUpAlways, now.minusMinutes(1), true);

		assertThat(getDueSchedules(now))
			.as("Due schedules: after successful initial")
			.containsExactly(followUpOnSuccessSchedule, followUpAlways);
	}

	/**
	 * Check that a followup schedule is executed, when its last runtime
	 * overlapped with a new execution of the followed schedule.
	 */
	@Test
	public void testFollowUpLongRunning() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule initialSchedule = createSchedule("Initial", new ScheduleData().setType(ScheduleType.manual));
		SchedulerSchedule followUp = createSchedule("FollowUp Once OnSuccess", prepareFollowUpData(true, initialSchedule));

		addExecutions(initialSchedule, now.minusMinutes(2), true);
		addExecutions(followUp, now.minusMinutes(2).plusSeconds(1), 90, true);
		addExecutions(initialSchedule, now.minusMinutes(1), true);

		assertThat(getDueSchedules(now))
			.as("Due schedules: followup")
			.containsExactly(followUp);
	}

	/**
	 * Check that a followup schedule is not executed, when the followed schedule is still running.
	 */
	@Test
	public void testFollowUpStillRunning() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule initialSchedule = createSchedule("Initial A", new ScheduleData().setType(ScheduleType.manual));
		SchedulerSchedule followUp = createSchedule("FollowUp", prepareFollowUpData(true, initialSchedule));

		assertThat(getDueSchedules(now))
			.as("Initial due schedules")
			.isEmpty();

		int executionId = startExecution(initialSchedule, now.minusMinutes(1));

		assertThat(getDueSchedules(now))
			.as("Initial running")
			.isEmpty();

		finishExecution(executionId, now, 60, true);

		assertThat(getDueSchedules(now))
			.as("Initial finished")
			.containsExactly(followUp);
	}

	@Test
	public void testFollowUpMultipleTriggerSchedules() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule initialScheduleA = createSchedule("Initial A", new ScheduleData().setType(ScheduleType.manual));
		SchedulerSchedule initialScheduleB = createSchedule("Initial B", new ScheduleData().setType(ScheduleType.manual));
		SchedulerSchedule followUp = createSchedule("FollowUp", prepareFollowUpData(true, initialScheduleA, initialScheduleB));

		assertThat(getDueSchedules(now))
			.as("Initial due schedules")
			.isEmpty();

		int executionIdA = startExecution(initialScheduleA, now.minusMinutes(2));
		int executionIdB = startExecution(initialScheduleB, now.minusMinutes(2));

		assertThat(getDueSchedules(now))
			.as("Started follow schedules")
			.isEmpty();

		finishExecution(executionIdB, now.minusMinutes(1), 60, true);

		assertThat(getDueSchedules(now))
			.as("Finished follow schedule")
			.containsExactly(followUp);
	}

	/**
	 * Check that manual schedules are never executed.
	 */
	@Test
	public void testManual() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule initialSchedule = createSchedule("Manual", new ScheduleData().setType(ScheduleType.manual));

		assertThat(getDueSchedules(now))
			.as("Due schedules: initial none")
			.isEmpty();

		addExecutions(initialSchedule, now.minusMinutes(1), true);

		assertThat(getDueSchedules(now))
			.as("Due schedules: none")
			.isEmpty();
	}

	/**
	 * Check that an already runnin task is not executed.
	 */
	@Test
	public void testRunning() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule schedule = createSchedule("Interval minute", prepareIntervalData(IntervalUnit.minute, 1));

		assertThat(getDueSchedules(now))
			.as("Due schedules: initial")
			.containsExactly(schedule);

		int executionId = startExecution(schedule, now.minusMinutes(1));

		assertThat(getDueSchedules(now))
			.as("Due schedules: empty")
			.isEmpty();

		finishExecution(executionId, now.minusSeconds(1), 59, true);

		assertThat(getDueSchedules(now))
			.as("Due schedules: due again")
			.containsExactly(schedule);

	}

	/**
	 * Check that no task is executed when the scheduler is paused.
	 */
	@Test
	public void testDisabledScheduler() throws NodeException {
		OffsetDateTime now = OffsetDateTime.now();
		SchedulerSchedule schedule = createSchedule("Interval minute", prepareIntervalData(IntervalUnit.minute, 1));

		assertThat(Trx.supply(() -> factory.getDueSchedules()))
			.as("Due schedules")
			.isEmpty();
	}

	/**
	 * Check that non-parallel schedules are not executed at the same time.
	 */
	@Test
	public void testNonParallelSchedules() throws NodeException, IOException, InterruptedException {
		CountDownLatch startedLatchA = new CountDownLatch(1);
		CountDownLatch finishedLatchA = new CountDownLatch(1);
		SchedulerSchedule scheduleA = mockSchedule(
			createSchedule("Schedule A", new ScheduleData().setType(ScheduleType.manual)),
			mockTask(dummyTask, 0, startedLatchA, finishedLatchA));
		CountDownLatch startedLatchB = new CountDownLatch(1);
		CountDownLatch finishedLatchB = new CountDownLatch(1);
		SchedulerSchedule scheduleB = mockSchedule(
			createSchedule("Schedule B", new ScheduleData().setType(ScheduleType.manual)),
			mockTask(dummyTask, 0, startedLatchB, finishedLatchB));

		assertThat(factory.executeNow(scheduleA))
			.as("Execute A now")
			.isTrue();

		startedLatchA.await(10, TimeUnit.SECONDS);

		assertThat(startedLatchA.getCount() == 0)
			.as("Schedule A is running")
			.isTrue();

		assertThat(factory.executeNow(scheduleB))
			.as("Execute B now")
			.isTrue();

		// Give Schedule B some time to execute (which it should not, since A is
		// still running).
		Thread.sleep(1000);

		assertThat(startedLatchB.getCount() == 0)
			.as("Schedule B is running")
			.isFalse();

		assertThat(factory.getRunningSchedules())
			.as("Running schedules A")
			.containsOnly(scheduleA.getId());

		finishedLatchA.countDown();
		startedLatchB.await(10, TimeUnit.SECONDS);

		assertThat(factory.getRunningSchedules())
			.as("Running schedules B")
			.containsOnly(scheduleB.getId());

		finishedLatchB.countDown();

		// Wait for the scheduling factory to clean up.
		Thread.sleep(1000);

		assertThat(factory.getRunningSchedules())
			.as("Running schedules none")
			.isEmpty();
	}

	/**
	 * Check that parallel schedules run at the same time.
	 */
	@Test
	public void testParallelSchedules() throws NodeException, IOException, InterruptedException {
		CountDownLatch startedLatchA = new CountDownLatch(1);
		CountDownLatch finishedLatchA = new CountDownLatch(1);
		SchedulerSchedule scheduleA = mockSchedule(
			createSchedule("Schedule A", true, new ScheduleData().setType(ScheduleType.manual)),
			mockTask(dummyTask, 0, startedLatchA, finishedLatchA));
		CountDownLatch startedLatchB = new CountDownLatch(1);
		CountDownLatch finishedLatchB = new CountDownLatch(1);
		SchedulerSchedule scheduleB = mockSchedule(
			createSchedule("Schedule B", true, new ScheduleData().setType(ScheduleType.manual)),
			mockTask(dummyTask, 0, startedLatchB, finishedLatchB));

		assertThat(factory.executeNow(scheduleA))
			.as("Execute A now")
			.isTrue();

		assertThat(factory.executeNow(scheduleB))
			.as("Execute B now")
			.isTrue();

		startedLatchA.await(10, TimeUnit.SECONDS);
		startedLatchB.await(10, TimeUnit.SECONDS);

		assertThat(startedLatchA.getCount() == 0)
			.as("Schedule A is running")
			.isTrue();

		assertThat(startedLatchB.getCount() == 0)
			.as("Schedule B is running")
			.isTrue();

		assertThat(factory.getRunningSchedules())
			.as("Running schedules")
			.containsExactlyInAnyOrder(scheduleA.getId(), scheduleB.getId());

		finishedLatchA.countDown();
		finishedLatchB.countDown();

		// Wait for the scheduling factory to clean up.
		Thread.sleep(1000);

		assertThat(factory.getRunningSchedules())
			.as("Running schedules after finish")
			.isEmpty();
	}

	/**
	 * Check that non-parallel schedules run even when a parallel schedule is
	 * already running.
	 */
	@Test
	public void testMixedParallelSchedules() throws NodeException, IOException, InterruptedException {
		CountDownLatch startedLatchA = new CountDownLatch(1);
		CountDownLatch finishedLatchA = new CountDownLatch(1);
		SchedulerSchedule scheduleA = mockSchedule(
			createSchedule("Schedule A", true, new ScheduleData().setType(ScheduleType.manual)),
			mockTask(dummyTask, 0, startedLatchA, finishedLatchA));
		CountDownLatch startedLatchB = new CountDownLatch(1);
		CountDownLatch finishedLatchB = new CountDownLatch(1);
		SchedulerSchedule scheduleB = mockSchedule(
			createSchedule("Schedule B", false, new ScheduleData().setType(ScheduleType.manual)),
			mockTask(dummyTask, 0, startedLatchB, finishedLatchB));

		assertThat(factory.executeNow(scheduleA))
			.as("Execute A now")
			.isTrue();

		startedLatchA.await(10, TimeUnit.SECONDS);

		assertThat(startedLatchA.getCount() == 0)
			.as("Schedule A is running")
			.isTrue();

		assertThat(factory.executeNow(scheduleB))
			.as("Execute B now")
			.isTrue();

		startedLatchB.await(10, TimeUnit.SECONDS);

		assertThat(startedLatchB.getCount() == 0)
			.as("Schedule B is running")
			.isTrue();

		assertThat(factory.getRunningSchedules())
			.as("Running schedules")
			.containsExactlyInAnyOrder(scheduleA.getId(), scheduleB.getId());

		finishedLatchA.countDown();
		finishedLatchB.countDown();

		// Wait for the scheduling factory to clean up.
		Thread.sleep(1000);

		assertThat(factory.getRunningSchedules())
			.as("Running schedules after finish")
			.isEmpty();
	}

	/**
	 * Check if the scheduler executor is restarted after reloading the configuration.
	 */
	@Test
	public void testExecutorRestart() throws NodeException {
		assertThat(factory.checkExecutor(false))
			.as("Scheduler executor running initially")
			.isTrue();

		NodeConfigRuntimeConfiguration.getDefault().reloadConfiguration();

		assertThat(factory.checkExecutor(false))
			.as("Scheduler executor running after reload")
			.isTrue();
	}

	/**
	 * Get the due schedules for the given time.
	 * @param now The timestamp used to determine which schedules need to be executed.
	 * @return A list of due schedules.
	 * @throws NodeException
	 */
	private List<SchedulerSchedule> getDueSchedules(OffsetDateTime now) throws NodeException {
		try (Trx ignored = new Trx().at((int) now.toEpochSecond())) {
			return factory.getDueSchedules(true);
		}
	}

	/**
	 * Convenience method for {@link #createSchedule(String, boolean, ScheduleData, long)}
	 * for a non-parallel schedule with a creation timestamp of 0.
	 *
	 * @param name The name of the schedule.
	 * @param scheduleData The schedule data.
	 * @return The created schedule.
	 * @throws NodeException
	 */
	private SchedulerSchedule createSchedule(String name, ScheduleData scheduleData) throws NodeException {
		return createSchedule(name, false, scheduleData, 0);
	}

	/**
	 * Convenience method for {@link #createSchedule(String, boolean, ScheduleData, long)}
	 * for creating a parallel schedule with a creation timestamp of 0.
	 *
	 * @param name The name of the schedule.
	 * @param parallel Whether the schedule can be executed in parallel.
	 * @param scheduleData The schedule data.
	 * @return The created schedule.
	 * @throws NodeException
	 */
	private SchedulerSchedule createSchedule(String name, boolean parallel, ScheduleData scheduleData) throws NodeException {
		return createSchedule(name, parallel, scheduleData, 0);
	}

	/**
	 * Convenience method for {@link #createSchedule(String, boolean, ScheduleData, long)}
	 * for a non-parallel schedule with with the given timestamp.
	 *
	 * @param name The name of the schedule.
	 * @param scheduleData The schedule data.
	 * @param timestamp The creation timestamp of the schedule.
	 * @return The created schedule.
	 * @throws NodeException
	 */
	private SchedulerSchedule createSchedule(String name, ScheduleData scheduleData, long timestamp) throws NodeException {
		return createSchedule(name, false, scheduleData, timestamp);
	}

	/**
	 * Create an active schedule with the given name and schedule data for the
	 * {@link #dummyTask}.
	 *
	 * @param name The schedule name.
	 * @param parallel Whether the schedule can be executed in parallel.
	 * @param scheduleData The schedule data.
	 * @param timestamp The creation timestamp for the schedule.
	 * @return An active schedule with the specified name and schedule data.
	 */
	private SchedulerSchedule createSchedule(String name, boolean parallel, ScheduleData scheduleData, long timestamp) throws NodeException {
		return Builder.create(
				SchedulerSchedule.class,
				schedule -> {
					schedule.setName(name);
					schedule.setActive(true);
					schedule.setParallel(parallel);
					schedule.setSchedulerTask(dummyTask);
					schedule.setScheduleData(scheduleData);
				})
			.at((int) timestamp)
			.build();
	}

	/**
	 * Created a mocked version of the given task, which awaits on a
	 * {@link CountDownLatch} and finishes as sonn as the countdown occurs.
	 *
	 * <p>
	 *     When {@code original} is not {@code null}, the fields
	 *     {@code command}, {@code sanitizedCommand} and {@code isInternal}
	 *     are copied from the original task.
	 * </p>
	 *
	 * @param original The original task (maybe null).
	 * @param result The desired result code of the task execution.
	 * @param startedLatch Latch to wait on for the task to start. The latches
	 * 		{@link CountDownLatch#countDown() countDown()} method will be called
	 * 		at the start of the mocked
	 * 		{@link SchedulerTask#execute(List) execute()}.
	 * @param finishedLatch Latch to signal when the task should finish. The
	 * 		mocked {@code execute()} will wait for this latch before
	 * 		termination.
	 * @return The mocked version of the task.
	 */
	private SchedulerTask mockTask(SchedulerTask original, int result, CountDownLatch startedLatch, CountDownLatch finishedLatch) throws NodeException, IOException, InterruptedException {
		SchedulerTask task = mock(SchedulerTask.class);

		if (original != null) {
			when(task.getCommand()).thenReturn(original.getCommand());
			when(task.getSanitizedCommand()).thenReturn(original.getSanitizedCommand());
			when(task.isInternal()).thenReturn(original.isInternal());
		}

		when(task.execute(Mockito.anyList())).thenAnswer(invocation -> {
			startedLatch.countDown();

			List<String> output = invocation.getArgument(0);

			output.add(String.format("Started task at {}", OffsetDateTime.now()));
			output.add("Waiting on countdown finishedLatch");

			finishedLatch.await(1, TimeUnit.MINUTES);

			output.add(String.format("Finished task at {}", OffsetDateTime.now()));

			return result;
		});

		return task;
	}

	/**
	 * Create a mocked version of the given schedule which uses the specified
	 * task.
	 *
	 * @param original The original schedule to mock.
	 * @param task The task the mocked schedule should use.
	 * @return A mocked version of {@code original} which uses the given task
	 */
	private SchedulerSchedule mockSchedule(SchedulerSchedule original, SchedulerTask task) throws NodeException {
		SchedulerSchedule schedule = mock(SchedulerSchedule.class);

		when(schedule.getId()).thenReturn(original.getId());
		when(schedule.getName()).thenReturn(original.getName());
		when(schedule.getNotificationEmail()).thenReturn(original.getNotificationEmail());
		when(schedule.getScheduleData()).thenReturn(original.getScheduleData());
		when(schedule.getSchedulerTask()).thenReturn(task);
		when(schedule.isActive()).thenReturn(original.isActive());
		when(schedule.isParallel()).thenReturn(original.isParallel());

		return schedule;
	}

	/**
	 * Create a schedule data instance for followup schedule.
	 *
	 * @param onSuccess Whether to only execute when the followed schedule executed successfully.
	 * @param followedSchedules The followed schedules.
	 * @return The created schedule data.
	 */
	private ScheduleData prepareFollowUpData(boolean onSuccess, SchedulerSchedule ...followedSchedules) {
		ScheduleFollow follow = new ScheduleFollow()
			.setScheduleId(Stream.of(followedSchedules).map(SchedulerSchedule::getId).collect(Collectors.toSet()))
			.setOnlyAfterSuccess(onSuccess);

		return new ScheduleData()
			.setType(ScheduleType.followup)
			.setFollow(follow);
	}

	/**
	 * Create a {@link ScheduleData} of type {@code interval} with the given
	 * interval unit and an interval value of one.
	 *
	 * @param unit The needed interval unit.
	 * @return A {@code ScheduleData} of type {@code interval} with the
	 * 		specified unit.
	 */
	private ScheduleData prepareIntervalData(IntervalUnit unit) {
		return prepareIntervalData(unit, 1);
	}

	/**
	 * Create a {@link ScheduleData} of type {@code interval} with the given
	 * interval unit and an interval value.
	 *
	 * @param unit The needed interval unit.
	 * @param value The interval value.
	 * @return A {@code ScheduleData} of type {@code interval} with the
	 * 		specified unit.
	 */
	private ScheduleData prepareIntervalData(IntervalUnit unit, int value) {
		return new ScheduleData()
			.setType(ScheduleType.interval)
			.setInterval(new ScheduleInterval().setUnit(unit).setValue(value));
	}

	/**
	 * Convenience overload for {@link #addExecutions(Collection, OffsetDateTime, int, boolean)} which adds an
	 * execution with a duration of 1 second.
	 *
	 * @param schedule The schedule to add an execution for.
	 * @param startedAfter The timestamp <em>after</em> which the execution was started (1 second after).
	 * @param success Whether the created execution terminated successfully.
	 * @throws NodeException
	 */
	private void addExecutions(SchedulerSchedule schedule, OffsetDateTime startedAfter, boolean success) throws NodeException {
		addExecutions(Collections.singleton(schedule), startedAfter, 1, success);
	}

	/**
	 * Convenience overload for {@link #addExecutions(Collection, OffsetDateTime, int, boolean)}.
	 *
	 * @param schedule The schedule to add an execution for.
	 * @param startedAfter The timestamp <em>after</em> which the execution was started (1 second after).
	 * @param duration The duration of the execution in seconds.
	 * @param success Whether the created execution terminated successfully.
	 * @throws NodeException
	 */
	private void addExecutions(SchedulerSchedule schedule, OffsetDateTime startedAfter, int duration, boolean success) throws NodeException {
		addExecutions(Collections.singleton(schedule), startedAfter, duration, success);
	}

	/**
	 * Convenience overload for {@link #addExecutions(Collection, OffsetDateTime, int, boolean)} which adds an
	 * execution with a duration of 1 second.
	 *
	 * @param schedules The schedules to add an execution for.
	 * @param startedAfter The timestamp <em>after</em> which the execution was started (1 second after).
	 * @param success Whether the created execution terminated successfully.
	 * @throws NodeException
	 */
	private void addExecutions(Collection<SchedulerSchedule> schedules, OffsetDateTime startedAfter, boolean success) throws NodeException {
		addExecutions(schedules, startedAfter, 1, success);
	}

	/**
	 * Add executions for the given schedules.
	 *
	 * @param schedules The schedules to add an execution for.
	 * @param startedAfter The timestamp <em>after</em> which the execution was started (1 second after).
	 * @param duration The duration of the executions.
	 * @param success Whether the created execution terminated successfully.
	 * @throws NodeException
	 */
	private void addExecutions(Collection<SchedulerSchedule> schedules, OffsetDateTime startedAfter, int duration, boolean success) throws NodeException {
		OffsetDateTime startTime = startedAfter.plusSeconds(1);

		for (SchedulerSchedule schedule : schedules) {
			int executionId = startExecution(schedule, startTime);

			finishExecution(executionId, startTime.plusSeconds(duration), duration, success);
		}
	}

}
