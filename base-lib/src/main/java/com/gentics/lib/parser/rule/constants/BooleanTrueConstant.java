/*
 * BooleanConstant.java
 *
 * Created on 08. Dezember 2004, 16:05
 */

package com.gentics.lib.parser.rule.constants;

/**
 * @author Dietmar
 */
public class BooleanTrueConstant implements Constant {

	/** Creates a new instance of BooleanConstant */
	public BooleanTrueConstant() {}

	public String getConstantIdentifier() {
		return "TRUE";
	}

	public boolean verify(String TestableString) {
		boolean ret = false;

		if ("TRUE".equals(TestableString)) {
			ret = true;
		}
		return ret;
	}

	public String getStringValue() {
		return "1";
	}

}
