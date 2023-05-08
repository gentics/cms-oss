/*
 * @author norbert
 * @date 12.07.2006
 * @version $Id: CNDatasourceFilterTest.java,v 1.2.2.1 2011-04-07 10:09:30 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

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
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.VersionedObject;
import com.gentics.lib.datasource.object.ObjectAttributeBean;
import com.gentics.lib.datasource.object.ObjectManagementManager;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.resolving.FilterableResolvable;
import com.gentics.node.testutils.QueryCounter;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.SQLUtilsFactory;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;



/**
 * Test for creation of DatasourceFilter for CNDatasources.
 */
@Category(BaseLibTest.class)
public class CNDatasourceFilterTest extends AbstractCNDatasourceFilterTest {

	public CNDatasourceFilterTest(TestDatabase testDatabase) throws Exception {
		super(testDatabase);
	}

	/**
	 * Get variation data
	 * @return variation data
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(
				getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS, TestDatabaseVariationConfigurations.MSSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.ORACLE_VARIATIONS));
	}



	/**
	 * Test the matches function.
	 *
	 * @throws Exception
	 */
	@Test
	public void testMatchesFunction() throws Exception {
		Map dataMap = new HashMap();
		List match = new Vector();

		addMatch(match, "muh", "leaf5");
		addMatch(match, "nomuh", "leaf4");
		addMatch(match, "cool", "leaf1");
		addMatch(match, "har", "leaf3");
		dataMap.put("match", match);

		testResult("matches(data.match, object.obj_type == 2 && object.aliases CONTAINSONEOF this.aliases && object.shortname == this.shortname)", 2, -1,
				dataMap);
	}

	@Test
	public void testLimitResult() throws Exception {
		Expression expression = PortalConnectorFactory.createExpression("object.obj_type == 2");
		DatasourceFilter filter = ds.createDatasourceFilter(expression);
		Collection result = ds.getResult(filter, new String[] { "shortname" }, 2, 5,
				new Datasource.Sorting[] { new Datasource.Sorting("shortname", Datasource.SORTORDER_ASC) });

		assertEquals("Verifying result count", 5, result.size());
		Iterator i = result.iterator();

		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf3");
		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf4");
		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf5");
		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf6");
		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf7");
	}

	@Test
	public void testLimitResultNoMax() throws Exception {
		Expression expression = PortalConnectorFactory.createExpression("object.obj_type == 2");
		DatasourceFilter filter = ds.createDatasourceFilter(expression);
		Collection result = ds.getResult(filter, new String[] { "shortname" }, 2, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("shortname", Datasource.SORTORDER_ASC) });

		assertEquals("Verifying result count", 6, result.size());
		Iterator i = result.iterator();

		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf3");
	}

	@Test
	public void testLimitResultNoStartNoMax() throws Exception {
		Expression expression = PortalConnectorFactory.createExpression("object.obj_type == 2");
		DatasourceFilter filter = ds.createDatasourceFilter(expression);
		Collection result = ds.getResult(filter, new String[] { "shortname" }, -1, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("shortname", Datasource.SORTORDER_ASC) });

		assertEquals("Verifying result count", 8, result.size());
		Iterator i = result.iterator();

		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf1");
	}

	/**
	 * Test filtering binary data (with isempty())
	 *
	 * @throws Exception
	 */
	@Test
	public void testFilterBinaryData() throws Exception {
		// first check whether one object is returned
		testResult("!isempty(object.binarydata)", 1, -1);
	}

	/**
	 * Test reading binary data
	 *
	 * @throws Exception
	 */
	@Test
	public void testReadBinaryData() throws Exception {
		// now fetch the object and read the binary data
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.contentid == data.contentid"));
		Map dataMap = new HashMap();

		dataMap.put("contentid", leaf3.get("contentid"));
		filter.addBaseResolvable("data", new MapResolver(dataMap));

		// first without prefilling
		checkBinaryData(ds.getResult(filter, null));

		// then again with prefilling
		checkBinaryData(ds.getResult(filter, new String[] { "binarydata" }));
	}

	/**
	 * Test the sorting in the datasource
	 *
	 * @throws Exception
	 */
	@Test
	public void testDatasourceSorting() throws Exception {
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true"));
		Collection objects = ds.getResult(filter, new String[] { "parentnode.shortname", "shortname" }, 0, -1, new Datasource.Sorting[] {
			new Datasource.Sorting("parentnode.shortname", Datasource.SORTORDER_ASC), new Datasource.Sorting("shortname", Datasource.SORTORDER_DESC) });

		checkSortingOfObjects(objects, true);
	}

	/**
	 * Test manually sorting (using the {@link ResolvableComparator})
	 *
	 * @throws Exception
	 */
	@Test
	public void testManualSorting() throws Exception {
		DatasourceFilter filter = ds.createDatasourceFilter(ExpressionParser.getInstance().parse("true"));

		// first sort by contentid
		List objects = (List) ds.getResult(filter, new String[] { "parentnode.shortname", "shortname" }, 0, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("contentid", Datasource.SORTORDER_DESC) });

		// now sort manually
		Collections.sort(objects, new ResolvableComparator(new Datasource.Sorting[] {
			new Datasource.Sorting("parentnode.shortname", Datasource.SORTORDER_ASC), new Datasource.Sorting("shortname", Datasource.SORTORDER_DESC) }));

		checkSortingOfObjects(objects, false);
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
	 * Only if all of the above steps are processed without errors and give the expexted results, the overall test succeeds. In case of any
	 * error an exception is thrown.
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
	 * Variant of {@link #testResult(String, int, int)} with an additional map of properties that can be access as [data.*] in the
	 * expression.
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
	 * Variant of {@link #testResult(String, int, int)} with an additional map of properties that can be access as [data.*] in the
	 * expression and an option to omit matching of objects.
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
	public void testResult(String testExpressionString, int numExpectedObjects, int versionTimestamp, Map parameters, boolean matchObjects) throws Exception {
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
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		// cleanDatasource();
		disconnectDs();
	}

	/**
	 * Remove the test databases
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
	protected Changeable createLeaf(Resolvable node, String name, int versionTimestamp, String[] aliases, byte[] binaryData, int optimizedInt) throws DatasourceException {
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

	@Test
	public void testFilterableResolvableObjectLink() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) leaf1).getFiltered("parentnode", null);

		logger.info("Expression: " + filter.getExpressionString());
		Collection parentnode = ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("assert that we got only one object", 1, parentnode.size());

		Resolvable pnode = (Resolvable) parentnode.iterator().next();

		logger.info("parent node: " + pnode.get("contentid"));
		assertEquals("asserting that we got the root node", ((Resolvable) leaf1.get("parentnode")).get("contentid"), pnode.get("contentid"));
	}

	@Test
	public void testFilterableResolvableObjectLinkWithFilter() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) leaf1).getFiltered("parentnode", "1 == 1");

		logger.info("Expression: " + filter.getExpressionString());
		Collection parentnode = ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("assert that we got only one object", 1, parentnode.size());

		Resolvable pnode = (Resolvable) parentnode.iterator().next();

		logger.info("parent node: " + pnode.get("contentid"));
		assertEquals("asserting that we got the root node", ((Resolvable) leaf1.get("parentnode")).get("contentid"), pnode.get("contentid"));

		DatasourceFilter filter2 = ((FilterableResolvable) leaf1).getFiltered("parentnode", "1 == 0");

		logger.info("Expression: " + filter2.getExpressionString());
		Collection parentnode2 = ds.getResult(filter2, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("assert that we got no object", 0, parentnode2.size());
	}

	@Test
	public void testFilterableResolvableForeignLink() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) topNode).getFiltered("subnodes", null);
		Collection subnodes = ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("Assert that we got the right number of nodes", 2, subnodes.size());

		// Collection subnodes = (Collection) topNode.get("subnodes");
		Iterator i = subnodes.iterator();
		Resolvable subnode = (Resolvable) i.next();

		assertEquals("assert right name", "fullnode", subnode.get("name"));

		subnode = (Resolvable) i.next();
		assertEquals("assert right name", "emptynode", subnode.get("name"));
	}

	@Test
	public void testFilterableResolvableForeignLinkWithFilter() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) topNode).getFiltered("subnodes", "object.name == \"emptynode\"");
		Collection subnodes = ds.getResult(filter, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("Assert that we got the right number of nodes", 1, subnodes.size());

		// Collection subnodes = (Collection) topNode.get("subnodes");
		Iterator i = subnodes.iterator();
		Resolvable subnode = (Resolvable) i.next();

		assertEquals("assert right name", "emptynode", subnode.get("name"));
	}

	/**
	 * Test filtering a LOB attribute with ==
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectLOBAttribute() throws Exception {
		testResult("object.longname == 'leaf1'", 1, -1);
	}

	/**
	 * Test filtering a LOB attribute with LIKE
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectLOBAttributeWithLike() throws Exception {
		testResult("!(object.longname LIKE \"leaf%\")", 5, -1);
	}

	/**
	 * Test writing and reading multivalue attributes that contain empty values
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultivalueWithEmpty() throws Exception {
		// create an object with a multivalue attribute that contains an empty
		// value
		Map params = new HashMap();

		params.put("obj_type", LEAFTYPE);
		Changeable object = ds.create(params);
		Vector multiValue = new Vector();

		multiValue.add("A");
		multiValue.add("B");
		multiValue.add("");
		multiValue.add("D");
		multiValue.add("E");
		object.setProperty("aliases", multiValue);
		ds.store(Collections.singletonList(object));
		params.clear();
		params.put("contentid", object.get("contentid"));

		// read the object
		final Vector dbValues = new Vector();

		DB.query(ds.getHandle().getDBHandle(),
				"select * from " + ds.getHandle().getDBHandle().getContentAttributeName() + " where contentid = '" + object.get("contentid")
				+ "' and name = 'aliases' order by sortorder",
				new ResultProcessor() {
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					dbValues.add(rs.getString("value_text"));
				}
			}

			public void takeOver(ResultProcessor p) {}
		});

		// now check that the db contains exactly the values, we get when
		// fetching the object via the connector
		assertEquals("Check whether db contains exactly the values that are fetched", ds.create(params).get("aliases"), dbValues);

		// now change the multivalue by adding a new value in the middle
		multiValue.clear();
		multiValue.add("A");
		multiValue.add("B");
		multiValue.add("C");
		multiValue.add("D");
		multiValue.add("E");
		object.setProperty("aliases", multiValue);
		ds.update(Collections.singletonList(object));

		// read the object
		dbValues.clear();
		DB.query(ds.getHandle().getDBHandle(),
				"select * from " + ds.getHandle().getDBHandle().getContentAttributeName() + " where contentid = '" + object.get("contentid")
				+ "' and name = 'aliases' order by sortorder",
				new ResultProcessor() {
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					dbValues.add(rs.getString("value_text"));
				}
			}

			public void takeOver(ResultProcessor p) {}
		});

		// now check that the db contains exactly the values, we get when
		// fetching the object via the connector
		assertEquals("Check whether db contains exactly the values that are fetched", ds.create(params).get("aliases"), dbValues);
	}

	/**
	 * Test whether the updatetimestamp is written on update/insert/delete, when configured with datasource parameter
	 * "setUpdatetimestampOnWrite" (and not, when not configured)
	 */
	@Test
	public void testSetUpdatetimestampOnWrite() throws Exception {
		internalTestSetUpdatetimestampOnWrite(ds, false);
		internalTestSetUpdatetimestampOnWrite(cachedDs, true);
	}

	/**
	 * Test whether attribute prefilling works.
	 *
	 * @throws Exception
	 */
	@Test
	public void testAttributePrefilling() throws Exception {
		// initialize the db query counter
		QueryCounter counter = new QueryCounter(false, true);

		// get some objects with attributes to prefill
		Collection result = ds.getResult(ds.createDatasourceFilter(ExpressionParser.getInstance().parse("object.obj_type == " + NODETYPE)),
				new String[] { "shortname" });

		// access the attribute values
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			Resolvable object = (Resolvable) iterator.next();

			object.get("shortname");
		}

		// check that only 2 db queries were done (one to fetch the objects, one
		// to prefill the attributes)
		assertEquals("Check whether the prefilling worked ", 2, counter.getCount());
	}

	/**
	 * Test the differential sync-checking
	 *
	 * @throws Exception
	 */
	@Test
	public void testDifferentialSynchchecking() throws Exception {
		Expression expr = ExpressionParser.getInstance().parse("object.obj_type == " + NODETYPE);

		// first make sure that both datasources have the objects and attributes
		// in the cache
		Collection result = cachedDs.getResult(cachedDs.createDatasourceFilter(expr), new String[] { "shortname" });
		Collection result2 = syncCheckingDs.getResult(syncCheckingDs.createDatasourceFilter(expr), new String[] { "shortname" });

		// get the last update timestamp
		cachedDs.setLastUpdate();
		long lastUpdate = cachedDs.getLastUpdate();

		// wait a bit
		Thread.sleep(1500);

		// now update one object
		Iterator resultIterator = result.iterator();

		if (resultIterator.hasNext()) {
			Changeable next = (Changeable) resultIterator.next();

			next.setProperty("shortname", "this is the new shortname!");
			cachedDs.store(Collections.singleton(next));
		}

		// now differentially clear the caches
		GenticsContentFactory.clearDifferentialCaches(syncCheckingDs, lastUpdate);

		// refetch the result, without prefilling
		result2 = syncCheckingDs.getResult(syncCheckingDs.createDatasourceFilter(expr), null);

		// now access the attributes, and count the # of db queries
		QueryCounter counter = new QueryCounter(false, true);

		// access the attribute values, all but one attributes should still come
		// from the caches
		for (Iterator iterator = result2.iterator(); iterator.hasNext();) {
			Resolvable object = (Resolvable) iterator.next();

			object.get("shortname");
		}

		// check that exactly 1 db query was done (fetching the attribute for
		// the modified object)
		assertEquals("Check whether the cache was cleared only for the modified object ", 1, counter.getCount());

		// the test is only valid if we fetched more than one object
		assertTrue("Check whether enough objects were fetched (must be more than 1)", result2.size() > 1);
	}

	/**
	 * Test autoprefetching of optimized attributes
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingUnsorted() throws Exception {
		internalTestAutoPrefetching(null);
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by an optimized attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingSortedByQuick() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("shortname", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by a meta attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingSortedByMeta() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by a normal attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingSortedByNormal() throws Exception {
		internalTestAutoPrefetching(new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test autoprefetching of optimized attributes when sorting by a more than one attribute
	 *
	 * @throws Exception
	 */
	@Test
	public void testAutoPrefetchingMultisorted() throws Exception {
		internalTestAutoPrefetching(
				new Datasource.Sorting[] {
			new Datasource.Sorting("name", Datasource.SORTORDER_ASC), new Datasource.Sorting("shortname", Datasource.SORTORDER_DESC),
			new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) });
	}

	/**
	 * Test prefetching of large result sets (more than 1000 entries)
	 *
	 * @throws Exception
	 */
	@Test
	public void testPrefetchLargeResults() throws Exception {
		// first generate 3000 new entries
		int numEntries = 3000;

		for (int i = 1; i <= numEntries; i++) {
			createNode(null, "entry #" + i);
		}

		// create the filter for fetching the results
		DatasourceFilter filter = ds.createDatasourceFilter(
				ExpressionParser.getInstance().parse("object.obj_type == " + NODETYPE + " AND object.name LIKE 'entry #%'"));

		// first fetch less than 1000 entries
		internalTestPrefetchLargeResults(filter, 500);

		// now test with 999 entries
		internalTestPrefetchLargeResults(filter, 999);

		// now test with 1000 entries
		internalTestPrefetchLargeResults(filter, 1000);

		// and test with more than 1000 entries
		internalTestPrefetchLargeResults(filter, 2500);

		// and test with all entries
		internalTestPrefetchLargeResults(filter, -1);
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
		Collection entries = ds.getResult(filter, new String[] { "name" }, 0, numEntriesFetched,
				new Datasource.Sorting[] { new Datasource.Sorting("obj_id", Datasource.SORTORDER_ASC) });

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
		Thread.sleep(1020);
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
		Thread.sleep(1020);
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
		Thread.sleep(1020);
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

		ObjectAttributeBean newAttribute = new ObjectAttributeBean(name, attributeType, optimized, quickName, multivalue, objectType, linkedObjectType, null,
				foreignLinkAttributeType, foreignLinkAttributeRule, excludeVersioning, false);

		return ObjectManagementManager.saveAttributeType(datasource, newAttribute, true);
	}

	/**
	 * Test filtering a very long LOB attribute with like
	 *
	 * @throws Exception
	 */
	@Test
	public void testVeryLongLOBAttributeWithLike() throws Exception {
		testResult("object.longname LIKE '%" + SEARCHED_IN_LONG_VALUE + "%'", 5, -1);
	}
}
