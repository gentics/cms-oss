package com.gentics.api.lib.expressionparser.filtergenerator;

import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Interface for implementation of PostProcessors.
 * PostProcessor can be used when querying a ContentRepository Datasource by using the filter() function.
 */
public interface PostProcessor {

	/**
	 * Process the given list of resolvables, which is the result of the datasource query
	 * @param resolvables list of resolvables
	 * @param data data object
	 * @throws EvaluationException in case of errors
	 */
	void process(List<Resolvable> resolvables, Object data) throws EvaluationException;
}
