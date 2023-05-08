/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: NestedFilterPart.java,v 1.9 2007-01-16 12:05:41 clemens Exp $
 */
package com.gentics.lib.expressionparser.filtergenerator;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.api.lib.expressionparser.functions.Function;

/**
 * Implementation of a {@link FilterPart} that is a container for sub
 * filterparts.
 */
public class NestedFilterPart extends GenericFilterPart {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = 617638452635330554L;

	/**
	 * list of the sub filterparts
	 */
	protected List innerParts = new Vector();

	/**
	 * Create an instance of the filterpart
	 * @param filter datasource filter
	 */
	public NestedFilterPart(DatasourceFilter filter) {
		super(filter);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addFilterPartGenerator(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter.FilterPartGenerator)
	 */
	public void addFilterPartGenerator(FilterPartGenerator filterPartGenerator) throws ExpressionParserException {
		innerParts.add(filterPartGenerator);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addFilterStatementPart(java.lang.String,
	 *      java.lang.Object[])
	 */
	public void addFilterStatementPart(String statementPart, Object[] parameters) throws ExpressionParserException {
		addFilterPart(filter.generateConstantFilterPart(statementPart, parameters));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addFilterStatementPart(java.lang.String)
	 */
	public void addFilterStatementPart(String statementPart) throws ExpressionParserException {
		addFilterPart(filter.generateConstantFilterPart(statementPart, null));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addFunctionToEvaluate(com.gentics.lib.expressionparser.functions.Function,
	 *      int, com.gentics.lib.expressionparser.EvaluableExpression[])
	 */
	public void addFunctionToEvaluate(Function function, int type,
			EvaluableExpression[] operands, int expectedValueType) throws ExpressionParserException {
		addFilterPart(new FunctionEvaluatorFilterPart(filter, function, type, operands, expectedValueType));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addLiteral(java.lang.Object)
	 */
	public void addLiteral(Object literal, int expectedValueType) throws ExpressionParserException {
		addFilterPart(filter.generateLiteralFilterPart(literal, expectedValueType));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addResolvableObject(java.lang.String,
	 *      int)
	 */
	public void addResolvableObject(String expressionName, int expectedValueType) throws ExpressionParserException {
		addFilterPart(new ResolveObjectFilterPart(filter, expressionName, expectedValueType));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#addVariable(java.lang.String)
	 */
	public void addVariable(String expressionName, int expectedValueType) throws ExpressionParserException {
		addFilterPart(filter.generateVariableFilterPart(expressionName, expectedValueType));
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.GenericFilterPart#mergeInto(com.gentics.lib.expressionparser.filtergenerator.MergedFilter)
	 */
	public void mergeInto(MergedFilter mergedFilter) throws ExpressionParserException {
		// merge all innerparts into the merged filter
		for (Iterator iter = innerParts.iterator(); iter.hasNext();) {
			Object element = (Object) iter.next();

			if (element instanceof FilterPart) {
				// forward the call ot the subpart
				((FilterPart) element).mergeInto(mergedFilter);
			} else if (element instanceof FilterPartGenerator) {
				// let the generator generate the filterpart and merge it into
				// the mergedFilter
				((FilterPartGenerator) element).getFilterPart(mergedFilter.getRequest()).mergeInto(mergedFilter);
			}
		}
	}

	/**
	 * Add the given filterpart to the list of filterparts
	 * @param filterPart filter part to add
	 */
	protected void addFilterPart(FilterPart filterPart) {
		// do optimization here. when the last filterpart is an instance of
		// ConstantFilterPart and the new one also, we just merge the two
		if (filterPart instanceof ConstantFilterPart && innerParts.size() > 0 && innerParts.get(innerParts.size() - 1) instanceof ConstantFilterPart) {
			// append the new filterpart to the old one
			ConstantFilterPart oldFilterPart = (ConstantFilterPart) innerParts.get(innerParts.size() - 1);
			ConstantFilterPart newFilterPart = (ConstantFilterPart) filterPart;

			oldFilterPart.append(newFilterPart);
		} else {
			// the filterparts cannot be optimized, so just add the new
			// filterpart to the list
			innerParts.add(filterPart);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return innerParts.toString();
	}
}
