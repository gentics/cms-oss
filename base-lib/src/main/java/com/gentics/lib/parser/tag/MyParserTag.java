package com.gentics.lib.parser.tag;

import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: wpsadmin Date: 28.05.2003 Time: 10:43:40 To
 * change this template use Options | File Templates.
 * @deprecated used by deprecated parser
 */
public class MyParserTag extends MyParserNode {
	String m_keyname;

	Vector m_parts;

	Vector m_result;

	public MyParserTag(String type) {
		super(type);
		this.m_keyname = "";
		this.m_parts = new Vector();
		m_result = null;
	}

	public void setKeyname(String value) {
		this.m_keyname = value;
	}

	public String getKeyname() {
		return this.m_keyname;
	}

	public void setParts(Vector values) {
		this.m_parts = values;
	}

	public Vector getParts() {
		return this.m_parts;
	}

	public Vector getResult() {
		return m_result;
	}

	public void setResult(Vector result) {
		m_result = result;
	}
}
