package com.gentics.lib.parser.condition.function;

import java.util.Vector;

import com.gentics.lib.parser.condition.ConditionParser;

/**
 * Created by IntelliJ IDEA. User: eabin Date: May 28, 2003 Time: 8:49:37 PM To
 * change this template use Options | File Templates.
 */
public final class XnlFunctionFactory {
	private static XnlFunction[] d_funcs = null;

	static {
		Vector d_funcs_vec = new Vector();

		d_funcs_vec.add(new XnlFunctionStrToLower());
		d_funcs_vec.add(new XnlFunctionTrim());
		d_funcs_vec.add(new XnlFunctionIsPublishing());
		d_funcs = (XnlFunction[]) d_funcs_vec.toArray(new XnlFunction[d_funcs_vec.size()]);
	}

	public static ConditionParser.ParserResult isFunction(String str, int pos) {
		String substr = str.substring(pos);
		XnlFunction found = null;
		int foundLength = 0;

		for (int i = 0; i < d_funcs.length; i++) {
			XnlFunction xnlfunction = d_funcs[i];
			String[] names = xnlfunction.getFunctionStrings();

			for (int j = 0; j < names.length; j++) {
				String name = names[j];

				if (name.length() > foundLength) {
					if (substr.startsWith(name)) {
						found = xnlfunction;
						foundLength = name.length();
					}
				}
			}

		}
		if (found != null) {
			return new ConditionParser.ParserResult(found, pos + foundLength);
		}
		return null;
	}
}
