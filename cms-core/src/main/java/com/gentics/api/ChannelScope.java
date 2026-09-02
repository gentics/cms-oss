package com.gentics.api;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.NoMcTrx;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.render.RenderUtils;

import groovy.lang.Closure;

/**
 * Channel Scope helper which should be used in Groovy Scripts to load objects in the scope of channels
 */
public class ChannelScope {
	/**
	 * Private constructor
	 */
	private ChannelScope() {
	}

	/**
	 * Run the closure body in a {@link ChannelTrx}
	 * @param <T> type of the return value
	 * @param scope scope (channel)
	 * @param body closure body
	 * @return return value
	 * @throws NodeException
	 */
	public static <T> T withChannel(Object scope, Closure<T> body) throws NodeException {
		Node node = null;
		try (final NoMcTrx nMcTrx = new NoMcTrx()) {
			node = RenderUtils.getObject(scope, Node.class, NodePartType.class, NodePartType::getNode);
		}

		try (ChannelTrx cTrx = new ChannelTrx(node)) {
			return body.call(cTrx);
		}
	}
}
