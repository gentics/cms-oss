/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: GenericCalcFunction.java,v 1.9 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * Implementation for evaluation of binary calculation
 * functions (+, -, *, /, %).
 */
public class GenericCalcFunction extends AbstractGenericBinaryFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_ADD, TYPE_SUB, TYPE_MULT, TYPE_DIV, TYPE_MOD};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_NUMBER, expectedValueType);

		// check whether both operands are integers or at least one is a
		// double, when at least one operand is not a number, throw an error
		Number leftValue = ExpressionEvaluator.getAsNumber(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));
		Number rightValue = ExpressionEvaluator.getAsNumber(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));

		// check for null values
		if (leftValue == null || rightValue == null) {
			throw new EvaluationException("Cannot perform calculation when at least one of the operands is null");
		}

		boolean resultIsInteger = ExpressionEvaluator.isInteger(leftValue) && ExpressionEvaluator.isInteger(rightValue);

		switch (functionType) {
		case TYPE_ADD:
			return resultIsInteger
					? (Number) new Long(leftValue.longValue() + rightValue.longValue())
					: (Number) new Double(leftValue.doubleValue() + rightValue.doubleValue());

		case TYPE_SUB:
			return resultIsInteger
					? (Number) new Long(leftValue.longValue() - rightValue.longValue())
					: (Number) new Double(leftValue.doubleValue() - rightValue.doubleValue());

		case TYPE_MULT:
			return resultIsInteger
					? (Number) new Long(leftValue.longValue() * rightValue.longValue())
					: (Number) new Double(leftValue.doubleValue() * rightValue.doubleValue());

		case TYPE_DIV:
			try {
				return resultIsInteger
						? (Number) new Long(leftValue.longValue() / rightValue.longValue())
						: (Number) new Double(leftValue.doubleValue() / rightValue.doubleValue());
			} catch (Exception e) {
				throw new EvaluationException(e);
			}

		case TYPE_MOD:
			try {
				return resultIsInteger
						? (Number) new Long(leftValue.longValue() % rightValue.longValue())
						: (Number) new Double(leftValue.doubleValue() % rightValue.doubleValue());
			} catch (Exception e) {
				throw new EvaluationException(e);
			}

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
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class[] getSupportedDatasourceClasses() {
		return AbstractGenericFunction.EVALUATIONCLASS;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_NUMBER;
	}
}
