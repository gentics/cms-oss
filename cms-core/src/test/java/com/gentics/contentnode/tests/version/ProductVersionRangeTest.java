package com.gentics.contentnode.tests.version;

import com.gentics.contentnode.version.CmpProductVersion;
import com.gentics.contentnode.version.ProductVersionRange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link CmpProductVersion#inRange(ProductVersionRange)}.
 */
@RunWith(Parameterized.class)
public class ProductVersionRangeTest {

	@Parameters(name = "{index} version: {0}, min: {1}, max: {2}, match: {3}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			new Object[] { "1.2.3", "1.1.1", "2.2.2", true },
			new Object[] { "1.0.3", "1.1.1", "2.2.2", false },
			new Object[] { "3.2.3", "1.1.1", "2.2.2", false },
			new Object[] { "1.2.3-SNAPSHOT", "1.1.1", "2.2.2", true },
			new Object[] { "1.0.0", "2.0.0", "1.0.0", false },
			new Object[] { "1.2", "1.1.0", "1.1.9", false },
			new Object[] { "1.2", "1.3", "1.3", false }
		);
	}

	@Parameter
	public String version;

	@Parameter(1)
	public String min;

	@Parameter(2)
	public String max;

	@Parameter(3)
	public boolean expectMatch;

	@Test
	public void testInRange() {
		assertThat(new CmpProductVersion(version).inRange(new ProductVersionRange().setMinVersion(min).setMaxVersion(max)))
			.as("In range")
			.isEqualTo(expectMatch);
	}
}
