package com.gentics.lib.datasource.mccr.filter;

import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.lib.datasource.mccr.MCCRDatasource;

public class MCCRDatasourceConcatFunction implements Function {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 * com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		StringBuffer output = new StringBuffer();

		for (int i = 0; i < operand.length; ++i) {
			output.append(operand[i].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));
		}
		return output.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 * com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request, FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		try {
			MCCRDatasource mccrDs = (MCCRDatasource) request.getDatasource();

			// get the concat operator the underlying database supports
			String concatOperator = mccrDs.getHandle().getPreferredConcatOperator();

			if (concatOperator != null) {
				// found an operator, surround by blanks
				concatOperator = " " + concatOperator + " ";

				// generate the concat part using the operator
				filterPart.addFilterStatementPart("(");
				for (int i = 0; i < operand.length; i++) {
					// operator only in between two operands
					if (i > 0) {
						filterPart.addFilterStatementPart(concatOperator);
					}
					operand[i].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_STRING);
				}
				filterPart.addFilterStatementPart(")");
			} else {
				// check for concat functions
				String concatFunction = mccrDs.getHandle().getPreferredConcatFunction();

				if (concatFunction != null) {
					// concat function found, so create the part with it
					addConcat(concatFunction, request, filterPart, operand, 0);
				} else {
					// exception: datasource does not support concat
					throw new FilterGeneratorException("Datasource does not support concat()");
				}
			}
		} catch (DatasourceException e) {
			throw new FilterGeneratorException(e);
		}
	}

	/**
	 * Recursive method to add calls to concat() with just two parameters.
	 * 
	 * @param concatFunctionName
	 *            name of the concat function
	 * @param request
	 *            Request
	 * @param filterPart
	 *            filter part
	 * @param operand
	 *            array of operands
	 * @param position
	 *            current position in the array of operands
	 * @throws ExpressionParserException
	 */
	protected void addConcat(String concatFunctionName, ExpressionQueryRequest request, FilterPart filterPart, EvaluableExpression[] operand,
			int position) throws ExpressionParserException {
		// when at least two operands are left, we add concat(operand, [rest])
		if (operand.length - position >= 2) {
			filterPart.addFilterStatementPart("concat(");
			operand[position].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_STRING);
			filterPart.addFilterStatementPart(",");
			addConcat(concatFunctionName, request, filterPart, operand, position + 1);
			filterPart.addFilterStatementPart(")");
		} else if (operand.length - position == 1) {
			operand[position].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_STRING);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return Function.UNBOUNDED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "concat";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class<?>[] getSupportedDatasourceClasses() {
		return MCCRDatasourceFilter.MCCRFILTER_FUNCTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#getType()
	 */
	public int[] getTypes() {
		return NAMEDFUNCTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.expressionparser.functions.Function#supportStaticEvaluation()
	 */
	public boolean supportStaticEvaluation() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_STRING;
	}
}
