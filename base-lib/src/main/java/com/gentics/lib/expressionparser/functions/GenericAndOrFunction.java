/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: GenericAndOrFunction.java,v 1.8 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * Implementation for evaluation of "and" and "or".
 */
public class GenericAndOrFunction extends AbstractGenericBinaryFunction {

	/**
	 * Constant for the function types
	 */
	public final static int[] TYPES = new int[] { TYPE_AND, TYPE_OR};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);
		switch (functionType) {
		case TYPE_AND:
			return Boolean.valueOf(
					ObjectTransformer.getBoolean(ExpressionEvaluator.getAsBoolean(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY)), false)
							&& ObjectTransformer.getBoolean(ExpressionEvaluator.getAsBoolean(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY)), false));

		case TYPE_OR:
			return Boolean.valueOf(
					ObjectTransformer.getBoolean(ExpressionEvaluator.getAsBoolean(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY)), false)
							|| ObjectTransformer.getBoolean(ExpressionEvaluator.getAsBoolean(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY)), false));

		default:
			// this will rarely happen, but do it anyway
			throw new EvaluationException("Unknown function type {" + functionType + "}");
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
