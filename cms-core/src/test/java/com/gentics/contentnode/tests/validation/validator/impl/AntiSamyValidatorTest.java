package com.gentics.contentnode.tests.validation.validator.impl;

import java.util.Locale;

import com.gentics.contentnode.validation.validator.impl.AbstractAntiSamyValidator;
import com.gentics.contentnode.validation.validator.impl.AntiSamyPolicy;
import com.gentics.contentnode.validation.validator.impl.AntiSamyValidator;

public class AntiSamyValidatorTest extends AbstractValidatorTest<AbstractAntiSamyValidator> {
	
	protected class MockAntiSamyPolicy extends AntiSamyPolicy {
		public MockAntiSamyPolicy() {
			this.convertNodeTags = true;
			this.domMode = false;
			// other settings are not used directly by the validator
		}
	}
	
	public AbstractAntiSamyValidator newValidator() {
		return new AntiSamyValidator(new MockAntiSamyPolicy(), policy, Locale.getDefault());
	}
	
	public void test_invalid_namespace() throws Exception {
		assertFailValidation("<p><unknown:span>x</unknown:span></p>");
	}
}
