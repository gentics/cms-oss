package com.gentics.lib.datasource.functions;

import java.util.ArrayList;
import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluableExpression;
import com.gentics.api.lib.expressionparser.ExpressionEvaluator;
import com.gentics.api.lib.expressionparser.ExpressionParserException;
import com.gentics.api.lib.expressionparser.ExpressionQueryRequest;
import com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Post Processor Evaluator implements collecting PostProcessor instances (together with their data) and 
 */
public class PostProcessorEvaluator {

	/**
	 * post processors
	 */
	protected List<PostProcessorWithData> postProcessors = new ArrayList<PostProcessorWithData>();

	/**
	 * Add a post processor with data
	 * @param postProcessor post processor instance
	 * @param data data (may be null)
	 * @throws ExpressionParserException
	 */
	public void addPostProcessor(PostProcessor postProcessor, EvaluableExpression data) throws ExpressionParserException {
		postProcessors.add(new PostProcessorWithData(postProcessor, data));
	}

	/**
	 * Do post processing
	 * @param result list of resolvables, that will be passed to the post processors
	 * @param request expression query request
	 * @throws ExpressionParserException
	 */
	public void doPostProcessing(List<Resolvable> result, ExpressionQueryRequest request) throws ExpressionParserException {
		for (PostProcessorWithData proc : postProcessors) {
			proc.process(result, request);
		}
	}

	/**
	 * Check whether any post processors are set
	 * @return true if post processors are set, false if not
	 */
	public boolean hasPostProcessors() {
		return postProcessors.size() > 0;
	}

	/**
	 * Internal class to encapsulate a post processor with its data expression
	 */
	protected class PostProcessorWithData {

		/**
		 * Post processor instance
		 */
		protected PostProcessor postProcessor;

		/**
		 * data expression
		 */
		protected EvaluableExpression data;

		/**
		 * Create an instance
		 * @param postProcessor post processor instance
		 * @param data data expression
		 */
		public PostProcessorWithData(PostProcessor postProcessor, EvaluableExpression data) {
			this.postProcessor = postProcessor;
			this.data = data;
		}

		/**
		 * Process the given list of resolvables with the post processor instance. If a data expression is given,
		 * evaluate it first (using the given request) and pass the result to the post processor
		 * @param resolvables list of resolvables
		 * @param request request
		 * @throws ExpressionParserException
		 */
		public void process(List<Resolvable> resolvables, ExpressionQueryRequest request) throws ExpressionParserException {
			if (this.postProcessor != null) {
				this.postProcessor.process(resolvables, data != null ? data.evaluate(request, ExpressionEvaluator.OBJECTTYPE_ANY) : null);
			}
		}
	}
}
