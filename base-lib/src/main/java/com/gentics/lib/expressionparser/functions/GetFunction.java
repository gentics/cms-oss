/*
 * @author norbert
 * @date 02.10.2008
 * @version $Id: GetFunction.java,v 1.3.4.1 2011-04-07 09:57:53 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.log.NodeLogger;

/**
 * get( [changeable/changeable collection], [attribute name] ) get the value of
 * an object or a collection of objects.
 * @author norbert
 */
public class GetFunction extends AbstractGenericFunction {
	NodeLogger logger = NodeLogger.getNodeLogger(GetFunction.class);

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		Object baseObject = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);
		String attributename = ObjectTransformer.getString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_STRING), null);

		return getValue(baseObject, attributename);
	}

	/**
	 * get the value of a attribute on a given base object (or a collection/map
	 * of base objects)
	 * @param baseObject Changeable, or a Collection or Map of Changeables.
	 * @param attributename
	 */
	private Object getValue(Object baseObject, String attributename) {
		if (baseObject instanceof Resolvable) {
			return ((Resolvable) baseObject).get(attributename);
		} else {
			logger.error(
					"get function got invalid baseObject {" + baseObject + "} of type {" + (baseObject == null ? "null" : baseObject.getClass().getName()) + "}");
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType(int)
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#getName()
	 */
	public String getName() {
		return "get";
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}
}
