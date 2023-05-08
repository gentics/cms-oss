/*
 * @author norbert
 * @date 19.07.2006
 * @version $Id: SubRuleFunction.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.exception.ParserException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParser;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.mccr.MCCRDatasource;

/**
 * Implementation for the subrule function for the CNDatasource. The subrule
 * function queries the datasource with the given subrule, resolves the given
 * attribute from the resulting objects and inserts the values as literals into
 * the main filter.
 */
public class SubRuleFunction implements Function {

	/**
	 * constant for the function support provided by Portal.Node
	 */
	private final static Class<?>[] PROVIDEDCLASSES = new Class<?>[] { CNDatasource.class, ExpressionEvaluator.class, MCCRDatasource.class };

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		if (request.getDatasource() == null) {
			throw new EvaluationException("Static evaluation of subrule function not implemented");
		} else {
			try {
				String attribute = ExpressionEvaluator.getAsString(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));

				String subExpression = ExpressionEvaluator.getAsString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));

				if (subExpression == null) {
					throw new EvaluationException("Cannot perform subrule function with null as subrule");
				}

				// replace "subobject" with "object"
				subExpression = subExpression.replaceAll("\\bsubobject\\b", "object");

				// create a new datasource filter
				Expression expression = ExpressionParser.getInstance().parse(subExpression);
				DatasourceFilter subFilter = request.getDatasource().createDatasourceFilter(expression);

				// clone the datasource
				// Datasource ds = (Datasource)request.getDatasource().clone();
				Datasource ds = request.getDatasource();
				Collection<?> subResult = null;

				// share the resolvable with the request
				subFilter.setCustomResolver(request.getResolver());

				if (ds instanceof VersioningDatasource) {
					// for versioning datasources use the versiontimestamp
					subResult = ((VersioningDatasource) ds).getResult(subFilter, new String[] { attribute}, request.getVersionTimestamp());
				} else {
					subResult = ds.getResult(subFilter, new String[] { attribute});
				}
				Map<String, Object> dataMap = new HashMap<String, Object>();

				dataMap.put("results", subResult);
				PropertyResolver subResolver = new PropertyResolver(new MapResolver(dataMap));

				return ExpressionEvaluator.getAsType(subResolver.resolve("results." + attribute), expectedValueType);

			} catch (ExpressionParserException e) {
				throw e;
			} catch (Exception e) {
				throw new EvaluationException(e);
			}
		}
	}

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
		if (operand[0].isVariable(request.getFilter()) || operand[1].isVariable(request.getFilter())) {
			throw new FilterGeneratorException("Subrule function does not work with \"object.\" operands");
		}

		try {
			String attribute = ExpressionEvaluator.getAsString(operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));

			String subExpression = ExpressionEvaluator.getAsString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY));

			if (subExpression == null) {
				throw new FilterGeneratorException("Cannot perform subrule function with null as subrule");
			}

			// replace "subobject" with "object"
			subExpression = subExpression.replaceAll("\\bsubobject\\b", "object");

			// create a new datasource filter
			Expression expression = ExpressionParser.getInstance().parse(subExpression);
			DatasourceFilter subFilter = request.getDatasource().createDatasourceFilter(expression);

			// register a filterpart generator here
			filterPart.addFilterPartGenerator(new SubRuleEvaluator(attribute, subFilter));
		} catch (ParserException e) {
			throw new FilterGeneratorException("Error in subrule function", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "subrule";
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return NAMEDFUNCTION;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMinParameters()
	 */
	public int getMinParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getMaxParameters()
	 */
	public int getMaxParameters() {
		return 2;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class<?>[] getSupportedDatasourceClasses() {
		return PROVIDEDCLASSES;
	}

	/**
	 * Implementation of {@link FilterPartGenerator} to perform the subfilter
	 * and insert the results as literal filter part into the datasource filter
	 */
	public static class SubRuleEvaluator implements FilterPartGenerator {

		/**
		 * serial version id
		 */
		private static final long serialVersionUID = 3114832749018906801L;

		/**
		 * evaluated attribute
		 */
		protected String evaluatedAttribute;

		/**
		 * subfilter to query the objects
		 */
		protected DatasourceFilter subFilter;

		/**
		 * Create an instance of the filter part generator
		 * @param evaluatedAttribute evaluated attribute
		 * @param subFilter subfilter
		 */
		public SubRuleEvaluator(String evaluatedAttribute, DatasourceFilter subFilter) {
			this.evaluatedAttribute = evaluatedAttribute;
			this.subFilter = subFilter;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator#getFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
		 */
		public FilterPart getFilterPart(ExpressionQueryRequest request) throws ExpressionParserException {
			try {
				// clone the datasource
				Datasource ds = (Datasource) request.getDatasource().clone();
				Collection<?> subResult = null;

				// share the resolvables with the request
				subFilter.setCustomResolver(request.getResolver());
				if (ds instanceof VersioningDatasource) {
					// for versioning datasources use the versiontimestamp
					subResult = ((VersioningDatasource) ds).getResult(subFilter, new String[] { evaluatedAttribute}, request.getVersionTimestamp());
				} else {
					subResult = ds.getResult(subFilter, new String[] { evaluatedAttribute});
				}

				Map<String, Object> dataMap = new HashMap<String, Object>();

				dataMap.put("results", subResult);
				PropertyResolver resolver = new PropertyResolver(new MapResolver(dataMap));

				return request.getFilter().generateLiteralFilterPart(resolver.resolve("results." + evaluatedAttribute), ExpressionEvaluator.OBJECTTYPE_ANY);
			} catch (Exception e) {
				throw new FilterGeneratorException("Error while performing subrule function ", e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#supportStaticEvaluation()
	 */
	public boolean supportStaticEvaluation() {
		// the subrule function needs a datasource to evaluate
		return false;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}
}
