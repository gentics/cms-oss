package com.gentics.contentnode.tests.rest.datasource;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Datasource.SourceType;
import com.gentics.contentnode.rest.model.Datasource;
import com.gentics.contentnode.rest.model.DatasourceType;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.DatasourceResource;
import com.gentics.contentnode.rest.resource.impl.DatasourceResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link DatasourceResource#list(SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
public class DatasourceResourceListTest extends AbstractListSortAndFilterTest<Datasource> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Datasource, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("globalId", Datasource::getGlobalId),
				Pair.of("name", Datasource::getName),
				Pair.of("type", item -> addLeadingOrder(item.getType()))
		);
		List<Pair<String, Function<Datasource, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("globalId", Datasource::getGlobalId),
				Pair.of("name", Datasource::getName),
				Pair.of("type", item -> item.getType().name())
		);
		return data(sortAttributes, filterAttributes);
	}

	protected static String addLeadingOrder(DatasourceType type) {
		switch (type) {
		case STATIC:
			return "0_" + type.name();
		case SITEMINDER:
			return "1_" + type.name();
		default:
			return type.name();
		}
	}

	@Override
	protected Datasource createItem() throws NodeException {
		return supply(() -> com.gentics.contentnode.object.Datasource.TRANSFORM2REST
				.apply(Builder.create(com.gentics.contentnode.object.Datasource.class, ds -> {
					ds.setName(randomStringGenerator.generate(5, 10));
					ds.setSourceType(getRandomEntry(SourceType.values()));
				}).build()));
	}

	@Override
	protected AbstractListResponse<Datasource> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging) throws NodeException {
		return new DatasourceResourceImpl().list(sort, filter, paging);
	}
}
