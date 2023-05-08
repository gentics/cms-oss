package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.assertEquals;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Test rule that assert that the {@link #CountDirective} was never invoked
 */
public class CountDirectiveRule extends TestWatcher {
	/**
	 * Rendered name
	 */
	protected String renderedName;

	/**
	 * Create rule for rendered name
	 * @param renderedName rendered name
	 */
	public CountDirectiveRule(String renderedName) {
		this.renderedName = renderedName;
	}

	@Override
	protected void starting(Description description) {
		CountDirective.reset();
	}

	@Override
	protected void finished(Description description) {
		assertEquals("Check render counts", 0, CountDirective.get(renderedName));
	}
}
