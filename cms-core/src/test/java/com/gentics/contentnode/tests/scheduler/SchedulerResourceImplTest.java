package com.gentics.contentnode.tests.scheduler;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.scheduler.ExecutionListResponse;
import com.gentics.contentnode.rest.model.scheduler.ExecutionModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleListResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.rest.resource.impl.scheduler.SchedulerResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SchedulerJobFilterParameterBean;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;


public class SchedulerResourceImplTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	private SchedulerTask createdTask = null;
	private SchedulerSchedule schedule = null;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		operate(() -> createNode());
	}

	@Before
	public void init() throws NodeException {
		createdTask = givenTask();
		schedule = givenScheduleWithTask(createdTask);
	}

	@After
	public void reset() throws NodeException {
		Trx.operate(trx -> {
			if (schedule != null) {
				trx.getObject(schedule, true).delete();
			}
			if (createdTask != null) {
				trx.getObject(createdTask, true).delete();
			}
		});
	}

	@Test
	public void givenScheduleWithEmbeddedTaskRequest_shouldHaveEmbeddedTaskInResponse()
			throws Exception {
		ScheduleListResponse scheduleListResponse = new SchedulerResourceImpl().listSchedules(
				null, null, null, null,
				new EmbedParameterBean().withEmbed("task"), null);

		ScheduleModel retrievedSchedule = scheduleListResponse.getItems()
				.stream().filter(scheduleModel -> scheduleModel.getId().equals(schedule.getId())).findAny()
				.get();

		assertThat(retrievedSchedule.getTask()).hasFieldOrPropertyWithValue("id", createdTask.getId());
		assertThat(retrievedSchedule.getTask()).hasFieldOrPropertyWithValue("name",
				createdTask.getName());
		assertThat(retrievedSchedule.getTask()).hasFieldOrPropertyWithValue("command",
				createdTask.getCommand());
	}

	@Test
	public void givenSchedule_filterByActive_shouldHaveResponse()
			throws Exception {
		givenScheduleWithTask(createdTask, false);

		SchedulerJobFilterParameterBean filter = new SchedulerJobFilterParameterBean();

		filter.active = true;
		ScheduleListResponse scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
		assertThat(scheduleListResponse.getItems()).isNotEmpty().allMatch(a -> ((ScheduleModel) a).getActive());

		filter.active = false;
		scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
		assertThat(scheduleListResponse.getItems()).isNotEmpty().allMatch(a -> !((ScheduleModel) a).getActive());
	}

	@Test
	public void givenSchedule_filterByFailed_shouldHaveResponse()
			throws Exception {
		SchedulerJobFilterParameterBean filter = new SchedulerJobFilterParameterBean();

		// 1. No runs
		filter.failed = true;
		ScheduleListResponse scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
		assertThat(scheduleListResponse.getItems()).isEmpty();
		filter.failed = false;
		scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
		assertThat(scheduleListResponse.getItems()).isNotEmpty().allMatch(a -> ((ScheduleModel) a).getLastExecution() == null);

		// 2. Failed run
		operate(() -> {
			new SchedulerResourceImpl().executeSchedule(schedule.getId().toString());
		});
		Thread.sleep(500);
		operate(() -> {
			while (new SchedulerResourceImpl().getExecution(schedule.getId().toString()).getItem().isRunning()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
	
		filter.failed = true;
		scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
		assertThat(scheduleListResponse.getItems()).hasSize(1).allMatch(a -> 
				((ScheduleModel) a).getLastExecution() != null 
				&& ((ScheduleModel) a).getLastExecution().getResult() != null 
				&& !((ScheduleModel) a).getLastExecution().getResult() && a.getId().equals(schedule.getId()));
		filter.failed = false;
		scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
		assertThat(scheduleListResponse.getItems()).isEmpty();

		// 3. Succeeded run
		SchedulerTask dummyTask = Builder.create(
				SchedulerTask.class,
				task -> {
					task.setName("Purge logs task");
					task.setCommand("purgelogs");
					task.setInternal(true);
				}).build();
		SchedulerSchedule dummySchedule = givenScheduleWithTask(dummyTask);

		try {
			operate(() -> {
				new SchedulerResourceImpl().executeSchedule(dummySchedule.getId().toString());
			});
			Thread.sleep(500);
			operate(() -> {
				while (new SchedulerResourceImpl().getExecution(dummySchedule.getId().toString()).getItem().isRunning()) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						break;
					}
				}
			});
		
			filter.failed = true;
			scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
			assertThat(scheduleListResponse.getItems()).hasSize(1).allMatch(a -> 
					((ScheduleModel) a).getLastExecution() != null 
					&& ((ScheduleModel) a).getLastExecution().getResult() != null 
					&& !((ScheduleModel) a).getLastExecution().getResult() 
					&& !a.getId().equals(dummySchedule.getId()));
			filter.failed = false;
			scheduleListResponse = new SchedulerResourceImpl().listSchedules(null, null, null, null, null, filter);
			assertThat(scheduleListResponse.getItems()).hasSize(1).allMatch(a -> 
					((ScheduleModel) a).getLastExecution() != null 
					&& ((ScheduleModel) a).getLastExecution().getResult() != null 
					&& ((ScheduleModel) a).getLastExecution().getResult() 
					&& a.getId().equals(dummySchedule.getId()));
		} catch (Throwable e) {
			Trx.operate(trx -> {
				trx.getObject(dummySchedule, true).delete();
				trx.getObject(dummyTask, true).delete();
			});
			throw e;
		}
	}

	@Test
	public void givenExecutionWithEmbeddedScheduleRequest_shouldHaveEmbeddedScheduleInResponse()
			throws Exception {
		OffsetDateTime now = OffsetDateTime.now();
		int executionId = SchedulerTestUtils.startExecution(schedule, now);

		SchedulerTestUtils.finishExecution(executionId, now.minusSeconds(1), 59, true);


		ExecutionListResponse executionListResponse = new SchedulerResourceImpl().listExecutions(
				schedule.getId().toString(), null, null, null, null,
				new EmbedParameterBean().withEmbed("schedule"));


		ExecutionModel retrievedExecution = executionListResponse.getItems().stream()
				.filter(executionModel -> executionId == executionModel.getId())
				.findAny()
				.get();

		assertThat(retrievedExecution).isNotNull();
		assertThat(retrievedExecution.getSchedule()).hasFieldOrPropertyWithValue("id", schedule.getId());
		assertThat(retrievedExecution.getSchedule()).hasFieldOrPropertyWithValue("name", schedule.getName());
		assertThat(retrievedExecution.getSchedule()).hasFieldOrPropertyWithValue("description", schedule.getDescription());
	}

	private SchedulerSchedule givenScheduleWithTask(SchedulerTask task) throws NodeException {
		return givenScheduleWithTask(task, true);
	}

	private SchedulerSchedule givenScheduleWithTask(SchedulerTask task, boolean active) throws NodeException {
		return Builder.create(SchedulerSchedule.class, create -> {
			create.setSchedulerTask(task);
			create.setName("Publish Schedule");
			create.setDescription("Description of the Publish Schedule");
			create.setActive(active);
			create.setScheduleData(new ScheduleData().setType(ScheduleType.manual));
		}).build();
	}

	private SchedulerTask givenTask() throws NodeException {
		return Builder.create(SchedulerTask.class, create -> {
			create.setName("My scheduler task");
			create.setCommand("publish");
		}).build();
	}

}
