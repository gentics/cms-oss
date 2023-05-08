/*
 * @author norbert
 * @date 26.03.2007
 * @version $Id: MatchesFunction.java,v 1.1 2010-02-03 09:32:49 norbert Exp $
 */
package com.gentics.lib.datasource.functions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.VersioningDatasource;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.Expression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.MapResolver;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.expressionparser.filtergenerator.NestedFilterPart;
import com.gentics.lib.expressionparser.functions.AbstractGenericBinaryFunction;
import com.gentics.lib.log.NodeLogger;

/**
 * The matches() function implements a special kind of permission rule (for CN
 * DatasourceFilters and evaluation). The function is used like:
 * <p>
 * <code>
 * matches(portal.user.permission, object.location CONTAINSONEOF this.location && object.region CONTAINSONEOF this.region)
 * </code>
 * </p>
 * Meaning: filter all objects that matches at least one of the given user
 * permissions (portal.user.permission). <code>portal.user.permission</code>
 * is supposed to be a collection of objects with attributes
 * <code>location</code> and <code>region</code> each. An object matches the
 * user's permissions if it matches the given rule agains one of those
 * permission objects that are referred to as <code>this</code> in the rule.<br>
 * This is different from the rule
 * <p>
 * <code>
 * object.location CONTAINSONEOF portal.user.permission.location && object.region CONTAINSONEOF portal.user.permission.region
 * </code>
 * </p>
 * because in this rule, it is not garantueed that the filtered object matches
 * location and region for the SAME permissions object.
 */
public class MatchesFunction extends AbstractGenericBinaryFunction {

	/**
	 * logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * This function supports filter for cn datasources and evaluation
	 */
	public final static Class<?>[] SUPPORTEDCLASSES = new Class<?>[] { CNDatasource.class, ExpressionEvaluator.class, MCCRDatasource.class };

	/**
	 * name of the object reference in the expression
	 */
	public final static String OBJECT_REFERENCE = "this";

	public final static String OBJECT_REFERENCE_DOT = OBJECT_REFERENCE + ".";

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class<?>[] getSupportedDatasourceClasses() {
		return SUPPORTEDCLASSES;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return Function.NAMEDFUNCTION;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getName()
	 */
	public String getName() {
		return "matches";
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#evaluate(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public Object evaluate(int functionType, ExpressionQueryRequest request,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// evaluate the first operand (which is supposed to be a resolvable or a collection of resolvables)
		Object thisObject = operand[0].evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

		// check whether the second operand is an expression
		if (operand[1] != null) {
			try {
				// generate the extended property resolver (wrapping the
				// property resolver from the original request, but can add
				// objects referred to as "this").
				ExtendedPropertyResolver extendedPropertyResolver = new ExtendedPropertyResolver(request.getResolver());
				// generate a subrequest based on the extended property resolver
				ExpressionQueryRequest subRequest = new ExpressionQueryRequest(extendedPropertyResolver, request.getParameters());

				// check for collections or resolvables
				if (thisObject instanceof Collection) {
					// "this" is supposed to be a collection of resolvables
					Collection<?> thisColl = (Collection<?>) thisObject;

					for (Iterator<?> iter = thisColl.iterator(); iter.hasNext();) {
						Object element = (Object) iter.next();

						if (element instanceof Resolvable) {
							// set the current "this" object
							extendedPropertyResolver.setThisObject((Resolvable) element);
							// evaluate the subrule (into a boolean)
							boolean subResult = ObjectTransformer.getBoolean(operand[1].evaluate(subRequest, ExpressionEvaluator.OBJECTTYPE_BOOLEAN), false);

							// when the subrule matches, we are done
							if (subResult) {
								return Boolean.TRUE;
							}
						} else {
							logger.warn("Expected Resolvables but found an object of class {" + element.getClass().getName() + "}: ignoring");
						}
					}
				} else if (thisObject instanceof Resolvable) {
					// set the current "this" object
					extendedPropertyResolver.setThisObject((Resolvable) thisObject);
					// evaluate the subrule
					boolean subResult = ObjectTransformer.getBoolean(operand[1].evaluate(subRequest, ExpressionEvaluator.OBJECTTYPE_BOOLEAN), false);

					// when the subrule matches, we are done
					if (subResult) {
						return Boolean.TRUE;
					}
				} else {
					logger.warn(
							"Expected Resolvables but found an object of class {" + (thisObject != null ? thisObject.getClass().getName() : null) + "}: ignoring");
				}

			} catch (Exception e) {
				throw new EvaluationException("Error while evaluating matches() function", e);
			}

		} else {
			throw new EvaluationException("Error while evaluting the matches() function: second operand must be an expression");
		}

		return Boolean.FALSE;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#generateFilterPart(int,
	 *      com.gentics.api.lib.expressionparser.ExpressionQueryRequest,
	 *      com.gentics.api.lib.expressionparser.filtergenerator.FilterPart,
	 *      com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	public void generateFilterPart(int functionType, ExpressionQueryRequest request,
			FilterPart filterPart, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		// get the second operand as expression
		if (operand[1] != null) {
			// generate the filter from the expression
			DatasourceFilter subFilter = request.getDatasource().createDatasourceFilter((Expression) operand[1]);

			// register a filterpart generator here
			filterPart.addFilterPartGenerator(new SubRuleEvaluator(operand[0], subFilter));
		} else {
			throw new FilterGeneratorException("Error while generating filter for matches() function: second operand must be an expression");
		}
	}

	/**
	 * Inner helper class to extend the property resolver of the query request by adding the "this" object
	 */
	public static class ExtendedPropertyResolver extends PropertyResolver {

		/**
		 * Current "this" object
		 */
		protected Resolvable thisObject;

		/**
		 * property resolver based on the "this" object
		 */
		protected PropertyResolver thisObjectResolver;

		/**
		 * wrapped original property resolver
		 */
		protected PropertyResolver wrappedResolver;

		/**
		 * Create an instance of the extended property resolver
		 * @param wrappedResolver wrapped Resolver
		 */
		public ExtendedPropertyResolver(PropertyResolver wrappedResolver) {
			super(null);
			this.wrappedResolver = wrappedResolver;
		}

		/**
		 * Set the current "this" object
		 * @param thisObject "this" object
		 */
		public void setThisObject(Resolvable thisObject) {
			this.thisObject = thisObject;
			this.thisObjectResolver = new PropertyResolver(this.thisObject);
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.PropertyResolver#resolve(java.lang.String,
		 *      boolean)
		 */
		public Object resolve(String propertyPath, boolean failIfUnresolvablePath) throws UnknownPropertyException {
			if (OBJECT_REFERENCE.equals(propertyPath)) {
				// resolve the "this" object itself
				return thisObject;
			} else if (propertyPath != null && propertyPath.startsWith(OBJECT_REFERENCE_DOT) && thisObjectResolver != null) {
				// resolve from the thisObject
				return thisObjectResolver.resolve(propertyPath.substring(OBJECT_REFERENCE_DOT.length()), failIfUnresolvablePath);
			} else if (wrappedResolver != null) {
				// forward to the wrapped resolver
				return wrappedResolver.resolve(propertyPath, failIfUnresolvablePath);
			} else {
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
		 */
		public boolean canResolve() {
			return thisObject != null || (wrappedResolver != null && wrappedResolver.canResolve());
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		 */
		public Object get(String key) {
			if (OBJECT_REFERENCE.equals(key)) {
				return thisObject;
			} else if (wrappedResolver != null) {
				return wrappedResolver.get(key);
			} else {
				return null;
			}
		}
	}

	/**
	 * Implementation of {@link FilterPartGenerator} to perform the subfilter
	 * and insert the results as literal filter part into the datasource filter
	 */
	public class SubRuleEvaluator implements FilterPartGenerator {

		/**
		 * serial version id
		 */
		private static final long serialVersionUID = 3114832749018906801L;

		/**
		 * evaluated attribute
		 */
		protected EvaluableExpression thisObjectOperand;

		/**
		 * subfilter to query the objects
		 */
		protected DatasourceFilter subFilter;

		/**
		 * Create an instance of the filter part generator
		 * @param thisObjectOperand "this" object(s)
		 * @param subFilter subfilter
		 */
		public SubRuleEvaluator(EvaluableExpression thisObjectOperand,
				DatasourceFilter subFilter) {
			this.thisObjectOperand = thisObjectOperand;
			this.subFilter = subFilter;
		}

		/*
		 * (non-Javadoc)
		 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator#getFilterPart(com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
		 */
		public FilterPart getFilterPart(ExpressionQueryRequest request) throws ExpressionParserException {
			try {
				// clone the datasource
				Datasource ds = request.getDatasource();
				Collection<Object> subResult = new Vector<Object>();

				// evaluate the "this" object operand
				Object thisObject = thisObjectOperand.evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY);

				// share the resolvables with the request
				ExtendedPropertyResolver extendedPropertyResolver = new ExtendedPropertyResolver(request.getResolver());

				subFilter.setCustomResolver(extendedPropertyResolver);

				if (thisObject instanceof Collection) {
					// "this" is supposed to be a collection of resolvables
					Collection<?> thisColl = (Collection<?>) thisObject;

					for (Iterator<?> iter = thisColl.iterator(); iter.hasNext();) {
						Object element = (Object) iter.next();

						if (element instanceof Resolvable) {
							addTempResults(request, ds, subFilter, extendedPropertyResolver, (Resolvable) element, subResult);
						} else {
							logger.warn(
									"Expected Resolvables but found an object of class {" + (element != null ? element.getClass().getName() : null) + "}: ignoring");
						}
					}
				} else if (thisObject instanceof Resolvable) {
					addTempResults(request, ds, subFilter, extendedPropertyResolver, (Resolvable) thisObject, subResult);
				} else {
					logger.warn(
							"Expected Resolvables but found an object of class {" + (thisObject != null ? thisObject.getClass().getName() : null) + "}: ignoring");
				}

				// when collection is empty, matches() is false
				if (subResult.isEmpty()) {
					return request.getFilter().generateLiteralFilterPart(Boolean.FALSE, ExpressionEvaluator.OBJECTTYPE_BOOLEAN);
				} else {
					NestedFilterPart part = new NestedFilterPart(request.getFilter());

					part.addVariable("object.contentid", ExpressionEvaluator.OBJECTTYPE_ANY);
					part.addFilterStatementPart(" in ");

					Map<String, Object> dataMap = new HashMap<String, Object>();

					dataMap.put("results", subResult);
					PropertyResolver resolver = new PropertyResolver(new MapResolver(dataMap));

					part.addLiteral(resolver.resolve("results.contentid"), ExpressionEvaluator.OBJECTTYPE_ANY);

					return part;
				}
			} catch (Exception e) {
				throw new FilterGeneratorException("Error while performing matches() function ", e);
			}
		}

		/**
		 * Add the results from the datasource for the given "this" object into
		 * the results collection
		 * @param request expression parser request
		 * @param ds datasource
		 * @param filter filter
		 * @param extendedPropertyResolver extended property resolver
		 * @param thisObject this object
		 * @param results collection of results
		 * @throws ExpressionParserException
		 * @throws DatasourceException
		 */
		protected void addTempResults(ExpressionQueryRequest request, Datasource ds,
				DatasourceFilter filter, ExtendedPropertyResolver extendedPropertyResolver,
				Resolvable thisObject, Collection<Object> results) throws ExpressionParserException,
					DatasourceException {
			extendedPropertyResolver.setThisObject(thisObject);
			Collection<?> tempResults = null;

			if (ds instanceof VersioningDatasource) {
				tempResults = ((VersioningDatasource) ds).getResult(filter, null, request.getVersionTimestamp());
			} else {
				tempResults = ds.getResult(filter, null);
			}
			tempResults.removeAll(results);
			results.addAll(tempResults);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType()
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_BOOLEAN;
	}
}
