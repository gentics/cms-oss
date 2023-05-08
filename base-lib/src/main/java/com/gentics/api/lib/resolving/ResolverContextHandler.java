/*
 * @author norbert
 * @date 03.12.2007
 * @version $Id: ResolverContextHandler.java,v 1.1 2007-12-03 13:08:05 norbert Exp $
 */
package com.gentics.api.lib.resolving;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.gentics.lib.log.NodeLogger;

/**
 * Static handler for the {@link ResolverContext}.
 */
public final class ResolverContextHandler {

	/**
	 * registered {@link ResolverContext} instances
	 */
	private static Map contextMap = new TreeMap();

	/**
	 * logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(ResolverContextHandler.class);

	/**
	 * Private constructor to avoid instantiation
	 */
	private ResolverContextHandler() {}

	/**
	 * Register the given ResolverContext under the given name
	 * @param name name of the ResolverContext
	 * @param context ResolverContext instance to register
	 */
	public static void registerContext(String name, ResolverContext context) {
		contextMap.put(name, context);
	}

	/**
	 * Inregister the ResolverContext which was registered under the given name
	 * @param name name of the ResolverContext to unregister
	 */
	public static void unregisterContext(String name) {
		contextMap.remove(name);
	}

	/**
	 * Push the given object into all registered ResolverContext instances
	 * @param object object to push into the ResolverContext instances
	 */
	public static void push(Object object) {
		for (Iterator iterator = contextMap.values().iterator(); iterator.hasNext();) {
			ResolverContext context = (ResolverContext) iterator.next();

			try {
				context.push(object);
			} catch (Exception e) {
				logger.error("Error while pushing object onto context stack", e);
			}
		}
	}

	/**
	 * Pop the given object from the registered ResolverContext instances
	 * @param object object to pop from the ResolverContext instances
	 */
	public static void pop(Object object) {
		for (Iterator iterator = contextMap.values().iterator(); iterator.hasNext();) {
			ResolverContext context = (ResolverContext) iterator.next();

			try {
				context.pop(object);
			} catch (Exception e) {
				logger.error("Error while popping object from context stack", e);
			}
		}
	}
}
