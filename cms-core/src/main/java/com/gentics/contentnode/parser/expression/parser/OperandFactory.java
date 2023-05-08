/*
 * @author Stefan Hepp
 * @date 03.02.2006
 * @version $Id: OperandFactory.java,v 1.2 2006-02-03 17:34:39 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

import com.gentics.contentnode.parser.expression.Operand;

/**
 * An interface for operand-factories. An operandfactory can be used to get all known
 * operand keynames and to create a new operand by keyname.
 *
 * TODO define methods to get keynames so that they can be used by the tokenizer without any reformatting or parsing.
 * TODO define methods to set aliases for operands (like NOT for ! and so on ..), or get aliases from operands.
 */
public interface OperandFactory {

	/**
	 * Create a new operand by keyname.
	 *
	 * @param keyname the keyname of the operand.
	 * @return a new operand instance, or null if the keyname is unknown.
	 */
	Operand createOperand(String keyname);

}
