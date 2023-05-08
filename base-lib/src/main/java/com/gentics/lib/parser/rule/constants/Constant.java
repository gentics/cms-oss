/*
 * Constant.java
 *
 * Created on 08. Dezember 2004, 16:04
 */

package com.gentics.lib.parser.rule.constants;

/**
 * @author Dietmar
 */
public interface Constant {
	String getConstantIdentifier();

	String getStringValue();

	boolean verify(String TestableString);
}
