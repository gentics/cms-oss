package com.gentics.lib.util.text;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) <p/>Date: 10.12.2003
 */
public class TextFormatter {
	public static String cleannl(String str) {
		return str.replaceAll("\r\n", "\n").replaceAll("\n\r", "\n").replaceAll("\r", "\n");
	}

	private static String textnl2br_regex = null;

	private static String getTextNl2BrRegex() {
		if (textnl2br_regex != null) {
			return textnl2br_regex;
		}
		return null;
	}

	public static String textNl2Br(String str) {
		String TAG_PREFIX = "node";

		// kill all spaces at the end

		String ret = cleannl(str);

		ret = ret.replaceAll(" *\n", "\n");

		// replace all \n after an open tag "<" with \r

		ret = ret.replaceAll("(<[^>]*)\n", "$1\r");

		// list of tags which will be br'd

		ArrayList rep = new ArrayList(6);

		rep.add("b");
		rep.add("i");
		rep.add("font");
		rep.add("div");
		rep.add("span");
		rep.add("br");
		rep.add(TAG_PREFIX);
		String myreg = "";

		for (int i = 0; i < rep.size(); i++) {
			java.lang.String val = (java.lang.String) rep.get(i);

			myreg += "(<" + val + "[^>]*>)|(</" + val + ">)|";
		}
		myreg = myreg.substring(0, myreg.length() - 1);
		ret = ret.replaceAll("/(" + myreg + ")\n/i", "$1<br />\n");

		// br text newlines
		StringBuffer retBuf = new StringBuffer(ret.length());
		StringTokenizer tokenizer = new StringTokenizer(ret, "\n", true);
		boolean canBreak = true;

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();

			if (token.equals("\n")) {
				if (canBreak) {
					retBuf.append("<br />\n");
				} else {
					retBuf.append(token);
				}
			} else {
				retBuf.append(token);
				int opener = token.lastIndexOf('<');

				if (opener >= 0) {
					int closer = token.lastIndexOf('>');

					if (opener < closer) {
						canBreak = true;
					} else {
						canBreak = false;
					}

				} else {
					canBreak = true;
				}
			}
		}
		ret = retBuf.toString();

		ret = cleannl(ret);
		return ret;
	}
}
