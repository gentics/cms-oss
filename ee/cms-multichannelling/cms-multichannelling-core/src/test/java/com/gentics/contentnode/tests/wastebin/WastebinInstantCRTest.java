package com.gentics.contentnode.tests.wastebin;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.TestedInheritance;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test deleting into and restoring from wastebin with instant publishing
 */
@RunWith(Parameterized.class)
public class WastebinInstantCRTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node, channel;

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public TestedInheritance inheritance;

	/**
	 * Get the testing parameters.
	 *
	 * @return A list of object types.
	 */
	@Parameters(name = "{index}: type: {0}, inheritance: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (MCTestedType type : MCTestedType.values()) {
			for (TestedInheritance inheritance : TestedInheritance.values()) {
				data.add(new Object[] { type, inheritance });
			}
		}

		return data;
	}

	/**
	 * Enable the wastebin and setup a node.
	 */
	@BeforeClass
	public static void setup() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();

		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		prefs.setFeature("instant_cr_publishing", true);

		node = ContentNodeTestDataUtils.createNode("testnode", "Test node", PublishTarget.CONTENTREPOSITORY, false, true);
		channel = ContentNodeTestDataUtils.createChannel(node, "Test channel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY, true);
	}

	@Test
	public void testDeleteRestore() throws Exception {
		// publish (to sync the attributes with the cr)
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// create test data
		LocalizableNodeObject<?>[] objects = null;
		try (Trx trx = new Trx()) {
			objects = inheritance.createPublished(type, node, channel);
			trx.success();
		}

		// assert objects in CR
		try (Trx trx = new Trx()) {
			if (objects[0] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[0], node, true);
			}
			if (objects[1] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[1], channel, true);
			} else {
				ContentNodeTestUtils.assertPublishCR(objects[0], channel, false);
			}
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert objects in CR
		try (Trx trx = new Trx()) {
			if (objects[0] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[0], node, true);
			}
			if (objects[1] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[1], channel, true);
			} else {
				ContentNodeTestUtils.assertPublishCR(objects[0], channel, true);
			}
			trx.success();
		}

		// delete
		try (Trx trx = new Trx()) {
			if (objects[0] != null) {
				objects[0].delete();
			} else if (objects[1] != null) {
				objects[1].delete();
			}
			trx.success();
		}

		// assert objects in CR
		try (Trx trx = new Trx()) {
			if (objects[0] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[0], node, false);
			}
			if (objects[1] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[1], channel, false);
			}
			trx.success();
		}

		// restore
		try (Trx trx = new Trx()) {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				if (objects[0] != null) {
					trx.getTransaction().getObject(objects[0]).restore();
				}
				if (objects[1] != null) {
					trx.getTransaction().getObject(objects[1]).restore();
				}
			}
			trx.success();
		}

		// assert objects in CR
		boolean expected = type != MCTestedType.page;
		try (Trx trx = new Trx()) {
			if (objects[0] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[0], node, expected);
			}
			if (objects[1] != null) {
				ContentNodeTestUtils.assertPublishCR(objects[1], channel, expected);
			}
			trx.success();
		}
	}
}
