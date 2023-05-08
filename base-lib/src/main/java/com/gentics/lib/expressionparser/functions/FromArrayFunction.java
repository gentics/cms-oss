/*
 * @author norbert
 * @date 08.09.2006
 * @version $Id: FromArrayFunction.java,v 1.4 2010-09-28 17:01:32 norbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * The fromArray function can be used to fetch a specific object from a collection of objects
 */
public class FromArrayFunction extends AbstractGenericFunction {

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		Collection values = ExpressionEvaluator.getAsCollection(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));
		int index = ExpressionEvaluator.getAsNumber(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY), Integer.class).intValue();

		// when the collection is null, the object is null
		if (values == null) {
			return null;
		}

		// check for bounds
		if (index >= values.size()) {
			throw new EvaluationException("Index {" + index + "} out of bounds {" + values.size() + "}");
		}

		if (values instanceof List) {
			// just get the object from the list
			return ((List) values).get(index);
		} else {
			// iterate through the collection and get the correct value
			int counter = 0;

			for (Iterator iter = values.iterator(); iter.hasNext() && counter <= index; counter++) {
				Object element = (Object) iter.next();

				if (counter == index) {
					return element;
				}
			}
			// this will rarely happen (index out of bounds was checked before)
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "fromArray";
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 2;
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
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}
}
