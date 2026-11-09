package com.gentics.contentnode.tests.multichannelling;

import com.gentics.contentnode.etc.Feature;

/**
 * Variant of {@link ChannelPublishSandboxTest} that uses multithreaded publishing
 */
public class ChannelMTPublishSandboxTest extends ChannelPublishSandboxTest {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTITHREADED_PUBLISHING, true);
		testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty("contentnode.config.loadbalancing.threadlimit", "2");
	}
}
