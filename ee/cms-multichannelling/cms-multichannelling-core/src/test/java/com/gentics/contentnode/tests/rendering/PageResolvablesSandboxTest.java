package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test various resolvables
 *
 * @author escitalopram
 *
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class PageResolvablesSandboxTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Set the languages for a node and clean the cache.
	 *
	 * @param n
	 *            the node to change the languages for
	 * @param contentgroupids
	 *            an array of the contentgroup ids of the languages in sort
	 *            order
	 * @return a fresh copy of n
	 * @throws NodeException
	 */
	private Node setNodeLanguages(Node n, Integer[] contentgroupids) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		List<ContentLanguage> res = t.getObjects(ContentLanguage.class, Arrays.asList(contentgroupids));
		DBUtils.executeInsert("delete from node_contentgroup where node_id = ?", new Object[] { n.getId() });
		for (int i = 0; i < res.size(); i++) {
			DBUtils.executeInsert("insert into node_contentgroup (node_id, contentgroup_id, sortorder) values (?,?,?)", new Object[] { n.getId(),
					res.get(i).getId(), i + 1 });
		}
		t.dirtObjectCache(Node.class, n.getId());
		return t.getObject(Node.class, n.getId());
	}

	/**
	 * Test if language sort order is maintained properly when using
	 * $cms.page.languageset
	 * @throws Exception 
	 */
	@Test
	public void testPageLanguageOrdering() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Create node
		Node node = ContentNodeTestDataUtils.createNode("noname", "test666", "/wtf/", null,  false, false, false);
		Folder rootFolder = node.getFolder();

		node = setNodeLanguages(node, new Integer[] { 1, 5 });

		// Create velocity construct to print languages in sortorder
		Construct construct = t.createObject(Construct.class);
		construct.setKeyword("constr");
		construct.setName("Construct (de)", 1);
		construct.setName("Construct (en)", 2);
		construct.getNodes().add(node);

		Part hbsPart = t.createObject(Part.class);
		hbsPart.setKeyname("hbs");
		hbsPart.setHidden(false);
		hbsPart.setEditable(0);
		hbsPart.setName("hbs", 1);
		hbsPart.setName("hbs", 2);
		hbsPart.setPartOrder(1);
		hbsPart.setPartTypeId(getPartTypeId(HandlebarsPartType.class));

		List<Part> parts = construct.getParts();
		parts.add(hbsPart);
		construct.save();

		Value vval = t.createObject(Value.class);
		vval.setContainer(construct);
		vval.setPart(hbsPart);
		vval.setValueText("{{#each cms.page.languageset.pages}}{{gtx_render language.code}} {{/each}}");
		hbsPart.setDefaultValue(vval);

		construct.save();

		// Create template using the construct
		Template template = t.createObject(Template.class);
		template.setName("hallo");
		template.setFolderId(rootFolder.getId());
		template.setSource("<node vtag>");
		TemplateTag tt = t.createObject(TemplateTag.class);
		tt.setConstructId(construct.getId());
		tt.setEnabled(true);
		tt.setName("vtag");
		tt.setPublic(false);
		template.getTags().put("vtag", tt);
		template.save();

		// Create two language variant pages
		Page page = t.createObject(Page.class);
		page.setFolderId(rootFolder.getId());
		page.setTemplateId(template.getId());
		page.setLanguageId(1);
		page.save();

		Page page2 = (Page) page.copy();
		page2.setContentsetId(page.getContentsetId());
		page2.setLanguageId(5);
		page2.setName(page.getName() + "2");
		page2.setFilename("Blaah");
		page2.save();

		t.commit(false);

		// Test sort order
		t.getRenderType().setEditMode(RenderType.EM_PREVIEW);
		String result = page.render(new RenderResult()).trim();
		assertEquals("Page language ordering doesn't match ", "de hu", result);

		// reverse sort order
		node = setNodeLanguages(node, new Integer[] { 5, 1 });

		// test again
		result = page.render(new RenderResult()).trim();
		assertEquals("Page language ordering doesn't match", "hu de", result);
	}
}
