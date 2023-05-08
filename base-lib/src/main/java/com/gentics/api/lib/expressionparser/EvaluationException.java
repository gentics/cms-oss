/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: EvaluationException.java,v 1.1 2006-08-02 11:33:36 norbert Exp $
 */
package com.gentics.api.lib.expressionparser;

/**
 * Exception that is thrown whenever the evaluation of any
 * {@link com.gentics.api.lib.expressionparser.EvaluableExpression} fails.
 */
public class EvaluationException extends ExpressionParserException {

	/**
	 * static first part of every error message
	 */
	private final static String ERRORMESSAGE = "Error while evaluating expression";

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -8741256741806675444L;

	/**
	 * Create empty exception
	 */
	public EvaluationException() {
		super();
	}

	/**
	 * Create exception with a message
	 * @param message exception message
	 */
	public EvaluationException(String message) {
		super(message);
	}

	/**
	 * Create exception with a cause
	 * @param cause exception cause
	 */
	public EvaluationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create exception with message and cause
	 * @param message exception message
	 * @param cause exception cause
	 */
	public EvaluationException(String message, Throwable cause) {
		super(message, cause);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.ExpressionParserException#getGeneralExceptionMessagePrefix()
	 */
	protected String getGeneralExceptionMessagePrefix() {
		return ERRORMESSAGE;
	}
}
