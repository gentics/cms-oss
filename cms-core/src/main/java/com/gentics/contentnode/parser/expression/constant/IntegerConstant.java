/*
 * @author: Stefan Hepp
 * @date: 22.01.2006
 * @version: $Id: IntegerConstant.java,v 1.3 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.constant;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * An integer constant implementation, which holds a single Integer value.
 */
public class IntegerConstant implements ConstantOperand {

	private Integer value;

	public IntegerConstant(Integer value) {
		this.value = value;
	}

	public IntegerConstant(int value) {
		this.value = new Integer(value);
	}

	public String getOperandSymbol() {
		return value.toString();
	}

	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		return value;
	}

	public Object getValue() {
		return value;
	}
}
