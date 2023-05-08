package com.gentics.contentnode.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.commons.lang3.StringUtils;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.gentics.api.lib.exception.NodeException;

/**
 * Test rule that checks whether exceptions thrown by a test match the expected exception (annotated with {@link Expected}).
 */
public class ExceptionChecker implements TestRule {
	protected Class<? extends NodeException> expected;

	protected String expectedMessage;

	/**
	 * Except the given exception
	 * @param expected expected exception
	 */
	public void expect(Class<? extends NodeException> expected) {
		expect(expected, null);
	}

	/**
	 * Except the given exception with message
	 * @param expected expected exception
	 * @param expectedMessage expected message
	 */
	public void expect(Class<? extends NodeException> expected, String expectedMessage) {
		this.expected = expected;
		this.expectedMessage = expectedMessage;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		if (expected == null) {
			Expected expectedAnnotation = description.getAnnotation(Expected.class);
			if (expectedAnnotation != null) {
				expect(expectedAnnotation.ex(), expectedAnnotation.message());
			}
		}

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
					if (expected != null) {
						fail(String.format("Expected exception %s was not thrown", expected.getName()));
					}
				} catch (AssumptionViolatedException e) {
					throw e;
				} catch (Throwable t) {
					if (expected == null) {
						throw t;
					} else if (!expected.isAssignableFrom(t.getClass())) {
						throw t;
					} else {
						if (!StringUtils.isEmpty(expectedMessage)) {
							assertEquals("Exception message", expectedMessage, t.getLocalizedMessage());
						}
					}
				}
			}
		};
	}
}
