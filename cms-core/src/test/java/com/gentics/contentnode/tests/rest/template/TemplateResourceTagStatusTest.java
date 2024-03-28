package com.gentics.contentnode.tests.rest.template;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.model.response.TagStatus;
import com.gentics.contentnode.rest.resource.TemplateResource;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;

/**
 * Sorting and filtering tests for {@link TemplateResource#tagStatus(String, SortParameterBean, FilterParameterBean, PagingParameterBean)}
 */
public class TemplateResourceTagStatusTest extends AbstractListSortAndFilterTest<TagStatus> {
	private static com.gentics.contentnode.object.Node node;
	private static Template template;
	private static Integer constructId;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode());

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = supply(() -> createConstruct(node, HTMLPartType.class, randomStringGenerator.generate(5, 10), randomStringGenerator.generate(5, 10)));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<TagStatus, String>>> sortAttributes = Arrays.asList(
				Pair.of("name", TagStatus::getName)
		);
		List<Pair<String, Function<TagStatus, String>>> filterAttributes = Arrays.asList(
				Pair.of("name", TagStatus::getName)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected TagStatus createItem() throws NodeException {
		String tagName = randomStringGenerator.generate(5, 10);
		template = Builder.update(template, t -> {
			Map<String, TemplateTag> templateTags = t.getTemplateTags();
			TemplateTag templateTag = Builder.create(TemplateTag.class, tag -> {
				tag.setConstructId(constructId);
				tag.setEnabled(true);
				tag.setName(tagName);
				tag.setPublic(true);
			}).doNotSave().build();
			templateTags.put(tagName, templateTag);
		}).build();

		return new TagStatus().setName(tagName);
	}

	@Override
	protected AbstractListResponse<TagStatus> getResult(SortParameterBean sort, FilterParameterBean filter,
			PagingParameterBean paging) throws NodeException {
		return new TemplateResourceImpl().tagStatus(Integer.toString(template.getId()), sort, filter, paging);
	}
}
