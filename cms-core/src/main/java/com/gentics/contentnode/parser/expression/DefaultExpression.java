package com.gentics.contentnode.parser.expression;

import com.gentics.contentnode.render.RenderResult;
import com.gentics.contentnode.render.RenderType;

import java.util.Set;
import java.util.Iterator;

/**
 * User: Stefan Hepp
 * Date: 22.01.2006
 * Time: 22:49:37
 */
public class DefaultExpression implements Expression {

	private Operand rootOperand;

	public DefaultExpression() {
		rootOperand = null;
	}

	public void setRootOperand(Operand operand) {
		rootOperand = operand;
	}

	public Operand getRootOperand() {
		return rootOperand;
	}

	public void registerVariable(VariableOperand variable) {}

	public boolean unregisterVariable(VariableOperand variable) {
		return false;
	}

	public VariableOperand unregisterVariable(String name) {
		return null;
	}

	public Set getVariables() {
		return null;
	}

	public Set getVariableOperands(String name) {
		return null;
	}

	public Object bindVariable(String name, Object value) {

		// TODO if the old value is an expression, unregister its variables.

		return null;
	}

	public Object readVariable(String name) {
		return null;
	}

	public Object bindExpression(String name, Expression expression) {
		return bindExpression(name, expression, true);
	}

	public Object bindExpression(String name, Expression expression, boolean registerVariables) {

		if (registerVariables) {
			Set variables = expression.getVariables();

			for (Iterator it = variables.iterator(); it.hasNext();) {
				String varname = (String) it.next();
				final Set vars = expression.getVariableOperands(varname);

				for (Iterator it2 = vars.iterator(); it2.hasNext();) {
					VariableOperand var = (VariableOperand) it2.next();

					registerVariable(var);
				}
			}
		}

		return bindVariable(name, expression);
	}

	public String getOperandSymbol() {
		return null;
	}

	public Object evaluate(RenderType renderType, RenderResult renderResult) {
		return rootOperand != null ? rootOperand.evaluate(renderType, renderResult) : null;
	}
}
