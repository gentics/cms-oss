/*
 * @author Stefan Hepp
 * @date 27.04.2005 16:26
 * @version $Id: ContentImportError.java,v 1.1 2010-02-03 09:32:50 norbert Exp $
 */
package com.gentics.lib.content.contentimport;

/**
 * A container to store parse errors. The error levels are 
 * defined in {@link ContentImportLogger}.
 */
public class ContentImportError {
	private int level;

	private String attribute;

	private int line;

	private String message;

	/**
	 * create a new error.
	 * @see ContentImportLogger
	 * @param level the severity level of the error.
	 * @param attribute the attribute which caused the error, or an empty string if not applicable.
	 * @param line the line number of the parsed file, in which the error occured.
	 * @param message the error message.
	 */
	public ContentImportError(int level, String attribute, int line, String message) {
		super();
		this.level = level;
		this.attribute = attribute;
		this.line = line;
		this.message = message;
	}

	/**
	 * @return the attribute which caused the error, or an empty string if not applicable.
	 */
	public String getAttribute() {
		return attribute;
	}

	/**
	 * @return the line number of the parsed file, in which the error occured.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return the error message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the severity error.
	 */
	public int getLevel() {
		return level;
	}
}
