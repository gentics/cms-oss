/*
 * @author norbert
 * @date 12.07.2006
 * @version $Id: CNDatasourceFilterVersioningTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.etc.StringUtils;
import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.SQLUtils;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;

/**
 * Test for creation of DatasourceFilter for CNDatasources. These supplement the other datasource tests with some versioning tests
 */
@Category(BaseLibTest.class)
public class CNDatasourceFilterVersioningTest extends CNDatasourceFilterTest {

	/**
	 * Map of db identifiers to dump filenames
	 */
	public static Map<String, String> dumpFileNames = new HashMap<String, String>();

	static {
		dumpFileNames.put("generic", "filter_tests_data.sql");
		dumpFileNames.put("mssql08", "filter_tests_data_mssql.sql");
	}

	/**
	 * Constructor for the test
	 */
	public CNDatasourceFilterVersioningTest(TestDatabase testDatabase) throws Exception {
		super(testDatabase);
		this.versioning = "true";
	}

	/**
	 * Get variation data
	 * @return variation data
	 * @throws JDBCMalformedURLException
	 */
	@Parameters(name = "{index}: singleDBTest: {0}")
	public static Collection<Object[]> data() throws JDBCMalformedURLException {
		return Arrays.asList(getData(TestDatabaseVariationConfigurations.MYSQL_VARIATIONS));
	}

	/**
	 * Get the dump filename for the test database
	 * @return dump filename
	 */
	protected String getDumpFileName() {
		TestDatabase testDatabase = getTestDatabase();

		if (dumpFileNames.containsKey(testDatabase.getIdentifier())) {
			return dumpFileNames.get(testDatabase.getIdentifier());
		} else {
			return dumpFileNames.get("generic");
		}
	}

	@Override
	protected void insertTestData(SQLUtils sqlUtils) throws Exception {
		BufferedReader dataReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(getDumpFileName()), "UTF-8"));
		String statement = null;

		while ((statement = dataReader.readLine()) != null) {
			statement = statement.trim();
			if (!StringUtils.isEmpty(statement)) {
				sqlUtils.executeQueryManipulation(statement);
			}
		}
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *   object.shortname LIKE &quot;modified%&quot;
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedDirectOptimizedAttribute() throws Exception {
		testVersionedResults("object.shortname LIKE \"modified%\"", new int[] { 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.age &gt;= 42 &amp;&amp; object.age &lt; 4711
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedDirectNormalAttribute() throws Exception {
		testVersionedResults("object.age >= 42 && object.age < 4711", new int[] { 1, 1, 1, 1, 2, 1, 1, 1, 1, 1 });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.parentnode == data.node
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedDirectLinkAttribute() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("node", topNode);
		testVersionedResults("object.parentnode == data.node", new int[] { 4, 4, 4, 4, 5, 5, 5, 5, 5, 4 }, data, true);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *   object.leafs CONTAINSONEOF data.node
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedDirectForeignlinkAttribute() throws Exception {
		// search the parent of fullnode
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("node", newLeaf);
		testVersionedResults("object.leafs CONTAINSONEOF data.node", new int[] { 0, 1, 1, 0, 1, 1, 1, 1, 1, 0 }, data, true);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.parentnode.shortname LIKE &quot;modi%&quot;
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedLinkedOptimizedAttribute() throws Exception {
		testVersionedResults("object.parentnode.shortname LIKE \"modi%\"", new int[] { 0, 0, 0, 0, 0, 4, 0, 0, 0, 0 });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.parentnode.age == 40 + 2
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedLinkedNormalAttribute() throws Exception {
		testVersionedResults("object.parentnode.age == 40 + 2", new int[] { 2, 3, 2, 2, 2, 2, 2, 2, 2, 2 });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.parentnode.parentnode CONTAINSONEOF data.nodes
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedLinkedLinkedAttribute() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		Collection<Changeable> nodes = new Vector<Changeable>();

		nodes.add(fullNode);
		nodes.add(topNode);
		data.put("nodes", nodes);
		testVersionedResults("object.parentnode.parentnode CONTAINSONEOF data.nodes", new int[] { 8, 9, 9, 8, 8, 8, 8, 8, 8, 8 }, data, true);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.parentnode.leafs CONTAINSONEOF data.node
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedLinkedForeignlinkedAttribute() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("node", leaf7);
		testVersionedResults("object.parentnode.leafs CONTAINSONEOF data.node", new int[] { 4, 4, 4, 4, 5, 5, 5, 5, 5, 4 }, data, true);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.leafs.shortname LIKE &quot;mod%leaf%&quot;
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedForeignlinkedOptimizedAttribute() throws Exception {
		testVersionedResults("object.leafs.shortname LIKE \"mod%leaf%\"", new int[] { 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.leafs.name CONTAINSONEOF [&quot;newleaf&quot;, &quot;modifiednewleaf&quot;]
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedForeignLinkedNormalAttribute() throws Exception {
		testVersionedResults("object.leafs.name CONTAINSONEOF [\"newleaf\", \"modifiednewleaf\"]", new int[] { 0, 1, 1, 0, 1, 1, 1, 1, 1, 0 });
	}

	/**
	 * test expressions with a foreign link attribute which is referenced by a optimized link attribute.
	 */
	@Test
	public void testVersionedOptimizedForeignLinkedNormalAttribute() throws Exception {
		testVersionedResults("object.quickleafs.name CONTAINSONEOF [\"newleaf\", \"modifiednewleaf\"]", new int[] { 0, 1, 1, 0, 1, 1, 1, 1, 1, 0 });
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.leafs.parentnode == data.node
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedForeignLinkedLinkedAttribute() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("node", emptyNode);
		testVersionedResults("object.leafs.parentnode == data.node", new int[] { 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 }, data, true);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.subnodes.leafs CONTAINSONEOF data.node
	 * </pre>
	 *
	 * at various timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedForeignLinkedForeignLinkAttribute() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("node", newLeaf);
		testVersionedResults("object.subnodes.leafs CONTAINSONEOF data.node", new int[] { 0, 1, 1, 0, 0, 0, 0, 0, 0, 0 }, data, true);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.name CONTAINSONEOF subrule(&quot;name&quot;, &quot;subobject.age == 40 + data.value&quot;)
	 * </pre>
	 *
	 * (subrule function) with different version timestamps.
	 *
	 * @throws Exception
	 */
	@Test
	public void testVersionedSubRuleFunction() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("value", new Integer(2));

		testVersionedResults("object.name CONTAINSONEOF subrule(\"name\", \"subobject.age == 40 + data.value\")", new int[] { 1, 1, 1, 1, 2, 1, 1, 1, 1, 1 },
				data, false);
	}

	/**
	 * Test the datasource against the given expression using all configured versiontimestamps.
	 *
	 * @param testExpressionString
	 *            expression to test
	 * @param numExpectedObjects
	 *            array of number of expected objects for each timestamp
	 * @throws Exception
	 */
	protected void testVersionedResults(String testExpressionString, int[] numExpectedObjects) throws Exception {
		testVersionedResults(testExpressionString, numExpectedObjects, null, true);
	}

	/**
	 * Test the datasource against the given expression using all configured versiontimestamps.
	 *
	 * @param testExpressionString
	 *            expression to test
	 * @param numExpectedObjects
	 *            array of number of expected objects for each timestamp
	 * @param parameters
	 *            map of additional parameters. may be null or empty
	 * @param matchObjects
	 *            true when fetched objects shall be matched against the rule, false if not
	 * @throws Exception
	 */
	public void testVersionedResults(String testExpressionString, int[] numExpectedObjects, Map<String, Object> parameters, boolean matchObjects) throws Exception {
		assertEquals("Check given number of expectations", ALLTIMESTAMPS.length, numExpectedObjects.length);
		for (int i = 0; i < ALLTIMESTAMPS.length; ++i) {
			testResult(testExpressionString, numExpectedObjects[i], ALLTIMESTAMPS[i], parameters, matchObjects);
		}
	}
}
