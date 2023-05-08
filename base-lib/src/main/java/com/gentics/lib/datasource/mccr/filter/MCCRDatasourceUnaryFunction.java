package com.gentics.lib.datasource.mccr.filter;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;

/**
 * Implementation for evaluation and filter generation for unary functions (+, -, !)
 */
public class MCCRDatasourceUnaryFunction extends AbstractUnaryMCCRDatasourceFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_MINUS, TYPE_PLUS, TYPE_NOT };

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 * com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request, FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		switch (functionType) {
		case TYPE_PLUS:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_NUMBER, expectedValueType);
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
			break;

		case TYPE_MINUS:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_NUMBER, expectedValueType);
			filterPart.addFilterStatementPart("-(");
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(")");
			break;

		case TYPE_NOT:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);
			filterPart.addFilterStatementPart("NOT (");
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(")");
			break;
		}
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
		switch (functionType) {
		case TYPE_PLUS:
		case TYPE_MINUS:
			return ExpressionEvaluator.OBJECTTYPE_NUMBER;

		case TYPE_NOT:
			return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;

		default:
			return ExpressionEvaluator.OBJECTTYPE_ANY;
		}
	}
}
