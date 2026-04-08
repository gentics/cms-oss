package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.assertObject;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.TEMPLATE_PARTNAME;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.FormRESTUtils.getFormResource;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.rest.model.ContentRepositoryModel.Status;
import com.gentics.contentnode.rest.model.form.FormModel;
import com.gentics.contentnode.rest.model.response.ContentRepositoryResponse;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for forms which reference a page as mailsource_pageid
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.DISABLE_INSTANT_DELETE })
@RunWith(Parameterized.class)
@Category(MeshTest.class)
public class FormReferencingPagePublishTest extends AbstractFormPublishTest {
	/**
	 * Test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: en published {0}, en modified {1}, de published {2}, de modified {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean enPublished : Arrays.asList(true, false)) {
			for (boolean enModified : Arrays.asList(true, false)) {
				for (boolean dePublished : Arrays.asList(true, false)) {
					for (boolean deModified : Arrays.asList(true, false)) {
						if ((!enPublished && enModified) || (!dePublished && deModified)) {
							continue;
						}
						data.add(new Object[] { enPublished, enModified, dePublished, deModified });
					}
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean enPublished;

	@Parameter(1)
	public boolean enModified;

	@Parameter(2)
	public boolean dePublished;

	@Parameter(3)
	public boolean deModified;

	protected Page emailPage;

	@Override
	@Before
	public void setup() throws NodeException {
		super.setup();

		// create english target page
		Page enTargetPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(template.getId());
			create.setFilename("target.en.html");
			create.setLanguage(getLanguage("en"));
		}).publish().build();

		// create german translation
		Page deTargetPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(template.getId());
			create.setFilename("ziel.de.html");
			create.setLanguage(getLanguage("de"));
			create.setContentsetId(enTargetPage.getContentsetId());
		}).publish().build();

		String enVtlTemplate = String.format("This is the e-Mail Template Page. Target Page: $cms.imps.loader.getPage(%d).url, Language: $cms.page.language.code", enTargetPage.getId());
		String deVtlTemplate = String.format("Das ist die e-Mail Vorlagen Seite. Zielseite: $cms.imps.loader.getPage(%d).url, Sprache: $cms.page.language.code", deTargetPage.getId());
		emailPage = getEmailPage(enVtlTemplate, deVtlTemplate);
	}

	/**
	 * Test publishing the form
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Form form = create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());

			try {
				ObjectNode data = mapper.readValue(getClass().getResource("generic_form_data.json"), ObjectNode.class);
				data.put("mailsource_pageid", emailPage.getId());
				f.setData(data);
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}).publish().build();

		String downloadBaseUrl = String.format("http://cms.domain.test/editor/#proxy/rest/form/%d/data", form.getId());
		Map<String, ObjectNode> expected = new HashMap<>();
		expected.put("de", mapper.readValue(getClass().getResource("form_published_de.json"), ObjectNode.class).put(FIELD_DOWNLOAD_BASE_URL, String.format(FMT_DOWNLOAD_BASE_URL, form.getId())));
		expected.put("en", mapper.readValue(getClass().getResource("form_published_en.json"), ObjectNode.class).put(FIELD_DOWNLOAD_BASE_URL, String.format(FMT_DOWNLOAD_BASE_URL, form.getId())));
		expected.get("de").put("cms_id", form.getId());
		expected.get("en").put("cms_id", form.getId());
		expected.get("de").put("downloadBaseUrl", downloadBaseUrl);
		expected.get("en").put("downloadBaseUrl", downloadBaseUrl);
		expected.get("de").put("mailsource", getExpectedMailsource("de"));
		expected.get("en").put("mailsource", getExpectedMailsource("en"));

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		for (String language : Arrays.asList("de", "en")) {
			operate(() -> assertObject("", mesh.client(), MeshPublisher.getMeshProjectName(node), null, form, language,
					true, response -> {
						assertThat(response.getSchema().getName()).as("Schema").isEqualTo("form");
						assertThat(response.getFields().getStringField("name").getString()).isEqualTo("Test Form");
						assertThat(response.getFields().getBooleanField("online").getValue()).isTrue();
						assertThat(response.getAvailableLanguages()).containsKey(language);
						assertThat(response.getAvailableLanguages().get(language).isPublished()).isEqualTo(true);
						try {
							JsonNode actual = mapper.readValue(response.getFields().getStringField("json").getString(), ObjectNode.class);
							assertThat(mapper.convertValue(actual, Map.class))
								.as("Published JSON")
								.isEqualTo(mapper.convertValue(expected.get(language), Map.class));
						} catch (IOException e) {
							throw new NodeException(e);
						}
					}));
		}

		operate(t -> {
			ContentRepository cr = t.getObject(ContentRepository.class, crId);

			try (MeshPublisher mp = new MeshPublisher(cr)) {
				StringBuilder result = new StringBuilder();
				boolean crIsClean = mp.checkDataConsistency(false, result);
				assertThat(crIsClean).as("Data check result: " + result.toString()).isTrue();
			}
		});
	}

	/**
	 * Test rendering the preview
	 * @throws Exception
	 */
	@Test
	public void testRenderPreview() throws Exception {
		ContentRepositoryResponse response = crResource.repair(Integer.toString(crId), 0);
		ContentNodeRESTUtils.assertResponseOK(response);
		assertThat(response.getContentRepository().getCheckStatus()).as("Check result: " + response.getContentRepository().getCheckResult()).isEqualTo(Status.ok);

		Form form = create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de", "en"));
			f.setFolderId(node.getFolder().getId());

			try {
				ObjectNode data = mapper.readValue(getClass().getResource("generic_form_data.json"), ObjectNode.class);
				data.put("mailsource_pageid", emailPage.getId());
				f.setData(data);
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}).build();
		String formId = Integer.toString(form.getId());
		String meshUuid = execute(f -> MeshPublisher.getMeshUuid(f), form);

		FormModel previewModel = new FormModel();
		ObjectNode previewData = mapper.readValue(getClass().getResource("form_preview_data.json"), ObjectNode.class);
		previewData.put("mailsource_pageid", emailPage.getId());
		previewModel.setData(previewData);

		for (String language : Arrays.asList("de", "en")) {
			StringWriter writer = new StringWriter();
			IOUtils.copy(getClass().getResourceAsStream(String.format("form_preview_%s.html", language)), writer);
			String expectedMailsource = getExpectedMailsource(language);
			String expected = String.format(writer.toString(), meshUuid, expectedMailsource);
			String renderedForm = supply(() -> getFormResource().preview(formId, language, null, previewModel));
			assertThat(renderedForm).as(String.format("Rendered form in %s", language)).isEqualTo(expected);
		}
	}

	/**
	 * Get the expected mailsource for the form in the given language
	 * @param language form language
	 * @return expected mailsource
	 * @throws IOException
	 */
	protected String getExpectedMailsource(String language) throws IOException {
		String resourceName = null;
		switch (language) {
		case "de":
			if (dePublished) {
				resourceName = "form_mailsource_de.html";
			} else if (enPublished) {
				resourceName = "form_mailsource_en.html";
			}
			break;
		case "en":
			if (enPublished) {
				resourceName = "form_mailsource_en.html";
			} else if (dePublished) {
				resourceName = "form_mailsource_de.html";
			}
			break;
		default:
			break;
		}

		if (resourceName == null) {
			return "";
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(getClass().getResourceAsStream(resourceName), writer);
		return writer.toString();
	}

	/**
	 * Create language variants for the email page
	 * @param enVtlTemplate vtl template for the english language variant
	 * @param deVtlTemplate vtl template for the german language variant
	 * @return english page
	 * @throws NodeException
	 */
	protected Page getEmailPage(String enVtlTemplate, String deVtlTemplate) throws NodeException {
		// create english page
		Builder<Page> builder = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(LongHTMLPartType.class, create.getContentTag("content"), TEMPLATE_PARTNAME).setText(enVtlTemplate);
			create.setLanguage(getLanguage("en"));
		}).at(1);

		// optionally published
		if (enPublished) {
			builder = builder.publish();
		}

		Page enPage = builder.build();

		// optionally modified
		if (enModified) {
			enPage = update(enPage, update -> {
				getPartType(LongHTMLPartType.class, update.getContentTag("content"), TEMPLATE_PARTNAME).setText("modified " + enVtlTemplate);
			}).at(2).build();
		}

		int contentSetId = enPage.getContentsetId();

		// create german language variant
		builder = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(LongHTMLPartType.class, create.getContentTag("content"), TEMPLATE_PARTNAME).setText(deVtlTemplate);
			create.setLanguage(getLanguage("de"));
			create.setContentsetId(contentSetId);
		}).at(1);

		// optionally published
		if (dePublished) {
			builder = builder.publish();
		}

		Page dePage = builder.build();

		// optionally modified
		if (deModified) {
			dePage = update(dePage, update -> {
				getPartType(LongHTMLPartType.class, update.getContentTag("content"), TEMPLATE_PARTNAME).setText("modifiziert " + deVtlTemplate);
			}).at(2).build();
		}

		return enPage;
	}
}
