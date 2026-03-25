package com.gentics.contentnode.tests.rest.form;

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
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.response.AbstractListResponse;
import com.gentics.contentnode.rest.resource.FormResource;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterFormParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.rest.AbstractListSortAndFilterTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.ConfigBy;

/**
 * Sorting and filtering tests for {@link FormResource#list(InFolderParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, WastebinParameterBean, PublishableParameterBean)}
 */
@GCNFeature(set = { Feature.FORMS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormResourceListTest extends AbstractListSortAndFilterTest<FormModel> {
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode(Feature.FORMS));
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<FormModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", FormModel::getName),
				Pair.of("description", FormModel::getDescription)
		);
		List<Pair<String, Function<FormModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", FormModel::getName),
				Pair.of("description", FormModel::getDescription)
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected FormModel createItem() throws NodeException {
		return supply(() -> FormResourceImpl.TRANSFORM2REST.apply(Builder.create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setFolderId(node.getFolder().getId());
			f.setName(randomStringGenerator.generate(5, 10));
			f.setDescription(randomStringGenerator.generate(10, 20));
		}).build()));
	}

	@Override
	protected AbstractListResponse<FormModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		FilterFormParameterBean formFilter = new FilterFormParameterBean();
		formFilter.query = filter.query;
		return new FormResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())), formFilter, sort,
				paging, new WastebinParameterBean(), new PublishableParameterBean());
	}
}
