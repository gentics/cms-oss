package com.gentics.contentnode.tests.rendering;

import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

public class AbstractVelocityRenderingTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Node
	 */
	protected static Node node;

	/**
	 * Template
	 */
	protected static Template template;

	/**
	 * Construct
	 */
	protected static Construct construct;

	/**
	 * Page
	 */
	protected static Page page;
	/**
	 * Name of the editable tag
	 */
	public final static String VTL_TAGNAME = "vtltag";

	@BeforeClass
	public static void setupOnce() throws Exception {
		String sessionToken = testContext.getContext().login("node", "node");
		TransactionManager.getCurrentTransaction().commit();

		testContext.getContext().getContentNodeFactory()
				.startTransaction(sessionToken, true);

		// Create node
		node = ContentNodeTestDataUtils.createNode();
		//
		// Create velocity construct that renders the given template
		construct = ContentNodeTestDataUtils.createVelocityConstruct(node);
		//
		// // Create template using the construct
		template = createTemplate(node.getFolder(), construct);
		//
		// // Create a page
		page = createPage(node.getFolder(), template);

	}

	/**
	 * Update the construct by setting the velocity template
	 * 
	 * @param vtl
	 *            template
	 * @throws Exception
	 */
	protected static void updateConstruct(String vtl) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		for (Part part : construct.getParts()) {
			if ("template".equals(part.getKeyname())) {
				part.getDefaultValue().setValueText(vtl);
			}
		}

		construct.save();
		t.commit(false);
	}

	/**
	 * Create a test template rendering a single tag of the construct
	 * 
	 * @param folder
	 *            folder to which the template is assigned
	 * @param construct
	 *            construct of the template tag
	 * @return template
	 * @throws Exception
	 */
	protected static Template createTemplate(Folder folder, Construct construct)
			throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Template template = t.createObject(Template.class);
		template.setName("hallo");
		template.setFolderId(folder.getId());
		template.setSource("<node " + VTL_TAGNAME + ">");
		TemplateTag tt = t.createObject(TemplateTag.class);
		tt.setConstructId(construct.getId());
		tt.setEnabled(true);
		tt.setName(VTL_TAGNAME);
		tt.setPublic(true);
		template.getTags().put(VTL_TAGNAME, tt);
		template.save();

		return template;
	}

	/**
	 * Create a test page
	 * 
	 * @param folder
	 *            folder of the page
	 * @param template
	 *            template of the page
	 * @return page
	 * @throws Exception
	 */
	protected static Page createPage(Folder folder, Template template)
			throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Page page = t.createObject(Page.class);
		page.setFolderId(folder.getId());
		page.setTemplateId(template.getId());

		ContentTag tag = page.getContentTag(VTL_TAGNAME);
		tag.getValues().getByKeyname("text")
				.setValueText("This is the test content");

		page.save();

		return page;
	}
}
