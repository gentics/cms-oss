package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.TEMPLATE_PARTNAME;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.category.MeshTest;
import com.gentics.contentnode.tests.utils.FormRESTUtils;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for dirting forms that reference a page
 */
@GCNFeature(set = { Feature.FORMS, Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING, Feature.WASTEBIN, Feature.DISABLE_INSTANT_DELETE })
@Category(MeshTest.class)
public class FormReferencingPageDirtTest extends AbstractFormPublishTest {
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
		refPageEn = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(LongHTMLPartType.class, create.getContentTag("content"), TEMPLATE_PARTNAME).setText("");
			create.setLanguage(getLanguage("en"));
		}).at(1).publish().build();

		refPageDe = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(LongHTMLPartType.class, create.getContentTag("content"), TEMPLATE_PARTNAME).setText("");
			create.setLanguage(getLanguage("de"));
			create.setContentsetId(refPageEn.getContentsetId());
		}).at(1).publish().build();

		otherRefPageEn = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(LongHTMLPartType.class, create.getContentTag("content"), TEMPLATE_PARTNAME).setText("");
			create.setLanguage(getLanguage("en"));
		}).at(1).publish().build();

		otherRefPageDe = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			getPartType(LongHTMLPartType.class, create.getContentTag("content"), TEMPLATE_PARTNAME).setText("");
			create.setLanguage(getLanguage("de"));
			create.setContentsetId(otherRefPageEn.getContentsetId());
		}).at(1).publish().build();

		formDe = create(Form.class, f -> {
			f.setFormType(FormTypeConfigurationModifier.GENERIC_TYPE);
			f.setName("Test Form");
			f.setLanguages(Arrays.asList("de"));
			f.setFolderId(node.getFolder().getId());

			try {
				ObjectNode data = mapper.readValue(getClass().getResource("form_data.json"), ObjectNode.class);
				data.put("mailsource_pageid", refPageEn.getId());
				ObjectNode element = (ObjectNode)data.get("elements").get(0);
				element.put("test_pageid", otherRefPageEn.getId());
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
			getPartType(LongHTMLPartType.class, update.getContentTag("content"), TEMPLATE_PARTNAME).setText("modified");
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
			getPartType(LongHTMLPartType.class, update.getContentTag("content"), TEMPLATE_PARTNAME).setText("modified");
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
			getPartType(LongHTMLPartType.class, update.getContentTag("content"), TEMPLATE_PARTNAME).setText("modified");
		}).at(2).publish().build();

		operate(() -> context.waitForDirtqueueWorker());
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Form.class, false, node))).as("Dirted form IDs").doesNotContain(formDe.getId());
	}
}
