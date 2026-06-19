package com.gentics.contentnode.tests.wastebin;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.ContentNodeHelper;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.scheduler.CoreInternalSchedulerTask;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.WASTEBIN })
public class WastebinPurgeLinkedTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Static setup (activate feature wastebin)
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
	}

	@Test
	public void testPurgeLinkedPage() throws NodeException, InterruptedException {
		Node node;
		Template template;
		Page page1, page2;

		node = supply(() -> createNode());
		operate(trx -> {
			NodePreferences prefs = trx.getNodeConfig().getDefaultPreferences();
			prefs.setPropertyMap("wastebin_maxage_node", Map.of(node.getId().toString(), "1"));
		});
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		Integer nodeConstructId = supply(t -> {
			Integer constructId = createConstruct(null, PageURLPartType.class, "both", "ds");
			update(t.getObject(Construct.class, constructId), c -> {
				c.getNodes().add(node);
			});
			return constructId;
		});

		page1 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 1");
		}));

		page2 = supply(() -> create(Page.class, p -> {
			p.setFolderId(node.getFolder().getId());
			p.setTemplateId(template.getId());
			p.setName("Page 2");
			p.getContent().addContentTag(nodeConstructId);
			PageURLPartType pageUrl = getPartType(PageURLPartType.class, p.getTag("both1"), "ds");
			pageUrl.setTargetPage(page1);
		}));

		// publish the referencing, waste the referenced
		operate(t -> {
			t.getObject(page2, true).publish();
			t.getObject(page1).delete();
		});

		// check that deleted objects are in wastebin
		try (Trx trx = new Trx(null, 1); WastebinFilter wb = Wastebin.INCLUDE.set()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			Page page1bis = t.getObject(page1);
			assertNotNull(page1bis + " must still exist in the wastebin", page1bis);
			assertTrue(page1bis + " must be deleted", page1bis.isDeleted());
		}

		// Reset the current lang context to mimic the truly scheduled env (not possible by default as per ContentNodeTestContext)
		ContentNodeHelper.setLanguageId(-1);
		// Purge 'em all. Should not throw.
		CoreInternalSchedulerTask.purgewastebin.execute(new ArrayList<>());
		// check referenced deleted
		operate(t -> {
			assertThat(t.getObject(page1)).as("Wasted page").isNull();
		});
	}
}
