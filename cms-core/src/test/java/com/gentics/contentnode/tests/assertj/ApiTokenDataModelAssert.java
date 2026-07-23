package com.gentics.contentnode.tests.assertj;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.rest.model.token.ApiTokenDataModel;

/**
 * Assert for instances of {@link ApiTokenDataModelM}
 */
public class ApiTokenDataModelAssert extends AbstractAssert<ApiTokenDataModelAssert, ApiTokenDataModel> {
	/**
	 * Create an instance
	 * @param actual actual object
	 */
	protected ApiTokenDataModelAssert(ApiTokenDataModel actual) {
		super(actual, ApiTokenDataModelAssert.class);
	}

	/**
	 * Assert that the model has the given name
	 * @param name expected name
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert hasName(String name) {
		assertThat(actual.getName()).as("%s name".formatted(descriptionText())).isEqualTo(name);
		return myself;
	}

	/**
	 * Assert that the model has the given user Id
	 * @param userId user ID
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert hasUserId(int userId) {
		assertThat(actual.getUserId()).as("%s userId".formatted(descriptionText())).isEqualTo(userId);
		return myself;
	}

	/**
	 * Assert that the model expires at the given time
	 * @param expires expected time
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert expiresAt(int expires) {
		assertThat(actual.getExpires()).as("%s expires".formatted(descriptionText())).isEqualTo(expires);
		return myself;
	}

	/**
	 * Assert that the model does not expire
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert doesNotExpire() {
		assertThat(actual.getExpires()).as("%s expires".formatted(descriptionText())).isLessThanOrEqualTo(0);
		return myself;
	}

	/**
	 * Assert that the model was last used at the given time
	 * @param lastUsed expected time
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert wasLastUsedAt(int lastUsed) {
		assertThat(actual.getLastUsed()).as("%s lastUsed".formatted(descriptionText())).isEqualTo(lastUsed);
		return myself;
	}

	/**
	 * Assert that the model was never used
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert wasNeverUsed() {
		assertThat(actual.getLastUsed()).as("%s lastUsed".formatted(descriptionText())).isLessThanOrEqualTo(0);
		return myself;
	}

	/**
	 * Assert that the model is valid
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert isValid() {
		assertThat(actual.isValid()).as("%s valid".formatted(descriptionText())).isTrue();
		return myself;
	}

	/**
	 * Assert that the model is not valid
	 * @return fluent API
	 */
	public ApiTokenDataModelAssert isNotValid() {
		assertThat(actual.isValid()).as("%s valid".formatted(descriptionText())).isFalse();
		return myself;
	}
}
