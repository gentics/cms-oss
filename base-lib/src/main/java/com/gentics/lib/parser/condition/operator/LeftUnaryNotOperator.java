package com.gentics.lib.parser.condition.operator;

import com.gentics.lib.parser.condition.ConditionParser;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 29, 2003 Time: 11:16:55 AM To
 * change this template use Options | File Templates.
 */
public class LeftUnaryNotOperator implements LeftUnaryOperator {
	public Object performOperation(Object operand) {
		Boolean isTrue = ConditionParser.isTrue(operand);

		if (isTrue == null) {
			return null;
		}
		return new Boolean(!isTrue.booleanValue());
	}

	public String[] getOperatorStrings() {
		return new String[] { "!" };
	}
}
