package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.Construct;
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
 * Sorting and filtering tests for {@link NodeResource#getConstructs(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class NodeResourceGetConstructsTest extends AbstractListSortAndFilterTest<Construct> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Construct, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", construct -> AbstractListSortAndFilterTest.addLeadingZeros(construct.getId())),
				Pair.of("globalId", Construct::getGlobalId),
				Pair.of("keyword", Construct::getKeyword),
				Pair.of("name", Construct::getName),
				Pair.of("description", Construct::getDescription)
		);
		List<Pair<String, Function<Construct, String>>> filterAttributes = Arrays.asList(
				Pair.of("keyword", Construct::getKeyword),
				Pair.of("name", Construct::getName),
				Pair.of("description", Construct::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Construct createItem() throws NodeException {
		return supply(() -> {
			com.gentics.contentnode.object.Construct construct = Builder.create(com.gentics.contentnode.object.Construct.class, c -> {
				c.setName(randomStringGenerator.generate(5, 10), 1);
				c.setName(randomStringGenerator.generate(5, 10), 2);
				c.setKeyword(randomStringGenerator.generate(5, 10));
				c.setDescription(randomStringGenerator.generate(10, 20), 1);
				c.setDescription(randomStringGenerator.generate(10, 20), 2);
			}).build();
			node = Builder.update(node, n -> {
				n.addConstruct(construct);
			}).build();
			return com.gentics.contentnode.object.Construct.TRANSFORM2REST
					.apply(construct);
		});
	}

	@Override
	protected AbstractListResponse<Construct> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new NodeResourceImpl().getConstructs(Integer.toString(node.getId()), filter, sort, paging, new PermsParameterBean());
	}
}
