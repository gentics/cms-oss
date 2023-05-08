/*
 * constantFactory.java
 *
 * Created on 01. Dezember 2004, 12:15
 */

package com.gentics.lib.parser.rule.constants;

import java.util.Collection;
import java.util.Vector;

/**
 * @author Dietmar
 */
public final class ConstantFactory {
	// TODO use factory!!!
	private static Vector constantList;

	static {
		constantList = new Vector();
		constantList.add(new BooleanTrueConstant());
		constantList.add(new BooleanFalseConstant());
	}

	/** Creates a new instance of constantFactory */
	private ConstantFactory() {}

	public final static Collection getConstants() {
		return constantList;
	}
}
