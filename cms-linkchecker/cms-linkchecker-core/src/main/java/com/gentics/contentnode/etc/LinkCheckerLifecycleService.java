package com.gentics.contentnode.etc;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.page.linkchecker.LinkChecker;

/**
 * Service that starts and stops the linkchecker
 */
public class LinkCheckerLifecycleService implements LifecycleService {
	@Override
	public void start() {
		LinkChecker.init();
	}

	@Override
	public void stop() {
		try {
			LinkChecker.terminate();
		} catch (NodeException e) {
			LinkChecker.logger.warn("Error while stopping LinkChecker", e);
		}
	}
}
