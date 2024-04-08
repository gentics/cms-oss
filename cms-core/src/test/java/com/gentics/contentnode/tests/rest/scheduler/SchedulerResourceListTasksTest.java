package com.gentics.contentnode.tests.rest.scheduler;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.scheduler.SchedulerTask;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.scheduler.TaskModel;
import com.gentics.contentnode.rest.resource.impl.scheduler.SchedulerResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.scheduler.SchedulerResource;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link SchedulerResource#listTasks(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class SchedulerResourceListTasksTest extends AbstractListSortAndFilterTest<TaskModel> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<TaskModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", TaskModel::getName),
				Pair.of("description", TaskModel::getDescription),
				Pair.of("command", TaskModel::getCommand),
				Pair.of("cdate", item -> addLeadingZeros(item.getCdate())),
				Pair.of("edate", item -> addLeadingZeros(item.getEdate()))
		);
		List<Pair<String, Function<TaskModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", TaskModel::getName),
				Pair.of("description", TaskModel::getDescription),
				Pair.of("command", TaskModel::getCommand)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected TaskModel createItem() throws NodeException {
		SchedulerTask task = Builder.create(SchedulerTask.class, t -> {
			t.setName(randomStringGenerator.generate(5, 10));
			t.setDescription(randomStringGenerator.generate(10, 20));
			t.setInternal(false);
			t.setCommand(randomStringGenerator.generate(5, 10));
		}).build();

		return supply(() -> {
			return SchedulerTask.TRANSFORM2REST.apply(task);
		});
	}

	@Override
	protected AbstractListResponse<TaskModel> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new SchedulerResourceImpl().listTasks(filter, sort, paging, new PermsParameterBean());
	}
}
