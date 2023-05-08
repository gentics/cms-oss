/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: MergedFilter.java,v 1.2 2006-08-23 15:32:18 norbert Exp $
 */
package com.gentics.api.lib.expressionparser.filtergenerator;

import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;

/**
 * Complete and ready to use filter. As a final step in execution of a
 * datasource filter all previously generated {@link FilterPart}s are merged
 * into an instance of this class.
 */
public class MergedFilter {

	/**
	 * internal stringbuffer for the generated final statement
	 */
	private StringBuffer statement;

	/**
	 * internal list for all parameters
	 */
	private List params;

	/**
	 * expression request
	 */
	private ExpressionQueryRequest request;

	/**
	 * Create an instance of the merged filter
	 * @param request expression request
	 */
	public MergedFilter(ExpressionQueryRequest request) {
		statement = new StringBuffer();
		params = new Vector();
		this.request = request;
	}

	/**
	 * Get the parameters list
	 * @return parameters list
	 */
	public List getParams() {
		return params;
	}

	/**
	 * Get the statement stringbuffer
	 * @return statement stringbuffer
	 */
	public StringBuffer getStatement() {
		return statement;
	}

	/**
	 * Get the Expression request
	 * @return expression request
	 */
	public ExpressionQueryRequest getRequest() {
		return request;
	}
}
