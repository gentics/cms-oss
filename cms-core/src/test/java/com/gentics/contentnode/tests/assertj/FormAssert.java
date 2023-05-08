package com.gentics.contentnode.tests.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Form;

/**
 * Assert for forms
 */
public class FormAssert extends PublishableNodeObjectAssert<FormAssert, Form> {

	protected FormAssert(Form actual) {
		super(actual, FormAssert.class);
	}

	public FormAssert isDeleted() throws NodeException {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isTrue();
		return this;
	}

	public FormAssert isNotDeleted() throws NodeException {
		assertThat(actual.isDeleted()).as(descriptionText() + " deleted").isFalse();
		return this;
	}
}
