package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.factory.UniquifyHelper.makePathSegmentUnique;
import static org.apache.commons.collections4.SetUtils.hashSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.UniquifyHelper;

/**
 * Tests for {@link UniquifyHelper#makePathSegmentUnique(String, Set)}
 */
@RunWith(value = Parameterized.class)
public class UniquifyPathSegmentTest {
	@Parameters(name = "{index}: start {0}, expected {1}, obstructors {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"image", "image_1", hashSet("image")});
		data.add(new Object[] {"image", "image_2", hashSet("image", "image_1")});
		data.add(new Object[] {"image_1", "image_2", hashSet("image", "image_1")});
		data.add(new Object[] {"IMAGE_1", "IMAGE_2", hashSet("image", "image_1")});
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
		Pattern searchPattern = Pattern.compile(UniquifyHelper.SEGMENT_SEARCH_PATTERN.apply(start),
				Pattern.CASE_INSENSITIVE);
		Set<String> filteredObstructors = obstructors.stream().filter(o -> searchPattern.matcher(o).matches())
				.collect(Collectors.toSet());

		String result = makePathSegmentUnique(start, filteredObstructors);
		assertThat(result).as("Unique value").isEqualTo(expected);
	}
}
