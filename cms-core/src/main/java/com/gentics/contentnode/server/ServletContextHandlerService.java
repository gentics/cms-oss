package com.gentics.contentnode.server;

import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Interface for services, that modify the {@link ServletContextHandler} (e.g. add routes)
 */
public interface ServletContextHandlerService {
	/**
	 * Initialize the context
	 * @param context context
	 */
	void init(ServletContextHandler context);
}
