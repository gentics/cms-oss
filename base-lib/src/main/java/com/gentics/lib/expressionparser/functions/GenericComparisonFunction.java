/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: GenericComparisonFunction.java,v 1.11 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * Implementation for evaluation of comparison functions
 * (==, !=, &lt;, &lt;=, &gt;, &gt;=).
 */
public class GenericComparisonFunction extends AbstractGenericBinaryFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_EQUAL, TYPE_SMALLER, TYPE_SMALLEROREQUAL, TYPE_GREATER, TYPE_GREATEROREQUAL, TYPE_UNEQUAL};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);

		// TODO switch this
		if (functionType == TYPE_EQUAL) {
			return Boolean.valueOf(
					ExpressionEvaluator.isTypeSafeEqual(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY),
					operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY)));
		} else if (functionType == TYPE_UNEQUAL) {
			return Boolean.valueOf(
					ExpressionEvaluator.isTypeSafeUnequal(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY),
					operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY)));
		} else {
			return Boolean.valueOf(
					ExpressionEvaluator.typeSafeComparison(request, operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY),
					operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY), functionType));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return TYPES;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
