package com.gentics.contentnode.tests.rest.node;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponseOK;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getNodeResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.clear;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getLanguage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestUtils.assertPublishFS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.model.PageLanguageCode;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for changing SEO settings
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class NodeChangeSEOSettingsTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Template template;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode("master", "Master", PublishTarget.FILESYSTEM, getLanguage("de"), getLanguage("en")));

		node = update(node, n -> {
			n.setPublishDir("/");
			n.setUtf8(true);
		});

		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));

		template = supply(() -> createTemplate(node.getFolder(), "Template"));
	}

	/**
	 * Setup start settings (default settings)
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		update(node, n -> {
			n.setOmitPageExtension(false);
			n.setPageLanguageCode(PageLanguageCode.FILENAME);
		});
	}

	/**
	 * Clean node and channel (remove all generated test data)
	 * @throws NodeException
	 */
	@After
	public void clean() throws NodeException {
		operate(() -> {
			clear(node);
			clear(channel);
		});
	}

	/**
	 * Test successful change of the SEO setting
	 * @throws NodeException
	 */
	@Test
	public void testChange() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/publish/path");
		});
		Page page = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setLanguage(getLanguage("de"));
		});

		String path = execute(p -> p.getFullPublishPath(true), page);
		assertThat(path).as("Path for default setting").isEqualTo("/publish/path/");

		assertResponseOK(set(PageLanguageCode.PATH));

		path = execute(p -> p.getFullPublishPath(true), page);
		assertThat(path).as("Path for changed setting").isEqualTo("/de/publish/path/");

		assertResponseOK(set(PageLanguageCode.NONE));

		path = execute(p -> p.getFullPublishPath(true), page);
		assertThat(path).as("Path for changed setting").isEqualTo("/publish/path/");

		assertResponseOK(set(PageLanguageCode.FILENAME));

		path = execute(p -> p.getFullPublishPath(true), page);
		assertThat(path).as("Path for changed setting").isEqualTo("/publish/path/");
	}

	@Test
	public void testConflictMasterPage() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/publish/path");
		});

		// set PATH
		assertResponseOK(set(PageLanguageCode.PATH));

		// create pages with different languages but same name
		Page german = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("Page.html");
			p.setLanguage(getLanguage("de"));
		});
		assertThat(german.getFilename()).as("Filename of german page").isEqualTo("Page.html");
		Page english = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("Page.html");
			p.setLanguage(getLanguage("en"));
		});
		assertThat(english.getFilename()).as("Filename of english page").isEqualTo("Page.html");

		assertResponse(set(PageLanguageCode.FILENAME), ResponseCode.INVALIDDATA, "node saving failed");
	}

	@Test
	public void testConflictChannelPage() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/publish/path");
		});

		// set PATH
		assertResponseOK(set(PageLanguageCode.PATH));

		// create pages with different languages but same name
		Page german = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("Page.html");
			p.setLanguage(getLanguage("de"));
		});
		assertThat(german.getFilename()).as("Filename of german page").isEqualTo("Page.html");
		Page english = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("Page.html");
			p.setLanguage(getLanguage("en"));
			p.setChannelInfo(channel.getId(), null);
		});
		assertThat(english.getFilename()).as("Filename of english page").isEqualTo("Page.html");

		assertResponse(set(PageLanguageCode.FILENAME), ResponseCode.INVALIDDATA, "node saving failed");
	}

	@Test
	public void testConflictMasterFile() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/publish/path");
		});

		// set PATH
		assertResponseOK(set(PageLanguageCode.PATH));

		// create a file and a page with same filename
		File file = supply(() -> createFile(folder, "Page.html", "File data".getBytes()));
		assertThat(file.getFilename()).as("Filename of file").isEqualTo("Page.html");
		Page english = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("Page.html");
			p.setLanguage(getLanguage("en"));
		});
		assertThat(english.getFilename()).as("Filename of english page").isEqualTo("Page.html");

		assertResponse(set(PageLanguageCode.FILENAME), ResponseCode.INVALIDDATA, "node saving failed");
	}

	@Test
	public void testConflictChannelFile() throws NodeException {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/publish/path");
		});

		// set PATH
		assertResponseOK(set(PageLanguageCode.PATH));

		// create a file and a page with same filename
		File file = supply(() -> createFile(folder, "Page.html", "File data".getBytes(), channel));
		assertThat(file.getFilename()).as("Filename of file").isEqualTo("Page.html");
		Page english = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("Page.html");
			p.setLanguage(getLanguage("en"));
		});
		assertThat(english.getFilename()).as("Filename of english page").isEqualTo("Page.html");

		assertResponse(set(PageLanguageCode.FILENAME), ResponseCode.INVALIDDATA, "node saving failed");
	}

	@Test
	public void testRepublishOnChange() throws Exception {
		Folder folder = create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder");
			f.setPublishDir("/publish/path");
		});

		// create and publish a page
		Page german = create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setLanguage(getLanguage("de"));
		});
		german = update(german, Page::publish);

		// run publish process (language code in filename) and assert, that page was published into filesystem
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}
		consume(p -> assertPublishFS(testContext.getPubDir(), p, node, true), german);

		// now cycle through the settings, let the publish process run and assert again
		for (PageLanguageCode plcSetting : Arrays.asList(PageLanguageCode.PATH, PageLanguageCode.NONE,
				PageLanguageCode.FILENAME)) {
			assertResponseOK(set(plcSetting));
			node = execute(Node::reload, node);
			try (Trx trx = new Trx()) {
				testContext.publish(false);
				trx.success();
			}
			consume(p -> assertPublishFS(testContext.getPubDir(), p, node, true), german);
		}
	}

	/**
	 * Set the given page language code
	 * @param pageLanguageCode
	 * @return response
	 * @throws NodeException
	 */
	protected GenericResponse set(PageLanguageCode pageLanguageCode) throws NodeException {
		return supply(() -> {
			com.gentics.contentnode.rest.model.Node updateNode = new com.gentics.contentnode.rest.model.Node();
			updateNode.setPageLanguageCode(pageLanguageCode);
			NodeSaveRequest request = new NodeSaveRequest();
			request.setNode(updateNode);
			return getNodeResource().update(String.valueOf(node.getId()), request);
		});
	}
}
