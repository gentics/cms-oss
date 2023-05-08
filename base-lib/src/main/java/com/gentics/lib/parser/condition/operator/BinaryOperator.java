package com.gentics.lib.parser.condition.operator;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 8:43:50 PM To
 * change this template use Options | File Templates.
 */
// ----------------- binary operators -------------------------
public interface BinaryOperator extends XnlOperator {
	public static final int PRIORITY_AND = 10;

	public static final int PRIORITY_OR = 10;

	public static final int PRIORITY_EQ = 15;

	public static final int PRIORITY_NEQ = 15;

	public static final int PRIORITY_GT = 15;

	public static final int PRIORITY_LT = 15;

	public static final int PRIORITY_GTEQ = 15;

	public static final int PRIORITY_LTEQ = 15;

	public static final int PRIORITY_MOD = 20;

	/**
	 * BinaryOperator::performOperation() performs an operation with
	 * $leftOperand and $rightOperand and returns its result NOTE: you must not
	 * return a boolean - value!!!! they are reserved for internal error
	 * handling. return 0 | 1 instead
	 * @param leftOperand
	 * @param rightOperand
	 * @return
	 */
	public Object performOperation(Object leftOperand, Object rightOperand);

	/**
	 * BinaryOperator::getPriority() returns the priority of this operator. the
	 * higher the priority the sooner will be executed. e.g: 1 || 3 > 2 ||
	 * priority 1 > priority 2 first 3 > 2 will be evaluated and then 1 || 3
	 * NOTE: priorities should be coordinated using the XNL_O_PRIORITY_
	 * constants
	 * @return
	 */
	public int getPriority();
}
