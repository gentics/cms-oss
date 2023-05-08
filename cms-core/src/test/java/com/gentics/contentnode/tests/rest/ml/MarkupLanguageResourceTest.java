package com.gentics.contentnode.tests.rest.ml;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.rest.model.MarkupLanguage;
import com.gentics.contentnode.rest.model.response.MarkupLanguageListResponse;
import com.gentics.contentnode.rest.resource.impl.MarkupLanguageResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the MarkupLanguageResource
 */
@RunWith(Parameterized.class)
public class MarkupLanguageResourceTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	protected static MarkupLanguage html;
	protected static MarkupLanguage formsEmailTemplate;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		html = new MarkupLanguage().setId(1).setName("HTML").setExtension("html")
				.setContentType("text/html").setExcludeFromPublishing(false);
		formsEmailTemplate = new MarkupLanguage().setId(21).setName("Forms e-mail template")
				.setExtension("hbs").setContentType("text/x-handlebars").setFeature("forms").setExcludeFromPublishing(true);

	}

	@Parameters(name = "{index}: forms feature {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean formsFeature : Arrays.asList(true, false)) {
			data.add(new Object[] { formsFeature });
		}
		return data;
	}

	@Parameter(0)
	public boolean formsFeature;

	@Before
	public void setup() throws NodeException {
		operate(() -> NodeConfigRuntimeConfiguration.getDefault().getNodeConfig().getDefaultPreferences()
				.setFeature(Feature.FORMS, formsFeature));
	}

	/**
	 * Test getting the list of markup languages
	 * @throws Exception
	 */
	@Test
	public void testList() throws Exception {
		MarkupLanguageListResponse mlListResponse = new MarkupLanguageResourceImpl().list(new SortParameterBean(),
				new FilterParameterBean(), new PagingParameterBean());
		assertResponseOK(mlListResponse);

		if (formsFeature) {
			assertThat(mlListResponse.getItems()).as("List of markup languages").isNotEmpty()
					.usingFieldByFieldElementComparator().contains(html, formsEmailTemplate);
		} else {
			assertThat(mlListResponse.getItems()).as("List of markup languages").isNotEmpty()
					.usingFieldByFieldElementComparator().contains(html).doesNotContain(formsEmailTemplate);
		}
	}

	/**
	 * Test sorting by feature
	 * @throws Exception
	 */
	@Test
	public void testSortByFeature() throws Exception {
		if (formsFeature) {
			// sort by feature ascending
			MarkupLanguageListResponse mlListResponse = new MarkupLanguageResourceImpl().list(new SortParameterBean().setSort("+feature"),
					new FilterParameterBean(), new PagingParameterBean());
			assertResponseOK(mlListResponse);

			// ml with feature must come last
			assertThat(mlListResponse.getItems()).as("List of markup languages").isNotEmpty()
				.usingFieldByFieldElementComparator().endsWith(formsEmailTemplate);

			// sort by feature descending
			mlListResponse = new MarkupLanguageResourceImpl().list(new SortParameterBean().setSort("-feature"),
					new FilterParameterBean(), new PagingParameterBean());

			// ml with feature must come first
			assertThat(mlListResponse.getItems()).as("List of markup languages").isNotEmpty()
				.usingFieldByFieldElementComparator().startsWith(formsEmailTemplate);
		}
	}

	/**
	 * Test sorting by exclusion flag
	 * @throws Exception
	 */
	@Test
	public void testSortByExlusionFlag() throws Exception {
		if (formsFeature) {
			// sort by flag ascending
			MarkupLanguageListResponse mlListResponse = new MarkupLanguageResourceImpl().list(new SortParameterBean().setSort("+excludeFromPublishing"),
					new FilterParameterBean(), new PagingParameterBean());
			assertResponseOK(mlListResponse);

			// ml with feature must come last
			assertThat(mlListResponse.getItems()).as("List of markup languages").isNotEmpty()
				.usingFieldByFieldElementComparator().endsWith(formsEmailTemplate);

			// sort by flag descending
			mlListResponse = new MarkupLanguageResourceImpl().list(new SortParameterBean().setSort("-excludeFromPublishing"),
					new FilterParameterBean(), new PagingParameterBean());

			// ml with feature must come first
			assertThat(mlListResponse.getItems()).as("List of markup languages").isNotEmpty()
				.usingFieldByFieldElementComparator().startsWith(formsEmailTemplate);
		}
	}
}
