/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: GenericExtendedComparisonFunction.java,v 1.11 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.expressionparser.functions;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.resolving.NestedCollection;
import com.gentics.lib.etc.StringUtils;

/**
 * Implementation for evaluation of extended comparison
 * functions (CONTAINSONEOF, CONTAINSNONE, LIKE).
 */
public class GenericExtendedComparisonFunction extends AbstractGenericBinaryFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_CONTAINSONEOF, TYPE_CONTAINSNONE, TYPE_LIKE, TYPE_CONTAINSALL};

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
		case TYPE_LIKE: {
			Object leftOperandObject = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);
			String rightOperand = StringUtils.likeStringToRegex(
					ExpressionEvaluator.getAsString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_STRING)));

			if (leftOperandObject instanceof NestedCollection) {
				// the left operand is resolved from a property path where at
				// least one of the path entries resolved into a collection of
				// objects
				return Boolean.valueOf(isOneLike((NestedCollection) leftOperandObject, rightOperand));
			} else {
				String leftOperand = ExpressionEvaluator.getAsString(leftOperandObject);
				Pattern p = Pattern.compile(rightOperand, Pattern.CASE_INSENSITIVE);

				return Boolean.valueOf(leftOperand != null && p.matcher(leftOperand).matches());
			}
		}

		case TYPE_CONTAINSONEOF: {
			// containsoneof comparison:
			Collection leftOperand = ExpressionEvaluator.getAsCollection(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION));
			Collection rightOperand = ExpressionEvaluator.getAsCollection(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION));

			return Boolean.valueOf(ExpressionEvaluator.containsOneOf(leftOperand, rightOperand));
		}

		case TYPE_CONTAINSNONE: {
			// containsnone comparison:
			Collection leftOperand = ExpressionEvaluator.getAsCollection(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION));
			Collection rightOperand = ExpressionEvaluator.getAsCollection(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION));

			return Boolean.valueOf(ExpressionEvaluator.containsNone(leftOperand, rightOperand));
		}

		case TYPE_CONTAINSALL: {
			// containsall comparison:
			Collection leftOperand = ExpressionEvaluator.getAsCollection(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION));
			Collection rightOperand = ExpressionEvaluator.getAsCollection(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION));

			return Boolean.valueOf(ExpressionEvaluator.containsAll(leftOperand, rightOperand));
		}

		default:
			unknownTypeFound(functionType);
			return Boolean.FALSE;
		}
	}

	/**
	 * Check whether at least one entry of the collection is like the given
	 * pattern. Do this recursive if the collection contains elements of type
	 * {@link NestedCollection}.
	 * @param collection nested collection
	 * @param pattern comparison pattern
	 * @return true when at least one object (as string) is like the given
	 *         pattern, false if not
	 */
	private static boolean isOneLike(NestedCollection collection, String pattern) {
		for (Iterator iter = collection.iterator(); iter.hasNext();) {
			Object element = (Object) iter.next();

			// for NestedCollections do the recursion
			if (element instanceof NestedCollection && isOneLike((NestedCollection) element, pattern)) {
				// found match in the recursion, so we are done
				return true;
			} else {
				try {
					// check whether the string matches the pattern
					String string = ExpressionEvaluator.getAsString(element);
					Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

					if (p.matcher(string).matches()) {
						return true;
					}
				} catch (EvaluationException e) {// the exception is simply ignored
				}
			}
		}

		// no matching part found
		return false;
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
