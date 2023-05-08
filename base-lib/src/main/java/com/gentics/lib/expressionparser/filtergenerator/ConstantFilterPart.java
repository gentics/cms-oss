/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: ConstantFilterPart.java,v 1.5 2006-08-02 11:33:38 norbert Exp $
 */
package com.gentics.lib.expressionparser.filtergenerator;

import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;

/**
 * Implementation of a constant filter part. This filter part may not contain
 * subparts.
 */
public class ConstantFilterPart extends GenericFilterPart {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -1568341876275966486L;

	/**
	 * Constant statement
	 */
	private StringBuffer statement;

	/**
	 * Constant parameters
	 */
	private List params;

	/**
	 * Create an instance of the constant filter part
	 * @param filter filter
	 * @param statement constant statement
	 * @param params constant parameters
	 */
	public ConstantFilterPart(DatasourceFilter filter, String statement, Object[] params) {
		super(filter);
		this.statement = new StringBuffer(statement);
		this.params = new Vector();
		if (params != null) {
			for (int i = 0; i < params.length; i++) {
				this.params.add(params[i]);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#mergeInto(com.gentics.lib.expressionparser.filtergenerator.MergedFilter)
	 */
	public void mergeInto(MergedFilter mergedFilter) throws ExpressionParserException {
		if (statement != null) {
			mergedFilter.getStatement().append(statement);
		}
		if (params != null) {
			mergedFilter.getParams().addAll(params);
		}
	}

	/**
	 * Append the given ConstantFilterPart to this filter part
	 * @param otherPart filter part to append
	 */
	public void append(ConstantFilterPart otherPart) {
		statement.append(otherPart.statement);
		params.addAll(otherPart.params);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer string = new StringBuffer();

		string.append("constantpart {").append(statement).append("}, params ").append(params);
		return string.toString();
	}
}
