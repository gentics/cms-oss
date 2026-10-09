package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.JSONPartType;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

import jakarta.ws.rs.core.Response.Status;

/**
 * Test cases for unparseable JSON in values of the {@link JSONPartType}
 */
public class JSONUnparseableContentTest {
	protected static final String PART_KEYWORD = "json";
	protected static final String CONSTRUCT_KEYWORD = "json";
	protected static final String TAG_KEYWORD = "json";
	protected static final String OBJPROP_KEYWORD = "object." + TAG_KEYWORD;

	/**
	 * Unparseable JSON
	 */
	protected static final String UNPARSEABLE_JSON = "{\"unparseable\":";

	/**
	 * Valid JSON
	 */
	protected static final String VALID_JSON = "{\"valid\":true}";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	protected static Node node;
	protected static Template template;
	protected static Construct construct;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = create(Node.class, n -> {
			Folder root = create(Folder.class, f -> {
				f.setName("Test Node");
				f.setPublishDir("/");
			}).doNotSave().build();
			n.setFolder(root);
			n.setHostname("json.unparseable.test");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();

		construct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword(CONSTRUCT_KEYWORD);
			c.setName(CONSTRUCT_KEYWORD, 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(JSONPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(PART_KEYWORD);
				p.setName(PART_KEYWORD, 1);
			}).doNotSave().build());
		}).build();

		template = create(Template.class, t -> {
			t.setFolderId(node.getFolder().getId());
			t.setMlId(1);
			t.setName("Template");
			t.setSource("[<node " + TAG_KEYWORD + ">]");

			t.getTemplateTags().put(TAG_KEYWORD, create(TemplateTag.class, tag -> {
				tag.setConstructId(construct.getId());
				tag.setEnabled(true);
				tag.setPublic(true);
				tag.setName(TAG_KEYWORD);
			}).doNotSave().build());
		}).build();

		create(ObjectTagDefinition.class, oe -> {
			oe.setTargetType(Page.TYPE_PAGE);
			oe.setName(TAG_KEYWORD, 1);
			oe.getNodes().add(node);
			ObjectTag objectTag = oe.getObjectTag();

			objectTag.setConstructId(construct.getId());
			objectTag.setEnabled(true);
			objectTag.setName(OBJPROP_KEYWORD);
			objectTag.setObjType(Page.TYPE_PAGE);
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));
		// the tests modify the template tag
		template = update(template, t -> {
			getValue(t.getTemplateTag(TAG_KEYWORD)).setValueText("");
		}).build();
	}

	/**
	 * Test that saving a page with unparseable JSON in a content tag fails with "Bad Request" and is not stored
	 */
	@Test
	public void testContentTagUnparseable() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(page, update -> {
			getValue(update.getContentTag(TAG_KEYWORD)).setValueText(UNPARSEABLE_JSON);
		}).build());

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getContentTag(TAG_KEYWORD)), page));
		assertContentTagText(page, "");
	}

	/**
	 * Test that saving a page with unparseable JSON in a content tag via {@link PageResourceImpl#save(String, PageSaveRequest)} fails with "Bad Request"
	 */
	@Test
	public void testRestContentTagUnparseable() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class,
				() -> supply(() -> new PageResourceImpl().save(page.getId().toString(), saveRequest(page, UNPARSEABLE_JSON))));

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getContentTag(TAG_KEYWORD)), page));
		assertContentTagText(page, "");
	}

	/**
	 * Test that valid JSON can still be saved via {@link PageResourceImpl#save(String, PageSaveRequest)}
	 */
	@Test
	public void testRestContentTagValid() throws NodeException {
		Page page = createPage();

		assertThat(supply(() -> new PageResourceImpl().save(page.getId().toString(), saveRequest(page, VALID_JSON)))).as("Response")
				.hasCode(ResponseCode.OK);
		assertContentTagText(page, VALID_JSON);
	}

	/**
	 * Test that saving a template with unparseable JSON in a template tag fails with "Bad Request"
	 */
	@Test
	public void testTemplateTagUnparseable() throws NodeException {
		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(template, update -> {
			getValue(update.getTemplateTag(TAG_KEYWORD)).setValueText(UNPARSEABLE_JSON);
		}).build());

		assertBadRequest(e, execute(t -> expectedTagMessage(t.getTemplateTag(TAG_KEYWORD)), template));
		consume(t -> {
			assertThat(getValue(t.getTemplateTag(TAG_KEYWORD)).getValueText()).as("Stored JSON").isEqualTo("");
		}, template);
	}

	/**
	 * Test that saving a page with unparseable JSON in an object property fails with "Bad Request"
	 */
	@Test
	public void testObjectTagUnparseable() throws NodeException {
		Page page = createPage();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(page, update -> {
			getValue(update.getObjectTag(TAG_KEYWORD)).setValueText(UNPARSEABLE_JSON);
		}).build());

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getObjectTag(TAG_KEYWORD)), page));
		consume(p -> {
			assertThat(getValue(p.getObjectTag(TAG_KEYWORD)).getValueText()).as("Stored JSON").isEqualTo("");
		}, page);
	}

	/**
	 * Test that saving a construct with unparseable JSON in the default value of a JSON part fails with "Bad Request"
	 */
	@Test
	public void testConstructDefaultValueUnparseable() throws NodeException {
		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct_with_" + TAG_KEYWORD);
			c.setName("construct_with_" + TAG_KEYWORD, 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(JSONPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(PART_KEYWORD);
				p.setName(PART_KEYWORD, 1);
				p.setDefaultValue(create(Value.class, v -> {
					v.setValueText(UNPARSEABLE_JSON);
				}).doNotSave().build());
			}).doNotSave().build());
		}).build());

		assertBadRequest(e, supply(() -> I18NHelper.get("validation.json.part.failed", PART_KEYWORD, I18NHelper.get("validation.json.unparseable"))));
	}

	/**
	 * Test that a page with already stored unparseable JSON (e.g. stored before the validation existed) still renders without the tag
	 */
	@Test
	public void testRenderStoredUnparseable() throws NodeException {
		Page page = createPageWithStoredUnparseableJson();

		try (Trx trx = new Trx(); RenderTypeTrx rTrx = RenderTypeTrx.publish()) {
			assertThat(trx.getTransaction().getObject(page).render()).as("Rendered page").isEqualTo("[]");
			trx.success();
		}
	}

	/**
	 * Test that a page with already stored unparseable JSON can be loaded (so that the JSON can be fixed)
	 */
	@Test
	public void testLoadStoredUnparseable() throws NodeException {
		Page page = createPageWithStoredUnparseableJson();

		PageLoadResponse response = supply(() -> new PageResourceImpl().load(page.getId().toString(), false, false, false, false, false, false,
				false, false, false, false, null, null));

		assertThat(response).as("Response").hasCode(ResponseCode.OK);
		assertThat(response.getPage().getTags().get(TAG_KEYWORD).getProperties().get(PART_KEYWORD).getStringValue()).as("Loaded JSON")
				.isEqualTo(UNPARSEABLE_JSON);
	}

	/**
	 * Create a page using the test template
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage() throws NodeException {
		return create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setName("Page");
		}).build();
	}

	/**
	 * Create a page and store unparseable JSON in its content tag directly in the DB, bypassing the validation
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPageWithStoredUnparseableJson() throws NodeException {
		Page page = update(createPage(), update -> {
			getValue(update.getContentTag(TAG_KEYWORD)).setValueText(VALID_JSON);
		}).build();
		Integer valueId = execute(p -> getValue(p.getContentTag(TAG_KEYWORD)).getId(), page);
		operate(() -> DBUtils.update("UPDATE value SET value_text = ? WHERE id = ?", UNPARSEABLE_JSON, valueId));
		operate(t -> t.clearNodeObjectCache());
		return page;
	}

	/**
	 * Create a save request for the page, which sets the JSON into the content tag
	 * @param page page
	 * @param json JSON
	 * @return save request
	 */
	protected PageSaveRequest saveRequest(Page page, String json) {
		Property prop = new Property();
		prop.setType(Property.Type.RICHTEXT);
		prop.setStringValue(json);
		com.gentics.contentnode.rest.model.Tag tag = new com.gentics.contentnode.rest.model.Tag();
		tag.setType(com.gentics.contentnode.rest.model.Tag.Type.CONTENTTAG);
		tag.setName(TAG_KEYWORD);
		tag.setActive(true);
		tag.setProperties(Map.of(PART_KEYWORD, prop));

		com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
		restPage.setId(page.getId());
		restPage.setTags(Map.of(TAG_KEYWORD, tag));
		PageSaveRequest request = new PageSaveRequest();
		request.setPage(restPage);
		return request;
	}

	/**
	 * Assert that the content tag of the page stores the expected text
	 * @param page page
	 * @param expected expected text
	 * @throws NodeException
	 */
	protected void assertContentTagText(Page page, String expected) throws NodeException {
		consume(p -> {
			assertThat(getValue(p.getContentTag(TAG_KEYWORD)).getValueText()).as("Stored JSON").isEqualTo(expected);
		}, page);
	}

	/**
	 * Get the expected message for unparseable JSON in the given tag (must be called within a transaction)
	 * @param tag tag
	 * @return expected message
	 */
	protected static String expectedTagMessage(Tag tag) {
		return I18NHelper.get("validation.json.tag.part.failed", tag.getName() + " / " + tag.getId(), PART_KEYWORD,
				I18NHelper.get("validation.json.unparseable"));
	}

	/**
	 * Assert that the exception is a "Bad Request" with the expected message
	 * @param e exception (may be null, if nothing was thrown)
	 * @param expectedMessage expected message
	 */
	protected void assertBadRequest(RestMappedException e, String expectedMessage) {
		assertThat(e).as("Thrown exception").isNotNull();
		assertThat(e.getStatus()).as("Response status").isEqualTo(Status.BAD_REQUEST);
		assertThat(e.getResponseCode()).as("Response code").isEqualTo(ResponseCode.INVALIDDATA);
		assertThat(e.getMessageType()).as("Message type").isEqualTo(Message.Type.CRITICAL);
		assertThat(e.getLocalizedMessage()).as("Message").isEqualTo(expectedMessage);
	}

	/**
	 * Get the value of the JSON part from the tag
	 * @param tag tag
	 * @return value
	 * @throws NodeException
	 */
	protected static Value getValue(Tag tag) throws NodeException {
		return tag.getValues().getByKeyname(PART_KEYWORD);
	}
}
