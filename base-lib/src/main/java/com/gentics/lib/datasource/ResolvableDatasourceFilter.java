/*
 * @author norbert
 * @date 11.10.2007
 * @version $Id: ResolvableDatasourceFilter.java,v 1.3 2007-12-04 14:51:32 norbert Exp $
 */
package com.gentics.lib.datasource;

import java.util.Map;

import com.gentics.api.lib.datasource.ResolvableDatasource;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.lib.expressionparser.filtergenerator.AbstractDatasourceFilter;

/**
 * @author norbert
 *
 */
public class ResolvableDatasourceFilter extends AbstractDatasourceFilter {

	/**
	 * expression
	 */
	protected Expression expression;

	/**
	 * Create the resolvable datasource filter for the expression
	 * @param expression expression
	 */
	public ResolvableDatasourceFilter(Expression expression) {
		super();
		this.expression = expression;
	}

	/**
	 * Create the resolvable datasource filter for the expression
	 * @param expression expression
	 * @param baseObjects map of base objects for resolving
	 */
	public ResolvableDatasourceFilter(Expression expression, Map baseObjects) {
		super(baseObjects);
		this.expression = expression;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#allowsNullValues(java.lang.String)
	 */
	public boolean allowsNullValues(String attributeName) throws FilterGeneratorException {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#generateLiteralFilterPart(java.lang.Object, int)
	 */
	public FilterPart generateLiteralFilterPart(Object literal, int expectedValueType) throws FilterGeneratorException {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#getDatasourceClass()
	 */
	public Class getDatasourceClass() {
		return ResolvableDatasource.class;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#getValueType(java.lang.String)
	 */
	public int getValueType(String attributeName) throws FilterGeneratorException {
		return -1;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter#getVariableName(java.lang.String, int)
	 */
	public String getVariableName(String expressionName, int expectedValueType) throws FilterGeneratorException {
		return null;
	}

	/**
	 * Get the expression
	 * @return expression
	 */
	public Expression getExpression() {
		return expression;
	}

	/**
	 * Get an expression evaluator suitable to search by this filter
	 * @return expression evaluator
	 */
	public ExpressionEvaluator getExpressionEvaluator() {
		ExpressionEvaluator evaluator = new ExpressionEvaluator();

		evaluator.setResolver(getResolver());
		return evaluator;
	}
}
