package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.contentnode.object.DatasourceEntry;

/**
 * Assert for datasource entries
 */
public class DatasourceEntryAssert extends AbstractNodeObjectAssert<DatasourceEntryAssert, DatasourceEntry> {
	/**
	 * Create an instance
	 * @param actual actual datasource entry
	 */
	protected DatasourceEntryAssert(DatasourceEntry actual) {
		super(actual, DatasourceEntryAssert.class);
	}

	/**
	 * Assert key
	 * @param key key
	 * @return fluent API
	 */
	public DatasourceEntryAssert hasKey(String key) {
		assertThat(actual.getKey()).as(descriptionText() + " key").isEqualTo(key);
		return this;
	}

	/**
	 * Assert value
	 * @param value value
	 * @return fluent API
	 */
	public DatasourceEntryAssert hasValue(String value) {
		assertThat(actual.getValue()).as(descriptionText() + " value").isEqualTo(value);
		return this;
	}
}
