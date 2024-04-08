package com.gentics.contentnode.tests.rest.admin;

import static com.gentics.contentnode.factory.Trx.operate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.log.ActionLogger;
import com.gentics.contentnode.perm.TypePerms;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.response.log.ActionLogType;
import com.gentics.contentnode.rest.resource.AdminResource;
import com.gentics.contentnode.rest.resource.impl.AdminResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;

import io.reactivex.Flowable;

/**
 * Sorting and filtering tests for {@link AdminResource#getActionLogTypes(FilterParameterBean, SortParameterBean, PagingParameterBean)}
 */
public class AdminResourceGetActionLogTypesTest extends AbstractListSortAndFilterTest<ActionLogType> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ActionLogType, String>>> attributes = Arrays.asList(
				Pair.of("name", ActionLogType::getName),
				Pair.of("label", ActionLogType::getLabel)
		);
		return data(attributes, attributes);
	}

	@Override
	protected ActionLogType createItem() throws NodeException {
		return null;
	}

	@Override
	protected void fillItemsList(List<? super Object> items) throws NodeException {
		operate(() ->  items.addAll(Flowable.fromIterable(ActionLogger.LOGGED_TYPES).map(TypePerms.TRANSFORM2REST::apply).toList()
				.blockingGet()));
	}

	@Override
	protected AbstractListResponse<ActionLogType> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new AdminResourceImpl().getActionLogTypes(filter, sort, paging);
	}
}
