/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: FilterPart.java,v 1.3 2007-01-16 12:05:41 clemens Exp $
 */
package com.gentics.api.lib.expressionparser.filtergenerator;

import java.io.Serializable;
import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Interface for a filterpart. A filterpart is part of a filter, might be static
 * or dynamic and might contain subparts. Most of the filterparts are generated
 * during creation of the {@link DatasourceFilter} instance. Some are generated
 * later (during execution of the {@link DatasourceFilter}), using
 * {@link com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator}s.
 */
public interface FilterPart extends Serializable {

	/**
	 * Add a filter statement part
	 * @param statementPart filter statement part to add
	 * @throws ExpressionParserException in case of errors
	 */
	void addFilterStatementPart(String statementPart) throws ExpressionParserException;

	/**
	 * Add a filter statement part together with some parameters (e.g. bind
	 * variables)
	 * @param statementPart filter statement part to add
	 * @param parameters array of parameters
	 * @throws ExpressionParserException in case of errors
	 */
	void addFilterStatementPart(String statementPart, Object[] parameters) throws ExpressionParserException;

	/**
	 * Add a variable to the filter. The expression contains the property path
	 * of the variable and will always start with "object."
	 * @param expressionName expression name of the variable
	 * @param expectedValueType expected value type
	 * @throws ExpressionParserException in case of errors
	 */
	void addVariable(String expressionName, int expectedValueType) throws ExpressionParserException;

	/**
	 * Add the given literal to the filter.
	 * @param literal literal to add to the filter
	 * @param expectedValueType expected value type
	 * @throws ExpressionParserException in case of errors
	 */
	void addLiteral(Object literal, int expectedValueType) throws ExpressionParserException;

	/**
	 * Add the given function to the filter for later evaluation. This is only
	 * valid when the function is not variable.
	 * @param function function to be added
	 * @param type type of the function
	 * @param operands array of operands
	 * @param expectedValueType expected value type of the function result
	 * @throws ExpressionParserException in case of errors
	 */
	void addFunctionToEvaluate(Function function, int type, EvaluableExpression[] operands, int expectedValueType) throws ExpressionParserException;

	/**
	 * Add a resolvable object to the filter. The expectedValueType must be one
	 * of ({@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_ANY},
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_BOOLEAN},
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_COLLECTION},
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_DATE},
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_NULL},
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_NUMBER},
	 * {@link com.gentics.api.lib.expressionparser.ExpressionEvaluator#OBJECTTYPE_STRING}).
	 * @param expressionName full property path of the resolvable object
	 * @param expectedValueType expected value type
	 * @throws ExpressionParserException in case of errors
	 */
	void addResolvableObject(String expressionName, int expectedValueType) throws ExpressionParserException;

	/**
	 * Add a filter part generator to the filter that generates a part of the
	 * statement dynamically when the filter is used to fetch results from a
	 * datasource. This shall only be used by functions that need to adjust the
	 * generated statement to the current usage (for example: the datatypes of
	 * operands that are paths to resolvables).
	 * @param filterPartGenerator filter part generator
	 * @throws ExpressionParserException in case of errors
	 * @see FilterPartGenerator
	 */
	void addFilterPartGenerator(FilterPartGenerator filterPartGenerator) throws ExpressionParserException;

	/**
	 * Add a post processor to this filter part
	 * 
	 * @param postProcessor
	 *            post processor instance
	 * @param data
	 *            evaluable expression that will be evaluated and the result
	 *            passed to the post processor in the call {@link PostProcessor#process(List, Object)}.
	 * @throws ExpressionParserException in case of errors
	 */
	void addPostProcessor(PostProcessor postProcessor, EvaluableExpression data) throws ExpressionParserException;

	/**
	 * Do post processing for this filter part. Pass the given result to the
	 * post processors. Use the request to evaluate the data expression first
	 * 
	 * @param result
	 *            filter result to be post processed
	 * @param request
	 *            request
	 * @throws ExpressionParserException in case of errors
	 */
	void doPostProcessing(List<Resolvable> result,
			ExpressionQueryRequest request) throws ExpressionParserException;

	/**
	 * Check whether the filterpart has post processors set
	 * @return true if the filterpart has post processors set, false if not
	 */
	boolean hasPostProcessors();

	/**
	 * Merge this filterpart into the merged filter (final step of filter
	 * generation). All nested filterparts are also merged into the mergedFilter
	 * and instances of {@link FilterPartGenerator} will generate and merge
	 * their filter parts.
	 * @param mergedFilter merged filter
	 * @throws ExpressionParserException in case of errors
	 */
	void mergeInto(MergedFilter mergedFilter) throws ExpressionParserException;
}
