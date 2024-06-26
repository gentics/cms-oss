/* Generated By:JJTree: Do not edit this line. ASTTrue.java */

package com.gentics.lib.expressionparser.parser;

import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;

public class ASTTrue extends LiteralNode {
	public ASTTrue(int id) {
		super(id);
	}

	public ASTTrue(Parser p, int id) {
		super(p, id);
	}

	protected Object getLiteralValue() {
		return Boolean.TRUE;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.EvaluableExpression#getExpectedValueType(com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter)
	 */
	public int getExpectedValueType(DatasourceFilter filter) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
