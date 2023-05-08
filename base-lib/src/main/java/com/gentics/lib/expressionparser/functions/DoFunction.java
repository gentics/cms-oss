/*
 * @author norbert
 * @date 24.08.2006
 * @version $Id: DoFunction.java,v 1.3 2008-09-30 16:00:11 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * The do() function can be used to perform a series of assignments in the given order.
 */
public class DoFunction extends AbstractGenericFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// check the expected value type
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_ASSIGNMENT, expectedValueType);

		// simple evaluate all operands
		for (int i = 0; i < operand.length; i++) {
			operand[i].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);
		}

		// return the assignment result
		return ExpressionParser.ASSIGNMENT;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "do";
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return UNBOUNDED;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ASSIGNMENT;
	}
}
