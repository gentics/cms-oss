package com.gentics.lib.parser.tag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: wpsadmin Date: 27.05.2003 Time: 17:03:04 To
 * change this template use Options | File Templates.
 *
 * @deprecated use new tagparser
 */

public class MyTagParser {

	int bracket;

	int lt;

	boolean string;

	Vector parts;

	boolean isClosed;

	String lastTag;

	String lastPart;

	boolean simpleTags;

	public MyTagParser(boolean simpleTags) {

		this.simpleTags = simpleTags;

		this.startTag();

	}

	public void startTag() {

		this.bracket = 0;

		this.string = false;

		this.lt = 0;

		this.parts = new Vector();

		this.isClosed = false;

		this.lastPart = new String(new StringBuffer(200));

		this.lastTag = "";

	}

	public int getTagEnd(String code, int start, boolean addTags) {

		return this._parseTag(code, start, addTags);

	}

	public int getTagEnd(String code) {

		return getTagEnd(code, 0, false);

	}

	private int _parseTag(String code, int start, boolean addTags) {

		if (this.simpleTags) {

			return this._parseSimpleTag(code, start, addTags);

		} else {

			return this._parseCharTag(code, start, addTags);

		}

	}

	private int _parseSimpleTag(String code, int start, boolean addTags) {

		int pos = code.indexOf(">", start);

		if (addTags) {

			this.lastPart += (pos == -1) ? code.substring(start) : code.substring(start, pos);

			if (pos != -1) {

				this._addPart(this.lastPart);

				this.lastPart = "";

			}

		}

		return pos;

	}

	private int _parseCharTag(String code, int start, boolean addTags) {

		int length = code.length();

		for (int i = start; i < length; i++) {

			String hack = null;

			char ch = code.charAt(i);

			switch (ch) {

			case '\\':

				i++;

				ch = code.charAt(i);

				break;

			case '(':

				if (!this.string) {

					this.bracket++;
				}

				break;

			case ')':

				if (!this.string) { // if braket == 0 -. error....

					this.bracket--;
				}

				break;

			case '<':

				if (!this.string && this.bracket == 0) {

					this.lt++;
				}

				break;

			case '\"':

				this.string = !this.string;

				// performance boost:

				if (this.string) {

					int next = code.indexOf("\"", i);

					if (next == -1) {

						ch = code.charAt(i);

						i = code.length();

					} else if (next > i + 2) {

						hack = code.substring(i, next - 1);

						i = next - 1;

					}

				}

				break;

			case '/':

				if (!this._inString()) {

					this.isClosed = true;

					ch = '\0';

				}

				break;

			case ' ':

				if (addTags && !this._inString() && this.lastPart.length() > 0) {

					this._addPart(this.lastPart);

					this.lastPart = "";

					ch = '\0';

				}

				break;

			case '>':

				if (!this.string) {

					if (this.lt > 0) {

						this.lt--;

					} else if (this.bracket == 0) {

						if (addTags && this.lastPart.length() > 0) {

							this._addPart(this.lastPart);

						}

						return i;

					}

				}

				break;

			}

			if (ch != '\0' && ch != ' ') {
				this.isClosed = false;
			}

			// add last char to lastpart

			if (addTags && ch != '\0') {
				this.lastPart += ch;
			}

			if (hack != null) {

				this.lastPart += hack;

				hack = null;

			}

		}

		if (addTags && this.lastPart.length() > 0 && !this._inString()) {

			this._addPart(this.lastPart);

			this.lastPart = "";

		}

		return -1;

	}

	private boolean _inString() {

		return (this.string || this.lt > 0 || this.bracket > 0);

	}

	public int parseTag(String code) {

		return this._parseTag(code, 0, true);

	}

	public boolean isClosed() {

		return this.isClosed;

	}

	public HashMap getHashedParts() {

		HashMap ret = new HashMap();

		Iterator i = this.parts.iterator();

		while (i.hasNext()) {

			Object[] o = (Object[]) i.next();

			// put o[0] = key / o[1] = value

			// TODO verify if we really don't need toLowerCase (performance!);
			// should have been done by MyGenericParser::parse()
			// ret.put( ((String) o[0]).toLowerCase(), o[1] );
			ret.put(((String) o[0]), o[1]);

		}

		return ret;

	}

	public Vector getParts() {

		return this.parts;

	}

	public void _addPart(String part) {

		int pos = part.indexOf('=');

		// we also split by '('

		int pos2 = part.indexOf('(');

		if (pos2 == 0 && this.parts.size() > 0) {

			// o[0]: key / o[1]: val

			Object[] o = (Object[]) this.parts.get((this.parts.size()) - 1);

			if (o[1] instanceof Boolean) {

				o[1] = part;

				return;

			}

		}

		String key = "", val = "";

		if (pos2 > 0 && (pos == -1 || pos2 < pos)) {

			pos = pos2;

			key = part.substring(0, pos);

			val = part.substring(pos);

		} else if (pos != -1) {

			key = part.substring(0, pos);

			val = part.substring(pos + 1);

		}

		if (pos == -1) {

			this.parts.add(new Object[] { part, new Boolean(true) });

		} else if (val.length() > 0) {

			if (val.charAt(0) == '\"' && val.charAt(val.length() - 1) == '\"') {

				val = val.substring(1, val.length() - 2);

			}

			this.parts.add(new Object[] { key, val });

		}

	}
}
