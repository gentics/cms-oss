package com.gentics.contentnode.tests.init;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.init.MigrateScheduler;
import com.gentics.contentnode.init.MigrateScheduler.OldTask;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the {@link MigrateScheduler} job
 */
public class MigrateSchedulerTest {
	/**
	 * ID of the purgelogs task (example of an internal task)
	 */
	public final static int PURGELOGS_TASK_ID = 5;

	/**
	 * ID of the crsync task (example of an external task)
	 */
	public final static int CRSYNC_TASK_ID = 13;

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
	}

	/**
	 * Reset migration data
	 * @throws NodeException
	 */
	@Before
	public void reset() throws NodeException {
		operate(() -> {
			// set all old tasks to unmigrated
			update("UPDATE jobrun SET migrated = false");
			update("UPDATE job SET migrated = false");
			update("UPDATE task SET migrated = false");

			// delete all external scheduler tasks
			update("DELETE FROM scheduler_task WHERE internal != 1");
			// delete all schedules
			update("DELETE FROM scheduler_schedule");
		});
	}

	/**
	 * Test migration of all scheduler tasks
	 * @throws NodeException
	 */
	@Test
	public void testMigration() throws NodeException {
		MigrateScheduler job = new MigrateScheduler();
		operate(job::execute);

		assertAllMigrated();
	}

	/**
	 * Test that migration does not fail on jobs that reference non-existent tasks
	 * @throws NodeException
	 */
	@Test
	public void testMigrateJobWithInvalidTask() throws NodeException {
		setAllMigrated();
		operate(() -> {
			update("INSERT INTO job (name, schedule_type, schedule_data, task_id) VALUES (?, ?, ?, ?)", "dummy", "manual", "a:1:{s:1:\"s\";s:4:\"b:0;\";}", 4711);
		});

		MigrateScheduler job = new MigrateScheduler();
		operate(job::execute);

		assertAllMigrated();
	}

	/**
	 * Test that migration does not fail on jobruns that reference non-existent jobs
	 * @throws NodeException
	 */
	@Test
	public void testMigrateJobrunWithInvalidJob() throws NodeException {
		setAllMigrated();
		operate(() -> {
			update("INSERT INTO jobrun (job_id) VALUES (?)", 4711);
		});

		MigrateScheduler job = new MigrateScheduler();
		operate(job::execute);

		assertAllMigrated();
	}

	/**
	 * Test continuing the migration that stopped after migration of an internal task
	 * @throws NodeException
	 */
	@Test
	public void testContinueMigration1() throws NodeException {
		testContinueMigration(PURGELOGS_TASK_ID);
	}

	/**
	 * Test continuing the migration that stopped after migration of an external task
	 * @throws NodeException
	 */
	@Test
	public void testContinueMigration2() throws NodeException {
		testContinueMigration(CRSYNC_TASK_ID);
	}

	/**
	 * Test continuing the migration that stopped after migration of the given task
	 * @param oldTaskId old task ID
	 * @throws NodeException
	 */
	protected void testContinueMigration(int oldTaskId) throws NodeException {
		OldTask oldTask = loadTask(oldTaskId);
		String parsedCommand = execute(t -> t.getParsedCommand(), oldTask);

		// migrate only the task (without everything else)
		TestableMigrateScheduler job = new TestableMigrateScheduler();
		consume(t -> job.migrate(t), oldTask);

		// assert that the task was migrated, but not its schedules
		SchedulerTask migratedTask = supply(t -> t.getObject(SchedulerTask.class,
				DBUtils.select("SELECT id FROM scheduler_task WHERE command = ?", pst -> {
					pst.setString(1, getMigratedCommand(parsedCommand));
				}, DBUtils.firstInt("id"))));
		assertThat(migratedTask).as("Migrated Task").isNotNull();
		assertThat(getSchedules(migratedTask)).as("Schedules for Migrated Task").isEmpty();

		// execute the migration job
		operate(job::execute);
		// assert that the schedules of the task are now also migrated
		assertThat(getSchedules(migratedTask)).as("Schedules for Migrated Task").isNotEmpty();

		assertAllMigrated();
	}

	/**
	 * Get the command of the migrated task from the given parsed command of the old task
	 * @param parsedCommand parsed command
	 * @return command of the migrated task
	 */
	protected String getMigratedCommand(String parsedCommand) {
		Matcher matcher = MigrateScheduler.INTERNAL_TASK_COMMAND_PATTERN.matcher(parsedCommand);
		if (matcher.matches()) {
			return matcher.group("cmd");
		} else {
			return parsedCommand;
		}
	}

	/**
	 * Set all old tasks to be migrated
	 * @throws NodeException
	 */
	protected void setAllMigrated() throws NodeException {
		operate(() -> {
			update("UPDATE jobrun SET migrated = true");
			update("UPDATE job SET migrated = true");
			update("UPDATE task SET migrated = true");
		});
	}

	/**
	 * Load the old task with given ID
	 * @param oldTaskId old task ID
	 * @return old task
	 * @throws NodeException
	 */
	protected MigrateScheduler.OldTask loadTask(int oldTaskId) throws NodeException {
		OldTask oldTask = execute(id -> DBUtils.select(
				"SELECT task.*, tasktemplate.command FROM task LEFT JOIN tasktemplate ON task.tasktemplate_id = tasktemplate.id WHERE task.id = ?",
				pst -> {
					pst.setInt(1, id);
				}, rs -> {
					if (rs.next()) {
						return new MigrateScheduler.OldTask(rs);
					} else {
						return null;
					}
				}), oldTaskId);

		assertThat(oldTask).as(String.format("Old task with ID %d", oldTaskId)).isNotNull();

		consume(t -> t.attachParameters(), oldTask);

		return oldTask;
	}

	/**
	 * Get the schedules of the given task
	 * @param task task
	 * @return list of schedules
	 * @throws NodeException
	 */
	protected List<SchedulerSchedule> getSchedules(SchedulerTask task) throws NodeException {
		return supply(t -> t.getObjects(SchedulerSchedule.class,
				DBUtils.select("SELECT id FROM scheduler_schedule WHERE scheduler_task_id = ?", pst -> {
					pst.setInt(1, task.getId());
				}, DBUtils.IDS)));
	}

	/**
	 * Assert that everything has been migrated
	 * @throws NodeException
	 */
	protected void assertAllMigrated() throws NodeException {
		assertAllTasksMigrated();
		assertAllJobsMigrated();
		assertAllJobrunsMigrated();
	}

	/**
	 * Assert that all tasks have been migrated
	 * @throws NodeException
	 */
	protected void assertAllTasksMigrated() throws NodeException {
		Set<Integer> unmigratedTasks = supply(() -> DBUtils.select("SELECT id FROM task WHERE migrated = false", DBUtils.IDS));
		assertThat(unmigratedTasks).as("IDs of unmigrated tasks").isEmpty();
	}

	/**
	 * Assert that all jobs have been migrated
	 * @throws NodeException
	 */
	protected void assertAllJobsMigrated() throws NodeException {
		Set<Integer> unmigratedTasks = supply(() -> DBUtils.select("SELECT DISTINCT task_id id FROM job WHERE migrated = false", DBUtils.IDS));
		assertThat(unmigratedTasks).as("Task IDs of unmigrated jobs").isEmpty();
	}

	/**
	 * Assert that all jobruns have been migrated
	 * @throws NodeException
	 */
	protected void assertAllJobrunsMigrated() throws NodeException {
		Set<Integer> unmigratedTasks = supply(() -> DBUtils.select("SELECT DISTINCT job_id id FROM jobrun WHERE migrated = false", DBUtils.IDS));
		assertThat(unmigratedTasks).as("Job IDs of unmigrated jobruns").isEmpty();
	}
}
