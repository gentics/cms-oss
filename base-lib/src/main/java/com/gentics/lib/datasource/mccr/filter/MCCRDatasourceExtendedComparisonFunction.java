package com.gentics.lib.datasource.mccr.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.lib.expressionparser.filtergenerator.NestedFilterPart;

/**
 * Implementation for evaluation and filter generation of extended comparison
 * functions (CONTAINSONEOF, CONTAINSNONE, LIKE).
 */
public class MCCRDatasourceExtendedComparisonFunction extends AbstractBinaryMCCRDatasourceFunction {

	/**
	 * constant for the implemented function types
	 */
	protected final static int[] TYPES = new int[] { TYPE_CONTAINSONEOF, TYPE_CONTAINSNONE, TYPE_LIKE, TYPE_CONTAINSALL};

	/**
	 * Maximum number of entries allowed in an SQL IN (). Some databases (like Oracle) limit the values to 1000.
	 */
	protected final static int MAX_IN_ENTRIES = 999;

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand,
			int expectedValueType) throws ExpressionParserException {
		assertCompatibleValueType(ExpressionEvaluator.OBJECTTYPE_BOOLEAN, expectedValueType);

		switch (functionType) {
		case TYPE_LIKE:
			generateLikeForCNDatasource(request, filterPart, operand);
			break;

		case TYPE_CONTAINSONEOF:
			generateContainsoneofForCNDatasource(request, filterPart, operand);
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
	private void generateLikeForCNDatasource(ExpressionQueryRequest request, FilterPart filterPart,
			EvaluableExpression[] operand) throws ExpressionParserException {
		filterPart.addFilterStatementPart("(");
		operand[0].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_STRING);
		filterPart.addFilterStatementPart(" LIKE ");
		operand[1].generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_WILDCARDSTRING);
		filterPart.addFilterStatementPart(")");
	}

	/**
	 * Generate the filter part for "containsoneof" for the cndatasource
	 * @param request expression request
	 * @param filterPart filter part
	 * @param operand array of operands
	 * @throws ExpressionParserException
	 */
	private void generateContainsoneofForCNDatasource(ExpressionQueryRequest request,
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
	 * Generate the filter part for "containsnone" for the cndatasource
	 * @param request expression request
	 * @param filterPart filter part
	 * @param operand array of operands
	 * @throws ExpressionParserException
	 */
	private void generateContainsnoneForCNDatasource(ExpressionQueryRequest request,
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
				filterPart.addLiteral(Boolean.TRUE, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			} else if (staticValue.contains(null)) {
				// collection contains null value

				// make a copy of the collection (we want to modify it)
				staticValue = new Vector(staticValue);

				// remove all nulls from the collection
				boolean nullFound = true;

				while (nullFound) {
					nullFound = staticValue.remove(null);
				}
				filterPart.addFilterStatementPart(" NOT ");
				filterPart.addFilterPartGenerator(new ContainsOneofFilterPartGenerator(variableOperand, staticValue, true));
			} else {
				filterPart.addFilterStatementPart(" NOT ");
				filterPart.addFilterPartGenerator(new ContainsOneofFilterPartGenerator(variableOperand, staticValue, false));
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

			MCCRDatasourceFilter filter = (MCCRDatasourceFilter) request.getFilter();

			if (staticValue == null || staticValue.size() == 0) {
				// null or empty collection -> false
				filterPart.addLiteral(Boolean.FALSE, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			} else if (filter.isOptimized(variableOperand)) {
				// the variable operand is an optimized attribute, so we can use the simpler and faster "column IN ([values])" notation

				// split the staticValue into parts of not more than MAX_IN_ENTRIES entries each
				List<Object> tmpStaticValue = new Vector<Object>(staticValue);
				// remove all nulls from the collection
				boolean nullFound = true;

				while (nullFound) {
					nullFound = tmpStaticValue.remove(null);
				}

				int startIndex = 0;
				int endIndex = Math.min(tmpStaticValue.size(), startIndex + MAX_IN_ENTRIES);

				filterPart.addFilterStatementPart(" ((");

				// add the first block
				variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
				filterPart.addFilterStatementPart(" IN ");
				filterPart.addLiteral(tmpStaticValue.subList(startIndex, endIndex), ExpressionEvaluator.OBJECTTYPE_ANY);

				// add more blocks if necessary
				while (endIndex < tmpStaticValue.size()) {
					startIndex = endIndex;
					endIndex = Math.min(tmpStaticValue.size(), startIndex + MAX_IN_ENTRIES);

					filterPart.addFilterStatementPart(" OR ");
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart(" IN ");
					filterPart.addLiteral(tmpStaticValue.subList(startIndex, endIndex), ExpressionEvaluator.OBJECTTYPE_ANY);
				}
				filterPart.addFilterStatementPart(") ");

				if (nullFound && variableOperand.allowsNullValues(request.getFilter())) {

					if (staticValue.size() > 0) {
						filterPart.addFilterStatementPart(" OR ");
					}
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart(" IS NULL ");
				} else {
					if (staticValue.size() > 0) {
						filterPart.addFilterStatementPart(" AND ");
					}
					variableOperand.generateFilterPart(request, filterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					filterPart.addFilterStatementPart(" IS NOT NULL ");
				}
				filterPart.addFilterStatementPart(") ");
			} else if (staticValue.contains(null)) {
				// collection contains null value

				// make a copy of the collection (we want to modify it)
				staticValue = new Vector(staticValue);

				// remove all nulls from the collection
				boolean nullFound = true;

				while (nullFound) {
					nullFound = staticValue.remove(null);
				}

				filterPart.addFilterPartGenerator(new ContainsOneofFilterPartGenerator(variableOperand, staticValue, true));
			} else {
				filterPart.addFilterPartGenerator(new ContainsOneofFilterPartGenerator(variableOperand, staticValue, false));
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

	/**
	 * A Part Generator which creates the filter statement parts for contains one of.
	 * It is also used for contains none.
	 * 
	 * @author herbert
	 */
	public static class ContainsOneofFilterPartGenerator implements FilterPartGenerator {
		private static final long serialVersionUID = -6282333149911551571L;

		private EvaluableExpression variableOperand;

		private Collection staticValue;

		private boolean staticContainsNullValue;

		public ContainsOneofFilterPartGenerator(EvaluableExpression variableOperand,
				Collection staticValue, boolean staticContainsNullValue) {
			this.variableOperand = variableOperand;
			this.staticValue = staticValue;
			this.staticContainsNullValue = staticContainsNullValue;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator#getFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
		 */
		public FilterPart getFilterPart(ExpressionQueryRequest request) throws ExpressionParserException {

			if (staticValue.size() == 0 && !variableOperand.allowsNullValues(request.getFilter())) {
				return request.getFilter().generateLiteralFilterPart(Boolean.FALSE, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
			}

			// Create a new filter (for the subquery ...)
			MCCRDatasourceFilter filter = (MCCRDatasourceFilter) request.getFilter();
			MCCRDatasourceFilter newFilter = filter.createMCCRSubFilter();
			FilterPart newFilterPart = newFilter.getMainFilterPart();

			// Configure the subquery - filter
			newFilterPart.addFilterStatementPart(" (");
			if (staticValue.size() > 0) {
				// split the staticValue into parts of not more than MAX_IN_ENTRIES entries each
				List<Object> tmpStaticValue = new Vector<Object>(staticValue);

				int startIndex = 0;
				int endIndex = Math.min(tmpStaticValue.size(), startIndex + MAX_IN_ENTRIES);

				newFilterPart.addFilterStatementPart(" (");

				// add the first block
				variableOperand.generateFilterPart(request, newFilterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
				newFilterPart.addFilterStatementPart(" IN ");
				newFilterPart.addLiteral(tmpStaticValue.subList(startIndex, endIndex), ExpressionEvaluator.OBJECTTYPE_ANY);

				// add more blocks if necessary
				while (endIndex < tmpStaticValue.size()) {
					startIndex = endIndex;
					endIndex = Math.min(tmpStaticValue.size(), startIndex + MAX_IN_ENTRIES);

					newFilterPart.addFilterStatementPart(" OR ");
					variableOperand.generateFilterPart(request, newFilterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
					newFilterPart.addFilterStatementPart(" IN ");
					newFilterPart.addLiteral(tmpStaticValue.subList(startIndex, endIndex), ExpressionEvaluator.OBJECTTYPE_ANY);
				}
				newFilterPart.addFilterStatementPart(") ");
			}

			if (staticContainsNullValue && variableOperand.allowsNullValues(request.getFilter())) {

				if (staticValue.size() > 0) {
					newFilterPart.addFilterStatementPart(" OR ");
				}
				variableOperand.generateFilterPart(request, newFilterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
				newFilterPart.addFilterStatementPart(" IS NULL ");
			} else {
				if (staticValue.size() > 0) {
					newFilterPart.addFilterStatementPart(" AND ");
				}
				variableOperand.generateFilterPart(request, newFilterPart, ExpressionEvaluator.OBJECTTYPE_ANY);
				newFilterPart.addFilterStatementPart(" IS NOT NULL ");
			}
			newFilterPart.addFilterStatementPart(") AND " + filter.getMainCmAlias() + ".contentid = " + newFilter.getMainCmAlias() + ".contentid");

			// Return subquery filter as filter part ...
			Map mainRequestParameters = new HashMap(request.getParameters());

			mainRequestParameters.put("usegroupby", "false");
			mainRequestParameters.put("selectfields", "false");
			ExpressionQueryRequest subRequest = new ExpressionQueryRequest(request.getFilter(), request.getDatasource(), request.getStart(), request.getCount(),
					null, request.getVersionTimestamp(), request.getResolver(), mainRequestParameters);
			MergedFilter mergedFilter = newFilter.getSelectStatement(subRequest);

			StringBuffer query = new StringBuffer(" EXISTS (").append(mergedFilter.getStatement()).append(") ");

			return request.getFilter().generateConstantFilterPart(query.toString(), mergedFilter.getParams().toArray());
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
