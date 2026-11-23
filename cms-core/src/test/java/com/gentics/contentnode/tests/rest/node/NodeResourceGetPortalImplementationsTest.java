package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.PortalImplementation;
import com.gentics.contentnode.rest.model.PortalImplementationModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.NodeResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link NodeResource#getPortalImplementations(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class NodeResourceGetPortalImplementationsTest extends AbstractListSortAndFilterTest<PortalImplementationModel> {
	private static Node node;

	/**
	 * Counter to make keywords and names unique
	 */
	private static AtomicInteger counter = new AtomicInteger();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

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
		return supply(() -> {
			PortalImplementation impl = Builder.create(PortalImplementation.class, created -> {
				created.setKeyword(randomStringGenerator.generate(5, 10) + index);
				created.setName(randomStringGenerator.generate(5, 10) + index);
				created.setDescription(randomStringGenerator.generate(10, 20));
				created.setType(getRandomEntry(Arrays.asList(PortalImplementationModel.Type.values())));
			}).build();
			node.addPortalImplementation(impl);
			return PortalImplementation.TRANSFORM2REST.apply(impl);
		});
	}

	@Override
	protected AbstractListResponse<PortalImplementationModel> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		try {
			return new NodeResourceImpl().getPortalImplementations(Integer.toString(node.getId()), filter, sort, paging, new PermsParameterBean());
		} catch (NodeException e) {
			throw e;
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}
}
