package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.Strings;
import org.assertj.core.api.AbstractAssert;

import com.gentics.contentnode.publish.PublishInfo;

/**
 * Assert for {@link PublishInfo} instances
 */
public class PublishInfoAssert extends AbstractAssert<PublishInfoAssert, PublishInfo> {
	/**
	 * Create an instance
	 * 
	 * @param actual
	 *            actual item
	 */
	protected PublishInfoAssert(PublishInfo actual) {
		super(actual, PublishInfoAssert.class);
	}

	/**
	 * Assert that the publish process succeeded
	 * @return fluent API
	 */
	public PublishInfoAssert succeeded() {
		assertThat(actual.getReturnCode()).as(descriptionText() + " Return code").isEqualTo(PublishInfo.RETURN_CODE_SUCCESS);
		return this;
	}

	/**
	 * Assert that the publish process failed
	 * @return fluent API
	 */
	public PublishInfoAssert failed() {
		assertThat(actual.getReturnCode()).as(descriptionText() + " Return code").isEqualTo(PublishInfo.RETURN_CODE_ERROR);
		return this;
	}

	/**
	 * Assert that the publish process contains a message containing the given text
	 * @param text
	 * @return fluent API
	 */
	public PublishInfoAssert containsMessage(String text) {
		assertThat(actual.getMessages()).as(descriptionText() + " Messages")
				.anyMatch(m -> Strings.CI.contains(m.getMessage(), text), "Message [%s]".formatted(text));
		return this;
	}
}
