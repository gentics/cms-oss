package com.gentics.contentnode.tests.rest.util;

import static com.gentics.contentnode.rest.util.PropertySubstitutionUtil.isSingleProperty;
import static com.gentics.contentnode.rest.util.PropertySubstitutionUtil.substituteSingleProperty;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for property substitution
 */
@RunWith(value = Parameterized.class)
public class PropertySubstitutionUtilTest {
	/**
	 * Get variation data
	 * @return
	 */
	@Parameters(name = "{index}: property {0}, expected {1}, is property {2}, expected prefix {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		data.add(new Object[] {"bla", "bla", false, null});
		data.add(new Object[] {"partial: ${sys:com.gentics.contentnode.existent}", "partial: ${sys:com.gentics.contentnode.existent}", false, null});
		data.add(new Object[] {"${sys:com.gentics.contentnode.nonexistent}", "${sys:com.gentics.contentnode.nonexistent}", true, null});
		data.add(new Object[] {"${sys:com.gentics.contentnode.existent}", "This is the value of the system property", true, null});
		data.add(new Object[] {"${sys:com.gentics.contentnode.existent", "${sys:com.gentics.contentnode.existent", false, null});
		data.add(new Object[] {"${sys:with.default:-this is the default}", "this is the default", true, null});
		data.add(new Object[] {"${sys:com.gentics.contentnode.existent}", "This is the value of the system property", true, "com.gentics.contentnode.ex"});
		data.add(new Object[] {"${sys:com.gentics.contentnode.existent}", "${sys:com.gentics.contentnode.existent}", true, "com.gentics.contentnode.nonex"});

		return data;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		System.setProperty("com.gentics.contentnode.existent", "This is the value of the system property");
	}

	/**
	 * Property to substitute
	 */
	@Parameter(0)
	public String property;

	/**
	 * Expected result
	 */
	@Parameter(1)
	public String expected;

	/**
	 * Flag whether the property is a single property (with correct syntax)
	 */
	@Parameter(2)
	public boolean isProperty;

	/**
	 * Possible expected prefix (if not blank, a filter will be used for substitution)
	 */
	@Parameter(3)
	public String prefix;

	/**
	 * Optional filter (if {@link #prefix} is set)
	 */
	protected Predicate<String> filter;

	@Before
	public void setup() {
		if (!StringUtils.isBlank(prefix)) {
			filter = value -> {
				return StringUtils.startsWithIgnoreCase(value, prefix);
			};
		}
	}

	/**
	 * Test substitution of a single property
	 */
	@Test
	public void testSubstitution() {
		assertThat(substituteSingleProperty(property, filter)).isEqualTo(expected);
	}

	/**
	 * Test checking the predicate
	 */
	@Test
	public void testPropertyPredicate() {
		if (isProperty) {
			assertThat(isSingleProperty(property)).contains(property);
		} else {
			assertThat(isSingleProperty(property)).isEmpty();
		}
	}
}
