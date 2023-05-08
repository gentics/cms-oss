/*
 * IsEmptyFunction.java
 *
 * Created on 01. Dezember 2004, 12:10
 */

package com.gentics.lib.parser.rule.functions;

import java.util.Iterator;

import com.gentics.lib.parser.rule.Operand;

/**
 * @author Dietmar
 */
public class IsEmptyFunction implements Function {

	/** Creates a new instance of IsEmptyFunction */
	public IsEmptyFunction() {}

	public String getName() {
		return "isempty";
	}

	public int getMaxParameterCount() {
		return 1;
	}

	/**
	 * @param params a list of Operands
	 * @return
	 */
	public Object execute(java.util.Vector params) {
		Boolean returnValue = Boolean.TRUE;

		for (Iterator iter = params.iterator(); iter.hasNext() && returnValue.booleanValue();) {
			Operand operand = (Operand) iter.next();
			String[] operandValues = operand.getValues();

			if (operandValues.length > 2) {
				returnValue = Boolean.FALSE;
			} else if (operandValues.length == 1) {
				if (operandValues[0] != null && operandValues[0].length() > 0) {
					returnValue = Boolean.FALSE;
				}
			}
		}

		return returnValue;
	}

	public int getMinParameterCount() {
		return 1;
	}

	public String[] getAllNames() {
		return new String[] { getName() };
	}

}
