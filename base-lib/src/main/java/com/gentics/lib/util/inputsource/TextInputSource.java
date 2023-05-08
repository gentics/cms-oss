/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:08
 * @version $Id: TextInputSource.java,v 1.4 2006-08-10 09:58:43 stefan Exp $
 */
package com.gentics.lib.util.inputsource;

import java.io.Reader;

/**
 * An interface for text sources which provide lines which are splitted into fields.
 * TODO better name .. ;)
 */
public interface TextInputSource {

	/**
	 * Start reading from an Text Input Stream.
	 * @param input the stream to parse
	 */
	void startInput(Reader input);

	/**
	 * fetch the next entry from the input stream.
	 * @return an array of all fields of the read dataset.
	 */
	public String[] readLine();
}
