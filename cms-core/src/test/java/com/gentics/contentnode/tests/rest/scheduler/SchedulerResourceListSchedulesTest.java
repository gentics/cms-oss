package com.gentics.contentnode.tests.rest.scheduler;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.scheduler.SchedulerSchedule;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.scheduler.ScheduleData;
import com.gentics.contentnode.rest.model.scheduler.ScheduleModel;
import com.gentics.contentnode.rest.model.scheduler.ScheduleType;
import com.gentics.contentnode.rest.resource.impl.scheduler.SchedulerResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SchedulerJobFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.scheduler.SchedulerResource;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link SchedulerResource#listSchedules(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class SchedulerResourceListSchedulesTest extends AbstractListSortAndFilterTest<ScheduleModel> {
	protected final static int NUM_TASKS = 20;

	protected static List<SchedulerTask> tasks = new ArrayList<>();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		for (int i = 0; i < NUM_TASKS; i++) {
			tasks.add(Builder.create(SchedulerTask.class, t -> {
				t.setName(randomStringGenerator.generate(5, 10));
				t.setDescription(randomStringGenerator.generate(10, 20));
				t.setInternal(false);
				t.setCommand(randomStringGenerator.generate(5, 10));
			}).build());
		}
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ScheduleModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", ScheduleModel::getName),
				Pair.of("description", ScheduleModel::getDescription),
				Pair.of("taskId", item -> addLeadingZeros(item.getTaskId())),
				Pair.of("cdate", item -> addLeadingZeros(item.getCdate())),
				Pair.of("edate", item -> addLeadingZeros(item.getEdate()))
		);
		List<Pair<String, Function<ScheduleModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", ScheduleModel::getName),
				Pair.of("description", ScheduleModel::getDescription),
				Pair.of("taskId", item -> addLeadingZeros(item.getTaskId()))
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ScheduleModel createItem() throws NodeException {
		SchedulerSchedule schedule = Builder.create(SchedulerSchedule.class, s -> {
			s.setActive(random.nextBoolean());
			s.setName(randomStringGenerator.generate(5, 10));
			s.setDescription(randomStringGenerator.generate(10, 20));
			s.setParallel(random.nextBoolean());
			s.setSchedulerTask(getRandomEntry(tasks));
			s.setScheduleData(new ScheduleData().setType(ScheduleType.manual));
		}).build();

		return supply(() -> {
			return SchedulerSchedule.TRANSFORM2REST.apply(schedule);
		});
	}

	@Override
	protected AbstractListResponse<ScheduleModel> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new SchedulerResourceImpl().listSchedules(filter, sort, paging, new PermsParameterBean(), new EmbedParameterBean(), new SchedulerJobFilterParameterBean());
	}
}
