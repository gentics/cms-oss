package com.gentics.contentnode.tests.rest.scheduler;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.scheduler.ExecutionModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.rest.resource.impl.scheduler.SchedulerResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ExecutionFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.scheduler.SchedulerResource;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link SchedulerResource#listExecutions(String, FilterParameterBean, SortParameterBean, PagingParameterBean, ExecutionFilterParameterBean)}
 */
public class SchedulerResourceListExecutionsTest extends AbstractListSortAndFilterTest<ExecutionModel> {
	protected static SchedulerSchedule schedule;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		SchedulerTask task = Builder.create(SchedulerTask.class, t -> {
			t.setName(randomStringGenerator.generate(5, 10));
			t.setDescription(randomStringGenerator.generate(10, 20));
			t.setInternal(false);
			t.setCommand(randomStringGenerator.generate(5, 10));
		}).build();

		schedule = Builder.create(SchedulerSchedule.class, s -> {
			s.setActive(random.nextBoolean());
			s.setName(randomStringGenerator.generate(5, 10));
			s.setDescription(randomStringGenerator.generate(10, 20));
			s.setParallel(random.nextBoolean());
			s.setSchedulerTask(task);
			s.setScheduleData(new ScheduleData().setType(ScheduleType.manual));
		}).build();
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ExecutionModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("startTime", item -> addLeadingZeros(item.getStartTime())),
				Pair.of("endTime", item -> addLeadingZeros(item.getEndTime())),
				Pair.of("duration", item -> addLeadingZeros(item.getDuration())),
				Pair.of("result", item -> item.getResult() ? "0" : "1")
		);
		List<Pair<String, Function<ExecutionModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("scheduleId", item -> addLeadingZeros(item.getScheduleId())),
				Pair.of("log", ExecutionModel::getLog)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ExecutionModel createItem() throws NodeException {
		int executionId = supply(() -> {
			int startTimestamp = random.nextInt(10000);
			int endTimestamp = startTimestamp + random.nextInt(10000);
			int duration = endTimestamp - startTimestamp;
			int result = random.nextInt(2);
			String log = randomStringGenerator.generate(10, 20);

			List<Integer> ids = DBUtils.executeInsert(
					"INSERT INTO scheduler_execution(scheduler_schedule_id, starttime, endtime, duration, result, log) VALUES (?, ?, ?, ?, ?, ?)",
					new Object[] { schedule.getId(), startTimestamp, endTimestamp, duration, result, log });

			return ids.get(0);
		});

		return supply(() -> {
			return DBUtils.select(
					"SELECT * FROM scheduler_execution WHERE id = ?",
					ps -> ps.setInt(1, executionId),
					rs -> rs.next() ? ExecutionModel.fromDbResult(rs) : null);
		});
	}

	@Override
	protected AbstractListResponse<ExecutionModel> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new SchedulerResourceImpl().listExecutions(Integer.toString(schedule.getId()), filter, sort, paging, new ExecutionFilterParameterBean(), new EmbedParameterBean());
	}
}
