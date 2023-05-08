package com.gentics.lib.parser.condition.operator;

import com.gentics.lib.parser.condition.ConditionParser;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 11:36:45 PM To
 * change this template use Options | File Templates.
 */
class BinaryAndOperator implements BinaryOperator {

	public String[] getOperatorStrings() {
		return new String[] { "&&" };
	}

	// ----------------- as defined in BinaryOperator -------------------------

	public int getPriority() {
		return PRIORITY_AND;
	}

	public Object performOperation(Object leftOperand, Object rightOperand) {
		Boolean bLeftOperand = ConditionParser.isTrue(leftOperand);

		if (bLeftOperand == null) {
			return null;
		}
		Boolean bRightOperand = ConditionParser.isTrue(rightOperand);

		if (bRightOperand == null) {
			return null;
		}

		if ((bLeftOperand).booleanValue() && (bRightOperand).booleanValue()) {
			return new Boolean(true);
		}
		return new Boolean(false);
	}
}
