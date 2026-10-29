package com.gentics.contentnode.tests.wastebin;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;


/**
 * Test consequences of wastebin actions for the content repository.
 */
@RunWith(Parameterized.class)
public class WastebinCRTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node crNode, crChannel, mccrNode, mccrChannel;

	@Parameter(0)
	public MCTestedType type;

	@Parameter(1)
	public boolean mccr;

	@Parameter(2)
	public boolean publishBetweenActions;

	/**
	 * Get the testing parameters.
	 *
	 * @return A list of object types.
	 */
	@Parameters(name = "{index}: type: {0}, mccr: {1}, publish between actions: {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();

		for (MCTestedType type : MCTestedType.values()) {
			for (boolean mccr : Arrays.asList(true, false)) {
				for (boolean publishBetweenActions : Arrays.asList(true, false)) {
					data.add(new Object[] { type, mccr, publishBetweenActions });
				}
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
		prefs.setFeature("mccr", true);

		crNode = ContentNodeTestDataUtils.createNode("testnode", "Test node", PublishTarget.CONTENTREPOSITORY);
		crChannel = ContentNodeTestDataUtils.createChannel(crNode, "Test channel", "testchannel", "/", PublishTarget.CONTENTREPOSITORY);
		mccrNode = ContentNodeTestDataUtils.createNode("mccr_testnode", "MCCR Test node", PublishTarget.CONTENTREPOSITORY, true, false);
		mccrChannel = ContentNodeTestDataUtils.createChannel(mccrNode, "MCCR Test channel", "mccr_testchannel", "/", PublishTarget.CONTENTREPOSITORY);
	}

	@Test
	public void testRestoreMaster() throws Exception {
		deleteRestore(true, false, true);
	}

	@Test
	public void testRestoreLocalizedMaster() throws Exception {
		deleteRestore(true, true, true);
	}

	@Test
	public void testRestoreLocalizedChannel() throws Exception {
		deleteRestore(true, true, false);
	}

	@Test
	public void testRestoreLocal() throws Exception {
		deleteRestore(false, true, false);
	}

	/**
	 * Create an object, delete and restore it. Check that publish process works as expected
	 * @param inMaster true to create the object in the node
	 * @param inChannel true to create the object in the channel (localize if also in master)
	 * @param deleteMaster true to create the object in the master, even if localized
	 * @throws Exception
	 */
	protected void deleteRestore(boolean inMaster, boolean inChannel, boolean deleteMaster) throws Exception {
		Node node = mccr ? mccrNode : crNode;
		Node channel = mccr ? mccrChannel : crChannel;
		NodeObject masterObject = null;
		NodeObject channelObject = null;
		NodeObject toRestore = null;

		// create test data
		try (Trx trx = new Trx()) {
			Template template = type == MCTestedType.page ? Creator.createTemplate("Test template", "", node.getFolder()) : null;

			if (inMaster) {
				masterObject = type.create(node.getFolder(), template);
				if (inChannel) {
					channelObject = type.localize((LocalizableNodeObject<?>) masterObject, channel);
				}
				toRestore = inChannel && !deleteMaster ? channelObject : masterObject;
			} else if (inChannel) {
				channelObject = type.create(node.getFolder(), null, template, channel);
				toRestore = channelObject;
			}

			if (type == MCTestedType.page) {
				if (masterObject != null) {
					trx.getTransaction().getObject(Page.class, masterObject.getId(), true).publish();
				}
				if (channelObject != null) {
					trx.getTransaction().getObject(Page.class, channelObject.getId(), true).publish();
				}
			}

			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			if (masterObject != null) {
				ContentNodeTestUtils.assertPublishCR(masterObject, node, true);
			}
			if (channelObject != null) {
				ContentNodeTestUtils.assertPublishCR(channelObject, node, false);
				ContentNodeTestUtils.assertPublishCR(channelObject, channel, true);
			} else {
				ContentNodeTestUtils.assertPublishCR(masterObject, channel, true);
			}
			trx.success();
		}

		// delete
		try (Trx trx = new Trx()) {
			if (toRestore != null) {
				toRestore.delete();
			}
			trx.success();
		}

		if (publishBetweenActions) {
			// publish
			try (Trx trx = new Trx()) {
				testContext.publish(false);
				trx.success();
			}

			// assert
			try (Trx trx = new Trx()) {
				if (masterObject != null) {
					ContentNodeTestUtils.assertPublishCR(masterObject, node, !toRestore.equals(masterObject));
				}
				if (channelObject != null) {
					ContentNodeTestUtils.assertPublishCR(channelObject, node, false);
					ContentNodeTestUtils.assertPublishCR(channelObject, channel, false);
				} else {
					ContentNodeTestUtils.assertPublishCR(masterObject, channel, false);
				}
				trx.success();
			}
		} else {
			try (Trx trx = new Trx()) {
				testContext.waitForDirtqueueWorker();
				trx.success();
			}
		}

		// restore
		try (Trx trx = new Trx()) {
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				trx.getTransaction().getObject(toRestore).restore();
			}
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			boolean expected = type != MCTestedType.page;

			if (masterObject != null) {
				ContentNodeTestUtils.assertPublishCR(masterObject, node, expected || !toRestore.equals(masterObject));
			}
			if (toRestore.equals(channelObject)) {
				ContentNodeTestUtils.assertPublishCR(channelObject, channel, expected);
			} else {
				ContentNodeTestUtils.assertPublishCR(masterObject, channel, expected);
			}
			trx.success();
		}
	}
}
