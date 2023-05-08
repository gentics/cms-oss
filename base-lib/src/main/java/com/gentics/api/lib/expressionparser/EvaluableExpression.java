/*
 * @author norbert
 * @date 27.06.2006
 * @version $Id: EvaluableExpression.java,v 1.4 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.api.lib.expressionparser;

import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;

/**
 * Interface for expression that can be evaluated.
 */
public interface EvaluableExpression extends Expression {

	/**
	 * Generate the filter part for the evaluable expression. The
	 * expectedValueType must be one of ({@link ExpressionEvaluator#OBJECTTYPE_ANY},
	 * {@link ExpressionEvaluator#OBJECTTYPE_BOOLEAN},
	 * {@link ExpressionEvaluator#OBJECTTYPE_COLLECTION},
	 * {@link ExpressionEvaluator#OBJECTTYPE_DATE},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NULL},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NUMBER},
	 * {@link ExpressionEvaluator#OBJECTTYPE_STRING}), where
	 * {@link ExpressionEvaluator#OBJECTTYPE_ANY} shall be used when any value
	 * type is acceptable.
	 * @param request expression request
	 * @param filterPart part of the filter that is generated
	 * @param expectedValueType expected value type
	 * @throws ExpressionParserException when the generation of the filter part
	 *         fails
	 */
	void generateFilterPart(ExpressionQueryRequest request, FilterPart filterPart,
			int expectedValueType) throws ExpressionParserException;

	/**
	 * Evaluate the expression and return the value. The expectedValueType must
	 * be on of ({@link ExpressionEvaluator#OBJECTTYPE_ANY},
	 * {@link ExpressionEvaluator#OBJECTTYPE_BOOLEAN},
	 * {@link ExpressionEvaluator#OBJECTTYPE_COLLECTION},
	 * {@link ExpressionEvaluator#OBJECTTYPE_DATE},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NULL},
	 * {@link ExpressionEvaluator#OBJECTTYPE_NUMBER},
	 * {@link ExpressionEvaluator#OBJECTTYPE_STRING}), where
	 * {@link ExpressionEvaluator#OBJECTTYPE_ANY} shall be used when any value
	 * type is acceptable. When a datasource is given, the evaluation occurs
	 * while generating a filter part.
	 * @param request expression request
	 * @param expectedValueType expected value type
	 * @return the value of the expression
	 * @throws ExpressionParserException when the evaluation fails
	 */
	Object evaluate(ExpressionQueryRequest request, int expectedValueType) throws ExpressionParserException;

	/**
	 * Check whether the evaluable expression (and all its sub parts) is static
	 * or not. Static expression use only literals, no names (neither with
	 * "object." nor with other prefixes)
	 * @param filter datasource filter
	 * @return true when the expression is static, false if not
	 * @throws ExpressionParserException when evaluation fails
	 */
	boolean isStatic(DatasourceFilter filter) throws ExpressionParserException;

	/**
	 * Check whether the evaluable expression (with its sub parts) contains a
	 * variable part. variable parts are names that start with "object."
	 * @param filter datasource filter
	 * @return true when the expression contains a variable part, false if not
	 * @throws ExpressionParserException when evaluation fails
	 */
	boolean isVariable(DatasourceFilter filter) throws ExpressionParserException;
    
	/**
	 * Checks wheter this expression allows null values.
	 * @param filter datasource filter
	 * @return true if expression might contain null values.
	 * @throws ExpressionParserException when evaluation fails
	 */
	public boolean allowsNullValues(DatasourceFilter filter) throws ExpressionParserException;

	/**
	 * Get the value type which this expression is expected to return
	 * @param filter datasource filter
	 * @return expected value type ({@link ExpressionEvaluator#OBJECTTYPE_ANY}
	 *         if no specific value type can be expected)
	 * @throws ExpressionParserException when evaluation fails
	 */
	int getExpectedValueType(DatasourceFilter filter) throws ExpressionParserException;
}
