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
 * Test cases for removing the publish directory from a path
 */
@RunWith(value = Parameterized.class)
public class MeshRemovePublishDirTest {
	@Parameters(name = "{index}: {0} without {1} -> {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"/welcome.jpg", "", "/welcome.jpg"}); // empty publish dir
		data.add(new Object[] {"/welcome.jpg", "/", "/welcome.jpg"}); // root publish dir
		data.add(new Object[] {"/pubdir/folder/image.jpg", "pubdir", "/folder/image.jpg"}); // publish dir without slashes
		data.add(new Object[] {"/pubdir/folder/image.jpg", "/pubdir/", "/folder/image.jpg"}); // publish dir with leading and trailing slash
		data.add(new Object[] {"/a/b/folder/image.jpg", "a/b", "/folder/image.jpg"}); // publish dir with multiple segments
		data.add(new Object[] {"/pubdir/pubdir/image.jpg", "pubdir", "/pubdir/image.jpg"}); // publish dir occurs again in the path
		data.add(new Object[] {"/images/image.jpg", "image", "/images/image.jpg"}); // publish dir is only a prefix of the first segment
		data.add(new Object[] {"/a.c/abc/image.jpg", "a.c", "/abc/image.jpg"}); // publish dir is not a regular expression
		data.add(new Object[] {"/folder/image.jpg", "pubdir", "/folder/image.jpg"}); // path does not start with the publish dir
		return data;
	}

	@Parameter(0)
	public String path;

	@Parameter(1)
	public String publishDir;

	@Parameter(2)
	public String expected;

	@Test
	public void test() {
		assertThat(MeshPublisher.removePublishDir(path, publishDir)).isEqualTo(expected);
	}
}
