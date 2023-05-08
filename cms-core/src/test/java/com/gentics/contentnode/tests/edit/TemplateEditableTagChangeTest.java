package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for changing template tags, that are editable in page
 */
@RunWith(value = Parameterized.class)
public class TemplateEditableTagChangeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected final static String TAGNAME = "testtag";

	protected static Node node;

	protected static int constructOneId, constructTwoId;

	@Parameter(0)
	public boolean syncPages;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: syncPages {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (Boolean sync : Arrays.asList(true, false)) {
			data.add(new Object[] { sync });
		}
		return data;
	}

	/**
	 * Setup common test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setup() throws Exception {
		try (Trx trx = new Trx()) {
			node = ContentNodeTestDataUtils.createNode();
			constructOneId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "constructone", "text");
			constructTwoId = ContentNodeTestDataUtils.createConstruct(node, ShortTextPartType.class, "constructtwo", "text");
			trx.success();
		}
	}

	/**
	 * Test page migration after construct of template tag (in page editable) is changed
	 * @throws Exception
	 */
	@Test
	public void testChangeTagConstruct() throws Exception {
		changeTemplate(template -> {
			template.getTemplateTag(TAGNAME).setConstructId(constructTwoId);
		}, page -> {
			ContentTag contentTag = page.getContentTag(TAGNAME);
			assertNotNull(TAGNAME + " not found in " + page, contentTag);
			assertEquals("Check constructID of " + contentTag, syncPages ? constructTwoId : constructOneId, contentTag.getConstructId().intValue());
		});
	}

	/**
	 * Test page migration after template tag (in page editable) is removed
	 * @throws Exception
	 */
	@Test
	public void testRemoveTag() throws Exception {
		changeTemplate(template -> {
			template.getTemplateTags().remove(TAGNAME);
		}, page -> {
			ContentTag contentTag = page.getContentTag(TAGNAME);
			if (syncPages) {
				assertNull(TAGNAME + " must be removed from " + page, contentTag);
			} else {
				assertNotNull(TAGNAME + " not found in " + page, contentTag);
			}
		});
	}

	/**
	 * Test page migration after template tag (in page editable) is changed to no longer be editable in page
	 * @throws Exception
	 */
	@Test
	public void testMakeTagNotEditable() throws Exception {
		changeTemplate(template -> {
			template.getTemplateTag(TAGNAME).setPublic(false);
		}, page -> {
			ContentTag contentTag = page.getContentTag(TAGNAME);
			if (syncPages) {
				assertNull(TAGNAME + " must be removed from " + page, contentTag);
			} else {
				assertNotNull(TAGNAME + " not found in " + page, contentTag);
			}
		});
	}

	/**
	 * Test migration of a published page after template was changed
	 * @throws Exception
	 */
	@Test
	public void testSyncPublishedPage() throws Exception {
		changeTemplate(Page::publish, template -> {
			template.getTemplateTag(TAGNAME).setConstructId(constructTwoId);
		}, page -> {
			ContentTag contentTag = page.getContentTag(TAGNAME);
			assertNotNull(TAGNAME + " not found in " + page, contentTag);
			assertEquals("Check constructID of " + contentTag, syncPages ? constructTwoId : constructOneId, contentTag.getConstructId().intValue());

			assertThat(page).as("Test page").isOnline();
		});
	}

	/**
	 * Variant of {@link #changeTemplate(Consumer, Consumer, Consumer)} without pageHandler
	 * @param changer implementation changing the template
	 * @param asserter implementation doing assertions on the pages
	 * @throws Exception
	 */
	protected void changeTemplate(Consumer<Template> changer, Consumer<Page> asserter) throws Exception {
		changeTemplate(null, changer, asserter);
	}

	/**
	 * Perform the test:
	 * <ol>
	 * <li>Create template with a tag (in page editable) using construct "one"</li>
	 * <li>Create pages using the template</li>
	 * <li>Optionally handle the pages with the <code>pageHandler</code></li>
	 * <li>Assert that the pages have the contenttags from the template</li>
	 * <li>Let <code>changer</code> change the template</li>
	 * <li>Save the template (optionally syncing the pages)</li>
	 * <li>Let <code>asserter</code> do assertions on the pages</li>
	 * </ol>
	 * @param pageHandler optional page handler implementation
	 * @param changer implementation changing the template
	 * @param asserter implementation doing assertions on the pages
	 * @throws Exception
	 */
	protected void changeTemplate(Consumer<Page> pageHandler, Consumer<Template> changer, Consumer<Page> asserter) throws Exception {
		Template template;
		Set<Page> testPages = new HashSet<>();

		// create test data
		try (Trx trx = new Trx()) {
			template = ContentNodeTestDataUtils.createTemplate(node.getFolder(), "<node " + TAGNAME + ">", "Template",
					ContentNodeTestDataUtils.createTemplateTag(constructOneId, TAGNAME, true, false));

			for (int i = 1; i <= 3; i++) {
				testPages.add(ContentNodeTestDataUtils.createPage(node.getFolder(), template, "Testpage " + i));
			}

			if (pageHandler != null) {
				for (Page page : testPages) {
					Page editable = trx.getTransaction().getObject(page, true);
					pageHandler.accept(editable);
					editable.save();
				}
			}

			trx.success();
		}

		// assert tags
		try (Trx trx = new Trx()) {
			for (Page page : testPages) {
				ContentTag contentTag = page.getContentTag(TAGNAME);
				assertNotNull("Tag " + TAGNAME + " not found in " + page, contentTag);
				assertEquals("Check tag name" , TAGNAME, contentTag.getName());
				assertTrue(contentTag + " must come from the template", contentTag.comesFromTemplate());
				assertEquals("Check construct of " + contentTag, constructOneId, contentTag.getConstructId().intValue());
			}
			trx.success();
		}

		// change template
		try (Trx trx = new Trx()) {
			Template editableTemplate = trx.getTransaction().getObject(template, true);
			changer.accept(editableTemplate);
			editableTemplate.save(syncPages);
			trx.success();
		}

		// assert changed pages
		try (Trx trx = new Trx()) {
			for (Page page : testPages) {
				asserter.accept(trx.getTransaction().getObject(page));
			}
			trx.success();
		}
	}

	/**
	 * Consumer interface that may throw a NodeException
	 *
	 * @param <T> consumed object
	 */
	@FunctionalInterface
	protected static interface Consumer<T> {
		void accept(T t) throws NodeException;
	}
}
