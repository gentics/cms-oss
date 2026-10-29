package com.gentics.contentnode.tests.multichannelling;

/**
 * Variant of {@link ChannelMTPublishSandboxTest} that also uses publish_cache
 */
public class ChannelPublishCachePublishSandboxTest extends ChannelMTPublishSandboxTest {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("publish_cache", true);
	}
}
