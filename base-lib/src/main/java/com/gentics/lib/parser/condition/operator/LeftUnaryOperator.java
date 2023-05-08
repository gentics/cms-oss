package com.gentics.lib.parser.condition.operator;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 8:41:07 PM To
 * change this template use Options | File Templates.
 */
// ----------------- left unary operators -------------------------
public interface LeftUnaryOperator extends XnlOperator {

	/**
	 * LeftUnaryOperator::performOperation() performs an operation on $operand
	 * and returns its result NOTE:
	 * @param operand
	 * @return the result of this unary operator, when used with the given
	 *         operand
	 */
	public Object performOperation(Object operand);
}
