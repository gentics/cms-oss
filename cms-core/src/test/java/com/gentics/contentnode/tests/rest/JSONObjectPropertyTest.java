package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
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
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.object.ObjectModificationException;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.LocalizableNodeObject;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.FileSaveRequest;
import com.gentics.contentnode.rest.model.request.FolderSaveRequest;
import com.gentics.contentnode.rest.model.request.ImageSaveRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.TestedType;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test updating JSON tag part for different restriction / target usecases
 */
@RunWith(value = Parameterized.class)
public class JSONObjectPropertyTest {

	private static final String PART_KEYWORD = "json";
	private static final String CONSTRUCT_KEYWORD = "construct";
	private static final String OBJPROP_SHORT_KEYWORD = "json";
	private static final String OBJPROP_KEYWORD = "object." + OBJPROP_SHORT_KEYWORD;
	private static final String RANDOM_JSON = "{\"whatever\":\"wherever\"}";

	private static Node node;
	private static Template template;
	private static Integer constructId;

	private static List<ObjectTagDefinition> properties = new ArrayList<>();

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Parameters(name = "{index}: type {0} restriction {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (TestedType type : TestedType.values()) {
			for (Entry<String, String> schemaRestriction : Map.of(
					"", RANDOM_JSON,
					"{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", "[\"one\", \"two\", \"three\"]",
					"{\"type\":\"object\",\"properties\":{\"firstName\":{\"type\":\"string\"},\"lastName\":{\"type\":\"string\"},\"middleName\":{\"type\":\"string\"}},\"required\":[\"firstName\",\"lastName\"]}", "{\"firstName\":\"Mickey\", \"lastName\":\"Mouse\"}"
			).entrySet()) {
				data.add(new Object[] { type, schemaRestriction.getKey(), schemaRestriction.getValue() });
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		// basic setup
		node = supply(() -> createNode());
		template = supply(() -> createTemplate(node.getFolder(), "Template"));
		constructId = supply(() -> createConstruct(node, JSONPartType.class, CONSTRUCT_KEYWORD, PART_KEYWORD));

		for (int type : Arrays.asList(Folder.TYPE_FOLDER, Page.TYPE_PAGE, ContentFile.TYPE_FILE, ContentFile.TYPE_IMAGE)) {
			properties.add(supply(() -> createObjectPropertyDefinition(type, constructId, OBJPROP_SHORT_KEYWORD, OBJPROP_KEYWORD)));
		}
	}

	@Parameter(0)
	public TestedType type;

	@Parameter(1)
	public String jsonSchemaRestriction;

	@Parameter(2)
	public String correctAnswer;

	@Before
	public void setup() throws NodeException {
		operate(trx -> {
			Construct construct = trx.getObject(Construct.class, constructId, true);
			for (Part part: construct.getParts()) {
				if (part.getKeyname().equals(PART_KEYWORD)) {
					part.setInfoText(jsonSchemaRestriction);
					break;
				}
			}
			construct.save();
			trx.commit(false);
		});
		operate(() -> clear(node));

		for (ObjectTagDefinition def : properties) {
			update(def, update -> {
				update.getNodes().clear();
				update.getNodes().add(node);
			}).build();
		}
	}

	@Test
	public void testCoreRight() throws NodeException {
		testInput(correctAnswer, false);
	}

	@Test(expected = ObjectModificationException.class)
	public void testCoreWrong() throws NodeException {
		testInput(RANDOM_JSON, false);

		// The no-restriction case should pass here
		if (correctAnswer.equals(RANDOM_JSON)) {
			throw new ObjectModificationException(PART_KEYWORD, PART_KEYWORD, PART_KEYWORD);
		}
	}

	@Test
	public void testRestRight() throws NodeException {
		GenericResponse response = (GenericResponse) testInput(correctAnswer, true);
		assertThat(response.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(ResponseCode.OK);
	}

	@Test
	public void testRestWrong() throws NodeException {
		GenericResponse response = (GenericResponse) testInput(RANDOM_JSON, true);
		assertThat(response.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(correctAnswer.equals(RANDOM_JSON) ? ResponseCode.OK : ResponseCode.INVALIDDATA);
	}

	protected Object testInput(String input, boolean useRest) throws NodeException {
		// create tested object
		LocalizableNodeObject<?> testedObject = supply(() -> type.create(node.getFolder(), template));

		if (useRest) {
			return supply(() -> {
				Property prop = new Property();
				prop.setType(com.gentics.contentnode.rest.model.Property.Type.RICHTEXT);
				prop.setStringValue(input);
				Tag tag = new Tag();
				tag.setType(Type.OBJECTTAG);
				tag.setName(OBJPROP_SHORT_KEYWORD);
				tag.setActive(true);
				tag.setProperties(Map.of(PART_KEYWORD, prop));
				Object request;
				switch(type) {
				case file:
					com.gentics.contentnode.rest.model.File file = new com.gentics.contentnode.rest.model.File();
					file.setId(testedObject.getId());
					file.setTags(Map.of(OBJPROP_KEYWORD, tag));
					FileSaveRequest fileRequest = new FileSaveRequest();
					fileRequest.setFile(file);
					request = fileRequest;
					break;
				case folder:
					com.gentics.contentnode.rest.model.Folder folder = new com.gentics.contentnode.rest.model.Folder();
					folder.setId(testedObject.getId());
					folder.setTags(Map.of(OBJPROP_KEYWORD, tag));
					FolderSaveRequest folderRequest = new FolderSaveRequest();
					folderRequest.setFolder(folder);
					request = folderRequest;
					break;
				case image:
					com.gentics.contentnode.rest.model.Image image = new com.gentics.contentnode.rest.model.Image();
					image.setId(testedObject.getId());
					image.setTags(Map.of(OBJPROP_KEYWORD, tag));
					ImageSaveRequest imageRequest = new ImageSaveRequest();
					imageRequest.setImage(image);
					request = imageRequest;
					break;
				case page:
					com.gentics.contentnode.rest.model.Page page = new com.gentics.contentnode.rest.model.Page();
					page.setId(testedObject.getId());
					page.setTags(Map.of(OBJPROP_KEYWORD, tag));
					PageSaveRequest pageRequest = new PageSaveRequest();
					pageRequest.setPage(page);
					request = pageRequest;
					break;
				default:
					throw new IllegalStateException("Unsupported type");
				}
				return type.save(testedObject, request);
			});
		} else {
			// fill object tags
			ObjectTagContainer container = update(((ObjectTagContainer) testedObject), update -> {
				ObjectTag tag = update.getObjectTag(OBJPROP_SHORT_KEYWORD);
				getPartType(JSONPartType.class, tag, PART_KEYWORD).setText(input);
				tag.setEnabled(true);
			}).build();

			// assert all object tags available and filled
			consume(o -> {
				assertThat(o.getObjectTag(OBJPROP_SHORT_KEYWORD)).isNotNull().hasPartWithText(JSONPartType.class, PART_KEYWORD, input);
			}, container);

			return container;
		}
	}
}
