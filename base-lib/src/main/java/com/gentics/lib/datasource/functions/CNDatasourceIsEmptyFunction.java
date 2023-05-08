/*
 * @author norbert
 * @date 25.07.2006
 * @version $Id: CNDatasourceIsEmptyFunction.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * Implementation of an "isEmpty()" function (tests for null values)
 */
public class CNDatasourceIsEmptyFunction extends AbstractUnaryCNDatasourceFunction {

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
		if (operand[0].getExpectedValueType(request.getFilter()) == ExpressionEvaluator.OBJECTTYPE_BINARY) {
			// for binary operands, only compare with null
			filterPart.addFilterStatementPart("(");
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(" IS NULL)");
		} else {
			// other operands (e.g. strings) may also be empty
			filterPart.addFilterStatementPart("((");
			filterPart.addLiteral("", ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(" IS NOT NULL AND ");
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_STRING);
			filterPart.addFilterStatementPart(" = ");
			filterPart.addLiteral("", ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(") OR ");
			operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
			filterPart.addFilterStatementPart(" IS NULL)");
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "isempty";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
