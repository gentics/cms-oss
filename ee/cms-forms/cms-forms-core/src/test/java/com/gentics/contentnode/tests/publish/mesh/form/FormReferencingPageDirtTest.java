package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.configuration.FormTypeConfiguration;
import com.gentics.contentnode.configuration.FormTypeConfigurationMap;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.FormConfigProvider;
import com.gentics.contentnode.etc.MapPreferences;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FormFactory;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.rest.model.formtype.ControlConfig;
import com.gentics.contentnode.rest.model.formtype.Controls;
import com.gentics.contentnode.rest.model.formtype.PublishAsType;
import com.gentics.contentnode.rest.model.formtype.ReferenceSetting;
import com.gentics.contentnode.rest.model.formtype.ReferenceType;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.ConfigModifier;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for dirting forms that reference a page
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.DISABLE_INSTANT_DELETE })
@Category(MeshTest.class)
@ConfigBy(FormReferencingPageDirtTest.class)
public class FormReferencingPageDirtTest extends AbstractFormPublishTest implements ConfigModifier {
	protected final static String FORM_TYPE = "generic";

	/**
	 * Referenced page (english variant)
	 */
	protected Page refPageEn;

	/**
	 * Referenced page (german variant)
	 */
	protected Page refPageDe;

	/**
	 * Other referenced page (english variant)
	 */
	protected Page otherRefPageEn;

	/**
	 * Other referenced page (german variant)
	 */
	protected Page otherRefPageDe;

	/**
	 * Form (german only) using the referenced page
	 */
	protected Form formDe;

	@Override
	@Before
	public void setup() throws NodeException {
		super.setup();

		consume(n -> {
			FormFactory.addFormTypeToNode(FORM_TYPE, n.getId());
		}, node);

		refPageEn = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(HandlebarsPartType.class, create.getContentTag("content"), HBS_PART_NAME).setText("");
			create.setLanguage(getLanguage("en"));
		}).at(1).publish().build();

		refPageDe = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(HandlebarsPartType.class, create.getContentTag("content"), HBS_PART_NAME).setText("");
			create.setLanguage(getLanguage("de"));
			create.setContentsetId(refPageEn.getContentsetId());
		}).at(1).publish().build();

		otherRefPageEn = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(HandlebarsPartType.class, create.getContentTag("content"), HBS_PART_NAME).setText("");
			create.setLanguage(getLanguage("en"));
		}).at(1).publish().build();

		otherRefPageDe = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(HandlebarsPartType.class, create.getContentTag("content"), HBS_PART_NAME).setText("");
			create.setLanguage(getLanguage("de"));
			create.setContentsetId(otherRefPageEn.getContentsetId());
		}).at(1).publish().build();

		formDe = create(Form.class, f -> {
			f.setFormType(FORM_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de"));
			f.setFolderId(node.getFolder().getId());

			try {
				ObjectNode data = mapper.readValue(getClass().getResource("generic_form_data.json"), ObjectNode.class);
				data.put("adminEmailPageId", refPageEn.getId());
				addUiSchemaElement(data, "control")
					.withObjectProperty("contentPage")
					.put("id", otherRefPageEn.getId())
					.put("nodeId", node.getId())
					.put("type", "page");
				f.setData(data);
			} catch (IOException e) {
				throw new NodeException(e);
			}
		}).publish().build();

		// run publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		} catch (Exception e) {
			throw new NodeException(e);
		}
	}

	/**
	 * Test that modifying the used language variant of the referenced page dirts the form
	 * @throws NodeException
	 */
	@Test
	public void testModifyRefPage() throws NodeException {
		refPageDe = update(refPageDe, update -> {
			getPartType(HandlebarsPartType.class, update.getContentTag("content"), HBS_PART_NAME).setText("modified");
		}).at(2).publish().build();

		operate(() -> context.waitForDirtqueueWorker());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Form.class, false, node))).as("Dirted form IDs").contains(formDe.getId());
	}

	/**
	 * Test that modifying the used language variant of the other referenced page dirts the form
	 * @throws NodeException
	 */
	@Test
	public void testModifyOtherRefPage() throws NodeException {
		otherRefPageDe = update(otherRefPageDe, update -> {
			getPartType(HandlebarsPartType.class, update.getContentTag("content"), HBS_PART_NAME).setText("modified");
		}).at(2).publish().build();

		operate(() -> context.waitForDirtqueueWorker());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Form.class, false, node))).as("Dirted form IDs").contains(formDe.getId());
	}

	/**
	 * Test that modifying the unused language variant of the referenced page does not dirt the form
	 * @throws NodeException
	 */
	@Test
	public void testModifyRefPageUnusedLanguage() throws NodeException {
		refPageEn = update(refPageEn, update -> {
			getPartType(HandlebarsPartType.class, update.getContentTag("content"), HBS_PART_NAME).setText("modified");
		}).at(2).publish().build();

		operate(() -> context.waitForDirtqueueWorker());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Form.class, false, node))).as("Dirted form IDs").doesNotContain(formDe.getId());
	}

	/**
	 * Add an element of the given type to the ui-schema
	 * @param data data
	 * @param elementType element type
	 * @return added element
	 */
	protected ObjectNode addUiSchemaElement(ObjectNode data, String elementType) {
		ArrayNode pages = data.withObjectProperty("ui-schema").withArrayProperty("pages");
		ObjectNode page = pages.addObject();
		ObjectNode element = page.withArrayProperty("elements").addObject();
		return element.withObjectProperty("formGridOptions")
			.put("type", elementType);
	}

	@Override
	public void accept(MapPreferences prefs) throws NodeException {
		FormTypeConfigurationMap formTypeConfiguration = new FormTypeConfigurationMap();

		FormTypeConfiguration formsConfig = new FormTypeConfiguration(Map.of("en", "Testing Type"), "forms");

		ControlConfig control = new ControlConfig(Map.of("de", "Kontrollelement"));

		ReferenceSetting pageContentReference = new ReferenceSetting("contentPage", Map.of(),
				Set.of(ReferenceType.page), PublishAsType.content);

		control.settings = List.of(pageContentReference);

		formsConfig.controls = new Controls(Map.of("control", control));

		formTypeConfiguration.put(FORM_TYPE, formsConfig);

		prefs.set(FormConfigProvider.TYPES_CONFIG_PATH, formTypeConfiguration);
	}
}
