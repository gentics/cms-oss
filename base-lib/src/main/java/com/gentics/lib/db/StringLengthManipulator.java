/*
 * @author jan
 * @date Jul 22, 2008
 * @version $Id: StringLengthManipulator.java,v 1.2 2008-08-20 14:01:15 norbert Exp $
 */
package com.gentics.lib.db;

/**
 * Implementations of this interface are meant to be used for truncating text
 * that should be stored in a database.
 * @author jan
 */
public interface StringLengthManipulator {

	/**
	 * Truncate a text to a given length according to database specific
	 * constraints (e.g. encoding etc)
	 * @param text The text which should be truncated.
	 * @param length Desired length of the resulting string. The semantics of
	 *        length are not specified and depend on the database in use, e.g.
	 *        it can be the number of character or the length of the data in
	 *        bytes using some encoding.
	 * @return The truncated with the desired length (according to the semantics
	 *         of length).
	 */
	public String truncate(String text, int length);
    
	/**
	 * Get length of a given String according to the db-specific semantics of length
	 * @param text
	 * @return
	 */
	public int getLength(String text);

}
