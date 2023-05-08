/*
 * @author: Stefan Hepp
 * @date: 03.02.2006
 * @version: $Id: BooleanConstant.java,v 1.1 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression.constant;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * A boolean constant implementation. This contains one single boolean value.
 */
public class BooleanConstant implements ConstantOperand {

	private Boolean value;

	/**
	 * Constructor to create a new constant with value.
	 * @param value the value of the constant.
	 */
	public BooleanConstant(Boolean value) {
		this.value = value;
	}

	/**
	 * Constructor to create a new constant with a value.
	 * @param value the value of the constant.
	 */
	public BooleanConstant(boolean value) {
		this.value = Boolean.valueOf(value);
	}

	/**
	 * get the current value as Boolean.
	 * @return the current value as Boolean, or null if not set.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * get the current boolean symbol.
	 * @return the current constant symbol.
	 */
	public String getOperandSymbol() {
		return value != null ? value.toString() : null;
	}

	/**
	 * get the current value of the constant.
	 * @param renderType
	 * @param renderResult
	 * @return the current value of the constant, or null if not set.
	 */
	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		return value;
	}
}
