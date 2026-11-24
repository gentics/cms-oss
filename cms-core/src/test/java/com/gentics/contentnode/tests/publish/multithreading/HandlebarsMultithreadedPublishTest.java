package com.gentics.contentnode.tests.publish.multithreading;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.AbstractPartType;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.object.parttype.HTMLTextPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLTextPartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.publish.PageDistributor;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for multithreaded publishing of pages containing handlebars tags, which resolve parts of different part types.
 * The publish worker threads share the static cache of resolvable methods in {@link AbstractPartType}, which must not fail.
 */
@GCNFeature(set = { Feature.MULTITHREADED_PUBLISHING })
public class HandlebarsMultithreadedPublishTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Number of publish worker threads
	 */
	public final static int THREADS = 4;

	/**
	 * Number of pages
	 */
	public final static int PAGES = 100;

	/**
	 * Number of publish runs (each starting with an empty cache)
	 */
	public final static int ROUNDS = 5;

	/**
	 * Name of the template tag
	 */
	public final static String TAG_NAME = "hbstag";

	/**
	 * Part keynames and part type classes of the text parts (all different classes), together with the text of the part
	 */
	public final static List<TextPartDefinition> TEXT_PARTS = List.of(
			new TextPartDefinition("shorttext", ShortTextPartType.class, "Short text"),
			new TextPartDefinition("normaltext", NormalTextPartType.class, "Normal text"),
			new TextPartDefinition("html", HTMLPartType.class, "HTML"),
			new TextPartDefinition("htmltext", HTMLTextPartType.class, "HTML text"),
			new TextPartDefinition("longhtml", LongHTMLPartType.class, "Long HTML"),
			new TextPartDefinition("longhtmltext", LongHTMLTextPartType.class, "Long HTML text"));

	/**
	 * Keyname of the handlebars part
	 */
	public final static String HBS_PART_NAME = "hb";

	/**
	 * Expected rendered content of every page
	 */
	public final static String EXPECTED_CONTENT = String.join("|", TEXT_PARTS.stream().map(TextPartDefinition::text).toList());

	private static Node node;

	private static List<Page> pages;

	/**
	 * Setup test data: a node with a template, which contains a handlebars tag that resolves the text of parts with different part types,
	 * and pages using the template
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
		prefs.setProperty(PageDistributor.THREAD_LIMIT_CONFIG_KEY, Integer.toString(THREADS));

		node = supply(() -> createNode("hbs.mt.publish", "Handlebars MT Publish", PublishTarget.FILESYSTEM));

		Construct construct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("hbs_mt_publish");
			c.setName("Handlebars MT Publish", 1);

			for (TextPartDefinition def : TEXT_PARTS) {
				c.getParts().add(create(Part.class, p -> {
					p.setPartTypeId(getPartTypeId(def.clazz()));
					p.setEditable(1);
					p.setHidden(false);
					p.setKeyname(def.keyname());
					p.setName(def.keyname(), 1);
				}).doNotSave().build());
			}

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(HBS_PART_NAME);
				p.setName(HBS_PART_NAME, 1);
			}).doNotSave().build());
		}).build();

		// resolve the parts in the order of the definitions, separated by |
		String hbsTemplate = String.join("|", TEXT_PARTS.stream().map(def -> "{{cms.tag.parts.%s.text}}".formatted(def.keyname())).toList());

		Template template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Handlebars MT Publish Template");
			// render only the handlebars part of the tag
			t.setSource("<node %s.parts.%s>".formatted(TAG_NAME, HBS_PART_NAME));

			t.getTemplateTags().put(TAG_NAME, create(TemplateTag.class, tag -> {
				tag.setConstructId(construct.getId());
				tag.setEnabled(true);
				tag.setName(TAG_NAME);
				tag.setPublic(false);
			}).doNotSave().build());
		}).unlock().build();

		template = update(template, t -> {
			TemplateTag tag = t.getTemplateTag(TAG_NAME);
			for (TextPartDefinition def : TEXT_PARTS) {
				getPartType(def.clazz(), tag, def.keyname()).setText(def.text());
			}
			getPartType(HandlebarsPartType.class, tag, HBS_PART_NAME).setText(hbsTemplate);
		}).unlock().build();
		Template finalTemplate = template;

		pages = new ArrayList<>();
		for (int i = 0; i < PAGES; i++) {
			int pageNumber = i;
			pages.add(supply(() -> {
				Transaction t = TransactionManager.getCurrentTransaction();
				Page page = createPage(node.getFolder(), finalTemplate, "Page %d".formatted(pageNumber));
				page = t.getObject(page, true);
				page.publish();
				return t.getObject(page);
			}));
		}
	}

	/**
	 * Test that publishing the pages with multiple threads, starting with an empty cache of resolvable methods, renders all pages correctly
	 * @throws Exception
	 */
	@Test
	public void testPublishWithEmptyCache() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			// start with cold caches, like after a server start
			clearResolvableMethodsCache();
			Introspector.flushCaches();

			// dirt all pages of the node (committed before publishing, so that the publish process does not wait for locks)
			try (Trx trx = new Trx()) {
				PublishQueue.dirtPublishedPages(new int[] { node.getId() }, null, 0, 0, PublishQueue.Action.DEPENDENCY);
				trx.success();
			}

			PublishInfo info = null;
			try (Trx trx = new Trx()) {
				info = testContext.getContext().publish(false);
				trx.success();
			}
			assertThat(info.getReturnCode()).as("Publish return code in round %d", round).isEqualTo(PublishInfo.RETURN_CODE_SUCCESS);

			Map<Integer, String> sources = getPublishedSources();
			for (Page page : pages) {
				assertThat(sources.get(page.getId())).as("Published content of %s in round %d", page, round).isEqualTo(EXPECTED_CONTENT);
			}
		}
	}

	/**
	 * Empty the static cache of resolvable methods in {@link AbstractPartType}
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected void clearResolvableMethodsCache() throws Exception {
		Field field = AbstractPartType.class.getDeclaredField("resolvableKeys");
		field.setAccessible(true);
		((Map<?, ?>) field.get(null)).clear();
	}

	/**
	 * Get the published sources of the pages in the node
	 * @return map of page ID to published source
	 * @throws NodeException
	 */
	protected Map<Integer, String> getPublishedSources() throws NodeException {
		return supply(() -> DBUtils.select("SELECT page_id, source FROM publish WHERE node_id = ? AND active = 1", ps -> {
			ps.setInt(1, node.getId());
		}, rs -> {
			Map<Integer, String> sources = new HashMap<>();
			while (rs.next()) {
				sources.put(rs.getInt("page_id"), rs.getString("source"));
			}
			return sources;
		}));
	}

	/**
	 * Definition of a text part
	 * @param keyname part keyname
	 * @param clazz part type class
	 * @param text text of the part
	 */
	public static record TextPartDefinition(String keyname, Class<? extends TextPartType> clazz, String text) {
	}
}
