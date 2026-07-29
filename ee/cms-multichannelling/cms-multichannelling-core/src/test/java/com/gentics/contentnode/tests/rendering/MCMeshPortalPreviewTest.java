package com.gentics.contentnode.tests.rendering;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.NodePartType;
import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.render.RenderUtils;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.PUB_DIR_SEGMENT, Feature.MULTICHANNELLING })
public class MCMeshPortalPreviewTest extends MeshPortalPreviewTestBase {
	/**
	 * REST Application used as "preview" portal
	 */
	@ClassRule
	public static RESTAppContext appContext = new RESTAppContext(new ResourceConfig().registerResources(Resource.builder(PreviewResource.class).build()));

	protected static Node channel;

	private static Construct urlInChannelConstruct;

	private static File targetFile;

	private static Page sourcePage;

	private static String tagName;

	@Override
	protected RESTAppContext getRestAppContext() {
		return appContext;
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		MeshPortalPreviewTestBase.setupOnce();

		node = update(node, upd -> {
			upd.setUrlRenderWayFiles(RenderUrl.LINK_AUTO);
			upd.setUrlRenderWayPages(RenderUrl.LINK_AUTO);
		}).build();

		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		urlInChannelConstruct = create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("url_in_channel");
			c.setName("URL in Channel", 1);

			// File part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("file");
				p.setName("File", 1);
				p.setPartTypeId(getPartTypeId(FileURLPartType.class));
				p.setDefaultValue(create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());

			// Node part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(true);
				p.setKeyname("node");
				p.setName("Node", 1);
				p.setPartTypeId(getPartTypeId(NodePartType.class));
				p.setDefaultValue(create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());

			// hbs part
			c.getParts().add(create(Part.class, p -> {
				p.setEditable(0);
				p.setHidden(false);
				p.setKeyname("hbs");
				p.setName("hbs", 1);
				p.setPartTypeId(getPartTypeId(HandlebarsPartType.class));
				p.setDefaultValue(create(Value.class, v -> {}).doNotSave().build());
			}).doNotSave().build());
		}).build();

		urlInChannelConstruct = update(urlInChannelConstruct, c -> {
			getPartType(HandlebarsPartType.class, c, "hbs").setText("{{#gtx_channel cms.tag.parts.node.id}}{{gtx_render cms.tag.parts.file.url}}{{/gtx_channel}}");
		}).build();

		Folder fileContainerInMaster = create(Folder.class, createFolder -> {
			createFolder.setMotherId(node.getFolder().getId());
			createFolder.setName("File Container");
			createFolder.setPublishDir("/file-in-master");
		}).build();

		Folder fileContainerInChannel = supply(() -> localize(fileContainerInMaster, channel));
		fileContainerInChannel = update(fileContainerInChannel, upd -> {
			upd.setPublishDir("/file-in-channel");
		}).build();

		targetFile = create(File.class, createFile -> {
			createFile.setFolder(node, fileContainerInMaster);
			createFile.setName("file.txt");
			createFile.setFileStream(new ByteArrayInputStream("This is the content".getBytes()));
		}).build();

		Folder folder = create(Folder.class, createFolder -> {
			createFolder.setMotherId(node.getFolder().getId());
			createFolder.setChannelInfo(channel.getId(), null);
			createFolder.setName("Folder");
			createFolder.setPublishDir("/");
		}).build();

		sourcePage = create(Page.class, createPage -> {
			createPage.setFolderId(folder.getId());
			createPage.setChannelInfo(channel.getId(), null);
			createPage.setTemplateId(template.getId());
			createPage.setName("Page");

			ContentTag newTag = createPage.getContent().addContentTag(urlInChannelConstruct.getId());
			tagName = newTag.getName();
			getPartType(LongHTMLPartType.class, createPage.getContent().getContentTag("tag"), "part").setText("<node %s>".formatted(newTag.getName()));
		}).build();
	}

	@Test
	public void testFileInMaster() throws NodeException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/render");
		}).build();

		sourcePage = update(sourcePage, upd -> {
			ContentTag tag = upd.getContentTag(tagName);
			getPartType(FileURLPartType.class, tag, "file").setTargetFile(targetFile);
			getPartType(NodePartType.class, tag, "node").setNode(node);
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(sourcePage, RenderType.EM_PUBLISH));
		assertThat(preview).as("Page preview").isEqualTo("<div class=\"tag\">/home/file-in-master/file.txt</div><div class=\"live\"></div>");
	}

	@Test
	public void testFileInChannel() throws NodeException {
		update(node, n -> {
			setMeshPreviewUrl(n, getRestAppContext().getBaseUri() + "preview/render");
		}).build();

		sourcePage = update(sourcePage, upd -> {
			ContentTag tag = upd.getContentTag(tagName);
			getPartType(FileURLPartType.class, tag, "file").setTargetFile(targetFile);
			getPartType(NodePartType.class, tag, "node").setNode(channel);
		}).build();

		String preview = Trx.supply(() -> RenderUtils.getPreviewTemplate(sourcePage, RenderType.EM_PUBLISH));
		assertThat(preview).as("Page preview").isEqualTo("<div class=\"tag\">http://channel/file-in-channel/file.txt</div><div class=\"live\"></div>");
	}
}
