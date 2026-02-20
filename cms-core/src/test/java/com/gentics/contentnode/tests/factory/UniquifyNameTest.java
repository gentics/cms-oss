package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.factory.UniquifyHelper.makeNameUnique;
import static org.apache.commons.collections4.SetUtils.hashSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;

/**
 * Tests for {@link UniquifyHelper#makeNameUnique(String, Set)e}
 */
@RunWith(value = Parameterized.class)
public class UniquifyNameTest {
	public final static Locale LOCALE = new Locale("en", "EN");

	@Parameters(name = "{index}: start {0}, expected {1}, obstructors {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"image", "image 1", hashSet("image")});
		data.add(new Object[] {"image", "image 2", hashSet("image", "image 1")});
		data.add(new Object[] {"Copy of image", "Copy of image 1", hashSet("Copy of image")});
		data.add(new Object[] {"IMAGE", "IMAGE 1", hashSet("image")});
		return data;
	}

	@Parameter(0)
	public String start;

	@Parameter(1)
	public String expected;

	@Parameter(2)
	public Set<String> obstructors;

	@Test
	public void testMakeUnique() throws NodeException {
		String result = makeNameUnique(start, obstructors);
		assertThat(result).as("Unique value").isEqualTo(expected);
	}
}
