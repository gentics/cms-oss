package com.gentics.contentnode.tests.rest.form;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.object.FormFactory;
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
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Sorting and filtering tests for {@link FormResource#list(InFolderParameterBean, FilterParameterBean, SortParameterBean, PagingParameterBean, WastebinParameterBean, PublishableParameterBean)}
 */
@GCNFeature(set = { Feature.FORMS })
@ConfigBy(FormResourceListTest.class)
public class FormResourceListTest extends AbstractListSortAndFilterTest<FormModel> implements ConfigModifier {
	private static Node node;
	private static FormTypeConfigurationMap config;
	private static List<String> formTypes;

	/**
	 * Number of generated form type configs
	 */
	public final static int NUM_CONFIGS = 10;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		AbstractListSortAndFilterTest.setupOnce();

		node = supply(() -> createNode(Feature.FORMS));
		config = FormConfigProvider.getConfig();

		formTypes = new ArrayList<>(config.keySet());

		operate(() -> {
			for (String type : formTypes) {
				FormFactory.addFormTypeToNode(type, node.getId());
			}
		});
	}

	@Parameters(name = "{index}: sortBy {0}, ascending {2}, filter {3}")
	public static Collection<Object[]> data() {
		List<Pair<String, Function<FormModel, String>>> sortAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", FormModel::getName),
				Pair.of("description", FormModel::getDescription),
				Pair.of("formType", FormModel::getFormType)
		);
		List<Pair<String, Function<FormModel, String>>> filterAttributes = Arrays.asList(
				Pair.of("id", item -> AbstractListSortAndFilterTest.addLeadingZeros(item.getId())),
				Pair.of("name", FormModel::getName),
				Pair.of("description", FormModel::getDescription),
				Pair.of("formType", FormModel::getFormType),
				Pair.of("external", form -> Boolean.toString(config.get(form.getFormType()).external))
		);
		return data(sortAttributes, filterAttributes);
	}

	@Override
	protected FormModel createItem() throws NodeException {
		return supply(() -> FormResourceImpl.TRANSFORM2REST.apply(Builder.create(Form.class, f -> {
			f.setFormType(randomFormType());
			f.setFolderId(node.getFolder().getId());
			f.setName(randomStringGenerator.generate(5, 10));
			f.setDescription(randomStringGenerator.generate(10, 20));
		}).build()));
	}

	@Override
	protected AbstractListResponse<FormModel> getResult(SortParameterBean sort, FilterParameterBean filter, PagingParameterBean paging)
			throws NodeException {
		FilterFormParameterBean formFilter = new FilterFormParameterBean();
		if (filterBy != null) {
			switch (filterBy) {
			case "formType":
				formFilter.formType = filter.query;
				break;
			case "external":
				formFilter.external = Boolean.valueOf(filter.query);
				break;
			default:
				formFilter.query = filter.query;
				break;
			}
		}

		return new FormResourceImpl().list(
				new InFolderParameterBean().setFolderId(Integer.toString(node.getFolder().getId())), formFilter, sort,
				paging, new WastebinParameterBean(), new PublishableParameterBean());
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		for (int i = 0; i < NUM_CONFIGS; i++) {
			FormTypeConfiguration config = new FormTypeConfiguration(
					Map.of("de", randomStringGenerator.generate(5, 10), "en", randomStringGenerator.generate(5, 10)),
					randomStringGenerator.generate(5, 10));
			config.external = random.nextBoolean();
			config.submitOnce = random.nextBoolean();

			formTypeConfiguration.put(randomStringGenerator.generate(5, 10), config);
		}

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}

	/**
	 * Return a random form type
	 * @return random form type
	 */
	protected String randomFormType() {
		return formTypes.get(random.nextInt(formTypes.size()));
	}
}
