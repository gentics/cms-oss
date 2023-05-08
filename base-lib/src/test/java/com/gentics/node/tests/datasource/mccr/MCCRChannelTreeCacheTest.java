package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.lib.db.DB;
import com.gentics.node.testutils.QueryCounter;
import org.junit.experimental.categories.Category;

/**
 * Test cases for caching the channel tree
 */
@Category(BaseLibTest.class)
public class MCCRChannelTreeCacheTest extends AbstractMCCRCacheTest {

	/**
	 * Maximum level of children
	 */
	public final static int MAXLEVEL = 3;

	/**
	 * Number of children per level
	 */
	public final static int CHILDRENPERLEVEL = 3;

	/**
	 * Id Generator
	 */
	protected IdGenerator idGen = new IdGenerator();

	@Override
	public void setUp() throws Exception {
		super.setUp();

		// for this test cases, we set a different (more complex) channel tree
		ChannelTree tree = new ChannelTree();

		createChannelTree(tree.getChildren(), 0, "Channel");
		ds.saveChannelStructure(tree);
		ds.clearCaches();
	}

	/**
	 * Test whether setting the channel ids works and everything is cached
	 * @throws Exception
	 */
	@Test
	public void testSetChannelId() throws Exception {
		// read the channel structure once to have it cached
		ds.getChannelStructure();

		QueryCounter counter = new QueryCounter(false, true);

		// now set all channels and check whether no SQL statement is issued
		for (int channelId = 1; channelId <= idGen.counter; channelId++) {
			DatasourceChannel channel = ds.setChannel(channelId);

			assertEquals("Check id of set channel", channelId, channel.getId());
		}

		// make assertions
		assertEquals("Check # of DB statements", 0, counter.getCount());
	}

	/**
	 * Test whether clearing the caches clears the cached tree
	 * @throws Exception
	 */
	@Test
	public void testCacheClear() throws Exception {
		// read the channel structure once to have it cached
		ds.getChannelStructure();

		// clear the caches
		ds.clearCaches();

		// modify the data in the DB
		DB.update(ds.getHandle(), "UPDATE channel SET name = CONCAT('mod', name)");

		// get the tree again
		QueryCounter counter = new QueryCounter(false, true);
		ChannelTree tree = ds.getChannelStructure();

		// make assertions
		assertEquals("Check # of DB statements", 1, counter.getCount());

		// all names must be modified now
		checkModifiedNames(tree.getChildren());

	}

	/**
	 * Recursively check the names of the channels
	 * @param children list of children
	 * @throws Exception
	 */
	protected void checkModifiedNames(List<ChannelTreeNode> children) throws Exception {
		for (ChannelTreeNode node : children) {
			assertTrue("Name of the channel must be modified", node.getChannel().getName().startsWith("mod"));
			checkModifiedNames(node.getChildren());
		}
	}

	/**
	 * Test whether saving a different channel structure will correctly put it into the cache
	 * @throws Exception
	 */
	@Test
	public void testCacheClearOnSave() throws Exception {
		// read the channel structure once to have it cached
		ds.getChannelStructure();

		// create a different tree (different names and ids)
		ChannelTree tree = new ChannelTree();

		createChannelTree(tree.getChildren(), 0, "Modified");
		ds.saveChannelStructure(tree);

		// now get the channel structure again (new one must be cached now)
		QueryCounter counter = new QueryCounter(false, true);
		ChannelTree newTree = ds.getChannelStructure();

		// make assertions
		assertEquals("Check # of DB statements", 0, counter.getCount());
		assertTrue("Stored channel tree does not match the expected", tree.equals(newTree));
	}

	/**
	 * Recursively create a channel tree
	 * @param children list of children to be modified
	 * @param level current level (starting with 0)
	 * @param prefix current prefix
	 * @param idGen id generator
	 */
	protected void createChannelTree(List<ChannelTreeNode> children, int level, String prefix) {
		if (level < MAXLEVEL) {
			for (int i = 1; i <= CHILDRENPERLEVEL; i++) {
				String channelName = prefix + "-" + i;
				ChannelTreeNode treeNode = new ChannelTreeNode(new DatasourceChannel(idGen.get(), channelName));

				children.add(treeNode);
				createChannelTree(treeNode.getChildren(), level + 1, channelName);
			}
		}
	}

	/**
	 * Internal class to generate unique IDs
	 */
	protected class IdGenerator {

		/**
		 * Internal counter
		 */
		protected int counter = 0;

		/**
		 * Get the next ID
		 * @return next ID
		 */
		public int get() {
			return ++counter;
		}
	}
}
