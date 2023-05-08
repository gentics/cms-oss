package com.gentics.contentnode.tests.parttype;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.ChangeableListPartType;
import com.gentics.contentnode.object.parttype.OrderedListPartType;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.UnorderedListPartType;
import com.gentics.contentnode.rest.model.request.PageCreateRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for modification of ListPartTypes
 */
public class ListPartTypeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Node for tests
	 */
	private static Node node;

	@BeforeClass
	public static void setupOnce() throws Exception {
		node = ContentNodeTestDataUtils.createNode("node", "Node", PublishTarget.NONE);
	}

	@Test
	public void testChangeableListOrdered() throws Exception {
		doTest(ChangeableListPartType.class, "clisto", "<ol>\n<li>one</li>\n<li>two</li>\n<li>three</li>\n</ol>\n", true);
	}

	@Test
	public void testChangeableListUnordered() throws Exception {
		doTest(ChangeableListPartType.class, "clistu", "<ul>\n<li>one</li>\n<li>two</li>\n<li>three</li>\n</ul>\n", false);
	}

	@Test
	public void testOrderedList() throws Exception {
		doTest(OrderedListPartType.class, "olist", "<ol>\n<li>one</li>\n<li>two</li>\n<li>three</li>\n</ol>\n", null);
	}

	@Test
	public void testUnorderedList() throws Exception {
		doTest(UnorderedListPartType.class, "olist", "<ul>\n<li>one</li>\n<li>two</li>\n<li>three</li>\n</ul>\n", null);
	}

	/**
	 * Do the test
	 * @param clazz parttype class
	 * @param keyword construct keyword
	 * @param expected expected output
	 * @param bool true for ChangeableList set to ordered, false for ChangeableList set to unordered, null if using OrderedList or UnorderedList
	 * @throws Exception
	 */
	protected void doTest(Class<? extends PartType> clazz, String keyword, String expected, Boolean bool) throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		int constructId = ContentNodeTestDataUtils.createConstruct(node, clazz, keyword, "list");
		Template template = Creator.createTemplate("Template", "<node list>", node.getFolder());
		TemplateTag tag = t.createObject(TemplateTag.class);
		tag.setConstructId(constructId);
		tag.setName("list");
		tag.setEnabled(true);
		tag.setPublic(true);
		template.getTemplateTags().put("list", tag);
		template.save();
		t.commit(false);

		PageCreateRequest request = new PageCreateRequest();
		request.setFolderId(node.getFolder().getId().toString());
		request.setTemplateId(ObjectTransformer.getInt(template.getId(), 0));
		PageLoadResponse response = ContentNodeRESTUtils.getPageResource().create(request);
		t.commit(false);
		ContentNodeRESTUtils.assertResponseOK(response);

		response.getPage().getTags().get("list").getProperties().get("list").setStringValues(Arrays.asList("one", "two", "three"));
		if (bool != null) {
			response.getPage().getTags().get("list").getProperties().get("list").setBooleanValue(bool);
		}

		GenericResponse saveResponse = ContentNodeRESTUtils.getPageResource().save(response.getPage().getId().toString(), new PageSaveRequest(response.getPage()));
		t.commit(false);
		ContentNodeRESTUtils.assertResponseOK(saveResponse);

		assertEquals("Check rendered page", expected, t.getObject(Page.class, response.getPage().getId()).render(null));
	}
}
