package com.gentics.lib.parser.condition.operator;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 10:45:58 PM To
 * change this template use Options | File Templates.
 */
public class BinaryModOperator implements BinaryOperator {
	public String[] getOperatorStrings() {
		return new String[] { "%" };
	}

	public int getPriority() {
		return BinaryOperator.PRIORITY_MOD;
	}

	public Object performOperation(Object leftOperand, Object rightOperand) {
		try {
			int leftint, rightint;

			leftint = ObjectTransformer.getInt(leftOperand, 0);
			rightint = ObjectTransformer.getInt(rightOperand, 0);
			return new Integer(leftint % rightint);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
