package com.gentics.contentnode.tests.publish.mesh.form;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishFS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.MarkupLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.publish.PublishQueue.Action;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.ConfigBy;

/**
 * Test cases for pages, which are forms e-mail templates, and are therefore excluded from publishing
 */
@GCNFeature(set = { Feature.FORMS, Feature.MCCR, Feature.MULTICHANNELLING })
@ConfigBy(FormTypeConfigurationModifier.class)
public class FormEmailTemplatePageTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();

	private static Node node;

	private static ContentRepository cr;

	private static Template eMailTemplate;

	private static Template normalTemplate;

	@BeforeClass
	public static void setupOnce() throws Exception {
		context.getContext().getTransaction().commit();

		MarkupLanguage formsTemplateMarkupLanguage = supply(t -> t.getObject(MarkupLanguage.class, DBUtils.select("SELECT id FROM ml WHERE feature = ?",
				ps -> ps.setString(1, "forms"), DBUtils.firstInt("id"))));
		assertThat(formsTemplateMarkupLanguage).as("Forms e-mail template ml").isNotNull();

		node = supply(() -> createNode("host", "Test Node", PublishTarget.BOTH, true, false));
		cr = supply(() -> node.getContentRepository());
		eMailTemplate = create(Template.class, create -> {
			create.setSource("");
			create.setName("E-Mail Template");
			create.addFolder(node.getFolder());
			create.setMlId(formsTemplateMarkupLanguage.getId());
		}).build();

		normalTemplate = create(Template.class, create -> {
			create.setSource("");
			create.setName("Normal Template");
			create.addFolder(node.getFolder());
		}).build();

		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}
	}

	/**
	 * Clear all test data from the node
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));

		cr = update(cr, update -> {
			update.setInstantPublishing(false);
		}).build();
	}

	/**
	 * Test that excluded pages are not instant published
	 * @throws NodeException
	 */
	@Test
	public void testInstantPublishing() throws NodeException {
		cr = update(cr, update -> {
			update.setInstantPublishing(true);
		}).build();

		Page eMailPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			create.setName("E-Mail Template Page");
		}).publish().build();

		Page normalPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(normalTemplate.getId());
			create.setName("Normal Page");
		}).publish().build();

		operate(() -> {
			assertPublishCR(normalPage, node, true);
			assertPublishCR(eMailPage, node, false);
		});
	}

	/**
	 * Test that excluded pages are not published by the publish process
	 * @throws Exception 
	 */
	@Test
	public void testPublishProcess() throws Exception {
		Page eMailPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			create.setName("E-Mail Template Page");
		}).publish().build();

		Page normalPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(normalTemplate.getId());
			create.setName("Normal Page");
		}).publish().build();

		// nothing has been published yet
		operate(() -> {
			assertPublishCR(normalPage, node, false);
			assertPublishCR(eMailPage, node, false);
			assertPublishFS(context.getPubDir(), normalPage, node, false);
			assertPublishFS(context.getPubDir(), eMailPage, node, false);
		});

		// run the publish process
		try (Trx trx = new Trx()) {
			context.publish(false);
			trx.success();
		}

		operate(() -> {
			assertPublishCR(normalPage, node, true);
			assertPublishCR(eMailPage, node, false);
			assertPublishFS(context.getPubDir(), normalPage, node, true);
			assertPublishFS(context.getPubDir(), eMailPage, node, false);
		});
	}

	/**
	 * Test dirting pages
	 * @throws Exception
	 */
	@Test
	public void testDirting() throws Exception {
		Page eMailPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(eMailTemplate.getId());
			create.setName("E-Mail Template Page");
		}).publish().build();

		Page normalPage = create(Page.class, create -> {
			create.setFolderId(node.getFolder().getId());
			create.setTemplateId(normalTemplate.getId());
			create.setName("Normal Page");
		}).publish().build();

		// undirt all pages
		operate(() -> PublishQueue.undirtObjects(null, Page.TYPE_PAGE, null, -1, -1));
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node))).as("Dirted page IDs").isEmpty();

		// dirt "all" pages
		operate(() -> PublishQueue.dirtPublishedPages(null, null, -1, -1, Action.DEPENDENCY));
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node))).as("Dirted page IDs").containsOnly(normalPage.getId());

		// undirt all pages
		operate(() -> PublishQueue.undirtObjects(null, Page.TYPE_PAGE, null, -1, -1));
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node))).as("Dirted page IDs").isEmpty();

		// dirt normal page directly
		assertThat(supply(() -> PublishQueue.dirtObject(normalPage, Action.DEPENDENCY, 0))).as("publish queue entries").isNotEmpty();
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node))).as("Dirted page IDs").containsOnly(normalPage.getId());

		// dirt email page directly
		assertThat(supply(() -> PublishQueue.dirtObject(eMailPage, Action.DEPENDENCY, 0))).as("publish queue entries").isEmpty();
		assertThat(supply(() -> PublishQueue.getDirtedObjectIds(Page.class, false, node))).as("Dirted page IDs").containsOnly(normalPage.getId());
	}
}
