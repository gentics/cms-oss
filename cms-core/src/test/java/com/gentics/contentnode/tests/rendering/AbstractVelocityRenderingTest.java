package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.db.DBUtils.firstInt;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createVelocityConstruct;

import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract base class for velocity rendering tests
 */
public abstract class AbstractVelocityRenderingTest {

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

	protected static SystemUser user;

	/**
	 * Name of the editable tag
	 */
	public final static String VTL_TAGNAME = "vtltag";


	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		// Create node
		node = supply(() -> createNode());

		// Create velocity construct that renders the given template
		construct = supply(() ->  createVelocityConstruct(node));

		// Create template using the construct
		template = supply(() -> createTemplate(node.getFolder(), construct));

		// Create a page
		page = supply(() -> createPage(node.getFolder(), template));

		user = supply(t -> t.getObject(SystemUser.class, DBUtils.select("SELECT id FROM systemuser WHERE login = ?",
				pst -> pst.setString(1, "node"), firstInt("id"))));
	}

	/**
	 * Update the construct by setting the velocity template
	 * 
	 * @param vtl
	 *            template
	 * @throws NodeException
	 */
	protected static void updateConstruct(String vtl) throws NodeException {
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
	 * @throws NodeException
	 */
	protected static Template createTemplate(Folder folder, Construct construct)
			throws NodeException {
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
	 * @throws NodeException
	 */
	protected static Page createPage(Folder folder, Template template)
			throws NodeException {
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
