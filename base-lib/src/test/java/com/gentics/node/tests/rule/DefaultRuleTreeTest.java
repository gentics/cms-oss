package com.gentics.node.tests.rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import com.gentics.contentnode.tests.category.BaseLibTest;
import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceNotAvailableException;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.rule.LogicalOperator;
import com.gentics.api.lib.rule.Rule;
import com.gentics.api.lib.rule.RuleTree;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.lib.parser.rule.DefaultRuleTree;
import com.gentics.lib.parser.rule.RuleTreeHelper;
import com.gentics.portalconnector.tests.AbstractLegacyNavigationDumpTest;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtilException;
import org.junit.experimental.categories.Category;

/**
 * A "simple" test to test the DefaultRuleTree - creates multiple threads and applys a ruletree on a datasource and verifies results.
 *
 * It is actually completely useless, since it now actually tests the datasource filter.. but ... anyway..
 *
 * @author herbert
 *
 */
@Ignore
@Category(BaseLibTest.class)
public class DefaultRuleTreeTest extends AbstractLegacyNavigationDumpTest {
	protected static final int TESTDATA_CORRECT = 2;

	protected int threadCount;
	private RuleTree basicRuleTree;
	private int resnumber;

	/**
	 * Test collections for testing 'CONTAINSONEOF'
	 */
	protected List testColl;

	private Random random;

	@Before
	public void setUp() throws Exception {
		prepareTestDatabase(getClass().getSimpleName());

		// Deativate new expression parser.
		System.setProperty("portal.expressionparser", "false");
		random = new Random();

		testColl = new ArrayList();
		testColl.add("10002.10067");
		testColl.add("10002.10093");
	}

	@After
	public void tearDown() throws Exception {
		removeTestDatabase();
	}

	/**
	 * Starts the actual test of the deepCopy...
	 *
	 * @throws IOException
	 * @throws ParserException
	 * @throws DatasourceNotAvailableException
	 * @throws InterruptedException
	 * @throws SQLUtilException
	 * @throws JDBCMalformedURLException
	 */
	@Test
	public void testDeepCopy() throws IOException, ParserException, DatasourceNotAvailableException, InterruptedException, JDBCMalformedURLException,
				SQLUtilException {

		insertDumpIntoDatabase();
		Datasource ds = getDatasource();
		DefaultRuleTree ruleTree = new DefaultRuleTree();
		Map map = new HashMap();

		map.put("testdata", Integer.toString(TESTDATA_CORRECT));
		map.put("parent", "10002.10000");
		map.put("testcoll", null);
		ruleTree.addResolver("test", map);
		// ruleTree.parse("(object.folder_id == 10002.10001 || test == abc) && test.testdata == 345");
		// ruleTree.parse("object.folder_id == 10002.10000");
		// ruleTree.parse("object.folder_id LIKE 10002.%");

		ruleTree.parse(
				"(object.folder_id LIKE '10002.%' || test == abc) && test.testdata == 2 && (test.enablecontainsoneof == \"\" || object.contentid CONTAINSONEOF test.testcoll)");

		// map.put("enablecontainsoneof","true");
		// map.put("testcoll",testColl);
		// ruleTree.parse("object.contentid CONTAINSONEOF test.testcoll");
		this.basicRuleTree = ruleTree;
		ds.setRuleTree(ruleTree);
		Collection coll = ds.getResult();

		this.resnumber = coll.size();
		assertNotSame(new Integer(resnumber), new Integer(0));
		// System.out.println("Got: " + coll.size());

		// System.out.println("Starting threads ...");
		int numberThreads = 100;

		for (int i = 0; i < numberThreads; i++) {
			new Thread(new RuleTestRun(this)).start();
		}
		Thread.sleep(500);
		synchronized (this) {
			assertEquals(numberThreads, threadCount);
			this.notifyAll();

			while (threadCount > 0) {
				this.wait();
				// System.out.println("Left: " + threadCount);
			}
		}
	}

	public RuleTree getRuleTree() {
		return basicRuleTree.deepCopy();
	}

	public Datasource getDatasource() throws JDBCMalformedURLException, SQLUtilException {
		Map dsProperties = new HashMap();

		dsProperties.put("sanitycheck", "false");

		Properties handleProperties = getTestDatabase().getSettings();

		return PortalConnectorFactory.createDatasource(handleProperties, dsProperties);
	}

	public class RuleTestRun implements Runnable {
		private DefaultRuleTreeTest parent;

		public RuleTestRun(DefaultRuleTreeTest parent) {
			this.parent = parent;
		}

		public void run() {
			int id;

			synchronized (parent) {
				try {
					id = parent.threadCount++;
					// System.out.println("id: " + id);
					parent.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
			// System.out.println(id + " Starting work..");

			try {
				Map map = new HashMap();

				for (int i = 0; i < 10; i++) {
					RuleTree ruleTree = getRuleTree();
					int testdata = random.nextInt(4);

					if (testdata == 3) {
						map.put("testcoll", parent.testColl);
						map.put("enablecontainsoneof", "true");
						map.put("testdata", Integer.toString(TESTDATA_CORRECT));
					} else {
						map.put("testdata", Integer.toString(testdata));
						map.put("testcoll", null);
						map.put("enablecontainsoneof", "");
					}
					ruleTree.addResolver("test", map);
					Datasource ds = getDatasource();

					ds.setRuleTree(ruleTree);
					Collection coll;

					try {
						coll = ds.getResult();
					} catch (DatasourceNotAvailableException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
					// System.out.println(id + " got " + coll.size());
					try {
						if (testdata == TESTDATA_CORRECT) {
							assertEquals(parent.resnumber, coll.size());
						} else if (testdata == 3) {
							assertEquals(parent.testColl.size(), coll.size());
						} else {
							assertEquals(0, coll.size());
						}
					} catch (AssertionFailedError e) {
						System.err.println("Error with testdata: " + testdata);
						throw e;
					}
				}
			} catch (JDBCMalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLUtilException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				// System.out.println(id + " finished.");
				synchronized (parent) {
					parent.threadCount--;
					parent.notify();
				}
			}
		}
	}

	/**
	 * disabled test because completely useless
	 *
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testConcatOperation() throws Exception {
		RuleTree ruleTree = new DefaultRuleTree();

		ruleTree.parse("test LIKE concat(%,est)");
		for (int i = 0; i < 10000; i++) {
			// Match once to make sure all classes are loaded
			Rule rule = new Rule(ruleTree);

			rule.match();
		}
		System.gc();
		System.gc();
		System.gc();
		Runtime r = Runtime.getRuntime();
		long memory = r.maxMemory() - r.freeMemory();
		long started = System.currentTimeMillis();
		List ruleList = new ArrayList(1000);

		for (int i = 0; i < 10000; i++) {
			Rule rule = new Rule(ruleTree);

			rule.match();
			ruleList.add(rule);
		}
		long ended = System.currentTimeMillis();

		System.gc();
		System.gc();
		System.gc();
		System.out.println("CONCAT Duration: " + (ended - started));
		long usedMemory = ((r.maxMemory() - r.freeMemory()) - memory);

		assertTrue("Testing if memory usage was not too high - expected: {<260000} was: {" + usedMemory + "}", usedMemory < 260000);
		assertTrue("Testing if memory usage was lower than expected - if memory improvements were made, modify this test ! {>200000} was: {" + usedMemory + "}",
				usedMemory > 200000);
		System.out.println("CONCAT Memory  : " + usedMemory);
	}

	/**
	 * disabled test because completely useless
	 *
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testLikeOperation() throws Exception {
		RuleTree ruleTree = new DefaultRuleTree();

		ruleTree.parse("test LIKE %est");

		for (int i = 0; i < 10000; i++) {
			// Match once to make sure all classes are loaded
			Rule rule = new Rule(ruleTree);

			rule.match();
		}
		System.gc();
		System.gc();
		System.gc();
		Runtime r = Runtime.getRuntime();
		long memory = r.maxMemory() - r.freeMemory();
		long started = System.currentTimeMillis();
		List ruleList = new ArrayList(1000);

		for (int i = 0; i < 10000; i++) {
			Rule rule = new Rule(ruleTree);

			rule.match();
			ruleList.add(rule);
		}
		long ended = System.currentTimeMillis();

		System.gc();
		System.gc();
		System.gc();
		System.out.println("LIKE Duration  : " + (ended - started));
		long usedMemory = ((r.maxMemory() - r.freeMemory()) - memory);

		assertTrue("Testing if memory usage was not too high - expected: {<3000000} was: {" + usedMemory + "}", usedMemory < 300000);
		assertTrue(
				"Testing if memory usage was lower than expected - if memory improvements were made, modify this test ! expected: {>100000} was: {" + usedMemory + "}",
				usedMemory > 100000);
		System.out.println("LIKE Memory    : " + usedMemory);
	}

	/**
	 * Test whether a deep copy of a ruletree preserves the resolvers
	 *
	 * @throws Exception
	 */
	@Test
	public void testDeepCopyResolver() throws Exception {
		System.setProperty("portal.expressionparser", "false");
		doTestDeepCopyResolver();
		System.setProperty("portal.expressionparser", "");
	}

	/**
	 * Test whether a deep copy of a ruletree preserves the resolvers when expression parser is used
	 *
	 * @throws Exception
	 */
	@Test
	public void testDeepCopyResolverExpressionParser() throws Exception {
		System.setProperty("portal.expressionparser", "true");
		doTestDeepCopyResolver();
		System.setProperty("portal.expressionparser", "");
	}

	/**
	 * Perform the deep copy - resolver test
	 *
	 * @throws Exception
	 */
	protected void doTestDeepCopyResolver() throws Exception {
		RuleTree orig = new DefaultRuleTree();
		Map data = new HashMap();

		data.put("one", "one");
		data.put("two", "two");
		orig.addResolver("data", data);
		orig.parse("data.one == \"one\" && data.two == \"two\" && isempty(data.three)");

		Rule rule = new Rule(orig);

		assertTrue("Check whether original rule matches", rule.match());

		// now create a deep copy of the rule
		RuleTree copy = orig.deepCopy();
		Rule ruleCopy = new Rule(copy);

		assertTrue("Check whether copy of the rule matches", ruleCopy.match());
	}

	/**
	 * Test whether the ruletree helper works
	 *
	 * @throws Exception
	 */
	@Test
	public void testRuleTreeHelper() throws Exception {
		System.setProperty("portal.expressionparser", "false");
		doTestRuleTreeHelper();
		System.setProperty("portal.expressionparser", "");
	}

	/**
	 * Test whether the ruletree helper works when expression parser is used
	 *
	 * @throws Exception
	 */
	@Test
	public void testRuleTreeHelperExpressionParser() throws Exception {
		System.setProperty("portal.expressionparser", "true");
		doTestRuleTreeHelper();
		System.setProperty("portal.expressionparser", "");
	}

	/**
	 * Perform the ruletreehelper test
	 *
	 * @throws Exception
	 */
	protected void doTestRuleTreeHelper() throws Exception {
		RuleTree firstRuleTree = new DefaultRuleTree();
		Map data = new HashMap();

		data.put("one", "one");
		data.put("two", "two");
		firstRuleTree.addResolver("data", data);
		firstRuleTree.parse("data.one == \"one\"");

		RuleTree secondRuleTree = new DefaultRuleTree();

		secondRuleTree.addResolver("data", data);
		secondRuleTree.parse("data.two == \"two\"");

		RuleTree combinedRuleTree = RuleTreeHelper.concat(firstRuleTree, secondRuleTree, LogicalOperator.OPERATOR_AND);
		Rule rule = new Rule(combinedRuleTree);

		assertTrue("Check whether the combined rule matches", rule.match());
	}
}
