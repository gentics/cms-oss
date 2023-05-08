/*
 * @author norbert
 * @date 18.07.2006
 * @version $Id: AssignmentException.java,v 1.3 2006-08-02 11:33:37 norbert Exp $
 */
package com.gentics.lib.expressionparser;

import com.gentics.api.lib.expressionparser.ExpressionParserException;

/**
 * Exception that might be thrown during assignments
 */
public class AssignmentException extends ExpressionParserException {

	/**
	 * static first part of every error message
	 */
	private final static String ERRORMESSAGE = "Error while performing an assignment for expression";

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -9204706525828897242L;

	/**
	 * Create an instance of the exception
	 */
	public AssignmentException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message
	 * @param message exception message
	 */
	public AssignmentException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause exception cause
	 */
	public AssignmentException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception with a message and a cause
	 * @param message exception message
	 * @param cause exception cause
	 */
	public AssignmentException(String message, Throwable cause) {
		super(message, cause);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.ExpressionParserException#getGeneralExceptionMessagePrefix()
	 */
	protected String getGeneralExceptionMessagePrefix() {
		return ERRORMESSAGE;
	}
}
