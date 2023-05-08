/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: CNDatasourceCalcFunction.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;

/**
 * Implementation for filter generation for binary calculation
 * functions (+, -, *, /, %).
 */
public class CNDatasourceCalcFunction extends AbstractBinaryCNDatasourceFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_ADD, TYPE_SUB, TYPE_MULT, TYPE_DIV, TYPE_MOD};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.filtergenerator.FilterPart,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_NUMBER, expectedValueType);

		filterPart.addFilterStatementPart("(");
		operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_NUMBER);
		switch (functionType) {
		case TYPE_ADD:
			filterPart.addFilterStatementPart(" + ");
			break;

		case TYPE_SUB:
			filterPart.addFilterStatementPart(" - ");
			break;

		case TYPE_MULT:
			filterPart.addFilterStatementPart(" * ");
			break;

		case TYPE_DIV:
			filterPart.addFilterStatementPart(" / ");
			break;

		case TYPE_MOD:
			filterPart.addFilterStatementPart(" % ");
			break;

		default:
			break;
		}
		operand[1].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_NUMBER);
		filterPart.addFilterStatementPart(")");
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
		return ExpressionEvaluator.OBJECTTYPE_NUMBER;
	}
}
