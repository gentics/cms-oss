package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import com.gentics.api.lib.auth.GenticsUser;
import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableBean;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.lib.datasource.VersionedObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.node.testutils.QueryCounter;
import com.gentics.testutils.GenericTestUtils;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.AbstractSingleVariationDatabaseTest;

public class AbstractCNDatasourceFilterTest extends AbstractSingleVariationDatabaseTest {

	public final static Integer NODETYPE = new Integer(1);

	public final static Integer LEAFTYPE = new Integer(2);

	public Properties handleProperties;

	protected Changeable topNode;

	protected Changeable emptyNode;

	protected Changeable fullNode;

	protected Changeable subNode1;

	protected Changeable subNode2;

	protected Changeable leaf1;

	protected Changeable leaf2;

	protected Changeable leaf3;

	protected Changeable leaf4;

	protected Changeable leaf5;

	protected Changeable leaf6;

	protected Changeable leaf7;

	protected Changeable leaf8;

	protected Changeable newLeaf;

	protected GenticsUser tester = new DummyUser();

	protected String versioning = "false";

	/**
	 * The datasource under test
	 */
	protected CNWriteableDatasource ds;

	/**
	 * The non-cached instance of the datasource
	 */
	protected CNWriteableDatasource nonCachedDs;

	/**
	 * Another instance (using the same handle) with caching enabled
	 */
	protected CNWriteableDatasource cachedDs;

	/**
	 * Another instance (using the same hanel) with caching and syncchecking enabled
	 */
	protected CNWriteableDatasource syncCheckingDs;

	/**
	 * And yet another one... with autoprefetching of optimized attributes enabled
	 */
	protected CNWriteableDatasource autoprefetchDs;

	public final static String EMPTYRULE = "";

	/**
	 * timestamp constants for doing versioned modifications
	 */
	public static int TIMESTAMP_NOW = (int) (System.currentTimeMillis() / 1000);

	public static int TIMESTAMP_PREMODIFY = 1000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_ADDOBJECT = 2000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_LINKOBJECT = 3000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_REMOVELINK = 4000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_ADDATTRIBUTE = 5000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_MODIFYATTRIBUTE = 6000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_REMOVEATTRIBUTE = 7000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_REMOVEOBJECT = 8000 + TIMESTAMP_NOW;

	public static int TIMESTAMP_POSTMODIFY = 9000 + TIMESTAMP_NOW;

	public static int[] ALLTIMESTAMPS = new int[] { TIMESTAMP_PREMODIFY, TIMESTAMP_ADDOBJECT, TIMESTAMP_LINKOBJECT, TIMESTAMP_REMOVELINK,
			TIMESTAMP_ADDATTRIBUTE, TIMESTAMP_MODIFYATTRIBUTE, TIMESTAMP_REMOVEATTRIBUTE, TIMESTAMP_REMOVEOBJECT, TIMESTAMP_POSTMODIFY, -1 };

	/**
	 * logger
	 */
	public final static NodeLogger logger = NodeLogger.getNodeLogger(CNDatasourceFilterTest.class);

	/**
	 * maximum number of seconds, the transactional test will wait for its subthread to finish
	 * 
	 * @see #testTransactionalAccess()
	 */
	protected final static int TRANSACTIONTEST_MAXWAIT = 60;

	/**
	 * binary test data
	 */
	public final static byte[] BINARYDATA = "Who is General Failure and why is he reading my hard disk ?".getBytes();

	/**
	 * parent names for the sorting tests
	 */
	public final static String[] PARENTNAMES = new String[] { null, "fullnode", "fullnode", "fullnode", "fullnode", "subnode1", "subnode1",
			"subnode2", "subnode2", "topnode", "topnode", "topnode", "topnode" };

	/**
	 * parent names for the sorting tests on oracle (which sorts nulls last by default)
	 */
	public final static String[] PARENTNAMES_ORACLE = new String[] { "fullnode", "fullnode", "fullnode", "fullnode", "subnode1", "subnode1",
			"subnode2", "subnode2", "topnode", "topnode", "topnode", "topnode", null };

	/**
	 * object names for the sorting tests
	 */
	public final static String[] OBJECTNAMES = new String[] { "topnode", "subnode2", "subnode1", "leaf6", "leaf5", "leaf2", "leaf1", "leaf4",
			"leaf3", "leaf8", "leaf7", "fullnode", "emptynode" };

	/**
	 * object names for the sorting tests on oracle (which sorts nulls last by default)
	 */
	public final static String[] OBJECTNAMES_ORACLE = new String[] { "subnode2", "subnode1", "leaf6", "leaf5", "leaf2", "leaf1", "leaf4", "leaf3",
			"leaf8", "leaf7", "fullnode", "emptynode", "topnode" };

	public final static String SEARCHED_IN_LONG_VALUE = "This is the part we will search in the long value";

	public final static String LONG_VALUE = StringUtils.repeat("a", 10) + SEARCHED_IN_LONG_VALUE + StringUtils.repeat("b", 4000);

	/**
	 * Map of all used test databases
	 */
	public static Map<String, TestDatabase> databases = new HashMap<String, TestDatabase>();

	/**
	 * Constructor for the test
	 */
	public AbstractCNDatasourceFilterTest(TestDatabase testDatabase) throws Exception {
		super(testDatabase);
		// initialize the cache
		GenericTestUtils.initConfigPathForCache();
	}

	/**
	 * Create a node object
	 * 
	 * @param parent
	 *            parent node (may be null)
	 * @param name
	 *            name of the node
	 * @return the node object
	 * @throws DatasourceException
	 */
	protected Changeable createNode(Resolvable parent, String name) throws DatasourceException {
		return createNode(parent, name, -1);
	}

	/**
	 * Create a node object
	 * 
	 * @param parent
	 *            parent node (may be null)
	 * @param name
	 *            name of the node
	 * @param versionTimestamp
	 *            version timestamp
	 * @return the node object
	 * @throws DatasourceException
	 */
	protected Changeable createNode(Resolvable parent, String name, int versionTimestamp) throws DatasourceException {
		Map nodeData = new HashMap();

		nodeData.put("obj_type", NODETYPE);
		nodeData.put("name", name);
		nodeData.put("longname", LONG_VALUE);
		nodeData.put("shortname", name);
		if (parent != null) {
			nodeData.put("parentnode", parent);
			nodeData.put("quickparentnode", parent);
		}
		ds.setVersionTimestamp(versionTimestamp);
		Changeable node = ds.create(nodeData, versionTimestamp);

		// Changeable node = ds.create(nodeData);
		ds.store(Collections.singletonList(node), tester);
		return node;
	}

	/**
	 * Create a leaf object (current version)
	 * 
	 * @param node
	 *            parent node (must not be null)
	 * @param name
	 *            name of the leaf
	 * @return the leaf object
	 * @throws DatasourceException
	 */
	protected Changeable createLeaf(Resolvable node, String name) throws DatasourceException {
		return createLeaf(node, name, -1);
	}

	/**
	 * Create a leaf object
	 * 
	 * @param node
	 *            parent node (must not be null)
	 * @param name
	 *            name of the leaf
	 * @param versionTimestamp
	 *            version timestamp for the created object
	 * @return the leaf object
	 * @throws DatasourceException
	 */
	protected Changeable createLeaf(Resolvable node, String name, int versionTimestamp) throws DatasourceException {
		return createLeaf(node, name, versionTimestamp, null, null, 0);
	}

	/**
	 * Create a leaf object
	 * 
	 * @param node
	 *            parent node (must not be null)
	 * @param name
	 *            name of the leaf
	 * @param versionTimestamp
	 *            version timestamp for the created object
	 * @param aliases
	 *            values for the attribute "aliases" (may be null)
	 * @return the leaf object
	 * @throws DatasourceException
	 */
	protected Changeable createLeaf(Resolvable node, String name, int versionTimestamp, String[] aliases) throws DatasourceException {
		return createLeaf(node, name, versionTimestamp, aliases, null, 0);
	}

	/**
	 * Create a leaf object
	 * 
	 * @param node
	 *            parent node (must not be null)
	 * @param name
	 *            name of the leaf
	 * @param versionTimestamp
	 *            version timestamp for the created object
	 * @param aliases
	 *            values for the attribute "aliases" (may be null)
	 * @param binaryData
	 *            value for the attribute "binarydata" (may be null)
	 * @param optimizedInt
	 *            value for the attribute "optimizedint"
	 * @return the leaf object
	 * @throws DatasourceException
	 */
	protected Changeable createLeaf(Resolvable node, String name, int versionTimestamp, String[] aliases, byte[] binaryData, int optimizedInt)
			throws DatasourceException {
		Map nodeData = new HashMap();

		nodeData.put("obj_type", LEAFTYPE);
		nodeData.put("name", name);
		nodeData.put("longname", name);
		nodeData.put("parentnode", node);
		nodeData.put("quickparentnode", node);
		nodeData.put("shortname", name);
		nodeData.put("optimizedint", new Integer(optimizedInt));
		if (aliases != null) {
			nodeData.put("aliases", Arrays.asList(aliases));
		}
		if (binaryData != null) {
			nodeData.put("binarydata", binaryData);
		}
		ds.setVersionTimestamp(versionTimestamp);
		Changeable leaf = ds.create(nodeData, versionTimestamp);

		// Changeable leaf = ds.create(nodeData);
		ds.store(Collections.singletonList(leaf), tester);
		return leaf;
	}

	/**
	 * Store the object at the given timestamp
	 * 
	 * @param object
	 *            object to store
	 * @param versionTimestamp
	 *            version timestamp
	 * @throws DatasourceException
	 */
	protected void storeObject(Resolvable object, int versionTimestamp) throws DatasourceException {
		((VersionedObject) object).setVersionTimestamp(versionTimestamp);
		ds.setVersionTimestamp(versionTimestamp);
		ds.store(Collections.singletonList(object), tester);
	}

	/**
	 * Remove the given object at the timestamp
	 * 
	 * @param object
	 *            object to remove
	 * @param versionTimestamp
	 *            version timestamp
	 * @throws DatasourceException
	 */
	protected void removeObject(Resolvable object, int versionTimestamp) throws DatasourceException {
		((VersionedObject) object).setVersionTimestamp(versionTimestamp);
		ds.setVersionTimestamp(versionTimestamp);
		ds.delete(Collections.singletonList(object), tester);
	}

	/**
	 * Get the name of the given timestamp
	 * 
	 * @param timestamp
	 *            timestamp
	 * @return name of the timestamp or the value as string
	 */
	protected final static String getTimestampName(int timestamp) {
		if (timestamp == TIMESTAMP_NOW) {
			return "now";
		} else if (timestamp == TIMESTAMP_PREMODIFY) {
			return "premodify";
		} else if (timestamp == TIMESTAMP_ADDOBJECT) {
			return "addobject";
		} else if (timestamp == TIMESTAMP_LINKOBJECT) {
			return "linkobject";
		} else if (timestamp == TIMESTAMP_REMOVEOBJECT) {
			return "removeobject";
		} else if (timestamp == TIMESTAMP_ADDATTRIBUTE) {
			return "addattribute";
		} else if (timestamp == TIMESTAMP_MODIFYATTRIBUTE) {
			return "modifyattribute";
		} else if (timestamp == TIMESTAMP_REMOVEATTRIBUTE) {
			return "removeattribute";
		} else if (timestamp == TIMESTAMP_REMOVEOBJECT) {
			return "removeobject";
		} else if (timestamp == TIMESTAMP_POSTMODIFY) {
			return "postmodify";
		} else {
			return Integer.toString(timestamp);
		}
	}

	/**
	 * Helper method to check the sorting of the given objects
	 * 
	 * @param objects
	 *            collection of objects
	 * @param datasource
	 *            true when the objects were sorted by a datasource, false if not
	 * @throws Exception
	 */
	protected void checkSortingOfObjects(Collection objects, boolean datasource) throws Exception {
		// hack for oracle (which sorts nulls last by default)
		String databaseProductName = DB.getDatabaseProductName(ds.getHandle().getDBHandle());
		String[] parentnames = null;
		String[] objectnames = null;

		if (datasource && "Oracle".equals(databaseProductName)) {
			parentnames = PARENTNAMES_ORACLE;
			objectnames = OBJECTNAMES_ORACLE;
		} else {
			parentnames = PARENTNAMES;
			objectnames = OBJECTNAMES;
		}

		// check whether the correct number of objects were received
		assertEquals("Check whether correct number of objects fetched", parentnames.length, objects.size());

		// check sorting
		int i = 0;

		for (Iterator iterator = objects.iterator(); iterator.hasNext();) {
			Resolvable object = (Resolvable) iterator.next();

			assertEquals("Check parent.shortname for object #" + i, parentnames[i], PropertyResolver.resolve(object, "parentnode.shortname"));
			assertEquals("Check shortname for object #" + i, objectnames[i], PropertyResolver.resolve(object, "shortname"));

			++i;
		}
	}

	/**
	 * Helper method to check the binary data of the object in the collection
	 * 
	 * @param objects
	 *            collection of one object (hopefully) holding the binary data
	 * @throws Exception
	 */
	protected void checkBinaryData(Collection objects) throws Exception {
		assertEquals("Check number of objects with binary data", 1, objects.size());
		Iterator i = objects.iterator();
		Resolvable object = (Resolvable) i.next();

		Object data = object.get("binarydata");

		if (data == null) {
			fail("Binary data must not be null");
		} else if (!(data instanceof byte[])) {
			fail("Binary data are expected to be byte[] but are of class " + data.getClass().getName());
		}

		byte[] binaryData = (byte[]) data;

		assertEquals("Check size of binary data", BINARYDATA.length, binaryData.length);
		for (int j = 0; j < binaryData.length; j++) {
			assertEquals("Check data[" + j + "]", BINARYDATA[j], binaryData[j]);
		}
	}

	/**
	 * Helper function to add a match object to the given list
	 * 
	 * @param match
	 *            list of match objects
	 * @param alias
	 *            alias
	 * @param shortname
	 *            shortname
	 */
	protected void addMatch(List match, final String alias, final String shortname) {
		match.add(new Resolvable() {
			public Object getProperty(String key) {
				return get(key);
			}

			public Object get(String key) {
				if ("aliases".equals(key)) {
					return alias;
				} else if ("shortname".equals(key)) {
					return shortname;
				} else {
					return null;
				}
			}

			public boolean canResolve() {
				return true;
			}
		});
	}

	/**
	 * Test the datasource against the given expression.
	 * <ol>
	 * <li>Parse the expression</li>
	 * <li>Generate a datasource filter and get the filtered result</li>
	 * <li>Check the number of filtered objects against the expexted number</li>
	 * <li>Check whether all filtered objects match the expression using the ExpressionEvaluator</li>
	 * <li>Check whether all not-filtered objects do not match the expression using the ExpressionEvaluator</li>
	 * </ol>
	 * Only if all of the above steps are processed without errors and give the expexted results, the overall test succeeds. In case of any error an exception is thrown.
	 * 
	 * @param testExpressionString
	 *            expression to be tested
	 * @param numExpectedObjects
	 *            expected number of objects matching the expression
	 * @param versionTimestamp
	 *            version timestamp
	 * @throws Exception
	 */
	public void testResult(String testExpressionString, int numExpectedObjects, int versionTimestamp) throws Exception {
		testResult(testExpressionString, numExpectedObjects, versionTimestamp, null, true);
	}

	/**
	 * Variant of {@link #testResult(String, int, int)} with an additional map of properties that can be access as [data.*] in the expression.
	 * 
	 * @param testExpressionString
	 *            expression to be tested
	 * @param numExpectedObjects
	 *            expected number of objects matching the expression
	 * @param versionTimestamp
	 *            version timestamp
	 * @param parameters
	 *            map of additional parameters. may be null or empty
	 * @throws Exception
	 */
	public void testResult(String testExpressionString, int numExpectedObjects, int versionTimestamp, Map parameters) throws Exception {
		testResult(testExpressionString, numExpectedObjects, versionTimestamp, parameters, true);
	}

	/**
	 * Variant of {@link #testResult(String, int, int)} with an additional map of properties that can be access as [data.*] in the expression and an option to omit
	 * matching of objects.
	 * 
	 * @param testExpressionString
	 *            expression to be tested
	 * @param numExpectedObjects
	 *            expected number of objects matching the expression
	 * @param versionTimestamp
	 *            version timestamp
	 * @param parameters
	 *            map of additional parameters. may be null or empty
	 * @param matchObjects
	 *            true when fetched objects shall be matched against the rule, false if not
	 * @throws Exception
	 */
	public void testResult(String testExpressionString, int numExpectedObjects, int versionTimestamp, Map parameters, boolean matchObjects)
			throws Exception {
		// parse the expression
		Expression testExpression = ExpressionParser.getInstance().parse(testExpressionString);

		// generate the filter
		DatasourceFilter filter = ds.createDatasourceFilter(testExpression);

		// generate the expression evaluator
		ExpressionEvaluator evaluator = new ExpressionEvaluator();

		// set all parameters
		if (parameters != null) {
			evaluator.setProperty("data", new MapResolver(parameters));
			filter.addBaseResolvable("data", new MapResolver(parameters));
		}

		// prepare the timestamp name
		String timestampName = getTimestampName(versionTimestamp);

		// count the filtered results
		assertEquals("Check count of filtered results for {" + testExpressionString + "} @ " + timestampName, numExpectedObjects,
				ds.getCount(filter, versionTimestamp));

		// fetch filtered objects
		Collection filteredObjects = ds.getResult(filter, null, versionTimestamp);

		// check number filtered objects
		assertEquals("Check # of filtered objects for {" + testExpressionString + "} @ " + timestampName, numExpectedObjects, filteredObjects.size());

		if (matchObjects) {
			// check whether all filtered objects match the rule
			for (Iterator iter = filteredObjects.iterator(); iter.hasNext();) {
				Resolvable element = (Resolvable) iter.next();

				if (!evaluator.match(testExpression, element)) {
					fail("Found a filtered object that does not match the expression {" + testExpressionString + "} @ " + timestampName);
				}
			}
		}

		// get all not-filtered objects
		Collection allObjects = getAllObjects(versionTimestamp);
		Collection notFiltered = new Vector(allObjects);

		notFiltered.removeAll(filteredObjects);

		// check # of not-filtered objects
		assertEquals("Check # of not-filtered objects for {" + testExpressionString + "} @ " + timestampName, allObjects.size() - numExpectedObjects,
				notFiltered.size());

		if (matchObjects) {
			// check whether all not-filtered objects do not match the rule
			for (Iterator iter = notFiltered.iterator(); iter.hasNext();) {
				Resolvable element = (Resolvable) iter.next();

				if (evaluator.match(testExpression, element)) {
					fail("Found a not-filtered object that matches the expression {" + testExpressionString + "} @ " + timestampName);
				}
			}
		}
	}

	/**
	 * Test the testExpressionString that is expected to throw an exception while generating the datasource filter
	 * 
	 * @param testExpressionString
	 *            test expression
	 * @param expectedError
	 *            array of strings that are expected to be in the exception message
	 * @throws Exception
	 */
	public void testExpressionFailure(String testExpressionString, String[] expectedError) throws Exception {
		// parse the expression
		Expression testExpression = null;

		try {
			testExpression = ExpressionParser.getInstance().parse(testExpressionString);
		} catch (ParserException e) {
			// check whether the exception message contains all expected phrases
			String exceptionMessage = e.getLocalizedMessage();
			String containsMessage = "Check the exception message";

			// the message shall contain the testexpression
			assertContains(containsMessage, exceptionMessage, testExpressionString);
			// check for all expected error message parts
			if (expectedError != null) {
				for (int i = 0; i < expectedError.length; ++i) {
					assertContains(containsMessage, exceptionMessage, expectedError[i]);
				}
			}
			return;
		}

		// generate the filter
		try {
			ds.createDatasourceFilter(testExpression);
		} catch (FilterGeneratorException e) {
			// check whether the exception message contains all expected phrases
			String exceptionMessage = e.getLocalizedMessage();
			String containsMessage = "Check the exception message";

			// the message shall contain the testexpression
			assertContains(containsMessage, exceptionMessage, testExpressionString);
			// the message shall contain the classname of the datasource
			assertContains(containsMessage, exceptionMessage, CNDatasource.class.getName());
			if (expectedError != null) {
				for (int i = 0; i < expectedError.length; ++i) {
					assertContains(containsMessage, exceptionMessage, expectedError[i]);
				}
			}
			return;
		}

		fail("Test was expected to fail, but did not");
	}

	/**
	 * Assert that the given fullString contains the stringPart
	 * 
	 * @param message
	 *            message to be shown in case of error
	 * @param fullString
	 *            full string
	 * @param stringPart
	 *            string part
	 * @throws Exception
	 */
	protected static void assertContains(String message, String fullString, String stringPart) throws Exception {
		if (fullString == null || stringPart == null) {
			fail(message + ": <" + fullString + "> was expected to contain <" + stringPart + ">");
		}

		if (fullString.indexOf(stringPart) < 0) {
			fail(message + ": <" + fullString + "> was expected to contain <" + stringPart + ">");
		}
	}

	/**
	 * Get all objects at a version timestamp
	 * 
	 * @param versionTimestamp
	 *            version timestamp
	 * @return collection of all objects
	 * @throws Exception
	 */
	protected Collection getAllObjects(int versionTimestamp) throws Exception {
		// parse the expression
		Expression testExpression = ExpressionParser.getInstance().parse(EMPTYRULE);

		// generate the filter
		DatasourceFilter filter = ds.createDatasourceFilter(testExpression);

		return ds.getResult(filter, null, versionTimestamp);
	}

	/**
	 * Clean the datasource (remove/reset all data)
	 * 
	 * @throws Exception
	 */
	protected void cleanDatasource() throws Exception {
		// reset all the data
		DBHandle dbHandle = ds.getHandle().getDBHandle();

		// stop all running transactions
		DB.cleanupAllTransactions();

		// delete contentmap data
		DB.update(dbHandle, "delete from " + dbHandle.getContentMapName());
		DB.update(dbHandle, "delete from " + dbHandle.getContentMapName() + "_nodeversion");

		// delete contentattribute data
		DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeName());
		DB.update(dbHandle, "delete from " + dbHandle.getContentAttributeName() + "_nodeversion");

		// reset the id counters
		DB.update(dbHandle, "update " + dbHandle.getContentObjectName() + " set id_counter = 0");
	}

	/**
	 * Remove the data
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		// cleanDatasource();
		disconnectDs();
	}

	/**
	 * Remove the test databases
	 * 
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
	 * Create the timestamps that will be used for versioning tests
	 */
	public static void createVersioningTimestamps() {
		TIMESTAMP_NOW = (int) (System.currentTimeMillis() / 1000);
		TIMESTAMP_PREMODIFY = 1000 + TIMESTAMP_NOW;
		TIMESTAMP_ADDOBJECT = 2000 + TIMESTAMP_NOW;
		TIMESTAMP_LINKOBJECT = 3000 + TIMESTAMP_NOW;
		TIMESTAMP_REMOVELINK = 4000 + TIMESTAMP_NOW;
		TIMESTAMP_ADDATTRIBUTE = 5000 + TIMESTAMP_NOW;
		TIMESTAMP_MODIFYATTRIBUTE = 6000 + TIMESTAMP_NOW;
		TIMESTAMP_REMOVEATTRIBUTE = 7000 + TIMESTAMP_NOW;
		TIMESTAMP_REMOVEOBJECT = 8000 + TIMESTAMP_NOW;
		TIMESTAMP_POSTMODIFY = 9000 + TIMESTAMP_NOW;
		ALLTIMESTAMPS = new int[] { TIMESTAMP_PREMODIFY, TIMESTAMP_ADDOBJECT, TIMESTAMP_LINKOBJECT, TIMESTAMP_REMOVELINK, TIMESTAMP_ADDATTRIBUTE,
				TIMESTAMP_MODIFYATTRIBUTE, TIMESTAMP_REMOVEATTRIBUTE, TIMESTAMP_REMOVEOBJECT, TIMESTAMP_POSTMODIFY, -1 };
	}

	/**
	 * connect to datasources
	 * 
	 */
	protected void connectDs() throws Exception {
		boolean setupStructure = false;
		SQLUtils sqlUtils = null;

		if (!databases.containsKey(testDatabase.getIdentifier())) {
			databases.put(testDatabase.getIdentifier(), testDatabase);
			sqlUtils = SQLUtilsFactory.getSQLUtils(testDatabase);
			sqlUtils.connectDatabase();
			testDatabase.setRandomDatabasename("VersioningTest" + System.currentTimeMillis());
			handleProperties = testDatabase.getSettings();
			sqlUtils.createCRDatabase(getClass());
			sqlUtils.selectDatabase();
			setupStructure = true;
		} else {
			handleProperties = testDatabase.getSettings();
		}

		// create the datasource handle
		SQLHandle handle = new SQLHandle("versioning");

		handle.init(handleProperties);

		// create the datasource without caching
		Map dsProps = new HashMap(handleProperties);

		// we do not use caching here, but we use versioning
		dsProps.put("cache", "false");
		dsProps.put("versioning", versioning);
		dsProps.put("prefetchAttribute.threshold", "0");
		nonCachedDs = new CNWriteableDatasource(null, new SimpleHandlePool(handle), dsProps) {
			public String toString() {
				// make the toString of each datasource unique, to have them
				// separate cache regions
				return "noncached" + super.toString();
			}
		};

		// create another datasource with caching
		Map cachingDsProps = new HashMap(handleProperties);

		// use caching and versioning here
		cachingDsProps.put("cache", "true");
		cachingDsProps.put("versioning", versioning);
		// set this datasource to update the timestamp upon insert/update/delete
		cachingDsProps.put("setUpdatetimestampOnWrite", "true");
		cachedDs = new CNWriteableDatasource(null, new SimpleHandlePool(handle), cachingDsProps) {
			public String toString() {
				// make the toString of each datasource unique, to have them
				// separate cache regions
				return "cached" + super.toString();
			}
		};

		Map syncCheckingDsProps = new HashMap(handleProperties);

		// use caching and syncchecking here
		syncCheckingDsProps.put("cache", "true");
		syncCheckingDsProps.put("cache.syncchecking", "true");
		syncCheckingDsProps.put("cache.syncchecking.interval", "10");
		syncCheckingDsProps.put("cache.syncchecking.differential", "false");
		syncCheckingDsProps.put("prefetchAttribute.threshold", "0");
		syncCheckingDsProps.put("setUpdatetimestampOnWrite", "true");
		syncCheckingDs = new CNWriteableDatasource(null, new SimpleHandlePool(handle), syncCheckingDsProps) {
			public String toString() {
				// make the toString of each datasource unique, to have them
				// separate cache regions
				return "synccheck" + super.toString();
			}
		};

		Map autoprefetchDsProps = new HashMap(handleProperties);

		autoprefetchDsProps.put("cache", "true");
		autoprefetchDsProps.put("autoprefetch", "true");
		autoprefetchDs = new CNWriteableDatasource(null, new SimpleHandlePool(handle), autoprefetchDsProps) {
			public String toString() {
				// make the toString of each datasource unique, to have them
				// separate cache regions
				return "autoprefetch" + super.toString();
			}
		};

		if (setupStructure) {
			// import structure and data
			String definitionFileName = "filter_tests_structure.xml";

			ObjectManagementManager.importTypes((CNDatasource) nonCachedDs, getClass().getResourceAsStream(definitionFileName));

			insertTestData(sqlUtils);
			sqlUtils.disconnectDatabase();
		}
	}

	/**
	 * Insert Test data (may be overwritten in subclasses)
	 * 
	 * @param sqlUtils
	 *            sql utils
	 * @throws Exception
	 */
	protected void insertTestData(SQLUtils sqlUtils) throws Exception {
	}

	/**
	 * close handles
	 * 
	 */
	protected void disconnectDs() throws Exception {
		nonCachedDs.getHandlePool().close();
		nonCachedDs = null;
		cachedDs.getHandlePool().close();
		cachedDs = null;
		syncCheckingDs.getHandlePool().close();
		syncCheckingDs = null;
		autoprefetchDs.getHandlePool().close();
		autoprefetchDs = null;
	}

	@Before
	public void setUp() throws Exception {
		createVersioningTimestamps();
		connectDs();

		// nearly all tests are done with the non-cached datasource
		ds = nonCachedDs;

		// clean the datasource first
		cleanDatasource();

		DBHandle dbHandle = ds.getHandle().getDBHandle();

		// create objecttypes and attributetypes we'll use for our tests

		/*
		 * BufferedReader sqlTypesBuffReader = new BufferedReader( new InputStreamReader(getClass().getResourceAsStream("CNDatasourceFilter.sql"), "utf8"));
		 * 
		 * String line; while ((line = sqlTypesBuffReader.readLine()) != null) { if (line.startsWith("--")) { continue; } DB.update(dbHandle, line); }
		 * 
		 * // create an optimized int attribute createAttributeIfNotExistent(ds, "optimizedint", GenticsContentAttribute.ATTR_TYPE_INTEGER, true, "quick_optimizedint",
		 * false, LEAFTYPE.intValue(), 0, null, null, false);
		 */

		// create the objects (current versions)
		topNode = createNode(null, "topnode");
		emptyNode = createNode(topNode, "emptynode");
		fullNode = createNode(topNode, "fullnode");
		subNode1 = createNode(fullNode, "subnode1");
		subNode2 = createNode(fullNode, "subnode2");
		subNode2.setProperty("age", new Integer(42));
		storeObject(subNode2, -1);

		leaf1 = createLeaf(subNode1, "leaf1", -1, new String[] { "cool", "cewl", "kewl" });
		leaf2 = createLeaf(subNode1, "leaf2");
		leaf3 = createLeaf(subNode2, "leaf3", -1, new String[] { "ha", "har", "hey" }, BINARYDATA, 42);
		leaf4 = createLeaf(subNode2, "leaf4", -1, new String[] { "muh" });
		leaf5 = createLeaf(fullNode, "leaf5", -1, new String[] { null });
		leaf6 = createLeaf(fullNode, "leaf6");
		leaf7 = createLeaf(topNode, "leaf7");
		leaf8 = createLeaf(topNode, "leaf8");

		// now versioned data modification
		// create an object
		if (ds.isVersioning()) {
			newLeaf = createLeaf(subNode2, "newleaf", TIMESTAMP_ADDOBJECT, new String[] { "new leaf", "very new leaf", "new new new" });

			// link the object to another object
			newLeaf.setProperty("parentnode", emptyNode);
			newLeaf.setProperty("quickparentnode", emptyNode);
			newLeaf.setProperty("aliases", Arrays.asList(new String[] { "kind of new", "too new" }));
			storeObject(newLeaf, TIMESTAMP_LINKOBJECT);

			// remove the object link
			newLeaf.setProperty("parentnode", null);
			newLeaf.setProperty("quickparentnode", null);
			storeObject(newLeaf, TIMESTAMP_REMOVELINK);

			// add an attribute
			newLeaf.setProperty("age", new Integer(42));
			newLeaf.setProperty("parentnode", topNode);
			newLeaf.setProperty("quickparentnode", topNode);
			storeObject(newLeaf, TIMESTAMP_ADDATTRIBUTE);

			// modify attribute values
			newLeaf.setProperty("age", new Integer(4711));
			newLeaf.setProperty("shortname", "modifiednewleaf");
			storeObject(newLeaf, TIMESTAMP_MODIFYATTRIBUTE);

			fullNode.setProperty("shortname", "modifiedfullnode");
			storeObject(fullNode, TIMESTAMP_MODIFYATTRIBUTE);

			// remove attribute values
			newLeaf.setProperty("age", null);
			newLeaf.setProperty("shortname", null);
			storeObject(newLeaf, TIMESTAMP_REMOVEATTRIBUTE);

			fullNode.setProperty("shortname", null);
			storeObject(fullNode, TIMESTAMP_REMOVEATTRIBUTE);
		}
	}

	/**
	 * Test prefetching of attributes for large resultsets
	 * 
	 * @param filter
	 *            datasource filter for fetching objects
	 * @param numEntriesFetched
	 *            number of entries fetched
	 * @throws Exception
	 */
	protected void internalTestPrefetchLargeResults(DatasourceFilter filter, int numEntriesFetched) throws Exception {
		// first clear the caches
		ds.clearCaches();

		// now fetch the objects and prefetch the name
		Collection entries = ds.getResult(filter, new String[] { "name" }, 0, numEntriesFetched, new Datasource.Sorting[] { new Datasource.Sorting(
				"obj_id", Datasource.SORTORDER_ASC) });

		// get the db statement counter
		QueryCounter counter = new QueryCounter(false, true);

		// now access all names
		int nameCounter = 1;

		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			Resolvable entry = (Resolvable) iterator.next();

			assertEquals("Check the accessed name", "entry #" + nameCounter, entry.get("name"));
			nameCounter++;
		}

		// check whether all data was fetched from the cache
		assertEquals("Check number of db statements while accessing names", 0, counter.getCount());
	}

	/**
	 * Internal method to test autoprefetching with various sortings
	 * 
	 * @param sorting
	 *            sorting to be used
	 * @throws Exception
	 */
	protected void internalTestAutoPrefetching(Datasource.Sorting[] sorting) throws Exception {
		// to make sure, clear all caches for the datasource
		autoprefetchDs.clearCaches();
		Expression expr = ExpressionParser.getInstance().parse("object.obj_type == " + NODETYPE);

		// get the db statement counter
		QueryCounter counter = new QueryCounter(false, true);

		// filter objects
		Collection result = autoprefetchDs.getResult(autoprefetchDs.createDatasourceFilter(expr), null, 0, -1, sorting);

		// access the attribute values, all attributes should come from the
		// caches
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			Resolvable object = (Resolvable) iterator.next();

			object.get("shortname");
		}

		// now assert that autoprefetching worked, and there was only a single
		// db statement
		assertEquals("Check whether optimized attributes were autoprefetched", 1, counter.getCount());
	}

	/**
	 * Internal test for the datasource parameter "setUpdatetimestampOnWrite"
	 * 
	 * @param wds
	 *            datasource to test
	 * @param updateDoesChange
	 *            true when the updatetimestamp should change, false if not
	 * @throws Exception
	 */
	protected void internalTestSetUpdatetimestampOnWrite(CNWriteableDatasource wds, boolean updateDoesChange) throws Exception {
		// get the current lastupdate timestamp
		long lastUpdate = wds.getLastUpdate();
		Map objectParams = new HashMap();

		objectParams.put("obj_type", NODETYPE);
		Changeable object = wds.create(objectParams);

		// wait a little time
		TimingUtils.waitForNextSecond();
		// insert the object
		wds.insert(Collections.singletonList(object));
		// check the lastupdate timestamp
		if (updateDoesChange) {
			assertTrue("Check whether lastupdate timestamp changes when configured", lastUpdate != wds.getLastUpdate());
		} else {
			assertTrue("Check whether lastupdate timestamp does not change when not configured", lastUpdate == wds.getLastUpdate());
		}

		// get hte current lastupdate timestamp
		lastUpdate = wds.getLastUpdate();
		object.setProperty("name", "A name");
		// wait a little time
		TimingUtils.waitForNextSecond();
		// update the object
		wds.update(Collections.singletonList(object));
		// check the lastupdate timestamp
		if (updateDoesChange) {
			assertTrue("Check whether lastupdate timestamp changes when configured", lastUpdate != wds.getLastUpdate());
		} else {
			assertTrue("Check whether lastupdate timestamp does not change when not configured", lastUpdate == wds.getLastUpdate());
		}

		// get hte current lastupdate timestamp
		lastUpdate = wds.getLastUpdate();
		// wait a little time
		TimingUtils.waitForNextSecond();
		// delete the object
		wds.delete(Collections.singletonList(object));
		// check the lastupdate timestamp
		if (updateDoesChange) {
			assertTrue("Check whether lastupdate timestamp changes when configured", lastUpdate != wds.getLastUpdate());
		} else {
			assertTrue("Check whether lastupdate timestamp does not change when not configured", lastUpdate == wds.getLastUpdate());
		}
	}

	protected boolean createAttributeIfNotExistent(CNDatasource datasource, String name, int attributeType, boolean optimized, String quickName,
			boolean multivalue, int objectType, int linkedObjectType, String foreignLinkAttributeType, String foreignLinkAttributeRule,
			boolean excludeVersioning) throws Exception {
		// check whether the attribute already exists
		Collection attributeTypes = ObjectManagementManager.loadAttributeTypes(datasource.getHandle().getDBHandle());

		for (Iterator iterator = attributeTypes.iterator(); iterator.hasNext();) {
			ObjectAttributeBean attr = (ObjectAttributeBean) iterator.next();

			if (attr.getName().equals(name)) {
				// TODO eventually check whether the attribute is defined
				// correctly
				return true;
			}
		}

		ObjectAttributeBean newAttribute = new ObjectAttributeBean(name, attributeType, optimized, quickName, multivalue, objectType,
				linkedObjectType, null, foreignLinkAttributeType, foreignLinkAttributeRule, excludeVersioning, false);

		return ObjectManagementManager.saveAttributeType(datasource, newAttribute, true);
	}

	/**
	 * Internal dummy class for a GenticsUser
	 */
	protected class DummyUser extends ResolvableBean implements GenticsUser {

		/**
		 * serial version id
		 */
		private static final long serialVersionUID = 8380874104228394559L;

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.auth.GenticsUser#isLoggedIn()
		 */
		public boolean isLoggedIn() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.auth.GenticsUser#isAnonymous()
		 */
		public boolean isAnonymous() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.gentics.api.lib.auth.GenticsUser#getId()
		 */
		public String getId() {
			return "tester";
		}
	}
}
