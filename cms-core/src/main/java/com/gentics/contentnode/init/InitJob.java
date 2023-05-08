package com.gentics.contentnode.init;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract base class for initialization jobs.
 * Implementations, that are required to run successfully at tomcat start for the CMS to work, must be annotated with {@link Essential}.
 */
public abstract class InitJob {
	/**
	 * Logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * Execute the initialization job
	 * @throws NodeException
	 */
	public abstract void execute() throws NodeException;
}
