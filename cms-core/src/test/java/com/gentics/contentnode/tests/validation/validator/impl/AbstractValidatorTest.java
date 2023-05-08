package com.gentics.contentnode.tests.validation.validator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.owasp.validator.html.Policy;

import com.gentics.contentnode.validation.map.PolicyMap;
import com.gentics.contentnode.validation.validator.ValidationResult;
import com.gentics.contentnode.validation.validator.Validator;

public abstract class AbstractValidatorTest<T extends Validator> {

	protected static final Policy policy;
	static {
		// is a bit slow, so we only do this once
		// (should be a read-only structure anyway).
		InputStream policyStream = PolicyMap.getDefaultAntiSamyPolicyAsInputStream();

		try {
			try {
				policy = Policy.getInstance(policyStream);
			} finally {
				policyStream.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected T validator;

	public abstract T newValidator() throws Exception;

	@Before
	public void setUp() throws Exception {
		this.validator = newValidator();
	}

	protected ValidationResult validate(String ml) throws Exception {
		return validator.validate(ml);
	}

	public ValidationResult assertPassValidation(String ml) throws Exception {
		ValidationResult result = validate(ml);

		// this check is mainly for tidy, which will strip content without generating errors.
		assertFalse("Empty string means something didn't validate", "".equals(result.getCleanMarkup()));
		assertFalse(result.toString(), result.hasErrors());
		return result;
	}

	public ValidationResult assertFailValidation(String ml) throws Exception {
		ValidationResult result = validate(ml);

		assertTrue(result.toString(), result.hasErrors());
		return result;
	}

	/**
	 * Some default tests that are run for each extending validator.
	 */
	@Test
	public void testPassValidation() throws Exception {
		// a valid link
		assertPassValidation("<p><a href='www.gentics.com'>link</a>");
		// node tags
		assertPassValidation("<p><node tag-name><p><node tag_name/>");
		// in-line styles
		assertPassValidation("<p style='font-size: 12px; background-image: url(\"img.jpg\");'>x</p>");
	}

	/**
	 * Some default tests that are run for each extending validator.
	 */
	@Test
	public void testFailValidation() throws Exception {
		// script tag is obviously invalid
		assertFailValidation("<p><script>x</script>");
		// no indirect references (parts must not be resolvable)
		assertFailValidation("<p><node tag.name>");
		// a special pattern is used that makes references that begin with "part" invalid
		assertFailValidation("<p><node partname>");
		// dangerous in-line styles
		assertFailValidation("<p style='background-image: url(\"javascript:alert()\");'>x</p>");

		// the following will report an error in AntiSamy DOM mode but in SAX mode, which is the default,
		// will only strip the attribute and generate no errors.
		ValidationResult result = validate("<a href='javascript:window.open()'>opens a new window</a>");

		assertEquals(result.toString(), "<a>opens a new window</a>", result.getCleanMarkup().replaceAll("\\n", ""));
	}
}
