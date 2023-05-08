/*
 * @author norbert
 * @date 27.09.2006
 * @version $Id: HSQLDatasourceTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource.hsql;

import java.io.File;

import java.io.FileInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.*;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.CNWriteableDatasource;
import com.gentics.lib.datasource.SQLHandle;
import com.gentics.lib.datasource.SimpleHandlePool;
import com.gentics.testutils.GenericTestUtils;
import org.junit.experimental.categories.Category;

/**
 * Test case for internal HSQL database
 */
@Category(BaseLibTest.class)
public class HSQLDatasourceTest {

	/**
	 * datasource
	 */
	protected CNWriteableDatasource ds;

	/**
	 * constant for an empty rule
	 */
	public final static String EMPTYRULE = "";

	/**
	 * Create an instance of the test. initialize caching and the datasource
	 *
	 * @throws Exception
	 */
	public HSQLDatasourceTest() throws Exception {
		// initialize the cache
		GenericTestUtils.initConfigPathForCache();

		URL url = getClass().getResource("hsql-datasource.properties");

		if (!"file".equals(url.getProtocol())) {
			throw new Exception("This test can not be run in a jar file!");
		}

		File propertiesFile = new File(url.getPath());

		System.setProperty("dbrootpath", propertiesFile.getParent());

		// create the datasource
		Properties handleProperties = new Properties();

		handleProperties.load(new FileInputStream(propertiesFile));

		// create the datasource handle
		SQLHandle handle = new SQLHandle("hsql");

		handle.init(handleProperties);

		// create the datasource without caching
		Map dsProps = new HashMap();

		// we do not use caching here, but we use versioning
		dsProps.put("cache", "false");
		ds = new CNWriteableDatasource("hsql", new SimpleHandlePool(handle), dsProps);
	}

	/**
	 * Test whether the empty rule fetches all objects
	 *
	 * @throws Exception
	 */
	@Test
	public void testEmptyRule() throws Exception {
		// check number of all objects
		testResult(EMPTYRULE, 11);
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
		testResult("object.obj_type == 1002", 9);
	}

	/**
	 * Test a fancy combination of NOT, isempty() function and a static boolean
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticBoolean() throws Exception {
		testResult("!isempty(object.name) && true", 9);
	}

	/**
	 * Test the datasource against the given expression.
	 * <ol>
	 * <li>Parse the expression</li>
	 * <li>Generate a datasource filter and get the filtered result</li>
	 * <li>Check the number of filtered objects against the expexted number</li>
	 * <li>Check whether all filtered objects match the expression using the
	 * ExpressionEvaluator</li>
	 * <li>Check whether all not-filtered objects do not match the expression
	 * using the ExpressionEvaluator</li>
	 * </ol>
	 * Only if all of the above steps are processed without errors and give the
	 * expexted results, the overall test succeeds. In case of any error an
	 * exception is thrown.
	 *
	 * @param testExpressionString
	 *            expression to be tested
	 * @param numExpectedObjects
	 *            expected number of objects matching the expression
	 * @throws Exception
	 */
	protected void testResult(String testExpressionString,
			int numExpectedObjects) throws Exception {
		testResult(testExpressionString, numExpectedObjects, null, true);
	}

	/**
	 * Variant of {@link #testResult(String, int)} with an additional map of
	 * properties that can be access as [data.*] in the expression.
	 *
	 * @param testExpressionString
	 *            expression to be tested
	 * @param numExpectedObjects
	 *            expected number of objects matching the expression
	 * @param parameters
	 *            map of additional parameters. may be null or empty
	 * @throws Exception
	 */
	protected void testResult(String testExpressionString,
			int numExpectedObjects, Map parameters) throws Exception {
		testResult(testExpressionString, numExpectedObjects, parameters, true);
	}

	/**
	 * Variant of {@link #testResult(String, int)} with an additional map of
	 * properties that can be access as [data.*] in the expression and an option
	 * to omit matching of objects.
	 *
	 * @param testExpressionString
	 *            expression to be tested
	 * @param numExpectedObjects
	 *            expected number of objects matching the expression
	 * @param parameters
	 *            map of additional parameters. may be null or empty
	 * @param matchObjects
	 *            true when fetched objects shall be matched against the rule,
	 *            false if not
	 * @throws Exception
	 */
	protected void testResult(String testExpressionString,
			int numExpectedObjects, Map parameters, boolean matchObjects) throws Exception {
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

		// count the filtered results
		assertEquals("Check count of filtered results for {" + testExpressionString + "}", numExpectedObjects, ds.getCount(filter));

		// fetch filtered objects
		Collection filteredObjects = ds.getResult(filter, null);

		// check number filtered objects
		assertEquals("Check # of filtered objects for {" + testExpressionString + "}", numExpectedObjects, filteredObjects.size());

		if (matchObjects) {
			// check whether all filtered objects match the rule
			for (Iterator iter = filteredObjects.iterator(); iter.hasNext();) {
				Resolvable element = (Resolvable) iter.next();

				if (!evaluator.match(testExpression, element)) {
					fail("Found a filtered object that does not match the expression {" + testExpressionString + "}");
				}
			}
		}

		// get all not-filtered objects
		Collection allObjects = getAllObjects();
		Collection notFiltered = new Vector(allObjects);

		notFiltered.removeAll(filteredObjects);

		// check # of not-filtered objects
		assertEquals("Check # of not-filtered objects for {" + testExpressionString + "}", allObjects.size() - numExpectedObjects, notFiltered.size());

		if (matchObjects) {
			// check whether all not-filtered objects do not match the rule
			for (Iterator iter = notFiltered.iterator(); iter.hasNext();) {
				Resolvable element = (Resolvable) iter.next();

				if (evaluator.match(testExpression, element)) {
					fail("Found a not-filtered object that matches the expression {" + testExpressionString + "}");
				}
			}
		}
	}

	/**
	 * Get all objects at a version timestamp
	 *
	 * @return collection of all objects
	 * @throws Exception
	 */
	protected Collection getAllObjects() throws Exception {
		// parse the expression
		Expression testExpression = ExpressionParser.getInstance().parse(EMPTYRULE);

		// generate the filter
		DatasourceFilter filter = ds.createDatasourceFilter(testExpression);

		return ds.getResult(filter, null);
	}

}
