package com.gentics.contentnode.publish;

/**
 * Interface for LoadMonitor implementations, that will control, how fast pages are distributed by the {@link PageDistributor}.
 */
public interface WorkLoadMonitor {
	/**
	 * Check whether the load is currently too high to distribute further pages.
	 * Implementations should block as long as the load is too high.
	 * @throws Exception
	 */
	void checkHighLoad() throws Exception;
}
