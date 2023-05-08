package com.gentics.contentnode.tests.parttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the OverviewPartType
 */
public class OverviewPartTypeTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	private static final String OVERVIEW_PARTNAME = "ds";

	/**
	 * Timestamp for initial creation of test data
	 */
	protected final static int creationTime = 1;

	/**
	 * Node holding the tests
	 */
	protected Node node;


	@Before
	public void setUp() throws Exception {
		testContext.startTransaction(creationTime);
		node = ContentNodeTestDataUtils.createNode("Test Node", "testnode", "/Content.Node", null, false, false);
	}

	/**
	 * If a page contains a newly created Overview, the OverviewPartType must not be initialized.
	 * However, if the selection type and the object class only have one available
	 * option, then they must be initialized.
	 * @throws Exception
	 */
	@Test
	public void testEmptyOverviewPartType() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		Folder folder = t.createObject(Folder.class);
		folder.setMotherId(node.getFolder().getId());
		folder.setName("Test Folder");
		folder.setPublishDir("/");
		folder.save();
		t.commit(false);

		// create the template
		Template template = t.createObject(Template.class);
		template.getFolders().add(folder);
		template.setMlId(1);
		template.setName("Template");
		template.setSource("<node page.name>");
		template.save();
		t.commit(false);

		int overviewConstructId = ContentNodeTestDataUtils.createConstruct(node, OverviewPartType.class, OVERVIEW_PARTNAME, OVERVIEW_PARTNAME);

		//only one option for object class and selection type.
		//those fields should be initialized
		setInfoText("10007;1", overviewConstructId);
		Page page = t.createObject(Page.class);
		page.setName("ovpage 1");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		ContentTag tag = page.getContent().addContentTag(overviewConstructId);
		Overview overview = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, OVERVIEW_PARTNAME).getOverview();

		assertEquals("Check if selection type is folder", overview.getSelectionType(), com.gentics.contentnode.object.Overview.SELECTIONTYPE_FOLDER);
		assertTrue("Check if the object class is page", overview.getObjectClass().isAssignableFrom(Page.class));
		assertEquals("Check if orderBy is undefined",  overview.getOrderKind(), com.gentics.contentnode.object.Overview.ORDER_UNDEFINED);
		assertEquals("Check if orderDirection is undefined",  overview.getOrderWay(), com.gentics.contentnode.object.Overview.ORDERWAY_UNDEFINED);
		page.save();
		t.commit(false);

		//more than one option for object class and selection type.
		//those fields must not be initialized
		setInfoText("10007,10002,10008,10011;1,2,3;", overviewConstructId);
		page = t.createObject(Page.class);
		page.setName("ovpage 2");
		page.setTemplateId(template.getId());
		page.setFolderId(folder.getId());
		tag = page.getContent().addContentTag(overviewConstructId);
		overview = ContentNodeTestDataUtils.getPartType(OverviewPartType.class, tag, OVERVIEW_PARTNAME).getOverview();

		assertEquals("Check if the selection type is undefined", overview.getSelectionType(), com.gentics.contentnode.object.Overview.SELECTIONTYPE_UNDEFINED);
		assertNull("Check if the object class is null", overview.getObjectClass());
		assertEquals("Check if orderBy is undefined",  overview.getOrderKind(), com.gentics.contentnode.object.Overview.ORDER_UNDEFINED);
		assertEquals("Check if orderDirection is undefined",  overview.getOrderWay(), com.gentics.contentnode.object.Overview.ORDERWAY_UNDEFINED);
	}

	private void setInfoText(String infoText, int overviewConstructId) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Construct c = t.getObject(Construct.class, overviewConstructId, true);
		Part ovPart = null;
		if (c.containsOverviewPart()) {
			for (Part part : c.getParts()) {
				if (part.getName().toString().equals("ds")) {
					ovPart = part;
				}
			}
		}
		assertNotNull("The page must contain an OverViewPart", ovPart);
		ovPart.setInfoText(infoText);
		assertTrue("Error saving overview part", ovPart.save());
		c.getValues().getByKeyname(OVERVIEW_PARTNAME).setInfo(1);
		c.save();
		t.commit(false);
	}
}
