package com.gentics.contentnode.tests.pubdirsegment;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.DisinheritUtils;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.factory.object.ObjectModificationException;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for uniqueness tests
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT, Feature.MULTICHANNELLING })
public class PubDirUniquenessTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node nodeWithFeature;

	private static Node nodeWithoutFeature;

	private static Template template;

	private Node node;

	private Node otherNode;

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		nodeWithFeature = Trx.supply(() -> update(createNode(), n -> {
			n.setPubDirSegment(true);
			n.setPublishDir("");
			n.setBinaryPublishDir("");
			n.getFolder().setPublishDir("");
			n.setHostname("with");
		}));
		nodeWithoutFeature = Trx.supply(() -> update(createNode(), n -> {
			n.setPubDirSegment(false);
			n.setPublishDir("");
			n.setBinaryPublishDir("");
			n.getFolder().setPublishDir("");
			n.setHostname("without");
		}));
		template = Trx.supply(() -> createTemplate(nodeWithFeature.getFolder(), "Template"));
	}

	@Parameters(name = "{index}: feature {0}, siblings {1}, conflicting files {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean feature : Arrays.asList(true, false)) {
			for (boolean siblings : Arrays.asList(true, false)) {
				for (boolean conflicts : Arrays.asList(true, false)) {
					data.add(new Object[] { feature, siblings, conflicts });
				}
			}
		}
		return data;
	}

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
			update(nodeWithFeature, n -> n.setPubDirSegment(true));
			update(nodeWithoutFeature, n -> n.setPubDirSegment(false));
		});
		nodeWithFeature = Trx.supply(t -> t.getObject(nodeWithFeature));
		nodeWithoutFeature = Trx.supply(t -> t.getObject(nodeWithoutFeature));

		Trx.operate(t -> {
			assertThat(nodeWithFeature.getFolder().getChildFolders()).as("List of folders in " + nodeWithFeature).isEmpty();
			assertThat(nodeWithoutFeature.getFolder().getChildFolders()).as("List of folders in " + nodeWithoutFeature).isEmpty();
		});

		node = feature ? nodeWithFeature : nodeWithoutFeature;
		otherNode = feature ? nodeWithoutFeature : nodeWithFeature;
	}

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public boolean siblings;

	@Parameter(2)
	public boolean conflicts;

	/**
	 * Test changing the pub_dir of a folder
	 * @throws NodeException
	 */
	@Test
	public void testChangePubDir() throws NodeException {
		Folder mother1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother 1");
			f.setPublishDir("mother1");
		}));

		Folder mother2 = siblings ? mother1 : Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother 2");
			f.setPublishDir("mother2");
		}));

		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother1.getId());
			f.setName("Folder 1");
			f.setPublishDir("a");
		}));

		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother2.getId());
			f.setName("Folder 1");
			f.setPublishDir("b");
		}));

		if (conflicts) {
			Trx.supply(() -> createFile(folder1, "file.txt", "Contents".getBytes()));
			Trx.supply(() -> createFile(folder2, "file.txt", "Contents".getBytes()));
		} else {
			Trx.supply(() -> createFile(folder1, "file.txt", "Contents".getBytes()));
			Trx.supply(() -> createFile(folder2, "otherfile.txt", "Contents".getBytes()));
		}

		// if the pub_dir_segment feature is used, and the folders have the same mother, the pub_dir must have been made unique
		// in that case, we do not expect an error
		String expectedPubDir = feature ? (siblings ? "a_1" : "a") : "/a/";
		boolean expectError = !feature && conflicts;

		try {
			Trx.operate(() -> update(folder2, f -> {
				f.setPublishDir("a");
			}));
			if (expectError) {
				fail("We expected setting the pub dir to fail");
			}
			Trx.operate(t -> assertThat(t.getObject(folder2).getPublishDir()).as("Changed publish directory").isEqualTo(expectedPubDir));
		} catch (ObjectModificationException e) {
			if (!expectError) {
				fail(String.format("We did not expect setting the pub dir to fail with %s", e.getMessage()));
			}
			assertThat(e.getProperty()).as("Causing property").isEqualTo("publishDir");
		}
	}

	/**
	 * Test creating a page with a specific filename
	 * @throws NodeException
	 */
	@Test
	public void testCreatePage() throws NodeException {
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder 1");
			f.setPublishDir("a");
		}));

		// folder2 is either folder1 or a subfolder of folder1
		Folder folder2 = siblings ? folder1 : Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 2");
			f.setPublishDir("a");
		}));

		if (conflicts) {
			Trx.supply(() -> create(Page.class, p -> {
				p.setFolderId(folder1.getId());
				p.setTemplateId(template.getId());
				p.setName("Conflict");
				p.setFilename("page.html");
			}));
		} else {
			Trx.supply(() -> create(Page.class, p -> {
				p.setFolderId(folder1.getId());
				p.setTemplateId(template.getId());
				p.setName("No Conflict");
				p.setFilename("otherpage.html");
			}));
		}

		Page page = Trx.supply(() -> create(Page.class, p -> {
			p.setFolderId(folder2.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("page.html");
		}));

		// the filename must have been made unique, if there is a conflicting page in the same folder, or in the other folder and the feature is not used
		String expectedFilename = (!feature && conflicts) || (feature && siblings && conflicts) ? "page1.html" : "page.html";
		Trx.operate(t -> assertThat(t.getObject(page).getFilename()).as("Page filename").isEqualTo(expectedFilename));
	}

	/**
	 * Test creating a file with a specific filename
	 * @throws NodeException
	 */
	@Test
	public void testCreateFile() throws NodeException {
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder 1");
			f.setPublishDir("a");
		}));

		// folder2 is either folder1 or a subfolder of folder1
		Folder folder2 = siblings ? folder1 : Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 2");
			f.setPublishDir("a");
		}));

		if (conflicts) {
			Trx.supply(() -> createFile(folder1, "file.txt", "Contents".getBytes()));
		} else {
			Trx.supply(() -> createFile(folder1, "otherfile.txt", "Contents".getBytes()));
		}

		File file = Trx.supply(() -> createFile(folder2, "file.txt", "Contents".getBytes()));

		// the filename must have been made unique, if there is a conflicting page in the same folder, or in the other folder and the feature is not used
		String expectedFilename = (!feature && conflicts) || (feature && siblings && conflicts) ? "file_1.txt" : "file.txt";
		Trx.operate(t -> assertThat(t.getObject(file).getFilename()).as("File filename").isEqualTo(expectedFilename));
	}

	/**
	 * Test moving folders next to each other
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolder() throws NodeException {
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder 1");
			f.setPublishDir("a");
		}));

		Folder motherOf2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother");
			f.setPublishDir("mother");
		}));

		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(motherOf2.getId());
			f.setName("Folder 2");
			f.setPublishDir("a");
		}));

		Folder target = siblings ? motherOf2 : Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Other Mother");
			f.setPublishDir("othermother");
		}));

		if (conflicts) {
			Trx.supply(() -> createFile(folder1, "file.txt", "Contents".getBytes()));
			Trx.supply(() -> createFile(folder2, "file.txt", "Contents".getBytes()));
		}

		boolean expectFailure = feature && siblings;

		OpResult result = Trx.supply(t -> t.getObject(folder1, true).move(target, 0));
		if (expectFailure) {
			assertThat(result.isOK()).isFalse();
		} else {
			assertThat(result.isOK()).isTrue();
			assertDataConsistent();
		}
	}

	/**
	 * Test moving a folder cross node
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderCrossNode() throws NodeException {
		Folder mother1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother");
			f.setPublishDir("mother");
		}));

		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother1.getId());
			f.setName("Folder 1");
			f.setPublishDir("a");
		}));

		Folder mother2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(otherNode.getFolder().getId());
			f.setName("Mother");
			f.setPublishDir("mother");
		}));

		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother2.getId());
			f.setName("Folder 2");
			f.setPublishDir(siblings ? "a" : "b");
		}));

		if (conflicts) {
			Trx.supply(() -> createFile(folder1, "file.txt", "Contents".getBytes()));
			Trx.supply(() -> createFile(folder2, "file.txt", "Contents".getBytes()));
		}

		boolean expectFailure = (feature && siblings && conflicts) || (!feature && siblings);
		OpResult result = Trx.supply(t -> t.getObject(folder1, true).move(mother2, 0));

		if (expectFailure) {
			assertThat(result.isOK()).isFalse();
		} else {
			assertThat(result.isOK()).isTrue();
			assertDataConsistent();
		}
	}

	/**
	 * Test moving a folder structure cross node
	 * @throws NodeException
	 */
	@Test
	public void testMoveFolderStructureCrossNode() throws NodeException {
		Folder mother1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother");
			f.setPublishDir("mother");
		}));
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother1.getId());
			f.setName("Folder 1");
			f.setPublishDir("source");
		}));
		Folder folder1_1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 1.1");
			f.setPublishDir("a");
		}));
		Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 1.2");
			f.setPublishDir(siblings ? "a" : "b");
		}));

		Folder mother2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(otherNode.getFolder().getId());
			f.setName("Mother");
			f.setPublishDir("mother");
		}));
		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother2.getId());
			f.setName("Folder 2");
			f.setPublishDir("target");
		}));
		Folder folder2_1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder2.getId());
			f.setName("Folder 2.1");
			f.setPublishDir("a");
		}));
		Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder2.getId());
			f.setName("Folder 2.2");
			f.setPublishDir(siblings ? "a" : "b");
		}));

		if (conflicts) {
			Trx.supply(() -> createFile(folder1_1, "file.txt", "Contents".getBytes()));
			Trx.supply(() -> createFile(folder2_1, "file.txt", "Contents".getBytes()));
		}

		boolean expectFailure = (feature && conflicts) || (!feature && siblings);
		OpResult result = Trx.supply(t -> t.getObject(folder1, true).move(mother2, 0));

		if (expectFailure) {
			assertThat(result.isOK()).isFalse();
		} else {
			assertThat(result.isOK()).isTrue();
			assertDataConsistent();
		}
	}

	/**
	 * Test changing the node setting
	 * @throws NodeException
	 */
	@Test
	public void testChangeNodeSetting() throws NodeException {
		Folder mother1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother 1");
			f.setPublishDir("mother1");
		}));
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother1.getId());
			f.setName("Folder 1");
			f.setPublishDir("a");
		}));

		Folder mother2 = siblings ? mother1 : Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Mother 2");
			f.setPublishDir("mother2");
		}));
		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(mother2.getId());
			f.setName("Folder 2");
			f.setPublishDir("a");
		}));

		if (conflicts) {
			Trx.supply(() -> createFile(folder1, "file.txt", "Contents".getBytes()));
			Trx.supply(() -> createFile(folder2, "file.txt", "Contents".getBytes()));
		}

		// we expect this to fail, when
		// a) we are turning the feature on (was off) and the folders are siblings (because the folders would have the same segment then)
		// b) we are turning the feature off (was on) and the folders are no siblings but contain conflicting files (because the files would then have the same paths)
		boolean expectFailure = (!feature && siblings) || (feature && conflicts && !siblings);
		try {
			node = Trx.supply(() -> update(node, n -> n.setPubDirSegment(!n.isPubDirSegment())));
			if (expectFailure) {
				fail("Changing to pub_dir_segments was expected to fail");
			}

			nodeWithFeature = Trx.supply(t -> t.getObject(nodeWithFeature));
			nodeWithoutFeature = Trx.supply(t -> t.getObject(nodeWithoutFeature));
			assertDataConsistent();
		} catch (NodeException e) {
			if (!expectFailure) {
				fail("Changing to pub_dir_segments unexpectedly failed", e);
			}
		}
	}

	/**
	 * Assert that the data in the nodes is consistent
	 * @throws NodeException
	 */
	protected void assertDataConsistent() throws NodeException {
		Trx.operate(t -> {
			for (Node node : Arrays.asList(nodeWithFeature, nodeWithoutFeature)) {
				try (ChannelTrx cTrx = new ChannelTrx(node)) {
					for (Folder f : node.getFolder().getChildFolders()) {
						assertDataConsistent(f, node.isPubDirSegment());
					}
				}
			}
		});
	}

	/**
	 * Recursive method to check data consistency in the folder.
	 * @param folder folder
	 * @param pubDirSegment true when node uses pub dir segments
	 * @throws NodeException
	 */
	protected void assertDataConsistent(Folder folder, boolean pubDirSegment) throws NodeException {
		ChannelTreeSegment segment = new ChannelTreeSegment(folder, false);
		// assert uniqueness of the folder name
		assertThat(DisinheritUtils.isPropertyAvailable(folder, f -> f.getObject().getName(), "name", segment))
				.as(String.format("Name available for %s", folder)).isTrue();
		if (pubDirSegment) {
			// assert uniqueness of the pub dir segment
			assertThat(DisinheritUtils.isPropertyAvailable(folder, f -> f.getObject().getPublishDir(), "pub_dir", segment))
					.as(String.format("PubDir available for %s", folder)).isTrue();
		}

		// assert correctly cleaned pub dir
		assertThat(folder.getPublishDir()).as(String.format("PubDir of %s", folder))
				.isEqualTo(FolderFactory.cleanPubDir(folder.getPublishDir(), pubDirSegment, true));

		for (Page page : folder.getPages()) {
			segment = new ChannelTreeSegment(page, false);
			assertThat(DisinheritUtils.isPropertyAvailable(page, p -> p.getObject().getName(), "name", segment))
					.as(String.format("Name available for %s", page)).isTrue();
			assertThat(DisinheritUtils.isPropertyAvailable(page, p -> p.getObject().getFilename(), "filename", segment))
					.as(String.format("Filename available for %s", page)).isTrue();
		}
		for (File file : folder.getFilesAndImages()) {
			segment = new ChannelTreeSegment(file, false);
			assertThat(DisinheritUtils.isPropertyAvailable(file, f -> f.getObject().getName(), "name", segment))
					.as(String.format("Name available for %s", file)).isTrue();
		}

		for (Folder child : folder.getChildFolders()) {
			assertDataConsistent(child, pubDirSegment);
		}
	}
}
