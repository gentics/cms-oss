package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.cleanMesh;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.crResource;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.shaded.org.bouncycastle.util.Objects;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.object.ContentRepository;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
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
@GCNFeature(set = { Feature.INSTANT_CR_PUBLISHING, Feature.MULTICHANNELLING })
public class MCPagePublishMessageTest {
	/**
	 * Name of the mesh project
	 */
	public final static String MESH_PROJECT_NAME = "testproject";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@ClassRule
	public static MeshContext mesh = new MeshContext();

	private static Message PAGE_PUBLISHED_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("Die Seite 'Test Page' wurde veröffentlicht.");

	private static Message PAGE_PUBLISHED_STATUS_MESSAGE = new Message().setType(Type.SUCCESS)
			.setMessage("Die Seite 'Test Page' wurde in den Status 'veröffentlicht' gesetzt.");

	private static Message PUBLISH_CR_NOT_READY_MESSAGE = new Message().setType(Type.WARNING).setMessage(
			"Die sofortige Veröffentlichung von 'Test Page' ist derzeit nicht möglich, da das Content.Repository nicht bereit ist.");

	private static Message PAGE_INHERITED_MESSAGE = new Message().setType(Type.CRITICAL)
			.setMessage("Eine geerbte Seite muss in ihrem eigenen Node veröffentlicht werden.");

	private static Node node;

	private static Integer crId;

	private static ContentRepository cr;

	private static Template template;

	private static Node channel;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		node = supply(() -> createNode("node", "Node", PublishTarget.CONTENTREPOSITORY, getLanguage("de")));
		crId = createMeshCR(mesh, MESH_PROJECT_NAME);
		cr = supply(t -> t.getObject(ContentRepository.class, crId));

		cr = update(cr, upd -> {
			upd.setProjectPerNode(true);
		}).build();

		node = update(node, upd -> {
			upd.setPubDirSegment(false);
			upd.setContentrepositoryId(crId);
		}).build();

		channel = supply(() -> createChannel(node, "Channel", "channel", "/", PublishTarget.NONE));

		channel = update(channel, upd -> {
			upd.setPubDirSegment(false);
			upd.setPublishContentmap(true);
			upd.setContentrepositoryId(crId);
		}).build();

		template = create(Template.class, create -> {
			create.setName("Test Template");
			create.setFolderId(node.getFolder().getId());
			create.setSource("");
		}).build();
	}

	@Before
	public void setup() throws Exception {
		cleanMesh(mesh.client());
		operate(() -> clear(node));

		operate(() -> {
			for (Node c : node.getChannels()) {
				if (!Objects.areEqual(c, channel)) {
					c.delete(true);
				}
			}
		});

		cr = update(cr, upd -> {
			upd.setInstantPublishing(false);
		}).build();
	}

	/**
	 * Test publishing a local page in a channel
	 * @throws Exception
	 */
	@Test
	public void testPublishLocalInChannel() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();
		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", channel);

		// publish test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), channel.getId(), request);
		}, page);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_MESSAGE);
	}

	/**
	 * Test publishing a localized page in a channel
	 * @throws Exception
	 */
	@Test
	public void testPublishLocalizedInChannel() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();
		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", node);
		Page localizedPage = execute(p -> localize(p, channel), page);

		// publish localized test page
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), channel.getId(), request);
		}, localizedPage);

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_MESSAGE);
	}

	/**
	 * Test publishing an inherited page in a channel
	 * @throws Exception
	 */
	@Test
	public void testPublishInheritedInChannel() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();
		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", node);

		// publish the inherited test page in channel
		GenericResponse response = execute(p -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(p.getId()), channel.getId(), request);
		}, page);

		// assert response messages
		assertResponse(response, ResponseCode.INVALIDDATA);
		assertMessages(response, PAGE_INHERITED_MESSAGE);
	}

	/**
	 * Test publishing a local page in a channel, which does not exist in the Mesh CR (as branch)
	 * @throws Exception
	 */
	@Test
	public void testPublishInChannelNotExistent() throws Exception {
		// enabled instant publishing
		cr = update(cr, upd -> {
			upd.setInstantPublishing(true);
		}).build();
		// repair the CR (so it is ready for instant publishing)
		assertResponseOK(crResource.repair(Integer.toString(crId), 0));

		Node otherChannel = supply(() -> createChannel(node, "Other Channel", "other.channel", "/", PublishTarget.NONE));
		otherChannel = update(otherChannel, upd -> {
			upd.setPubDirSegment(false);
			upd.setPublishContentmap(true);
			upd.setContentrepositoryId(crId);
		}).build();

		MeshPublisher.clearCachedData();

		// create test page
		Page page = createTestPage("Test Page", "testpage.html", otherChannel);

		// publish the test page in channel
		GenericResponse response = execute(pair -> {
			PagePublishRequest request = new PagePublishRequest();
			return ContentNodeRESTUtils.getPageResource().publish(Integer.toString(pair.getLeft().getId()), pair.getRight().getId(), request);
		}, Pair.of(page, otherChannel));

		// assert response messages
		assertResponseOK(response);
		assertMessages(response, PAGE_PUBLISHED_STATUS_MESSAGE, PUBLISH_CR_NOT_READY_MESSAGE);
	}

	/**
	 * Create a test page with given name and filename
	 * @param name test page name
	 * @param filename test page filename
	 * @param channel optional channel to create the page in
	 * @return test page
	 * @throws NodeException
	 */
	protected Page createTestPage(String name, String filename, Node channel) throws NodeException {
		return create(Page.class, create -> {
			create.setFolder(node, node.getFolder());
			if (channel != null && channel.isChannel()) {
				create.setChannelInfo(channel.getId(), null);
			}
			create.setTemplateId(template.getId());
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
