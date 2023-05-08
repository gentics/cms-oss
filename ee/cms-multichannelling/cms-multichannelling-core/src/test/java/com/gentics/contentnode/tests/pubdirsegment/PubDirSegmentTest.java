package com.gentics.contentnode.tests.pubdirsegment;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.FeatureClosure;
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.factory.url.StaticUrlFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.render.RenderUrl;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for publish directory segments
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.PUB_DIR_SEGMENT, Feature.MULTICHANNELLING })
public class PubDirSegmentTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Node having the pub dir segments activated
	 */
	private static Node nodeWithFeature;

	/**
	 * Channel of the node with the feature
	 */
	private static Node channelWithFeature;

	/**
	 * Node not having the pub dir segments activated
	 */
	private static Node nodeWithoutFeature;

	/**
	 * Channel of the node without the feature
	 */
	private static Node channelWithoutFeature;

	/**
	 * Template for pages
	 */
	private static Template template;

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		nodeWithFeature = Trx.supply(() -> update(createNode(), n -> {
			n.setPubDirSegment(true);
			n.setPublishDir("");
			n.setBinaryPublishDir("");
			n.getFolder().setPublishDir("");
			n.setPublishFilesystem(true);
			n.setHostname("with");
		}));
		channelWithFeature = Trx.supply(() -> createChannel(nodeWithFeature, "Channel With", "channelwith", "/", PublishTarget.FILESYSTEM));
		nodeWithoutFeature = Trx.supply(() -> update(createNode(), n -> {
			n.setPubDirSegment(false);
			n.setPublishDir("");
			n.setBinaryPublishDir("");
			n.getFolder().setPublishDir("");
			n.setPublishFilesystem(true);
			n.setHostname("without");
		}));
		channelWithoutFeature = Trx.supply(() -> createChannel(nodeWithoutFeature, "Channel without", "channelwithout", "/", PublishTarget.FILESYSTEM));
		template = Trx.supply(() -> createTemplate(nodeWithFeature.getFolder(), "Template"));
	}

	@Parameters(name = "{index}: feature {0}, folder level 1 in {1}, folder level 2 in {2}, object in {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean feature : Arrays.asList(true, false)) {
			for (MCSetting level1Location : MCSetting.values()) {
				for (MCSetting level2Location : MCSetting.values()) {
					for (MCSetting objectLocation : MCSetting.values()) {
						data.add(new Object[] { feature, level1Location, level2Location, objectLocation });
					}
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean feature;

	@Parameter(1)
	public MCSetting level1Location;

	@Parameter(2)
	public MCSetting level2Location;

	@Parameter(3)
	public MCSetting objectLocation;

	/**
	 * Tested node
	 */
	protected Node node;

	/**
	 * Tested channel
	 */
	protected Node channel;

	/**
	 * Rendered node
	 */
	protected Node renderedNode;

	/**
	 * Setup test data (clean old data)
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		Trx.operate(t -> {
			for (Node n : Arrays.asList(nodeWithFeature, nodeWithoutFeature)) {
				for (Folder f : n.getFolder().getChildFolders()) {
					t.getObject(f, true).delete(true);
				}
			}
		});
		node = feature ? nodeWithFeature : nodeWithoutFeature;
		channel = feature ? channelWithFeature : channelWithoutFeature;
		switch (objectLocation) {
		case master:
			renderedNode = node;
			break;
		case channel:
			renderedNode = channel;
			break;
		}
	}

	/**
	 * Test rendering a URL to a page
	 * @throws NodeException
	 */
	@Test
	public void testRenderPageUrl() throws NodeException {
		Folder folder1 = createLevel1();
		Folder folder2 = createLevel2(folder1);
		Page page = createPage(folder2);

		String url = Trx.supply(() -> {
			try (ChannelTrx cTrx = new ChannelTrx(renderedNode)) {
				try (RenderTypeTrx rTrx = RenderTypeTrx.publish(page)) {
					StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINKWAY_ABS, RenderUrl.LINKWAY_ABS, null);
					return urlFactory.createRenderUrl(Page.class, page.getId()).toString();
				}
			}
		});

		assertThat(url).as("Page URL").isEqualTo(getExpectedPath(getExpectedPageFilename()));
	}

	/**
	 * Test rendering a URL to a file
	 * @throws NodeException
	 * @throws IOException
	 */
	@Test
	public void testRenderFileUrl() throws NodeException, IOException {
		Folder folder1 = createLevel1();
		Folder folder2 = createLevel2(folder1);
		File file = createFile(folder2);

		String url = Trx.supply(() -> {
			try (ChannelTrx cTrx = new ChannelTrx(renderedNode)) {
				try (RenderTypeTrx rTrx = RenderTypeTrx.publish(file)) {
					StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINKWAY_ABS, RenderUrl.LINKWAY_ABS, null);
					return urlFactory.createRenderUrl(File.class, file.getId()).toString();
				}
			}
		});

		assertThat(url).as("File URL").isEqualTo(getExpectedPath(getExpectedFilename()));
	}

	/**
	 * Test publishing a page into the Filesystem
	 * @throws Exception
	 */
	@Test
	public void testPublishPage() throws Exception {
		doPagePublishTest(false);
	}

	/**
	 * Test publishing a page into the Filesystem using multithreaded publishing
	 * @throws Exception
	 */
	@Test
	public void testPublishPageMultithreaded() throws Exception {
		doPagePublishTest(true);
	}

	/**
	 * Do the page publish test
	 * @param multithreaded true to use multithreaded publishing
	 * @throws Exception
	 */
	protected void doPagePublishTest(boolean multithreaded) throws Exception {
		Folder folder1 = createLevel1();
		Folder folder2 = createLevel2(folder1);
		Page page = createPage(folder2);

		try (FeatureClosure mtp = new FeatureClosure(Feature.MULTITHREADED_PUBLISHING, multithreaded)) {
			testContext.getContext().publish(false);
		}

		assertThat(new java.io.File(testContext.getPubDir(), String.format("%s/%s", getExpectedHostname(), getExpectedPath(getExpectedPageFilename()))))
				.as("published page").exists();

		Trx.operate(() -> {
			if (feature) {
				assertThat(page).as("Published page").dependsOn(folder1, "pub_dir", 0).dependsOn(folder2, "pub_dir", 0);
			} else {
				assertThat(page).as("Published page").dependsOn(folder2, "pub_dir", 0).doesNotDependOn(folder1, "pub_dir", 0);
			}
		});
	}

	/**
	 * Test publishing a file into the Filesystem
	 * @throws Exception
	 */
	@Test
	public void testPublishFile() throws Exception {
		Folder folder1 = createLevel1();
		Folder folder2 = createLevel2(folder1);
		createFile(folder2);

		testContext.getContext().publish(false);

		assertThat(new java.io.File(testContext.getPubDir(), String.format("%s/%s", getExpectedHostname(), getExpectedPath(getExpectedFilename()))))
				.as("published file").exists();
	}

	/**
	 * Test dependencies on page URLs
	 * @throws Exception
	 */
	@Test
	public void testPageDependencies() throws Exception {
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder 1");
			f.setPublishDir("a/b");
		}));

		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 2");
			f.setPublishDir("c/d");
		}));

		Page page = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder2.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("page.html");
		}), p -> {
			p.publish();
		}));

		Page renderedPage = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder2.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("rendered.html");
		}), p -> {
			p.publish();
		}));

		// render the URL to the page in the scope of another page, which will create a dependency
		Trx.supply(() -> {
			try (RenderTypeTrx rTrx = RenderTypeTrx.publish(renderedPage)) {
				StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINKWAY_ABS, RenderUrl.LINKWAY_ABS, null);
				return urlFactory.createRenderUrl(Page.class, page.getId()).toString();
			}
		});

		Trx.operate(() -> {
			if (feature) {
				assertThat(renderedPage).as("Rendered page").dependsOn(node, "pub_dir", 0).dependsOn(folder1, "pub_dir", 0).dependsOn(folder2, "pub_dir", 0)
						.dependsOn(page, "filename", 0);
			} else {
				assertThat(renderedPage).as("Rendered page").dependsOn(node, "pub_dir", 0).dependsOn(folder2, "pub_dir", 0).dependsOn(page, "filename", 0)
						.doesNotDependOn(folder1, "pub_dir", 0);
			}
		});
	}

	/**
	 * Test dependencies on file URLs
	 * @throws Exception
	 */
	@Test
	public void testFileDependencies() throws Exception {
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder 1");
			f.setPublishDir("a/b");
		}));

		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 2");
			f.setPublishDir("c/d");
		}));
		File file;
		try (InputStream in = new ByteArrayInputStream("File contents".getBytes())) {
			file = Trx.supply(() -> create(File.class, f -> {
				f.setFolderId(folder2.getId());
				f.setName("file.txt");
				f.setFileStream(in);
			}));
		}

		Page renderedPage = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder2.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("rendered.html");
		}), p -> {
			p.publish();
		}));

		// render the URL to the file in the scope of another page, which will create a dependency
		Trx.supply(() -> {
			try (RenderTypeTrx rTrx = RenderTypeTrx.publish(renderedPage)) {
				StaticUrlFactory urlFactory = new StaticUrlFactory(RenderUrl.LINKWAY_ABS, RenderUrl.LINKWAY_ABS, null);
				return urlFactory.createRenderUrl(File.class, file.getId()).toString();
			}
		});

		Trx.operate(() -> {
			if (feature) {
				assertThat(renderedPage).as("Rendered page").dependsOn(node, "pub_dir_bin", 0).dependsOn(folder1, "pub_dir", 0).dependsOn(folder2, "pub_dir", 0)
						.dependsOn(file, "name", 0);
			} else {
				assertThat(renderedPage).as("Rendered page").dependsOn(node, "pub_dir_bin", 0).dependsOn(folder2, "pub_dir", 0).dependsOn(file, "name", 0)
						.doesNotDependOn(folder1, "pub_dir", 0);
			}
		});
	}

	/**
	 * Create folder in level 1
	 * @return folder
	 * @throws NodeException
	 */
	protected Folder createLevel1() throws NodeException {
		Folder folder1 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(node.getFolder().getId());
			f.setName("Folder 1");
			f.setPublishDir("a/b");
		}));

		if (level1Location == MCSetting.channel) {
			Trx.supply(() -> update(localize(folder1, channel), f -> {
				f.setPublishDir("ac/bc");
			}));
		}

		return folder1;
	}

	/**
	 * Create folder in level 2
	 * @param folder1 mother folder
	 * @return folder
	 * @throws NodeException
	 */
	protected Folder createLevel2(Folder folder1) throws NodeException {
		Folder folder2 = Trx.supply(() -> create(Folder.class, f -> {
			f.setMotherId(folder1.getId());
			f.setName("Folder 2");
			f.setPublishDir("c/d");
		}));

		if (level2Location == MCSetting.channel) {
			Trx.supply(() -> update(localize(folder2, channel), f -> {
				f.setPublishDir("cc/dc");
			}));
		}

		return folder2;
	}

	/**
	 * Create page
	 * @param folder mother folder
	 * @return page
	 * @throws NodeException
	 */
	protected Page createPage(Folder folder) throws NodeException {
		Page page = Trx.supply(() -> update(create(Page.class, p -> {
			p.setFolderId(folder.getId());
			p.setTemplateId(template.getId());
			p.setName("Page");
			p.setFilename("page.html");
		}), p -> {
			p.publish();
		}));

		if (objectLocation == MCSetting.channel) {
			Trx.supply(() -> update(update(localize(page, channel), p -> p.setFilename("pagec.html")), p -> p.publish()));
		}

		return page;
	}

	/**
	 * Create file
	 * @param folder mother folder
	 * @return file
	 * @throws NodeException
	 * @throws IOException
	 */
	protected File createFile(Folder folder) throws NodeException, IOException {
		File file;
		try (InputStream in = new ByteArrayInputStream("File contents".getBytes())) {
			file = Trx.supply(() -> create(File.class, f -> {
				f.setFolderId(folder.getId());
				f.setName("file.txt");
				f.setFileStream(in);
			}));
		}

		if (objectLocation == MCSetting.channel) {
			Trx.supply(() -> update(localize(file, channel), f -> f.setName("filec.txt")));
		}

		return file;
	}

	/**
	 * Get the expected path
	 * @param filename filename
	 * @return path
	 */
	protected String getExpectedPath(String filename) {
		if (feature) {
			switch (objectLocation) {
			case master:
				return String.format("/a-b/c-d/%s", filename);
			case channel:
				return String.format("/%s/%s/%s", level1Location == MCSetting.master ? "a-b" : "ac-bc", level2Location == MCSetting.master ? "c-d" : "cc-dc",
						filename);
			default:
				return null;
			}
		} else {
			switch (objectLocation) {
			case master:
				return String.format("/c/d/%s", filename);
			case channel:
				return String.format("/%s/%s", level2Location == MCSetting.master ? "c/d" : "cc/dc", filename);
			default:
				return null;
			}
		}
	}

	/**
	 * Get the expected page filename
	 * @return page filename
	 */
	protected String getExpectedPageFilename() {
		return objectLocation == MCSetting.master ? "page.html" : "pagec.html";
	}

	/**
	 * Get the expected file filename
	 * @return file filename
	 */
	protected String getExpectedFilename() {
		return objectLocation == MCSetting.master ? "file.txt" : "filec.txt";
	}

	/**
	 * Get the expected hostname
	 * @return hostname
	 */
	protected String getExpectedHostname() {
		if (feature) {
			return objectLocation == MCSetting.master ? "with" : "channelwith";
		} else {
			return objectLocation == MCSetting.master ? "without" : "channelwithout";
		}
	}

	/**
	 * Multichannelling locations
	 */
	public static enum MCSetting {
		master, channel
	}
}
