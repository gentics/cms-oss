/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: LiteralNode.java,v 1.7 2007-04-13 09:42:30 norbert Exp $
 */
package com.gentics.lib.expressionparser.parser;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.resolving.PropertyResolver;

/**
 * Abstract base class for literal nodes.
 */
public abstract class LiteralNode extends SimpleNode implements EvaluableExpression {

	/**
	 * Create an instance
	 * @param i
	 */
	public LiteralNode(int i) {
		super(i);
	}

	/**
	 * Create an instance
	 * @param p
	 * @param i
	 */
	public LiteralNode(Parser p, int i) {
		super(p, i);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.EvaluableExpression#isStatic()
	 */
	public boolean isStatic(DatasourceFilter filter) throws ExpressionParserException {
		// literals are static
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.EvaluableExpression#isVariable()
	 */
	public boolean isVariable(DatasourceFilter filter) throws ExpressionParserException {
		// literals are never variable
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#generateFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, int)
	 */
	public void generateFilterPart(ExpressionQueryRequest request, FilterPart filterPart,
			int expectedValueType) throws ExpressionParserException {
		// try to convert the literal value into the expected value type
		filterPart.addLiteral(ExpressionEvaluator.getAsType(getLiteralValue(), expectedValueType), expectedValueType);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#evaluate(com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      int)
	 */
	public Object evaluate(ExpressionQueryRequest request, int expectedValueType) throws ExpressionParserException {
		// try to convert the literal value into the expected value type
		return ExpressionEvaluator.getAsType(getLiteralValue(), expectedValueType);
	}

	/**
	 * Abstract method to get the literal value
	 * @return the literal value
	 */
	protected abstract Object getLiteralValue();

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.Expression#getExpressionString()
	 */
	public String getExpressionString() {
		if (parent instanceof Expression) {
			return ((Expression) parent).getExpressionString();
		} else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#allowsNullValues(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public boolean allowsNullValues(DatasourceFilter filter) throws ExpressionParserException {
		return false;
	}
}
