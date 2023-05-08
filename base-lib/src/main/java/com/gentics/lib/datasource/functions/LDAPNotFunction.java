/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: LDAPNotFunction.java,v 1.4 2007-08-17 10:37:12 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;

/**
 * Implementation for ldap filter generation for NOT (!)
 */
public class LDAPNotFunction extends AbstractUnaryLDAPFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_NOT};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.filtergenerator.FilterPart,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		switch (functionType) {
		case TYPE_NOT:
			assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);
			filterPart.addFilterStatementPart("!(");
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(")");
			break;

		default:
			unknownTypeFound(functionType);
		}
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
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
