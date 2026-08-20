package com.gentics.contentnode.tests.nodecopy;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.copy;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertRequiredPermissions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.devtools.Synchronizer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.I18nMap;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.utility.FolderComparator;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.perm.PermissionPair;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.request.NodeCopyRequest;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.tests.utils.FormTypeConfigurationModifier;
import com.gentics.contentnode.testutils.ConfigBy;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for node copy
 */
@RunWith(Parameterized.class)
@GCNFeature(set = { Feature.FORMS, Feature.DEVTOOLS, Feature.NICE_URLS })
@ConfigBy(FormTypeConfigurationModifier.class)
public class NodeCopyTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Random random = new Random();

	private Node node;

	private Node foreignNode;

	private Template template;

	private com.gentics.contentnode.object.Folder folder;

	private static UserGroup group;

	private static SystemUser user;

	private PageLanguageCode pageLanguageCode;

	private boolean omitPageExtension;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		Synchronizer.start();
		Synchronizer.addPackage("devtool-package");
	}

	@Before
	public void setup() throws Exception {
		pageLanguageCode = PageLanguageCode.values()[random.nextInt(PageLanguageCode.values().length)];
		omitPageExtension = random.nextBoolean();

		node = create(Node.class, n -> {
			n.setHostname("hostname");
			n.setPublishDir("/");

			n.setFolder(create(Folder.class, f -> {
				f.setName("Node");
				f.setPublishDir("/");
			}).doNotSave().build());

			n.getLanguages().add(getLanguage("de"));
			n.getLanguages().add(getLanguage("en"));

			n.setPageLanguageCode(pageLanguageCode);
			n.setOmitPageExtension(omitPageExtension);
		}).build();

		operate(() -> {
			Synchronizer.addPackage(node, "devtool-package");
		});

		foreignNode = create(Node.class, n -> {
			n.setHostname("foreign.hostname");
			n.setPublishDir("/");

			n.setFolder(create(Folder.class, f -> {
				f.setName("Foreign");
				f.setPublishDir("/");
			}).doNotSave().build());
		}).build();

		folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder (generic)");
			f.setDescription("Description of the folder (generic)");
			f.setPublishDir("/dir/generic/");

			f.setNameI18n(new I18nMap().put("de", "Ordner").put("en", "Folder"));
			f.setDescriptionI18n(new I18nMap().put("de", "Beschreibung des Ordners").put("en", "Description of the folder"));
			f.setPublishDirI18n(new I18nMap().put("de", "/verzeichnis/").put("en", "/dir/"));
		}).build();

		template = create(Template.class, t -> {
			t.setName("Template");
			t.setSource("");
			t.addFolder(folder);
			t.addFolder(foreignNode.getFolder());
		}).build();

		create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");

			p.setNiceUrl("/nice/page");
			p.setAlternateUrls("/alternate/page/url", "/alternate/page/url/again");
		}).build();

		create(File.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("file.txt");
			f.setFileStream(new ByteArrayInputStream("File Contents".getBytes()));

			f.setNiceUrl("/nice/file");
			f.setAlternateUrls("/alternate/file/url", "/alternate/file/url/again");
		}).build();

		create(ImageFile.class, f -> {
			f.setFolderId(folder.getId());
			f.setName("blume.jpg");
			f.setFileStream(GenericTestUtils.getPictureResource("blume.jpg"));

			f.setNiceUrl("/nice/image");
			f.setAlternateUrls("/alternate/image/url", "/alternate/image/url/again");
		}).build();
	}

	@Parameters(name = "{index}: pages {0}, templates: {1}, files: {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean pages : Arrays.asList(false, true)) {
			for (boolean templates : Arrays.asList(false, true)) {
				for (boolean files : Arrays.asList(false, true)) {
					data.add(new Object[] { pages, templates, files });
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean copyPages;

	@Parameter(1)
	public boolean copyTemplates;

	@Parameter(2)
	public boolean copyFiles;

	@Test
	public void testCopy() throws Exception {
		Node copy = Trx.supply(() -> copy(node, copyPages, copyTemplates, copyFiles));
		assertCopy(copy);
	}

	/**
	 * Test copying a node using the REST Endpoint
	 * @throws NodeException
	 */
	@Test
	public void testCopyEndpoint() throws NodeException {
		Integer folderId = supply(() -> node.getFolder().getId());

		// get node IDs before copy
		Set<Integer> nodeIdsBeforeCopy = supply(() -> DBUtils.select("SELECT id FROM node", DBUtils.IDS));

		assertRequiredPermissions(group, user,
				() -> new NodeResourceImpl().copy(Integer.toString(node.getId()), 0,
						new NodeCopyRequest().setFiles(copyFiles).setPages(copyPages).setTemplates(copyTemplates)),
				Triple.of(PermHandler.TYPE_CONTENTNODE, 1, PermHandler.PERM_VIEW), Triple.of(PermHandler.TYPE_CONTENTNODE, 1, PermHandler.PERM_NODE_CREATE),
				Triple.of(Node.TYPE_NODE, folderId, PermHandler.PERM_VIEW));

		// get new node
		Set<Integer> nodeIdsAfterCopy = supply(() -> DBUtils.select("SELECT id FROM node", DBUtils.IDS));
		nodeIdsAfterCopy.removeAll(nodeIdsBeforeCopy);
		assertThat(nodeIdsAfterCopy).as("New node IDs").hasSize(1);
		Node newNode = supply(t -> t.getObject(Node.class, nodeIdsAfterCopy.iterator().next()));
		assertCopy(newNode);
		assertPermissions(node, newNode);
	}

	/**
	 * Assert that the copy contains data according to the copy settings
	 * @param copy node copy
	 * @throws NodeException
	 */
	protected void assertCopy(Node copy) throws NodeException {
		// assert the node
		consume(n -> {
			assertThat(copy).as("Copied node")
				.isNotNull()
				.hasFieldOrPropertyWithValue("pageLanguageCode", pageLanguageCode)
				.hasFieldOrPropertyWithValue("omitPageExtension", omitPageExtension);
			assertThat(copy.getLanguages()).as("Languages of copy").containsExactly(getLanguage("de"), getLanguage("en"));

			assertThat(Synchronizer.getPackages(n)).as("Packages of copy").containsOnly("devtool-package");
		}, copy);

		// assert copied folders
		List<Folder> folders = Trx.supply(() -> copy.getFolder().getChildFolders());
		assertTrue("Copy must have a subfolder", folders.size() == 1);

		consume(f -> {
			assertThat(f)
				.as("Copied folder")
				.hasFieldOrPropertyWithValue("name", "Folder (generic)")
				.hasFieldOrPropertyWithValue("description", "Description of the folder (generic)")
				.hasFieldOrPropertyWithValue("publishDir", "/dir/generic/")
				.hasFieldOrPropertyWithValue("nameI18n", new I18nMap().put("de", "Ordner").put("en", "Folder"))
				.hasFieldOrPropertyWithValue("descriptionI18n", new I18nMap().put("de", "Beschreibung des Ordners").put("en", "Description of the folder"))
				.hasFieldOrPropertyWithValue("publishDirI18n", new I18nMap().put("de", "/verzeichnis/").put("en", "/dir/"));
		}, folders.get(0));

		// assert copied pages
		List<Page> pages = Trx.supply(() -> folders.get(0).getPages());
		if (copyPages) {
			assertTrue("Pages must have been copied", pages.size() == 1);
			consume(p -> {
				assertThat(p).as("Copied page")
					.isNotNull()
					.hasFieldOrPropertyWithValue("niceUrl", "/nice/page")
					.hasFieldOrPropertyWithValue("alternateUrls", new HashSet<>(Arrays.asList("/alternate/page/url", "/alternate/page/url/again")));
			}, pages.get(0));
		} else {
			assertTrue("Pages must not have been copied", pages.size() == 0);
		}

		// assert copied files
		List<File> files = Trx.supply(() -> folders.get(0).getFiles());
		if (copyFiles) {
			assertTrue("Files must have been copied", files.size() == 1);
			consume(f -> {
				assertThat(f).as("Copied file")
					.isNotNull()
					.hasFieldOrPropertyWithValue("niceUrl", "/nice/file")
					.hasFieldOrPropertyWithValue("alternateUrls", new HashSet<>(Arrays.asList("/alternate/file/url", "/alternate/file/url/again")));
			}, files.get(0));
		} else {
			assertTrue("Files must not have been copied", files.size() == 0);
		}

		// assert copied images
		List<ImageFile> images = Trx.supply(() -> folders.get(0).getImages());
		if (copyFiles) {
			assertTrue("Images must have been copied", images.size() == 1);
			consume(i -> {
				assertThat(i).as("Copied image")
					.isNotNull()
					.hasFieldOrPropertyWithValue("niceUrl", "/nice/image")
					.hasFieldOrPropertyWithValue("alternateUrls", new HashSet<>(Arrays.asList("/alternate/image/url", "/alternate/image/url/again")));
			}, images.get(0));
		} else {
			assertTrue("Images must not have been copied", images.size() == 0);
		}

		// assert copied templates
		List<Template> templates = Trx.supply(() -> folders.get(0).getTemplates());
		assertTrue("Template must be assigned", templates.size() == 1);
		if (copyTemplates) {
			assertFalse("Template must be a copy", templates.get(0).getId().equals(template.getId()));
		} else {
			assertTrue("Template must not be a copy", templates.get(0).getId().equals(template.getId()));
		}

		// assert templates linked to foreign node
		List<Template> foreignTemplates = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.dirtObjectCache(Folder.class, foreignNode.getFolder().getId());
			return t.getObject(foreignNode.getFolder()).getTemplates();
		});

		assertEquals("Check # of templates in foreign node", 1, foreignTemplates.size());
	}

	/**
	 * Assert that the test user has identical permission on the original and copy
	 * @param original
	 * @param copy
	 * @throws NodeException
	 */
	protected void assertPermissions(Node original, Node copy) throws NodeException {
		PermissionPair originalNodePermissions = supply(user, t -> t.getPermHandler().getPermissions(Node.TYPE_NODE, original.getFolder().getId(), 0, 0));
		PermissionPair copyNodePermissions = supply(user, t -> t.getPermHandler().getPermissions(Node.TYPE_NODE, copy.getFolder().getId(), 0, 0));

		assertThat(copyNodePermissions).as("Permissions on copy of the node").isEqualTo(originalNodePermissions);

		assertSubfolderPermissions(supply(() -> original.getFolder()), supply(() -> copy.getFolder()));
	}

	/**
	 * Assert that the test user has identical permission on all subfolders of the original folder and its copy.
	 * This also asserts that both folders have the same number of children.
	 * Assertion will be done recursively
	 * @param original original folder
	 * @param copy folder copy
	 * @throws NodeException
	 */
	protected void assertSubfolderPermissions(Folder original, Folder copy) throws NodeException {
		List<Folder> originalChildren = supply(() -> {
			List<Folder> children = new ArrayList<>(original.getChildFolders());
			Collections.sort(children, new FolderComparator("name", "asc"));
			return children;
		});
		List<Folder> copyChildren = supply(() -> {
			List<Folder> children = new ArrayList<>(copy.getChildFolders());
			Collections.sort(children, new FolderComparator("name", "asc"));
			return children;
		});
		assertThat(copyChildren.size()).as("Number of copied folders").isEqualTo(originalChildren.size());

		for (int i = 0; i < originalChildren.size(); i++) {
			Folder originalChild = originalChildren.get(i);
			Folder copyChild = copyChildren.get(i);

			PermissionPair originalFolderPermissions = supply(user, t -> t.getPermHandler().getPermissions(Folder.TYPE_FOLDER, originalChild.getId(), 0, 0));
			PermissionPair copyFolderPermissions = supply(user, t -> t.getPermHandler().getPermissions(Folder.TYPE_FOLDER, copyChild.getId(), 0, 0));
			assertThat(copyFolderPermissions).as("Permissions on copy of the folder").isEqualTo(originalFolderPermissions);

			assertSubfolderPermissions(originalChild, copyChild);
		}
	}
}
