package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createVelocityConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.TextPartType;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Tests for rendering velocity tags containing macros
 */
public class VelomacroRenderSandboxTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Number of different templates
	 */
	protected final static int NUM_TEMPLATES = 5;

	/**
	 * Number of pages generated per template
	 */
	protected final static int NUM_PAGES_PER_TEMPLATE = 10;

	/**
	 * Number of threads that render pages
	 */
	protected final static int NUM_THREADS = 10;

	/**
	 * Node for the test data
	 */
	protected Node node;

	/**
	 * Collection of templates
	 */
	protected Collection<Template> templates = new ArrayList<Template>();

	/**
	 * Collection of test pages
	 */
	protected Collection<Page> pages = new ArrayList<Page>();

	@Before
	public void setUp() throws Exception {
		testContext.getContext().login("node", "node");
		node = createNode("Test Node", "test", "/Content.Node", null, false, false, false);
		Folder root = node.getFolder();

		for (int i = 0; i < NUM_TEMPLATES; i++) {
			String name = "Template" + i;
			templates.add(createVelocityTemplate(node, name, generateVTL(name)));
		}

		for (int i = 0; i < NUM_PAGES_PER_TEMPLATE; i++) {
			for (Template template : templates) {
				pages.add(createPage(root, template, "Page " + i + " of " + template.getName()));
			}
		}
	}

	/**
	 * Test rendering the pages using velocity macros in several threads
	 * @throws Exception
	 */
	@Test
	public void testRendering() throws Throwable {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<PageRenderThread> threads = new ArrayList<PageRenderThread>(NUM_THREADS);

		for (int i = 0; i < NUM_THREADS; i++) {
			PageRenderThread thread = new PageRenderThread(t.getSessionId());
			threads.add(thread);
			thread.start();
		}

		for (PageRenderThread thread : threads) {
			thread.join();
		}

		for (PageRenderThread thread : threads) {
			thread.assertSuccess();
		}
	}

	/**
	 * Create a template containing a single (non editable) tag that renders the given vtl using velocity
	 * @param node node
	 * @param name template name
	 * @param vtl velocity code
	 * @return template
	 * @throws NodeException
	 */
	protected Template createVelocityTemplate(Node node, String name, String vtl) throws NodeException {
		int vtlConstructId = createVelocityConstruct(node, "velocity", "vtl");
		Transaction t = TransactionManager.getCurrentTransaction();
		Template template = t.createObject(Template.class);
		template.setFolderId(node.getFolder().getId());
		template.setMlId(1);
		template.setName(name);
		template.setSource("<node velocity>");

		TemplateTag tag = t.createObject(TemplateTag.class);
		tag.setConstructId(vtlConstructId);
		tag.setEnabled(true);
		tag.setName("velocity");
		tag.setPublic(false);
		getPartType(TextPartType.class, tag, "template").getValueObject().setValueText(vtl);
		template.getTemplateTags().put("velocity", tag);

		template.save();
		t.commit(false);

		return t.getObject(Template.class, template.getId());
	}

	/**
	 * Create a page in the folder using the given template
	 * @param folder folder
	 * @param template template
	 * @param name name
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage(Folder folder, Template template, String name) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page page = t.createObject(Page.class);
		page.setFolderId(folder.getId());
		page.setTemplateId(template.getId());
		page.setName(name);
		page.save();
		t.commit(false);
		return t.getObject(Page.class, page.getId());
	}

	/**
	 * Generate the vtl for the given test template
	 * @param name template name
	 * @return vtl
	 */
	protected String generateVTL(String name) {
		StringBuilder vtl = new StringBuilder();
		vtl.append("#testmacro(1 10)##\n");
		vtl.append("#macro(testmacro $level $max)##\n");
		vtl.append("#set($a = \"").append(name).append("\")##\n");
		vtl.append("$a##\n");
		vtl.append("#if($level < $max)##\n");
		vtl.append("#set($level = $level + 1)##\n");
		vtl.append("#testmacro($level $max)##\n");
		vtl.append("#end##\n");
		vtl.append("#end##\n");
		return vtl.toString();
	}

	public class PageRenderThread extends Thread {
		protected Throwable e;

		protected String sid;

		public PageRenderThread(String sid) {
			this.sid = sid;
		}

		@Override
		public void run() {
			try (Trx trx = new Trx(sid, -1)) {
				for (Page page : pages) {
					PageResource resource = new PageResourceImpl();

					PageRenderResponse renderResponse = resource.render(ObjectTransformer.getString(page.getId(), null), null, null, false, null,
							LinksType.frontend, false, false, false);
					assertResponseOK(renderResponse);
					assertEquals("Check rendered content", StringUtils.repeat(page.getTemplate().getName(), 10), renderResponse.getContent());
				}
			} catch (Throwable e) {
				this.e = e;
			}
		}

		/**
		 * Assert success of all render calls
		 * @throws Exception
		 */
		public void assertSuccess() throws Throwable {
			if (e != null) {
				throw e;
			}
		}
	}
}
