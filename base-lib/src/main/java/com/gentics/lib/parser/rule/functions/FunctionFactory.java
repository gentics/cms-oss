/*
 * FunctionFactory.java
 *
 * Created on 01. Dezember 2004, 12:15
 */

package com.gentics.lib.parser.rule.functions;

import java.util.Collection;
import java.util.Vector;

/**
 * @author Dietmar
 */
public final class FunctionFactory {
	// TODO use factory!!!
	private static Vector functionList;

	static {
		functionList = new Vector();
		functionList.add(new ConcatFunction());
		functionList.add(new IsEmptyFunction());
		functionList.add(new SubruleFunction());
	}

	/** Creates a new instance of FunctionFactory */
	private FunctionFactory() {}

	public final static Collection getFunctions() {
		return functionList;
	}
}
