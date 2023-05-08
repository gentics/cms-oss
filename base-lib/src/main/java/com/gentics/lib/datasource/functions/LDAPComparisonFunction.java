/*
 * @author norbert
 * @date 03.07.2006
 * @version $Id: LDAPComparisonFunction.java,v 1.6 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.lib.datasource.LDAPDatasource;
import com.gentics.lib.expressionparser.filtergenerator.NestedFilterPart;

/**
 * Implementation for ldap filter generation of comparison functions
 * (==, !=, &lt;, &lt;=, &gt;, &gt;=).
 */
public class LDAPComparisonFunction extends AbstractBinaryLDAPFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_EQUAL, TYPE_SMALLER, TYPE_SMALLEROREQUAL, TYPE_GREATER, TYPE_GREATEROREQUAL, TYPE_UNEQUAL};

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.filtergenerator.FilterPart,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);

		if (functionType == TYPE_EQUAL || functionType == TYPE_UNEQUAL) {
			// take care of possible null values
			generateNullSafeFilterPart(functionType, request, filterPart, operand, expectedValueType);
		} else {
			// comparisons other than == or != are easy to generate
			simpleComparison(functionType, request, filterPart, operand);
		}
	}

	/**
	 * Generate the filter part for "equals" or "unequals"
	 * comparisons. Make sure that also null is treated correctly
	 * @param functionType function type (either TYPE_EQUAL or TYPE_UNEQUAL)
	 * @param request expression request
	 * @param filterPart filter part
	 * @param operand operands
	 * @param expectedValueType expected value type
	 * @throws ExpressionParserException
	 */
	protected void generateNullSafeFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// first check the operands. we expect one to be variable and the other
		// to be either also variable, nonstatic or static
		if (operand[0].isVariable(request.getFilter())) {
			// lhs operand is variable
			if (operand[1].isVariable(request.getFilter())) {
				// all variable (not supported by LDAP)
				if (LDAPDatasource.class.isAssignableFrom(request.getFilter().getDatasourceClass())) {
					throw new FilterGeneratorException("All variable comparison not supported by LDAP");
				}
				simpleComparison(functionType, request, filterPart, operand);
			} else if (operand[1].isStatic(request.getFilter())) {
				// variable with static
				variableWithStaticComparison(functionType, request, filterPart, operand[0], operand[1]);
			} else {
				// variable with non-static
				filterPart.addFilterPartGenerator(new EqualityGenerator(operand[0], operand[1], functionType));
			}
		} else if (operand[0].isStatic(request.getFilter())) {
			if (operand[1].isVariable(request.getFilter())) {
				// static with variable
				variableWithStaticComparison(functionType, request, filterPart, operand[1], operand[0]);
			} else if (operand[1].isStatic(request.getFilter())) {
				// all static
				filterPart.addFunctionToEvaluate(this, functionType, operand, expectedValueType);
			} else {
				// static with nonstatic
				filterPart.addFunctionToEvaluate(this, functionType, operand, expectedValueType);
			}
		} else {
			if (operand[1].isVariable(request.getFilter())) {
				// nonstatic with variable
				filterPart.addFilterPartGenerator(new EqualityGenerator(operand[1], operand[0], functionType));
			} else {
				// nonstatic with static and all nonstatic
				filterPart.addFunctionToEvaluate(this, functionType, operand, expectedValueType);
			}
		}
	}

	protected void variableWithStaticComparison(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression variableOperand,
			EvaluableExpression staticOperand) throws ExpressionParserException {
		try {
			// evaluate the static value
			Object staticValue = staticOperand.evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

			if (staticValue == null) {
				// comparison with null
				if (functionType == TYPE_EQUAL) {
					filterPart.addFilterStatementPart("(!(");
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart("=*))");
				} else if (functionType == TYPE_UNEQUAL) {
					filterPart.addFilterStatementPart("(");
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart("=*)");
				}
			} else {
				if (functionType == TYPE_EQUAL) {
					filterPart.addFilterStatementPart("(");
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart("=");
					filterPart.addLiteral(staticValue, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart(")");
				} else if (functionType == TYPE_UNEQUAL) {
					filterPart.addFilterStatementPart("(!(");
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart("=");
					filterPart.addLiteral(staticValue, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart("))");
				}
			}

		} catch (EvaluationException e) {
			throw new FilterGeneratorException(e);
		}
	}

	protected void simpleComparison(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand) throws ExpressionParserException {
		// rewrite some function types
		boolean includeNot = false;

		switch (functionType) {
		case TYPE_UNEQUAL:
			functionType = TYPE_EQUAL;
			includeNot = true;
			break;

		case TYPE_SMALLER:
			functionType = TYPE_GREATEROREQUAL;
			includeNot = true;
			break;

		case TYPE_GREATER:
			functionType = TYPE_SMALLEROREQUAL;
			includeNot = true;
			break;

		default:
			break;
		}
		filterPart.addFilterStatementPart("(");
		if (includeNot) {
			filterPart.addFilterStatementPart("!(");
		}
		operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
		filterPart.addFilterStatementPart(getOperator(functionType));
		operand[1].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
		if (includeNot) {
			filterPart.addFilterStatementPart(")");
		}
		filterPart.addFilterStatementPart(")");
	}

	protected String getOperator(int functionType) throws FilterGeneratorException {
		switch (functionType) {
		case TYPE_EQUAL:
			return "=";

		case TYPE_UNEQUAL:
			return "!=";

		case TYPE_SMALLEROREQUAL:
			return "<=";

		case TYPE_GREATEROREQUAL:
			return ">=";

		default:
			throw new FilterGeneratorException("Unknown type {" + functionType + "} found");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return TYPES;
	}

	/**
	 * Inner helper class that generates a filterpart for "==" and "!="
	 * comparisons in case one of the operands is nonstatic and the other is
	 * variable
	 */
	public class EqualityGenerator implements FilterPartGenerator {

		/**
		 * serial version id
		 */
		private static final long serialVersionUID = -3808711792530819724L;

		/**
		 * variable operand
		 */
		protected EvaluableExpression variableExpression;

		/**
		 * static operand (when the filter is generated, this operand is
		 * nonstatic, but when the generator generates the filterpart it becomes
		 * static, since we may resolve now)
		 */
		protected EvaluableExpression staticExpression;

		/**
		 * type if the function
		 */
		protected int functionType;

		/**
		 * Create an instance of thie filterpart generator
		 * @param variableExpression variable expression
		 * @param staticExpression static expression
		 * @param functionType function type
		 */
		public EqualityGenerator(EvaluableExpression variableExpression,
				EvaluableExpression staticExpression, int functionType) {
			this.variableExpression = variableExpression;
			this.staticExpression = staticExpression;
			this.functionType = functionType;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator#getFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
		 */
		public FilterPart getFilterPart(ExpressionQueryRequest request) throws ExpressionParserException {
			FilterPart newFilterPart = new NestedFilterPart(request.getFilter());

			variableWithStaticComparison(functionType, request, newFilterPart, variableExpression, staticExpression);
			return newFilterPart;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
