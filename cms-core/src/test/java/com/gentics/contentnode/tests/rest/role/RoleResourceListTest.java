package com.gentics.contentnode.tests.rest.role;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Role;
import com.gentics.contentnode.rest.model.RoleModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.RoleResource;
import com.gentics.contentnode.rest.resource.impl.RoleResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link RoleResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class RoleResourceListTest extends AbstractListSortAndFilterTest<RoleModel> {

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<RoleModel, String>>> attributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", item -> item.getName()),
				Pair.of("description", item -> item.getDescription())
		);
		return data(attributes, attributes);
	}

	@Override
	protected RoleModel createItem() throws NodeException {
		return supply(() -> {
			return Role.TRANSFORM2REST.apply(Builder.create(Role.class, role -> {
				role.setName(randomStringGenerator.generate(5, 10), 1);
				role.setName(randomStringGenerator.generate(5, 10), 2);
				role.setDescription(randomStringGenerator.generate(10, 20), 1);
				role.setDescription(randomStringGenerator.generate(10, 20), 2);
			}).build());
		});
	}

	@Override
	protected AbstractListResponse<RoleModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new RoleResourceImpl().list(filter, sort, paging, new PermsParameterBean());
	}
}
