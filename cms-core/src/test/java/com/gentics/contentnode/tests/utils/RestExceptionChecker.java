package com.gentics.contentnode.tests.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.gentics.contentnode.rest.model.response.GenericResponse;

/**
 * Testrule that checks, whether the expected WebApplicationException was thrown during the test
 */
public class RestExceptionChecker implements TestRule {
	/**
	 * Expected exception
	 */
	protected Class<? extends WebApplicationException> expected;

	/**
	 * Optional expected message
	 */
	protected String expectedMessage;

	/**
	 * Except the given exception
	 * @param expected expected exception
	 */
	public void expect(Class<? extends WebApplicationException> expected) {
		expect(expected, null);
	}

	/**
	 * Except the given exception with message
	 * @param expected expected exception
	 * @param expectedMessage expected message
	 */
	public void expect(Class<? extends WebApplicationException> expected, String expectedMessage) {
		this.expected = expected;
		this.expectedMessage = expectedMessage;
	}

	@Override
	public Statement apply(Statement base, Description description) {
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
							GenericResponse response = ((WebApplicationException) t).getResponse()
									.readEntity(GenericResponse.class);
							assertThat(response.getMessages()).as("Response messages").isNotEmpty();
							assertThat(response.getMessages().get(0).getMessage()).as("Message").isEqualTo(expectedMessage);
						}
					}
				}
			}
		};
	}
}
