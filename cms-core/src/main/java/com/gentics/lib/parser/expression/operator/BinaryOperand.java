/*
 * @author: Stefan Hepp
 * @date: 22.01.2006
 * @version: $Id: BinaryOperand.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.lib.parser.expression.operator;

import com.gentics.contentnode.parser.expression.Operand;

/**
 * This interface defines an operand which is enclosed by two values.
 */
public interface BinaryOperand extends Operand {

	/**
	 * Get the left operand.
	 * @return the left operand, or null if not set.
	 */
	Operand getLeftOperand();

	/**
	 * Get the right operand.
	 * @return the right operand, or null if not set.
	 */
	Operand getRightOperand();

	/**
	 * set the left operand.
	 * @param operand the new left operand.
	 * @return the old left operand, or null if it was not set.
	 */
	Operand setLeftOperand(Operand operand);

	/**
	 * set the right operand.
	 * @param operand the new right operand.
	 * @return the old right operand, or null if it was not set.
	 */
	Operand setRightOperand(Operand operand);

}
