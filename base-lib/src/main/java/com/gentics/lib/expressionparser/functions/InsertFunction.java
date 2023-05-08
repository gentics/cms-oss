/*
 * @author norbert
 * @date 21.08.2007
 * @version $Id: InsertFunction.java,v 1.2 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * Implementation of the insert() function to insert an object into a collection
 * insert(array, newobject, index, unique)
 * - array: the array (collection) to modify
 * - newobject: new object
 * - index: index where to add the object (-1 for end), optional, default: -1
 * - unique: whether the object shall be removed from the array first, optional, default: false
 * the function does not modify array itself but returns a modified copy of array
 */
public class InsertFunction extends AbstractGenericFunction {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// check the expected value type
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_COLLECTION, expectedValueType);

		// first operand must be an array
		List array = new Vector(ExpressionEvaluator.getAsCollection(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION)));

		// second operand is the object to insert
		Object newObject = operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

		// do not insert null objects
		if (newObject == null) {
			return array;
		}

		int index = -1;
		boolean unique = false;

		// eventually existing third operand is the index
		if (operand.length > 2) {
			index = ((Number) (operand[2].evaluate(request, ExpressionEvaluator.OBJECTTYPE_NUMBER))).intValue();
		}

		// eventually existing fourth operand defines whether the insert uniquely
		if (operand.length > 3) {
			unique = ObjectTransformer.getBoolean(operand[3].evaluate(request, ExpressionEvaluator.OBJECTTYPE_BOOLEAN), unique);
		}

		// when inserting unique, first remove all occurrances of the newObject
		if (unique) {
			for (Iterator iterator = array.iterator(); iterator.hasNext();) {
				Object arrayObject = (Object) iterator.next();

				if (arrayObject != null) {
					if (arrayObject.equals(newObject) || newObject.equals(arrayObject)) {
						iterator.remove();
					}
				}
			}
		}

		if (index > array.size()) {
			index = array.size();
		}

		if (index < 0) {
			// TODO eventually support inserting a index th position from end 
			array.add(newObject);
		} else {
			array.add(index, newObject);
		}

		// return the resulting array
		return array;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType(int)
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_COLLECTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 4;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#getName()
	 */
	public String getName() {
		return "insert";
	}
}
