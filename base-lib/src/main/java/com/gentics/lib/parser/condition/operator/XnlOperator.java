package com.gentics.lib.parser.condition.operator;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 8:39:27 PM To
 * change this template use Options | File Templates.
 */
public interface XnlOperator {

	/**
	 * Operator::getOperatorStr()
	 * @return an array of names for this operator ( eg array( "==" ) )
	 */
	public String[] getOperatorStrings();
}
