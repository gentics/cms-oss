package com.gentics.contentnode.scheduler;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;

/**
 * Interface for internal scheduler tasks
 */
public interface InternalSchedulerTask {
	/**
	 * Get the command
	 * @return command
	 */
	String getCommand();

	/**
	 * Get the default name
	 * @return default name
	 */
	String getName();

	/**
	 * Get the default description
	 * @return default description
	 */
	String getDescription();

	/**
	 * Execute the internal task
	 * @param out list of string to collect output
	 * @return true for success, false for error
	 * @throws NodeException
	 */
	boolean execute(List<String> out) throws NodeException;
}
