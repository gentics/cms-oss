/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: GenericUnaryFunction.java,v 1.9 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * Implementation for evaluation of unary functions (+, -, !)
 */
public class GenericUnaryFunction extends AbstractGenericUnaryFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_MINUS, TYPE_PLUS, TYPE_NOT};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		switch (functionType) {
		case TYPE_PLUS:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_NUMBER, expectedValueType);
			return ExpressionEvaluator.getAsNumber(operand[0].evaluate(request, expectedValueType));

		case TYPE_MINUS:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_NUMBER, expectedValueType);
			Number number = ExpressionEvaluator.getAsNumber(operand[0].evaluate(request, expectedValueType));

			return ExpressionEvaluator.isInteger(number) ? (Number) new Long(-number.longValue()) : (Number) new Double(-number.doubleValue());

		case TYPE_NOT:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);
			boolean bool = ObjectTransformer.getBoolean(ExpressionEvaluator.getAsBoolean(operand[0].evaluate(request, expectedValueType)), false);

			return Boolean.valueOf(!bool);

		default:
			unknownTypeFound(functionType);
			return null;
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
		switch (functionType) {
		case TYPE_PLUS:
		case TYPE_MINUS:
			return ExpressionEvaluator.OBJECTTYPE_NUMBER;

		case TYPE_NOT:
			return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;

		default:
			return ExpressionEvaluator.OBJECTTYPE_ANY;
		}
	}
}
