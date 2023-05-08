package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.contentnode.object.Value;

/**
 * Assert for values
 */
public class ValueAssert extends AbstractNodeObjectAssert<ValueAssert, Value> {
	/**
	 * Create an instance
	 * @param actual actual value
	 */
	protected ValueAssert(Value actual) {
		super(actual, ValueAssert.class);
	}

	/**
	 * Assert valueText
	 * @param valueText valueText
	 * @return fluent API
	 */
	public ValueAssert hasText(String valueText) {
		assertThat(actual.getValueText()).as(descriptionText() + " valueText").isEqualTo(valueText);
		return this;
	}
}
