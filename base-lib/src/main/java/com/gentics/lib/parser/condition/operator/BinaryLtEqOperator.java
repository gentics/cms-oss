/**
 * @author erwin
 * @date: May 28, 2003 Time: 10:45:58 PM $Id: BinaryLtEqOperator.java,v 1.3
 *        2005/03/09 11:28:00 haymo Exp $
 */
package com.gentics.lib.parser.condition.operator;

import com.gentics.api.lib.etc.ObjectTransformer;

public class BinaryLtEqOperator implements BinaryOperator {

	/**
	 * Gets the LowerThan Operator String
	 * @return
	 * @see com.gentics.lib.parser.condition.operator.XnlOperator#getOperatorStrings()
	 */
	public String[] getOperatorStrings() {
		return new String[] { "<=" };
	}

	/**
	 * Gets the priority of LowerThan Operator
	 * @return
	 * @see com.gentics.lib.parser.condition.operator.BinaryOperator#getPriority()
	 */
	public int getPriority() {
		return BinaryOperator.PRIORITY_LTEQ;
	}

	/**
	 * Performs the operation of LowerThan Operator with 2 Operands
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
			if (leftint <= rightint) {
				return new Boolean(true);
			}
			return new Boolean(false);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
