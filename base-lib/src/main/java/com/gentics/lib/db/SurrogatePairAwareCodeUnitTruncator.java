package com.gentics.lib.db;

/**
 * Support for truncating Java Strings that may contain UTF-16 surrogate pairs.
 * UTF16 code units are counted. If all surrogate characters are paired
 * correctly, this property is maintained using this class.
 *
 * @author escitalopram
 *
 */
public class SurrogatePairAwareCodeUnitTruncator implements StringLengthManipulator {

	public String truncate(String text, int length) {
		if (text == null) {
			return null;
		}
		if (getLength(text) <= length) {
			return text;
		}
		if (length == 0) {
			return "";
		}
		char endCharacter = text.charAt(length - 1);
		if (Character.isHighSurrogate(endCharacter)) {
			return text.substring(0, length - 1);
		} else {
			return text.substring(0, length);
		}
	}

	public int getLength(String text) {
		if (text == null) {
			return 0;
		}
		return text.length();
	}

}
