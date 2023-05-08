package com.gentics.contentnode.tests.rendering;

import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

public abstract class AbstractRenderTypeTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	protected static Node node;
	protected static Folder folder;
	protected static Template template;
	protected static Page page1;
	protected static Page page2;
	protected static Integer constructId;
	protected static ContentTag contentTag;

	@BeforeClass
	public static void setup() throws NodeException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		folder = Trx.supply(() -> node.getFolder());
		template = Trx.supply(() -> ContentNodeTestDataUtils.createTemplate(folder, "Template"));
		page1 = Trx.supply(() -> ContentNodeTestDataUtils.createPage(folder, template, "Page1"));
		page2 = Trx.supply(() -> ContentNodeTestDataUtils.createPage(folder, template, "Page2"));
		constructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, LongHTMLPartType.class, "construct", "html"));
		contentTag = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page editablePage = t.getObject(page1, true);
			ContentTag editableContentTag = editablePage.getContent().addContentTag(constructId);
			editablePage.save();

			return t.getObject(editableContentTag);
		});
	}

}
