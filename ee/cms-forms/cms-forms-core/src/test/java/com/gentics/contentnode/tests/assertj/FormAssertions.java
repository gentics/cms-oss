package com.gentics.contentnode.tests.assertj;

import com.gentics.contentnode.rest.model.form.FormModel;

/**
 * Form specific assertions
 */
public class FormAssertions extends GCNAssertions {
	public static FormModelAssert assertThat(FormModel actual) {
		return new FormModelAssert(actual);
	}
}
