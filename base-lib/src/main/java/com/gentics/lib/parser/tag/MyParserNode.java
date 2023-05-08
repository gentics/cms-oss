package com.gentics.lib.parser.tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: wpsadmin Date: 28.05.2003 Time: 09:30:26 To
 * change this template use Options | File Templates.
 * @deprecated used by deprecated parser
 */
public class MyParserNode {
	String d_type;

	Map d_params;

	public MyParserNode(String type) {
		this.d_type = type;
		this.d_params = new HashMap();
	}

	public String getTagType() {
		return this.d_type;
	}

	public void setProperties(Map value) {
		this.d_params = value;
	}

	public Map getProperties() {
		return this.d_params;
	}
}
