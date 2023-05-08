package com.gentics.node.tests.expressionparser;

import java.util.List;

import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * Dummy post processor that will match all objects
 */
public class TruePostProcessor implements PostProcessor {

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor#process(java.util.List, java.lang.Object)
	 */
	public void process(List<Resolvable> resolvables, Object data) throws EvaluationException {}
}
