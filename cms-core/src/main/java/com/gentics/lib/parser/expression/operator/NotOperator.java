/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: NotOperator.java,v 1.3 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.lib.parser.expression.operator;

import com.gentics.contentnode.parser.expression.Operand;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * This is a simple NOT operand, which negates a boolean value. If the value returned
 * by the operand is not a boolean value, it is regarded as Boolean.TRUE. if the return-value
 * is null, it is regarded as Boolean.FALSE.
 */
public class NotOperator implements LeftUnaryOperand {

	private Operand operand;

	public NotOperator() {
		operand = null;
	}

	public String getOperandSymbol() {
		return "!";
	}

	public Operand getOperand() {
		return operand;
	}

	public Operand setOperand(Operand operand) {
		Operand old = this.operand;

		this.operand = operand;
		return old;
	}

	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		Object value = operand != null ? operand.evaluate(renderType, renderResult) : null;

		if (value instanceof Boolean) {
			return Boolean.valueOf(!((Boolean) value).booleanValue());
		}
		return Boolean.valueOf(value == null);
	}
}
