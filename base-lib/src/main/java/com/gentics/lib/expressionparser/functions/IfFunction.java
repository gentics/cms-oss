/*
 * @author norbert
 * @date 28.07.2006
 * @version $Id: IfFunction.java,v 1.6 2008-01-09 09:11:20 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * Implementation of an if(): when first operand is true, evaluate second
 * operand, if false evaluate third operand or null if not third operand exists.
 */
public class IfFunction extends AbstractGenericFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// check whether the first operand evaluates to true
		boolean expressionResult = ObjectTransformer.getBoolean(ExpressionEvaluator.getAsBoolean(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_BOOLEAN)), false);

		if (expressionResult) {
			// for true return the second operand value
			return operand[1].evaluate(request, expectedValueType);
		} else if (operand.length >= 3) {
			// for false the third operand value
			return operand[2].evaluate(request, expectedValueType);
		} else {
			// null, since the first expression was false and no third operand exists
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 3;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "if";
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}
}
