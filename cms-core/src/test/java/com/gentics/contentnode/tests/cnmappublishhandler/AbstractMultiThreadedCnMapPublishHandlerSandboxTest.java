package com.gentics.contentnode.tests.cnmappublishhandler;

import org.junit.Before;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Sandbox tests for CnMapPublishHandler when multithreaded publishing is used
 */
abstract public class AbstractMultiThreadedCnMapPublishHandlerSandboxTest extends AbstractCnMapPublishHandlerSandboxTest {

	/**
	 * Create a test instance
	 * @param mccr true for mccr
	 * @param instantPublishing true for instant publishing
	 * @param diffDelete true for differential delete
	 */
	public AbstractMultiThreadedCnMapPublishHandlerSandboxTest(boolean mccr, boolean instantPublishing, boolean diffDelete) {
		super(mccr, instantPublishing, diffDelete);
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();

		NodeConfigRuntimeConfiguration.getPreferences().setFeature(Feature.MULTITHREADED_PUBLISHING, true);
	}
}
