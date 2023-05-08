package com.gentics.lib.parser.condition.operator;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 10:45:58 PM To
 * change this template use Options | File Templates.
 */

public class BinaryGtOperator implements BinaryOperator {

	/**
	 * @return
	 * @see com.gentics.lib.parser.condition.operator.XnlOperator#getOperatorStrings()
	 */
	public String[] getOperatorStrings() {
		return new String[] { ">" };
	}

	/**
	 * @return
	 * @see com.gentics.lib.parser.condition.operator.BinaryOperator#getPriority()
	 */
	public int getPriority() {
		return BinaryOperator.PRIORITY_GT;
	}

	/**
	 * @param leftOperand
	 * @param rightOperand
	 * @return
	 * @see com.gentics.lib.parser.condition.operator.BinaryOperator#performOperation(
	 *      java.lang.Object, java.lang.Object)
	 */
	public Object performOperation(Object leftOperand, Object rightOperand) {
		try {
			int leftint, rightint;

			leftint = ObjectTransformer.getInt(leftOperand, 0);
			rightint = ObjectTransformer.getInt(rightOperand, 0);
			if (leftint > rightint) {
				return new Boolean(true);
			}
			return new Boolean(false);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
