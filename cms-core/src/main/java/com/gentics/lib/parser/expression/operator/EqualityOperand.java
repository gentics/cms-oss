/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: EqualityOperand.java,v 1.3 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.lib.parser.expression.operator;

import com.gentics.contentnode.parser.expression.Operand;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * This is an equality comparison operand. It compares the operands depending on the
 * evaluated values' equals() method.
 */
public class EqualityOperand implements BinaryOperand {

	private Operand leftOperand;
	private Operand rightOperand;

	public EqualityOperand() {}

	public String getOperandSymbol() {
		return "==";
	}

	public Operand getLeftOperand() {
		return leftOperand;
	}

	public Operand getRightOperand() {
		return rightOperand;
	}

	public Operand setLeftOperand(Operand operand) {
		Operand old = leftOperand;

		leftOperand = operand;
		return old;
	}

	public Operand setRightOperand(Operand operand) {
		Operand old = rightOperand;

		rightOperand = operand;
		return old;
	}

	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		return null;
	}
}
