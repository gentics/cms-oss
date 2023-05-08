/*
 * @author norbert
 * @date 04.04.2005
 * @version $Id: SubruleFunction.java,v 1.2 2005-08-01 07:41:39 laurin Exp $
 */
package com.gentics.lib.parser.rule.functions;

import java.util.Vector;

/**
 * class for the subrule function.
 * @author norbert
 */
public class SubruleFunction implements Function {
	private final static String[] NAMES = { "subrule" };

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.functions.Function#getMinParameterCount()
	 */
	public int getMinParameterCount() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.functions.Function#getMaxParameterCount()
	 */
	public int getMaxParameterCount() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.functions.Function#getName()
	 */
	public String getName() {
		return NAMES[0];
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.functions.Function#getAllNames()
	 */
	public String[] getAllNames() {
		return NAMES;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.parser.rule.functions.Function#execute(java.util.Vector)
	 */
	public Object execute(Vector params) {
		return "";
	}
}
