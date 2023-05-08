/*
 * @author jan
 * @date Jul 22, 2008
 * @version $Id: CharacterCountTruncator.java,v 1.2 2008-08-20 14:01:15 norbert Exp $
 */
package com.gentics.lib.db;

/**
 * Truncates the strings using the number of characters as definition of length.
 *
 * @deprecated Java 1.5 introduced support for characters outside the BMP by
 *             using utf16 surrogate character pairs. use
 *             SurrogatePairAwareCodePointTruncator
 *             or SurrogatePairAwareCodeUnitTruncator instead.
 * @author jan
 */
@Deprecated
public class CharacterCountTruncator implements StringLengthManipulator {

	public String truncate(String text, int length) {
		if (text == null) {
			return null;
		}
		return text.substring(0, Math.min(length, text.length()));
	}

	public int getLength(String text) {
		if (text == null) {
			return 0;
		}
		return text.length();
	}

}
