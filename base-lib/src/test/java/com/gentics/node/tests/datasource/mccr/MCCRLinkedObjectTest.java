package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.lib.datasource.mccr.MCCRObject;
import org.junit.experimental.categories.Category;

/**
 * Special test cases for linked objects
 */
@Category(BaseLibTest.class)
public class MCCRLinkedObjectTest extends AbstractMCCRCacheTest {
	/**
	 * Test getting a linked object from another object, when the datasource has a wrong channel set
	 * @throws Exception
	 */
	@Test
	public void testGetLinkTargetWrongChannel() throws Exception {
		testGetLinkTarget(MASTER_ID);
	}

	/**
	 * Test getting a linked object from another object, when the datasource has the correct channel set
	 * @throws Exception
	 */
	@Test
	public void testGetLinkTargetCorrectChannel() throws Exception {
		testGetLinkTarget(CHANNEL_ID);
	}

	/**
	 * Do the test for getting the linked object
	 * @param channelIdToSet channel id, that is set before the linked object is fetched
	 * @throws Exception
	 */
	protected void testGetLinkTarget(int channelIdToSet) throws Exception {
		// create an object in the channel
		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put("text", "target");
		MCCRObject target = MCCRTestDataHelper.createObject(ds, CHANNEL_ID, 1, "1000.1", dataMap);

		// create another object in the channel, linking to the first object
		dataMap.clear();
		dataMap.put("text", "source");
		dataMap.put("link", target);
		MCCRObject source = MCCRTestDataHelper.createObject(ds, CHANNEL_ID, 2, "1000.2", dataMap);

		// set the master as current channel in the ds
		ds.setChannel(channelIdToSet);

		// get the set channels
		List<DatasourceChannel> before = ds.getChannels();

		// get the linked object from the source object
		Object linkTarget = source.get("link");
		assertNotNull("Link target is null", linkTarget);
		assertEquals("Check link target", target, linkTarget);

		// the list of set channels must not change
		List<DatasourceChannel> after = ds.getChannels();
		assertEquals("Check list of channel structures", before.size(), after.size());
		for (int i = 0; i < before.size(); i++) {
			assertEquals("Check channel #" + i, before.get(i).getId(), after.get(i).getId());
		}
	}
}
