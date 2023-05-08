package com.gentics.contentnode.tests.version;

import com.gentics.contentnode.version.CmpProductVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the {@link CmpProductVersion#compareTo(CmpProductVersion)}.
 */
@RunWith(Parameterized.class)
public class CmpProductVersionTest {

	@Parameters(name = "{index} version: {0}, other: {1}, expectedResult: {2}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			new Object[] { "1.2.3", "1.2.3", 0 },
			new Object[] { "1.2.3", "1.2", 0 },
			new Object[] { "1.2", "1.2.3", 0 },
			new Object[] { "1.0.9", "1.1", -1 },
			new Object[] { "1.0.9", "1.1.0", -1 },
			new Object[] { "1.0.0-SNAPSHOT", "1.0.0", -1 },
			new Object[] { "1.1.0-SNAPSHOT", "1.0.0", 1 },
			new Object[] { "1.1", "1.0.9", 1 },
			new Object[] { "1.1.0", "1.0.9", 1 },
			new Object[] { "1.0.0", "1.0.0-SNAPSHOT", 1 },
			new Object[] { "1.0.0", "1.1.0-SNAPSHOT", -1 }
		);
	}

	@Parameter
	public String version;

	@Parameter(1)
	public String other;

	@Parameter(2)
	public int expectedResult;

	@Test
	public void testVersionComparison() {
		assertThat(new CmpProductVersion(version).compareTo(new CmpProductVersion(other)))
			.as("Compare value")
			.isEqualTo(expectedResult);
	}
}
