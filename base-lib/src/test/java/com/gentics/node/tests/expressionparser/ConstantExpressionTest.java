/**
 *
 */
package com.gentics.node.tests.expressionparser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;

import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import org.junit.experimental.categories.Category;

/**
 * Test case for constant expressions
 *
 * @author norbert
 */
@Category(BaseLibTest.class)
public class ConstantExpressionTest {

	/**
	 * Test the constant TRUE expression
	 *
	 * @throws Exception
	 */
	@Test
	public void testTrue() throws Exception {
		// check for existance
		assertNotNull("Check whether the constant expression TRUE is set", ExpressionParser.TRUE);

		// check for validity
		assertTrue("Check whether the constant expression TRUE is really 'true'", new ExpressionEvaluator().match(ExpressionParser.TRUE));
	}

	/**
	 * Test the constant FALSE expression
	 *
	 * @throws Exception
	 */
	@Test
	public void testFalse() throws Exception {
		// check for existance
		assertNotNull("Check whether the constant expression FALSE is set", ExpressionParser.FALSE);

		// check for validity
		assertFalse("Check whether the constant expression FALSE is really 'false'", new ExpressionEvaluator().match(ExpressionParser.FALSE));
	}
}
