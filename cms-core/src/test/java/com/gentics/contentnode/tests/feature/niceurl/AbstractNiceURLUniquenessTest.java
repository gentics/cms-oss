package com.gentics.contentnode.tests.feature.niceurl;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
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
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.object.FolderFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.NodeObjectWithAlternateUrls;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Uniqueness tests for nice URLs
 */
@GCNFeature(set = { Feature.NICE_URLS, Feature.PUBLISH_CACHE })
@RunWith(value = Parameterized.class)
public abstract class AbstractNiceURLUniquenessTest extends AbstractNiceURLTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Test template
	 */
	private static Template template;

	/**
	 * Test folder
	 */
	private Folder folder;

	@Parameter(0)
	public String nodePubDir;

	@Parameter(1)
	public String folderPubDir;

	@Parameter(2)
	public String name;

	@Parameter(3)
	public Action action;

	protected TestedType testedType;

	protected TestedType conflictType;

	@Parameters(name = "{index}: Node {0}, folder {1}, name {2}, {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (String nodePubDir : Arrays.asList("/", "/Content.Node")) {
			for (String folderPubDir : Arrays.asList("/", "/bla/", "/bli/bla/blubb/")) {
				for (String name : Arrays.asList("filename", "filename.ext")) {
					for (Action action : Action.values()) {
						data.add(new Object[] { nodePubDir, folderPubDir, name, action });
					}
				}
			}
		}
		return data;
	}

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransactionOrNull();
		if (t != null) {
			t.commit();
		}
		node = Trx.supply(() -> createNode());
		template = Trx.supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Create test folder
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		node = update(node, n -> {
			n.setPublishDir(nodePubDir);
			n.setBinaryPublishDir(nodePubDir);
		});
		folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Testfolder");
			f.setPublishDir(folderPubDir);
		});
	}

	/**
	 * Delete the test folder (and everything in it)
	 * @throws NodeException
	 */
	@After
	public void tearDown() throws NodeException {
		Trx.operate(() -> folder.delete(true));
		folder = null;
	}

	/**
	 * Test object with a nice URL that already exists
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithNiceURL() throws NodeException {
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting object
		NodeObjectWithAlternateUrls conflicting = createObjectWithNiceUrl(conflictType, folder1,
				template, getConflictingNiceURL());
		assertThat(conflicting.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());

		// create object with nice URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getExpectedNiceURL());
	}

	/**
	 * Test object with a nice URL in a published page (current version of the page does not create conflict)
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithPublishedNiceURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setNiceUrl(getConflictingNiceURL());
			});
			assertThat(conflicting.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict (but do not publish)
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			t.setTimestamp(2000);
			Page editableConflicting = t.getObject(conflicting, true);
			editableConflicting.setNiceUrl("");
			editableConflicting.save();
			conflicting = t.getObject(conflicting);
			trx.success();
		}

		// create object with nice URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getExpectedNiceURL());
	}

	/**
	 * Test object with a nice URL in an old version of another page (current version of the page does not create conflict)
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithOldVersionNiceURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setNiceUrl(getConflictingNiceURL());
			});
			assertThat(conflicting.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict and publish
		conflicting = update(conflicting, p -> {
			p.setNiceUrl("");
		});

		// create object with nice URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());
	}

	/**
	 * Test object with nice URL that conflicts with another object's alternate URL
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithAlternateURL() throws NodeException {
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting object
		NodeObjectWithAlternateUrls conflicting = createObjectWithAlternateUrls(conflictType, folder1,
				template, getConflictingNiceURL());
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, conflicting)).as("Alternate URLs")
				.containsExactly(getConflictingNiceURL());

		// create object with nice URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getExpectedNiceURL());
	}

	/**
	 * Test object with nice URL that conflicts with another object's alternate URL (published version)
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithPublishedAlternateURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}

		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setAlternateUrls(getConflictingNiceURL());
			});
			assertThat(conflicting.getAlternateUrls()).as("Alternate URLs").containsExactly(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict (but do not publish)
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			t.setTimestamp(2000);
			Page editableConflicting = t.getObject(conflicting, true);
			editableConflicting.setAlternateUrls();
			editableConflicting.save();
			conflicting = t.getObject(conflicting);
			trx.success();
		}

		// create object with nice URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getExpectedNiceURL());
	}

	/**
	 * Test object with nice URL that conflicts with another object's alternate URL (old version)
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithOldVersionAlternateURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}

		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setAlternateUrls(getConflictingNiceURL());
			});
			assertThat(conflicting.getAlternateUrls()).as("Alternate URLs").containsExactly(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict and publish
		conflicting = update(conflicting, p -> {
			p.setAlternateUrls();
		});

		// create object with nice URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());
	}

	/**
	 * Test object with nice URL that conflicts with another object's filename
	 * @throws NodeException
	 */
	@Test
	public void testNiceURLWithFilename() throws NodeException {
		// create conflicting object
		createObjectWithFilename(conflictType, folder, template, name);

		// create object with nice URL
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder1,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getExpectedNiceURL());
	}

	/**
	 * Test object with an alternate URL that already exists
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithNiceURL() throws NodeException {
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting object
		NodeObjectWithAlternateUrls conflicting = createObjectWithNiceUrl(conflictType, folder1,
				template, getConflictingNiceURL());
		assertThat(conflicting.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());

		// create object with alternate URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithAlternateUrls(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setAlternateUrls(getConflictingNiceURL());
			});
		}
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, testedObject)).as("Alternate URLs")
				.containsExactly(getExpectedNiceURL());
	}

	/**
	 * Test object with an alternate URL in a published page (current version of the page does not create conflict)
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithPublishedNiceURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setNiceUrl(getConflictingNiceURL());
			});
			assertThat(conflicting.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict (but do not publish)
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			t.setTimestamp(2000);
			Page editableConflicting = t.getObject(conflicting, true);
			editableConflicting.setNiceUrl("");
			editableConflicting.save();
			conflicting = t.getObject(conflicting);
			trx.success();
		}

		// create object with alternate URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithAlternateUrls(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setAlternateUrls(getConflictingNiceURL());
			});
		}
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, testedObject)).as("Alternate URLs")
				.containsExactly(getExpectedNiceURL());
	}

	/**
	 * Test object with an alternate URL in an old version of another page (current version of the page does not create conflict)
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithOldVersionNiceURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setNiceUrl(getConflictingNiceURL());
			});
			assertThat(conflicting.getNiceUrl()).as("Nice URL").isEqualTo(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict and publish
		conflicting = update(conflicting, p -> {
			p.setNiceUrl("");
		});

		// create object with alternate URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithAlternateUrls(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setAlternateUrls(getConflictingNiceURL());
			});
		}
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, testedObject)).as("Alternate URLs")
				.containsExactly(getConflictingNiceURL());
	}

	/**
	 * Test object with alternate URL that conflicts with another object's alternate URL
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithAlternateURL() throws NodeException {
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting object
		NodeObjectWithAlternateUrls conflicting = createObjectWithAlternateUrls(conflictType, folder1,
				template, getConflictingNiceURL());
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, conflicting)).as("Alternate URLs")
				.containsExactly(getConflictingNiceURL());

		// create object with alternate URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithAlternateUrls(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setAlternateUrls(getConflictingNiceURL());
			});
		}
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, testedObject)).as("Alternate URLs")
				.containsExactly(getExpectedNiceURL());
	}

	/**
	 * Test object with alternate URL that conflicts with another object's alternate URL (published version)
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithPublishedAlternateURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}

		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setAlternateUrls(getConflictingNiceURL());
			});
			assertThat(conflicting.getAlternateUrls()).as("Alternate URLs").containsExactly(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict (but do not publish)
		try (Trx trx = new Trx()) {
			Transaction t = trx.getTransaction();
			t.setTimestamp(2000);
			Page editableConflicting = t.getObject(conflicting, true);
			editableConflicting.setAlternateUrls();
			editableConflicting.save();
			conflicting = t.getObject(conflicting);
			trx.success();
		}

		// create object with alternate URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithAlternateUrls(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setAlternateUrls(getConflictingNiceURL());
			});
		}
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, testedObject)).as("Alternate URLs")
				.containsExactly(getExpectedNiceURL());
	}

	/**
	 * Test object with alternate URL that conflicts with another object's alternate URL (old version)
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithOldVersionAlternateURL() throws NodeException {
		// only pages have versions
		if (conflictType != TestedType.page) {
			return;
		}

		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		Folder folder2 = Trx.supply(() -> createFolder(folder, "Testfolder2"));

		// create conflicting page
		Page conflicting;
		try (Trx trx = new Trx()) {
			trx.getTransaction().setTimestamp(1000);
			conflicting = create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(folder1.getId());
				p.setName("Page1");
				p.setAlternateUrls(getConflictingNiceURL());
			});
			assertThat(conflicting.getAlternateUrls()).as("Alternate URLs").containsExactly(getConflictingNiceURL());

			// publish conflicting page
			update(conflicting, p -> p.publish());
			trx.success();
		}

		// resolve conflict and publish
		conflicting = update(conflicting, p -> {
			p.setAlternateUrls();
		});

		// create object with alternate URL
		NodeObjectWithAlternateUrls testedObject = createObjectWithAlternateUrls(testedType, folder2,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setAlternateUrls(getConflictingNiceURL());
			});
		}
		assertThat(execute(NodeObjectWithAlternateUrls::getAlternateUrls, testedObject)).as("Alternate URLs")
				.containsExactly(getConflictingNiceURL());
	}

	/**
	 * Test object with alternate URL that conflicts with another object's filename
	 * @throws NodeException
	 */
	@Test
	public void testAlternateURLWithFilename() throws NodeException {
		// create conflicting object
		createObjectWithFilename(conflictType, folder, template, name);

		// create object with nice URL
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		NodeObjectWithAlternateUrls testedObject = createObjectWithNiceUrl(testedType, folder1,
				template, action == Action.create ? getConflictingNiceURL() : null);
		if (action == Action.update) {
			testedObject = update(testedObject, p -> {
				p.setNiceUrl(getConflictingNiceURL());
			});
		}
		assertThat(testedObject.getNiceUrl()).as("Nice URL").isEqualTo(getExpectedNiceURL());
	}

	/**
	 * Test object with a filename that conflicts with another object 's nice URL
	 * @throws NodeException
	 */
	@Test
	public void testFilenameWithNiceURL() throws NodeException {
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		// create conflicting object
		createObjectWithNiceUrl(conflictType, folder1, template, getConflictingNiceURL());

		// create object with filename
		NodeObject testedObject = createObjectWithFilename(testedType, folder, template, action == Action.create ? name : "other");
		if (action == Action.update) {
			testedObject = update(testedObject, upd -> {
				if (upd instanceof Page) {
					((Page) upd).setFilename(name);
				} else if (upd instanceof File) {
					((File) upd).setName(name);
				}
			});
		}

		String name = null;
		if (testedObject instanceof Page) {
			name = ((Page) testedObject).getFilename();
		} else if (testedObject instanceof File) {
			name = ((File) testedObject).getName();
		}

		assertThat(name).as("Filename").isEqualTo(getExpectedFilename(testedObject instanceof File));
	}

	/**
	 * Test object with a filename that conflicts with another object 's alternate URL
	 * @throws NodeException
	 */
	@Test
	public void testFilenameWithAlternateURL() throws NodeException {
		Folder folder1 = Trx.supply(() -> createFolder(folder, "Testfolder1"));
		// create conflicting object
		createObjectWithAlternateUrls(conflictType, folder1, template, getConflictingNiceURL());

		// create object with filename
		NodeObject testedObject = createObjectWithFilename(testedType, folder, template, action == Action.create ? name : "other");
		if (action == Action.update) {
			testedObject = update(testedObject, upd -> {
				if (upd instanceof Page) {
					((Page) upd).setFilename(name);
				} else if (upd instanceof File) {
					((File) upd).setName(name);
				}
			});
		}

		String name = null;
		if (testedObject instanceof Page) {
			name = ((Page) testedObject).getFilename();
		} else if (testedObject instanceof File) {
			name = ((File) testedObject).getName();
		}

		assertThat(name).as("Filename").isEqualTo(getExpectedFilename(testedObject instanceof File));
	}

	/**
	 * Get the conflicting nice URL
	 * @return conflicting nice URL
	 */
	protected String getConflictingNiceURL() {
		return String.format("%s%s", FolderFactory.getPath(nodePubDir, folderPubDir, true), name); 
	}

	/**
	 * Get the expected nice URL (after it has been made unique)
	 * @return expected nice URL
	 */
	protected String getExpectedNiceURL() {
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex < 0) {
			return String.format("%s%s1", FolderFactory.getPath(nodePubDir, folderPubDir, true), name);
		} else {
			return String.format("%s%s1%s", FolderFactory.getPath(nodePubDir, folderPubDir, true), name.substring(0, dotIndex), name.substring(dotIndex));
		}
	}

	/**
	 * Get the expected filename (after it has been made unique)
	 * @param isFile true if the filename belongs to a file, false if it belongs to a page
	 * @return expected filename
	 */
	protected String getExpectedFilename(boolean isFile) {
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex < 0) {
			return String.format("%s%s1", name, isFile ? "_" : "");
		} else {
			return String.format("%s%s1%s", name.substring(0, dotIndex), isFile ? "_" : "", name.substring(dotIndex));
		}
	}

	/**
	 * Action, which would cause a conflict
	 */
	public static enum Action {
		create, update
	}
}
