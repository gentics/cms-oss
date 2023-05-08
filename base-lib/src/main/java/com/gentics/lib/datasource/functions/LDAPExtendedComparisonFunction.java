/*
 * @author norbert
 * @date 04.07.2006
 * @version $Id: LDAPExtendedComparisonFunction.java,v 1.5 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.datasource.functions;

import java.util.Collection;
import java.util.Iterator;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.lib.expressionparser.filtergenerator.NestedFilterPart;

/**
 * Implementation for ldap filter generation of extended comparison
 * functions (CONTAINSONEOF, CONTAINSNONE, LIKE).
 */
public class LDAPExtendedComparisonFunction extends AbstractBinaryLDAPFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_CONTAINSONEOF, TYPE_CONTAINSNONE, TYPE_LIKE, TYPE_CONTAINSALL};

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);

		switch (functionType) {
		case TYPE_LIKE:
			generateLike(request, filterPart, operand);
			break;

		case TYPE_CONTAINSONEOF:
			generateContainsoneof(request, filterPart, operand);
			break;

		case TYPE_CONTAINSNONE:
			generateContainsnoneForCNDatasource(request, filterPart, operand);
			break;

		case TYPE_CONTAINSALL:
			throw new EvaluationException("The operator CONTAINSALL is not supported for filters.");

		default:
			unknownTypeFound(functionType);
			break;
		}
	}

	/**
	 * Generate the filter part for "like" for the cndatasource
	 * @param request expression request
	 * @param filterPart filter part
	 * @param operand array of operands
	 * @throws FilterGeneratorException
	 */
	protected void generateLike(ExpressionQueryRequest request, FilterPart filterPart,
			EvaluableExpression[] operand) throws ExpressionParserException {
		filterPart.addFilterStatementPart("(");
		operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_STRING);
		filterPart.addFilterStatementPart("=");
		operand[1].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_WILDCARDSTRING);
		filterPart.addFilterStatementPart(")");
	}

	/**
	 * Generate the filter part for "containsoneof" for the LDAPDatasource
	 * @param request expression request
	 * @param filterPart filter part
	 * @param operand array of operands
	 * @throws ExpressionParserException
	 */
	protected void generateContainsoneof(ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand) throws ExpressionParserException {
		if (!operand[0].isVariable(request.getFilter())) {
			throw new FilterGeneratorException("Cannot create filter for static CONTAINSONEOF comparison");
		}
		if (operand[1].isVariable(request.getFilter())) {
			throw new FilterGeneratorException(
					"Cannot create filter for all dynamic CONTAINSONEOF comparison " + "(righthandside of CONTAINSONEOF must not containt \"object.*\" expressions)");
		}

		EvaluableExpression variableOperand = operand[0];
		EvaluableExpression staticOperand = operand[1];

		if (staticOperand.isStatic(request.getFilter())) {
			// generate the filterpart right now
			variableWithStaticContainsOneOfComparison(request, filterPart, variableOperand, staticOperand);
		} else {
			filterPart.addFilterPartGenerator(new ContainsPartGenerator(variableOperand, staticOperand, TYPE_CONTAINSONEOF));
		}
	}

	/**
	 * Generate the filter part for "containsnone" for the LDAPDatasource
	 * @param request expression request
	 * @param filterPart filter part
	 * @param operand array of operands
	 * @throws ExpressionParserException
	 */
	protected void generateContainsnoneForCNDatasource(ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand) throws ExpressionParserException {
		if (!operand[0].isVariable(request.getFilter())) {
			throw new FilterGeneratorException("Cannot create filter for static CONTAINSNONE comparison");
		}
		if (operand[1].isVariable(request.getFilter())) {
			throw new FilterGeneratorException(
					"Cannot create filter for all dynamic CONTAINSNONE comparison " + "(righthandside of CONTAINSNONE must not containt \"object.*\" expressions)");
		}

		EvaluableExpression variableOperand = operand[0];
		EvaluableExpression staticOperand = operand[1];

		if (staticOperand.isStatic(request.getFilter())) {
			// generate the filterpart right now
			variableWithStaticContainsNoneComparison(request, filterPart, variableOperand, staticOperand);
		} else {
			filterPart.addFilterPartGenerator(new ContainsPartGenerator(variableOperand, staticOperand, TYPE_CONTAINSNONE));
		}
	}

	private void variableWithStaticContainsNoneComparison(ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression variableOperand,
			EvaluableExpression staticOperand) throws ExpressionParserException {
		try {
			Collection staticValue = (Collection) staticOperand.evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION);

			if (staticValue == null || staticValue.size() == 0) {
				// null or empty collection -> true
				filterPart.addLiteral(Boolean.TRUE, ExpressionEvaluator.OBJECTTYPE_ANY);
			} else {
				filterPart.addFilterStatementPart("(&");
				for (Iterator iter = staticValue.iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					filterPart.addFilterStatementPart("(");
					if (element != null) {
						filterPart.addFilterStatementPart("!(");
						variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
						filterPart.addFilterStatementPart("=");
						filterPart.addLiteral(element, ExpressionEvaluator.OBJECTTYPE_STRING);
						filterPart.addFilterStatementPart(")");
					} else {
						variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
						filterPart.addFilterStatementPart("=*");
					}
					filterPart.addFilterStatementPart(")");
				}
				filterPart.addFilterStatementPart(")");
			}

		} catch (EvaluationException e) {
			throw new FilterGeneratorException(e);
		}
	}

	private void variableWithStaticContainsOneOfComparison(ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression variableOperand,
			EvaluableExpression staticOperand) throws ExpressionParserException {
		try {
			Collection staticValue = (Collection) staticOperand.evaluate(request, ExpressionEvaluator.OBJECTTYPE_COLLECTION);

			if (staticValue == null || staticValue.size() == 0) {
				// null or empty collection -> false
				filterPart.addLiteral(Boolean.FALSE, ExpressionEvaluator.OBJECTTYPE_ANY);
			} else {
				filterPart.addFilterStatementPart("(|");
				for (Iterator iter = staticValue.iterator(); iter.hasNext();) {
					Object element = (Object) iter.next();

					filterPart.addFilterStatementPart("(");
					if (element != null) {
						variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
						filterPart.addFilterStatementPart("=");
						filterPart.addLiteral(element, ExpressionEvaluator.OBJECTTYPE_STRING);
					} else {
						filterPart.addFilterStatementPart("!(");
						variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
						filterPart.addFilterStatementPart("=*)");
					}
					filterPart.addFilterStatementPart(")");
				}
				filterPart.addFilterStatementPart(")");
			}

		} catch (EvaluationException e) {
			throw new FilterGeneratorException(e);
		}
	}

	public class ContainsPartGenerator implements FilterPartGenerator {

		/**
		 * serial version uid
		 */
		private static final long serialVersionUID = -4321919289889880301L;

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
		 * Create an instance of the ContainsPartGenerator
		 * @param variableExpression variable expression
		 * @param staticExpression static expression
		 * @param functionType type of the function
		 */
		public ContainsPartGenerator(EvaluableExpression variableExpression,
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

			switch (functionType) {
			case TYPE_CONTAINSONEOF:
				variableWithStaticContainsOneOfComparison(request, newFilterPart, variableExpression, staticExpression);
				break;

			case TYPE_CONTAINSNONE:
				variableWithStaticContainsNoneComparison(request, newFilterPart, variableExpression, staticExpression);
				break;

			default:
				// this cannot really happen
				throw new FilterGeneratorException("Unknown function type found");
			}
			return newFilterPart;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return TYPES;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
