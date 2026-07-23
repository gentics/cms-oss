package com.gentics.contentnode.monitoring;

import com.gentics.monitoring.liveness.DefaultLivenessManager;
import com.gentics.monitoring.liveness.LivenessManagerOptions;

/**
 * Singleton for liveness checks.
 */
public class CmsLivenessManager extends DefaultLivenessManager {

	private static CmsLivenessManager instance;

	/**
	 * Hidden default constructor.
	 */
	private CmsLivenessManager() {
		super(new LivenessManagerOptions("cms.live"), null);
	}

	/**
	 * Get the singleton instance.
	 *
	 * @return The singleton instance.
	 */
	public static CmsLivenessManager getInstance() {
		if (instance == null) {
			instance = new CmsLivenessManager();
		}

		return instance;
	}
}
