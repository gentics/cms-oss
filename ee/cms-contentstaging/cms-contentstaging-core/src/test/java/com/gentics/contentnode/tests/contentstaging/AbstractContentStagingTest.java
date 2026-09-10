package com.gentics.contentnode.tests.contentstaging;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.perm;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Form;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.CmsFormPartType;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.FolderURLPartType;
import com.gentics.contentnode.object.parttype.ImageURLPartType;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract test cases for content staging
 */
abstract public class AbstractContentStagingTest<T extends StageableNodeObject> implements ContentStagingMethods {
	/**
	 * Object mapper
	 */
	public static ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

	/**
	 * Database context
	 */
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static UserGroup testGroup;

	protected static SystemUser testUser;

	protected Class<T> clazz;

	protected String type;

	protected Node node;

	protected Node foreign;

	protected Template template;

	protected Construct construct;

	protected ObjectTagDefinition folderObjProp;

	protected ObjectTagDefinition pageObjProp;

	protected ObjectTagDefinition fileObjProp;

	protected ObjectTagDefinition imageObjProp;

	protected Folder nodeRootFolder;

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		testGroup = supply(() -> createUserGroup("Testgroup", NODE_GROUP_ID));
		testUser = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(testGroup)));
	}

	protected AbstractContentStagingTest(Class<T> clazz, String type) {
		this.clazz = clazz;
		this.type = type;
	}

	@Before
	public void setup() throws NodeException, IOException {
		makeClean();

		// create a test node
		node = create(Node.class, n -> {
			Folder rootFolder = create(Folder.class, f -> {
				f.setName("Staged Node");
				f.setPublishDir("/");
			}).doNotSave().build();

			n.setFolder(rootFolder);
			n.setHostname("staged.node");
			n.setPublishDir("/");
			n.getLanguages().add(getLanguage("de"));
			n.getLanguages().add(getLanguage("en"));
		}).build();

		nodeRootFolder = supply(() -> node.getFolder());

		node = update(node, n -> {
			n.activateFeature(Feature.FORMS);
		}).build();

		foreign = create(Node.class, n -> {
			Folder rootFolder = create(Folder.class, f -> {
				f.setName("Foreign Node");
				f.setPublishDir("/");
			}).doNotSave().build();

			n.setFolder(rootFolder);
			n.setHostname("foreign.node");
			n.setPublishDir("/");
		}).build();

		// create a template
		template = create(Template.class, t -> {
			t.setMlId(1);
			t.setName("Template");
			t.setSource("");
			t.addFolder(node.getFolder());
			t.addFolder(foreign.getFolder());
		}).build();

		// create a construct which can reference other objects
		construct = create(Construct.class, c -> {
			c.setName("Test Construct", 1);
			c.setKeyword("testconstruct");
			c.setAutoEnable(true);

			List<Part> parts = c.getParts();
			// page reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("page");
				p.setPartTypeId(getPartTypeId(PageURLPartType.class));
			}).doNotSave().build());

			// folder reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("folder");
				p.setPartTypeId(getPartTypeId(FolderURLPartType.class));
			}).doNotSave().build());

			// file reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("file");
				p.setPartTypeId(getPartTypeId(FileURLPartType.class));
			}).doNotSave().build());

			// image reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("image");
				p.setPartTypeId(getPartTypeId(ImageURLPartType.class));
			}).doNotSave().build());

			// form reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("form");
				p.setPartTypeId(getPartTypeId(CmsFormPartType.class));
			}).doNotSave().build());

			// normal text reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("normaltext");
				p.setPartTypeId(getPartTypeId(NormalTextPartType.class));
			}).doNotSave().build());

			// JSON text reference
			parts.add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("json");
				p.setPartTypeId(getPartTypeId(JSONPartType.class));
			}).doNotSave().build());
		}).build();

		folderObjProp = create(ObjectTagDefinition.class, o -> {
			ObjectTag tag = o.getObjectTag();
			tag.setConstructId(construct.getId());
			tag.setName("object.reference");
			tag.setObjType(Folder.TYPE_FOLDER);
		}).build();

		pageObjProp = create(ObjectTagDefinition.class, o -> {
			ObjectTag tag = o.getObjectTag();
			tag.setConstructId(construct.getId());
			tag.setName("object.reference");
			tag.setObjType(Page.TYPE_PAGE);
		}).build();

		fileObjProp = create(ObjectTagDefinition.class, o -> {
			ObjectTag tag = o.getObjectTag();
			tag.setConstructId(construct.getId());
			tag.setName("object.reference");
			tag.setObjType(com.gentics.contentnode.object.File.TYPE_FILE);
		}).build();

		imageObjProp = create(ObjectTagDefinition.class, o -> {
			ObjectTag tag = o.getObjectTag();
			tag.setConstructId(construct.getId());
			tag.setName("object.reference");
			tag.setObjType(ImageFile.TYPE_IMAGE);
		}).build();
	}

	/**
	 * Change the object to reference the given page
	 * @param object object to update
	 * @param referencedPage referenced page
	 * @return changed object
	 * @throws NodeException
	 */
	protected T setPageReference(T object, Page referencedPage) throws NodeException {
		return update(object, o -> {
			PageURLPartType partType = getPartType(PageURLPartType.class,
					((ObjectTagContainer) o).getObjectTag("reference"), "page");
			partType.setTargetPage(referencedPage);
			partType.setNode(referencedPage.getNode());
		}).build();
	};

	/**
	 * Assert that the object references the given page
	 * @param object object with reference
	 * @param referencedPage referenced page
	 * @throws NodeException
	 */
	protected void assertPageReference(T object, Page referencedPage) throws NodeException {
		assertThat(object).as("Staged object").isNotNull();
		assertThat(getPartType(PageURLPartType.class, ((ObjectTagContainer) object).getObjectTag("reference"),
				"page"))
			.as("Reference part")
			.isNotNull()
			.hasFieldOrPropertyWithValue("targetPage", referencedPage)
			.hasFieldOrPropertyWithValue("node", referencedPage != null ? referencedPage.getNode() : null);
	}

	/**
	 * Get the {@link Triple} defining the read permission for the given instance
	 * @param instance instance
	 * @return read permission Triple
	 * @throws NodeException
	 */
	protected Triple<Integer, Integer, Integer> readPerm(T instance) throws NodeException {
		if (instance instanceof Folder) {
			return perm(instance, PermType.read);
		} else if (instance instanceof Form) {
			return perm(execute(NodeObject::getParentObject, instance), PermType.viewform);
		} else {
			return perm(execute(NodeObject::getParentObject, instance), PermType.readitems);
		}
	}

	/**
	 * Get the {@link Triple} defining the permission to read items of the tested class in the folder
	 * @param folder folder
	 * @return read item permission Triple
	 * @throws NodeException
	 */
	protected Triple<Integer, Integer, Integer> readItemPerm(Folder folder) throws NodeException {
		if (Folder.class.isAssignableFrom(clazz)) {
			return null;
		} else if (Form.class.isAssignableFrom(clazz)) {
			return perm(folder, PermType.viewform);
		} else {
			return perm(folder, PermType.readitems);
		}
	}

	/**
	 * Get the {@link Triple} defining the permission to update the given instance
	 * @param instance instance
	 * @return update permissions triple
	 * @throws NodeException
	 */
	protected Triple<Integer, Integer, Integer> updatePerm(T instance) throws NodeException {
		if (instance instanceof Folder) {
			return perm(instance, PermType.updatefolder);
		} else if (instance instanceof Form) {
			return perm(execute(NodeObject::getParentObject, instance), PermType.updateform);
		} else {
			return perm(execute(NodeObject::getParentObject, instance), PermType.updateitems);
		}
	}

	/**
	 * Get the {@link Triple} defining the permission to create instances of the tested class in the folder
	 * @param folder folder
	 * @return create permission triple
	 * @throws NodeException
	 */
	protected Triple<Integer, Integer, Integer> createPerm(Folder folder) throws NodeException {
		if (Folder.class.isAssignableFrom(clazz)) {
			return perm(folder, PermType.create);
		} else if (Form.class.isAssignableFrom(clazz)) {
			return perm(folder, PermType.createform);
		} else {
			return perm(folder, PermType.createitems);
		}
	}
}
