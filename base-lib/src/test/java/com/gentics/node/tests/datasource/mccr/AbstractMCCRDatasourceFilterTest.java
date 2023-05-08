package com.gentics.node.tests.datasource.mccr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.datasource.ChannelTree;
import com.gentics.api.lib.datasource.ChannelTreeNode;
import com.gentics.api.lib.datasource.DatasourceChannel;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.datasource.mccr.MCCRHelper;
import com.gentics.lib.datasource.mccr.MCCRObject;
import com.gentics.lib.datasource.mccr.WritableMCCRDatasource;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectTypeBean;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;

/**
 * Test cases for filtering MCCRDatasources
 */
public class AbstractMCCRDatasourceFilterTest extends AbstractSingleVariationDatabaseTest {

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(AbstractMCCRDatasourceFilterTest.class);

	/**
	 * Tested filters
	 */
	protected final static List<FilterTest> FILTERTESTS = new Vector<FilterTest>();

	/**
	 * Number of objects
	 */
	public final static int NUM_OBJECTS = 10;

	/**
	 * Base path for filesystem attributes
	 */
	public final static String FS_BASEPATH = "/tmp/MCCRDatasourceFilterTest";

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * Currently used datasource instance
	 */
	protected WritableMCCRDatasource ds;

	/**
	 * Channel ids
	 */
	protected static List<Integer> channels = new Vector<Integer>();

	/**
	 * Object types
	 */
	protected static Map<Integer, ObjectTypeBean> types;

	/**
	 * Filter test
	 */
	protected FilterTest filterTest;

	static {
		try {
			types = MCCRTestDataHelper.loadObjectTypes(MCCRTest.class.getResourceAsStream("mccr_tests_structure.xml"));
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * Clean the databases when finished with all tests
	 * @throws Exception
	 */
	@AfterClass
	public static void tearDownOnce() throws Exception {
		logger.debug("Cleanup of test databases.");
		PortalConnectorFactory.destroy();
		for (TestDatabase testDatabase : databases.values()) {
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			sqlUtils.connectDatabase();
			sqlUtils.removeDatabase();
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Create an instance to test the filter on the given datasource
	 * @param testDatabase test database
	 * @param filterTest filter test
	 */
	public AbstractMCCRDatasourceFilterTest(TestDatabase testDatabase, FilterTest filterTest) {
		super(testDatabase);
		this.filterTest = filterTest;
	}

	/**
	 * Setup the test database if not done before
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		TestDatabase testDatabase = getTestDatabase();

		if (!databases.containsKey(testDatabase.getIdentifier())) {
			databases.put(testDatabase.getIdentifier(), testDatabase);
			logger.debug("Creating test database.");
			SQLUtils sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);

			logger.debug("Created database with name: {" + sqlUtils.getTestDatabase().getDBName() + "}");

			sqlUtils.connectDatabase();
			testDatabase.setRandomDatabasename(getClass().getSimpleName());
			sqlUtils.createDatabase();
			
			ds = createDataSource();
			MCCRTestDataHelper.importTypes(ds);

			createChannelStructure();

			// make sure that the filesystem basepath exists and is empty
			File basePath = new File(FS_BASEPATH);

			basePath.mkdirs();
			FileUtil.cleanDirectory(basePath);

			// create the test data
			createTestData();

			sqlUtils.disconnectDatabase();
		} else {
			// we need to set the datasource in any case
			ds = createDataSource();
		}

		ds.clearCaches();
	}

	/**
	 * Test the filter
	 * @throws Exception
	 */
	@Test
	public void testFilter() throws Exception {
		DatasourceFilter filter = filterTest.getFilter(ds);
		Expression expression = filterTest.getEvalExpression();
		ExpressionEvaluator evaluator = filterTest.getEvaluator();

		// select for all channels
		for (int channelId : channels) {
			ds.setChannel(channelId);
			Collection<Resolvable> result = ds.getResult(filter, null);

			// check number of filtered objects
			assertEquals("Check # of filtered objects for channels " + ds.getChannels(), filterTest.getExpectedResultSize().get(channelId).intValue(),
					result.size());

			// check count with filter
			assertEquals("Check # of counted objects for channels " + ds.getChannels(), filterTest.getExpectedResultSize().get(channelId).intValue(),
					ds.getCount(filter));

			// check that all objects are from correct channel
			for (Resolvable res : result) {
				if (res instanceof MCCRObject) {
					assertEquals("Check channelId of object ", channelId, ((MCCRObject) res).getChannelId());
				} else {
					fail("Object is of unexpected type " + res.getClass());
				}

				if (filterTest.isEval()) {
					// check whether the object matches the given expression
					assertTrue("Object " + res + " must match the expression in channel " + channelId, evaluator.match(expression, res));
				}
			}
		}
	}

	/**
	 * Create channel structure for the tests: master -> channel1 -> channel2
	 * 
	 * @throws Exception
	 */
	protected void createChannelStructure() throws Exception {
		ChannelTree tree = new ChannelTree();
		
		ChannelTreeNode master = new ChannelTreeNode(new DatasourceChannel(1, "Master"));

		tree.getChildren().add(master);
		channels.add(master.getChannel().getId());
		
		ChannelTreeNode channel1 = new ChannelTreeNode(new DatasourceChannel(2, "Channel 1"));

		master.getChildren().add(channel1);
		channels.add(channel1.getChannel().getId());
		
		ChannelTreeNode channel2 = new ChannelTreeNode(new DatasourceChannel(3, "Channel 2"));

		channel1.getChildren().add(channel2);
		channels.add(channel2.getChannel().getId());
		
		ds.saveChannelStructure(tree);
	}

	/**
	 * Create the test data
	 * @throws Exception
	 */
	protected void createTestData() throws Exception {
		for (int channelId : channels) {
			// create the objects
			List<MCCRObject> objects = new ArrayList<MCCRObject>();

			for (int i = 1; i <= NUM_OBJECTS; i++) {
				objects.add(MCCRTestDataHelper.createObject(ds, channelId, i, "1000." + i, null));
			}

			// setup the link targets
			MCCRTestDataHelper.setupLinkTargets(channelId, objects);

			// now set the attributes
			int i = 1;

			for (MCCRObject obj : objects) {
				for (String attribute : MCCRTestDataHelper.ATTRIBUTE_NAMES) {
					obj.setProperty(attribute, MCCRTestDataHelper.getValue(MCCRHelper.getAttributeType(ds, 1000, attribute), channelId, i));
				}

				i++;
			}
			ds.store(objects);
		}
	}

	/**
	 * Create a multichannelling datasource
	 * @return datasource
	 */
	protected WritableMCCRDatasource createDataSource() {
		Map<String, String> handleProperties = testDatabase.getSettingsMap();

		handleProperties.put("type", "jdbc");

		Map<String, String> dsProperties = new HashMap<String, String>(handleProperties);

		dsProperties.put("autorepair2", "true");
		dsProperties.put("sanitycheck2", "true");
		dsProperties.put(MCCRDatasource.ATTRIBUTE_PATH, FS_BASEPATH);

		WritableMCCRDatasource ds = null;

		// Get writable datasource
		ds = (WritableMCCRDatasource) PortalConnectorFactory.createWritableMultichannellingDatasource(handleProperties, dsProperties);

		if (ds == null) {
			fail("Creation of the datasource failed");
			return null;
		} else {
			return ds;
		}
	}

	/**
	 * Get the comparison operator for the attribute. For singlevalue, this returns "==" for multivalue "CONTAINSONEOF"
	 * @param attrType attribute type
	 * @return operator
	 */
	protected static String getComparisonOperator(ObjectAttributeBean attrType) {
		return attrType.getMultivalue() ? "CONTAINSONEOF" : "==";
	}

	/**
	 * Class for data resolver
	 */
	protected static class DataResolver implements Resolvable {

		/**
		 * Attribute
		 */
		protected ObjectAttributeBean attr;

		/**
		 * Channel ID
		 */
		protected int channelId;

		/**
		 * Object Index
		 */
		protected int index;

		/**
		 * Create a data resolver for the given attribute
		 * @param attr attribute
		 * @param channelId channel ID
		 * @param index object index
		 */
		public DataResolver(ObjectAttributeBean attr, int channelId, int index) {
			this.attr = attr;
			this.channelId = channelId;
			this.index = index;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
		 */
		public boolean canResolve() {
			return true;
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
		 */
		public Object getProperty(String key) {
			return get(key);
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		 */
		public Object get(String key) {
			try {
				if ("value".equals(key)) {
					return MCCRTestDataHelper.getValue(attr, channelId, index);
				} else {
					return null;
				}
			} catch (Exception e) {
				return null;
			}
		}
	}

	/**
	 * Resolver, that will resolve a list of sub resolvers
	 */
	protected static class ListResolver implements Resolvable {

		/**
		 * List of resolvers
		 */
		protected List<Resolvable> resolverList = new ArrayList<Resolvable>();

		/**
		 * Create an instance with a list of resolvables
		 * @param resolvables list of resolvables
		 */
		public ListResolver(Resolvable... resolvables) {
			for (Resolvable res : resolvables) {
				resolverList.add(res);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
		 */
		public Object getProperty(String key) {
			return get(key);
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		 */
		public Object get(String key) {
			int index = ObjectTransformer.getInt(key, -1);

			if (index < 0 || index >= resolverList.size()) {
				return null;
			} else {
				return resolverList.get(index);
			}
		}

		/* (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
		 */
		public boolean canResolve() {
			return true;
		}
	}

	/**
	 * Class for a filter test
	 */
	protected static class FilterTest {

		/**
		 * Expected result size per channel
		 */
		protected Map<Integer, Integer> expectedResultSize = new HashMap<Integer, Integer>();

		/**
		 * Filter String
		 */
		protected String expression;

		/**
		 * Optional static expression.
		 * This can be set for expressions that do not support static evaluation (like expressions containing the subrule function)
		 * The static expression must be compatible with the expression: All objects that are filtered with the expression must also match the static expression
		 */
		protected String staticExpression;

		/**
		 * Flag to mark, whether evaluation can be done for the filter
		 */
		protected boolean eval = true;

		/**
		 * Data resolver
		 */
		protected Resolvable dataResolver;

		/**
		 * Data map
		 */
		protected Map<String, Object> dataMap = new HashMap<String, Object>();

		/**
		 * Create an instance for the filter
		 * @param expression filter expression
		 */
		protected FilterTest(String expression) {
			this.expression = expression;
		}

		/**
		 * Create an instance for the given filter
		 * @param expression filter expression
		 * @return instance
		 */
		public static FilterTest create(String expression) {
			FilterTest test = new FilterTest(expression);

			return test;
		}

		/**
		 * Set an optional static expression for evaluation
		 * @param staticExpression static expression
		 * @return this instance
		 */
		public FilterTest eval(String staticExpression) {
			this.staticExpression = staticExpression;
			return this;
		}

		/**
		 * Set whether evaluation can be done
		 * @param eval true for evaluation, false for not evaluation
		 * @return this instance
		 */
		public FilterTest eval(boolean eval) {
			this.eval = eval;
			return this;
		}

		/**
		 * Set the expected # of results for a given channel
		 * @param channelId channel id
		 * @param numResults expected # of results
		 * @return this instance
		 */
		public FilterTest expect(int channelId, int numResults) {
			expectedResultSize.put(channelId, numResults);
			return this;
		}

		/**
		 * Add some resolvable data (will be available in the filter as data.[key])
		 * @param dataResolver resolver, that will resolve "data.[key]"
		 * @return this instance
		 */
		public FilterTest data(Resolvable dataResolver) {
			this.dataResolver = dataResolver;
			return this;
		}

		/**
		 * Add some resolvable data, which will be available as mapdata.[key]
		 * @param key key
		 * @param value value
		 * @return this instance
		 */
		public FilterTest data(String key, Object value) {
			dataMap.put(key, value);
			return this;
		}

		/**
		 * Get the map containing the expected result sizes
		 * @return expected result size map
		 */
		public Map<Integer, Integer> getExpectedResultSize() {
			return expectedResultSize;
		}

		/**
		 * Get the filter expression as string
		 * @return filter expression
		 */
		public String getExpressionString() {
			return expression;
		}

		@Override
		public String toString() {
			return expression;
		}

		/**
		 * Get the filter for the given datasource
		 * @param ds datasource
		 * @return filter
		 * @throws ExpressionParserException
		 * @throws ParserException
		 */
		public DatasourceFilter getFilter(WritableMCCRDatasource ds) throws ExpressionParserException, ParserException {
			DatasourceFilter filter = ds.createDatasourceFilter(getExpression());

			if (dataResolver != null) {
				filter.addBaseResolvable("data", dataResolver);
			}
			filter.addBaseResolvable("mapdata", new MapResolver(dataMap));
			return filter;
		}

		/**
		 * Get the expression
		 * @return expression
		 * @throws ParserException
		 */
		public Expression getExpression() throws ParserException {
			return ExpressionParser.getInstance().parse(expression);
		}

		/**
		 * Get an expression instance for evaluation.
		 * The expression is either the static expression (if one set) or the tested filter expression
		 * @return expression for evaluation
		 * @throws ParserException
		 */
		public Expression getEvalExpression() throws ParserException {
			if (staticExpression != null) {
				return ExpressionParser.getInstance().parse(staticExpression);
			} else {
				return getExpression();
			}
		}

		/**
		 * Get an expression evaluator
		 * @return expression evaluator
		 * @throws InsufficientPrivilegesException
		 */
		public ExpressionEvaluator getEvaluator() throws InsufficientPrivilegesException {
			ExpressionEvaluator evaluator = new ExpressionEvaluator();

			if (dataResolver != null) {
				evaluator.setProperty("data", dataResolver);
			}
			evaluator.setProperty("mapdata", new MapResolver(dataMap));
			return evaluator;
		}

		/**
		 * Get true if evaluation can be done, false if not
		 * @return true for evaluation, false otherwise
		 */
		public boolean isEval() {
			return eval;
		}

		/**
		 * Check whether the test is restricted for the given test DB
		 * @param testDB test DB
		 * @return true if the test is restricted, false if not
		 */
		public boolean isRestricted(TestDatabase testDB) {
			// Oracle cannot filter for blobs
			return testDB.getType() == TestDatabase.ORACLE && expression.contains(".blob");
		}
	}
}
