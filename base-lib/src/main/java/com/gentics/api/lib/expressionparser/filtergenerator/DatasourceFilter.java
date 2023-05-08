/*
 * @author norbert
 * @date 28.06.2006
 * @version $Id: DatasourceFilter.java,v 1.5 2007-08-17 10:37:13 norbert Exp $
 */
package com.gentics.api.lib.expressionparser.filtergenerator;

import java.io.Serializable;
import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Interface for FilterGenerators. Implementations transform expressions into
 * Datasource-specific filters.
 */
public interface DatasourceFilter extends Serializable, Cloneable {

	/**
	 * Convert the name of a variable in the expression into the datasource
	 * specific name
	 * @param expressionName name in the expression (something like
	 *        'object.path_to_property')
	 * @param expectedValueType expected value type
	 * @return datasource specific name of the variable to be used in the filter
	 *         statement
	 * @throws FilterGeneratorException if the filter cannot be generated
	 */
	String getVariableName(String expressionName, int expectedValueType) throws FilterGeneratorException;

	/**
	 * Get the datasource class of this datasource filter
	 * @return datasource class
	 */
	Class getDatasourceClass();

	/**
	 * Get the resolver to resolve object paths
	 * @return resolver to resolve object paths
	 */
	PropertyResolver getResolver();

	/**
	 * Set a custom resolver into the filter. When a custom resolver is set, any
	 * subsequent call to {@link #addBaseResolvable(String, Resolvable)} will
	 * throw an exception and previously set base resolvables will not be used.
	 * When the custom resolver is unset (set to null), the previously set base
	 * resolvables will be used.
	 * @param resolver resolver to set or null to unset the custom resolver
	 */
	void setCustomResolver(PropertyResolver resolver);

	/**
	 * Add a base resolvable to the resolver with the given baseName
	 * @param baseName base name of the resolvable (must not be "object")
	 * @param resolvable resolvable to add
	 * @throws FilterGeneratorException when the resolvable cannot be added
	 */
	void addBaseResolvable(String baseName, Resolvable resolvable) throws FilterGeneratorException;

	/**
	 * Generate a filterpart for a constant string with optional parameters
	 * @param constantString constant statement part
	 * @param params optional parameters, may be empty or null
	 * @return filterpart for a constant statement part
	 * @throws FilterGeneratorException if the filter cannot be generated
	 */
	FilterPart generateConstantFilterPart(String constantString, Object[] params) throws FilterGeneratorException;

	/**
	 * Generate a filterpart for a variable.
	 * @param expressionName name of the expression, should start with "object."
	 * @param expectedValueType expected value type
	 * @return filterpart for the variable
	 * @throws FilterGeneratorException if the filter cannot be generated
	 */
	FilterPart generateVariableFilterPart(String expressionName, int expectedValueType) throws FilterGeneratorException;

	/**
	 * Generate a filterpart for a literal.
	 * @param literal literal value
	 * @param expectedValueType expected value type.
	 * @return filterpart for a literal
	 * @throws FilterGeneratorException if the filter cannot be generated
	 */
	FilterPart generateLiteralFilterPart(Object literal, int expectedValueType) throws FilterGeneratorException;

	/**
	 * Get the main filterpart of this filter. This filter part is the entry
	 * point for all generated filterparts.
	 * @return main filterpart
	 */
	FilterPart getMainFilterPart();

	/**
	 * Get the expression string of this filter
	 * @return expression string of this filter
	 */
	String getExpressionString();

	/**
	 * Determines if the given attribute may contain null values.
	 * @param attributeName name of the attribute
	 * @return true if the attribute can contain null values.
	 * @throws FilterGeneratorException if the filter cannot be generated
	 */
	boolean allowsNullValues(String attributeName) throws FilterGeneratorException;

	/**
	 * Get the value type of the given attribute
	 * @param attributeName attribute name
	 * @return value type
	 * @throws FilterGeneratorException if the filter cannot be generated
	 */
	int getValueType(String attributeName) throws FilterGeneratorException;

	/**
	 * Do post processing of the result.
	 * @param result current result of the filter
	 * @param request request
	 * @throws ExpressionParserException if the filter cannot be generated
	 */
	void doPostProcessing(List<Resolvable> result,
			ExpressionQueryRequest request) throws ExpressionParserException;

	/**
	 * Check whether the filter contains PostProcessors
	 * @return true if the filter contains PostProcessors, false if not
	 */
	boolean hasPostProcessors();

	/**
	 * Add a post processor to this filter
	 * 
	 * @param postProcessor
	 *            post processor instance
	 * @param data
	 *            evaluable expression that will be evaluated and the result
	 *            passed to the post processor in the call {@link PostProcessor#process(List, Object)}.
	 * @throws ExpressionParserException if the filter cannot be generated
	 */
	void addPostProcessor(PostProcessor postProcessor, EvaluableExpression data) throws ExpressionParserException;
}
