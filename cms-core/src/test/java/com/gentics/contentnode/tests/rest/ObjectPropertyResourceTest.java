package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.tests.utils.Expected;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.ObjectTagDefinitionCategory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.ObjectProperty;
import com.gentics.contentnode.rest.model.ObjectPropertyCategory;
import com.gentics.contentnode.rest.model.ObjectPropertyType;
import com.gentics.contentnode.rest.model.request.BulkLinkUpdateRequest;
import com.gentics.contentnode.rest.model.response.ConstructListResponse;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.NodeList;
import com.gentics.contentnode.rest.model.response.ObjectPropertyCategoryListResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyCategoryLoadResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyListResponse;
import com.gentics.contentnode.rest.model.response.ObjectPropertyLoadResponse;
import com.gentics.contentnode.rest.model.response.PagedObjectPropertyListResponse;
import com.gentics.contentnode.rest.resource.ObjectPropertyResource;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ObjectPropertyResourceImpl;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.ObjectPropertyParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.contentnode.tests.utils.ExceptionChecker;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for the {@link ObjectPropertyResource}
 */
public class ObjectPropertyResourceTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static UserGroup group;
	private static SystemUser user;

	private static Node node;

	private static Integer constructId;

	@Rule
	public ExceptionChecker exceptionChecker = new ExceptionChecker();

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		group = supply(() -> createUserGroup("TestGroup", NODE_GROUP_ID));
		user = supply(() -> createSystemUser("Tester", "Tester", null, "tester", "tester", Arrays.asList(group)));

		node = supply(() -> createNode());

		constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "construct", "part"));
	}

	@Before
	public void setup() throws NodeException {
		operate(t -> {
			for (ObjectTag objectTag : t.getObjects(ObjectTag.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id != 0", DBUtils.IDS))) {
				objectTag.delete();
			}
		});
		operate(t -> {
			for (ObjectTagDefinition objectProperty : t.getObjects(ObjectTagDefinition.class, DBUtils.select("SELECT id FROM objtag WHERE obj_id = 0", DBUtils.IDS))) {
				objectProperty.delete();
			}
		});
	}

	@Test
	public void testList() throws NodeException {
		List<Pair<Integer, String>> types = Arrays.asList(Pair.of(Folder.TYPE_FOLDER, "Folder"), Pair.of(Page.TYPE_PAGE, "Page"),
				Pair.of(Template.TYPE_TEMPLATE, "Template"), Pair.of(File.TYPE_FILE, "File"),
				Pair.of(ImageFile.TYPE_IMAGE, "Image"));

		for (Pair<Integer, String> type : types) {
			for (int i = 1; i <= 3; i++) {
				consume(index -> createObjectPropertyDefinition(type.getLeft(), constructId,
						String.format("%s Property %d", type.getValue(), index), String.format("prop%d", index)), i);
			}
		}

		ObjectPropertyListResponse response = supply(user,
				() -> new ObjectPropertyResourceImpl().list(null, null, null, null, null));
		assertResponseOK(response);
		assertThat(response.getItems()).as("Object Properties").usingElementComparatorOnFields("keyword", "type")
				.containsOnly(new ObjectProperty().setKeyword("object.prop1").setType(Folder.TYPE_FOLDER),
						new ObjectProperty().setKeyword("object.prop2").setType(Folder.TYPE_FOLDER),
						new ObjectProperty().setKeyword("object.prop3").setType(Folder.TYPE_FOLDER),
						new ObjectProperty().setKeyword("object.prop1").setType(Page.TYPE_PAGE),
						new ObjectProperty().setKeyword("object.prop2").setType(Page.TYPE_PAGE),
						new ObjectProperty().setKeyword("object.prop3").setType(Page.TYPE_PAGE),
						new ObjectProperty().setKeyword("object.prop1").setType(Template.TYPE_TEMPLATE),
						new ObjectProperty().setKeyword("object.prop2").setType(Template.TYPE_TEMPLATE),
						new ObjectProperty().setKeyword("object.prop3").setType(Template.TYPE_TEMPLATE),
						new ObjectProperty().setKeyword("object.prop1").setType(File.TYPE_FILE),
						new ObjectProperty().setKeyword("object.prop2").setType(File.TYPE_FILE),
						new ObjectProperty().setKeyword("object.prop3").setType(File.TYPE_FILE),
						new ObjectProperty().setKeyword("object.prop1").setType(ImageFile.TYPE_IMAGE),
						new ObjectProperty().setKeyword("object.prop2").setType(ImageFile.TYPE_IMAGE),
						new ObjectProperty().setKeyword("object.prop3").setType(ImageFile.TYPE_IMAGE));
	}

	@Test
	public void testListPerType() throws NodeException {
		List<Pair<Integer, String>> types = Arrays.asList(Pair.of(Folder.TYPE_FOLDER, "Folder"), Pair.of(Page.TYPE_PAGE, "Page"),
				Pair.of(Template.TYPE_TEMPLATE, "Template"), Pair.of(File.TYPE_FILE, "File"),
				Pair.of(ImageFile.TYPE_IMAGE, "Image"));

		for (Pair<Integer, String> type : types) {
			for (int i = 1; i <= 3; i++) {
				consume(index -> createObjectPropertyDefinition(type.getLeft(), constructId,
						String.format("%s Property %d", type.getValue(), index), String.format("prop%d", index)), i);
			}
		}

		ObjectPropertyParameterBean filter = new ObjectPropertyParameterBean();

		filter.types = new HashSet<>(Arrays.asList(ObjectPropertyType.folder, ObjectPropertyType.template));
		ObjectPropertyListResponse response = supply(user,
				() -> new ObjectPropertyResourceImpl().list(null, null, null, filter, null));
		assertResponseOK(response);
		assertThat(response.getItems()).as("Object Properties").usingElementComparatorOnFields("keyword", "type")
				.containsOnly(new ObjectProperty().setKeyword("object.prop1").setType(Folder.TYPE_FOLDER),
						new ObjectProperty().setKeyword("object.prop2").setType(Folder.TYPE_FOLDER),
						new ObjectProperty().setKeyword("object.prop3").setType(Folder.TYPE_FOLDER),
						new ObjectProperty().setKeyword("object.prop1").setType(Template.TYPE_TEMPLATE),
						new ObjectProperty().setKeyword("object.prop2").setType(Template.TYPE_TEMPLATE),
						new ObjectProperty().setKeyword("object.prop3").setType(Template.TYPE_TEMPLATE));
	}

	@Test
	public void testCreate() throws NodeException {
		ObjectPropertyLoadResponse response = createRandomObjectProperty();

		// check the list contents
		ObjectPropertyListResponse opList = supply(user, () -> new ObjectPropertyResourceImpl().list(
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new ObjectPropertyParameterBean(), new EmbedParameterBean()));
		assertResponseCodeOk(opList);
		assertThat(opList.getItems()).as("Object properties").usingElementComparatorOnFields("id").contains(response.getObjectProperty());
	}

	/**
	 * Verify that create requests without keyword fail.
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Das 'keyword'-Feld ist erforderlich.")
	public void testCreateMissingKeyword() throws NodeException {
		ObjectPropertyLoadResponse response = supply(user, () -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Page.TYPE_PAGE)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4));

			return new ObjectPropertyResourceImpl().create(op);
		});
	}

	/**
	 * Verify that create requests with an invalid keyword fail.
	 * @throws NodeException
	 */
	@Test
	@Expected(ex = RestMappedException.class, message = "Das 'keyword'-Feld darf nur Buchstaben (A-Z und a-z), Ziffern (0-9), '-' und '_' enthalten und muss 3-255 Zeichen enthalten.")
	public void testCreateInvalidKeyword() throws NodeException {
		ObjectPropertyLoadResponse response = supply(user, () -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Page.TYPE_PAGE)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4))
				.setKeyword("Not Allowed!");

			return new ObjectPropertyResourceImpl().create(op);
		});
	}

	/**
	 * Test that create requests where the keyword already contains the "object." prefix succeed.
	 */
	@Test
	public void testCreatePrefixedKeyword() throws NodeException {
		String keyword = "object.validkeyword";
		ObjectPropertyLoadResponse response = supply(user, () -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Page.TYPE_PAGE)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4))
				.setKeyword(keyword);

			return new ObjectPropertyResourceImpl().create(op);
		});

		assertResponseOK(response);
		// When the keyword already starts with "object." it must not be prefixed again.
		assertThat(response.getObjectProperty().getKeyword()).as("Created object property keyword").isEqualTo(keyword);
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Es gibt bereits eine Objekteigenschaft object.keyword.")
	public void testCreateDuplicateKeyword() throws NodeException {
		String keyword = "keyword";
		supply(() -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Folder.TYPE_FOLDER)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4))
				.setKeyword(keyword);
			return new ObjectPropertyResourceImpl().create(op);
		});

		supply(() -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Folder.TYPE_FOLDER)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4))
				.setKeyword(keyword);
			return new ObjectPropertyResourceImpl().create(op);
		});
	}

	@Test
	public void testDelete() throws NodeException {
		ObjectPropertyLoadResponse response = createRandomObjectProperty();

		// deletion
		GenericResponse deleted = supply(() -> new ObjectPropertyResourceImpl().delete(response.getObjectProperty().getId().toString(), 0));
		assertResponseCodeOk(deleted);

		operate(() -> setPermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF, response.getObjectProperty().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		// check the list contents
		ObjectPropertyListResponse opList = supply(user, () -> new ObjectPropertyResourceImpl().list(
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new ObjectPropertyParameterBean(), new EmbedParameterBean()));
		assertResponseCodeOk(opList);
		assertThat(opList.getItems()).as("Object properties").usingElementComparatorOnFields("id").doesNotContain(response.getObjectProperty());
	}

	@Test
	public void testLoad() throws NodeException {
		ObjectPropertyLoadResponse response = createRandomObjectProperty();

		// read it
		ObjectPropertyLoadResponse read = supply(() -> new ObjectPropertyResourceImpl().get(
				response.getObjectProperty().getId().toString(),
				new EmbedParameterBean()));
		assertResponseCodeOk(read);
		assertEquals(read.getObjectProperty().getId(), response.getObjectProperty().getId());
		assertEquals(read.getObjectProperty().getName(), response.getObjectProperty().getName());
		assertEquals(read.getObjectProperty().getCategoryId(), response.getObjectProperty().getCategoryId());
		assertEquals(read.getObjectProperty().getType(), response.getObjectProperty().getType());
		assertEquals(read.getObjectProperty().getKeyword(), response.getObjectProperty().getKeyword());
	}

	@Test
	public void givenObjectPropertyWithReferencedObjectsRequest_shouldHaveEmbeddedObjectsInResponse() throws NodeException {
		ObjectProperty createdObjectProperty = createRandomObjectProperty().getObjectProperty();
		ObjectPropertyCategory createdObjectCategory =  createRandomObjectPropertyCategory().getObjectPropertyCategory();
		createdObjectProperty.setKeyword("updated");
		createdObjectProperty.setCategoryId(createdObjectCategory.getId());
		createdObjectProperty.setCategory(createdObjectCategory);

		new ObjectPropertyResourceImpl().update(createdObjectProperty.getId().toString(), createdObjectProperty);


		ObjectProperty retrievedObjectProperty = supply(() -> new ObjectPropertyResourceImpl().get(
				createdObjectProperty.getId().toString(),
				new EmbedParameterBean().withEmbed("category,construct"))).getObjectProperty();


		assertThat(retrievedObjectProperty).as("Referenced category should match").hasFieldOrPropertyWithValue("categoryId", createdObjectCategory.getId());
		assertThat(retrievedObjectProperty.getCategory()).as("Referenced category should match").hasFieldOrPropertyWithValue("globalId", createdObjectCategory.getGlobalId());
		assertThat(retrievedObjectProperty.getCategory()).as("Referenced category should match").hasFieldOrPropertyWithValue("name", createdObjectCategory.getName());

		assertThat(retrievedObjectProperty.getConstructId()).as("Referenced construct should match").isEqualTo(constructId);
		assertThat(retrievedObjectProperty.getConstruct()).hasFieldOrPropertyWithValue("id", constructId);
		assertThat(retrievedObjectProperty.getConstruct()).hasFieldOrPropertyWithValue("name", "construct");
	}

	@Test
	public void testUpdate() throws NodeException {
		ObjectPropertyLoadResponse response = createRandomObjectProperty();
		ObjectProperty op = response.getObjectProperty();
		String oldKw = op.getKeyword();

		operate(() -> setPermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF, response.getObjectProperty().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		op.setDescription(MiscUtils.getRandomNameOfLength(24));
		do {
			op.setKeyword(MiscUtils.getRandomNameOfLength(8));
		} while (op.getKeyword().equals(oldKw));
		Integer id = op.getId();
		String kw = "object." + op.getKeyword();
		Map<String, String> name = op.getNameI18n();
		Map<String, String> description = op.getDescriptionI18n();

		// update it
		ObjectPropertyLoadResponse updated = new ObjectPropertyResourceImpl().update(op.getId().toString(), op);
		assertResponseCodeOk(updated);
		assertEquals(updated.getObjectProperty().getId(), id);
		assertEquals(updated.getObjectProperty().getNameI18n(), name);
		assertEquals(updated.getObjectProperty().getKeyword(), kw);
		assertEquals(updated.getObjectProperty().getDescriptionI18n(), description);
	}

	@Test
	@Expected(ex = RestMappedException.class, message = "Es gibt bereits eine Objekteigenschaft object.duplicate.")
	public void testUpdateDuplicateKeyword() throws NodeException {
		String keyword = "keyword";
		String duplicate = "duplicate";
		ObjectPropertyLoadResponse objProp1 = supply(() -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Folder.TYPE_FOLDER)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4))
				.setKeyword(keyword);
			return new ObjectPropertyResourceImpl().create(op);
		});

		ObjectPropertyLoadResponse objProp2 = supply(() -> {
			ObjectProperty op = new ObjectProperty()
				.setConstructId(constructId)
				.setType(Folder.TYPE_FOLDER)
				.setDescription(MiscUtils.getRandomNameOfLength(24))
				.setName("Dummy name " + MiscUtils.getRandomNameOfLength(4))
				.setKeyword(duplicate);
			return new ObjectPropertyResourceImpl().create(op);
		});

		supply(() -> new ObjectPropertyResourceImpl().update(objProp1.getObjectProperty().getGlobalId(),
				new ObjectProperty().setKeyword(objProp2.getObjectProperty().getKeyword())));
	}

	@Test
	public void testListObjectPropertyConstructs() throws NodeException {
		ObjectPropertyLoadResponse response = createRandomObjectProperty();

		operate(() -> setPermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF, response.getObjectProperty().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ConstructListResponse updated = supply(() -> new ObjectPropertyResourceImpl().listObjectPropertyConstructs(response.getObjectProperty().getId().toString()));
		assertResponseCodeOk(updated);
		assertThat(updated.getConstructs()).isEmpty();
	}

	@Test
	public void testListCategories() throws NodeException {
		operate(() -> setPermissions(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		List<ObjectPropertyCategory> categories = new ArrayList<>();
		for (int i = 1; i <= 8; i++) {
			categories.add(supply(() -> createRandomObjectPropertyCategory().getObjectPropertyCategory()));
		}

		ObjectPropertyCategoryListResponse response = supply(user,
				() -> new ObjectPropertyResourceImpl().listCategories(null, null, null, null));
		assertResponseOK(response);
		assertThat(response.getItems()).as("Object Property categories").usingElementComparatorOnFields("id").containsAll(categories);
	}

	@Test
	public void testCreateCategory() throws NodeException {
		operate(() -> setPermissions(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ObjectPropertyCategoryLoadResponse response = createRandomObjectPropertyCategory();

		// check the list contents
		ObjectPropertyCategoryListResponse opList = supply(user, () -> new ObjectPropertyResourceImpl().listCategories(
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean()));
		assertResponseCodeOk(opList);
		assertThat(opList.getItems()).as("Object property categories").usingElementComparatorOnFields("id").contains(response.getObjectPropertyCategory());
	}

	@Test
	public void testDeleteCategory() throws NodeException {
		operate(() -> setPermissions(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ObjectPropertyCategoryLoadResponse response = createRandomObjectPropertyCategory();

		// deletion
		GenericResponse deleted = supply(() -> new ObjectPropertyResourceImpl().deleteCategory(response.getObjectPropertyCategory().getId().toString()));
		assertResponseCodeOk(deleted);

		// check the list contents
		ObjectPropertyCategoryListResponse opList = supply(user, () -> new ObjectPropertyResourceImpl().listCategories(
				new SortParameterBean(), new FilterParameterBean(), new PagingParameterBean(), new EmbedParameterBean()));
		assertResponseCodeOk(opList);
		assertThat(opList.getItems()).as("Object property categories").usingElementComparatorOnFields("id").doesNotContain(response.getObjectPropertyCategory());
	}

	@Test
	public void testLoadCategory() throws NodeException {
		operate(() -> setPermissions(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ObjectPropertyCategoryLoadResponse response = createRandomObjectPropertyCategory();

		// read it
		ObjectPropertyCategoryLoadResponse read = supply(() -> new ObjectPropertyResourceImpl().getCategory(response.getObjectPropertyCategory().getId().toString()));
		assertResponseCodeOk(read);
		assertEquals(read.getObjectPropertyCategory().getId(), response.getObjectPropertyCategory().getId());
		assertEquals(read.getObjectPropertyCategory().getName(), response.getObjectPropertyCategory().getName());
		assertEquals(read.getObjectPropertyCategory().getNameI18n(), response.getObjectPropertyCategory().getNameI18n());
	}

	@Test
	public void testUpdateCategory() throws NodeException {
		operate(() -> setPermissions(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ObjectPropertyCategoryLoadResponse response = createRandomObjectPropertyCategory();
		ObjectPropertyCategory opc = response.getObjectPropertyCategory();
		String oldEnName = opc.getNameI18n().get("en");

		do {
			opc.setName(MiscUtils.getRandomNameOfLength(12), "en");
		} while (opc.getNameI18n().get("en").equals(oldEnName));

		Integer id = opc.getId();
		Map<String, String> name = opc.getNameI18n();

		// update it
		ObjectPropertyCategoryLoadResponse updated = new ObjectPropertyResourceImpl().updateCategory(opc.getId().toString(), opc);
		assertResponseCodeOk(updated);
		assertEquals(updated.getObjectPropertyCategory().getId(), id);
		assertEquals(updated.getObjectPropertyCategory().getNameI18n(), name);
	}

	@Test
	public void testLinkUnlink() throws NodeException {
		ObjectPropertyLoadResponse response = createRandomObjectProperty();
		ObjectProperty op = response.getObjectProperty();

		operate(() -> setPermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF, response.getObjectProperty().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));
		operate(() -> setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));
		operate(() -> setPermissions(Folder.TYPE_FOLDER, node.getFolder().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW).toString()));

		BulkLinkUpdateRequest link = new BulkLinkUpdateRequest()
				.setTargetIds(Collections.singleton(op.getId().toString()))
				.setIds(new HashSet<>(Arrays.asList(node.getId())));

		// link it
		GenericResponse updated = supply(() -> new ObjectPropertyResourceImpl().link(link));
		assertResponseCodeOk(updated);

		PagedObjectPropertyListResponse opList = supply(user, () -> {
			try {
				return new NodeResourceImpl().getObjectProperties(node.getId().toString(), new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		assertResponseCodeOk(opList);

		assertThat(opList.getItems())
			.as("Node object property IDs").usingElementComparatorOnFields("id").contains(op);

		operate(() -> {
			NodeList nodes = new ObjectPropertyResourceImpl().listObjectPropertyNodes(op.getId().toString());
			assertResponseCodeOk(nodes);
			assertThat(nodes.getItems()).as("Object property nodes").usingElementComparatorOnFields("id").contains(Node.TRANSFORM2REST.apply(node));
		});

		// unlink
		updated = supply(() -> new ObjectPropertyResourceImpl().unlink(link));
		assertResponseCodeOk(updated);

		opList = supply(user, () -> {
			try {
				return new NodeResourceImpl().getObjectProperties(node.getId().toString(), new FilterParameterBean(),
						new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean());
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});
		assertResponseCodeOk(opList);

		assertThat(opList.getItems())
			.as("Node object property IDs").usingElementComparatorOnFields("id").doesNotContain(op);

		operate(() -> {
			NodeList nodes = new ObjectPropertyResourceImpl().listObjectPropertyNodes(op.getId().toString());
			assertResponseCodeOk(nodes);
			assertThat(nodes.getItems()).as("Object property nodes").usingElementComparatorOnFields("id").doesNotContain(Node.TRANSFORM2REST.apply(node));
		});
	}

	protected ObjectPropertyLoadResponse createRandomObjectProperty() throws NodeException {
		ObjectPropertyLoadResponse response = supply(user, () -> {
			ObjectProperty op = new ObjectProperty()
					.setConstructId(constructId)
					.setType(Page.TYPE_PAGE)
					.setDescription(MiscUtils.getRandomNameOfLength(24))
					.setName(MiscUtils.getRandomNameOfLength(10))
					.setKeyword(MiscUtils.getRandomNameOfLength(8));
			return new ObjectPropertyResourceImpl().create(op);
		});

		assertResponseCodeOk(response);
		assertThat(response.getObjectProperty()).as("Created object property").isNotNull();
		assertThat(response.getObjectProperty().getId()).as("Created object property ID").isNotNull();
		assertThat(response.getObjectProperty().getType()).as("Created object property type").isNotNull();
		assertThat(response.getObjectProperty().getDescription()).as("Created object property description").isNotNull();
		assertThat(response.getObjectProperty().getName()).as("Created object property name").isNotNull();
		assertThat(response.getObjectProperty().getKeyword()).as("Created object property keyword").isNotNull();
		assertThat(response.getObjectProperty().getKeyword()).as("Created object property keyword prefix").startsWith("object.");

		operate(() -> setPermissions(ObjectTagDefinition.TYPE_OBJTAG_DEF, response.getObjectProperty().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ObjectPropertyListResponse allProperties = supply(user,
				() -> new ObjectPropertyResourceImpl().list(null, null, null, null, null));
		assertResponseOK(allProperties);
		assertThat(allProperties.getItems()).as("Object properties").usingElementComparator((a, b) -> a.getId().compareTo(b.getId())).contains(response.getObjectProperty());

		return response;
	}

	protected ObjectPropertyCategoryLoadResponse createRandomObjectPropertyCategory() throws NodeException {
		ObjectPropertyCategoryLoadResponse response = supply(user, () -> {
			ObjectPropertyCategory op = new ObjectPropertyCategory()
					.setName(MiscUtils.getRandomNameOfLength(10), "en");
			return new ObjectPropertyResourceImpl().createCategory(op);
		});

		assertResponseCodeOk(response);
		assertThat(response.getObjectPropertyCategory()).as("Created object property category").isNotNull();
		assertThat(response.getObjectPropertyCategory().getId()).as("Created object property category ID").isNotNull();

		operate(() -> setPermissions(ObjectTagDefinitionCategory.TYPE_OBJTAG_DEF_CATEGORY, response.getObjectPropertyCategory().getId(), Arrays.asList(group),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_OBJPROP_UPDATE, PermHandler.PERM_OBJPROPS_UPDATE).toString()));

		ObjectPropertyCategoryListResponse allCategories = supply(user,
				() -> new ObjectPropertyResourceImpl().listCategories(null, null, null, null));
		assertResponseOK(allCategories);
		assertThat(allCategories.getItems()).as("Object property categories").usingElementComparator((a, b) -> a.getId().compareTo(b.getId())).contains(response.getObjectPropertyCategory());

		return response;
	}
}
