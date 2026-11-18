package com.gentics.contentnode.tests.rest.portalimplementation;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.PortalImplementationResource;
import com.gentics.contentnode.rest.resource.impl.PortalImplementationResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link PortalImplementationResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class PortalImplementationResourceListTest extends AbstractListSortAndFilterTest<PortalImplementationModel> {
	/**
	 * Counter to make keywords and names unique
	 */
	private static AtomicInteger counter = new AtomicInteger();

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<PortalImplementationModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", impl -> AbstractListSortAndFilterTest.addLeadingZeros(impl.getId())),
				Pair.of("globalId", PortalImplementationModel::getGlobalId),
				Pair.of("keyword", PortalImplementationModel::getKeyword),
				Pair.of("name", PortalImplementationModel::getName),
				Pair.of("description", PortalImplementationModel::getDescription),
				Pair.of("type", impl -> impl.getType().name())
		);
		List<Pair<String, Function<PortalImplementationModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", impl -> AbstractListSortAndFilterTest.addLeadingZeros(impl.getId())),
				Pair.of("globalId", PortalImplementationModel::getGlobalId),
				Pair.of("keyword", PortalImplementationModel::getKeyword),
				Pair.of("name", PortalImplementationModel::getName),
				Pair.of("description", PortalImplementationModel::getDescription),
				Pair.of("type", impl -> impl.getType().name()),
				Pair.of("cdate", impl -> AbstractListSortAndFilterTest.addLeadingZeros(impl.getCdate())),
				Pair.of("edate", impl -> AbstractListSortAndFilterTest.addLeadingZeros(impl.getEdate()))
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected PortalImplementationModel createItem() throws NodeException {
		int index = counter.incrementAndGet();
		return supply(() -> PortalImplementation.TRANSFORM2REST.apply(Builder.create(PortalImplementation.class, impl -> {
			impl.setKeyword(randomStringGenerator.generate(5, 10) + "_" + index);
			impl.setName(randomStringGenerator.generate(5, 10) + " " + index);
			impl.setDescription(randomStringGenerator.generate(10, 20));
			impl.setType(getRandomEntry(PortalImplementationModel.Type.values()));
		}).build()));
	}

	@Override
	protected AbstractListResponse<PortalImplementationModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new PortalImplementationResourceImpl().list(filter, sort, paging, new PermsParameterBean());
	}
}
