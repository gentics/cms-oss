/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: ConstantOperand.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.constant;

import com.gentics.contentnode.parser.expression.Operand;

/**
 * An interface for constant operands. Constants contain a value, which cannot be accessed by name
 * like the value of variables.
 */
public interface ConstantOperand extends Operand {

	/**
	 * get the current value of the constant.
	 * @return the value of the constant, or null if not set.
	 */
	Object getValue();

}
