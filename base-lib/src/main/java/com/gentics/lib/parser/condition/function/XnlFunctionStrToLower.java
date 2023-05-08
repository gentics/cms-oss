package com.gentics.lib.parser.condition.function;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 29, 2003 Time: 12:17:58 PM To
 * change this template use Options | File Templates.
 */
public class XnlFunctionStrToLower extends XnlFunction {
	public String[] getFunctionStrings() {
		return new String[] { "strToLower" };
	}

	public int getMinParamCount() {
		return 1;
	}

	public Object execute(Vector params) {
		return params.get(0).toString().toLowerCase();
	}
}
