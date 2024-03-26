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
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link TemplateResource#folders(String, SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
public class TemplateResourceFoldersTest extends AbstractListSortAndFilterTest<Folder> {
	private static Node node;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<Folder, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Folder::getName),
				Pair.of("description", Folder::getDescription)
		);
		List<Pair<String, Function<Folder, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Folder::getName),
				Pair.of("description", Folder::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Folder createItem() throws NodeException {
		return supply(() -> {
			com.gentics.contentnode.object.Folder folder = Builder.create(com.gentics.contentnode.object.Folder.class, f -> {
				f.setMotherId(node.getFolder().getId());
				f.setName(randomStringGenerator.generate(5, 10));
				f.setDescription(randomStringGenerator.generate(10, 20));
				f.setPublishDir(randomStringGenerator.generate(5, 10));
			}).build();

			template = Builder.update(template, t -> {
				t.addFolder(folder);
			}).build();

			return ModelBuilder.getFolder(folder);
		});
	}

	@Override
	protected AbstractListResponse<Folder> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new TemplateResourceImpl().folders(Integer.toString(template.getId()), sort, filter, paging);
	}
}
