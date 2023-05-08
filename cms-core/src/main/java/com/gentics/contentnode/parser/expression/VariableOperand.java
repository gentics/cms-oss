/*
 * @author Stefan Hepp
 * @date 22.01.2006
 * @version $Id: VariableOperand.java,v 1.2 2006-02-03 16:12:26 stefan Exp $
 */
package com.gentics.contentnode.parser.expression;

import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.parser.expression.constant.ConstantOperand;
import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

/**
 * A VariableOperand is a special operand which wraps and evaluates a value object.
 * If the value is an operand, it is evaluated, else the object itself is used as value.
 * If the result-value is a resolvable and a path is set to the variable, the resolvable will
 * be used to resolve the path.
 */
public class VariableOperand implements ConstantOperand {

	private String name;
	private String resolvePath;
	private Object value;

	/**
	 * Constructor to create a new variable with a value.
	 * @param name name of the variable.
	 * @param value the value of the variable.
	 */
	public VariableOperand(String name, Object value) {
		this.name = name;
		this.value = value;
		resolvePath = null;
	}

	/**
	 * Constructor to create a new variable with a value and a path.
	 * The value must either be a resolvable, or an operand with returns
	 * an operand when evaluated.
	 *
	 * @param name name of the variable.
	 * @param resolvePath the path to be resolved.
	 * @param value the value of the variable.
	 */
	public VariableOperand(String name, String resolvePath, Object value) {
		this.name = name;
		this.resolvePath = resolvePath;
		this.value = value;
	}

	/**
	 * get the name of the variable.
	 * @return the name of the variable.
	 */
	public String getName() {
		return name;
	}

	/**
	 * get the resolve path of the variable.
	 * @return the path to resolve, or null if not set.
	 */
	public String getResolvePath() {
		return resolvePath;
	}

	/**
	 * get the current value of the variable.
	 * @return the current value, or null if not set.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * set the current value of the variable.
	 * Note that you should usually register the variable to the root expression and bind its value
	 * using the expressions bind-methods.
	 *
	 * @param value the new value.
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * check if the variable has a value.
	 * @return true, if the value is set, else false.
	 */
	public boolean hasValue() {
		return value != null;
	}

	/**
	 * get the name of the variable as symbol.
	 * @return the name of the variable.
	 */
	public String getOperandSymbol() {
		return name;
	}

	/**
	 * Evaluate the variable. If the value is an operand, evaluate the operand first. If a resolve
	 * path is set, try to resolve the path.
	 *
	 * @param renderType
	 * @param renderResult
	 * @return the result value if everything was sucessfull, else null.
	 */
	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		Object rs;

		if (value instanceof Operand) {
			rs = ((Operand) value).evaluate(renderType, renderResult);
		} else {
			rs = value;
		}
		if (resolvePath != null && rs != null) {
			if (rs instanceof Resolvable) {
				PropertyResolver resolver = new PropertyResolver((Resolvable) rs);

				try {
					rs = resolver.resolve(resolvePath);
				} catch (UnknownPropertyException e) {
					renderResult.warn("Invalid expression", "Could not resolve the path '" + resolvePath + "'.");
					rs = null;
				}
			} else {
				renderResult.warn("Invalid expression", "Could not resolve the path '" + resolvePath + "'; the object is not resolvable.");
			}
		}
		return rs;
	}

}
