package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_UPDATE;
import static com.gentics.contentnode.perm.PermHandler.PERM_PAGE_VIEW;
import static com.gentics.contentnode.perm.PermHandler.PERM_VIEW;
import static com.gentics.contentnode.perm.PermHandler.setPermissions;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.HTMLPartType;
import com.gentics.contentnode.perm.PermHandler.Permission;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.Message.Type;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.mesh.MeshContext;

/**
 * Test cases for messages returned for page publish requests
 */
@GCNFeature(set = { Feature.INSTANT_CR_PUBLISHING })
public class PagePublishMessageTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Message PAGE_PUBLISHED_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("Die Seite wurde veröffentlicht.");

	private static Message PAGE_PUBLISHED_STATUS_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("Die Seite wurde in den Status \"veröffentlicht\" gesetzt.");

	private static Message PAGE_PUBLISH_AT_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("'Veröffentlichen am' wurde für die Seite gesetzt.");

	private static Message PAGE_PUBLISH_QUEUE_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("Die Veröffentlichung der Seite wurde beantragt.");

	private static Message PAGE_PUBLISH_AT_QUEUE_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("'Veröffentlichen am' der Seite wurde beantragt.");

	private static Message CR_NOT_READY_MESSAGE = new Message().setType(Type.WARNING).setMessage(
			"Die sofortige Veröffentlichung von Test Page ist derzeit nicht möglich, da das Content.Repository nicht bereit ist.");

	private static Message CR_UNAVAILABLE_MESSAGE = new Message().setType(Type.WARNING)
			.setMessage("Beim Schreiben der Seite Test Page in das Content.Repository ist ein Fehler aufgetreten.");

	private static Message CONFLICT_MESSAGE = new Message().setType(Type.WARNING).setMessage(
			"Die sofortige Veröffentlichung von Test Page ist wegen eines Konfliktes derzeit nicht möglich.");

	private static Message PAGE_LOCKED_MESSAGE = new Message().setType(Type.CRITICAL).setMessage(
			"Die Seite /Node/Test Page kann nicht bearbeitet werden, weil sie gerade von einem anderen Benutzer gesperrt ist.");

	private static Message PAGE_MANDATORY_TAGS_MESSAGE = new Message().setType(Type.CRITICAL).setMessage(
			"Die Seite \"Test Page\" wurde nicht veröffentlicht, weil folgende Tags nicht korrekt ausgefüllt sind: html");

	private static Node node;

	private static Integer crId;

	private static ContentRepository cr;

	private static String meshCrUrl;

	private static Integer htmlConstructId;

	private static Template template;

	private static SystemUser testUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, getLanguage("de")));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);
		cr = supply(t -> t.getObject(ContentRepository.class, crId));

		node = update(node, upd -> {
			upd.setPubDirSegment(false);
			upd.setContentrepositoryId(crId);
		}).build();

		meshCrUrl = supply(() -> cr.getEffectiveUrl());

		htmlConstructId = supply(() -> createConstruct(node, HTMLPartType.class, "html", "html"));
		Construct construct = execute(id -> TransactionManager.getCurrentTransaction().getObject(Construct.class, id),
				htmlConstructId);
		construct = update(construct, upd -> {
			for (Part part : upd.getParts()) {
				part.setRequired(true);
			}
		}).build();

		template = create(Template.class, create -> {
			create.setName("Test Template");
			create.setFolderId(node.getFolder().getId());
			create.setSource("<node html>");

			TemplateTag templateTag = create(TemplateTag.class, createTag -> {
				createTag.setConstructId(htmlConstructId);
				createTag.setEnabled(true);
				createTag.setPublic(true);
				createTag.setMandatory(true);
				createTag.setName("html");
			}).doNotSave().build();

			create.getTags().put("html", templateTag);
		}).build();

		UserGroup testGroup = create(UserGroup.class, create -> {
			create.setMotherId(NODE_GROUP_ID);
			create.setName("Test Group");
		}).build();

		testUser = create(SystemUser.class, create -> {
			create.setActive(true);
			create.setLogin("test");
			create.setPassword("test");
			create.getUserGroups().add(testGroup);
		}).build();

		operate(() -> {
			String perm = new Permission(PERM_VIEW, PERM_PAGE_VIEW, PERM_PAGE_UPDATE).toString();
			setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(testGroup), perm);
		});
	}

	@Before
	public void setup() throws NodeException {
		cleanMesh(mesh.client());
		operate(() -> clear(node));
		cr = update(cr, upd -> {
			upd.setUrl(meshCrUrl);
			upd.setInstantPublishing(false);
		}).build();
	}

	/**
	 * Test publishing with successful instant publishing
	 * @throws Exception
	 */
	@Test
	public void testWithInstantPublishing() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();
		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_MESSAGE);
	}

	/**
	 * Test publishing with disabled instant publishing
	 * @throws NodeException
	 */
	@Test
	public void testWithoutInstantPublishing() throws NodeException {
		// disable instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(false);
		}).build();

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_STATUS_MESSAGE);
	}

	/**
	 * Test publishing with invalid CR
	 * @throws NodeException
	 */
	@Test
	public void testWithCRInvalid() throws NodeException {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_STATUS_MESSAGE, CR_NOT_READY_MESSAGE);
	}

	/**
	 * Test publishing with unavailable CR
	 * @throws NodeException
	 */
	@Test
	public void testWithCRUnavailable() throws NodeException {
		// enabled instant publishing and change url to something inexistent
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
			upd.setUrl("this.does.not.exist:8080/bla");
		}).build();

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_STATUS_MESSAGE, CR_UNAVAILABLE_MESSAGE);
	}

	/**
	 * Test publishing with a conflict in the CR
	 * @throws Exception
	 */
	@Test
	public void testWithConflictInCR() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page, which will create the conflict lates
		Page conflictingPage = createTestPage("Test Page", "testpage.html", true);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, conflictingPage);

		// assert response messages (page should be published now)
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_MESSAGE);

		// disable instant publishing, so we can prepare for the conflict
		cr = update(cr, upd -> {
			upd.setInstantPublishing(false);
		}).build();
		// rename the page and publish it (without writing to CR)
		conflictingPage = update(conflictingPage, upd -> {
			upd.setName("Other Test Page");
			upd.setFilename("other_testpage.html");
		}).publish().build();

		// re-enable instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// create test page (having the same name and filename as the conflicting page earlier)
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page
		response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_STATUS_MESSAGE, CONFLICT_MESSAGE);
	}

	/**
	 * Test setting the publish at time
	 * @throws Exception
	 */
	@Test
	public void testPublishAt() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page at time in the future
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			int now = (int) (System.currentTimeMillis() / 1000L);
			request.setAt(now + 86400);
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISH_AT_MESSAGE);
	}

	/**
	 * Test publishing the page with a user without publish permission
	 * @throws Exception
	 */
	@Test
	public void testPublishQueue() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page with the test user
		GenericResponse response = execute(testUser, p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISH_QUEUE_MESSAGE);
	}

	/**
	 * Test publishing the page at a timestamp with a user without publish permission
	 * @throws Exception
	 */
	@Test
	public void testPublishAtQueue() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// publish test page with the test user
		GenericResponse response = execute(testUser, p -> {
			PagePublishRequest request = new PagePublishRequest();
			int now = (int) (System.currentTimeMillis() / 1000L);
			request.setAt(now + 86400);
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISH_AT_QUEUE_MESSAGE);
	}

	/**
	 * Test publishing a page which is locked by another user
	 * @throws Exception
	 */
	@Test
	public void testLocked() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", true);

		// lock the page with another user
		try (Trx trx = new Trx(testUser)) {
			trx.getTransaction().getObject(page, true);
			trx.success();
		}

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponse(response, ResponseCode.PERMISSION);
		assertMessages(response, PAGE_LOCKED_MESSAGE);
	}

	/**
	 * Test publishing a page that does not have all mandatory tags filled
	 * @throws Exception
	 */
	@Test
	public void testMandatoryTagsNotFilled() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();

		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", false);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), node.getId(), request);
		}, page);

		// assert response messages
		assertResponse(response, ResponseCode.INVALIDDATA);
		assertMessages(response, PAGE_MANDATORY_TAGS_MESSAGE);

	}

	/**
	 * Create a test page with given name and filename
	 * @param name test page name
	 * @param filename test page filename
	 * @param fillTag true when the tag shall be filled, false if not
	 * @return test page
	 * @throws NodeException
	 */
	protected Page createTestPage(String name, String filename, boolean fillTag) throws NodeException {
		return create(Page.class, create -> {
			create.setFolder(node, node.getFolder());
			create.setTemplateId(template.getId());
			if (fillTag) {
				getPartType(HTMLPartType.class, create.getContentTag("html"), "html").setText("Content");
			}
			create.setName(name);
			create.setFilename(filename);
			create.setLanguage(getLanguage("de"));
		}).unlock().build();
	}

	/**
	 * Assert that the response contains only the given messages (in any order). Messages are compared by "type" and "message"
	 * @param response response
	 * @param expected expected messages
	 */
	protected void assertMessages(GenericResponse response, Message... expected) {
		assertThat(response.getMessages()).as("Response Messages").usingElementComparatorOnFields("type", "message")
				.containsOnly(expected);
	}
}
