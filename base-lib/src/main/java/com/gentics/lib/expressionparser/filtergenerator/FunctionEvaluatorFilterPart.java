/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: FunctionEvaluatorFilterPart.java,v 1.7 2007-01-16 12:05:41 clemens Exp $
 */
package com.gentics.lib.expressionparser.filtergenerator;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.lib.expressionparser.functions.AbstractGenericFunction;

/**
 * Implementation of a {@link FilterPart} that evaluates a function into a
 * literal and adds this literal into the mergedFilter.
 */
public class FunctionEvaluatorFilterPart extends GenericFilterPart {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -1931214806369871688L;

	/**
	 * function to evaluate
	 */
	protected Function function;

	/**
	 * type of the function
	 */
	protected int type;

	/**
	 * function operands
	 */
	protected EvaluableExpression[] operands;

	/**
	 * expected value type of the function result
	 */
	protected int expectedValueType;

	/**
	 * Create an instance of thie filter part
	 * @param filter filter
	 * @param function function to evaluate
	 * @param type type of the function
	 * @param operands function operands
	 * @param expectedValueType expected value type
	 */
	public FunctionEvaluatorFilterPart(DatasourceFilter filter, Function function, int type, EvaluableExpression[] operands, int expectedValueType) {
		super(filter);
		this.function = function;
		this.type = type;
		this.operands = operands;
		this.expectedValueType = expectedValueType;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#mergeInto(com.gentics.lib.expressionparser.filtergenerator.MergedFilter)
	 */
	public void mergeInto(MergedFilter mergedFilter) throws ExpressionParserException {
		FilterPart literalFilterPart = filter.generateLiteralFilterPart(function.evaluate(type, mergedFilter.getRequest(), operands, expectedValueType),
				expectedValueType);

		literalFilterPart.mergeInto(mergedFilter);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer string = new StringBuffer();

		string.append("functionpart {").append(function.getClass().getName()).append("}, type {").append(AbstractGenericFunction.getFunctionTypeName(type)).append(
				"}");
		if (type == Function.TYPE_NAMEDFUNCTION) {
			string.append(", name {").append(function.getName()).append("}");
		}
		return string.toString();
	}
}
