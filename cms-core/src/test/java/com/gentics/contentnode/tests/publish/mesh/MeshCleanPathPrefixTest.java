package com.gentics.contentnode.tests.publish.mesh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.publish.mesh.MeshPublisher;

/**
 * Test cases for cleaning the path prefix
 */
@RunWith(value = Parameterized.class)
public class MeshCleanPathPrefixTest {
	@Parameters(name = "{index}: {0} -> {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {null, null}); // null is ok
		data.add(new Object[] {"", ""}); // empty string is ok
		data.add(new Object[] {"/", ""}); // no single /
		data.add(new Object[] {"abc", "/abc"}); // add leading slash
		data.add(new Object[] {"/def/", "/def"}); // remove trailing slash
		data.add(new Object[] {"/abc/def", "/abc/def"}); // can include slashes
		return data;
	}

	@Parameter(0)
	public String pathPrefix;

	@Parameter(1)
	public String cleanedPathPrefix;

	@Test
	public void test() {
		assertThat(MeshPublisher.cleanPathPrefix(pathPrefix)).isEqualTo(cleanedPathPrefix);
	}

}
