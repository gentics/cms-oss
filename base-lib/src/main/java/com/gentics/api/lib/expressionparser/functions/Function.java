/*
 * @author norbert
 * @date 27.06.2006
 * @version $Id: Function.java,v 1.4 2009-12-16 16:12:08 herbert Exp $
 */
package com.gentics.api.lib.expressionparser.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;

/**
 * Interface for functions. All parts of a parsed expression are implemented as
 * "functions" ("and", "or", "==", ...), not only the named expression
 * functions. A "named" function is something like "concat(...), isempty(...),
 * ..." in the expression.<br> Any implementation of this interface must be
 * <b>stateless</b> and <b>threadsafe</b>.
 */
public interface Function {

	/**
	 * type constant for a namedfunction.
	 */
	final static int[] NAMEDFUNCTION = new int[] { Function.TYPE_NAMEDFUNCTION};

	/**
	 * constant for unbounded number of operands
	 */
	final static int UNBOUNDED = -1;

	/**
	 * constant for "named functions"
	 */
	final static int TYPE_NAMEDFUNCTION = 0;

	/**
	 * constant for boolean "and" operation
	 */
	final static int TYPE_AND = 1;

	/**
	 * constant for boolean "and" operation
	 */
	final static int TYPE_OR = 2;

	/**
	 * constant for "equals" comparisons
	 */
	final static int TYPE_EQUAL = 3;

	/**
	 * constant for "smaller" comparisons
	 */
	final static int TYPE_SMALLER = 4;

	/**
	 * constant for "smaller or equal" comparisons
	 */
	final static int TYPE_SMALLEROREQUAL = 5;

	/**
	 * constant for "greater or equal" comparisons
	 */
	final static int TYPE_GREATEROREQUAL = 6;

	/**
	 * constant for "greater" comparisons
	 */
	final static int TYPE_GREATER = 7;

	/**
	 * constant for "unequal" comparisons
	 */
	final static int TYPE_UNEQUAL = 8;

	/**
	 * constant for "containsoneof" comparisons
	 */
	final static int TYPE_CONTAINSONEOF = 9;

	/**
	 * constant for "containsnone" comparisons
	 */
	final static int TYPE_CONTAINSNONE = 10;

	/**
	 * constant for "like" comparisons
	 */
	final static int TYPE_LIKE = 11;

	/**
	 * constant for binary "+" operations
	 */
	final static int TYPE_ADD = 12;

	/**
	 * constant for binary "-" operations
	 */
	final static int TYPE_SUB = 13;

	/**
	 * constant for binary "*" operations
	 */
	final static int TYPE_MULT = 14;

	/**
	 * constant for binary "/" operations
	 */
	final static int TYPE_DIV = 15;

	/**
	 * constant for binary "%" operations
	 */
	final static int TYPE_MOD = 16;

	/**
	 * constant for unary "-" operations
	 */
	final static int TYPE_MINUS = 17;

	/**
	 * constant for unary "+" operations
	 */
	final static int TYPE_PLUS = 18;

	/**
	 * constant for unary "!" operations
	 */
	final static int TYPE_NOT = 19;

	/**
	 * constant for "containsall" comparisons
	 */
	final static int TYPE_CONTAINSALL = 20;

	// when adding new function types, take care of the following:
	// 1. the function type values must a sequence, starting with 0 and without
	// holes.
	// 2. TYPE_UNKNOWN must be the last value
	// 3. the array FUNCTIONTYPE_NAMES must contain the human readable form for
	// all function types (in the same order).

	/**
	 * constant for unknown function type (this must always be the highest
	 * index)
	 */
	final static int TYPE_UNKNOWN = 21;

	/**
	 * constant of the function type names
	 */
	final static String[] FUNCTIONTYPE_NAMES = new String[] {
		"named function", "and", "or", "==", "<", "<=", ">=", ">", "!=", "CONTAINSONEOF", "CONTAINSNONE",
		"LIKE", "+ (binary)", "- (binary)", "*", "/", "%", "- (unary)", "+ (unary)", "! (unary)", "CONTAINSALL", "unknown"};

	/**
	 * Static evaluation of the function.<br> The functionType is one of ({@link #TYPE_ADD},
	 * {@link #TYPE_AND}, {@link #TYPE_CONTAINSNONE},
	 * {@link #TYPE_CONTAINSNONE}, {@link #TYPE_DIV}, {@link #TYPE_EQUAL},
	 * {@link #TYPE_GREATER}, {@link #TYPE_GREATEROREQUAL}, {@link #TYPE_LIKE},
	 * {@link #TYPE_MINUS}, {@link #TYPE_MOD}, {@link #TYPE_MULT},
	 * {@link #TYPE_NAMEDFUNCTION}, {@link #TYPE_NOT}, {@link #TYPE_OR},
	 * {@link #TYPE_PLUS}, {@link #TYPE_SMALLER}, {@link #TYPE_SMALLEROREQUAL},
	 * {@link #TYPE_SUB}, {@link #TYPE_UNEQUAL}) and will always be one of the
	 * types returned by {@link #getTypes()}.<br> The expectedValueType is
	 * one of ({@link ExpressionEvaluator#OBJECTTYPE_ANY},
	 * {@link ExpressionEvaluator#OBJECTTYPE_BOOLEAN},
	 * {@link ExpressionEvaluator#OBJECTTYPE_COLLECTION},
	 * {@link ExpressionEvaluator#OBJECTTYPE_DATE},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NULL},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NUMBER},
	 * {@link ExpressionEvaluator#OBJECTTYPE_STRING}), where
	 * {@link ExpressionEvaluator#OBJECTTYPE_ANY} shall be used when any value
	 * type is acceptable.<br> Implementations must respect the expected value
	 * type and must throw a
	 * {@link com.gentics.api.lib.expressionparser.EvaluationException} when it
	 * cannot generate a value of this type.
	 * @param functionType type of the function
	 * @param request expression request
	 * @param operand array of function operands
	 * @param expectedValueType expected value type for the result.
	 * @return value of the evaluated function
	 * @throws ExpressionParserException when evaluation of the function fails
	 * @see ExpressionEvaluator
	 */
	Object evaluate(int functionType, ExpressionQueryRequest request, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException;

	/**
	 * Generate the filter part for the function.<br> The functionType is one
	 * of ({@link #TYPE_ADD}, {@link #TYPE_AND}, {@link #TYPE_CONTAINSNONE},
	 * {@link #TYPE_CONTAINSNONE}, {@link #TYPE_DIV}, {@link #TYPE_EQUAL},
	 * {@link #TYPE_GREATER}, {@link #TYPE_GREATEROREQUAL}, {@link #TYPE_LIKE},
	 * {@link #TYPE_MINUS}, {@link #TYPE_MOD}, {@link #TYPE_MULT},
	 * {@link #TYPE_NAMEDFUNCTION}, {@link #TYPE_NOT}, {@link #TYPE_OR},
	 * {@link #TYPE_PLUS}, {@link #TYPE_SMALLER}, {@link #TYPE_SMALLEROREQUAL},
	 * {@link #TYPE_SUB}, {@link #TYPE_UNEQUAL}) and will always be one of the
	 * types returned by {@link #getTypes()}.<br> The expectedValueType is
	 * one of ({@link ExpressionEvaluator#OBJECTTYPE_ANY},
	 * {@link ExpressionEvaluator#OBJECTTYPE_BOOLEAN},
	 * {@link ExpressionEvaluator#OBJECTTYPE_COLLECTION},
	 * {@link ExpressionEvaluator#OBJECTTYPE_DATE},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NULL},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NUMBER},
	 * {@link ExpressionEvaluator#OBJECTTYPE_STRING}), where
	 * {@link ExpressionEvaluator#OBJECTTYPE_ANY} shall be used when any value
	 * type is acceptable.<br>Implementations must respect the expected value
	 * type and must throw a
	 * {@link com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException}
	 * when it cannot generate a value of this type.
	 * @param functionType type of the function
	 * @param request expression request
	 * @param filterPart filterpart that is generated
	 * @param operand array of function operands
	 * @param expectedValueType expected value type for the result
	 * @throws ExpressionParserException when generation of the filter part
	 *         fails
	 * @see ExpressionEvaluator
	 */
	void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException;

	/**
	 * Get the name of the function. This is ignored if the type of the function
	 * is not {@link #TYPE_NAMEDFUNCTION}.
	 * @return name of the function
	 * @see #getTypes()
	 * @see #TYPE_NAMEDFUNCTION
	 */
	String getName();

	/**
	 * Get the types of the function. The array may only contain values of ({@link #TYPE_ADD},
	 * {@link #TYPE_AND}, {@link #TYPE_CONTAINSNONE},
	 * {@link #TYPE_CONTAINSNONE}, {@link #TYPE_DIV}, {@link #TYPE_EQUAL},
	 * {@link #TYPE_GREATER}, {@link #TYPE_GREATEROREQUAL}, {@link #TYPE_LIKE},
	 * {@link #TYPE_MINUS}, {@link #TYPE_MOD}, {@link #TYPE_MULT},
	 * {@link #TYPE_NAMEDFUNCTION}, {@link #TYPE_NOT}, {@link #TYPE_OR},
	 * {@link #TYPE_PLUS}, {@link #TYPE_SMALLER}, {@link #TYPE_SMALLEROREQUAL},
	 * {@link #TYPE_SUB}, {@link #TYPE_UNEQUAL}). The function is registered
	 * to implement the returned function types and will be called for only
	 * those types.
	 * @return types of the function
	 */
	int[] getTypes();

	/**
	 * Get minimum number of parameters.
	 * @return minimum number of parameters
	 */
	int getMinParameters();

	/**
	 * Get maximum number of parameters. Must be either {@link #UNBOUNDED} (for
	 * unlimited number of parameters) or &lt;= {@link #getMinParameters()}.
	 * @return maximum number of parameters
	 * @see #getMinParameters()
	 */
	int getMaxParameters();

	/**
	 * Get the array of supported datasource classes for this function. This may
	 * also include the interface {@link ExpressionEvaluator} for functions that
	 * implement the evaluation of expression parts.
	 * @return array of classes for which this function can generate a part of
	 *         the datasource filter
	 * @see ExpressionEvaluator
	 */
	Class[] getSupportedDatasourceClasses();

	/**
	 * Check whether the function supports static evaluation (calls to
	 * {@link #evaluate(int, ExpressionQueryRequest, EvaluableExpression[], int)}
	 * without a given datasource).
	 * @return true when the function supports static evaluation, false if not
	 */
	boolean supportStaticEvaluation();

	/**
	 * Get the value type which this expression is expected to return
	 * @param functionType function type
	 * @return expected value type ({@link ExpressionEvaluator#OBJECTTYPE_ANY}
	 *         if no specific value type can be expected)
	 * @throws ExpressionParserException in case of errors
	 */
	int getExpectedValueType(int functionType) throws ExpressionParserException;
}
