package com.gentics.contentnode.tests.scheduler;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.gentics.api.lib.exception.NodeException;
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
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;
import java.time.OffsetDateTime;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;


public class SchedulerResourceImplTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();


	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		operate(() -> createNode());
	}


	@Test
	public void givenScheduleWithEmbeddedTaskRequest_shouldHaveEmbeddedTaskInResponse()
			throws Exception {
		SchedulerTask createdTask = givenTask();
		SchedulerSchedule schedule = givenScheduleWithTask(createdTask);

		ScheduleListResponse scheduleListResponse = new SchedulerResourceImpl().listSchedules(
				null, null, null, null,
				new EmbedParameterBean().withEmbed("task"));


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
	public void givenExecutionWithEmbeddedScheduleRequest_shouldHaveEmbeddedScheduleInResponse()
			throws Exception {
		SchedulerTask createdTask = givenTask();
		SchedulerSchedule schedule = givenScheduleWithTask(createdTask);

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
		return Builder.create(SchedulerSchedule.class, create -> {
			create.setSchedulerTask(task);
			create.setName("Publish Schedule");
			create.setDescription("Description of the Publish Schedule");
			create.setActive(true);
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
