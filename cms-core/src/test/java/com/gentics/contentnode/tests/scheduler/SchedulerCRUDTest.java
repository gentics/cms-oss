package com.gentics.contentnode.tests.scheduler;

import static com.gentics.contentnode.db.DBUtils.executeUpdate;
import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.rest.util.MiscUtils.load;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.scheduler.IntervalUnit;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleInterval;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for CRUD operations on scheduler tasks, scheduler schedules and scheduler executions
 */
public class SchedulerCRUDTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static SystemUser testUser1;
	private static SystemUser testUser2;

	@BeforeClass
	public static void setupOnce() throws TransactionException, NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup nodeGroup = supply(() -> load(UserGroup.class, Integer.toString(NODE_GROUP_ID)));
		testUser1 = supply(() -> createSystemUser("tester1", "tester1", "", "tester1", "tester1", Arrays.asList(nodeGroup)));
		testUser2 = supply(() -> createSystemUser("tester2", "tester2", "", "tester2", "tester2", Arrays.asList(nodeGroup)));
	}

	@Before
	public void setup() throws NodeException {
		// clear data
		operate(() -> executeUpdate("DELETE FROM scheduler_task", null));
		NodeFactory.getInstance().clear();
	}

	@Test
	public void testCreateTask() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
			create.setInternal(true);
		}).build();

		assertThat(task).as("Created task").hasFieldOrPropertyWithValue("name", "My super scheduler task")
				.hasFieldOrPropertyWithValue("command", "publish").hasFieldOrPropertyWithValue("internal", true).hasFieldOrProperty("id");

		SchedulerTask reloaded = execute(SchedulerTask::reload, task);

		assertThat(reloaded).as("Created task (reloaded)").hasFieldOrPropertyWithValue("name", "My super scheduler task")
				.hasFieldOrPropertyWithValue("command", "publish").hasFieldOrPropertyWithValue("id", task.getId());
	}

	@Test
	public void testTaskCreatorAndEditor() throws NodeException {
		ContentNodeDate creationDate = new ContentNodeDate(new Date());
		ContentNodeDate editDate = new ContentNodeDate(creationDate.getIntTimestamp() + 86400);

		SchedulerTask task = supply(testUser1, t -> {
			t.setTimestamp(creationDate.getIntTimestamp() * 1000L);
			return Builder.create(SchedulerTask.class, create -> {
				create.setName("Scheduler Task with User");
				create.setCommand("command");
			}).build();
		});

		operate(() -> {
			assertThat(task)
				.as("Created task")
				.hasFieldOrPropertyWithValue("cDate", creationDate)
				.hasFieldOrPropertyWithValue("creator", testUser1)
				.hasFieldOrPropertyWithValue("eDate", creationDate)
				.hasFieldOrPropertyWithValue("editor", testUser1);
		});

		SchedulerTask updatedTask = supply(testUser2, t -> {
			t.setTimestamp(editDate.getIntTimestamp() * 1000L);
			return Builder.update(task, update -> {
				update.setName("Updated Scheduler Task with User");
			}).build();
		});

		operate(() -> {
			assertThat(updatedTask)
				.as("Updated task")
				.hasFieldOrPropertyWithValue("cDate", creationDate)
				.hasFieldOrPropertyWithValue("creator", testUser1)
				.hasFieldOrPropertyWithValue("eDate", editDate)
				.hasFieldOrPropertyWithValue("editor", testUser2);
		});
	}

	@Test
	public void testCreateTaskDuplicateName() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("Scheduler Task");
			create.setCommand("publish");
		}).build();

		SchedulerTask task2 = Builder.create(SchedulerTask.class, create -> {
			create.setName("Scheduler Task");
			create.setCommand("publish");
		}).build();

		assertThat(task).as("First task").hasFieldOrPropertyWithValue("name", "Scheduler Task");
		assertThat(task2).as("Second task").hasFieldOrPropertyWithValue("name", "Scheduler Task 1");
	}

	@Test
	public void testUpdateTask() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
		}).build();

		SchedulerTask updated = Builder.update(task, t -> {
			t.setName("Even better scheduler task");
			t.setCommand("publish faster");
		}).build();

		assertThat(updated).as("Updated task").hasFieldOrPropertyWithValue("name", "Even better scheduler task")
				.hasFieldOrPropertyWithValue("command", "publish faster")
				.hasFieldOrPropertyWithValue("id", task.getId());

		SchedulerTask reloaded = execute(SchedulerTask::reload, task);
		assertThat(reloaded).as("Updated task (reloaded)")
				.hasFieldOrPropertyWithValue("name", "Even better scheduler task")
				.hasFieldOrPropertyWithValue("command", "publish faster")
				.hasFieldOrPropertyWithValue("id", task.getId());
	}

	@Test
	public void testDeleteTask() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
		}).build();

		consume(SchedulerTask::delete, task);
		SchedulerTask reloaded = execute(SchedulerTask::reload, task);
		assertThat(reloaded).as("Deleted task").isNull();
	}

	@Test
	public void testCreateSchedule() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
		}).build();

		ScheduleData data = new ScheduleData().setType(ScheduleType.interval).setStartTimestamp(4711)
				.setInterval(new ScheduleInterval().setValue(5).setUnit(IntervalUnit.minute));

		List<String> notificationEmail = Arrays.asList("norbert@gentics.com", "noone@salzamt");

		SchedulerSchedule schedule = Builder.create(SchedulerSchedule.class, create -> {
			create.setSchedulerTask(task);
			create.setName("Publish Schedule");
			create.setDescription("Description of the Publish Schedule");
			create.setActive(true);
			create.setScheduleData(data);
			create.setNotificationEmail(notificationEmail);
		}).build();

		operate(() -> {
			assertThat(schedule)
				.as("Created schedule")
				.hasFieldOrPropertyWithValue("name", "Publish Schedule")
				.hasFieldOrPropertyWithValue("description", "Description of the Publish Schedule")
				.hasFieldOrPropertyWithValue("active", true)
				.hasFieldOrPropertyWithValue("schedulerTask", task)
				.hasFieldOrPropertyWithValue("scheduleData", data)
				.hasFieldOrPropertyWithValue("notificationEmail", notificationEmail);
		});

		SchedulerSchedule reloaded = execute(SchedulerSchedule::reload, schedule);

		operate(() -> {
			assertThat(reloaded)
				.as("Reloaded schedule")
				.hasFieldOrPropertyWithValue("name", "Publish Schedule")
				.hasFieldOrPropertyWithValue("description", "Description of the Publish Schedule")
				.hasFieldOrPropertyWithValue("active", true)
				.hasFieldOrPropertyWithValue("schedulerTask", task)
				.hasFieldOrPropertyWithValue("scheduleData", data)
				.hasFieldOrPropertyWithValue("notificationEmail", notificationEmail);
		});
	}

	@Test
	public void testScheduleCreatorAndEditor() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("Scheduler Task");
			create.setCommand("command");
		}).build();

		ContentNodeDate creationDate = new ContentNodeDate(new Date());
		ContentNodeDate editDate = new ContentNodeDate(creationDate.getIntTimestamp() + 86400);

		SchedulerSchedule schedule = supply(testUser1, t -> {
			t.setTimestamp(creationDate.getIntTimestamp() * 1000L);
			return Builder.create(SchedulerSchedule.class, create -> {
				create.setName("Schedule with User");
				create.setSchedulerTask(task);
			}).build();
		});

		operate(() -> {
			assertThat(schedule)
				.as("Created schedule")
				.hasFieldOrPropertyWithValue("cDate", creationDate)
				.hasFieldOrPropertyWithValue("creator", testUser1)
				.hasFieldOrPropertyWithValue("eDate", creationDate)
				.hasFieldOrPropertyWithValue("editor", testUser1);
		});

		SchedulerSchedule updatedSchedule = supply(testUser2, t -> {
			t.setTimestamp(editDate.getIntTimestamp() * 1000L);
			return Builder.update(schedule, update -> {
				update.setName("Updated Schedule with User");
			}).build();
		});

		operate(() -> {
			assertThat(updatedSchedule)
				.as("Updated schedule")
				.hasFieldOrPropertyWithValue("cDate", creationDate)
				.hasFieldOrPropertyWithValue("creator", testUser1)
				.hasFieldOrPropertyWithValue("eDate", editDate)
				.hasFieldOrPropertyWithValue("editor", testUser2);
		});
	}

	@Test
	public void testUpdateSchedule() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
		}).build();

		SchedulerSchedule schedule = Builder.create(SchedulerSchedule.class, create -> {
			create.setName("My super duper schedule");
			create.setSchedulerTask(task);
		}).build();

		SchedulerSchedule updated = Builder.update(schedule, t -> {
			t.setName("Even better schedule");
		}).build();

		assertThat(updated).as("Updated schedule").hasFieldOrPropertyWithValue("name", "Even better schedule")
				.hasFieldOrPropertyWithValue("id", schedule.getId());

		SchedulerSchedule reloaded = execute(SchedulerSchedule::reload, updated);
		assertThat(reloaded).as("Updated schedule (reloaded)")
				.hasFieldOrPropertyWithValue("name", "Even better schedule")
				.hasFieldOrPropertyWithValue("id", schedule.getId());
	}

	@Test
	public void testDeleteSchedule() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
		}).build();

		SchedulerSchedule schedule = Builder.create(SchedulerSchedule.class, create -> {
			create.setName("My super duper schedule");
			create.setSchedulerTask(task);
		}).build();

		consume(SchedulerSchedule::delete, schedule);
		SchedulerSchedule reloaded = execute(SchedulerSchedule::reload, schedule);
		assertThat(reloaded).as("Deleted schedule").isNull();

		SchedulerTask reloadedTask = execute(SchedulerTask::reload, task);
		assertThat(reloadedTask).as("Task").isNotNull();
	}

	@Test
	public void testDeleteTaskWithSchedule() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, create -> {
			create.setName("My super scheduler task");
			create.setCommand("publish");
		}).build();

		SchedulerSchedule schedule = Builder.create(SchedulerSchedule.class, create -> {
			create.setName("My super duper schedule");
			create.setSchedulerTask(task);
		}).build();

		consume(SchedulerTask::delete, task);
		SchedulerTask reloadedTask = execute(SchedulerTask::reload, task);
		assertThat(reloadedTask).as("Deleted task").isNull();

		SchedulerSchedule reloadedSchedule = execute(SchedulerSchedule::reload, schedule);
		assertThat(reloadedSchedule).as("Schedule of deleted task").isNull();
	}
}
