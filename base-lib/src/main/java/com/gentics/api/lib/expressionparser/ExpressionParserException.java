/*
 * @author norbert
 * @date 20.07.2006
 * @version $Id: ExpressionParserException.java,v 1.2 2007-04-10 14:37:11 laurin Exp $
 */
package com.gentics.api.lib.expressionparser;

import com.gentics.api.lib.exception.NodeException;

/**
 * Abstract base class for all exceptions that are thrown by the expression
 * parser.
 */
public abstract class ExpressionParserException extends NodeException {

	/**
	 * expression string that caused this exception
	 */
	protected String expressionString;

	/**
	 * Create an instance of the exception
	 */
	public ExpressionParserException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message. The message will be
	 * prepended with the general exception message prefix
	 * @param message exception message
	 */
	public ExpressionParserException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause exception cause
	 */
	public ExpressionParserException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception with a message and cause. The message
	 * will be prepended with the general exception message prefix
	 * @param message exception message
	 * @param cause exception cause
	 */
	public ExpressionParserException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Get the general exception message prefix (dependent on the concrete
	 * exception class)
	 * @return general exception message prefix
	 */
	protected abstract String getGeneralExceptionMessagePrefix();

	/*
	 * (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage() {
		// overwrite this method to prepend the message with the general
		// exception message prefix and the expression string

		// the expression string was given, so put it into the message
		StringBuffer buffer = new StringBuffer();
		String superMessage = super.getMessage();

		buffer.append(getGeneralExceptionMessagePrefix());

		if (expressionString != null) {
			// the expression string was given, so put it into the message
			buffer.append(" {").append(expressionString).append("}");
		}
		if (superMessage != null && superMessage.length() > 0) {
			buffer.append(": ").append(super.getMessage());
		}
		return buffer.toString();
	}

	/**
	 * Get the expression string of the expression causing this exception
	 * @return Returns the expressionString.
	 */
	public String getExpressionString() {
		return expressionString;
	}

	/**
	 * Set the expression string of the expression causing this exception
	 * @param expressionString expressionString
	 */
	public void setExpressionString(String expressionString) {
		this.expressionString = expressionString;
	}
}
