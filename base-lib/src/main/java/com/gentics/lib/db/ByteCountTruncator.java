/*
 * @author jan
 * @date Jul 22, 2008
 * @version $Id: ByteCountTruncator.java,v 1.2 2008-08-20 14:01:15 norbert Exp $
 */
package com.gentics.lib.db;

import java.io.UnsupportedEncodingException;

/**
 * Truncates the strings using the number of bytes as definition of length.
 * @author jan
 *
 */
public class ByteCountTruncator implements StringLengthManipulator {

	public String truncate(String text, int length) {
		if (text == null) {
			return null;
		}
		while (getLength(text) > length) {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}
    
	public int getLength(String text) {
		if (text == null) {
			return 0;
		}
		try {
			return text.getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			return text.getBytes().length;
		}
	}

}
