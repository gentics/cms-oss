/*
 * @author norbert
 * @date 25.07.2006
 * @version $Id: IsEmptyFunction.java,v 1.10 2009-12-16 16:12:06 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * Implementation of an "isEmpty()" function (tests for null or empty values)
 */
public class IsEmptyFunction extends AbstractGenericUnaryFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);
		Object operandValue = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

		return Boolean.valueOf(ObjectTransformer.isEmpty(operandValue));
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "isempty";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
