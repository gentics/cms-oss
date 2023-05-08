/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: LDAPAndOrFunction.java,v 1.4 2007-08-17 10:37:12 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.lib.datasource.LDAPDatasource;

/**
 * Implementation for ldap filter generation for "and" and "or".
 */
public class LDAPAndOrFunction extends AbstractBinaryLDAPFunction {

	/**
	 * Constant for the function types
	 */
	public final static int[] TYPES = new int[] { TYPE_AND, TYPE_OR};

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
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);

		if (LDAPDatasource.class.isAssignableFrom(request.getFilter().getDatasourceClass())) {
			// generate the filter part for ldapdatasources
			switch (functionType) {
			case TYPE_AND:
				filterPart.addFilterStatementPart("(&");
				break;

			case TYPE_OR:
				filterPart.addFilterStatementPart("(|");
				break;

			default:
				throw new FilterGeneratorException("Unknown function type {" + functionType + "}");
			}
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			operand[1].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			filterPart.addFilterStatementPart(")");
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
