package com.gentics.contentnode.tests.validation.validator.impl;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.contentnode.validation.validator.impl.AttributePolicy;
import com.gentics.contentnode.validation.validator.impl.AttributeValidator;

public class AttributeValidatorTest extends AbstractValidatorTest<AttributeValidator> {

	protected class MockAttributePolicy extends AttributePolicy {
		public MockAttributePolicy() {
			this.convertNodeTags = true;
			this.domMode = false;
			this.occursIn = new MockOccursIn("a", "href");
		}
		protected class MockOccursIn extends OccursIn {
			protected MockOccursIn(String element, String attribute) {
				this.element = element;
				this.attribute = attribute;
			}
		}
	}
	
	@Override
	public AttributeValidator newValidator() throws Exception {
		return new AttributeValidator(new MockAttributePolicy(), policy, Locale.getDefault());
	}
	
	// override since can't use the default tests since we use completetly
	// different input for testing attributes.
	@Override
	public void testPassValidation() throws Exception {
		String url = "http://www.gentics.com";
		ValidationResult result = assertPassValidation(url);

		assertEquals("Attribute value should pass validation unchanged,", url, result.getCleanMarkup());
	}
	
	@Override
	public void testFailValidation() throws Exception {
		assertFailValidation("javascript:window.open()");
	}
}
