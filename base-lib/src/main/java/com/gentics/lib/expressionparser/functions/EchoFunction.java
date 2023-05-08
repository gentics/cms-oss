/*
 * @author herbert
 * @date 19.11.2007
 * @version $Id: EchoFunction.java,v 1.3 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementation of an "echo()" function (outputs it's content into the debugging logger)
 * Very simple and probably totally useless.
 */
public class EchoFunction extends AbstractGenericUnaryFunction {
	private static NodeLogger logger = NodeLogger.getNodeLogger(EchoFunction.class);
    
	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_ANY, expectedValueType);
		Object operandValue = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

		logger.forceInfo(
				"echo function: value: {" + (operandValue == null ? "null" : operandValue.toString()) + "} of type: {"
				+ (operandValue == null ? "null" : operandValue.getClass().getName()) + "}");

		return operandValue;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "echo";
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
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}
}
