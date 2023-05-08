/*
 * @author norbert
 * @date 27.07.2006
 * @version $Id: LDAPBinaryDummyFunction.java,v 1.2 2007-08-17 10:37:12 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;

/**
 * Dummy function to avoid error message for not implemented function (binary calculations)
 */
public class LDAPBinaryDummyFunction extends AbstractBinaryLDAPFunction {
	private final static int[] NOTIMPLEMENTED_TYPES = new int[] { TYPE_ADD, TYPE_SUB, TYPE_MULT, TYPE_DIV, TYPE_MOD};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return NOTIMPLEMENTED_TYPES;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_NUMBER;
	}
}
