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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.LangTrx;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Tag.Type;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Test cases for the syntax validation of handlebars templates in tags using the {@link HandlebarsPartType}
 */
public class HandlebarsContentTagTest {
	protected static final String PART_KEYWORD = "template";
	protected static final String CONSTRUCT_KEYWORD = "handlebars";
	protected static final String TAG_KEYWORD = "hbs";
	protected static final String OBJPROP_KEYWORD = "object." + TAG_KEYWORD;

	protected static final String LOGIN = "hbstester";
	protected static final String PASSWORD = "hbstester";

	/**
	 * Valid template
	 */
	protected static final String VALID_TEMPLATE = "{{#if cms.page}}{{cms.page.name}}{{/if}}";

	/**
	 * Template with unknown helpers and partials (which are only resolved when rendering)
	 */
	protected static final String UNKNOWN_HELPER_TEMPLATE = "{{unknownHelper cms.page}}{{#unknownBlock}}x{{/unknownBlock}}{{> unknown.partial}}";

	/**
	 * Template with an unclosed block
	 */
	protected static final String INVALID_TEMPLATE = "{{#if cms.page}}unclosed";

	/**
	 * Line, column and reason of the syntax error in {@link #INVALID_TEMPLATE}
	 */
	protected static final String[] INVALID_TEMPLATE_ERROR = { "1", "24", "found: 'EOF', expected: '{{/'" };

	private static DBTestContext testContext = new DBTestContext();

	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

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
			n.setHostname("handlebars.validation.test");
			n.setPublishDir("/");
			n.setBinaryPublishDir("/");
		}).build();

		construct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword(CONSTRUCT_KEYWORD);
			c.setName(CONSTRUCT_KEYWORD, 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
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
			t.setSource("");

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

		UserGroup nodeGroup = supply(t -> {
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});
		operate(() -> PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM));
		SystemUser tester = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin(LOGIN);
			u.setFirstname("Tester");
			u.setLastname("Tester");
			u.getUserGroups().add(nodeGroup);
		}).build();
		// the password hash is salted with the user ID
		update(tester, u -> {
			u.setPassword(SystemUserFactory.hashPassword(PASSWORD, u.getId()));
		}).build();
	}

	@Before
	public void setup() throws NodeException {
		operate(() -> clear(node));
	}

	/**
	 * Test that a page with a valid template in a content tag can be saved
	 */
	@Test
	public void testContentTagValid() throws NodeException {
		assertContentTagSaved(VALID_TEMPLATE);
	}

	/**
	 * Test that unknown helpers and partials are not considered syntax errors
	 */
	@Test
	public void testContentTagUnknownHelperAndPartial() throws NodeException {
		assertContentTagSaved(UNKNOWN_HELPER_TEMPLATE);
	}

	/**
	 * Test that saving a page with a syntax error in a content tag fails with "Bad Request" and is not stored
	 */
	@Test
	public void testContentTagInvalid() throws NodeException {
		Page page = createPage().build();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(page, update -> {
			getValue(update.getContentTag(TAG_KEYWORD)).setValueText(INVALID_TEMPLATE);
		}).build());

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getContentTag(TAG_KEYWORD)), page));
		assertContentTagText(page, "");
	}

	/**
	 * Test that saving a page with a syntax error in a disabled content tag also fails with "Bad Request" and is not stored
	 */
	@Test
	public void testDisabledContentTagInvalid() throws NodeException {
		Page page = createPage().build();

		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(page, update -> {
			ContentTag tag = update.getContentTag(TAG_KEYWORD);
			tag.setEnabled(false);
			getValue(tag).setValueText(INVALID_TEMPLATE);
		}).build());

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getContentTag(TAG_KEYWORD)), page));
		assertContentTagText(page, "");
	}

	/**
	 * Test that saving a page with a syntax error in a content tag via {@link PageResourceImpl#save(String, PageSaveRequest)} fails with "Bad Request"
	 */
	@Test
	public void testRestContentTagInvalid() throws NodeException {
		Page page = createPage().build();

		RestMappedException e = catchThrowableOfType(RestMappedException.class,
				() -> supply(() -> new PageResourceImpl().save(page.getId().toString(), saveRequest(page, TAG_KEYWORD, Type.CONTENTTAG, INVALID_TEMPLATE))));

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getContentTag(TAG_KEYWORD)), page));
		assertContentTagText(page, "");
	}

	/**
	 * Test that a valid template can be saved via {@link PageResourceImpl#save(String, PageSaveRequest)}
	 */
	@Test
	public void testRestContentTagValid() throws NodeException {
		Page page = createPage().build();

		GenericResponse response = supply(() -> new PageResourceImpl().save(page.getId().toString(), saveRequest(page, TAG_KEYWORD, Type.CONTENTTAG, VALID_TEMPLATE)));

		assertThat(response).as("Response").hasCode(ResponseCode.OK);
		assertContentTagText(page, VALID_TEMPLATE);
	}

	/**
	 * Test that the HTTP request for saving a page with a syntax error in a content tag responds with status 400 and the error message
	 */
	@Test
	public void testHttpContentTagInvalid() throws Exception {
		// the page must not be locked by the system user, because the test user will lock it when saving
		Page page = createPage().unlock().build();
		String expectedMessage = execute(p -> {
			try (LangTrx lTrx = new LangTrx("de")) {
				return expectedTagMessage(p.getContentTag(TAG_KEYWORD));
			}
		}, page);

		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path("page").path("save").path(page.getId().toString())
					.request(MediaType.APPLICATION_JSON_TYPE)
					.post(Entity.json(saveRequest(page, TAG_KEYWORD, Type.CONTENTTAG, INVALID_TEMPLATE)));

			assertThat(response.getStatusInfo().toEnum()).as("Response status").isEqualTo(Status.BAD_REQUEST);
			GenericResponse genericResponse = response.readEntity(GenericResponse.class);
			assertThat(genericResponse).as("Response").hasCode(ResponseCode.INVALIDDATA);
			assertThat(genericResponse.getMessages()).as("Response messages").extracting(Message::getType, Message::getMessage)
					.containsExactly(org.assertj.core.groups.Tuple.tuple(Message.Type.CRITICAL, expectedMessage));
		}
		assertContentTagText(page, "");
	}

	/**
	 * Test that saving a page with a syntax error in an object property fails with "Bad Request"
	 */
	@Test
	public void testObjectTagInvalid() throws NodeException {
		Page page = createPage().build();

		RestMappedException e = catchThrowableOfType(RestMappedException.class,
				() -> supply(() -> new PageResourceImpl().save(page.getId().toString(), saveRequest(page, OBJPROP_KEYWORD, Type.OBJECTTAG, INVALID_TEMPLATE))));

		assertBadRequest(e, execute(p -> expectedTagMessage(p.getObjectTag(TAG_KEYWORD)), page));
		consume(p -> {
			ObjectTag tag = p.getObjectTag(TAG_KEYWORD);
			assertThat(getValue(tag).getValueText()).as("Stored template").isEqualTo("");
		}, page);
	}

	/**
	 * Test that saving a template with a syntax error in a template tag fails with "Bad Request"
	 */
	@Test
	public void testTemplateTagInvalid() throws NodeException {
		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> update(template, update -> {
			getValue(update.getTemplateTag(TAG_KEYWORD)).setValueText(INVALID_TEMPLATE);
		}).build());

		assertBadRequest(e, execute(t -> expectedTagMessage(t.getTemplateTag(TAG_KEYWORD)), template));
		consume(t -> {
			assertThat(getValue(t.getTemplateTag(TAG_KEYWORD)).getValueText()).as("Stored template").isEqualTo("");
		}, template);
	}

	/**
	 * Test that saving a construct with a syntax error in the default value of a handlebars part fails with "Bad Request"
	 */
	@Test
	public void testConstructDefaultValueInvalid() throws NodeException {
		RestMappedException e = catchThrowableOfType(RestMappedException.class, () -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("construct_with_" + TAG_KEYWORD);
			c.setName("construct_with_" + TAG_KEYWORD, 1);
			c.getNodes().add(node);

			c.getParts().add(create(Part.class, p -> {
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname(PART_KEYWORD);
				p.setName(PART_KEYWORD, 1);
				p.setDefaultValue(create(Value.class, v -> {
					v.setValueText(INVALID_TEMPLATE);
				}).doNotSave().build());
			}).doNotSave().build());
		}).build());

		assertBadRequest(e, supply(() -> I18NHelper.get("validation.handlebars.part.failed", PART_KEYWORD, expectedReason())));
	}

	/**
	 * Get a builder for a new page using the test template
	 * @return page builder
	 */
	protected Builder<Page> createPage() {
		return create(Page.class, p -> {
			p.setTemplateId(template.getId());
			p.setFolderId(node.getFolder().getId());
			p.setName("Page");
		});
	}

	/**
	 * Save the given template into the content tag of a new page and assert that it was stored
	 * @param hbsTemplate handlebars template
	 * @throws NodeException
	 */
	protected void assertContentTagSaved(String hbsTemplate) throws NodeException {
		Page page = createPage().build();

		update(page, update -> {
			getValue(update.getContentTag(TAG_KEYWORD)).setValueText(hbsTemplate);
		}).build();

		assertContentTagText(page, hbsTemplate);
	}

	/**
	 * Assert that the content tag of the page stores the expected text
	 * @param page page
	 * @param expected expected text
	 * @throws NodeException
	 */
	protected void assertContentTagText(Page page, String expected) throws NodeException {
		consume(p -> {
			assertThat(getValue(p.getContentTag(TAG_KEYWORD)).getValueText()).as("Stored template").isEqualTo(expected);
		}, page);
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
	 * Get the expected message for the syntax error of {@link #INVALID_TEMPLATE} in the given tag (must be called within a transaction)
	 * @param tag tag
	 * @return expected message
	 * @throws NodeException
	 */
	protected static String expectedTagMessage(com.gentics.contentnode.object.Tag tag) throws NodeException {
		return I18NHelper.get("validation.handlebars.tag.part.failed", tag.getName() + " / " + tag.getId(), PART_KEYWORD, expectedReason());
	}

	/**
	 * Get the expected reason for the syntax error of {@link #INVALID_TEMPLATE}
	 * @return expected reason
	 */
	protected static String expectedReason() {
		return I18NHelper.get("validation.handlebars.syntaxerror", INVALID_TEMPLATE_ERROR);
	}

	/**
	 * Get the value of the handlebars part from the tag
	 * @param tag tag
	 * @return value
	 * @throws NodeException
	 */
	protected static Value getValue(com.gentics.contentnode.object.Tag tag) throws NodeException {
		return tag.getValues().getByKeyname(PART_KEYWORD);
	}

	/**
	 * Create a save request for the page, which sets the template into the tag
	 * @param page page
	 * @param tagName tag name (with prefix "object." for object properties)
	 * @param type tag type
	 * @param hbsTemplate handlebars template
	 * @return save request
	 */
	protected static PageSaveRequest saveRequest(Page page, String tagName, Type type, String hbsTemplate) {
		Property prop = new Property();
		prop.setType(Property.Type.RICHTEXT);
		prop.setStringValue(hbsTemplate);
		Tag tag = new Tag();
		tag.setType(type);
		tag.setName(type == Type.OBJECTTAG ? TAG_KEYWORD : tagName);
		tag.setActive(true);
		tag.setProperties(Map.of(PART_KEYWORD, prop));

		com.gentics.contentnode.rest.model.Page restPage = new com.gentics.contentnode.rest.model.Page();
		restPage.setId(page.getId());
		restPage.setTags(Map.of(tagName, tag));
		PageSaveRequest request = new PageSaveRequest();
		request.setPage(restPage);
		return request;
	}
}
