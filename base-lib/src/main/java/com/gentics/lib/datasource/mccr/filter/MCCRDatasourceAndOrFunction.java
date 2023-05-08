package com.gentics.lib.datasource.mccr.filter;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;

/**
 * Implementation for evaluation and filter generation for "and" and "or".
 */
public class MCCRDatasourceAndOrFunction extends AbstractBinaryMCCRDatasourceFunction {

	/**
	 * Constant for the function types
	 */
	public final static int[] TYPES = new int[] { TYPE_AND, TYPE_OR };

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 * com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request, FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);

		// generate the filter part for cndatasources
		filterPart.addFilterStatementPart("(");
		operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
		switch (functionType) {
		case TYPE_AND:
			filterPart.addFilterStatementPart(" and ");
			break;

		case TYPE_OR:
			filterPart.addFilterStatementPart(" or ");
			break;

		default:
			throw new FilterGeneratorException("Unknown function type {" + functionType + "}");
		}
		operand[1].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
		filterPart.addFilterStatementPart(")");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return TYPES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
