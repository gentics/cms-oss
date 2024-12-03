package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createObjectPropertyDefinition;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.ObjectTag;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.FileResourceImpl;
import com.gentics.contentnode.rest.resource.impl.FolderResourceImpl;
import com.gentics.contentnode.rest.resource.impl.ImageResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for loading/updating object properties with different restriction settings
 */
@RunWith(value = Parameterized.class)
public class ObjectPropertyRestrictionTest {
	private static final String RESTRICTED_SHORT_KEYWORD = "restricted";

	private static final String RESTRICTED_KEYWORD = "object." + RESTRICTED_SHORT_KEYWORD;

	private static final String RESTRICTED_NAME = "Restricted Object Property";

	private static final String OTHER_RESTRICTED_SHORT_KEYWORD = "other_restricted";

	private static final String OTHER_RESTRICTED_KEYWORD = "object." + OTHER_RESTRICTED_SHORT_KEYWORD;

	private static final String OTHER_RESTRICTED_NAME = "Other Restricted Object Property";

	private static final String RESTRICTED_UNASSIGNED_SHORT_KEYWORD = "restricted_unassigned";

	private static final String RESTRICTED_UNASSIGNED_KEYWORD = "object." + RESTRICTED_UNASSIGNED_SHORT_KEYWORD;

	private static final String RESTRICTED_UNASSIGNED_NAME = "Restricted Unassigned Object Property";

	private static final String UNRESTRICTED_SHORT_KEYWORD = "unrestricted";

	private static final String UNRESTRICTED_KEYWORD = "object." + UNRESTRICTED_SHORT_KEYWORD;

	private static final String UNRESTRICTED_NAME = "Unrestricted Object Property";

	private static final String PART_KEYWORD = "part";

	private static final String CONSTRUCT_KEYWORD = "construct";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	private static Node node;

	private static Node otherNode;

	private static Template template;

	private static Integer constructId;

	private static List<ObjectTagDefinition> restricted = new ArrayList<>();

	private static List<ObjectTagDefinition> other_restricted = new ArrayList<>();

	private static List<ObjectTagDefinition> restricted_unassigned = new ArrayList<>();

	private static List<ObjectTagDefinition> unrestricted = new ArrayList<>();


	@Parameters(name = "{index}: type {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType type : TestedType.values()) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		// basic setup
		node = supply(() -> createNode());
		otherNode = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = supply(() -> createConstruct(node, HTMLPartType.class, CONSTRUCT_KEYWORD, PART_KEYWORD));

		for (int type : Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, ContentFile.TYPE_FILE, ContentFile.TYPE_IMAGE)) {
			restricted.add(supply(() -> createObjectPropertyDefinition(type, constructId, RESTRICTED_NAME, RESTRICTED_KEYWORD)));
			restricted_unassigned.add(supply(() -> createObjectPropertyDefinition(type, constructId, RESTRICTED_UNASSIGNED_NAME, RESTRICTED_UNASSIGNED_KEYWORD)));
			other_restricted.add(supply(() -> createObjectPropertyDefinition(type, constructId, OTHER_RESTRICTED_NAME, OTHER_RESTRICTED_KEYWORD)));
			unrestricted.add(supply(() -> createObjectPropertyDefinition(type, constructId, UNRESTRICTED_NAME, UNRESTRICTED_KEYWORD)));
		}
	}

	@Parameter(0)
	public TestedType type;

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));

		for (ObjectTagDefinition def : restricted) {
			update(def, update -> {
				update.getNodes().clear();
				update.getNodes().add(node);
				update.setRestricted(true);
			}).build();
		}

		for (ObjectTagDefinition def : restricted_unassigned) {
			Builder.update(def, update -> {
				update.getNodes().clear();
				update.setRestricted(true);
			}).build();
		}

		for (ObjectTagDefinition def : other_restricted) {
			update(def, update -> {
				update.getNodes().clear();
				update.getNodes().add(otherNode);
				update.setRestricted(true);
			}).build();
		}

		for (ObjectTagDefinition def : unrestricted) {
			Builder.update(def, update -> {
				update.getNodes().clear();
				update.setRestricted(false);
			}).build();
		}
	}

	/**
	 * Test that loading objects will ignore object tags which were restricted to other nodes (or generally restricted) later
	 * @throws NodeException
	 */
	@Test
	public void testGetRestrictedExistingTag() throws NodeException {
		// remove restriction of definitions
		for (ObjectTagDefinition def : restricted) {
			update(def, update -> {
				update.getNodes().clear();
				update.setRestricted(false);
			}).build();
		}
		for (ObjectTagDefinition def : restricted_unassigned) {
			update(def, update -> {
				update.getNodes().clear();
				update.setRestricted(false);
			}).build();
		}
		for (ObjectTagDefinition def : other_restricted) {
			update(def, update -> {
				update.getNodes().clear();
				update.setRestricted(false);
			}).build();
		}

		// create tested object
		LocalizableNodeObject<?> testedObject = supply(() -> type.create(node.getFolder(), template));

		// fill object tags
		ObjectTagContainer container = update(((ObjectTagContainer) testedObject), update -> {
			getPartType(HTMLPartType.class, update.getObjectTag(UNRESTRICTED_SHORT_KEYWORD), PART_KEYWORD).setText("Unrestricted data");
			getPartType(HTMLPartType.class, update.getObjectTag(RESTRICTED_SHORT_KEYWORD), PART_KEYWORD).setText("Restricted data");
			getPartType(HTMLPartType.class, update.getObjectTag(RESTRICTED_UNASSIGNED_SHORT_KEYWORD), PART_KEYWORD).setText("Restricted Unassigned data");
			getPartType(HTMLPartType.class, update.getObjectTag(OTHER_RESTRICTED_SHORT_KEYWORD), PART_KEYWORD).setText("Other Restricted data");
		}).build();

		// assert all object tags available and filled
		consume(o -> {
			assertThat(o.getObjectTag(UNRESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class, PART_KEYWORD, "Unrestricted data");
			assertThat(o.getObjectTag(RESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class, PART_KEYWORD, "Restricted data");
			assertThat(o.getObjectTag(RESTRICTED_UNASSIGNED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class, PART_KEYWORD, "Restricted Unassigned data");
			assertThat(o.getObjectTag(OTHER_RESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class, PART_KEYWORD, "Other Restricted data");
		}, container);

		// restrict definitions
		for (ObjectTagDefinition def : restricted) {
			update(def, update -> {
				update.getNodes().add(node);
				update.setRestricted(true);
			}).build();
		}
		for (ObjectTagDefinition def : restricted_unassigned) {
			update(def, update -> {
				update.setRestricted(true);
			}).build();
		}
		for (ObjectTagDefinition def : other_restricted) {
			update(def, update -> {
				update.getNodes().add(otherNode);
				update.setRestricted(true);
			}).build();
		}

		// assert only unrestricted object tag available and filled
		consume(o -> {
			assertThat(o.getObjectTag(UNRESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class,
					PART_KEYWORD, "Unrestricted data");
			assertThat(o.getObjectTag(RESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class, PART_KEYWORD, "Restricted data");
			assertThat(o.getObjectTag(RESTRICTED_UNASSIGNED_SHORT_KEYWORD)).isNull();
			assertThat(o.getObjectTag(OTHER_RESTRICTED_SHORT_KEYWORD)).isNull();
		}, container);

		update(container, update -> {
			assertThat(update.getObjectTag(UNRESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class,
					PART_KEYWORD, "Unrestricted data");
			assertThat(update.getObjectTag(RESTRICTED_SHORT_KEYWORD)).isNotNull().hasPartWithText(HTMLPartType.class, PART_KEYWORD, "Restricted data");
			assertThat(update.getObjectTag(RESTRICTED_UNASSIGNED_SHORT_KEYWORD)).isNull();
			assertThat(update.getObjectTag(OTHER_RESTRICTED_SHORT_KEYWORD)).isNull();
		}).build();
	}

	/**
	 * Test that creating new objects will not generate object tags which are restricted to other nodes
	 * @throws NodeException
	 */
	@Test
	public void testGetRestrictedTagForNewObject() throws NodeException {
		// create tested object
		LocalizableNodeObject<?> testedObject = supply(() -> type.create(node.getFolder(), template));
		ObjectTagContainer container = (ObjectTagContainer) testedObject;

		// assert that container does have the correct object properties
		consume(o -> {
			assertThat(o.getObjectTag(UNRESTRICTED_SHORT_KEYWORD)).isNotNull();
			assertThat(o.getObjectTag(RESTRICTED_SHORT_KEYWORD)).isNotNull();
			assertThat(o.getObjectTag(RESTRICTED_UNASSIGNED_SHORT_KEYWORD)).isNull();
			assertThat(o.getObjectTag(OTHER_RESTRICTED_SHORT_KEYWORD)).isNull();
		}, container);
	}

	/**
	 * Test that updating an object over the REST API does not allow to create restricted object tags
	 * @throws NodeException
	 */
	@Test
	public void testUpdateRestrictedTags() throws NodeException {
		LocalizableNodeObject<?> testedObject = supply(() -> type.create(node.getFolder(), template));

		Set<String> tagsBeforeUpdate = getObjectTags(testedObject);
		assertThat(tagsBeforeUpdate).as("Object tags before update").contains(UNRESTRICTED_KEYWORD, RESTRICTED_KEYWORD)
				.doesNotContain(RESTRICTED_UNASSIGNED_KEYWORD, OTHER_RESTRICTED_KEYWORD, UNRESTRICTED_SHORT_KEYWORD,
						RESTRICTED_SHORT_KEYWORD, RESTRICTED_UNASSIGNED_SHORT_KEYWORD, OTHER_RESTRICTED_SHORT_KEYWORD);

		Map<String, Tag> objectTags = Map.of(UNRESTRICTED_KEYWORD, createObjectTagModel(UNRESTRICTED_KEYWORD),
				RESTRICTED_KEYWORD, createObjectTagModel(RESTRICTED_KEYWORD), RESTRICTED_UNASSIGNED_KEYWORD,
				createObjectTagModel(RESTRICTED_UNASSIGNED_KEYWORD), OTHER_RESTRICTED_KEYWORD,
				createObjectTagModel(OTHER_RESTRICTED_KEYWORD));

		switch(type) {
		case file:
			try (Trx trx = new Trx()) {
				FileResourceImpl res = new FileResourceImpl();

				File file = new File();
				file.setTags(objectTags);

				FileSaveRequest request = new FileSaveRequest();
				request.setFile(file);

				GenericResponse response = res.save(testedObject.getId(), request);
				assertResponseCodeOk(response);

				trx.success();
			}
			break;
		case folder:
			try (Trx trx = new Trx()) {
				FolderResourceImpl res = new FolderResourceImpl();

				com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();
				folder.setTags(objectTags);

				FolderSaveRequest request = new FolderSaveRequest();
				request.setFolder(folder);

				GenericResponse response = res.save(Integer.toString(testedObject.getId()), request);
				assertResponseCodeOk(response);

				trx.success();
			}
			break;
		case image:
			try (Trx trx = new Trx()) {
				ImageResourceImpl res = new ImageResourceImpl();

				Image image = new Image();
				image.setTags(objectTags);

				ImageSaveRequest request = new ImageSaveRequest();
				request.setImage(image);

				GenericResponse response = res.save(testedObject.getId(), request);
				assertResponseCodeOk(response);

				trx.success();
			}
			break;
		case page:
			try (Trx trx = new Trx()) {
				PageResourceImpl res = new PageResourceImpl();

				com.gentics.contentnode.rest.model.Page page = new com.gentics.contentnode.rest.model.Page();
				page.setTags(objectTags);

				PageSaveRequest request = new PageSaveRequest();
				request.setPage(page);

				GenericResponse response = res.save(Integer.toString(testedObject.getId()), request);
				assertResponseCodeOk(response);

				trx.success();
			}
			break;
		}

		Set<String> tagsAfterUpdate = getObjectTags(testedObject);
		assertThat(tagsAfterUpdate).as("Object tags after update").contains(UNRESTRICTED_KEYWORD, RESTRICTED_KEYWORD)
				.doesNotContain(RESTRICTED_UNASSIGNED_KEYWORD, OTHER_RESTRICTED_KEYWORD, UNRESTRICTED_SHORT_KEYWORD,
						RESTRICTED_SHORT_KEYWORD, RESTRICTED_UNASSIGNED_SHORT_KEYWORD, OTHER_RESTRICTED_SHORT_KEYWORD);
	}

	protected ObjectTag createObjectTagModel(String name) {
		ObjectTag tag = new ObjectTag();
		tag.setConstructId(constructId);
		tag.setActive(true);
		tag.setName(name);
		tag.setType(Type.OBJECTTAG);
		Property prop = new Property();
		prop.setStringValue("Contents");
		tag.setProperties(Map.of(PART_KEYWORD, prop));
		return tag;
	}

	protected Set<String> getObjectTags(LocalizableNodeObject<?> object) throws NodeException {
		return execute(o -> {
			return DBUtils.select("SELECT name FROM objtag WHERE obj_type = ? AND obj_id = ?", ps -> {
				ps.setInt(1, object.getTType());
				ps.setInt(2, object.getId());
			}, rs -> {
				Set<String> names = new HashSet<>();

				while (rs.next()) {
					names.add(rs.getString("name"));
				}

				return names;
			});
		}, object);
	}
}
