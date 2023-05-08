/*
 * @author: Stefan Hepp
 * @date: 22.01.2006
 * @version: $Id: NumericConstant.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.constant;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * A numeric constant implementation, which holds a single Double value.
 */
public class NumericConstant implements ConstantOperand {
    
	private Double value;

	public NumericConstant(Double value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public String getOperandSymbol() {
		return value.toString();
	}

	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		return value;
	}
}
