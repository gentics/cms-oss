package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for rendering the diff between page versions
 */
@RunWith(value = Parameterized.class)
public class PageVersionsDiffTest {
	@ClassRule
	public static DBTestContext context = new DBTestContext();
	private static Node node;

	private static int oldVersion;

	private static int newVersion;
	private static Construct htmlConstruct;
	private static Template template;

	/**
	 * Wrap the given content into a &lt;del&gt; Tag
	 * @param content removed content
	 * @return wrapped content
	 */
	protected static String removed(String content) {
		return "<del class='diff modified gtx-diff'>%s</del>".formatted(content);
	}

	/**
	 * Wrap the given content into a &lt;ins&gt; Tag
	 * @param content added content
	 * @return wrapped content
	 */
	protected static String added(String content) {
		return "<ins class='diff modified gtx-diff'>%s</ins>".formatted(content);
	}

	/**
	 * Render the change from one content to another
	 * @param from old content
	 * @param to new content
	 * @return diff
	 */
	protected static String modified(String from, String to) {
		return "%s%s".formatted(removed(from), added(to));
	}

	/**
	 * Wrap the content with the diff wrapper
	 * @param content content
	 * @return wrapped content
	 */
	protected static String daisyDiffWrapped(String content) {
		return "<div id=\"gtxDiffWrapper\">\n%s</div>\n".formatted(content);
	}

	/**
	 * Render the content as being removed (daisy diff)
	 * @param content removed content
	 * @param id id of the change
	 * @param next full id of the next change
	 * @param prev full id of the prev change
	 * @return rendered removal
	 */
	protected static String daisyDiffRemoved(String content, int id, String next, String prev) {
		return "<span changeId=\"removed-gtxDiff-%d\" class=\"diff-html-removed\" id=\"removed-gtxDiff-%d\" next=\"%s\" previous=\"%s\">%s</span>"
				.formatted(id, id, next, prev, content);
	}

	/**
	 * Render the content as being added (daisy diff)
	 * @param content added content
	 * @param id id of the change
	 * @param next full id of the next change
	 * @param prev full id of the prev change
	 * @return rendered adding
	 */
	protected static String daisyDiffAdded(String content, int id, String next, String prev) {
		return "<span changeId=\"added-gtxDiff-%d\" class=\"diff-html-added\" id=\"added-gtxDiff-%d\" next=\"%s\" previous=\"%s\">%s</span>"
				.formatted(id, id, next, prev, content);
	}

	/**
	 * Render a change from one content to another (daisy diff)
	 * @param from old content
	 * @param to new content
	 * @return rendered change
	 */
	protected static String daisyDiffModified(String from, String to) {
		return daisyDiffRemoved(from, 0, "added-gtxDiff-0", "first-gtxDiff") + daisyDiffAdded(to, 0, "last-gtxDiff", "removed-gtxDiff-0");
	}

	/**
	 * Render a change from one content to another (daisy diff)
	 * @param from old content
	 * @param between content between the old and new content
	 * @param to new content
	 * @return rendered change
	 */
	protected static String daisyDiffModified(String from, String between, String to) {
		return daisyDiffRemoved(from, 0, "added-gtxDiff-0", "first-gtxDiff") + between + daisyDiffAdded(to, 0, "last-gtxDiff", "removed-gtxDiff-0");
	}

	/**
	 * Remove newlines
	 * @param content content
	 * @return content with newlines removed
	 */
	protected static String removeNewLines(String content) {
		return content.replaceAll("\\n", "").replaceAll("\\r", "");
	}

	/**
	 * Replace &lt; and &gt; with the html entities
	 * @param content content
	 * @return content with html entities
	 */
	protected static String htmlEntities(String content) {
		return content.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();
		newVersion = (int)((System.currentTimeMillis() / 1000L));
		oldVersion = newVersion - 100;

		node = supply(() -> createNode());
		htmlConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("html");
			c.setName("HTML Construct", 1);

			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("html");
				p.setPartTypeId(ContentNodeTestDataUtils.getPartTypeId(LongHTMLPartType.class));
			}).doNotSave().build());
		}).build();

		template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("<node html>");
			t.getTemplateTags().put("html", create(TemplateTag.class, tag -> {
				tag.setConstructId(htmlConstruct.getId());
				tag.setEnabled(true);
				tag.setName("html");
				tag.setPublic(true);
			}).doNotSave().build());
		}).build();
	}

	@Parameters(name = "{index}: {0} -> {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		// plain text change
		data.add(new Object[] {
				"old content",
				"new content",
				modified("old", "new") + " content",
				modified("old", "new") + " content",
				daisyDiffWrapped(daisyDiffModified("old ", "new ") + "content")
		});

		// text change with added paragraph
		data.add(new Object[] {
				"<p>old content</p>",
				"<p>new</p><p>content</p>",
				htmlEntities("<p>") + modified("old ", htmlEntities("new</p><p>")) + htmlEntities("content</p>"),
				"<p>" + modified("old ", "new</p><p>") + "content</p>",
				daisyDiffWrapped("<p>" + daisyDiffModified("old ", "new") + "</p><p>content</p>")
		});

		// text change with removed paragraph
		data.add(new Object[] {
				"<p>old</p><p>content</p>",
				"<p>new content</p>",
				htmlEntities("<p>") + modified(htmlEntities("old</p><p>"), "new ") + htmlEntities("content</p>"),
				"<p>" + modified("old</p><p>", "new ") + "content</p>",
				daisyDiffWrapped("<p>" + daisyDiffModified("old", "</p><p>", "new ") + "content</p>")
		});

		// text change with paragraph transformed to break
		data.add(new Object[] {
				"<p>old</p><p>content</p>",
				"<p>new<br>content</p>",
				htmlEntities("<p>") + modified(htmlEntities("old</p><p>"), htmlEntities("new<br>")) + htmlEntities("content</p>"),
				"<p>" + modified("old</p><p>", "new<br>") + "content</p>",
				daisyDiffWrapped("<p>" + daisyDiffModified("old", "</p><p>", "new") + "<br>content</p>")
		});

		// text change with break transformed to paragraph
		data.add(new Object[] {
				"<p>old<br>content</p>",
				"<p>new</p><p>content</p>",
				htmlEntities("<p>") + modified(htmlEntities("old<br>"), htmlEntities("new</p><p>")) + htmlEntities("content</p>"),
				"<p>" + modified("old<br>", "new</p><p>") + "content</p>",
				daisyDiffWrapped("<p>" + daisyDiffModified("old", "new") + "</p><p>content</p>")
		});

		// change of link href and text
		data.add(new Object[] {
				"<a href=\"https://orf.at/\">Link to ORF</a>",
				"<a href=\"https://www.gentics.com/\">Link to Gentics</a>",
				modified(htmlEntities("<a href=\"https://orf.at/\">"), htmlEntities("<a href=\"https://www.gentics.com/\">")) + "Link to " + modified("ORF", "Gentics") + htmlEntities("</a>"),
				modified("<a href=\"https://orf.at/\">", "<a href=\"https://www.gentics.com/\">") + "Link to " + modified("ORF", "Gentics") + "</a>",
				daisyDiffWrapped("<a href=\"https://www.gentics.com/\"><span changeId=\"changed-gtxDiff-0\" changes=\"<ul class='changelist'><li>Moved out of a <b>link</b> with destination https://orf.at/.</li><li>Moved to a <b>link</b> with destination <br/>https://www.gentics.com/.</li></ul>\" class=\"diff-html-changed\" id=\"changed-gtxDiff-0\" next=\"removed-gtxDiff-0\" previous=\"first-gtxDiff\">Link to </span></a><span changeId=\"removed-gtxDiff-0\" class=\"diff-html-removed\" next=\"added-gtxDiff-0\" previous=\"changed-gtxDiff-0\"> </span><span changeId=\"removed-gtxDiff-0\" class=\"diff-html-removed\" id=\"removed-gtxDiff-0\" next=\"added-gtxDiff-0\" previous=\"changed-gtxDiff-0\">ORF</span><a href=\"https://www.gentics.com/\"><span changeId=\"added-gtxDiff-0\" class=\"diff-html-added\" id=\"added-gtxDiff-0\" next=\"last-gtxDiff\" previous=\"removed-gtxDiff-0\">Gentics</span></a>")
		});
		return data;
	}

	@Parameter(0)
	public String oldContent;

	@Parameter(1)
	public String newContent;

	@Parameter(2)
	public String expectedSourceDiff;

	@Parameter(3)
	public String expectedHtmlDiff;

	@Parameter(4)
	public String expectedHtmlDaisyDiff;

	protected Page page;

	@Before
	public void setup() throws NodeException {
		page = create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());

			getPartType(LongHTMLPartType.class, p.getContentTag("html"), "html").setText(oldContent);
		}).at(oldVersion).build();

		page = Builder.update(page, p -> {
			getPartType(LongHTMLPartType.class, p.getContentTag("html"), "html").setText(newContent);
		}).at(newVersion).build();
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> {
			clear(node);
		});
	}

	/**
	 * Test diff in source
	 * @throws NodeException
	 */
	@Test
	public void testDiffSource() throws NodeException {
		String diff = supply(t -> {
			Response response = getPageResource().diffVersions(String.valueOf(page.getId()), null, oldVersion,
					newVersion, true, false);
			return response.getEntity().toString();
		});

		assertThat(diff).as("Source diff").isEqualTo(expectedSourceDiff);
	}

	/**
	 * Test diff in HTML
	 * @throws NodeException
	 */
	@Test
	public void testDiffHtml() throws NodeException {
		String diff = supply(t -> {
			Response response = getPageResource().diffVersions(String.valueOf(page.getId()), null, oldVersion,
					newVersion, false, false);
			return response.getEntity().toString();
		});

		assertThat(diff).as("HTML diff").isEqualTo(expectedHtmlDiff);
	}

	/**
	 * Test diff in HTML using daisy diff
	 * @throws NodeException
	 */
	@Test
	public void testDaisyDiffHtml() throws NodeException {
		String diff = supply(t -> {
			Response response = getPageResource().diffVersions(String.valueOf(page.getId()), null, oldVersion,
					newVersion, false, true);
			return response.getEntity().toString();
		});

		assertThat(removeNewLines(diff)).as("HTML Daisy diff").isEqualTo(removeNewLines(expectedHtmlDaisyDiff));
	}
}
