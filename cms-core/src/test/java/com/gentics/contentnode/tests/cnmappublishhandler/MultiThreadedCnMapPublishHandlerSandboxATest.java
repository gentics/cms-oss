package com.gentics.contentnode.tests.cnmappublishhandler;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Sandbox tests for CnMapPublishHandler when multithreaded publishing is used
 */
@RunWith(value = Parameterized.class)
public class MultiThreadedCnMapPublishHandlerSandboxATest extends AbstractMultiThreadedCnMapPublishHandlerSandboxTest {

	/**
	 * Create a test instance
	 * @param mccr true for mccr
	 * @param instantPublishing true for instant publishing
	 * @param diffDelete true for differential delete
	 */
	public MultiThreadedCnMapPublishHandlerSandboxATest(boolean mccr, boolean instantPublishing, boolean diffDelete) {
		super(mccr, instantPublishing, diffDelete);
	}


	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: mccr {0}, instant publishing {1}, diff delete {2}")
	public static Collection<Object[]> data() {
		return data(true);
	}
}
