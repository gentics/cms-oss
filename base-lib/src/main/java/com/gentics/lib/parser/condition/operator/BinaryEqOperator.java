package com.gentics.lib.parser.condition.operator;

import com.gentics.lib.util.PHPTypeJuggler;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 10:45:58 PM To
 * change this template use Options | File Templates.
 */
public class BinaryEqOperator implements BinaryOperator {
	public String[] getOperatorStrings() {
		return new String[] { "==" };
	}

	public int getPriority() {
		return BinaryOperator.PRIORITY_EQ;
	}

	public Object performOperation(Object leftOperand, Object rightOperand) {
		if (leftOperand == null) {
			if (rightOperand == null) {
				return new Boolean(true);
			} else {
				return new Boolean(false);
			}
		} else if (rightOperand == null) {
			return new Boolean(false);
		} else {
			String strLeft = PHPTypeJuggler.string(leftOperand);
			String strRight = PHPTypeJuggler.string(rightOperand);

			if (strLeft.equals(strRight)) {
				return new Boolean(true);
			} else {
				return new Boolean(false);
			}
		}

	}
}
