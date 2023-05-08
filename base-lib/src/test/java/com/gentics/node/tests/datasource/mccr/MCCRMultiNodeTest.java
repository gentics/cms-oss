package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.portalconnector.tests.AssertionAppender;
import com.gentics.testutils.infrastructure.TestEnvironment;
import org.junit.experimental.categories.Category;

/**
 * Test cases for using multiple node structures
 */
@Category(BaseLibTest.class)
public class MCCRMultiNodeTest {

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FILESYSTEM_BASEPATH = "./target/";

	/**
	 * Directory, in which the fs attributes are stored
	 */
	protected File attributeDirectory;

	/**
	 * Datasource instance
	 */
	protected WritableMCCRDatasource ds;

	/**
	 * Assertion appender
	 */
	private AssertionAppender appender;

	@Before
	public void setUp() throws Exception {
		attributeDirectory = new File(FILESYSTEM_BASEPATH, "mccrfstests_" + TestEnvironment.getRandomHash(8));
		attributeDirectory.mkdirs();
		assertTrue("Check existence of " + attributeDirectory.getAbsolutePath(), attributeDirectory.exists());
		FileUtil.cleanDirectory(attributeDirectory);

		appender = new AssertionAppender();
		NodeLogger.getRootLogger().removeAllAppenders();
		NodeLogger.getRootLogger().addAppender(appender);

		Map<String, String> handleProperties = new HashMap<String, String>();

		handleProperties.put("type", "jdbc");
		handleProperties.put("driverClass", "org.hsqldb.jdbcDriver");
		handleProperties.put("url", "jdbc:hsqldb:mem:" + getClass().getSimpleName());
		handleProperties.put("shutDownCommand", "SHUTDOWN");

		Map<String, String> dsProperties = new HashMap<String, String>();

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put(MCCRDatasource.ATTRIBUTE_PATH, attributeDirectory.getAbsolutePath());

		ds = (WritableMCCRDatasource) PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);
		assertNotNull("Datasource creation failed: " + appender.getErrors(), ds);

		// import test structure
		MCCRTestDataHelper.importTypes(ds);

		// create and save the channel structure
		ChannelTree tree = new ChannelTree();
		// first channel structure
		ChannelTreeNode master = new ChannelTreeNode(new DatasourceChannel(1, "Master 1"));

		tree.getChildren().add(master);
		ChannelTreeNode channel = new ChannelTreeNode(new DatasourceChannel(2, "Channel 1"));

		master.getChildren().add(channel);

		// second channel structure
		master = new ChannelTreeNode(new DatasourceChannel(3, "Master 2"));
		tree.getChildren().add(master);
		channel = new ChannelTreeNode(new DatasourceChannel(4, "Channel 2"));
		master.getChildren().add(channel);

		// save the tree
		ds.saveChannelStructure(tree);

		// store some data
		createTestData();
	}

	/**
	 * Teardown
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		PortalConnectorFactory.destroy();
		FileUtil.deleteDirectory(attributeDirectory);
	}

	/**
	 * Test selecting objects from different nodes
	 * @throws Exception
	 */
	@Test
	public void testSelectCrossNode() throws Exception {
		String filterExpr = "object.obj_type == 1000";
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse(filterExpr));

		// filter without selecting channels
		List<Resolvable> result = new ArrayList<Resolvable>(
				ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_ASC)}));

		assertEquals("Check # of filtered objects", 2, result.size());
		assertEquals("Check first object", "Object in first Master", result.get(0).get("text"));
		assertEquals("Check second object", "Object in second Master", result.get(1).get("text"));

		// select a channel
		ds.setChannel(2);
		// filter again
		result = new ArrayList<Resolvable>(
				ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_ASC)}));
		assertEquals("Check # of filtered objects", 2, result.size());
		assertEquals("Check first object", "Object in first Channel", result.get(0).get("text"));
		assertEquals("Check second object", "Object in second Master", result.get(1).get("text"));

		// select another channel
		ds.setChannel(4);
		// filter again
		result = new ArrayList<Resolvable>(
				ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_ASC)}));
		assertEquals("Check # of filtered objects", 2, result.size());
		assertEquals("Check first object", "Object in first Channel", result.get(0).get("text"));
		assertEquals("Check second object", "Object in second Channel", result.get(1).get("text"));

		// select a master
		ds.setChannel(1);
		// filter again
		result = new ArrayList<Resolvable>(
				ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_ASC)}));
		assertEquals("Check # of filtered objects", 2, result.size());
		assertEquals("Check first object", "Object in first Master", result.get(0).get("text"));
		assertEquals("Check second object", "Object in second Channel", result.get(1).get("text"));
	}

	/**
	 * Create test data
	 * @throws Exception
	 */
	protected void createTestData() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		// store in first channel structure
		data.put("text", "Object in first Master");
		MCCRTestDataHelper.createObject(ds, 1, 1, "1000.1", data);
		data.put("text", "Object in first Channel");
		MCCRTestDataHelper.createObject(ds, 2, 1, "1000.2", data);

		// store in second channel structure
		data.put("text", "Object in second Master");
		MCCRTestDataHelper.createObject(ds, 3, 2, "1000.3", data);
		data.put("text", "Object in second Channel");
		MCCRTestDataHelper.createObject(ds, 4, 2, "1000.4", data);
	}
}
