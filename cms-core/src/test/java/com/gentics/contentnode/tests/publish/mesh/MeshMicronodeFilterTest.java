package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.publish.mesh.MeshMicronodePublisher;

/**
 * Test cases for parsing the micronode filter
 */
@RunWith(value = Parameterized.class)
public class MeshMicronodeFilterTest {
	@Parameter(0)
	public String filter;

	@Parameter(1)
	public String[] keynames;

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {null, new String[0]});
		data.add(new Object[] {"single", new String[] {"single"}});
		data.add(new Object[] {"separated by whitespace", new String[] {"separated", "by", "whitespace"}});
		data.add(new Object[] {"   leadingwhitespace", new String[] {"leadingwhitespace"}});
		data.add(new Object[] {"trailingwhitespace    ", new String[] {"trailingwhitespace"}});
		data.add(new Object[] {"separated, by, comma", new String[] {"separated", "by", "comma"}});
		data.add(new Object[] {", ,,superfluous,,commas, ,", new String[] {"superfluous", "commas"}});
		data.add(new Object[] {" +plus, -minus ", new String[] {"+plus", "-minus"}});
		data.add(new Object[] {"with|pipes", new String[] {"with", "pipes"}});
		return data;
	}

	@Test
	public void test() {
		assertThat(MeshMicronodePublisher.parseFilter(filter)).containsOnly(keynames);
	}
}
