package com.gentics.lib.parser.tag.xnl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.lib.parser.tag.MyGenericParser;
import com.gentics.lib.parser.tag.MyParserNode;
import com.gentics.lib.parser.tag.MyParserTag;

/**
 * Created by IntelliJ IDEA. <p/>User: wpsadmin <p/>Date: 28.05.2003 <p/>
 * Time: 11:09:13 <p/>To change this template use Options | File Templates.
 * @deprecated new xnlparser in cn.java on the way
 */
public class XnlParser extends MyGenericParser {
	private static HashMap instanceMap = new HashMap();

	// var d_requestor;

	private String prefix;

	/**
	 * Constructor inits parser.
	 */
	public XnlParser() {
		this("xnl");
	}

	public XnlParser(String prefix) {
		// parent::NodeParser(LINKT_XNL);
		this.prefix = prefix;
		this.setParseTagParams(true);
		this.setParseTagResult(false);
		this.initDefaultRequestor();
		this.initDefaultParser();
	}

	public static XnlParser getInstance() {
		return getInstance("xnl");
	}

	public static XnlParser getInstance(String prefix) {
		XnlParser instance = (XnlParser) instanceMap.get(prefix);

		if (instance == null) {
			instance = new XnlParser(prefix);
			instanceMap.put(prefix, instance);
		}
		return instance;
	}

	public String getParserKey() {
		return this.prefix;
	}

	public void initDefaultRequestor() {/*
		 * global XNL_FUNCTION, DEBUG; xnlloader = new XNLLoader(XNL_FUNCTION);
		 * xnlloader.fetchPackages(); requestor = new XNLRequest(xnlloader);
		 * this.d_requestor = requestor;
		 */}

	public void initDefaultParser() {
		this.addObjectParser("if", new XnlIfParser());

		// register those default parsers..

		// ifs
		/*
		 * parser = new XNLIfParser(); this.addObjectParser("if",parser);
		 * //workaround for really cool handling of object references in php
		 * *kotz* unset( parser ); // funcs
		 */
		this.addObjectParser("func", new XnlFunctionParser(), new String[] { "function" });

		/*
		 * parser = new XNLFunctionParser(this.d_requestor);
		 * this.addObjectParser("func", parser, array("function")); unset(
		 * parser ); // props
		 */
		this.addObjectParser("prop", new XnlPropertyParser(), new String[] { "property" });

		/*
		 * parser = new XNLPropertyParser(); this.addObjectParser("prop",
		 * parser, array("property"));
		 */
	}

	/*
	 * public XnlRequestor getRequestor() { return this.d_requestor; }
	 */

	private boolean inStringArray(String key, String[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].equals(key)) {
				return true;
			}
		}
		return false;
	}

	public TagResult _generateTags(Map properties, boolean hasparts, String tagcode) {
		// Vector tags = new Vector();

		// list of compatibility tags which do not require end tags

		String[] ctags = new String[] { "end", "else" };

		// list of splitter tags ..

		String[] stags = new String[] { "else" };
		Vector taglist = new Vector();
		Object[] keys = properties.keySet().toArray();

		// while (list(key,prop) = each(properties)) {

		for (int i = 0; i < keys.length; i++) {
			// build up splitter

			String key = (String) keys[i];
			Object prop = properties.get(key);

			if (prop == null) {
				continue;
			}
			if (inStringArray(key, stags) || key.equals("end")) {
				MyParserNode tag = new MyParserNode(key);

				// unset(properties[key]);
				properties.remove(key);
				taglist.add(tag);
			}

			if (key.equals("type")) {
				String type = (String) prop;
				String name = (String) properties.get("name");

				properties.remove("type");
				properties.remove("name");
				MyParserTag tag = new MyParserTag(type);

				tag.setKeyname(name);
				tag.setProperties(properties);
				taglist.add(tag);
				break;
			}

			// some compatibility/simplicity tags

			if (key.equals("func") || key.equals("function")) {
				String type = "function";
				String name = (String) prop;
				String edit = (String) properties.get("edit");

				properties.remove(key);
				properties.remove("edit");
				MyParserTag tag = new MyParserTag(type);

				tag.setKeyname(name);
				HashMap params = new HashMap();

				params.put("edit", edit);
				tag.setProperties(params);
				taglist.add(tag);
			}
			if (key.equals("prop") || key.equals("property")) {
				String type = "property";
				String name = (String) prop;

				properties.remove(key);
				MyParserTag tag = new MyParserTag(type);

				tag.setKeyname(name);
				HashMap params = new HashMap();

				params.put("object", properties.get("object"));
				tag.setProperties(params);
				properties.remove("object");
				taglist.add(tag);
			}
			if (key.equals("if")) {
				String type = "if";
				String sif = prop.toString();
				String cond = (String) properties.get("cond");

				if (cond == null) {
					cond = (String) properties.get("condition");
				}
				cond = (cond != null) ? cond : sif;
				properties.remove("if");
				properties.remove("cond");
				properties.remove("condition");
				if (cond == null) {// where can this condition be..
				}
				MyParserTag tag = new MyParserTag(type);
				HashMap params = new HashMap();

				params.put("cond", cond);
				tag.setProperties(params);
				taglist.add(tag);
			}
		}

		// finally we can parse those taglists into a structure

		int key = 0;

		// keylist = array();

		// ltags =& tags;

		// reset(taglist);

		Vector tags = new Vector();
		Vector ltags = tags;
		Vector keylist = new Vector();
		boolean isend = false, closed = false;

		// while (list(k, tag) = each(taglist)) {

		Iterator it = taglist.iterator();

		while (it.hasNext()) {
			MyParserNode tag = (MyParserNode) it.next();
			String name = tag.getTagType();

			closed = inStringArray(name, ctags);
			if (tag instanceof MyParserTag) {
				MyParserTag ptag = (MyParserTag) tag;

				keylist.add(new Integer(key));

				// @todo validate if this works without using key as index

				// ltags.add( new Integer(key), tag );

				ltags.add(tag);
				ltags = ptag.getParts();
				key = 0;
			} else if (name.equals("end")) {
				isend = true;
				closed = true;
				break;
			} else {
				key++;

				// @todo validate if this works without using key as index

				ltags.add(tag);
			}
		}
		if (taglist.size() > 1) {
			isend = true;
		}
		Vector myparts = (closed || !hasparts) ? null : keylist;

		if (tags.size() == 0 && !isend) {
			tags.add(new Exception("Could not build up XNL tag '{0}'."));
			myparts = null;
		}
		return new TagResult(tags, isend, myparts);
	}

	public TagPrefix[] _getTagPrefixList() {
		int prefixlen = this.prefix.length();
		TagPrefix[] tags = new TagPrefix[] {
			new TagPrefix("<" + this.prefix + ">", false, 1 + prefixlen), new TagPrefix("<" + this.prefix + " ", false, 2 + prefixlen),
			new TagPrefix("</" + this.prefix + ">", true, 2 + prefixlen), new TagPrefix("</" + this.prefix + " ", true, 3 + prefixlen) };

		return tags;
	}
}
