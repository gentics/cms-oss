package com.gentics.contentnode.tests.rest.template;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Node;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link TemplateResource#nodes(String, SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
public class TemplateResourceNodesTest extends AbstractListSortAndFilterTest<Node> {
	private static com.gentics.contentnode.object.Node node;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Node, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Node::getName)
		);
		List<Pair<String, Function<Node, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Node::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Node createItem() throws NodeException {
		return supply(() -> {
			com.gentics.contentnode.object.Node node = Builder.create(com.gentics.contentnode.object.Node.class, n -> {
				n.setFolder(Builder.create(Folder.class, f -> {
					f.setName(randomStringGenerator.generate(5, 10));
					f.setPublishDir("/");
				}).doNotSave().build());
				n.setHostname(randomStringGenerator.generate(5, 10));
				n.setPublishDir("/");
			}).build();

			node = Builder.update(node, n -> {
				n.addTemplate(template);
			}).build();

			return com.gentics.contentnode.object.Node.TRANSFORM2REST.apply(node);
		});
	}

	@Override
	protected AbstractListResponse<Node> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new TemplateResourceImpl().nodes(Integer.toString(template.getId()), sort, filter, paging);
	}
}
