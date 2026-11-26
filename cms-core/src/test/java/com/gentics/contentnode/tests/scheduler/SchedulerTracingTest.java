package com.gentics.contentnode.tests.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.SchedulerFactory;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;

/**
 * Test cases for the tracing context of scheduler executions
 */
public class SchedulerTracingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public OpenTelemetryRule otel = OpenTelemetryRule.create();

	private static SchedulerFactory factory;

	private static SchedulerTask task;

	private static SchedulerSchedule schedule;

	/**
	 * Original executor of the scheduler factory
	 */
	private ExecutorService originalExecutor;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		factory = (SchedulerFactory) testContext.getContext().getContentNodeFactory().getFactory().getObjectFactory(SchedulerSchedule.class);
		// the schedule is only executed with executeNow()
		factory.suspend(Collections.emptySet());
		testContext.getContext().getTransaction().commit();

		task = Builder.create(SchedulerTask.class, t -> {
			t.setName("Tracing task");
			t.setCommand("dummy");
		}).build();
		schedule = Builder.create(SchedulerSchedule.class, s -> {
			s.setName("Tracing schedule");
			s.setActive(true);
			s.setParallel(true);
			s.setSchedulerTask(task);
			s.setScheduleData(new ScheduleData().setType(ScheduleType.manual));
		}).build();
	}

	/**
	 * Let the executor of the scheduler factory propagate the context to the executed tasks, like the OpenTelemetry Java agent does
	 * @throws IllegalAccessException
	 */
	@Before
	public void setup() throws IllegalAccessException {
		originalExecutor = (ExecutorService) FieldUtils.readField(factory, "executor", true);
		FieldUtils.writeField(factory, "executor", Context.taskWrapping(originalExecutor), true);
	}

	@After
	public void tearDown() throws IllegalAccessException {
		FieldUtils.writeField(factory, "executor", originalExecutor, true);
	}

	/**
	 * Test that a schedule, which is executed while a span is current (e.g. the span of a REST request), is not executed with this span as parent
	 * @throws Exception
	 */
	@Test
	public void testExecuteNow() throws Exception {
		AtomicReference<SpanContext> spanContextInTask = new AtomicReference<>();
		CountDownLatch executed = new CountDownLatch(1);

		SchedulerTask mockedTask = mock(SchedulerTask.class);
		when(mockedTask.getCommand()).thenReturn(task.getCommand());
		when(mockedTask.getSanitizedCommand()).thenReturn(task.getSanitizedCommand());
		when(mockedTask.isInternal()).thenReturn(task.isInternal());
		when(mockedTask.execute(Mockito.anyInt(), Mockito.anyList())).thenAnswer(invocation -> {
			spanContextInTask.set(Span.current().getSpanContext());
			executed.countDown();
			return 0;
		});

		SchedulerSchedule mockedSchedule = mock(SchedulerSchedule.class);
		when(mockedSchedule.getId()).thenReturn(schedule.getId());
		when(mockedSchedule.getName()).thenReturn(schedule.getName());
		when(mockedSchedule.getScheduleData()).thenReturn(schedule.getScheduleData());
		when(mockedSchedule.getSchedulerTask()).thenReturn(mockedTask);
		when(mockedSchedule.isActive()).thenReturn(true);
		when(mockedSchedule.isParallel()).thenReturn(true);

		Span request = otel.getOpenTelemetry().getTracer("test").spanBuilder("request").startSpan();
		try (Scope scope = request.makeCurrent()) {
			assertThat(factory.executeNow(mockedSchedule)).as("Execute now").isTrue();
		} finally {
			request.end();
		}

		assertThat(executed.await(10, TimeUnit.SECONDS)).as("Task was executed").isTrue();
		assertThat(spanContextInTask.get()).as("Span context in task").isNotNull();
		assertThat(spanContextInTask.get().isValid()).as("Task executed with a span").isFalse();
	}
}
