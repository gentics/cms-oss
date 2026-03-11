package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.factory.UniquifyHelper.makeFilenameUnique;
import static org.apache.commons.collections4.SetUtils.hashSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
 * Tests for {@link UniquifyHelper#makeFilenameUnique(String, Set)}
 */
@RunWith(value = Parameterized.class)
public class UniquifyFilenameTest {
	public final static int REPEAT = 101;

	@Parameters(name = "{index}: start {0}, expected {1}, obstructors {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"image.png", "image_1.png", hashSet("image.png")});
		data.add(new Object[] {"image.png", "image_2.png", hashSet("image.png", "image_1.png")});
		data.add(new Object[] {"image_1.png", "image_2.png", hashSet("image.png", "image_1.png")});
		data.add(new Object[] {"IMAGE_1.png", "IMAGE_2.png", hashSet("image.png", "image_1.png")});
		data.add(new Object[] {"image_1.png", "image_3.png", hashSet("image.png", "image_1.png", "image_2.png", "image_4.png")});
		data.add(new Object[] {"image_1.png", "image_1.png", hashSet("image.png", "image_1.jpg", "image_2.png", "image_4.png")});
		data.add(new Object[] {"image", "image_1", hashSet("image")});
		data.add(new Object[] {"image", "image_2", hashSet("image", "image_1")});
		data.add(new Object[] {"image_1", "image_2", hashSet("image", "image_1")});
		data.add(new Object[] {"IMAGE_1", "IMAGE_2", hashSet("image", "image_1")});
		data.add(new Object[] {"image_1", "image_3", hashSet("image", "image_1", "image_2", "image_4")});
		data.add(new Object[] {"abcdefghijklmnopqrstuvwxyz_abcdefghijklmnopqrstuvwxyz_abcdefghijklmnopqrstuvwxyz.txt", "abcdefghijklmnopqrstuvwxyz_abcdefghijklmnopqrstuvwxyz_abcdef.txt", hashSet()});
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
		Pattern searchPattern = Pattern.compile(UniquifyHelper.FILENAME_SEARCH_PATTERN.apply(start), Pattern.CASE_INSENSITIVE);
		Set<String> filteredObstructors = obstructors.stream().filter(o -> searchPattern.matcher(o).matches()).collect(Collectors.toSet());

		String result = makeFilenameUnique(start, filteredObstructors);
		assertThat(result).as("Unique value").isEqualTo(expected);
	}

	@Test
	public void testMakeUniqueRepeatedly() throws NodeException {
		Set<String> obstructors = new HashSet<>();
		List<String> uniqueValues = new ArrayList<>();

		for (int i = 0; i < REPEAT; i++) {
			String unique = makeFilenameUnique(start, obstructors);
			uniqueValues.add(unique);
			obstructors.add(unique);
		}

		assertThat(uniqueValues).as("Unique values").doesNotHaveDuplicates().allMatch(s -> s.length() <= UniquifyHelper.MAX_FILENAME_LENGTH);
	}
}
