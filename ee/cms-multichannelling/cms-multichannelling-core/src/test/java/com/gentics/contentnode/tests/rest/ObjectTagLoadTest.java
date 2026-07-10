package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFileResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getFolderResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getImageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.rest.model.ObjectTag;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.response.FileLoadResponse;
import com.gentics.contentnode.rest.model.response.FolderLoadResponse;
import com.gentics.contentnode.rest.model.response.ImageLoadResponse;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.TemplateLoadResponse;
import com.gentics.contentnode.rest.resource.impl.TemplateResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for loading object tags
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class ObjectTagLoadTest {
	public final static String EMBEDDED_OBJECT_TAG_NAME = "embedded";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Node channel;

	private static Node otherNode;

	private static Integer constructId;

	private static Template template;

	private static Page page;

	private static Folder folder;

	private static File file;

	private static File image;

	private static Template prefilledTemplate;

	private static Page prefilledPage;

	private static Folder prefilledFolder;

	private static File prefilledFile;

	private static File prefilledImage;

	private static Template prefilledTemplateChannel;

	private static Page prefilledPageChannel;

	private static Folder prefilledFolderChannel;

	private static File prefilledFileChannel;

	private static File prefilledImageChannel;

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		// delete existing object tag definitions
		operate(t -> {
			for (ObjectTagDefinition def : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS), true)) {
				for (com.gentics.contentnode.object.ObjectTag tag : def.getObjectTags()) {
					tag.delete();
				}
				def.delete();
			}
		});

		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "channel", "channel", "/"));
		otherNode = supply(() -> createNode());

		// create template
		template = supply(() -> create(Template.class, create -> {
			create.setMlId(1);
			create.setName("Template");
			create.setSource("Template source");
			create.getFolders().add(node.getFolder());
		}));

		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "construct", "part"));

		page = supply(() -> createPage(node.getFolder(), template, "Page"));
		folder = supply(() -> createFolder(node.getFolder(), "Folder"));
		file = supply(() -> createFile(node.getFolder(), "file.txt", "File contents".getBytes()));
		byte[] imageData = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));
		image = supply(() -> createImage(node.getFolder(), "blume.jpg", imageData));

		ObjectTagDefinitionCategory cat1 = category("Cat 1", 3);
		ObjectTagDefinitionCategory cat2 = category("Cat 2", 2);
		ObjectTagDefinitionCategory cat3 = category("Cat 3", 1);

		Set<ObjectTagDefinition> restricted = new HashSet<>();
		for (int type : Arrays.asList(Page.TYPE_PAGE, Folder.TYPE_FOLDER, File.TYPE_FILE, ImageFile.TYPE_IMAGE, Template.TYPE_TEMPLATE)) {
			int counter = 0;
			for (ObjectTagDefinitionCategory cat : Arrays.asList(cat1, cat2, cat3, null)) {
				int i = counter++;
				def(cat, type, String.format("object.def%d", i), String.format("Object Tag %d", i), String.format("Description for Object Tag %d", i), true, false);
				i = counter++;
				def(cat, type, String.format("object.def%d", i), String.format("Object Tag %d", i), String.format("Description for Object Tag %d", i), false, true);
				i = counter++;
				def(cat, type, String.format("object.def%d", i), String.format("Object Tag %d", i), String.format("Description for Object Tag %d", i), false, false, node);
				i = counter++;
				def(cat, type, String.format("object.def%d", i), String.format("Object Tag %d", i), String.format("Description for Object Tag %d", i), true, true, node, otherNode);
			}
			if (type != Template.TYPE_TEMPLATE) {
				restricted.add(def(null, type, "object.restricted", "Restricted Object Tag", "Description for Restricted Object Tag", false, false));
			}
		}

		prefilledTemplate = supply(() -> update(create(Template.class, create -> {
			create.setMlId(1);
			create.setName("Template");
			create.setSource("Template source");
			create.getFolders().add(node.getFolder());
		}), ObjectTagLoadTest::embedObjectTag));
		prefilledPage = supply(() -> update(createPage(node.getFolder(), template, "Page"), ObjectTagLoadTest::embedObjectTag));
		prefilledFolder = supply(() -> update(createFolder(node.getFolder(), "Folder"), ObjectTagLoadTest::embedObjectTag));
		prefilledFile = supply(() -> update(createFile(node.getFolder(), "file.txt", "File contents".getBytes()), ObjectTagLoadTest::embedObjectTag));
		prefilledImage = supply(() -> update(createImage(node.getFolder(), "blume.jpg", imageData), ObjectTagLoadTest::embedObjectTag));

		prefilledTemplateChannel = supply(() -> update(create(Template.class, create -> {
			create.setMlId(1);
			create.setName("Template");
			create.setSource("Template source");
			create.getFolders().add(node.getFolder());
			create.setChannelInfo(channel.getId(), null);
		}), ObjectTagLoadTest::embedObjectTag));
		prefilledPageChannel = supply(() -> update(createPage(node.getFolder(), template, "Page", channel), ObjectTagLoadTest::embedObjectTag));
		prefilledFolderChannel = supply(() -> update(createFolder(node.getFolder(), "Folder", channel), ObjectTagLoadTest::embedObjectTag));
		prefilledFileChannel = supply(() -> update(createFile(node.getFolder(), "file.txt", "File contents".getBytes(), channel), ObjectTagLoadTest::embedObjectTag));
		prefilledImageChannel = supply(() -> update(createImage(node.getFolder(), "blume.jpg", imageData, channel), ObjectTagLoadTest::embedObjectTag));

		for (ObjectTagDefinition r : restricted) {
			update(r, upd -> {
				upd.getNodes().add(otherNode);
			});
		}
	}

	/**
	 * Create category
	 * @param name name
	 * @param sortOrder sort order
	 * @return category
	 * @throws NodeException
	 */
	protected static ObjectTagDefinitionCategory category(String name, int sortOrder) throws NodeException {
		return supply(() -> create(ObjectTagDefinitionCategory.class, cat -> {
			cat.setName(name, 1);
			cat.setSortorder(sortOrder);
		}));
	}

	/**
	 * Create object property definition
	 * @param cat category (may be null)
	 * @param type type
	 * @param name name
	 * @param displayName display name
	 * @param description description
	 * @param required required flag
	 * @param inheritable inheritable flag
	 * @param restricted restricted to node flag
	 * @return definition
	 * @throws NodeException
	 */
	protected static ObjectTagDefinition def(ObjectTagDefinitionCategory cat, int type, String name, String displayName, String description, boolean required,
			boolean inheritable, Node... restricted) throws NodeException {
		return supply(() -> create(ObjectTagDefinition.class, def -> {
			def.getObjectTag().setConstructId(constructId);
			def.getObjectTag().setName(name);
			def.getObjectTag().setObjType(type);
			def.getObjectTag().setRequired(required);
			def.getObjectTag().setInheritable(inheritable);
			if (cat != null) {
				def.setCategoryId(cat.getId());
			}
			def.setName(displayName, 1);
			def.setDescription(description, 1);

			if (restricted.length > 0) {
				def.getNodes().addAll(Arrays.asList(restricted));
			}
		}));
	}

	/**
	 * Get the reference ObjectTag
	 * @param name name
	 * @param displayName display name
	 * @param description description
	 * @param categoryName category name
	 * @param constructId construct ID
	 * @param sortOrder sort order
	 * @param required required flag
	 * @param inheritable inheritable flag
	 * @return reference instance
	 * @throws NodeException
	 */
	protected static ObjectTag tag(String name, String displayName, String description, String categoryName, int constructId, int sortOrder,
			boolean required, boolean inheritable) throws NodeException {
		ObjectTag tag = new ObjectTag();
		tag.setActive(false);
		tag.setCategoryName(categoryName);
		tag.setConstructId(constructId);
		tag.setDescription(description);
		tag.setDisplayName(displayName);
		tag.setName(name);
		tag.setRequired(required);
		tag.setInheritable(inheritable);
		tag.setSortOrder(sortOrder);
		tag.setType(Type.OBJECTTAG);

		return tag;
	}

	/**
	 * Get the reference Tag
	 * @param name name
	 * @param constructId construct ID
	 * @return reference instance
	 * @throws NodeException
	 */
	protected static Tag tag(String name, int constructId) throws NodeException {
		Tag tag = new Tag();
		tag.setActive(false);
		tag.setConstructId(constructId);
		tag.setName(name);
		tag.setType(Type.OBJECTTAG);

		return tag;
	}

	/**
	 * Add an "embedded" object tag to the container
	 * @param container container
	 * @throws NodeException
	 */
	protected static void embedObjectTag(ObjectTagContainer container) throws NodeException {
		container.getObjectTags().put(EMBEDDED_OBJECT_TAG_NAME,
				create(com.gentics.contentnode.object.ObjectTag.class, tag -> {
					tag.setConstructId(constructId);
					tag.setName("object." + EMBEDDED_OBJECT_TAG_NAME);
				}, false));
	}

	/**
	 * Test loading template
	 * @throws NodeException
	 */
	@Test
	public void testLoadTemplate() throws NodeException {
		operate(() -> {
			TemplateLoadResponse response = new TemplateResourceImpl().get(Integer.toString(template.getId()), null, true, false);
			assertResponseCodeOk(response);

			assertTags(response.getTemplate().getObjectTags().values());
		});
	}

	/**
	 * Test loading template with edit data
	 * @throws NodeException
	 */
	@Test
	public void testLoadTemplateFull() throws NodeException {
		operate(() -> {
			TemplateLoadResponse response = new TemplateResourceImpl().get(Integer.toString(template.getId()), null, true, true);
			assertResponseCodeOk(response);

			assertTagsFull(response.getTemplate().getObjectTags().values());
		});
	}

	/**
	 * Test loading prefilled template
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledTemplate() throws NodeException {
		operate(() -> {
			TemplateLoadResponse response = new TemplateResourceImpl().get(Integer.toString(prefilledTemplate.getId()), node.getId(), true, false);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getTemplate().getObjectTags().values());
		});
	}

	/**
	 * Test loading prefilled template in a channel
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledTemplateChannel() throws NodeException {
		operate(() -> {
			TemplateLoadResponse response = new TemplateResourceImpl().get(Integer.toString(prefilledTemplateChannel.getId()), channel.getId(), true, false);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getTemplate().getObjectTags().values());
		});
	}

	/**
	 * Test loading page
	 * @throws NodeException
	 */
	@Test
	public void testLoadPage() throws NodeException {
		operate(() -> {
			PageLoadResponse response = getPageResource().load(String.valueOf(page.getId()), true, false, false, false, false, false, false, false, false, false, null, null);
			assertResponseCodeOk(response);

			assertTags(response.getPage().getTags().values());
		});
	}

	/**
	 * Test loading page with edit data
	 * @throws NodeException
	 */
	@Test
	public void testLoadPageFull() throws NodeException {
		operate(() -> {
			PageLoadResponse response = getPageResource().load(String.valueOf(page.getId()), true, false, false, false, false, false, false, false, false, true, null, null);
			assertResponseCodeOk(response);

			assertTagsFull(response.getPage().getTags().values());
		});
	}

	/**
	 * Test loading a page with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledPage() throws NodeException {
		operate(() -> {
			PageLoadResponse response = getPageResource().load(String.valueOf(prefilledPage.getId()), true, false, false, false, false, false, false, false, false, false, null, null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getPage().getTags().values());
		});
	}

	/**
	 * Test loading a page in a channel with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledPageChannel() throws NodeException {
		operate(() -> {
			PageLoadResponse response = getPageResource().load(String.valueOf(prefilledPageChannel.getId()), true, false, false,
					false, false, false, false, false, false, false, channel.getId(), null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getPage().getTags().values());
		});
	}

	/**
	 * Test loading folder
	 * @throws NodeException
	 */
	@Test
	public void testLoadFolder() throws NodeException {
		operate(() -> {
			FolderLoadResponse response = getFolderResource().load(String.valueOf(folder.getId()), true, false, false, null, null);
			assertResponseCodeOk(response);

			assertTags(response.getFolder().getTags().values());
		});
	}

	/**
	 * Test loading folder with edit data
	 * @throws NodeException
	 */
	@Test
	public void testLoadFolderFull() throws NodeException {
		operate(() -> {
			FolderLoadResponse response = getFolderResource().load(String.valueOf(folder.getId()), true, false, true, null, null);
			assertResponseCodeOk(response);

			assertTagsFull(response.getFolder().getTags().values());
		});
	}

	/**
	 * Test loading a folder with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledFolder() throws NodeException {
		operate(() -> {
			FolderLoadResponse response = getFolderResource().load(String.valueOf(prefilledFolder.getId()), true, false, false, null, null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getFolder().getTags().values());
		});
	}

	/**
	 * Test loading a folder in a channel with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledFolderChannel() throws NodeException {
		operate(() -> {
			FolderLoadResponse response = getFolderResource().load(String.valueOf(prefilledFolderChannel.getId()), true, false,
					false, channel.getId(), null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getFolder().getTags().values());
		});
	}

	/**
	 * Test loading file
	 * @throws NodeException
	 */
	@Test
	public void testLoadFile() throws NodeException {
		operate(() -> {
			FileLoadResponse response = getFileResource().load(String.valueOf(file.getId()), true, false, null, null);
			assertResponseCodeOk(response);

			assertTags(response.getFile().getTags().values());
		});
	}

	/**
	 * Test loading file with edit data
	 * @throws NodeException
	 */
	@Test
	public void testLoadFileFull() throws NodeException {
		operate(() -> {
			FileLoadResponse response = getFileResource().load(String.valueOf(file.getId()), true, true, null, null);
			assertResponseCodeOk(response);

			assertTagsFull(response.getFile().getTags().values());
		});
	}

	/**
	 * Test loading a file with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledFile() throws NodeException {
		operate(() -> {
			FileLoadResponse response = getFileResource().load(String.valueOf(prefilledFile.getId()), true, false, null, null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getFile().getTags().values());
		});
	}

	/**
	 * Test loading a file in a channel with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledFileChannel() throws NodeException {
		operate(() -> {
			FileLoadResponse response = getFileResource().load(String.valueOf(prefilledFileChannel.getId()), true, false, channel.getId(), null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getFile().getTags().values());
		});
	}

	/**
	 * Test loading image
	 * @throws NodeException
	 */
	@Test
	public void testLoadImage() throws NodeException {
		operate(() -> {
			ImageLoadResponse response = getImageResource().load(String.valueOf(image.getId()), true, false, null, null);
			assertResponseCodeOk(response);

			assertTags(response.getImage().getTags().values());
		});
	}

	/**
	 * Test loading image with edit data
	 * @throws NodeException
	 */
	@Test
	public void testLoadImageFull() throws NodeException {
		operate(() -> {
			ImageLoadResponse response = getImageResource().load(String.valueOf(image.getId()), true, true, null, null);
			assertResponseCodeOk(response);

			assertTagsFull(response.getImage().getTags().values());
		});
	}

	/**
	 * Test loading an image with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledImage() throws NodeException {
		operate(() -> {
			ImageLoadResponse response = getImageResource().load(String.valueOf(prefilledImage.getId()), true, false, null, null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getImage().getTags().values());
		});
	}

	/**
	 * Test loading an image in a channel with a restricted OE prefilled
	 * @throws NodeException
	 */
	@Test
	public void testLoadPrefilledImageChannel() throws NodeException {
		operate(() -> {
			ImageLoadResponse response = getImageResource().load(String.valueOf(prefilledImageChannel.getId()), true, false, channel.getId(), null);
			assertResponseCodeOk(response);

			assertTagsPrefilled(response.getImage().getTags().values());
		});
	}

	/**
	 * Assert expected tags containing also edit data
	 * @param tags tags to check
	 * @throws NodeException
	 */
	protected void assertTagsFull(Collection<Tag> tags) throws NodeException {
		assertThat(tags).as("Object tags").usingElementComparatorIgnoringFields("id", "properties", "construct", "readOnly", "categoryId").containsOnly(
			tag("object.def10", "Object Tag 10", "Description for Object Tag 10", "Cat 3", constructId, 0, false, false),
			tag("object.def11", "Object Tag 11", "Description for Object Tag 11", "Cat 3", constructId, 1, true, true),
			tag("object.def8", "Object Tag 8", "Description for Object Tag 8", "Cat 3", constructId, 2, true, false),
			tag("object.def9", "Object Tag 9", "Description for Object Tag 9", "Cat 3", constructId, 3, false, true),

			tag("object.def4", "Object Tag 4", "Description for Object Tag 4", "Cat 2", constructId, 4, true, false),
			tag("object.def5", "Object Tag 5", "Description for Object Tag 5", "Cat 2", constructId, 5, false, true),
			tag("object.def6", "Object Tag 6", "Description for Object Tag 6", "Cat 2", constructId, 6, false, false),
			tag("object.def7", "Object Tag 7", "Description for Object Tag 7", "Cat 2", constructId, 7, true, true),

			tag("object.def0", "Object Tag 0", "Description for Object Tag 0", "Cat 1", constructId, 8, true, false),
			tag("object.def1", "Object Tag 1", "Description for Object Tag 1", "Cat 1", constructId, 9, false, true),
			tag("object.def2", "Object Tag 2", "Description for Object Tag 2", "Cat 1", constructId, 10, false, false),
			tag("object.def3", "Object Tag 3", "Description for Object Tag 3", "Cat 1", constructId, 11, true, true),

			tag("object.def12", "Object Tag 12", "Description for Object Tag 12", null, constructId, 12, true, false),
			tag("object.def13", "Object Tag 13", "Description for Object Tag 13", null, constructId, 13, false, true),
			tag("object.def14", "Object Tag 14", "Description for Object Tag 14", null, constructId, 14, false, false),
			tag("object.def15", "Object Tag 15", "Description for Object Tag 15", null, constructId, 15, true, true)
		);
	}

	/**
	 * Assert expected tags
	 * @param tags tags to check
	 * @throws NodeException
	 */
	protected void assertTags(Collection<Tag> tags) throws NodeException {
		assertThat(tags).as("Object tags").usingElementComparatorOnFields("active", "constructId", "name", "type").containsOnly(
			tag("object.def10", constructId),
			tag("object.def11", constructId),
			tag("object.def8", constructId),
			tag("object.def9", constructId),

			tag("object.def4", constructId),
			tag("object.def5", constructId),
			tag("object.def6", constructId),
			tag("object.def7", constructId),

			tag("object.def0", constructId),
			tag("object.def1", constructId),
			tag("object.def2", constructId),
			tag("object.def3", constructId),

			tag("object.def12", constructId),
			tag("object.def13", constructId),
			tag("object.def14", constructId),
			tag("object.def15", constructId)
		);
	}

	/**
	 * Assert expected tags for prefilled objects
	 * @param tags tags to check
	 * @throws NodeException
	 */
	protected void assertTagsPrefilled(Collection<Tag> tags) throws NodeException {
		assertThat(tags).as("Object tags").usingElementComparatorOnFields("active", "constructId", "name", "type").containsOnly(
			tag("object.def10", constructId),
			tag("object.def11", constructId),
			tag("object.def8", constructId),
			tag("object.def9", constructId),

			tag("object.def4", constructId),
			tag("object.def5", constructId),
			tag("object.def6", constructId),
			tag("object.def7", constructId),

			tag("object.def0", constructId),
			tag("object.def1", constructId),
			tag("object.def2", constructId),
			tag("object.def3", constructId),

			tag("object.def12", constructId),
			tag("object.def13", constructId),
			tag("object.def14", constructId),
			tag("object.def15", constructId),

			tag("object." + EMBEDDED_OBJECT_TAG_NAME, constructId)
		);
	}
}
