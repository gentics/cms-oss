package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.assertResultMessage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.OpResult.Status;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for copying or moving pages with languages between nodes with different languages
 */
public class CrossNodePageCopyAndMoveTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static ContentLanguage german;
	private static ContentLanguage english;
	private static Node germanNode;
	private static Node englishNode;
	private static Node germanEnglishNode;
	private static Node noLanguageNode;
	private static Node otherNoLanguageNode;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		german = supply(() -> getLanguage("de"));
		english = supply(() -> getLanguage("en"));
		germanNode = supply(() -> createNode("german.only", "Node with german only", PublishTarget.NONE, german));
		englishNode = supply(() -> createNode("english.only", "Node with english only", PublishTarget.NONE, english));
		germanEnglishNode = supply(() -> createNode("german.english", "Node with german and english", PublishTarget.NONE, german, english));
		noLanguageNode = supply(() -> createNode("no.language", "Node without languages", PublishTarget.NONE));
		otherNoLanguageNode = supply(() -> createNode("other.no.language", "Another Node without languages", PublishTarget.NONE));

		template = supply(() -> createTemplate(germanNode.getFolder(), "Template"));
	}

	/**
	 * Clear the nodes
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		operate(() -> {
			for (Node n : Arrays.asList(germanNode, englishNode, germanEnglishNode, noLanguageNode, otherNoLanguageNode)) {
				clear(n);
			}
		});
	}

	@Test
	public void testCopyGermanToGermanAndEnglish() throws NodeException {
		new TestCase()
			.createPage(germanNode, german)
			.copyTo(germanEnglishNode)
			.expectSuccess(german)
			.run();
	}

	@Test
	public void testCopyGermanToEnglish() throws NodeException {
		new TestCase()
			.createPage(germanNode, german)
			.copyTo(englishNode)
			.expectFailure("page_copy.missing_language", "Deutsch")
			.run();
	}

	@Test
	public void testCopyGermanEnglishToGerman() throws NodeException {
		new TestCase()
			.createPage(germanEnglishNode, german, english)
			.copyTo(germanNode)
			.expectFailure("page_copy.missing_language", "English")
			.run();
	}

	@Test
	public void testCopyGermanEnglishToNoLanguage() throws NodeException {
		new TestCase()
			.createPage(germanEnglishNode, german, english)
			.copyTo(noLanguageNode)
			.expectFailure("page_copy.missing_languages", "Deutsch, English")
			.run();
	}

	@Test
	public void testCopyGermanToNoLanguage() throws NodeException {
		new TestCase()
			.createPage(germanNode, german)
			.copyTo(noLanguageNode)
			.expectSuccess()
			.run();
	}

	@Test
	public void testCopyNoLanguageToGermanEnglish() throws NodeException {
		new TestCase()
			.createPage(noLanguageNode)
			.copyTo(germanEnglishNode)
			.expectSuccess(german)
			.run();
	}

	@Test
	public void testCopyNoLanguageToOtherNoLanguage() throws NodeException {
		new TestCase()
			.createPage(noLanguageNode)
			.copyTo(otherNoLanguageNode)
			.expectSuccess()
			.run();
	}

	@Test
	public void testMoveGermanToGermanAndEnglish() throws NodeException {
		new TestCase()
			.createPage(germanNode, german)
			.moveTo(germanEnglishNode)
			.expectSuccess(german)
			.run();
	}

	@Test
	public void testMoveGermanToEnglish() throws NodeException {
		new TestCase()
			.createPage(germanNode, german)
			.moveTo(englishNode)
			.expectFailure("page.move.missing_language", "Deutsch")
			.run();
	}

	@Test
	public void testMoveGermanEnglishToGerman() throws NodeException {
		new TestCase()
			.createPage(germanEnglishNode, german, english)
			.moveTo(germanNode)
			.expectFailure("page.move.missing_language", "English")
			.run();
	}

	@Test
	public void testMoveGermanEnglishToNoLanguage() throws NodeException {
		new TestCase()
			.createPage(germanEnglishNode, german, english)
			.moveTo(noLanguageNode)
			.expectFailure("page.move.missing_languages", "Deutsch, English")
			.run();
	}

	@Test
	public void testMoveGermanToNoLanguage() throws NodeException {
		new TestCase()
			.createPage(germanNode, german)
			.moveTo(noLanguageNode)
			.expectSuccess()
			.run();
	}

	@Test
	public void testMoveNoLanguageToGermanEnglish() throws NodeException {
		new TestCase()
			.createPage(noLanguageNode)
			.moveTo(germanEnglishNode)
			.expectSuccess(german)
			.run();
	}

	@Test
	public void testMoveNoLanugageToOtherNoLanguage() throws NodeException {
		new TestCase()
			.createPage(noLanguageNode)
			.moveTo(otherNoLanguageNode)
			.expectSuccess()
			.run();
	}

	/**
	 * Implementation of the test cases
	 */
	protected static class TestCase {
		/**
		 * Source node (where to create the page)
		 */
		protected Node sourceNode;

		/**
		 * Languages for creating the page (empty for creating a page without language)
		 */
		protected ContentLanguage[] sourceLanguages;

		/**
		 * True to copy
		 */
		protected boolean copy;

		/**
		 * True to move
		 */
		protected boolean move;

		/**
		 * Target node of the copy/move operation
		 */
		protected Node targetNode;

		/**
		 * True to expect the operation to succeed, false to expect it to fail
		 */
		protected boolean expectSuccess;

		/**
		 * Expected failure message
		 */
		protected String expectedMessage;

		/**
		 * Expected message parameters
		 */
		protected String[] expectedMessageParameters;

		/**
		 * Expected languages of the target pages (empty for expecting the target page to have no language)
		 */
		protected ContentLanguage[] expectedLanguages;

		/**
		 * Let the test case create a page in the node with the given languages
		 * @param node source node
		 * @param languages languages of the page to create
		 * @return fluent API
		 * @throws NodeException
		 */
		public TestCase createPage(Node node, ContentLanguage...languages) throws NodeException {
			this.sourceNode = node;
			this.sourceLanguages = languages;
			return this;
		}

		/**
		 * Let the test case copy the created page to the given node
		 * @param node target node
		 * @return fluent API
		 * @throws NodeException
		 */
		public TestCase copyTo(Node node) throws NodeException {
			copy = true;
			this.targetNode = node;
			return this;
		}

		/**
		 * Let the test case move the created page to the given node
		 * @param node target node
		 * @return fluent API
		 * @throws NodeException
		 */
		public TestCase moveTo(Node node) throws NodeException {
			move = true;
			this.targetNode = node;
			return this;
		}

		/**
		 * Let the test case expect the operation to succeed and expect the target page to have the given languages
		 * @param languages expected languages of the target page
		 * @return fluent API
		 * @throws NodeException
		 */
		public TestCase expectSuccess(ContentLanguage...languages) throws NodeException {
			expectSuccess = true;
			this.expectedLanguages = languages;
			return this;
		}

		/**
		 * Let the test case expect the operation to fail
		 * @param message expected failure message
		 * @param parameters parameters of the expected failure message
		 * @return fluent API
		 * @throws NodeException
		 */
		public TestCase expectFailure(String message, String...parameters) throws NodeException {
			expectSuccess = false;
			expectedMessage = message;
			expectedMessageParameters = parameters;
			return this;
		}

		/**
		 * Run the test case
		 * @throws NodeException
		 */
		public void run() throws NodeException {
			if (copy) {
				Page sourcePage = create();
				PageCopyOpResult result = execute(p -> p.copyTo(0, targetNode.getFolder(), false, null, null), sourcePage);
				if (expectSuccess) {
					assertThat(result).as("Page copy result").hasFieldOrPropertyWithValue("status", Status.OK);
					operate(() -> {
						assertThat(targetNode.getFolder().getPagesCount()).as("Number of pages in target node").isEqualTo(1);
						Page targetPage = targetNode.getFolder().getPages().get(0);
						assertThat(targetPage).as("Copied page").hasLanguageVariants(expectedLanguages);
					});
				} else {
					operate(() -> {
						assertThat(result).as("Page copy result").hasFieldOrPropertyWithValue("status", Status.FAILURE);
						assertResultMessage(result, expectedMessage, expectedMessageParameters);
						assertThat(targetNode.getFolder().getPagesCount()).as("Number of pages in target node")
							.isEqualTo(0);
					});
				}
			} else if (move) {
				Page sourcePage = create();
				OpResult result = execute(p -> p.move(targetNode.getFolder(), 0, true), sourcePage);
				if (expectSuccess) {
					assertThat(result).as("Page move result").hasFieldOrPropertyWithValue("status", Status.OK);
					operate(() -> {
						assertThat(targetNode.getFolder().getPagesCount()).as("Number of pages in target node").isEqualTo(1);
						Page targetPage = targetNode.getFolder().getPages().get(0);
						assertThat(targetPage).as("Moved page").hasLanguageVariants(expectedLanguages);
					});
				} else {
					operate(() -> {
						assertThat(result).as("Page move result").hasFieldOrPropertyWithValue("status", Status.FAILURE);
						assertResultMessage(result, expectedMessage, expectedMessageParameters);
						assertThat(targetNode.getFolder().getPagesCount()).as("Number of pages in target node")
							.isEqualTo(0);
					});
				}
			}
		}

		/**
		 * Create the test page according to the settings
		 * @return test page
		 * @throws NodeException
		 */
		protected Page create() throws NodeException {
			operate(() -> {
				// if languages are given, they all need to be activated in the node
				List<ContentLanguage> nodeLanguages = sourceNode.getLanguages();
				assertThat(nodeLanguages).as("Languages in node").contains(sourceLanguages);

				// if the node has languages, we cannot create a page without languages
				if (!nodeLanguages.isEmpty()) {
					assertThat(sourceLanguages).as("Requested languages for page in node").isNotEmpty();
				}
			});

			if (sourceLanguages.length == 0) {
				return Builder.create(Page.class, p -> {
					p.setTemplateId(template.getId());
					p.setFolderId(sourceNode.getFolder().getId());
				}).build();
			} else {
				AtomicInteger contentSetId = new AtomicInteger();
				Page page = null;

				for (ContentLanguage language : sourceLanguages) {
					page = Builder.create(Page.class, p -> {
						p.setTemplateId(template.getId());
						p.setFolderId(sourceNode.getFolder().getId());
						p.setLanguage(language);
						if (contentSetId.get() != 0) {
							p.setContentsetId(contentSetId.get());
						}
					}).build();
					contentSetId.set(page.getContentsetId());
				}

				return page;
			}
		}
	}
}
