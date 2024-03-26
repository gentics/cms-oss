package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PageListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link PageResource#list(InFolderParameterBean, PageListParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, PublishableParameterBean, WastebinParameterBean)}
 */
@GCNFeature(set = { Feature.NICE_URLS })
public class PageResourceListTest extends AbstractListSortAndFilterTest<Page> {
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
		List<Pair<String, Function<Page, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Page::getName),
				Pair.of("fileName", Page::getFileName),
				Pair.of("niceUrl", Page::getNiceUrl),
				Pair.of("alternateUrls", item -> item.getAlternateUrls().first())
		);
		List<Pair<String, Function<Page, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", Page::getName),
				Pair.of("fileName", Page::getFileName),
				Pair.of("description", Page::getDescription),
				Pair.of("niceUrl", Page::getNiceUrl),
				Pair.of("alternateUrls", item -> item.getAlternateUrls().first())
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected Page createItem() throws NodeException {
		return supply(() -> ModelBuilder.getPage(Builder.create(com.gentics.contentnode.object.Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName(randomStringGenerator.generate(5, 10));
			p.setFilename(randomStringGenerator.generate(5, 10));
			p.setDescription(randomStringGenerator.generate(10, 20));
			p.setNiceUrl(randomStringGenerator.generate(5, 10));
			p.setAlternateUrls(randomStringGenerator.generate(5, 10));
		}).build(), Collections.emptyList()));
	}

	@Override
	protected AbstractListResponse<Page> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		return new PageResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())),
				new PageListParameterBean(), filter, sort, paging, new PublishableParameterBean(),
				new WastebinParameterBean());
	}
}
