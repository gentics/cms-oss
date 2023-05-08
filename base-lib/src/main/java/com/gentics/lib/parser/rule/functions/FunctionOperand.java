package com.gentics.lib.parser.rule.functions;

import java.util.Iterator;
import java.util.Vector;

import com.gentics.api.lib.rule.RuleTree;
import com.gentics.lib.base.InvalidationListener;
import com.gentics.lib.parser.rule.Operand;

/**
 * created at Nov 21, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class FunctionOperand implements Operand {
	Function function;

	Vector params;

	public FunctionOperand(Function function, Vector params) {
		this.function = function;
		this.params = params;
	}

	public String getValue() {
		return function.execute(params).toString();
	}

	public String[] getValues() {
		return new String[] { getValue() };
	}

	public Function getFunction() {
		return function;
	}

	public Vector getParams() {
		return params;
	}

	public void setInvalidateListener(InvalidationListener listener) {// TODO implement -> on invalidate all sub-operands
	}

	public void removeListener() {}

	public String toString() {
		String ret = "RuleFunction: " + function.getName() + "(";
		Iterator it = params.iterator();
		boolean first = true;

		while (it.hasNext()) {
			if (first) {
				first = false;
			} else {
				ret += ", ";
			}
			Operand operand = (Operand) it.next();

			ret += operand;
		}
		return ret + ")";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.Operand#deepCopy(com.gentics.api.lib.rule.RuleTree)
	 */
	public Operand deepCopy(RuleTree ruleTree) {
		Vector copyParams = new Vector();

		for (Iterator i = params.iterator(); i.hasNext();) {
			Operand operand = (Operand) i.next();

			copyParams.add(operand.deepCopy(ruleTree));
		}
		return new FunctionOperand(function, copyParams);
	}
}
