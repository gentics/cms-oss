package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.rest.model.RolePermissionsModel;

/**
 * Assert for RolePermissionsModel
 */
public class RolePermissionsModelAssert extends AbstractAssert<RolePermissionsModelAssert, RolePermissionsModel> {
	/**
	 * Create an instance
	 * 
	 * @param actual
	 *            actual item
	 */
	protected RolePermissionsModelAssert(RolePermissionsModel actual) {
		super(actual, RolePermissionsModelAssert.class);
	}

	/**
	 * Assert that the permissions are consistent (contain no null values)
	 * @param languageIds collection of language IDs
	 * @return fluent API
	 */
	public RolePermissionsModelAssert isConsistent(Collection<Integer> languageIds) {
		assertThat(actual.getPage()).as("Page privileges").isNotNull().isConsistent();
		assertThat(actual.getFile()).as("File privileges").isNotNull().isConsistent();

		assertThat(actual.getPageLanguages()).as("Page language privileges").isNotNull().containsOnlyKeys(languageIds.toArray(new Integer[languageIds.size()]))
				.doesNotContainValue(null);
		for (Integer langId : languageIds) {
			assertThat(actual.getPageLanguages().get(langId)).as(String.format("Page language %d privileges", langId)).isNotNull().isConsistent();
		}

		return this;
	}
}
