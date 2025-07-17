package com.gentics.contentnode.tests.rest.construct;

import static com.gentics.contentnode.factory.Trx.operate;
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
import com.gentics.contentnode.rest.model.ConstructCategory;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ConstructResource;
import com.gentics.contentnode.rest.resource.impl.ConstructResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.ConstructCategoryParameterBean;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ConstructResource#listCategories(SortParameterBean, FilterParameterBean, PagingParameterBean, EmbedParameterBean)}
 */
public class ConstructResourceListCategoriesTest extends AbstractListSortAndFilterTest<ConstructCategory> {
	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		operate(t -> {
			for (com.gentics.contentnode.object.ConstructCategory category : t.getObjects(
					com.gentics.contentnode.object.ConstructCategory.class,
					DBUtils.select("SELECT id FROM construct_category", DBUtils.IDLIST))) {
				category.delete(true);
			}
		});
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<ConstructCategory, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", category -> AbstractListSortAndFilterTest.addLeadingZeros(category.getId())),
				Pair.of("globalId", ConstructCategory::getGlobalId),
				Pair.of("name", ConstructCategory::getName),
				Pair.of("sortOrder", category -> AbstractListSortAndFilterTest.addLeadingZeros(category.getSortOrder()))
		);
		List<Pair<String, Function<ConstructCategory, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", category -> AbstractListSortAndFilterTest.addLeadingZeros(category.getId())),
				Pair.of("globalId", ConstructCategory::getGlobalId),
				Pair.of("name", ConstructCategory::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ConstructCategory createItem() throws NodeException {
		return supply(() -> {
			return com.gentics.contentnode.object.ConstructCategory.TRANSFORM2REST
					.apply(Builder.create(com.gentics.contentnode.object.ConstructCategory.class, cat -> {
						cat.setName(randomStringGenerator.generate(5, 10), 1);
						cat.setName(randomStringGenerator.generate(5, 10), 2);
					}).build());
		});
	}

	@Override
	protected AbstractListResponse<ConstructCategory> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new ConstructResourceImpl().listCategories(sort, filter, paging, new EmbedParameterBean(), new ConstructCategoryParameterBean());
	}
}
