/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: LeftUnaryOperand.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.lib.parser.expression.operator;

import com.gentics.contentnode.parser.expression.Operand;

/**
 * This defines an interface for operands which operate on a single operand.
 */
public interface LeftUnaryOperand extends Operand {

	/**
	 * get the current operand.
	 * @return the current operand, or null if not set.
	 */
	Operand getOperand();

	/**
	 * set the current operand.
	 * @param operand the new operand to use.
	 * @return the old operand, or null if it was not set.
	 */
	Operand setOperand(Operand operand);
}
