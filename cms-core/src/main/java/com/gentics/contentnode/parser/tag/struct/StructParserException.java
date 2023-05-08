/*
 * @author Stefan Hepp
 * @date 15.12.2006
 * @version $Id: StructParserException.java,v 1.2 2006-02-05 14:24:21 stefan Exp $
 */
package com.gentics.contentnode.parser.tag.struct;

/**
 * This is a special exception for StructParser, which can be thrown on
 * parsing errors, which also contains a {@link RenderReturnCode}.
 */
public class StructParserException extends Exception {

	private RenderReturnCode retCode;

	/**
	 * Construct a new parser exception with a message and a returncode.
	 *
	 * TODO i18n of message?
	 *
	 * @param message the error message.
	 * @param retCode the returncode container.
	 */
	public StructParserException(String message, RenderReturnCode retCode) {
		super(message);
		this.retCode = retCode;
	}

	/**
	 * get the returncode container of the exception.
	 * @return the returncode container.
	 */
	public RenderReturnCode getReturnCode() {
		return retCode;
	}
}
