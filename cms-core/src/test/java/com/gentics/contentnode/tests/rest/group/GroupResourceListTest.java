package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;

/**
 * Sorting and filtering tests for {@link GroupResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class GroupResourceListTest extends AbstractListSortAndFilterTest<Group> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Group, String>>> attributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Group::getName),
				Pair.of("description", Group::getDescription)
		);
		return data(attributes, attributes);
	}

	@Override
	protected Group createItem() throws NodeException {
		return supply(() -> UserGroup.TRANSFORM2REST.apply(Builder.create(UserGroup.class, g -> {
			g.setMotherId(ContentNodeTestDataUtils.NODE_GROUP_ID);
			g.setName(randomStringGenerator.generate(5, 10));
			g.setDescription(randomStringGenerator.generate(10, 20));
		}).build()));
	}

	@Override
	protected AbstractListResponse<Group> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new GroupResourceImpl().list(filter, sort, paging, new PermsParameterBean());
	}
}
