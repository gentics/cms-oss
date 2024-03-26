package com.gentics.contentnode.tests.rest.cr;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.cr.CrFragment;
import com.gentics.contentnode.rest.model.ContentRepositoryFragmentModel;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Type;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ContentRepositoryResource;
import com.gentics.contentnode.rest.resource.impl.ContentRepositoryResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ContentRepositoryResource#listCrFragments(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class ContentRepositoryResourceListCrFragmentsTest extends AbstractListSortAndFilterTest<ContentRepositoryFragmentModel> {
	public static ContentRepository CR;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		CR = Builder.create(ContentRepository.class, cr -> {
			cr.setCrType(Type.mesh);
			cr.setName(randomStringGenerator.generate(5, 10));
		}).build();
	}

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
		CrFragment fragment = supply(() -> Builder.create(CrFragment.class, fr -> {
			fr.setName(randomStringGenerator.generate(5, 10));
		}).build());

		CR = Builder.update(CR, cr -> {
			cr.getAssignedFragments().add(fragment);
		}).build();

		return supply(() -> fragment.getModel());
	}

	@Override
	protected AbstractListResponse<ContentRepositoryFragmentModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ContentRepositoryResourceImpl().listCrFragments(Integer.toString(CR.getId()), filter, sort, paging, new PermsParameterBean());
	}
}
