package com.gentics.contentnode.tests.wastebin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.NodePreferences;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Wastebin;
import com.gentics.contentnode.factory.WastebinFilter;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.MCTestedType;
import com.gentics.contentnode.testutils.ChannelStructure;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for channel fallback and objects in the wastebin
 */
@RunWith(value = Parameterized.class)
public class WastebinChannelFallbackTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Channel Structure
	 */
	protected static ChannelStructure structure;

	/**
	 * Template for creating pages
	 */
	private static Template template;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: test: {0}, channel: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (MCTestedType type : MCTestedType.values()) {
			for (Channel channel : Arrays.asList(Channel.MASTER, Channel.CHANNEL, Channel.SUBCHANNEL, Channel.SIDECHANNEL)) {
				data.add(new Object[] {type, channel});
			}
		}
		return data;
	}

	/**
	 * Create static test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		NodePreferences prefs = t.getNodeConfig().getDefaultPreferences();
		prefs.setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		prefs.setFeature(Feature.WASTEBIN.toString().toLowerCase(), true);
		structure = new ChannelStructure();
		t.commit(false);
		template = ContentNodeTestDataUtils.createTemplate(structure.getNode(Channel.MASTER).getFolder(), "Source", "Template");
	}

	private MCTestedType type;

	private Channel channel;

	/**
	 * Create a test instance
	 * @param type tested type
	 * @param channel tested channel
	 */
	public WastebinChannelFallbackTest(MCTestedType type, Channel channel) {
		this.type = type;
		this.channel = channel;
	}

	/**
	 * Test getting objects in channels
	 */
	@Test
	public void testFallback() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		// create the master object and its localized copies
		LocalizableNodeObject<?> masterObject = type.create(structure.getNode(Channel.MASTER).getFolder(), template);
		LocalizableNodeObject<?> channelObject = type.localize(masterObject, structure.getNode(Channel.CHANNEL));
		LocalizableNodeObject<?> subChannelObject = type.localize(channelObject, structure.getNode(Channel.SUBCHANNEL));
		LocalizableNodeObject<?> sideChannelObject = type.localize(masterObject, structure.getNode(Channel.SIDECHANNEL));

		Class<? extends NodeObject> clazz = masterObject.getObjectInfo().getObjectClass();
		List<Integer> ids = Arrays.asList(masterObject.getId(), channelObject.getId(), subChannelObject.getId(), sideChannelObject.getId());

		Map<Node, LocalizableNodeObject<?>> objectMap = new HashMap<Node, LocalizableNodeObject<?>>();
		objectMap.put(structure.getNode(Channel.MASTER), masterObject);
		objectMap.put(structure.getNode(Channel.CHANNEL), channelObject);
		objectMap.put(structure.getNode(Channel.SUBCHANNEL), subChannelObject);
		objectMap.put(structure.getNode(Channel.SIDECHANNEL), sideChannelObject);

		Map<Node, LocalizableNodeObject<?>> wastebinObjectMap = new HashMap<Node, LocalizableNodeObject<?>>(objectMap);

		// delete the object in the tested channel
		switch (channel) {
		case MASTER:
			masterObject.reload().delete();
			t.commit(false);
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				masterObject = t.getObject(masterObject);
				assertNotNull("Deleted object must still exist", masterObject);
				assertTrue("Object must be in wastebin", masterObject.isDeleted());
			}
			objectMap.put(structure.getNode(Channel.MASTER), null);
			objectMap.put(structure.getNode(Channel.CHANNEL), null);
			objectMap.put(structure.getNode(Channel.SUBCHANNEL), null);
			objectMap.put(structure.getNode(Channel.SIDECHANNEL), null);
			break;
		case CHANNEL:
			channelObject.reload().delete();
			t.commit(false);
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				channelObject = t.getObject(channelObject);
				assertNotNull("Deleted object must still exist", channelObject);
				assertTrue("Object must be in wastebin", channelObject.isDeleted());
			}
			objectMap.put(structure.getNode(Channel.CHANNEL), masterObject);
			break;
		case SUBCHANNEL:
			subChannelObject.reload().delete();
			t.commit(false);
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				subChannelObject = t.getObject(subChannelObject);
				assertNotNull("Deleted object must still exist", subChannelObject);
				assertTrue("Object must be in wastebin", subChannelObject.isDeleted());
			}
			objectMap.put(structure.getNode(Channel.SUBCHANNEL), channelObject);
			break;
		case SIDECHANNEL:
			sideChannelObject.reload().delete();
			t.commit(false);
			try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
				sideChannelObject = t.getObject(sideChannelObject);
				assertNotNull("Deleted object must still exist", sideChannelObject);
				assertTrue("Object must be in wastebin", sideChannelObject.isDeleted());
			}
			objectMap.put(structure.getNode(Channel.SIDECHANNEL), masterObject);
			break;
		default:
			fail("Unexpected channel " + channel);
		}

		// get object specific to channel
		for (Node channel : Arrays.asList(structure.getNode(Channel.MASTER), structure.getNode(Channel.CHANNEL), structure.getNode(Channel.SUBCHANNEL),
				structure.getNode(Channel.SIDECHANNEL))) {
			try (ChannelTrx trx = new ChannelTrx(channel)) {
				// get every single object
				for (Integer id : ids) {
					NodeObject fallbackObject = t.getObject(clazz, id);
					assertEquals("Check object in " + channel + ":", objectMap.get(channel), fallbackObject);
				}

				// get list of objects
				List<? extends NodeObject> objects = t.getObjects(clazz, ids);
				for (NodeObject fallbackObject : objects) {
					assertEquals("Check object in " + channel + ":", objectMap.get(channel), fallbackObject);
				}

				// now let's include wastebin objects
				try (WastebinFilter filter = Wastebin.INCLUDE.set()) {
					// get every single object
					for (Integer id : ids) {
						NodeObject fallbackObject = t.getObject(clazz, id);
						assertEquals("Check object in " + channel + ":", wastebinObjectMap.get(channel), fallbackObject);
					}

					// get list of objects
					objects = t.getObjects(clazz, ids);
					for (NodeObject fallbackObject : objects) {
						assertEquals("Check object in " + channel + ":", wastebinObjectMap.get(channel), fallbackObject);
					}
				}
			}
		}
	}
}
