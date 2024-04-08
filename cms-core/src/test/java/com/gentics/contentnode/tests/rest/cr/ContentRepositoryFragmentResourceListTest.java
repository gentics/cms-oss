package com.gentics.contentnode.tests.rest.cr;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryFragmentResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryFragmentResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ContentRepositoryFragmentResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class ContentRepositoryFragmentResourceListTest extends AbstractListSortAndFilterTest<ContentRepositoryFragmentModel> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ContentRepositoryFragmentModel, String>>> attributes = Arrays.asList(
				Pair.of("id", fragment -> AbstractListSortAndFilterTest.addLeadingZeros(fragment.getId())),
				Pair.of("globalId", ContentRepositoryFragmentModel::getGlobalId),
				Pair.of("name", ContentRepositoryFragmentModel::getName)
		);
		return data(attributes, attributes);
	}

	@Override
	protected ContentRepositoryFragmentModel createItem() throws NodeException {
		return supply(() -> Builder.create(CrFragment.class, fragment -> {
			fragment.setName(randomStringGenerator.generate(5, 10));
		}).build().getModel());
	}

	@Override
	protected AbstractListResponse<ContentRepositoryFragmentModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ContentRepositoryFragmentResourceImpl().list(filter, sort, paging, new PermsParameterBean());
	}
}
