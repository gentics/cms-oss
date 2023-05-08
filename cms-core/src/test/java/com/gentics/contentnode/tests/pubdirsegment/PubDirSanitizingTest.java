package com.gentics.contentnode.tests.pubdirsegment;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT })
public class PubDirSanitizingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Node having the pub dir segments activated
	 */
	private static Node nodeWithFeature;

	/**
	 * Node not having the pub dir segments activated
	 */
	private static Node nodeWithoutFeature;

	/**
	 * Map of sanitized pub_dir's with feature
	 */
	private static Map<String, String> sanitizedWithFeature = new HashMap<>();

	/**
	 * Map of sanitized pub_dir's without feature
	 */
	private static Map<String, String> sanitizedWithoutFeature = new HashMap<>();

	static {
		sanitizedWithFeature.put("", "");
		sanitizedWithoutFeature.put("", "/");

		sanitizedWithFeature.put("/", "");
		sanitizedWithoutFeature.put("/", "/");

		sanitizedWithFeature.put("//", "");
		sanitizedWithoutFeature.put("//", "/");

		sanitizedWithFeature.put("a", "a");
		sanitizedWithoutFeature.put("a", "/a/");

		sanitizedWithFeature.put("/a", "a");
		sanitizedWithoutFeature.put("/a", "/a/");

		sanitizedWithFeature.put("a/", "a");
		sanitizedWithoutFeature.put("a/", "/a/");

		sanitizedWithFeature.put("/a/", "a");
		sanitizedWithoutFeature.put("/a/", "/a/");

		sanitizedWithFeature.put("a/a", "a-a");
		sanitizedWithoutFeature.put("a/a", "/a/a/");
	}

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		nodeWithFeature = Trx.supply(() -> update(createNode(), n -> {
			n.setPubDirSegment(true);
		}));
		nodeWithoutFeature = Trx.supply(() -> createNode());
	}

	@Parameters(name = "{index}: feature {0}, pub_dir \"{1}\"")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean feature : Arrays.asList(true, false)) {
			for (String pubDir : sanitizedWithFeature.keySet()) {
				data.add(new Object[] { feature, pubDir });
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public String pubDir;

	/**
	 * Tested node
	 */
	protected Node node;

	/**
	 * Map of sanitized pub_dir's
	 */
	protected Map<String, String> pubDirMap;

	/**
	 * Setup test data (clean old data)
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(t -> {
			for (Node n : Arrays.asList(nodeWithFeature, nodeWithoutFeature)) {
				for (Folder f : n.getFolder().getChildFolders()) {
					t.getObject(f, true).delete(true);
				}
			}
		});
		node = feature ? nodeWithFeature : nodeWithoutFeature;
		pubDirMap = feature ? sanitizedWithFeature : sanitizedWithoutFeature;
	}

	@Test
	public void testSanitizing() throws NodeException {
		Folder folder = Trx.supply(() -> update(createFolder(node.getFolder(), "Testfolder"), f -> {
			f.setPublishDir(pubDir);
		}));

		Trx.operate(() -> {
			assertThat(folder.getPublishDir()).isEqualTo(pubDirMap.get(pubDir));
		});
	}
}
