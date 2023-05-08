/*
 * @author norbert
 * @date 12.07.2006
 * @version $Id: CNDatasourceFilterTest.java,v 1.2.2.1 2011-04-07 10:09:30 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.testutils.database.JDBCMalformedURLException;
import com.gentics.testutils.database.TestDatabase;
import com.gentics.testutils.database.variations.TestDatabaseVariationConfigurations;



/**
 * Test for creation of DatasourceFilter for CNDatasources.
 */
@Category(BaseLibTest.class)
public class CNDatasourceFilter5Test extends AbstractCNDatasourceFilterTest {

	public CNDatasourceFilter5Test(TestDatabase testDatabase) throws Exception {
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
	 * Test whether the empty rule fetches all objects
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmptyRule() throws Exception {
		// check number of all objects
		testResult(EMPTYRULE, 13, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.obj_type == 2
	 * </pre>
	 *
	 * (direct access to an optimized attribute).
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectOptimizedAttribute() throws Exception {
		// search for leafs only
		testResult("object.obj_type == 2", 8, -1);
	}

	/**
	 * Test query for an optimized int attribute (with isempty)
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectOptimizedIntAttribute() throws Exception {
		testResult("!isempty(object.optimizedint)", 8, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *       !(object.name LIKE &quot;leaf%&quot;)
	 * </pre>
	 *
	 * (direct access to a normal attribute).
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectNormalAttribute() throws Exception {
		// search all non-leafs (==nodes)
		testResult("!(object.name LIKE \"leaf%\")", 5, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.parentnode == data.node
	 * </pre>
	 *
	 * (direct access to a linked attribute).
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectLinkedAttribute() throws Exception {
		// search for children of fullnode
		Map data = new HashMap();

		data.put("node", fullNode);
		testResult("object.parentnode == data.node", 4, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *       object.subnodes CONTAINSONEOF data.node
	 * </pre>
	 *
	 * (direct access to a foreignlinked attribute)
	 *
	 * @throws Exception
	 */
	@Test
	public void testDirectForeignLinkedAttribute() throws Exception {
		// search the parent of fullnode
		Map data = new HashMap();

		data.put("node", fullNode);
		testResult("object.subnodes CONTAINSONEOF data.node", 1, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.parentnode.obj_type &lt; 2
	 * </pre>
	 *
	 * (access to an optimized attribute of a linked object).
	 *
	 * @throws Exception
	 */
	@Test
	public void testLinkedOptimizedAttribute() throws Exception {
		// search for objects that link to folders
		testResult("object.parentnode.obj_type < 2", 12, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *       object.parentnode.name LIKE &quot;subnode%&quot;
	 * </pre>
	 *
	 * (access to a normal attribute of a linked object).
	 *
	 * @throws Exception
	 */
	@Test
	public void testLinkedNormalAttribute() throws Exception {
		testResult("object.parentnode.name LIKE \"subnode%\"", 4, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.parentnode.parentnode == data.node
	 * </pre>
	 *
	 * (access to a linked object of a linked object).
	 *
	 * @throws Exception
	 */
	@Test
	public void testLinkedLinkedAttribute() throws Exception {
		// fetch grandchildren of topNode
		Map data = new HashMap();

		data.put("node", topNode);
		testResult("object.parentnode.parentnode == data.node", 4, -1, data);

		// now fetch grandchildren of leaf1 (which do not exist)
		data.put("node", leaf1);
		testResult("object.parentnode.parentnode == data.node", 0, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *       object.parentnode.parentnode CONTAINSONEOF data.nodes
	 * </pre>
	 *
	 * (access to a linked object of a linked object with CONTAINSONEOF and a collection of objects)
	 *
	 * @throws Exception
	 */
	@Test
	public void testLinkedLinkedAttributeContains() throws Exception {
		// fetch grandchildren of fullNode and topNode
		Map data = new HashMap();
		Collection nodes = new Vector();

		nodes.add(fullNode);
		nodes.add(topNode);
		data.put("nodes", nodes);

		testResult("object.parentnode.parentnode CONTAINSONEOF data.nodes", 4 + 4, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *       object.parentnode.subnodes CONTAINSONEOF data.node
	 * </pre>
	 *
	 * (access to a foreign linked object of a linked object).
	 *
	 * @throws Exception
	 */
	@Test
	public void testLinkedForeignLinkedAttribute() throws Exception {
		// fetch all nodes and leafs parallel to fullNode (including fullNode
		// itself)
		Map data = new HashMap();

		data.put("node", fullNode);
		testResult("object.parentnode.subnodes CONTAINSONEOF data.node", 4, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.leafs.obj_type != 1
	 * </pre>
	 *
	 * (access to an optimized attribute of foreign linked objects).
	 *
	 * @throws Exception
	 */
	@Test
	public void testForeignLinkedOptimizedAttribute() throws Exception {
		testResult("object.leafs.obj_type == 2", 4, -1);
	}

	/**
	 * test expressions with a foreign link attribute which is referenced by a optimized link attribute.
	 */
	@Test
	public void testOptimizedForeignLinkedAttribute() throws Exception {
		testResult("object.quicksubnodes.name LIKE \"%node%\"", 2, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *       object.subnodes.name LIKE &quot;%node%&quot;
	 * </pre>
	 *
	 * (access to a normal attribute of foreign linked objects).
	 *
	 * @throws Exception
	 */
	@Test
	public void testForeignLinkedNormalAttribute() throws Exception {
		testResult("object.subnodes.name LIKE \"%node%\"", 2, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.subnodes.parentnode == data.node
	 * </pre>
	 *
	 * (access to a linked attribute of foreign linked objects).
	 *
	 * @throws Exception
	 */
	@Test
	public void testForeignLinkedLinkedAttribute() throws Exception {
		// fetch all objects that have subnodes which have fullNode as parent
		// (hmm, this is fullNode itself, if fullNode has subnodes)

		Map data = new HashMap();

		data.put("node", fullNode);
		testResult("object.subnodes.parentnode == data.node", 1, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *   object.subnodes.leafs CONTAINSONEOF data.nodes
	 * </pre>
	 *
	 * (access to foreign linked objects of foreign linked objects).
	 *
	 * @throws Exception
	 */
	@Test
	public void testForeignLinkedForeignLinkedAttribute() throws Exception {
		// fetch all grandparents of leaf1, leaf3 and leaf5 (grandparent of
		// leaf1 and leaf3 is the same!)
		Map data = new HashMap();
		Collection nodes = new Vector();

		nodes.add(leaf1);
		nodes.add(leaf3);
		nodes.add(leaf5);
		data.put("nodes", nodes);

		testResult("object.subnodes.leafs CONTAINSONEOF data.nodes", 2, -1, data);
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
	public void testStaticConcatFunction() throws Exception {
		testResult("object.name == concat(\"subnode\", 1)", 1, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * object.name == concat(&quot;subnode&quot;, data.number)
	 * </pre>
	 *
	 * (usage of named function "concat" with resolvable as operand).
	 *
	 * @throws Exception
	 */
	@Test
	public void testSemiStaticConcatFunction() throws Exception {
		Map data = new HashMap();

		data.put("number", new Integer(2));
		testResult("object.name == concat(\"subnode\", data.number)", 1, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 * concat(object.name, &quot; &quot;, object.age) == &quot;subnode2 42&quot;
	 * </pre>
	 *
	 * (usage of named function "concat" with variable operands).
	 *
	 * @throws Exception
	 */
	@Test
	public void testVariableConcatFunction() throws Exception {
		testResult("concat(object.name, \" \", object.age) == \"subnode2 42\"", 1, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  &quot;leaf1&quot; CONTAINSONEOF [&quot;leaf1&quot;, &quot;leaf2&quot;]
	 * </pre>
	 *
	 * (all static CONTAINSONEOF).
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllStaticContainsOneOfFunction() throws Exception {
		testResult("\"leaf1\" CONTAINSONEOF [\"leaf1\", \"leaf2\"]", 13, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  &quot;leaf1&quot; CONTAINSNONE [&quot;leaf1&quot;, &quot;leaf2&quot;]
	 * </pre>
	 *
	 * (all static CONTAINSNONE).
	 *
	 * @throws Exception
	 */
	@Test
	public void testAllStaticContainsNoneFunction() throws Exception {
		testResult("\"leaf1\" CONTAINSNONE [\"leaf1\", \"leaf2\"]", 0, -1);
	}

	/**
	 * Test expressions
	 *
	 * <pre>
	 *  object.optimizedint CONTAINSONEOF [...]
	 *  object.contentid CONTAINSONEOF []
	 * </pre>
	 *
	 * @throws Exception
	 */
	@Test
	public void testOptimizedContainsOneOfFunction() throws Exception {
		Map<Object, Object> data = new HashMap<Object, Object>();

		data.put("values", Arrays.asList(43, 99));
		testResult("object.optimizedint CONTAINSONEOF [43, 99]", 0, -1);
		testResult("object.optimizedint CONTAINSONEOF data.values", 0, -1, data);
		testResult("!(object.optimizedint CONTAINSONEOF [43, 99])", 13, -1);
		testResult("!(object.optimizedint CONTAINSONEOF data.values)", 13, -1, data);

		data.put("values", Arrays.asList(42, 43, 99));
		testResult("object.optimizedint CONTAINSONEOF [42, 43, 99]", 1, -1);
		testResult("object.optimizedint CONTAINSONEOF data.values", 1, -1, data);
		testResult("!(object.optimizedint CONTAINSONEOF [42, 43, 99])", 12, -1);
		testResult("!(object.optimizedint CONTAINSONEOF data.values)", 12, -1, data);

		data.put("values", Arrays.asList(0, 42, 43, 99));
		testResult("object.optimizedint CONTAINSONEOF [0, 42, 43, 99]", 8, -1);
		testResult("object.optimizedint CONTAINSONEOF data.values", 8, -1, data);
		testResult("!(object.optimizedint CONTAINSONEOF [0, 42, 43, 99])", 5, -1);
		testResult("!(object.optimizedint CONTAINSONEOF data.values)", 5, -1, data);

		data.put("values", Arrays.asList(fullNode.get("contentid"), leaf2.get("contentid"), leaf5.get("contentid")));
		testResult("object.contentid CONTAINSONEOF data.values", 3, -1, data);
		testResult("!(object.contentid CONTAINSONEOF data.values)", 10, -1, data);

		Collection<String> values = new ArrayList<String>();

		values.add(null);
		data.put("values", values);
		testResult("object.contentid CONTAINSONEOF [null]", 0, -1, data);
		testResult("object.contentid CONTAINSONEOF data.values", 0, -1, data);
		testResult("!(object.contentid CONTAINSONEOF [null])", 13, -1, data);
		testResult("!(object.contentid CONTAINSONEOF data.values)", 13, -1, data);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.name CONTAINSONEOF [&quot;leaf1&quot;, &quot;leaf2&quot;]
	 * </pre>
	 *
	 * (static CONTAINSONEOF).
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticContainsOneOfFunction() throws Exception {
		testResult("object.name CONTAINSONEOF [\"leaf1\", \"leaf2\"]", 2, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *  object.name CONTAINSNONE [&quot;leaf1&quot;, &quot;leaf2&quot;]
	 * </pre>
	 *
	 * (static CONTAINSNONE).
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticContainsNoneFunction() throws Exception {
		testResult("object.name CONTAINSNONE [\"leaf1\", \"leaf2\"]", 11, -1);
	}

	/**
	 * Test expression
	 *
	 * <pre>
	 *   object.obj_type == 2 AND object.aliases CONTAINSNONE [&quot;cewl&quot;]
	 * </pre>
	 *
	 * (static CONTAINSNONE with multivalue attribute) and negation of CONTAINSNONE
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticMultiValueContainsNoneFunction() throws Exception {
		testResult("object.obj_type == 2 AND object.aliases CONTAINSNONE [\"cewl\"]", 7, -1);
		testResult("object.obj_type == 2 AND object.aliases CONTAINSONEOF [\"cewl\"]", 1, -1);
		testResult("object.obj_type == 2 AND !(object.aliases CONTAINSNONE [\"cewl\"])", 1, -1);
		testResult("object.obj_type == 2 AND object.aliases CONTAINSNONE []", 8, -1);
		testResult("object.obj_type == 2 AND object.aliases CONTAINSNONE [null]", 8, -1);
	}

	/**
	 * Test negation of CONTAINSONEOF
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticNotOneofFunction() throws Exception {
		testResult("object.obj_type == 2 AND object.aliases CONTAINSONEOF [\"cewl\"]", 1, -1);
		testResult("object.obj_type == 2 AND !(object.aliases CONTAINSONEOF [\"cewl\"])", 7, -1);
		testResult("object.obj_type == 2 AND object.aliases CONTAINSONEOF []", 0, -1);
		testResult("object.obj_type == 2 AND object.aliases CONTAINSONEOF [null,\"cewl\"]", 1, -1);
		testResult("object.obj_type == 2 AND object.aliases CONTAINSONEOF [null]", 0, -1);
	}

	/**
	 * Test CONTAINSNONE with multiple static values.
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticMultiValueContainsNoneFunctionWithMultiStatic() throws Exception {
		testResult("object.obj_type == 2 AND object.aliases CONTAINSNONE [\"cewl\", \"muh\"]", 6, -1);
	}

	/**
	 * Test combination of CONTAINSNONE and CONTAINSONEOF
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticMultiValueContainsNoneFunctionAndContainsOneOfCombination() throws Exception {
		testResult(
				"object.obj_type == 2 AND object.aliases CONTAINSNONE [\"cewl\", \"muh\"] AND object.aliases CONTAINSONEOF [\"ha\"] "
						+ "AND !(object.aliases CONTAINSONEOF [\"muh\"])",
						1,
						-1);
	}

}
