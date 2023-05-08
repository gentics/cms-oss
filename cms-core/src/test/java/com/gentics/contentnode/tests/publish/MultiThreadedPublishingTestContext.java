/*
 * @author norbert
 * @date 05.01.2010
 * @version $Id: MultiThreadedPublishingTestContext.java,v 1.2 2010-08-26 12:49:14 johannes2 Exp $
 */
package com.gentics.contentnode.tests.publish;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.tests.rendering.ContentNodeTestContext;

/**
 * Test context for the multithreaded publishing tests
 */
public class MultiThreadedPublishingTestContext extends ContentNodeTestContext {
	public final static String MULTITHREADED_PUBLISHING_FEATURE = "multithreaded_publishing";

	public MultiThreadedPublishingTestContext() throws NodeException {
		super();
	}

	public MultiThreadedPublishingTestContext(boolean startTransaction) throws NodeException {
		super();
	}

	/**
	 * Change the feature "multithreaded_publishing" to switch multithreaded publishing on/off (dependending on the given flag)
	 * @param multithreadedPublishing true to switch multithreaded publishing on, false for switching it off
	 */
	public void setMultiThreadedPublishing(boolean multithreadedPublishing) {
		NodeConfigRuntimeConfiguration.getPreferences().setFeature(Feature.MULTITHREADED_PUBLISHING, multithreadedPublishing);
	}
}
