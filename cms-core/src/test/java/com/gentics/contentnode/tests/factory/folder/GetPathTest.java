package com.gentics.contentnode.tests.factory.folder;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.factory.object.FolderFactory;

/**
 * Test cases for {@link FolderFactory#getPath(String, String, boolean)}
 */
@RunWith(value = Parameterized.class)
public class GetPathTest {
	@Parameters(name = "{index}: getPath(\"{0}\", \"{1}\", {2}) -> \"{3}\"")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		// edge cases (empty, null)
		data.add(new Object[] {null, null, false, "/"});
		data.add(new Object[] {null, null, true, "/"});
		data.add(new Object[] {"", "", false, "/"});
		data.add(new Object[] {"", "", true, "/"});

		// cases with "/"
		data.add(new Object[] {"/", "/", false, "/"});
		data.add(new Object[] {"/", "/", true, "/"});
		data.add(new Object[] {"/bla", "/", false, "/bla"});
		data.add(new Object[] {"/bla", "/", true, "/bla/"});
		data.add(new Object[] {"/", "/bla", false, "/bla"});
		data.add(new Object[] {"/", "/bla", true, "/bla/"});

		// avoid "//"
		data.add(new Object[] {"/bla/", "/blubb/", true, "/bla/blubb/"});

		// check start-slashes
		data.add(new Object[] {"bla", "blubb", false, "/bla/blubb"});
		data.add(new Object[] {"bla", "blubb", true, "/bla/blubb/"});

		// normal cases
		data.add(new Object[] {"/bli", "/bla/blubb", false, "/bli/bla/blubb"});
		data.add(new Object[] {"/bli", "/bla/blubb", true, "/bli/bla/blubb/"});
		data.add(new Object[] {"/bli", "/bla/blubb/", false, "/bli/bla/blubb/"});
		data.add(new Object[] {"/bli", "/bla/blubb/", true, "/bli/bla/blubb/"});

		return data;
	}

	@Parameter(0)
	public String nodePubDir;

	@Parameter(1)
	public String folderPubDir;

	@Parameter(2)
	public boolean endSlash;

	@Parameter(3)
	public String expectedResult;

	@Test
	public void test() {
		assertThat(FolderFactory.getPath(nodePubDir, folderPubDir, endSlash)).as("Path").isEqualTo(expectedResult);
	}
}
