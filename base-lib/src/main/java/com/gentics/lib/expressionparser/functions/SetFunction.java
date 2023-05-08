/*
 * @author herbert
 * @date Sep 30, 2008
 * @version $Id: SetFunction.java,v 1.3 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.lib.log.NodeLogger;

/**
 * set( [changeable/changeable collection], [attribute name], [value] ) set the value
 * of an object or a collection of objects.
 * @author herbert
 */
public class SetFunction extends AbstractGenericFunction {
    
	NodeLogger logger = NodeLogger.getNodeLogger(SetFunction.class);
    
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		Object baseObject = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);
		String attributename = ObjectTransformer.getString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_STRING), null);
		Object value = operand[2].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);
        
		setValue(baseObject, attributename, value);
        
		return ExpressionParser.ASSIGNMENT;
	}

	/**
	 * set the value of a attribute on a given base object (or a collection/map of base objects)
	 * @param baseObject Changeable, or a Collection or Map of Changeables.
	 * @param attributename
	 * @param value
	 */
	private void setValue(Object baseObject, String attributename, Object value) {
		if (baseObject instanceof Changeable) {
			try {
				((Changeable) baseObject).setProperty(attributename, value);
			} catch (InsufficientPrivilegesException e) {
				logger.error("Error while calling setProperty of changeable {" + baseObject + "}", e);
			}
		} else {
			logger.error(
					"set function got invalid baseObject {" + baseObject + "} of type {" + (baseObject == null ? "null" : baseObject.getClass().getName()) + "}");
		}
	}
    
	public String getName() {
		return "set";
	}

	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}

	public int getMaxParameters() {
		return 3;
	}

	public int getMinParameters() {
		return 3;
	}

	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

}
