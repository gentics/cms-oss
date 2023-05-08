/*
 * @author: Stefan Hepp
 * @date: 22.01.2006
 * @version: $Id: StringConstant.java,v 1.3 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.constant;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * A simple String constant implementation which holds a single String value (without quotes or escape-chars).
 */
public class StringConstant implements ConstantOperand {

	private String value;

	public StringConstant(String value) {
		this.value = value;
	}

	public String getOperandSymbol() {
		return value;
	}

	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		return value;
	}

	public Object getValue() {
		return value;
	}
}
