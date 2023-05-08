/*
 * @author norbert
 * @date 12.07.2006
 * @version $Id: CNDatasourceFilterTest.java,v 1.2.2.1 2011-04-07 10:09:30 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.db.DB;
import com.gentics.lib.db.DBHandle;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;



/**
 * Test for creation of DatasourceFilter for CNDatasources.
 */
@Category(BaseLibTest.class)
public class CNDatasourceFilter4Test extends AbstractCNDatasourceFilterTest {

	public CNDatasourceFilter4Test(TestDatabase testDatabase) throws Exception {
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

	// public void testEvalFunction() throws Exception {
	// Map dataMap = new HashMap();
	// dataMap.put("rule", "isempty(object.age)");
	// testResult("eval(data.rule)", 12, -1, dataMap);
	// }

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
						Changeable leaf1Mod = PortalConnectorFactory.getChangeableContentObject(ObjectTransformer.getString(leaf1.get("contentid"), ""), ds);

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
				} catch (Exception e) {}
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

}
