package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test case for publishing pages that have more the !6MB of content
 */
@RunWith(value = Parameterized.class)
public class LargePagePublishTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Tested page
	 */
	protected static Page page;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: multithreaded {0}, resumable_publish_process {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean multithreaded : Arrays.asList(true, false)) {
			for (boolean resumable : Arrays.asList(true, false)) {
				data.add(new Object[] { multithreaded, resumable });
			}
		}
		return data;
	}

	/**
	 * Setup test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		try (Trx trx = new Trx(null, 1)) {
			DBUtils.executeUpdate("UPDATE node SET disable_publish = ?", new Object[] {1});

			Transaction t = TransactionManager.getCurrentTransaction();
			Node node = ContentNodeTestDataUtils.createNode();

			int vtlConstructId = ContentNodeTestDataUtils.createVelocityConstruct(node, "vtl", "vtl");

			page = ContentNodeTestDataUtils.createTemplateAndPage(node.getFolder(), "Test");
			Template template = t.getObject(Template.class, page.getTemplate().getId(), true);

			// add a template tag that renders more the 16M of content, so that inserting the rendered page into the publish table must fail
			TemplateTag vtlTag = ContentNodeTestDataUtils.createTemplateTag(vtlConstructId, "content", false, false);
			template.getTemplateTags().put(vtlTag.getName(), vtlTag);
			vtlTag.getValues().getByKeyname("template").setValueText("#foreach($i in [1..1700000])0123456789#end");
			template.setSource("<node " + vtlTag.getName() + ">");
			template.save();
			trx.success();
		}
	}

	/**
	 * true for multithreaded
	 */
	protected boolean multithreaded;

	/**
	 * true for resumable publish process
	 */
	protected boolean resumable;

	/**
	 * Create a test instance
	 * @param multithreaded true for multithreaded publishing
	 * @param resumable true for resumable publish process
	 */
	public LargePagePublishTest(boolean multithreaded, boolean resumable) {
		this.multithreaded = multithreaded;
		this.resumable = resumable;
	}

	/**
	 * Setup test context by setting the features
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		try (Trx trx = new Trx(null, 1)) {
			NodePreferences prefs = testContext.getContext().getNodeConfig().getDefaultPreferences();
			prefs.setFeature(Feature.MULTITHREADED_PUBLISHING, multithreaded);
			prefs.setFeature("resumable_publish_process", resumable);
			trx.success();
		}
	}

	/**
	 * Test publishing the page, which is too large to fit into the publish table
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		try (Trx trx = new Trx(null, 1)) {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.getObject(Page.class, page.getId(), true).publish();
			trx.success();
		}

		try (Trx trx = new Trx(null, 1)) {
			assertEquals("Check publish status", PublishInfo.RETURN_CODE_ERROR,
					testContext.getContext().publish(false, true, System.currentTimeMillis(), false).getReturnCode());
		}
	}
}
