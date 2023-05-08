/*
 * @author norbert
 * @date 28.06.2006
 * @version $Id: FilterGeneratorException.java,v 1.1 2006-08-02 11:33:37 norbert Exp $
 */
package com.gentics.api.lib.expressionparser.filtergenerator;

import com.gentics.api.lib.expressionparser.ExpressionParserException;

/**
 * Exception that might be thrown during the generation of a datasource filter.
 */
public class FilterGeneratorException extends ExpressionParserException {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 4375916296130491922L;

	/**
	 * static first part of every error message
	 */
	private final static String ERRORMESSAGE = "Error while generating filter for class {";

	/**
	 * second part of the error message
	 */
	private final static String ERRORMESSAGE_PART2 = "}, expression";

	/**
	 * name of the datasource class for which the filter should be generated
	 */
	protected String datasourceClassName;

	/**
	 * Create an instance of the exception
	 */
	public FilterGeneratorException() {
		super();
	}

	/**
	 * Create an instance of the exception with a message. The message will be
	 * prepended with<br> "Error while generating filter for expression
	 * {&lt;expression&gt;}: ".
	 * @param message exception message
	 */
	public FilterGeneratorException(String message) {
		super(message);
	}

	/**
	 * Create an instance of the exception with a cause
	 * @param cause exception cause
	 */
	public FilterGeneratorException(Throwable cause) {
		super(cause);
	}

	/**
	 * Create an instance of the exception with a message and cause. The message
	 * will be prepended with<br> "Error while generating filter for
	 * expression {&lt;expression&gt;}: ".
	 * @param message exception message
	 * @param cause exception cause
	 */
	public FilterGeneratorException(String message, Throwable cause) {
		super(message, cause);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.ExpressionParserException#getGeneralExceptionMessagePrefix()
	 */
	protected String getGeneralExceptionMessagePrefix() {
		StringBuffer generalMessage = new StringBuffer();

		generalMessage.append(ERRORMESSAGE);
		generalMessage.append(datasourceClassName);
		generalMessage.append(ERRORMESSAGE_PART2);
		return generalMessage.toString();
	}

	/**
	 * Get the set datasource class name
	 * @return Returns the datasourceClassName.
	 */
	public String getDatasourceClassName() {
		return datasourceClassName;
	}

	/**
	 * Set the datasource class name
	 * @param datasourceClassName The datasourceClassName to set.
	 */
	public void setDatasourceClassName(String datasourceClassName) {
		this.datasourceClassName = datasourceClassName;
	}
}
