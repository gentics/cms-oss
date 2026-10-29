package com.gentics.contentnode.tests.edit;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test case for creating a new channel when the master has a synchronized object property set
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.OBJTAG_SYNC })
public class ObjectTagSyncNewChannelTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Integer constructId;

	public final static String PART_NAME = "part";

	public final static String OBJTAG_NAME = "sync";

	public final static String ORIGINAL_TAG_VALUE = "original";

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "Node", PublishTarget.NONE));

		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "construct", PART_NAME));
		supply(() -> update(
				createObjectPropertyDefinition(Folder.TYPE_FOLDER, constructId, "Synchronized", OBJTAG_NAME), upd -> {
					upd.setSyncChannelset(true);
				}));
	}

	/**
	 * Test creating a new channel
	 * @throws NodeException
	 */
	@Test
	public void testCreateChannel() throws NodeException {
		// update the object property in the masters root folder
		operate(() -> update(node.getFolder(), upd -> {
			ObjectTag objectTag = upd.getObjectTag(OBJTAG_NAME);
			objectTag.setEnabled(true);
			getPartType(ShortTextPartType.class, objectTag, PART_NAME).getValueObject().setValueText(ORIGINAL_TAG_VALUE);
		}));

		// create a new channel
		Node channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		// assert that channel and master node have the object property set identically
		operate(() -> {
			for (Node n : Arrays.asList(node, channel)) {
				try (ChannelTrx cTrx = new ChannelTrx(n)) {
					assertThat(n.getFolder().getObjectTag(OBJTAG_NAME)).as("OE in " + n.getFolder().getName()).isNotNull().isEnabled(true)
						.hasPartWithText(ShortTextPartType.class, PART_NAME, ORIGINAL_TAG_VALUE);
				}
			}
		});
	}
}
