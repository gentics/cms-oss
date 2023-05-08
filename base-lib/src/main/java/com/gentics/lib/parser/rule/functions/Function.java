package com.gentics.lib.parser.rule.functions;

import java.util.Vector;

/**
 * created at Nov 21, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public interface Function {
	int getMinParameterCount();

	int getMaxParameterCount();

	String getName();

	String[] getAllNames();

	/**
	 * @param params a list of Operands
	 * @return
	 */
	Object execute(Vector params);
}
