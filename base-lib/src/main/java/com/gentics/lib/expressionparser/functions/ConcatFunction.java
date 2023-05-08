/*
 * @author norbert
 * @date 28.06.2006
 * @version $Id: ConcatFunction.java,v 1.9 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * Implementation for evaluation of the named "concat" function (concatenation of strings)
 */
public class ConcatFunction extends AbstractGenericFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		StringBuffer output = new StringBuffer();

		for (int i = 0; i < operand.length; ++i) {
			output.append(operand[i].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));
		}
		return output.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return Function.UNBOUNDED;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "concat";
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getType()
	 */
	public int[] getTypes() {
		return NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#supportStaticEvaluation()
	 */
	public boolean supportStaticEvaluation() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_STRING;
	}
}
