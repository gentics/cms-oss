package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.supply;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.rest.model.Node;
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
 * Sorting and filtering tests for {@link NodeResource#list(FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean, String)}
 */
public class NodeResourceListTest extends AbstractListSortAndFilterTest<Node> {
	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		// "id", "name"
		List<Pair<String, Function<Node, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", Node::getName)
		);
		// "id", "folder.name"
		List<Pair<String, Function<Node, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> addLeadingZeros(item.getId())),
				Pair.of("name", Node::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Node createItem() throws NodeException {
		return supply(() -> com.gentics.contentnode.object.Node.TRANSFORM2REST
				.apply(Builder.create(com.gentics.contentnode.object.Node.class, n -> {
					n.setFolder(Builder.create(Folder.class, f -> {
						f.setName(randomStringGenerator.generate(5, 10));
						f.setPublishDir("/");
					}).doNotSave().build());
					n.setHostname(randomStringGenerator.generate(5, 10));
					n.setPublishDir("/");
				}).build()));
	}

	@Override
	protected AbstractListResponse<Node> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new NodeResourceImpl().list(filter, sort, paging, new PermsParameterBean(), null);
	}
}
