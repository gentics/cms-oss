package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.MultichannellingDatasource;
import com.gentics.api.lib.datasource.WritableMultichannellingDatasource;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.AbstractContentRepositoryStructure;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.SimpleResultProcessor;
import com.gentics.lib.db.SimpleResultRow;
import com.gentics.lib.log.NodeLogger;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Test cases for multichannelling aware content repositories
 */
@Category(BaseLibTest.class)
public class MCCRTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Assertion appender
	 */
	private AssertionAppender appender;

	/**
	 * SQL Utilities
	 */
	private SQLUtils sqlUtils;

	/**
	 * Get the test parameters
	 * @return test parameters
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(
				getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS, TestDatabaseVariationConfigurations.MSSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.ORACLE_VARIATIONS));
	}

	/**
	 * Create an instance with the given test database
	 * @param testDatabase
	 */
	public MCCRTest(TestDatabase testDatabase) {
		super(testDatabase);
		appender = new AssertionAppender();
		NodeLogger.getRootLogger().addAppender(appender);
	}

	@Before
	public void setUp() throws Exception {
		appender.reset();

		TestDatabase testDatabase = getTestDatabase();

		sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
		sqlUtils.connectDatabase();
		testDatabase.setRandomDatabasename(getClass().getSimpleName());
		sqlUtils.createDatabase();
	}

	/**
	 * Test creation of the datasource
	 */
	@Test
	public void testCreateDatasource() throws Exception {
		WritableMultichannellingDatasource ds = createWritableDataSource();

		AbstractContentRepositoryStructure checkStructure = AbstractContentRepositoryStructure.getStructure(ds, "");

		appender.reset();
		if (!checkStructure.checkStructureConsistency(false)) {
			fail("Structure did not pass the test: " + appender.getErrors());
		}
	}

	/**
	 * Test saving and retrieving channel structure
	 */
	@Test
	public void testSavingChannelStructure() throws Exception {
		WritableMCCRDatasource writableDS = (WritableMCCRDatasource) createWritableDataSource();

		// Test with correct data
		List<ChannelTree> correctChannelTrees = createCorrectChannelTreeVariations();

		for (ChannelTree channelTree : correctChannelTrees) {
			writableDS.saveChannelStructure(channelTree);

			ChannelTree retrievedChannelTree = writableDS.getChannelStructure();

			// Assert that the initial tree is the same as the new tree
			assertTrue("Saved and retrieved trees are not the same.", channelTree.equals(retrievedChannelTree));

			// Clear channel data.
			DB.update(writableDS.getHandle(), "DELETE FROM channel");
		}

		// Test with incorrect data
		List<ChannelTree> brokenChannelTrees = createBrokenChannelTreeVariations();

		for (ChannelTree channelTree : brokenChannelTrees) {
			boolean exceptionProduced = false;

			try {
				writableDS.saveChannelStructure(channelTree);
			} catch (DatasourceException e) {
				exceptionProduced = true;
			}

			// Assert that the exception is produced by the incorrect channelTree structure we're trying to save
			assertTrue("Exception is not produced by incorrect channel tree.", exceptionProduced);

			// Clear channel data.
			DB.update(writableDS.getHandle(), "DELETE FROM channel");
		}
	}

	/**
	 * Test updating channel structure
	 */
	@Test
	public void testUpdateChannelStructure() throws Exception {
		WritableMCCRDatasource writableDS = (WritableMCCRDatasource) createWritableDataSource();

		List<ChannelTree> variations = createUpdateChannelTreeVariations();

		for (ChannelTree channelTree : variations) {

			// Save initial channel structure
			writableDS.saveChannelStructure(createInitialUpdateChannelTree());
			assertTrue("Initial channel structure is not the same with the generated initial channel structure.",
					writableDS.getChannelStructure().equals(createInitialUpdateChannelTree()));

			// Update the channel structure with the current changed structure
			writableDS.saveChannelStructure(channelTree);
			ChannelTree retrievedChannelTree = writableDS.getChannelStructure();

			assertTrue("Updated channel structure is not the same with the saved channel structure.", channelTree.equals(retrievedChannelTree));

			// Run data consistency checks
			checkChannelStructureDataConsistency(writableDS);

			// Clear channel data.
			DB.update(writableDS.getHandle(), "DELETE FROM channel");
		}
	}

	/**
	 * Test getting/setting current channel.
	 */
	@Test
	public void testGettingSettingCurrentChannel() throws Exception {
		WritableMultichannellingDatasource ds = createWritableDataSource();

		// Set a non-existing channel - we haven't set channel structure yet.
		boolean exceptionThrowed = false;

		try {
			ds.setChannel(1);
		} catch (DatasourceException e) {
			exceptionThrowed = true;
		}

		assertTrue("We set the non-existing channel.", exceptionThrowed);

		// Set channel structure
		ChannelTree tree = new ChannelTree();
		ChannelTreeNode rootNode = new ChannelTreeNode(new DatasourceChannel(1, "root"));

		rootNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(2, "channel")));
		tree.getChildren().add(rootNode);

		ds.saveChannelStructure(tree);

		// check the initial selection
		assertSelectedChannels(ds, 1);

		// Set the channel
		DatasourceChannel channel = ds.setChannel(2);

		assertEquals("Check the set channel ID", 2, channel.getId());

		// check the selection now
		assertSelectedChannels(ds, 2);
	}

	/**
	 * Test setting multiple current channels (when using multiple channel structures)
	 * @throws Exception
	 */
	@Test
	public void testSettingMultipleCurrentChannels() throws Exception {
		WritableMultichannellingDatasource ds = createWritableDataSource();

		// Set channel structure (two root nodes, having two parallel channels each)
		ChannelTree tree = new ChannelTree();
		ChannelTreeNode rootNode = new ChannelTreeNode(new DatasourceChannel(1, "root1"));

		rootNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(2, "root1-channel1")));
		rootNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(3, "root1-channel2")));
		tree.getChildren().add(rootNode);
		rootNode = new ChannelTreeNode(new DatasourceChannel(4, "root2"));
		rootNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(5, "root2-channel1")));
		rootNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(6, "root2-channel2")));
		tree.getChildren().add(rootNode);

		ds.saveChannelStructure(tree);

		// get the current channels (must be 1 and 4)
		assertSelectedChannels(ds, 1, 4);

		// select channel 6 (must replace 4)
		ds.setChannel(6);
		assertSelectedChannels(ds, 1, 6);

		// select channel 5 (must replace 6)
		ds.setChannel(5);
		assertSelectedChannels(ds, 1, 5);

		// select channel 4 (must replace 5)
		ds.setChannel(4);
		assertSelectedChannels(ds, 1, 4);

		// select channel 3 (must replace 1)
		ds.setChannel(3);
		assertSelectedChannels(ds, 3, 4);

		// select channel 2 (must replace 3)
		ds.setChannel(2);
		assertSelectedChannels(ds, 2, 4);

		// select channel 1 (must replace 2)
		ds.setChannel(1);
		assertSelectedChannels(ds, 1, 4);
	}

	/**
	 * Assert the given channel ids as selected
	 * @param ds datasource
	 * @param channelIds expected selected channel ids
	 * @throws Exception
	 */
	private void assertSelectedChannels(MultichannellingDatasource ds, int... channelIds) throws Exception {
		List<DatasourceChannel> channels = ds.getChannels();

		assertEquals("Check # of selected channels", channelIds.length, channels.size());

		Map<Integer, Boolean> selectedChannelIds = new HashMap<Integer, Boolean>(channelIds.length);

		for (int channelId : channelIds) {
			selectedChannelIds.put(channelId, false);
		}

		// iterate over all currently selected channels
		for (DatasourceChannel channel : channels) {
			// check whether the channel should really be selected
			assertTrue("Selected channel " + channel + " should not be selected", selectedChannelIds.containsKey(channel.getId()));
			// mark it as found
			selectedChannelIds.put(channel.getId(), true);
		}

		// check whether all expected channels have been selected
		for (Map.Entry<Integer, Boolean> entry : selectedChannelIds.entrySet()) {
			if (!entry.getValue()) {
				fail("Channel " + entry.getKey() + " was not selected");
			}
		}
	}

	/**
	 * Check channel data for consistency according MPTT.
	 *
	 * @param ds {@link CNWriteableDatasource} instance
	 * @throws SQLException
	 */
	private void checkChannelStructureDataConsistency(WritableMCCRDatasource ds) throws Exception {
		SimpleResultProcessor proc = new SimpleResultProcessor();

		DB.query(ds.getHandle(), "SELECT * FROM channel", proc);

		int minLeft = Integer.MAX_VALUE;
		int maxRight = Integer.MIN_VALUE;

		List<NodeBoundaries> channelIndex = new ArrayList<NodeBoundaries>(proc.size());
		HashSet<Integer> boundariesIndex = new HashSet<Integer>(2 * proc.size());
		HashSet<Integer> idIndex = new HashSet<Integer>(proc.size());

		for (SimpleResultRow row : proc.asList()) {
			int id = row.getInt("id");
			int left = row.getInt("mptt_left");
			int right = row.getInt("mptt_right");

			if (right > maxRight) {
				maxRight = right;
			}

			if (left < minLeft) {
				minLeft = left;
			}

			assertTrue("Found duplicated id: " + id, !idIndex.contains(id));
			assertTrue("Left boun should be always less then right: id - " + id + ", left - " + left + ", right - " + right, left < right);

			// Check for node boundaries intersection.
			for (NodeBoundaries boundaries : channelIndex) {
				boolean intersect = left < boundaries.left && right > boundaries.left && right < boundaries.right;

				assertTrue("Node with ID " + id + " is intersecting with node with ID " + boundaries.id, !intersect);

				intersect = left > boundaries.left && left < boundaries.right && right > boundaries.right;
				assertTrue("Node with ID " + id + " is intersecting with node with ID " + boundaries.id, !intersect);
			}

			idIndex.add(id);
			boundariesIndex.add(left);
			boundariesIndex.add(right);
			channelIndex.add(new NodeBoundaries(id, left, right));
		}

		assertTrue("This should not happen - left boundary should always be less than the right!", minLeft < maxRight);

		// Check if we have all numbers between minLeft and minRight boundary (specific to MPTT)
		for (int i = minLeft; i <= maxRight; i++) {
			assertTrue("The boundaries interval from min to max has gaps - not MPTT consistent.", boundariesIndex.contains(i));
		}
	}

	/**
	 * Store node's left and right boundaries + the ID of the node.
	 */
	private static class NodeBoundaries {
		int left;
		int right;
		int id;

		public NodeBoundaries(int id, int left, int right) {
			this.id = id;
			this.left = left;
			this.right = right;
		}
	}

	/**
	 * Create initial channel tree for update
	 *
	 * @return
	 */
	private ChannelTree createInitialUpdateChannelTree() {
		ChannelTreeNode node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		ChannelTreeNode node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		ChannelTreeNode node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		ChannelTreeNode node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		ChannelTreeNode node5 = new ChannelTreeNode(new DatasourceChannel(5, "node5"));
		ChannelTreeNode node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));
		ChannelTreeNode node7 = new ChannelTreeNode(new DatasourceChannel(7, "node7"));
		ChannelTreeNode node8 = new ChannelTreeNode(new DatasourceChannel(8, "node8"));

		node1.getChildren().add(node3);
		node1.getChildren().add(node4);
		node2.getChildren().add(node5);
		node2.getChildren().add(node6);
		node5.getChildren().add(node7);
		node5.getChildren().add(node8);

		ChannelTree tree = new ChannelTree();

		tree.getChildren().add(node1);
		tree.getChildren().add(node2);

		return tree;
	}

	/**
	 * Create data variations for update channel structure test.
	 *
	 * @return List of {@link ChannelTree} instances.
	 */
	private List<ChannelTree> createUpdateChannelTreeVariations() {
		List<ChannelTree> variations = new ArrayList<ChannelTree>();

		ChannelTree initial = null;

		// First test with empty channel tree
		variations.add(new ChannelTree());

		// Update to the same channel tree
		variations.add(createInitialUpdateChannelTree());

		// Remove leaf node
		initial = createInitialUpdateChannelTree();
		ChannelNodeHolder node8 = findNode(initial, 8);

		node8.parentNode.getChildren().remove(node8.targetNode);

		variations.add(initial);

		// Add leaf node
		initial = createInitialUpdateChannelTree();
		node8 = findNode(initial, 8);
		node8.targetNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(9, "node9")));

		variations.add(initial);

		// Remove first level node (sub-tree)
		initial = createInitialUpdateChannelTree();
		ChannelNodeHolder node1 = findNode(initial, 1);

		initial.getChildren().remove(node1.targetNode);

		variations.add(initial);

		// Add first level node between root and current first level node (insert in between)
		initial = createInitialUpdateChannelTree();
		node1 = findNode(initial, 1);

		ChannelTreeNode node9 = new ChannelTreeNode(new DatasourceChannel(9, "node9"));

		node9.getChildren().add(node1.targetNode);
		initial.getChildren().remove(node1.targetNode);
		initial.getChildren().add(node9);

		variations.add(initial);

		// Move leaf node to another level
		initial = createInitialUpdateChannelTree();
		node8 = findNode(initial, 8);
		node1 = findNode(initial, 1);

		node8.parentNode.getChildren().remove(node8.targetNode);
		node8.targetNode.getChildren().add(new ChannelTreeNode(new DatasourceChannel(9, "node9")));
		node1.targetNode.getChildren().add(node8.targetNode);

		variations.add(initial);

		// Move sub-tree to another level (no insertion, just append)
		initial = createInitialUpdateChannelTree();
		node1 = findNode(initial, 1);
		ChannelNodeHolder node5 = findNode(initial, 5);

		node1.targetNode.getChildren().add(node5.targetNode); // add node5 to node1
		node5.parentNode.getChildren().remove(node5.targetNode); // remove node5 from its parent

		variations.add(initial);

		// Move sub-tree to another level (insert it in between another sub-tree)
		initial = createInitialUpdateChannelTree();
		node1 = findNode(initial, 1);
		ChannelNodeHolder node2 = findNode(initial, 2);
		ChannelNodeHolder node3 = findNode(initial, 3);
		ChannelNodeHolder node6 = findNode(initial, 6);

		node1.targetNode.getChildren().add(node2.targetNode); // add node2 to node1
		initial.getChildren().remove(node2.targetNode); // remove node2 from the first level children

		node1.targetNode.getChildren().remove(node3.targetNode); // remove node3 from node1
		node6.targetNode.getChildren().add(node3.targetNode); // add node3 to node6 (leaf node of node2 sub-tree)

		variations.add(initial);

		return variations;
	}

	/**
	 * Find node in {@link ChannelTree} structure by it's ID.
	 *
	 * @param tree {@link ChannelTree} instance.
	 * @param id ID of the node
	 * @return {@link ChannelNodeHolder} instance.
	 */
	private ChannelNodeHolder findNode(ChannelTree tree, int id) {
		for (ChannelTreeNode node : tree.getChildren()) {
			ChannelNodeHolder result = findNodeInChannelTreeNode(null, node, id);

			if (result != null) {
				return result;
			}
		}

		throw new IllegalStateException("Node with ID " + id + " is not found!");
	}

	/**
	 * Recursive function that finds a node in {@link ChannelTree} structure by it's ID.
	 *
	 * @param parent The parent of the current proccessed node, null of there is no parent.
	 * @param node Currently processed node.
	 * @param id The ID of the node we want to find.
	 * @return {@link ChannelNodeHolder} instance.
	 */
	private ChannelNodeHolder findNodeInChannelTreeNode(ChannelTreeNode parent, ChannelTreeNode node, int id) {
		if (node.getChannel().getId() == id) {
			return new ChannelNodeHolder(node, parent);
		}

		for (ChannelTreeNode childNode : node.getChildren()) {
			ChannelNodeHolder result = findNodeInChannelTreeNode(node, childNode, id);

			if (result != null) {
				return result;
			}
		}

		return null;
	}

	/**
	 * Utility class that holds node reference and this node's parent reference.
	 */
	private static class ChannelNodeHolder {
		ChannelTreeNode targetNode;
		ChannelTreeNode parentNode;

		public ChannelNodeHolder(ChannelTreeNode targetNode, ChannelTreeNode parentNode) {
			this.targetNode = targetNode;
			this.parentNode = parentNode;
		}
	}

	/**
	 * Create list of {@link ChannelTree} objects that will throw an exception when trying to save.
	 */
	private List<ChannelTree> createBrokenChannelTreeVariations() {
		List<ChannelTree> variations = new ArrayList<ChannelTree>();

		// Initialization data
		ChannelTree tree = null;

		ChannelTreeNode node0 = null;
		ChannelTreeNode node1 = null;
		ChannelTreeNode node2 = null;
		ChannelTreeNode node3 = null;
		ChannelTreeNode node4 = null;
		ChannelTreeNode node5 = null;
		ChannelTreeNode node6 = null;
		ChannelTreeNode node6v2 = null;

		// One zero element at first level
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));

		tree = new ChannelTree();
		tree.getChildren().add(node0);

		variations.add(tree);

		// One zero element at second level
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));

		node1.getChildren().add(node0);

		tree = new ChannelTree();
		tree.getChildren().add(node1);

		variations.add(tree);

		// One negative element at first level
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));

		tree = new ChannelTree();
		tree.getChildren().add(node5);

		variations.add(tree);

		// One negative element at second level
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));

		node1.getChildren().add(node5);

		tree = new ChannelTree();
		tree.getChildren().add(node1);

		variations.add(tree);

		// Multiple elements on multiple levels with zero element at first level
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));

		node2.getChildren().add(node1);
		node2.getChildren().add(node4);

		tree = new ChannelTree();
		tree.getChildren().add(node0); // <--
		tree.getChildren().add(node2);
		tree.getChildren().add(node3);

		variations.add(tree);

		// Multiple elements on multiple levels with negative element at first level
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));

		node2.getChildren().add(node3);
		node2.getChildren().add(node4);

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node5); // <--

		variations.add(tree);

		// Multiple elements on multiple levels with zero element at second level
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));

		node2.getChildren().add(node0); // <--
		node2.getChildren().add(node4);

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node3);

		variations.add(tree);

		// Multiple elements on multiple levels with negative element at second level
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));

		node2.getChildren().add(node5); // <--
		node2.getChildren().add(node4);

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node3);

		variations.add(tree);

		// Equal ids - single level
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));
		node6v2 = new ChannelTreeNode(new DatasourceChannel(6, "node6v2"));

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node6); // <--
		tree.getChildren().add(node6v2); // <--

		variations.add(tree);

		// Equal ids - second level
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));
		node6v2 = new ChannelTreeNode(new DatasourceChannel(6, "node6v2"));

		node1.getChildren().add(node6); // <--
		node1.getChildren().add(node6v2); // <--
		node1.getChildren().add(node4);

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node3);

		variations.add(tree);

		// Mixed condition - zero, negative and duplicated ids on first level
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));
		node6v2 = new ChannelTreeNode(new DatasourceChannel(6, "node6v2"));

		tree = new ChannelTree();
		tree.getChildren().add(node0); // <--
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node5); // <--
		tree.getChildren().add(node6); // <--
		tree.getChildren().add(node6v2); // <--

		variations.add(tree);

		// Mixed condition - zaro, negative and duplicated ids on second level
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));
		node6v2 = new ChannelTreeNode(new DatasourceChannel(6, "node6v2"));

		node1.getChildren().add(node0); // <--
		node1.getChildren().add(node5); // <--
		node1.getChildren().add(node6); // <--
		node1.getChildren().add(node6v2); // <--

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);

		variations.add(tree);

		// Mixed condition - zaro, negative and duplicated ids on multiple levels
		node0 = new ChannelTreeNode(new DatasourceChannel(0, "node0"));
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node5 = new ChannelTreeNode(new DatasourceChannel(-5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));
		node6v2 = new ChannelTreeNode(new DatasourceChannel(6, "node6v2"));

		node1.getChildren().add(node0); // <--
		node1.getChildren().add(node5); // <--

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node6); // <--
		tree.getChildren().add(node6v2); // <--

		variations.add(tree);

		return variations;
	}

	/**
	 * Create list of correct {@link ChannelTree} objects that should work correctly with
	 * {@link MultichannellingDatasource}
	 */
	private List<ChannelTree> createCorrectChannelTreeVariations() {
		List<ChannelTree> variations = new ArrayList<ChannelTree>();

		// Initialization data
		ChannelTree tree = null;

		ChannelTreeNode node1 = null;
		ChannelTreeNode node2 = null;
		ChannelTreeNode node3 = null;
		ChannelTreeNode node4 = null;
		ChannelTreeNode node5 = null;
		ChannelTreeNode node6 = null;

		// Empty tree
		variations.add(new ChannelTree());

		// Single child
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));

		tree = new ChannelTree();
		tree.getChildren().add(node1);

		variations.add(tree);

		// Multiple direct children - one layer
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node2);
		tree.getChildren().add(node3);
		tree.getChildren().add(node4);
		tree.getChildren().add(node5);
		tree.getChildren().add(node6);

		variations.add(tree);

		// Two levels with single node on each layer
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));

		node1.getChildren().add(node2);

		tree = new ChannelTree();
		tree.getChildren().add(node1);

		variations.add(tree);

		// Two levels with multiple nodes on the first and single or none or the second
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));

		node1.getChildren().add(node2);
		node3.getChildren().add(node4);

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node3);
		tree.getChildren().add(node5);
		tree.getChildren().add(node6);

		variations.add(tree);

		// Two levels with multiple nodes on each one
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));

		node1.getChildren().add(node2);
		node1.getChildren().add(node3);
		node4.getChildren().add(node5);
		node4.getChildren().add(node6);

		tree = new ChannelTree();
		tree.getChildren().add(node1);
		tree.getChildren().add(node4);

		variations.add(tree);

		// Two levels with single node on the first one and multiple on the second one
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));

		node1.getChildren().add(node2);
		node1.getChildren().add(node3);
		node1.getChildren().add(node4);
		node1.getChildren().add(node5);
		node1.getChildren().add(node6);

		tree = new ChannelTree();
		tree.getChildren().add(node1);

		variations.add(tree);

		// Multiple (>2) levels deep
		node1 = new ChannelTreeNode(new DatasourceChannel(1, "node1"));
		node2 = new ChannelTreeNode(new DatasourceChannel(2, "node2"));
		node3 = new ChannelTreeNode(new DatasourceChannel(3, "node3"));
		node4 = new ChannelTreeNode(new DatasourceChannel(4, "node4"));
		node5 = new ChannelTreeNode(new DatasourceChannel(5, "node5"));
		node6 = new ChannelTreeNode(new DatasourceChannel(6, "node6"));

		node1.getChildren().add(node2);
		node2.getChildren().add(node3);
		node3.getChildren().add(node4);
		node4.getChildren().add(node5);
		node5.getChildren().add(node6);

		tree = new ChannelTree();
		tree.getChildren().add(node1);

		variations.add(tree);

		return variations;
	}

	/**
	 * Create writable multichannelling datasource
	 * @return datasource
	 */
	private WritableMultichannellingDatasource createWritableDataSource() {
		return (WritableMultichannellingDatasource) createDataSource(true);
	}

	/**
	 * Create a multichannelling datasource
	 * @return datasource
	 */
	private MultichannellingDatasource createDataSource(boolean requestWritableDatasource) {
		Map<String, String> handleProperties = testDatabase.getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");

		MultichannellingDatasource ds = null;

		// Get normal or writable datasource
		if (requestWritableDatasource) {
			ds = PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);
		} else {
			ds = PortalConnectorFactory.createMultichannellingDatasource(handleProperties, dsProperties);
		}

		if (ds == null) {
			fail("Creation of the datasource failed: " + appender.getErrors());
			return null;
		} else {
			return ds;
		}
	}

	@After
	public void tearDown() throws SQLUtilException {
		PortalConnectorFactory.destroy();
		sqlUtils.removeDatabase();
		sqlUtils.disconnectDatabase();
	}
}
