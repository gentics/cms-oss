package com.gentics.lib.db;

/**
 * Support for truncating Java Strings that may contain UTF-16 surrogate pairs.
 * Unicode code points are counted. If all surrogate characters are paired
 * correctly, this property is maintained using this class.
 *
 * @author escitalopram
 *
 */
public class SurrogatePairAwareCodePointTruncator implements StringLengthManipulator {

	public String truncate(String text, int length) {
		if (text == null) {
			return null;
		}
		if (getLength(text) <= length) {
			return text;
		}
		return text.substring(0, text.offsetByCodePoints(0, length));
	}

	public int getLength(String text) {
		if (text == null) {
			return 0;
		}
		return text.codePointCount(0, text.length());
	}

}
