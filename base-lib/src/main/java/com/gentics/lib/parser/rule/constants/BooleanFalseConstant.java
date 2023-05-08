/*
 * BooleanConstant.java
 *
 * Created on 08. Dezember 2004, 16:05
 */

package com.gentics.lib.parser.rule.constants;

/**
 * @author Dietmar
 */
public class BooleanFalseConstant implements Constant {

	/** Creates a new instance of BooleanConstant */
	public BooleanFalseConstant() {}

	public String getConstantIdentifier() {
		return "FALSE";
	}

	public boolean verify(String TestableString) {
		boolean ret = false;

		if ("FALSE".equals(TestableString)) {
			ret = true;
		}
		return ret;
	}

	public String getStringValue() {
		return "0";
	}

}
