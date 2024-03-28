package com.gentics.contentnode.tests.rest.objectproperty;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.rest.model.ObjectPropertyCategory;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.ObjectPropertyResource;
import com.gentics.contentnode.rest.resource.impl.ObjectPropertyResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link ObjectPropertyResource#listCategories(SortParameterBean, FilterParameterBean, PagingParameterBean, EmbedParameterBean)}
 */
public class ObjectPropertyResourceListCategoriesTest extends AbstractListSortAndFilterTest<ObjectPropertyCategory> {
	protected final static AtomicInteger sortOrder = new AtomicInteger();

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// "id", "globalId", "name", "sortorder"
		List<Pair<String, Function<ObjectPropertyCategory, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("globalId", ObjectPropertyCategory::getGlobalId),
				Pair.of("name", ObjectPropertyCategory::getName),
				Pair.of("sortOrder", item -> addLeadingZeros(item.getSortOrder()))
		);
		// "id", "globalId", "name", "sortorder"
		List<Pair<String, Function<ObjectPropertyCategory, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("globalId", ObjectPropertyCategory::getGlobalId),
				Pair.of("name", ObjectPropertyCategory::getName),
				Pair.of("sortOrder", item -> addLeadingZeros(item.getSortOrder()))
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected ObjectPropertyCategory createItem() throws NodeException {
		return supply(() -> {
			return ObjectTagDefinitionCategory.TRANSFORM2REST.apply(Builder.create(ObjectTagDefinitionCategory.class, cat -> {
				cat.setName(randomStringGenerator.generate(5, 10), 1);
				cat.setName(randomStringGenerator.generate(5, 10), 2);
				cat.setSortorder(sortOrder.getAndIncrement());
			}).build());
		});
	}

	@Override
	protected AbstractListResponse<ObjectPropertyCategory> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new ObjectPropertyResourceImpl().listCategories(sort, filter, paging, new EmbedParameterBean());
	}
}
