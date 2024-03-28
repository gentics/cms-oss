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
import com.gentics.contentnode.rest.model.Template;
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
 * Sorting and filtering tests for {@link NodeResource#getTemplates(String, FilterParameterBean, SortParameterBean, PagingParameterBean, PermsParameterBean)}
 */
public class NodeResourceGetTemplatesTest extends AbstractListSortAndFilterTest<Template> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Template, String>>> attributes = Arrays.asList(
				Pair.of("name", Template::getName),
				Pair.of("description", Template::getDescription)
		);
		return data(attributes, attributes);
	}

	@Override
	protected Template createItem() throws NodeException {
		return supply(() -> com.gentics.contentnode.object.Template.TRANSFORM2REST
				.apply(Builder.create(com.gentics.contentnode.object.Template.class, t -> {
					t.setFolderId(node.getFolder().getId());
					t.setName(randomStringGenerator.generate(5, 10));
					t.setDescription(randomStringGenerator.generate(10, 20));
				}).build()));
	}

	@Override
	protected AbstractListResponse<Template> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new NodeResourceImpl().getTemplates(Integer.toString(node.getId()), filter, sort, paging, new PermsParameterBean());
	}
}
