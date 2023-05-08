package com.gentics.lib.datasource.functions;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor;
import com.gentics.lib.datasource.CNDatasource;
import com.gentics.lib.datasource.mccr.MCCRDatasource;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.expressionparser.functions.AbstractGenericFunction;
import com.gentics.lib.log.NodeLogger;

/**
 * The filter() function can be used to process a result from a
 * contentrepository datasource filter with instances of {@link PostProcessor}.
 */
public class FilterFunction extends AbstractGenericFunction {

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(FilterFunction.class);

	/**
	 * supported classes
	 */
	public final static Class<?>[] SUPPORTED_CLASSES = new Class[] { CNDatasource.class, MCCRDatasource.class, ExpressionEvaluator.class};

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getTypes()
	 */
	public int[] getTypes() {
		return NAMEDFUNCTION;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#generateFilterPart(int, com.gentics.api.lib.expressionparser.ExpressionQueryRequest, com.gentics.api.lib.expressionparser.filtergenerator.FilterPart, com.gentics.api.lib.expressionparser.EvaluableExpression[], int)
	 */
	@Override
	public void generateFilterPart(int functionType,
			ExpressionQueryRequest request, FilterPart filterPart,
			EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		operand[0].generateFilterPart(request, filterPart, expectedValueType);

		PostProcessor postProcessor = getPostProcessor(request, operand);

		if (postProcessor != null) {
			EvaluableExpression data = null;

			if (operand.length == 3) {
				data = operand[2];
			}
			filterPart.addPostProcessor(postProcessor, data);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.functions.Function#getExpectedValueType(int)
	 */
	public int getExpectedValueType(int functionType) throws ExpressionParserException {
		return ExpressionEvaluator.OBJECTTYPE_ANY;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.AbstractGenericFunction#getName()
	 */
	@Override
	public String getName() {
		return "filter";
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.functions.Function#getSupportedDatasourceClasses()
	 */
	public Class<?>[] getSupportedDatasourceClasses() {
		return SUPPORTED_CLASSES;
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
		return 3;
	}

	@Override
	public Object evaluate(int functionType, ExpressionQueryRequest request, EvaluableExpression[] operand, int expectedValueType) throws ExpressionParserException {
		try {
			DatasourceFilter filter = request.getFilter();

			if (filter != null) {
				PostProcessor postProcessor = getPostProcessor(request, operand);

				if (postProcessor != null) {
					EvaluableExpression data = null;

					if (operand.length == 3) {
						data = operand[2];
					}
					filter.addPostProcessor(postProcessor, data);
				}
			} else {
				Object object = request.getResolver().resolve("object");

				// if "object" resolves to an instance of PostProcessorEvaluator, this evaluate request is made to collect the PostProcessor instances
				// the called (most likely ExpressionEvaluator) will then be responsible to use the PostProcessorEvaluator to do post processing
				if (object instanceof PostProcessorEvaluator) {
					PostProcessor postProcessor = getPostProcessor(request, operand);

					if (postProcessor != null) {
						EvaluableExpression data = null;

						if (operand.length == 3) {
							data = operand[2];
						}
						((PostProcessorEvaluator) object).addPostProcessor(postProcessor, data);
					}
				}
			}
		} catch (UnknownPropertyException e) {}
		// simply evaluate the first operand (which is the nested filter rule)
		return operand[0].evaluate(request, expectedValueType);
	}

	/**
	 * Get the post processor instance (or null)
	 * @param request query request
	 * @param operand list of operands
	 * @return post processor instance or null
	 * @throws ExpressionParserException
	 */
	protected PostProcessor getPostProcessor(ExpressionQueryRequest request, EvaluableExpression[] operand) throws ExpressionParserException {
		String className = ObjectTransformer.getString(operand[1].evaluate(request, ExpressionEvaluator.OBJECTTYPE_STRING), null);

		if (StringUtils.isEmpty(className)) {
			logger.warn("Found empty classname for postprocessor in filter() function, ignoring");
			return null;
		} else {
			// try to load the class
			try {
				Class<?> postProcessorClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());

				// check whether the class implements the PostProcessor interface
				if (PostProcessor.class.isAssignableFrom(postProcessorClass)) {
					// create an instance
					PostProcessor postProcessor = (PostProcessor) postProcessorClass.newInstance();

					return postProcessor;
				} else {
					throw new FilterGeneratorException(
							"postprocessor class " + postProcessorClass.getName() + " does not implement interface " + PostProcessor.class.getName());
				}
			} catch (Exception e) {
				throw new FilterGeneratorException("Error while preparing postprocessor of class " + className, e);
			}
		}
	}
}
