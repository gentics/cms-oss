package com.gentics.contentnode.tests.factory;

import static com.gentics.contentnode.factory.UniquifyHelper.makePageFilenameUnique;
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
 * Tests for {@link UniquifyHelper#makePageFilenameUnique(String, Set)}
 */
@RunWith(value = Parameterized.class)
public class UniquifyPageFilenameTest {
	@Parameters(name = "{index}: start {0}, expected {1}, obstructors {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"page.html", "page1.html", hashSet("page.html")});
		data.add(new Object[] {"page.html", "page2.html", hashSet("page.html", "page1.html")});
		data.add(new Object[] {"page1.html", "page2.html", hashSet("page.html", "page1.html")});
		data.add(new Object[] {"PAGE1.html", "PAGE2.html", hashSet("page.html", "page1.html")});
		data.add(new Object[] {"page1.html", "page3.html", hashSet("page.html", "page1.html", "page2.html", "page4.html")});
		data.add(new Object[] {"page1.html", "page1.html", hashSet("page.html", "page1.txt", "page2.html", "page4.html")});
		data.add(new Object[] {"page01.html", "page02.html", hashSet("page.html", "page01.html")});
		data.add(new Object[] {"page", "page1", hashSet("page")});
		data.add(new Object[] {"page", "page2", hashSet("page", "page1")});
		data.add(new Object[] {"page1", "page2", hashSet("page", "page1")});
		data.add(new Object[] {"PAGE1", "PAGE2", hashSet("page", "page1")});
		data.add(new Object[] {"page1", "page3", hashSet("page", "page1", "page2", "page4")});
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
		Pattern searchPattern = Pattern.compile(UniquifyHelper.PAGE_FILENAME_SEARCH_PATTERN.apply(start), Pattern.CASE_INSENSITIVE);
		Set<String> filteredObstructors = obstructors.stream().filter(o -> searchPattern.matcher(o).matches()).collect(Collectors.toSet());

		String result = makePageFilenameUnique(start, filteredObstructors);
		assertThat(result).as("Unique value").isEqualTo(expected);
	}
}
