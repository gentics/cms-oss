package com.gentics.contentnode.tests.rest.admin;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.log.Action;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.response.log.ActionModel;
import com.gentics.contentnode.rest.resource.AdminResource;
import com.gentics.contentnode.rest.resource.impl.AdminResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;

/**
 * Sorting and filtering tests for {@link AdminResource#getActionLogActions(FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class AdminResourceGetActionLogActionsTest extends AbstractListSortAndFilterTest<ActionModel> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ActionModel, String>>> attributes = Arrays.asList(
				Pair.of("name", ActionModel::getName),
				Pair.of("label", ActionModel::getLabel)
		);
		return data(attributes, attributes);
	}

	@Override
	protected ActionModel createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super Object> items) throws NodeException {
		operate(() ->  items.addAll(Arrays.asList(Action.values()).stream().map(Action.TRANSFORM2REST).collect(Collectors.toList())));
	}

	@Override
	protected AbstractListResponse<ActionModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new AdminResourceImpl().getActionLogActions(filter, sort, paging);
	}
}
