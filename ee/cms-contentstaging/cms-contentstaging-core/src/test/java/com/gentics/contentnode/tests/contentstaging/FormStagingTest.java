package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier.GENERIC_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObject.GlobalId;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.Controls;
import com.gentics.contentnode.rest.model.formtype.PublishAsType;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.rest.model.response.StagingResponse;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FormResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.FilterFormParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PublishableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.publish.mesh.form.FormPublishTest;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for staging forms
 */
@GCNFeature(set = { Feature.CONTENT_STAGING, Feature.FORMS, Feature.MULTICHANNELLING, Feature.NICE_URLS, Feature.WASTEBIN })
@ConfigBy(FormStagingTest.class)
public class FormStagingTest extends AbstractVersionedContentStagingTest<Form> implements ConfigModifier {
	protected ObjectNode originalData;

	protected ObjectNode modifiedData;

	/**
	 * Create instance
	 * @throws IOException
	 */
	public FormStagingTest() throws IOException {
		super(Form.class, "form");
		this.originalData = mapper.readValue(FormPublishTest.class.getResource("generic_form_data.json"), ObjectNode.class);
		this.modifiedData = mapper.readValue(FormPublishTest.class.getResource("generic_form_data.json"), ObjectNode.class);
		this.modifiedData.withObjectProperty("schema").withObjectProperty("properties")
				.withObjectProperty("b0bac784_007f_4831_9098_987aa7828ff8").put("name", "Modifiziert");
	}

	@Override
	@Before
	public void setup() throws NodeException, IOException {
		super.setup();

		consume(n -> {
			FormFactory.addFormTypeToNode(GENERIC_TYPE, n.getId());
		}, node);
	}

	@Override
	@Test
	@Ignore("Test ignored, because forms cannot reference other forms")
	public void testImportReferencingEachOther() throws NodeException {
	}

	/**
	 * Test importing a form, which contains a reference to a page in the data
	 * @throws NodeException
	 * @throws JsonProcessingException
	 * @throws JsonMappingException
	 */
	@Test
	public void testImportReferenceInData() throws NodeException, JsonMappingException, JsonProcessingException {
		// create referenced page in deeper folder level (so that the page will be imported after the form)
		Folder baseFolder = create(Folder.class, f -> {
			f.setMotherId(nodeRootFolder.getId());
			f.setName("Base Folder");
			f.setPublishDir("/");
		}).build();
		int baseFolderId = execute(NodeObject::getId, baseFolder);
		GlobalId baseFolderGlobalId = execute(NodeObject::getGlobalId, baseFolder);

		Folder pageFolder = create(Folder.class, f -> {
			f.setMotherId(baseFolderId);
			f.setName("Page Folder");
			f.setPublishDir("/");
		}).build();
		int pageFolderId = execute(NodeObject::getId, pageFolder);

		Page referencedPage = create(Page.class, p -> {
			p.setFolderId(pageFolderId);
			p.setTemplateId(template.getId());
			p.setName("Referenced");
		}).build();
		GlobalId referencedPageGlobalId = execute(NodeObject::getGlobalId, referencedPage);
		int oldPageId = execute(NodeObject::getId, referencedPage);

		// create a form containing a string element that references the page
		ObjectNode originalReferencingData = mapper.createObjectNode();
		originalReferencingData
			.withObjectProperty("schema")
			.withObjectProperty("properties")
			.withObjectProperty("stringfield")
			.put("type", "string")
			.withObjectProperty("formGridOptions")
			.withObjectProperty("reference")
			.put("id", oldPageId)
			.put("type", "page");

		Form form = create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setFolderId(baseFolderId);
			f.setName("Staged form");
			f.setLanguages(Arrays.asList("en"));
			f.setData(originalReferencingData);
		}).build();
		GlobalId formGlobalId = execute(NodeObject::getGlobalId, form);

		// create package and add objects
		String packageName = "TestPackage";
		createContentPackage(packageName);
		assertResponseOK(new ContentStagingResourceImpl().add(packageName, "folder", baseFolderGlobalId.toString(), true));

		// delete base folder (with form and referenced page)
		consume(f -> f.delete(true), baseFolder);

		// import the package
		importContentPackage(packageName);

		// reload and assert
		form = supply(t -> t.getObject(clazz, formGlobalId));
		referencedPage = supply(t -> t.getObject(Page.class, referencedPageGlobalId));

		try (Trx trx = new Trx()) {
			assertThat(referencedPage).as("Staged referenced page").isNotNull();
			assertThat(form).as("Staged form").isNotNull();
			JsonNode data = form.getData();
			assertThat(data).as("Form data").isNotNull();
			assertThat(data.path("schema").path("properties").path("stringfield").path("formGridOptions")
					.path("reference").path("id").asInt()).as("Form data reference").isEqualTo(referencedPage.getId());
			trx.success();
		}
	}

	@Override
	@Test
	@Ignore("Test ignored, because there is no legacy method to get forms")
	public void testLegacyStagingInfo() throws NodeException {
	}

	@Override
	protected Form createObject(Folder folder, int timestamp) throws NodeException {
		return create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setFolderId(folder.getId());
			f.setName("Staged form");
			f.setDescription("This is the staged form");
			f.setLanguages(Arrays.asList("en"));
			f.setData(originalData);
		}).at(timestamp).unlock().build();
	}

	@Override
	protected Form updateObject(Form object, int timestamp) throws NodeException {
		return update(object, f -> {
			f.setName("Modified form @" + timestamp);
			f.setDescription("This is the modified form");
			f.setLanguages(Arrays.asList("en", "de"));
			f.setData(modifiedData);
		}).at(timestamp).unlock().build();
	}

	@Override
	protected Form moveObject(Form object, Folder targetFolder) throws NodeException {
		return update(object, f -> f.setFolder(null, targetFolder)).unlock().build();
	}

	@Override
	protected Form setReference(Form object, Form referenced) throws NodeException {
		fail("Setting references to other forms is not supported for forms");
		return null;
	}

	@Override
	protected void assertStagedObject(Form object, Folder parent) throws NodeException {
		assertThat(object).as("Staged form")
			.isNotNull()
			.hasFieldOrPropertyWithValue("name", "Staged form")
			.hasFieldOrPropertyWithValue("description", "This is the staged form")
			.hasFieldOrPropertyWithValue("languages", Arrays.asList("en"))
			.hasFieldOrPropertyWithValue("data", originalData)
			.hasParent(parent);
	}

	@Override
	protected Folder setForeignReference(Folder folder, Form object) throws NodeException {
		return update(folder, f -> {
			CmsFormPartType partType = getPartType(CmsFormPartType.class, f.getObjectTag("reference"), "form");
			partType.setTarget(object);
		}).build();
	}

	@Override
	protected void assertForeignReference(Folder folder, Form object) throws NodeException {
		assertThat(folder).as("Folder with reference").isNotNull();
		assertThat(object).as("Referenced form").isNotNull();
		assertThat(getPartType(CmsFormPartType.class, folder.getObjectTag("reference"), "form")).as("Reference part")
				.isNotNull().hasFieldOrPropertyWithValue("target", object);
	}

	@Override
	protected Form setPageReference(Form object, Page referencedPage) throws NodeException {
		return update(object, f -> {
			ObjectNode data = f.getData().deepCopy();
			data.put("successPageId", referencedPage.getId());
			data.put("successNodeId", referencedPage.getNode().getId());
			f.setData(data);
		}).unlock().build();
	}

	@Override
	protected void assertPageReference(Form object, Page referencedPage) throws NodeException {
		assertThat(object).as("Staged form").isNotNull();
		assertThat(object.getData()).as("Staged form data").isNotNull();
		assertThat(object.getData().path("successPageId").asInt()).as("Success Page ID")
				.isEqualTo(referencedPage != null ? referencedPage.getId() : 0);

		if (referencedPage != null) {
			assertThat(object.getData().path("successNodeId").asInt()).as("Success Page ID")
					.isEqualTo(referencedPage.getNode().getId());
		}
	}

	@Override
	protected void assertReference(Form object, Form referenced) throws NodeException {
		fail("Setting references to other forms is not supported for forms");
	}

	@Override
	protected StagingResponse<String> getStagingResponse(String packageName, Folder folder) throws NodeException {
		return new FormResourceImpl().list(
				new InFolderParameterBean()
					.setFolderId(execute(Folder::getGlobalId, folder).toString())
					.setStagingPackageName(packageName),
				new FilterFormParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new WastebinParameterBean(),
				new PublishableParameterBean());
	}

	@Override
	protected StagingResponse<String> getLegacyStagingResponse(String packageName, Folder folder) throws NodeException {
		fail("There is no legacy method for getting forms");
		return null;
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		// prepare forms config
		FormTypeConfiguration formsConfig = new FormTypeConfiguration(Map.of("de", "CMS Formular", "en", "CMS Form"),
				"forms");
		ControlConfig stringControl = new ControlConfig(Map.of("de", "Eingabefeld"));
		ReferenceSetting referenceSetting = new ReferenceSetting("reference", Map.of("de", "Referenz"),
				Collections.singleton(ReferenceType.page), PublishAsType.url);
		referenceSetting.backend = true;
		stringControl.settings = Arrays.asList(referenceSetting);
		formsConfig.controls = new Controls(Map.of("string", stringControl));

		formTypeConfiguration.put(GENERIC_TYPE, formsConfig);

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}
}
