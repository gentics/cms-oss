package com.gentics.node.tests.expressionparser;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.lib.etc.StringUtils;

@RunWith(value = Parameterized.class)
@Category(BaseLibTest.class)
public class ExpressionTest {

	/**
	 * name of the test file for boolean tests that should evaluate to "true"
	 */
	public final static String booleanTrueTests = "boolean.true.txt";

	/**
	 * name of the test file for boolean tests that should evaluate to "false"
	 */
	public final static String booleanFalseTests = "boolean.false.txt";

	/**
	 * Tested expression
	 */
	protected String expression;

	/**
	 * Expected result
	 */
	protected boolean expected;

	/**
	 * Get the test parameters
	 *
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: expression {0}, match {1}")
	public static Collection<Object[]> data() throws Exception {
		Collection<Object[]> data = new ArrayList<Object[]>();

		// add the boolean expression tests
		addBooleanTests(data, booleanTrueTests, true);
		addBooleanTests(data, booleanFalseTests, false);
		return data;
	}

	/**
	 * Create a test instance
	 * @param expression tested expression
	 * @param expected expected result
	 */
	public ExpressionTest(String expression, boolean expected) {
		this.expression = expression;
		this.expected = expected;
	}

	/**
	 * Test matching the expression
	 * @throws Exception
	 */
	@Test
	public void testMatch() throws Exception {
		Expression exp = ExpressionParser.getInstance().parse(expression);
		ExpressionEvaluator evaluator = new ExpressionEvaluator();

		assertEquals("Check for the expected boolean result.", expected, evaluator.match(exp));
	}

	/**
	 * Add the boolean tests from the given testfile to the data collection
	 * @param data data collection (will be modified)
	 * @param testFile testfile
	 * @param expected expected result
	 * @throws Exception
	 */
	protected static void addBooleanTests(Collection<Object[]> data, String testFile, boolean expected) throws Exception {
		// read the file
		BufferedReader reader = new BufferedReader(new InputStreamReader(ExpressionTest.class.getResourceAsStream(testFile), "UTF-8"));

		String expression = null;

		while ((expression = reader.readLine()) != null) {
			// first trim the expression
			expression = expression.trim();

			// omit empty lines and lines beginning with // or # (comments)
			if (StringUtils.isEmpty(expression) || expression.startsWith("//") || expression.startsWith("#")) {
				continue;
			}

			data.add(new Object[] { expression, expected });
		}
	}

}
