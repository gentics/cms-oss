package com.gentics.contentnode.tests.publish.filesystem;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishFS;
import static org.junit.Assert.assertTrue;

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
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for publishing objects into the filesystem
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTITHREADED_PUBLISHING, Feature.NICE_URLS })
public class FilesystemPublishTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node node, otherNode = null;
	protected static Template template;

	@Parameter(0)
	public TestedType type;

	@Parameter(1)
	public boolean withLanguage;

	@Parameter(2)
	public PageLanguageCode pageLanguageCode;

	@Parameter(3)
	public boolean omitPageExtension;

	protected Folder folder, otherFolder = null;
	protected LocalizableNodeObject<?> testedObject = null;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: type {0}, language {1}, pageLanguageCode {2}, omitPageExtension {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType type : Arrays.asList(TestedType.page, TestedType.image, TestedType.file)) {
			for (boolean language : Arrays.asList(true, false)) {
				// languages make only sense for pages
				if (type != TestedType.page && language) {
					continue;
				}
				for (PageLanguageCode pageLanguageCode : PageLanguageCode.values()) {
					for (boolean omitPageExtension : Arrays.asList(true, false)) {
						data.add(new Object[] { type, language, pageLanguageCode, omitPageExtension });
					}
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("hostname", "Node name", PublishTarget.FILESYSTEM, getLanguage("de"), getLanguage("en")));
		otherNode = supply(() -> createNode("otherhost", "Other Node name", PublishTarget.FILESYSTEM));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Get the test context. Since static class members cannot be overridden, but
	 * only hidden in subclasses, it is necessary to use this method in all test
	 * methods to get the static test context. Subclasses must override this method
	 * to return their own static test context
	 * 
	 * @return test context
	 */
	public DBTestContext getTestContext() {
		return testContext;
	}

	@Before
	public void prepareData() throws Exception {
		// set the SEO URL settings to the node
		node = update(node, n -> {
			n.setHostname("hostname");
			n.setPageLanguageCode(pageLanguageCode);
			n.setOmitPageExtension(omitPageExtension);
		});

		folder = supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder1");
			f.setPublishDir("/folder1");
		}));

		otherFolder = supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder2");
			f.setPublishDir("/folder2");
		}));

		// create the tested object
		testedObject = supply(() -> type.create(folder, template));
		if (testedObject instanceof Page) {
			testedObject = update((Page) testedObject, p -> {
				preparePage(p);
				p.publish();
			});
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}
	}

	@After
	public void clean() throws NodeException {
		operate(() -> {
			clear(node);
			clear(otherNode);
		});
	}

	/**
	 * Method to prepare the page, before it is published the first time
	 * @param page editable page
	 * @throws Exception
	 */
	protected void preparePage(Page page) throws NodeException {
		if (withLanguage) {
			page.setLanguage(getLanguage("de"));
		}
	}

	@Test
	public void testPublish() throws Exception {
		// assert
		try (Trx trx = new Trx()) {
			assertPublishFS(getTestContext().getPubDir(), testedObject, node, true);
			trx.success();
		}
	}

	@Test
	public void testMoveToFolder() throws Exception {
		// move to other folder
		try (Trx trx = new Trx()) {
			if (testedObject instanceof Page) {
				assertTrue("Moving object failed", ((Page) testedObject).move(otherFolder, 0, false).isOK());
			} else if (testedObject instanceof File) {
				assertTrue("Moving object failed", ((File) testedObject).move(otherFolder, 0).isOK());
			}
			testedObject = trx.getTransaction().getObject(testedObject);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, node, true);
			trx.success();
		}
	}

	@Test
	public void testMoveToNode() throws Exception {
		// move to other node
		try (Trx trx = new Trx()) {
			if (testedObject instanceof Page) {
				assertTrue("Moving object failed", ((Page) testedObject).move(otherNode.getFolder(), 0, false).isOK());
			} else if (testedObject instanceof File) {
				assertTrue("Moving object failed", ((File) testedObject).move(otherNode.getFolder(), 0).isOK());
			}
			testedObject = trx.getTransaction().getObject(testedObject);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, otherNode, true);
			trx.success();
		}
	}

	@Test
	public void testMoveFolderToNode() throws Exception {
		// move folder to other node
		try (Trx trx = new Trx()) {
			Folder editableFolder = trx.getTransaction().getObject(folder, true);
			assertTrue("Moving of folder failed", editableFolder.move(otherNode.getFolder(), 0).isOK());
			editableFolder.save();
			folder = trx.getTransaction().getObject(folder);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, otherNode, true);
			trx.success();
		}
	}

	@Test
	public void testChangeHostname() throws Exception {
		// change hostname of node
		try (Trx trx = new Trx()) {
			Node editableNode = trx.getTransaction().getObject(node, true);
			editableNode.setHostname("modified." + editableNode.getHostname());
			editableNode.save();
			node = trx.getTransaction().getObject(node);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, node, true);
			trx.success();
		}
	}

	@Test
	public void testChangeFolderPubdir() throws Exception {
		// change pubdir of folder
		try (Trx trx = new Trx()) {
			Folder editableFolder = trx.getTransaction().getObject(folder, true);
			editableFolder.setPublishDir("/modified" + editableFolder.getPublishDir());
			editableFolder.save();
			folder = trx.getTransaction().getObject(folder);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, node, true);
			trx.success();
		}
	}

	@Test
	public void testChangeFilename() throws Exception {
		// change filename of object
		try (Trx trx = new Trx()) {
			if (testedObject instanceof Page) {
				Page editablePage = trx.getTransaction().getObject(Page.class, testedObject.getId(), true);
				editablePage.setFilename("modified-" + editablePage.getFilename());
				editablePage.save();
				editablePage.publish();
			} else if (testedObject instanceof File) {
				File editableFile = trx.getTransaction().getObject(File.class, testedObject.getId(), true);
				editableFile.setName("modified-" + editableFile.getName());
				editableFile.save();
			}
			testedObject = trx.getTransaction().getObject(testedObject);
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, node, true);
			trx.success();
		}
	}

	@Test
	public void testNiceAndAlternateURLs() throws Exception {
		try (Trx trx = new Trx()) {
			if (testedObject instanceof Page) {
				testedObject = Builder.update((Page)testedObject, p -> {
					p.setNiceUrl("/nice/page");
					p.setAlternateUrls("/alternate/page", "/another/alternate/page");
				}).save().publish().build();
			} else if (testedObject instanceof File) {
				testedObject = Builder.update((File) testedObject, f -> {
					f.setNiceUrl("/nice/file");
					f.setAlternateUrls("/alternate/file", "/another/alternate/file");
				}).save().build();
			}
			trx.success();
		}

		// publish
		try (Trx trx = new Trx()) {
			getTestContext().publish(false);
			trx.success();
		}

		// assert
		try (Trx trx = new Trx()) {
			ContentNodeTestUtils.assertPublishFS(getTestContext().getPubDir(), testedObject, node, true);
			trx.success();
		}
	}
}
