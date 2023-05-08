package com.gentics.lib.parser.tag;

import java.util.Map;

/**
 * Created by IntelliJ IDEA. <p/>User: wpsadmin <p/>Date: 28.05.2003 <p/>
 * Time: 10:50:00 <p/>To change this template use Options | File Templates.
 * @deprecated used by deprecated parser
 */
public abstract class MyObjectParser implements Cloneable {
	String d_keyname;

	String d_type;

	MyGenericParser d_parser = null;

	MyParserTag d_parentTag;

	public Object clone() throws CloneNotSupportedException {
		MyObjectParser parser = (MyObjectParser) super.clone(); // To change body of

		// overridden
		// methods use File
		// | Settings | File
		// Templates.
		parser.d_keyname = new String(d_keyname);
		return parser;
	}

	public MyObjectParser() {
		this.d_keyname = "";
		this.d_type = "";
	}

	public void setType(String type) {
		this.d_type = type;
	}

	public String getType() {
		return this.d_type;
	}

	public void setKeyname(String keyname) {
		// TODO we don't need toLowerCase() - do we?
		// this.d_keyname = keyname.toLowerCase();
		this.d_keyname = keyname;
	}

	public String getKeyname() {
		return this.d_keyname;
	}

	public MyGenericParser getParser() {
		return this.d_parser;
	}

	public void setParser(MyGenericParser parser) {
		this.d_parser = parser;
	}

	public MyParserTag getParentTag() {
		return d_parentTag;
	}

	public void setParentTag(MyParserTag parentTag) {
		this.d_parentTag = parentTag;
	}

	public abstract void addCodePart(String keyname, String code, Map params);

	public void clearCodeParts() {}

	public abstract boolean keyExists();

	public abstract Object parse(String input, Map props);

}
