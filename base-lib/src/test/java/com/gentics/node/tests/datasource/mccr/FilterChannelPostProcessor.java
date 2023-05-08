package com.gentics.node.tests.datasource.mccr;

import java.util.Iterator;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.expressionparser.EvaluationException;
import com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.datasource.mccr.MCCRObject;

/**
 * Post processor that filters object by their channelId
 */
public class FilterChannelPostProcessor implements PostProcessor {

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.expressionparser.filtergenerator.PostProcessor#process(java.util.List, java.lang.Object)
	 */
	public void process(List<Resolvable> resolvables, Object data) throws EvaluationException {
		int channelId = ObjectTransformer.getInt(data, 0);

		for (Iterator<Resolvable> iter = resolvables.iterator(); iter.hasNext();) {
			Resolvable res = iter.next();

			if (res instanceof MCCRObject) {
				if (((MCCRObject) res).getChannelId() != channelId) {
					iter.remove();
				}
			} else {
				iter.remove();
			}
		}
	}
}
