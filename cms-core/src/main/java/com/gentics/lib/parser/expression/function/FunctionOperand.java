/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: FunctionOperand.java,v 1.3 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.lib.parser.expression.function;

import com.gentics.contentnode.parser.expression.Operand;

/**
 * An interface for function operands. All functions must implement this interface.
 * A Function has a function name, and a list of argument. The number of
 * arguments can be fixed or variable.
 */
public interface FunctionOperand extends Operand {

	/**
	 * Get the number of expected arguments.
	 * @return the number of arguments for this function, or -1 if variable.
	 */
	int getArgumentCount();

	/**
	 * set the arguments to this function.
	 * @param operands a list of arguments for this function.
	 * @return the list of previously stored arguments.
	 */
	Operand[] setArguments(Operand[] operands);

	/**
	 * get the currently associated argument operands of this function.
	 * @return the list of arguments.
	 */
	Operand[] getArguments();

}
