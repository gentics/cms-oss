package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.rest.model.response.migration.MigrationJobLogEntryItem;

public class MigrationJobLogEntryItemAssert extends AbstractAssert<MigrationJobLogEntryItemAssert, MigrationJobLogEntryItem> {
	/**
	 * Create an instance
	 * @param actual actual entry
	 */
	protected MigrationJobLogEntryItemAssert(MigrationJobLogEntryItem actual) {
		super(actual, MigrationJobLogEntryItemAssert.class);
	}

	/**
	 * Assert that the entry has the given status
	 * @param status status
	 * @return fluent API
	 */
	public MigrationJobLogEntryItemAssert hasStatus(int status) {
		assertThat(actual.getStatus()).as(descriptionText() + " status").isEqualTo(status);
		return myself;
	}

	/**
	 * Assert that the entry is for the given object
	 * @param obj object
	 * @return fluent API
	 */
	public MigrationJobLogEntryItemAssert isFor(NodeObject obj) {
		assertThat(actual.getObjectType()).as(descriptionText() + " object type").isEqualTo(obj.getTType());
		assertThat(actual.getObjectId()).as(descriptionText() + " object id").isEqualTo(obj.getId());
		return myself;
	}
}
