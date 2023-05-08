package com.gentics.lib.parser.tag;

import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.ComponentsConstants;

/**
 * Created by IntelliJ IDEA. <p/>User: wpsadmin <p/>Date: 27.05.2003 <p/>
 * Time: 14:44:00 <p/>To change this template use Options | File Templates.
 * @deprecated use new tagparser
 */
public abstract class MyGenericParser {

	public class TagPrefix {
		public String tag = null;

		public boolean isend = false;

		public int taglen = 0;

		public TagPrefix(String tag, boolean isend, int taglen) {
			this.tag = tag;
			this.isend = isend;
			this.taglen = taglen;
		}
	}

	class TagPos {
		public int pos;

		public int endpos;

		public TagPrefix tag;

		public TagPos(int pos, int endpos, TagPrefix tag) {
			this.pos = pos;
			this.endpos = endpos;
			this.tag = tag;
		}
	}

	public class TagResult {
		Vector tags;

		Vector parts;

		boolean isend;

		public TagResult(Vector tags, boolean isend, Vector parts) {
			this.tags = tags;
			this.isend = isend;
			this.parts = parts;
		}
	}

	class ObjectWrapper {
		public Object d_o;

		public ObjectWrapper() {}

		public ObjectWrapper(Object o) {
			d_o = o;
		}
	}

	private class SCode {
		private StringBuffer code = new StringBuffer();

		private String name = "";

		private Map props = new HashMap();

		public void addCode(String code) {
			this.code.append(code);
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setProperties(Map props) {
			this.props = props;
		}

		public StringBuffer getCode() {
			return code;
		}

		public String getName() {
			return name;
		}

		public Map getProperties() {
			return props;
		}
	}

	boolean d_showErrors = false;

	boolean d_evaluate = true;

	HashMap p_parser = new HashMap();

	HashMap p_alias = new HashMap();

	HashMap d_tagPosHash = new HashMap();

	boolean d_parseTagResult = false;

	boolean d_parseTagParams = false;

	String d_parserkeyname;

	String d_errorPrefix;

	boolean d_useSimpleTags = false;

	boolean d_writeDependencies = true;

	public void setShowErrors(boolean val) {
		d_showErrors = val;
	}

	public boolean doShowErrors() {
		return d_showErrors;
	}

	public void setEvaluate(boolean value) {
		this.d_evaluate = value;
	}

	public void setWriteDependencies(boolean value) {
		this.d_writeDependencies = value;
	}

	public boolean doWriteDependencies() {
		return this.d_writeDependencies;
	}

	public boolean doEvaluate() {
		return this.d_evaluate;
	}

	public void setParseTagResult(boolean value) {
		this.d_parseTagResult = value;
	}

	public boolean doParseTagResult() {
		return this.d_parseTagResult;
	}

	public void setParseTagParams(boolean value) {
		this.d_parseTagParams = value;
	}

	public boolean doParseTagParams() {
		return this.d_parseTagParams;
	}

	public void setUseSimpleTags(boolean value) {
		this.d_useSimpleTags = value;
	}

	public boolean doUseSimpleTags() {
		return this.d_useSimpleTags;
	}

	public MyObjectParser getObjectParser(String type) {
		String key = this._getParserKey(type);

		// to avoid stacked use of object parser, return fresh instance every
		// time
		// TODO use pool or factory instead of quite ugly getInstance()
		if (key != null) {
			MyObjectParser parser = null;

			try {
				parser = (MyObjectParser) ((MyObjectParser) this.p_parser.get(key)).clone();
				return parser;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void setObjectParser(String type, MyObjectParser parser) {
		String key = this._getParserKey(type);

		if (key == null) {
			key = type;
		}
		this._initObjectParser(parser);
		parser.setType(key);
		this.p_parser.put(key, parser);
	}

	public void addObjectParser(String type, MyObjectParser parser, String[] alias) {
		this._initObjectParser(parser);
		parser.setType(type);
		this.p_parser.put(type, parser);
		for (int i = 0; i < alias.length; i++) {
			p_alias.put(alias[i], type);
		}
	}

	public void addObjectParser(String type, MyObjectParser parser) {
		this.addObjectParser(type, parser, new String[] {});
	}

	public void unsetObjectParser(String type) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public String parse(String code) {
		return parse(code, null).toString();
	}

	private StringBuffer parse(String code, MyParserTag parentTag) {
		if (code == null) {
			NodeLogger.getLogger(getClass()).warn("Parameter code for MyGenericParser::parse is null");
			return new StringBuffer("");
		}

		TIntObjectHashMap tagHash = null;

		RuntimeProfiler.beginMark(ComponentsConstants.XNLPARSER_FINDTAGS);
		try {
			// tagHash was sorted!
			tagHash = this._buildTagPosHash(code, 0);
			if (tagHash.size() == 0) {
				return new StringBuffer(code);
			}
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.XNLPARSER_FINDTAGS);
		}

		Vector parts = null;

		RuntimeProfiler.beginMark(ComponentsConstants.XNLPARSER_BUILDSTRUCTURE);
		try {
			// go out hunting tags ..
			parts = this._buildStructure(code, tagHash, null, null, parentTag);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.XNLPARSER_BUILDSTRUCTURE);
		}

		Vector rs = null;

		RuntimeProfiler.beginMark(ComponentsConstants.XNLPARSER_PARSESTRUCTURE);
		try {
			// and build up result code
			rs = this._parseStructure(parts, parentTag);
		} finally {
			RuntimeProfiler.endMark(ComponentsConstants.XNLPARSER_PARSESTRUCTURE);
		}
		return ((SCode) rs.get(0)).getCode();
	}

	public Vector _buildStructure(String code, TIntObjectHashMap tagHash, ObjectWrapper o_pos,
			String intag, MyParserTag parentTag) {
		boolean isend = false;
		Vector parts = new Vector();
		MyTagParser tagparser = new MyTagParser(this.d_useSimpleTags);

		// String tagname = (intag != null) ? intag : "";

		TagPos nextTag;
		int pos = (o_pos != null) ? ((Integer) o_pos.d_o).intValue() : 0;

		if (o_pos == null) {
			o_pos = new ObjectWrapper(new Integer(0));
		}
		String pcode;
		TagResult tagResult;
		int[] sortedHashKeys = tagHash.keys();

		Arrays.sort(sortedHashKeys);
		while ((nextTag = this._getNextTag(sortedHashKeys, tagHash, pos)) != null) {
			isend = nextTag.tag.isend;

			// first of all, add code before tag to parts

			if (nextTag.pos > pos) {
				pcode = code.substring(pos, nextTag.pos);
				parts.add(pcode);
				pos = nextTag.pos;
				o_pos.d_o = new Integer(pos);
			}

			// get end of tag

			tagparser.startTag();
			int tagend = tagparser.getTagEnd(code, nextTag.endpos, true);

			if (tagend == -1) {
				parts.add(new Exception("Could not find closing '>' for <b>" + intag + "</b>."));
				parts.add(code.substring(pos));
				o_pos.d_o = new Integer(code.length());
				return parts;
			}

			// parse content of tag, so we strip off all tagResult.. and only
			// for opening tagResult

			// itagpos = ((Integer)tagpos.d_o).intValue();

			pcode = code.substring(nextTag.endpos, tagend);
			boolean hasparts = false;

			if (pcode.length() > 0 && !nextTag.tag.isend) {
				// now we can parse all params and check, if we need some sort
				// of end-tag

				if (this.d_parseTagParams) {
					TIntObjectHashMap partHash = this._buildPartTagPosHash(tagHash, tagend, nextTag.endpos);

					if (partHash.size() > 0) {
						int _pos = 0;
						Vector struct = this._buildStructure(pcode, partHash, new ObjectWrapper(new Integer(_pos)), null, parentTag);
						Vector rs = this._parseStructure(struct, parentTag);

						pcode = ((SCode) rs.get(0)).getCode().toString();
						tagparser.startTag();
						tagparser.parseTag(pcode);
					}
				}
				Map params = tagparser.getHashedParts();

				hasparts = !tagparser.isClosed();

				// generate tagResult out of this stuff and add them..

				tagResult = this._generateTags(params, hasparts, pcode);
				// hasparts equals hasClosingTag() in CNTagParser?
				hasparts = tagResult.parts != null;
				isend = nextTag.tag.isend || tagResult.isend;
			} else {
				tagResult = new TagResult(new Vector(), isend, null);
			}
			pos = tagend + 1;
			o_pos.d_o = new Integer(pos);

			// we finally parse till the next end tag for this struct, set pos
			// after end tag

			if (hasparts && !isend) {
				Vector props = this._buildStructure(code, tagHash, o_pos, pcode, parentTag);

				pos = ((Integer) o_pos.d_o).intValue();
				this._addParts(tagResult.tags, props, tagResult.parts);
			}
			parts.addAll(tagResult.tags);

			// and check for end tagResult

			if (intag == null && tagResult.isend) {
				// whoo, end not within tag..

				parts.add(new Exception("Found end tag without begin tag"));
			} else if (intag != null && tagResult.isend) {
				// we are at the end of this tag, cursor is placed after this
				// tag, now return without end tag

				return parts;
			}
		}
		if (intag != null) {
			// whoops, here we got a tag without end tag..

			parts.add(new Exception("Could not find closing tag for <b>" + intag + "</b>."));
		} else if (pos < code.length()) {
			// get rest of code and add it

			pcode = code.substring(pos);
			parts.add(pcode);
			o_pos.d_o = new Integer(code.length());
		}
		return parts;
	}

	public Vector _parseStructure(Vector parts, MyParserTag parentTag) {
		int ckey = 0;
		Vector vscode = new Vector();
		SCode scode = new SCode();
		Iterator partIterator = parts.iterator();

		vscode.add(scode);

		// while (list(key,part) = each(parts)) {

		while (partIterator.hasNext()) {
			SCode ckeyScode = (SCode) vscode.get(ckey);
			Object part = partIterator.next();

			// is_a is somehow slow, so check for most likely tags first.. maybe
			// some caching for is_a would be nice..

			if (part instanceof String) {
				ckeyScode.addCode(this._handleCode(part));
			} else if (part instanceof MyParserTag) {
				MyParserTag ptPart = (MyParserTag) part;

				// get parts of this tag and parse it
				Vector tagparts = ptPart.getParts();

				/*
				 * @todo for if-condition, we do not need to parse the complete
				 * content, do some checking. *
				 */
				// TODO check, if tag wants it's structure parsed!!!!!!!!!
				String pcode;
				Vector tags;

				if (tagparts.size() > 0) {
					tags = this._parseStructure(tagparts, (MyParserTag) part);
					pcode = ((SCode) tags.get(0)).getCode().toString();
				} else {
					tags = new Vector();
					pcode = "";
				}

				// if this part is a tag, get a parser for this part and
				// initialize it
				String tagType = ptPart.getTagType();
				MyObjectParser obj = this.getObjectParser(tagType);

				obj.setParentTag((MyParserTag) part);
				obj.setKeyname(ptPart.getKeyname());

				// while (list(k,v) = each(tags)) {

				for (int i = 1; i < tags.size(); i++) {
					SCode t = (SCode) tags.get(i);

					obj.addCodePart(t.getName(), t.getCode().toString(), t.getProperties());
				}
				if (obj.keyExists()) {
					// do the parsing and handle result
					Object res = obj.parse(pcode, ptPart.getProperties());
					Vector rs;

					if (!(res instanceof Vector)) {
						rs = new Vector();
						rs.add(res);
					} else {
						rs = (Vector) res;
					}
					// if (!is_array(rs)) rs = array(rs);

					if (parentTag != null) {
						parentTag.setResult(rs);
					}
					// StringBuffer _code = new StringBuffer( 1000 );
					this._generateCode(ckeyScode.code, rs);

					/*
					 * @todo config depth by d_parseTagResult instead of magic
					 * number *
					 */
					if (this.d_parseTagResult) {
						String _code = this.parse(ckeyScode.code.toString());

						ckeyScode.code = new StringBuffer(_code);
					}
					// ckeyScode.addCode(_code);
				} else {// TODO
					// System.out.println( "ERROR: OBJ.keyExists() failed for
					// key: " + obj.getKeyname() );
					// System.out.println(I18n.s("Error: Key '{0}' for '{1}'
					// does not exist.", new String[]{obj.getKeyname(),
					// obj.getType()}));
					/*
					 * rs = new Exception(i18n("Key '{0}' for '{1}' does not
					 * exist.",0,part.getKeyname(), part.getTagType()) );
					 * scode[ckey][code] += this._generateCode(array(rs));
					 */}
			} else if (part instanceof MyParserNode) {
				// got some splitter, create new part

				MyParserNode pspart = (MyParserNode) part;

				ckey++;
				ckeyScode = new SCode();
				vscode.add(ckeyScode);
				ckeyScode.setName(pspart.getTagType());
				ckeyScode.setProperties(pspart.getProperties());
			} else {
				// got some kinda string
				Vector param = new Vector();

				param.add(part);
				this._generateCode(ckeyScode.code, param);
			}
		}
		return vscode;
	}

	public String _getParserKey(String alias) {
		// TODO: removed toLowerCase() because of performance tuning; verify!
		// String key = alias.toLowerCase();
		String key = alias;
		MyObjectParser ret = (MyObjectParser) p_parser.get(key);

		if (ret != null) {
			return key;
		}
		return (String) p_alias.get(key);
	}

	public boolean _addParts(Vector tags, Vector parts, Vector partkeys) {
		Vector t = tags;
		int i = 0;

		/*
		 * for ( ; i < partkeys.size() - 1; i++ ) t = (Vector) parts.get(
		 * ((Integer)partkeys.get(i)).intValue() ); Object o = t.get(
		 * ((Integer)partkeys.get(i)).intValue() );
		 */

		Object o = null;

		if (tags.size() > 0) {
			o = tags.get(tags.size() - 1);
		}
		if (o instanceof MyParserTag) {
			MyParserTag pt = (MyParserTag) o;

			pt.setParts(parts);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * _getNextTag returns start position of next tag or /tag. <p/>this public
	 * void does not check for xnl end or xnl / tags to set endtag and expects
	 * <p/>code in lowercase format for performance reasons..
	 * @param pos int position to start search from
	 * @return mixed position of next tag, false if not found
	 */
	private TagPos _getNextTag(int[] sortedHashKeys, TIntObjectHashMap tagHash, int pos) {
		int[] ret = new int[3];

		// while (list(_pos, val) = each(tagHash)) {
		for (int i = 0; i < sortedHashKeys.length; i++) {
			int _pos = sortedHashKeys[i];

			if (_pos < pos) {
				continue;
			}
			TagPrefix p = (TagPrefix) tagHash.get(_pos);

			/*
			 * endtag* / ret[2] = p.isend ? 1 : 0; /*tagpos* / ret[1] = _pos+
			 * p.taglen; ret[0] = _pos;
			 */
			return new TagPos(_pos, _pos + p.taglen, p);
		}
		return null;
	}

	// pos default: 0

	public TIntObjectHashMap _buildTagPosHash(String lcode, int pos) {
		TIntObjectHashMap hash = new TIntObjectHashMap();
		TagPrefix[] tags = this._getTagPrefixList();

		// while (list(tag,val) = each(tags)) {

		for (int i = 0; i < tags.length; i++) {
			int _pos = pos;
			int l = tags[i].tag.length();
			int p;

			while ((p = lcode.indexOf(tags[i].tag, _pos)) != -1) {
				hash.put(p, tags[i]);
				_pos = p + l;
			}
		}
		return hash;
	}

	public TIntObjectHashMap _buildPartTagPosHash(TIntObjectHashMap tagHash, int toPos,
			int offset) {
		TIntObjectHashMap ret = new TIntObjectHashMap();
		int[] keys = tagHash.keys();

		for (int i = 0; i < keys.length; i++) {
			// while (list(pos,val) = each(tagHash)) {

			int pos = keys[i];

			if (pos < toPos && pos >= offset) {
				ret.put(pos - offset, tagHash.get(pos));
			} else if (pos >= toPos) {
				return ret;
			}
		}
		return ret;
	}

	public abstract String getParserKey();

	public void _initObjectParser(MyObjectParser parser) {
		parser.setParser(this);
	}

	public void _generateCode(StringBuffer code, Vector tags) {
		// StringBuffer code = new StringBuffer( 1000 );
		Iterator i = tags.iterator();

		while (i.hasNext()) {
			Object tag = i.next();

			if (tag == null) {
				code.append(this._handleMessage(new Exception("_generateCode: tag is null")));
			} else if (tag instanceof Exception) {
				code.append(this._handleMessage(tag));
			} else {
				this._handleObject(code, tag);
			}
		}
		// return code.toString();
	}

	public abstract TagResult _generateTags(Map params, boolean hasParts, String code);

	public abstract TagPrefix[] _getTagPrefixList();

	public String _handleCode(Object tag) {
		return tag.toString();
	}

	public String getErrorPrefix() {
		return this.d_errorPrefix;
	}

	public void setErrorPrefix(String prefix) {
		this.d_errorPrefix = prefix;
	}

	public String _handleMessage(Object tag) {
		String code = "";

		if (this.d_showErrors) {
			NodeLogger.getLogger(getClass()).error("EXCEPTION in MyGenericParser: " + tag);

			/*
			 * message = i18n(get_class(tag)).":
			 * ".this.d_errorPrefix.tag.getMessage(); alert = new
			 * AlertStreamInfo(); info = new InfoStreamInfo(); if ( is_a( tag,
			 * "parsermessage" ) ) info.addMessage(0, ICO_INFO, message); else
			 * alert.addMessage( 0, ICO_ALERT, message);
			 */
		}
		return code;
	}

	public void _handleObject(StringBuffer code, Object tag) {
		code.append(tag.toString());
	}
}
