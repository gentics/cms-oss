/*
 * @author Stefan Hepp
 * @date 03.02.2006
 * @version $Id: DefaultOperandFactory.java,v 1.1 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.parser;

import com.gentics.contentnode.parser.expression.Operand;

import java.util.Map;
import java.util.HashMap;

/**
 * A default implementation of an operand factory which can register new operands and
 * initialize itself with all known operands.
 *
 * TODO allow configuration of additional operand classes? own OperandFactory impl?
 */
public class DefaultOperandFactory implements OperandFactory {

	private Map functions;
	private Map leftUnaryOperands;
	private Map rightUnaryOperands;
	private Map binaryOperands;

	public DefaultOperandFactory(boolean loadDefaultOperands) {
		initializeOperandTables();
		if (loadDefaultOperands) {
			registerDefaultOperands();
		}
	}

	private void initializeOperandTables() {

		functions = new HashMap();
		leftUnaryOperands = new HashMap();
		rightUnaryOperands = new HashMap();
		binaryOperands = new HashMap();

	}

	/**
	 * register all currently known operands in the operand tables.
	 */
	public void registerDefaultOperands() {}

	/**
	 * register a new operand to the operand tables. The operand keywords for different
	 * operand-classes must be mutually exclusive.
	 *
	 * TODO check for invalid syntax (overlapping prefixes,..)
	 *
	 * @param operand the operand to register.
	 * @return the operand previously stored under the operand's keyname, or null if not set.
	 */
	public Operand registerOperand(Operand operand) {
		return null;
	}

	public Operand createOperand(String keyname) {
		return null;
	}
}
