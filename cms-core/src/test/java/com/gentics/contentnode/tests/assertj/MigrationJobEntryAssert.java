package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.rest.model.response.migration.MigrationJobEntry;

/**
 * Assert for migration job entries
 */
public class MigrationJobEntryAssert extends AbstractAssert<MigrationJobEntryAssert, MigrationJobEntry> {
	/**
	 * Create an instance
	 * @param actual actual entry
	 */
	protected MigrationJobEntryAssert(MigrationJobEntry actual) {
		super(actual, MigrationJobEntryAssert.class);
	}

	/**
	 * Assert that the entry has the given status
	 * @param status status
	 * @return fluent API
	 */
	public MigrationJobEntryAssert hasStatus(int status) {
		assertThat(actual.getStatus()).as(descriptionText() + " status").isEqualTo(status);
		return myself;
	}
}
