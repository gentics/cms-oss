/*
 * @author norbert
 * @date 21.07.2006
 * @version $Id: GenericFilterPart.java,v 1.6 2007-01-16 12:05:41 clemens Exp $
 */
package com.gentics.lib.expressionparser.filtergenerator;

import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.DatasourceFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPart;
import com.gentics.api.lib.expressionparser.filtergenerator.FilterPartGenerator;
import com.gentics.api.lib.expressionparser.filtergenerator.MergedFilter;
import com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor;
import com.gentics.api.lib.expressionparser.functions.Function;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.datasource.functions.PostProcessorEvaluator;

/**
 * Generic abstract implementation of a filterpart. Throws a
 * {@link com.gentics.api.lib.expressionparser.filtergenerator.FilterGeneratorException}
 * on calls to every implemented method. Implementors of FilterParts can extend
 * this class and implement just the needed methods.
 */
public abstract class GenericFilterPart implements FilterPart {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5486378756270722301L;

	/**
	 * constant for the exception message
	 */
	private final static String NOTIMPLEMENTED_EXCEPTION = "this method is not implemented for this filter part";

	/**
	 * datasource filter
	 */
	protected DatasourceFilter filter;

	/**
	 * Post Processor evaluator
	 */
	protected PostProcessorEvaluator postProcessorEvaluator = new PostProcessorEvaluator();

	/**
	 * Create an instance of the filterpart
	 * @param filter datasource filter
	 */
	public GenericFilterPart(DatasourceFilter filter) {
		this.filter = filter;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addFilterStatementPart(java.lang.String)
	 */
	public void addFilterStatementPart(String statementPart) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addFilterStatementPart(java.lang.String,
	 *      java.lang.Object[])
	 */
	public void addFilterStatementPart(String statementPart, Object[] parameters) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addVariable(java.lang.String)
	 */
	public void addVariable(String expressionName, int expectedValueType) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addLiteral(java.lang.Object)
	 */
	public void addLiteral(Object literal, int expectedValueType) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addFunctionToEvaluate(com.gentics.lib.expressionparser.functions.Function,
	 *      int, com.gentics.lib.expressionparser.EvaluableExpression[])
	 */
	public void addFunctionToEvaluate(Function function, int type,
			EvaluableExpression[] operands, int expectedValueType) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addResolvableObject(java.lang.String,
	 *      int)
	 */
	public void addResolvableObject(String expressionName, int expectedValueType) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#addFilterPartGenerator(com.gentics.lib.expressionparser.filtergenerator.DatasourceFilter.FilterPartGenerator)
	 */
	public void addFilterPartGenerator(FilterPartGenerator filterPartGenerator) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPart#addPostProcessor(com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor, com.gentics.api.lib.expressionparser.EvaluableExpression)
	 */
	public void addPostProcessor(PostProcessor postProcessor, EvaluableExpression data) throws ExpressionParserException {
		postProcessorEvaluator.addPostProcessor(postProcessor, data);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPart#doPostProcessing(java.util.List, com.gentics.api.lib.expressionparser.ExpressionQueryRequest)
	 */
	public void doPostProcessing(List<Resolvable> result, ExpressionQueryRequest request) throws ExpressionParserException {
		postProcessorEvaluator.doPostProcessing(result, request);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.FilterPart#hasPostProcessors()
	 */
	public boolean hasPostProcessors() {
		return postProcessorEvaluator.hasPostProcessors();
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.expressionparser.filtergenerator.FilterPart#mergeInto(com.gentics.lib.expressionparser.filtergenerator.MergedFilter)
	 */
	public void mergeInto(MergedFilter mergedFilter) throws ExpressionParserException {
		throw new FilterGeneratorException(NOTIMPLEMENTED_EXCEPTION);
	}
}
