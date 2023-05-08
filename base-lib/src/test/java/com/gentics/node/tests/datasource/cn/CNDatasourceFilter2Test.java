package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.content.GenticsContentFactory;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.lib.db.ResultProcessor;
import com.gentics.lib.resolving.FilterableResolvable;
import com.gentics.node.testutils.QueryCounter;
import com.gentics.node.tests.utils.TimingUtils;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

@Category(BaseLibTest.class)
public class CNDatasourceFilter2Test extends AbstractCNDatasourceFilterTest {

	public CNDatasourceFilter2Test(TestDatabase testDatabase) throws Exception {
		super(testDatabase);
	}

	/**
	 * Get variation data
	 *
	 * @return variation data
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS, TestDatabaseVariationConfigurations.MSSQL_VARIATIONS,
				TestDatabaseVariationConfigurations.ORACLE_VARIATIONS));
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.name == concat(&quot;subnode&quot;, 1)
	 * </pre>
	 *
	 * (static usage of named function "concat").
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticMultiValueContainsNoneCombinationNegation() throws Exception {
		testResult("object.obj_type == 2 AND !(object.aliases CONTAINSNONE [\"cewl\"] AND object.aliases CONTAINSONEOF [\"muh\", \"ha\"])", 6, -1);
	}

	/**
	 * test CONTAINSONEOF with indirect object.* references requiring a JOIN.
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticMultiValueResolved() throws Exception {
		testResult("object.obj_type == 1 AND object.leafs.aliases CONTAINSONEOF [\"cewl\"]", 1, -1);
		testResult("object.obj_type == 1 AND object.leafs.aliases CONTAINSONEOF [null]", 0, -1);
		testResult("object.obj_type == 1 AND object.leafs.aliases CONTAINSONEOF [\"cewl\",null]", 1, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.name CONTAINSONEOF data.names
	 * </pre>
	 *
	 * (semi-static CONTAINSONEOF).
	 *
	 * @throws Exception
	 */
	@Test
	public void testSemiStaticContainsOneOfFunction() throws Exception {
		Map data = new HashMap();
		Collection names = new Vector();

		names.add("leaf1");
		names.add("leaf2");
		data.put("names", names);
		testResult("object.name CONTAINSONEOF data.names", 2, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.name CONTAINSNONE data.names
	 * </pre>
	 *
	 * (semi-static CONTAINSNONE).
	 *
	 * @throws Exception
	 */
	@Test
	public void testSemiStaticContainsNoneFunction() throws Exception {
		Map data = new HashMap();
		Collection names = new Vector();

		names.add("leaf1");
		names.add("leaf2");
		data.put("names", names);
		testResult("object.name CONTAINSNONE data.names", 11, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.age CONTAINSONEOF ...
	 * </pre>
	 *
	 * with various combinations of values (including nulls).
	 *
	 * @throws Exception
	 */
	@Test
	public void testContainsOneOfNullFunction() throws Exception {
		testResult("object.age CONTAINSONEOF null", 12, -1);
		testResult("object.age CONTAINSONEOF []", 0, -1);
		testResult("object.age CONTAINSONEOF [null]", 12, -1);
		testResult("object.age CONTAINSONEOF [42, null]", 13, -1);
		testResult("object.age CONTAINSONEOF [42, 4711]", 1, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.age CONTAINSNONE ...
	 * </pre>
	 *
	 * with various combinations of values (including nulls).
	 *
	 * @throws Exception
	 */
	@Test
	public void testContainsNoneNullFunction() throws Exception {
		testResult("object.age CONTAINSNONE null", 1, -1);
		testResult("object.age CONTAINSNONE []", 13, -1);
		testResult("object.age CONTAINSNONE [null]", 1, -1);
		testResult("object.age CONTAINSNONE [42, null]", 0, -1);
		testResult("object.age CONTAINSNONE [42, 4711]", 12, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.name == unknownfunction(&quot;huzli&quot;, &quot;0815&quot;, 4711)
	 * </pre>
	 *
	 * (usage of an unknown named function).
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalNamedFunction() throws Exception {
		testExpressionFailure("object.name == unknownfunction(\"huzli\", \"0815\", 4711)", new String[] { "unknownfunction" });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  this is an invalid expression
	 * </pre>
	 *
	 * (unparseable expression).
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalExpression() throws Exception {
		testExpressionFailure("this is an invalid expression", null);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.name CONTAINSONEOF object.shortname
	 * </pre>
	 *
	 * (all dynamic CONTAINSONEOF).
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalContainsOneOfUsage() throws Exception {
		testExpressionFailure("object.name CONTAINSONEOF object.shortname", null);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.name CONTAINSNONE object.shortname
	 * </pre>
	 *
	 * (all dynamic CONTAINSNONE).
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalContainsNoneUsage() throws Exception {
		testExpressionFailure("object.name CONTAINSNONE object.shortname", null);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.age == &quot;&quot;
	 * </pre>
	 *
	 * with or without compatibility mode for treating empty strings as null.
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmptyStringComparisonCompatibility() throws Exception {
		ExpressionParser.setTreatEmptyStringAsNull(true);
		testResult("object.age == \"\"", 12, -1);
		testResult("object.age != \"\"", 1, -1);
		ExpressionParser.setTreatEmptyStringAsNull(false);
		testResult("object.age == \"\"", 0, -1);
		testResult("object.age != \"\"", 13, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.name != null
	 * </pre>
	 *
	 * (comparison: different from null).
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnequalNullComparison() throws Exception {
		testResult("object.name != null", 13, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.age == null
	 * </pre>
	 *
	 * (equals null).
	 *
	 * @throws Exception
	 */
	@Test
	public void testEqualNullComparison() throws Exception {
		testResult("object.age == null", 12, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.age != 42
	 * </pre>
	 *
	 * (different nonnull).
	 *
	 * @throws Exception
	 */
	@Test
	public void testUnequalNonNullComparison() throws Exception {
		testResult("object.age != 42", 12, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.age == 42
	 * </pre>
	 *
	 * (equals nonnull).
	 *
	 * @throws Exception
	 */
	@Test
	public void testEqualNonNullComparison() throws Exception {
		testResult("object.age == 42", 1, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.contentid CONTAINSONEOF subrule(&quot;contentid&quot;, &quot;subobject.name == \&quot;leaf2\&quot;&quot;)
	 * </pre>
	 *
	 * (subrule function).
	 *
	 * @throws Exception
	 */
	@Test
	public void testSubRuleFunction() throws Exception {
		testResult("object.contentid CONTAINSONEOF subrule(\"contentid\", \"subobject.name == \\\"leaf2\\\"\")", 1, -1, null, false);
	}

	/**
	 * Test function isempty with literal values (null and nonnull).
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticIsemptyFunction() throws Exception {
		testResult("isempty(42)", 0, -1);
		testResult("isempty(null)", 13, -1);
	}

	/**
	 * Test function isempty with resolvables that resolve to null or nonnull.
	 *
	 * @throws Exception
	 */
	@Test
	public void testSemiStaticIsemptyFunction() throws Exception {
		Map data = new HashMap();

		data.put("nnull", null);
		data.put("nonnull", new Integer(42));

		testResult("isempty(data.nnull)", 13, -1, data);
		testResult("isempty(data.nonnull)", 0, -1, data);
	}

	/**
	 * Test function isempty with variable operand.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVariableIsemptyFunction() throws Exception {
		testResult("isempty(object.age)", 12, -1);
	}

	/**
	 * Test result caching (perform a query, modify the data in the background, repeat the query -&gt; same results as before)
	 *
	 * @throws Exception
	 */
	@Test
	public void testResultsCache() throws Exception {
		// now we use the cached datasource
		ds = cachedDs;
		// check the results once (putting the results into the cache)
		testResult("object.age == 42", 1, -1);

		// now be a bad boy and change the data directly in the database
		DBHandle dbHandle = ds.getHandle().getDBHandle();

		DB.update(dbHandle, "insert into " + dbHandle.getContentAttributeName() + " (contentid, name, value_int) values (?, ?, ?)", new Object[] {
				topNode.get("contentid"), "age", new Integer(42) });

		// now check the same expression again (should have the same results)
		testResult("object.age == 42", 1, -1);

		// and do the cross-check, using the non cached ds
		ds = nonCachedDs;

		// now we should see the modified object
		testResult("object.age == 42", 2, -1);
	}

	/**
	 * Test assignment expression
	 *
	 * <pre>
	 * portal.user.name = 'Franz'
	 * </pre>
	 *
	 * (failure to generate filter for assignment).
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalAssignment() throws Exception {
		testExpressionFailure("portal.user.name = 'Franz'", new String[] { "assignment" });
	}

	/**
	 * Test illegal expression
	 *
	 * <pre>
	 * object.name == null or portal.user.name = 'Franz'
	 * </pre>
	 *
	 * (illegal expression)
	 *
	 * @throws Exception
	 */
	@Test
	public void testIllegalExpression2() throws Exception {
		testExpressionFailure("object.name == null or portal.user.name = 'Franz'", new String[] { "parsing" });
	}

	/**
	 * Test a fancy combination of NOT, isempty() function and a static boolean.
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticBoolean() throws Exception {
		testResult("!isempty(object.name) && true", 13, -1);
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void testTransactionalAccess() throws Exception {
		// deactivate this test for mssql
		if ("Microsoft SQL Server".equals(DB.getDatabaseProductName(ds.getHandle().getDBHandle()))) {
			return;
		}

		// start a transaction
		DB.startTransaction(ds.getHandle().getDBHandle());

		Thread t = new Thread() {

			/*
			 * (non-Javadoc)
			 *
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				try {
					// start a transaction here
					DB.startTransaction(ds.getHandle().getDBHandle());

					synchronized (this) {
						logger.info(Thread.currentThread().getName() + " is notifying main thread");

						notify();

						logger.info(Thread.currentThread().getName() + " is waiting now");
						// now wait until woken up
						wait();

						logger.info(Thread.currentThread().getName() + " is modifying the data");
						// read the object to modify
						Changeable leaf1Mod = PortalConnectorFactory.getChangeableContentObject(
								ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);

						// modifiy the name
						leaf1Mod.setProperty("name", leaf1Mod.getProperty("name") + "_mod");

						// store the object
						ds.store(Collections.singleton(leaf1Mod));

						logger.info(Thread.currentThread().getName() + " is notifying main thread");

						notify();

						logger.info(Thread.currentThread().getName() + " is waiting now");
						// now wait again
						wait();

						logger.info(Thread.currentThread().getName() + " is commiting the transaction");
						// end the transaction
						DB.commitTransaction(ds.getHandle().getDBHandle());
					}
				} catch (Exception e) {
				}
			}
		};

		System.out.println(Thread.currentThread().getName() + " is starting a new thread");

		synchronized (t) {
			// start the thread
			t.start();
			logger.info(Thread.currentThread().getName() + " is waiting now (#1)");
			t.wait();
			logger.info(Thread.currentThread().getName() + " continues");
		}

		Changeable leaf1Mod = null;

		synchronized (t) {
			logger.info(Thread.currentThread().getName() + " is notifying the subthread #1 to modify data");
			// let the thread modify the object
			t.notify();

			logger.info(Thread.currentThread().getName() + " is waiting...");
			t.wait();

			// now read the "modified" object
			logger.info(Thread.currentThread().getName() + " is getting the modified object");
			leaf1Mod = PortalConnectorFactory.getChangeableContentObject(ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);
			// access the property "name"
			leaf1Mod.getProperty("name");
			logger.info(Thread.currentThread().getName() + " is notifying the subthread #2 to commit transaction");
			// wake the thread
			t.notify();
		}

		logger.info(Thread.currentThread().getName() + " is waiting for subthread to finish");

		int waitTime = 0;

		while (t.isAlive() && waitTime < TRANSACTIONTEST_MAXWAIT) {
			// waiting...
			Thread.sleep(1000);
			waitTime++;
		}

		if (t.isAlive()) {
			// subthread is still alive (and should not be)
			// kill the subthread and fail the test
			t.interrupt();
			throw new Exception("Subthread did not finish within " + TRANSACTIONTEST_MAXWAIT + " seconds.");
		}

		logger.info("subthread finished.");

		// end the transaction
		DB.commitTransaction(ds.getHandle().getDBHandle());

		// check whether the name has not been changed
		assertEquals("Check whether the modification in the other transaction is not visible to this transaction", leaf1.getProperty("name"),
				leaf1Mod.getProperty("name"));

		// now read again
		leaf1Mod = PortalConnectorFactory.getChangeableContentObject(ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);

		// check whether the name has now been changed
		assertNotSame("Check whether the modification in the other transaction is now visible to this transaction", leaf1.getProperty("name"),
				leaf1Mod.getProperty("name"));
	}

	/**
	 * Test whether the rollback of transaction works
	 *
	 * @throws Exception
	 */
	@Test
	public void testRollback() throws Exception {
		// start a transaction
		DB.startTransaction(ds.getHandle().getDBHandle());

		Changeable leaf1Mod = PortalConnectorFactory.getChangeableContentObject(ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);

		// modify the object and store it
		leaf1Mod.setProperty("name", leaf1Mod.getProperty("name") + "_mod");

		// store the object
		ds.store(Collections.singleton(leaf1Mod));

		// read the object again
		leaf1Mod = PortalConnectorFactory.getChangeableContentObject(ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);

		// whether modification is visible now
		assertNotSame("Check whether the modification is visible to this transaction", leaf1.getProperty("name"), leaf1Mod.getProperty("name"));

		// rollback the transaction
		DB.rollbackTransaction(ds.getHandle().getDBHandle());

		// read the object again
		leaf1Mod = PortalConnectorFactory.getChangeableContentObject(ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);

		// check whether the name has not been changed
		assertEquals("Check whether the rolled back modification is not visible any more", leaf1.getProperty("name"), leaf1Mod.getProperty("name"));
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

		testResult("matches(data.match, object.obj_type == 2 && object.aliases CONTAINSONEOF this.aliases && object.shortname == this.shortname)", 2,
				-1, dataMap);
	}

	@Test
	public void testLimitResult() throws Exception {
		Expression expression = PortalConnectorFactory.createExpression("object.obj_type == 2");
		DatasourceFilter filter = ds.createDatasourceFilter(expression);
		Collection result = ds.getResult(filter, new String[] { "shortname" }, 2, 5, new Datasource.Sorting[] { new Datasource.Sorting("shortname",
				Datasource.SORTORDER_ASC) });

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
		Collection result = ds.getResult(filter, new String[] { "shortname" }, 2, -1, new Datasource.Sorting[] { new Datasource.Sorting("shortname",
				Datasource.SORTORDER_ASC) });

		assertEquals("Verifying result count", 6, result.size());
		Iterator i = result.iterator();

		assertEquals("Verifying result.", ((Resolvable) i.next()).get("shortname"), "leaf3");
	}

	@Test
	public void testLimitResultNoStartNoMax() throws Exception {
		Expression expression = PortalConnectorFactory.createExpression("object.obj_type == 2");
		DatasourceFilter filter = ds.createDatasourceFilter(expression);
		Collection result = ds.getResult(filter, new String[] { "shortname" }, -1, -1, new Datasource.Sorting[] { new Datasource.Sorting("shortname",
				Datasource.SORTORDER_ASC) });

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
				new Datasource.Sorting("parentnode.shortname", Datasource.SORTORDER_ASC),
				new Datasource.Sorting("shortname", Datasource.SORTORDER_DESC) });

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
				new Datasource.Sorting("parentnode.shortname", Datasource.SORTORDER_ASC),
				new Datasource.Sorting("shortname", Datasource.SORTORDER_DESC) }));

		checkSortingOfObjects(objects, false);
	}

	@Test
	public void testFilterableResolvableObjectLink() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) leaf1).getFiltered("parentnode", null);

		logger.info("Expression: " + filter.getExpressionString());
		Collection parentnode = ds.getResult(filter, null, 0, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("assert that we got only one object", 1, parentnode.size());

		Resolvable pnode = (Resolvable) parentnode.iterator().next();

		logger.info("parent node: " + pnode.get("contentid"));
		assertEquals("asserting that we got the root node", ((Resolvable) leaf1.get("parentnode")).get("contentid"), pnode.get("contentid"));
	}

	@Test
	public void testFilterableResolvableObjectLinkWithFilter() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) leaf1).getFiltered("parentnode", "1 == 1");

		logger.info("Expression: " + filter.getExpressionString());
		Collection parentnode = ds.getResult(filter, null, 0, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

		assertEquals("assert that we got only one object", 1, parentnode.size());

		Resolvable pnode = (Resolvable) parentnode.iterator().next();

		logger.info("parent node: " + pnode.get("contentid"));
		assertEquals("asserting that we got the root node", ((Resolvable) leaf1.get("parentnode")).get("contentid"), pnode.get("contentid"));

		DatasourceFilter filter2 = ((FilterableResolvable) leaf1).getFiltered("parentnode", "1 == 0");

		logger.info("Expression: " + filter2.getExpressionString());
		Collection parentnode2 = ds.getResult(filter2, null, 0, -1, new Datasource.Sorting[] { new Datasource.Sorting("name",
				Datasource.SORTORDER_DESC) });

		assertEquals("assert that we got no object", 0, parentnode2.size());
	}

	@Test
	public void testFilterableResolvableForeignLink() throws Exception {
		DatasourceFilter filter = ((FilterableResolvable) topNode).getFiltered("subnodes", null);
		Collection subnodes = ds.getResult(filter, null, 0, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

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
		Collection subnodes = ds.getResult(filter, null, 0, -1,
				new Datasource.Sorting[] { new Datasource.Sorting("name", Datasource.SORTORDER_DESC) });

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

		DB.query(ds.getHandle().getDBHandle(), "select * from " + ds.getHandle().getDBHandle().getContentAttributeName() + " where contentid = '"
				+ object.get("contentid") + "' and name = 'aliases' order by sortorder", new ResultProcessor() {
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					dbValues.add(rs.getString("value_text"));
				}
			}

			public void takeOver(ResultProcessor p) {
			}
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
		DB.query(ds.getHandle().getDBHandle(), "select * from " + ds.getHandle().getDBHandle().getContentAttributeName() + " where contentid = '"
				+ object.get("contentid") + "' and name = 'aliases' order by sortorder", new ResultProcessor() {
			public void process(ResultSet rs) throws SQLException {
				while (rs.next()) {
					dbValues.add(rs.getString("value_text"));
				}
			}

			public void takeOver(ResultProcessor p) {
			}
		});

		// now check that the db contains exactly the values, we get when
		// fetching the object via the connector
		assertEquals("Check whether db contains exactly the values that are fetched", ds.create(params).get("aliases"), dbValues);
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
		TimingUtils.waitForNextSecond();

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
	 * Test filtering a very long LOB attribute with like
	 *
	 * @throws Exception
	 */
	@Test
	public void testVeryLongLOBAttributeWithLike() throws Exception {
		testResult("object.longname LIKE '%" + SEARCHED_IN_LONG_VALUE + "%'", 5, -1);
	}

}
