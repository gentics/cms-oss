package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertResponseCodeOk;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.aloha.AlohaRenderer;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.url.DynamicUrlFactory;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.TagmapEntry.AttributeType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.model.request.LinksType;
import com.gentics.contentnode.rest.model.response.PageRenderResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.json.JsonUtil;

/**
 * Abstract base class containing test cases for testing mesh portal preview URLs
 */
public abstract class AbstractMeshPortalPreviewTestCases extends MeshPortalPreviewTestBase {
	/**
	 * Test that portal preview is null when no CR set
	 * @throws NodeException
	 */
	@Test
	public void testNoCR() throws NodeException {
		update(node, n -> {
			n.setContentrepositoryId(null);
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test that portal preview is null when non-mesh CR is set
	 * @throws NodeException
	 */
	@Test
	public void testWrongCR() throws NodeException {
		update(node, n -> {
			n.setContentrepositoryId(otherCrId);
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test that portal preview is null when node does not publish into CR
	 * @throws NodeException
	 */
	@Test
	public void testNotPublishCR() throws NodeException {
		update(node, n -> {
			n.setPublishContentmap(false);
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test that portal preview is null when feature is off
	 * @throws NodeException
	 */
	@Test
	public void testNoFeature() throws NodeException {
		try (FeatureClosure feature = new FeatureClosure(Feature.MESH_CONTENTREPOSITORY, false)) {
			String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
			assertThat(preview).as("Preview").isNull();
		}
	}

	/**
	 * Test that portal preview is null when preview URL is not set
	 * @throws NodeException
	 */
	@Test
	public void testNoPreviewUrl() throws NodeException {
		update(node, n -> {
			setMeshPreviewUrl(n, null);
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNull();
	}

	/**
	 * Test data posted to preview URL for Preview
	 * @throws NodeException
	 */
	@Test
	public void testPreview() throws NodeException {
		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getUuid()).as("UUID").isEqualTo(supply(() -> MeshPublisher.getMeshUuid(page)));
		assertThat(sentNode.getFields().getStringField("tag")).as("Field 'tag'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tag").getString()).as("Field 'tag' value").isEqualTo("This is the content");
		assertThat(sentNode.getFields().getStringField("tagpart")).as("Field 'tagpart'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tagpart").getString()).as("Field 'tagpart' value").isEqualTo("This is the content");
		assertThat(sentNode.getFields().getNodeField("pagelink")).as("Field 'pagelink'").isNotNull();
		assertThat(sentNode.getFields().getNodeField("pagelink").getUuid()).as("Field 'pagelink'").isEqualTo(supply(() -> MeshPublisher.getMeshUuid(page)));
		assertThat(sentNode.getFields().getNodeFieldList("pagelinks")).as("Field 'pagelinks'").isNotNull();
		assertThat(sentNode.getFields().getNodeFieldList("pagelinks").getItems()).as("Field 'pagelinks'").usingElementComparatorOnFields("uuid")
				.containsOnly(new NodeFieldListItemImpl().setUuid(supply(() -> MeshPublisher.getMeshUuid(page))));
	}

	/**
	 * Test data posted to preview URL for Edit Mode
	 * @throws NodeException
	 */
	@Test
	public void testEdit() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));

		// make the live editable construct not live editable, because that would spoil the response (so that it is not valid json any more)
		update(liveEditableConstruct, c -> {
			c.getParts().get(0).setEditable(1);
		}).build();

		String expectedEdit = Trx.supply(user, () -> {
			try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA)) {
				rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
				return page.render("<node tag>", TransactionManager.getCurrentTransaction().getRenderResult(), null, null, null, null);
			}
		});

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getUuid()).as("UUID").isEqualTo(supply(() -> MeshPublisher.getMeshUuid(page)));
		assertThat(sentNode.getFields().getStringField("tag")).as("Field 'tag'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tag").getString()).as("Field 'tag' value").isEqualTo(expectedEdit);
		assertThat(sentNode.getFields().getStringField("tagpart")).as("Field 'tagpart'").isNotNull();
		assertThat(sentNode.getFields().getStringField("tagpart").getString()).as("Field 'tagpart' value").isEqualTo("This is the content");
	}

	/**
	 * Test path sent to preview URL
	 * @throws NodeException
	 */
	@Test
	public void testPreviewPath() throws NodeException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/path");
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Posted path").isEqualTo("Content.node/home/");
	}

	/**
	 * Test preview with a micronode
	 * @throws Exception
	 */
	@Test
	public void testMicronode() throws Exception {
		addTestTagmapEntry(Page.TYPE_PAGE, "singletag", "page.tags.tag", AttributeType.micronode.getType(), false);

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);

		assertThat(sentNode.getFields().getMicronodeField("singletag")).as("Field 'singletag'").usingComparator(MODEL_COMPARATOR)
				.isEqualTo(reference("This is the content"));
	}

	/**
	 * Test preview with list of micronodes
	 * @throws Exception
	 */
	@Test
	public void testMicronodeList() throws Exception {
		addTestTagmapEntry(Page.TYPE_PAGE, "tags", "page.tags", AttributeType.micronode.getType(), true);

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getMicronodeFieldList("tags")).as("Field 'tags'").isNotNull();
		assertThat(sentNode.getFields().getMicronodeFieldList("tags").getItems()).as("Micronodes").usingElementComparator(MODEL_COMPARATOR)
				.containsOnly(reference("This is the content"), reference("Embedded content"));
	}

	/**
	 * Create Reference MicronodeResponse
	 * @param content tag content
	 * @return MicronodeResponse
	 */
	protected MicronodeResponse reference(String content) {
		MicronodeResponse ref = new MicronodeResponse().setMicroschema(new MicroschemaReferenceImpl().setName("test_construct"));
		ref.getFields().put("part", new StringFieldImpl().setString(content));
		return ref;
	}

	/**
	 * Test that rendering the preview will use the SID, which is set in the DynamicUrlFactory of the RenderType in the surrounding transaction.
	 * @throws NodeException
	 */
	@Test
	public void testSidInUrl() throws NodeException {
		// make the live editable construct not live editable, because that would spoil the response (so that it is not valid json any more)
		update(liveEditableConstruct, c -> {
			c.getParts().get(0).setEditable(1);
		}).build();

		Integer nodeId = Trx.supply(() -> node.getId());
		Integer pageId = Trx.supply(() -> page.getId());
		String sid = "thisisthesid";
		String expectedUrl = String.format("/alohapage?nodeid=%d&language=1&sid=%s&real=newview&realid=%d", nodeId, sid, pageId);
		String preview = null;
		try (FeatureClosure f = new FeatureClosure(Feature.MANAGELINKURL_ONLYFORPUBLISH, true)) {
			preview = Trx.supply(t -> {
				RenderType r = new RenderType();
				r.setRenderUrlFactory(new DynamicUrlFactory(sid));
				t.setRenderType(r);
				return RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA);
			});
		}
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getStringField("pageurl")).as("Page URL Field").isNotNull();
		assertThat(sentNode.getFields().getStringField("pageurl").getString()).as("Page URL Field").isEqualTo(expectedUrl);
	}

	/**
	 * Test adding renderMode query parameter
	 * @throws NodeException
	 */
	@Test
	public void testRenderModeParam() throws NodeException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/query");
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Posted query").isEqualTo("{renderMode=[preview]}");

		preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA));
		assertThat(preview).as("Posted query").isEqualTo("{renderMode=[edit]}");
	}

	/**
	 * Test rendering the page in preview mode
	 * @throws NodeException
	 */
	@Test
	public void testRenderPreview() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/render");
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		String expectedPreview = Trx.supply(user, () -> {
			try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA_READONLY)) {
				rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
				return page.render();
			}
		});
		assertThat(preview).as("Rendered page").isEqualTo(expectedPreview);
	}

	/**
	 * Test rendering the page in edit mode
	 * @throws NodeException
	 */
	@Test
	public void testRenderEdit() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/render");
		}).build();

		String edit = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA));
		String expectedEdit = Trx.supply(user, () -> {
			try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA)) {
				rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
				return page.render();
			}
		});
		assertThat(edit).as("Rendered page").isEqualTo(expectedEdit);
	}

	/**
	 * Test rendering a single tag in edit mode
	 * @throws NodeException
	 */
	@Test
	public void testRenderTag() throws NodeException {
		SystemUser user = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/render");
		}).build();

		Set<String> tagNames = Trx.supply(() -> page.getContentTags().keySet());
		for (String tag : tagNames) {
			PageRenderResponse response = Trx.supply(user,
					() -> new PageResourceImpl().renderTag(Integer.toString(page.getId()), tag, null, null, LinksType.backend));
			assertResponseCodeOk(response);

			String tagEdit = response.getContent();

			String expectedTagEdit = Trx.supply(user, t -> {
				try (RenderTypeTrx rt = new RenderTypeTrx(RenderType.EM_ALOHA)) {
					rt.get().setParameter(AlohaRenderer.RENDER_SETTINGS, false);
					return page.render("<node " + tag + ">", t.getRenderResult(), null, null, null, null);
				}
			});
			assertThat(tagEdit).as("Rendered tag '" + tag + "'").isEqualTo(expectedTagEdit);
		}
	}

	/**
	 * Test call to endpoint, which "freezes" (i.e. does not return within 1 minute)
	 * @throws NodeException
	 */
	@Test(timeout = 20_000L)
	public void testFreeze() throws NodeException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/freeze");
		}).build();

		Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
	}

	/**
	 * Test rendering translated folder data
	 * @throws NodeException
	 */
	@Test
	public void testRenderTranslatedFolder() throws NodeException {
		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(page, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		NodeResponse sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getStringField("foldername")).as("Field 'foldername'").isNotNull();
		assertThat(sentNode.getFields().getStringField("foldername").getString()).as("Field 'foldername' value").isEqualTo("Ordner Name auf Deutsch");

		preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(translation, RenderType.EM_ALOHA_READONLY));
		assertThat(preview).as("Preview").isNotNull();
		sentNode = JsonUtil.readValue(preview, NodeResponse.class);
		assertThat(sentNode.getFields().getStringField("foldername")).as("Field 'foldername'").isNotNull();
		assertThat(sentNode.getFields().getStringField("foldername").getString()).as("Field 'foldername' value").isEqualTo("Node");
	}

	/**
	 * Test rendering a versioned content
	 * @throws NodeException
	 * @throws RestException
	 */
	@Test
	public void testRenderVersion() throws NodeException, RestException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/renderplain");
		}).build();
		int versionTimestamp = supply(() -> page.getVersion().getDate().getIntTimestamp());

		String currentVersionPreview, oldVersionPreview;
		try (LoggedInClient client = restContext.client("system", "system")) {
			oldVersionPreview = client.get().base().path("page").path("render").path("content")
					.path(Integer.toString(page.getId())).queryParam("version", OLD_TIMESTAMP).request()
					.get(String.class);
			currentVersionPreview = client.get().base().path("page").path("render").path("content")
					.path(Integer.toString(page.getId())).queryParam("version", versionTimestamp).request()
					.get(String.class);
		}

		assertThat(oldVersionPreview).as("Preview of old version").isEqualTo(
				"tag: This is the old content, live: Live <b>editable</b> content, containing also embedded tags: [Embedded content].");

		assertThat(currentVersionPreview).as("Preview of current version").isEqualTo(
				"tag: This is the content, live: Live <b>editable</b> content, containing also embedded tags: [Embedded content].");
	}

	/**
	 * Test rendering a version diff
	 * @throws NodeException
	 * @throws RestException
	 */
	@Test
	public void testRenderVersionDiff() throws NodeException, RestException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/renderplain");
		}).build();

		int versionTimestamp = supply(() -> page.getVersion().getDate().getIntTimestamp());

		String diff;
		try (LoggedInClient client = restContext.client("system", "system")) {
			diff = client.get().base().path("page").path("diff").path("versions").path(Integer.toString(page.getId()))
					.queryParam("old", OLD_TIMESTAMP).queryParam("new", versionTimestamp).request().get(String.class);
		}

		assertThat(diff).as("Version diff").isEqualTo(
				"tag: This is the <del class='diff modified gtx-diff'>old </del>content, live: Live <b>editable</b> content, containing also embedded tags: [Embedded content].");
	}
}
