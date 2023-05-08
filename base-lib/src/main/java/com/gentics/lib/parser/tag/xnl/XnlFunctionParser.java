package com.gentics.lib.parser.tag.xnl;

import java.util.Map;

import com.gentics.lib.parser.tag.MyObjectParser;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 10.05.2004 Time: 14:06:23 To
 * change this template use File | Settings | File Templates.
 * @deprecated new xnlparser in cn.node
 */
public class XnlFunctionParser extends MyObjectParser {
	public void addCodePart(String keyname, String code, Map params) {}

	public boolean keyExists() {
		return false;
	}

	public Object parse(String input, Map props) {
		return null;
	}
}
