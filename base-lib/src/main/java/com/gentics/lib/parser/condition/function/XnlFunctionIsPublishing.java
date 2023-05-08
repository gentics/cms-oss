package com.gentics.lib.parser.condition.function;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 29, 2003 Time: 12:17:58 PM To
 * change this template use Options | File Templates.
 */
public class XnlFunctionIsPublishing extends XnlFunction {
	public String[] getFunctionStrings() {
		return new String[] { "isPublishing" };
	}

	public int getMinParamCount() {
		return 0;
	}

	public Object execute(Vector params) {
		// TODO implement
		return new Boolean(false); // params.get(0).toString().toLowerCase();
	}
}
