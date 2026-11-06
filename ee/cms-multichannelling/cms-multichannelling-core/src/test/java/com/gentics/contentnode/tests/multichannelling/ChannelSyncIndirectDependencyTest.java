package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createImage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateTag;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.fillOverview;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartTypeId;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
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
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for dirting of overviews when objects are sync'ed between channels
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MULTICHANNELLING })
public class ChannelSyncIndirectDependencyTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext(true);

	private static Map<Channel, Node> channelMap = new HashMap<>();

	private static Construct overviewConstruct;
	private static Template overviewTemplate;

	@Parameters(name = "{index}: from {0} to {1} overwrite {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Channel from : Arrays.asList(Channel.channel, Channel.subchannel, Channel.subsubchannel)) {
			for (Channel to : Arrays.asList(Channel.node, Channel.channel, Channel.subchannel)) {
				if (!to.parentOf(from)) {
					continue;
				}
				for (boolean overwrite : Arrays.asList(true, false)) {
					data.add(new Object[] { from, to, overwrite });
				}
			}
		}
		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			for (Node n : t.getObjects(Node.class, DBUtils.select("SELECT id FROM node", DBUtils.IDS))) {
				n.delete();
			}
		});

		Trx.operate(() -> {
			DBUtils.executeUpdate("DELETE FROM dirtqueue", null);
			DBUtils.executeUpdate("DELETE FROM publishqueue", null);
		});

		channelMap.put(Channel.node, Trx.supply(() -> createNode("master", "Master", PublishTarget.FILESYSTEM)));
		channelMap.put(Channel.channel, Trx.supply(() -> createChannel(channelMap.get(Channel.node), "Channel", "channel", "/", PublishTarget.FILESYSTEM)));
		channelMap.put(Channel.subchannel, Trx.supply(() -> createChannel(channelMap.get(Channel.channel), "Subchannel", "subchannel", "/", PublishTarget.FILESYSTEM)));
		channelMap.put(Channel.subsubchannel, Trx.supply(() -> createChannel(channelMap.get(Channel.subchannel), "Subsubchannel", "subsubchannel", "/", PublishTarget.FILESYSTEM)));
		channelMap.put(Channel.sidechannel, Trx.supply(() -> createChannel(channelMap.get(Channel.node), "Sidechannel", "sidechannel", "/", PublishTarget.FILESYSTEM)));

		overviewConstruct = Trx.supply(() -> create(Construct.class, c -> {
			c.setAutoEnable(true);
			c.setKeyword("overview");
			c.setName("overview", 1);
			c.getNodes().add(channelMap.get(Channel.node));

			c.getParts().add(
			create(Part.class, p -> {
				p.setEditable(1);
				p.setHidden(false);
				p.setKeyname("ds");
				p.setName("ds", 1);
				p.setPartTypeId(getPartTypeId(OverviewPartType.class));
				p.setDefaultValue(create(Value.class, v -> {
					v.setInfo(1);
				}, false));
			}, false));
		}));

		overviewTemplate = Trx.supply(() -> createTemplate(channelMap.get(Channel.node).getFolder(), "<node overview>", "Overview Template",
				createTemplateTag(overviewConstruct.getId(), "overview", true, false)));
	}

	@Parameter(0)
	public Channel from;

	@Parameter(1)
	public Channel to;

	@Parameter(2)
	public boolean overwrite;

	@Before
	public void setup() throws NodeException {
		Trx.operate(() -> {
			for (Channel channel : Channel.values()) {
				Node n = channelMap.get(channel);
				try (ChannelTrx cTrx = new ChannelTrx(n)) {
					for (Folder folder : n.getFolder().getChildFolders()) {
						folder.delete(true);
					}
				}
			}
		});
	}

	@Test
	public void testPushPageToMaster() throws Exception {
		Folder folder1 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 1"));
		Folder folder2 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 2"));

		Page dependency = null;
		if (overwrite) {
			dependency = Trx.supply(() -> update(createPage(folder2, overviewTemplate, "Dependency", channelMap.get(to)), Page::publish));
			Trx.consume(dep -> update(update(localize(dep, channelMap.get(from)), p -> {
				p.setName("Local Dependency");
			}), Page::publish), dependency);
			dependency = Trx.execute(Page::reload, dependency);
		} else {
			dependency = Trx.supply(() -> update(createPage(folder2, overviewTemplate, "Dependency", channelMap.get(from)), Page::publish));
		}
		Page overview = Trx.supply(() -> update(createPage(folder1, overviewTemplate, "Overview Page"), p -> {
			fillOverview(p.getContentTag("overview"), "ds", "<node page.name><br>", Page.class, Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME,
					Overview.ORDERWAY_ASC, false, Arrays.asList(folder2));
			p.publish();
		}));

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// push dependency to master
		Trx.consume(d -> {
			MultichannellingFactory.getChannelVariant(d, channelMap.get(from)).pushToMaster(channelMap.get(to));
		}, dependency);

		// wait for dependency calculation
		testContext.waitForDirtqueueWorker();

		try (Trx trx = new Trx()) {
			for (Channel channel : Channel.values()) {
				Node node = channelMap.get(channel);
				// note: when overwrite is true, the local object is deleted, and this will trigger DELETE for the object in ALL channels,
				// therefore the overview will be dirted in all channels
				if (to == channel || to.parentOf(channel)) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, overview, dependency);
				} else if (overwrite) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, overview);
				} else {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node);
				}
			}
			trx.success();
		}
	}

	@Test
	public void testPushFileToMaster() throws Exception {
		Folder folder1 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 1"));
		Folder folder2 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 2"));

		File dependency = null;
		if (overwrite) {
			dependency = Trx.supply(() -> createFile(folder2, "dependency.txt", "Bla".getBytes(), channelMap.get(to)));
			Trx.consume(dep -> update(localize(dep, channelMap.get(from)), f -> f.setName("local_dependency.txt")), dependency);
			dependency = Trx.execute(File::reload, dependency);
		} else {
			dependency = Trx.supply(() -> createFile(folder2, "dependency.txt", "Bla".getBytes(), channelMap.get(from)));
		}
		Page overview = Trx.supply(() -> update(createPage(folder1, overviewTemplate, "Overview Page"), p -> {
			fillOverview(p.getContentTag("overview"), "ds", "<node file.name><br>", File.class, Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME,
					Overview.ORDERWAY_ASC, false, Arrays.asList(folder2));
			p.publish();
		}));

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// push dependency to master
		Trx.consume(d -> {
			MultichannellingFactory.getChannelVariant(d, channelMap.get(from)).pushToMaster(channelMap.get(to));
		}, dependency);

		// wait for dependency calculation
		testContext.waitForDirtqueueWorker();

		try (Trx trx = new Trx()) {
			for (Channel channel : Channel.values()) {
				Node node = channelMap.get(channel);
				// note: when overwrite is true, the local object is deleted, and this will trigger DELETE for the object in ALL channels,
				// therefore the overview will be dirted in all channels
				if (to == channel || to.parentOf(channel) || overwrite) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, overview);
				} else {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node);
				}
			}
			trx.success();
		}
	}

	@Test
	public void testPushImageToMaster() throws Exception {
		Folder folder1 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 1"));
		Folder folder2 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 2"));
		byte[] image = IOUtils.toByteArray(GenericTestUtils.getPictureResource("blume.jpg"));

		File dependency = null;
		if (overwrite) {
			dependency = Trx.supply(() -> createImage(folder2, "dependency.png", image, channelMap.get(to)));
			Trx.consume(dep -> update(localize(dep, channelMap.get(from)), f -> f.setName("local_dependency.png")), dependency);
			dependency = Trx.execute(File::reload, dependency);
		} else {
			dependency = Trx.supply(() -> createImage(folder2, "dependency.png", image, channelMap.get(from)));
		}
		Page overview = Trx.supply(() -> update(createPage(folder1, overviewTemplate, "Overview Page"), p -> {
			fillOverview(p.getContentTag("overview"), "ds", "<node image.name><br>", ImageFile.class, Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME,
					Overview.ORDERWAY_ASC, false, Arrays.asList(folder2));
			p.publish();
		}));

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// push dependency to master
		Trx.consume(d -> {
			MultichannellingFactory.getChannelVariant(d, channelMap.get(from)).pushToMaster(channelMap.get(to));
		}, dependency);

		// wait for dependency calculation
		testContext.waitForDirtqueueWorker();

		try (Trx trx = new Trx()) {
			for (Channel channel : Channel.values()) {
				Node node = channelMap.get(channel);
				// note: when overwrite is true, the local object is deleted, and this will trigger DELETE for the object in ALL channels,
				// therefore the overview will be dirted in all channels
				if (to == channel || to.parentOf(channel) || overwrite) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, overview);
				} else {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node);
				}
			}
			trx.success();
		}
	}

	@Test
	public void testPushFolderToMaster() throws Exception {
		Folder folder1 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 1"));
		Folder folder2 = Trx.supply(() -> createFolder(channelMap.get(Channel.node).getFolder(), "Folder 2"));

		Folder dependency = null;
		if (overwrite) {
			dependency = Trx.supply(() -> createFolder(folder2, "Dependency", channelMap.get(to)));
			Trx.consume(dep -> update(localize(dep, channelMap.get(from)), f -> f.setName("Local Dependency")), dependency);
			dependency = Trx.execute(Folder::reload, dependency);
		} else {
			dependency = Trx.supply(() -> createFolder(folder2, "Dependency", channelMap.get(from)));
		}
		Page overview = Trx.supply(() -> update(createPage(folder1, overviewTemplate, "Overview Page"), p -> {
			fillOverview(p.getContentTag("overview"), "ds", "<node folder.name><br>", Folder.class, Overview.SELECTIONTYPE_FOLDER, 0, Overview.ORDER_NAME,
					Overview.ORDERWAY_ASC, false, Arrays.asList(folder2));
			p.publish();
		}));

		// run publish process
		try (Trx trx = new Trx()) {
			testContext.publish(false);
			trx.success();
		}

		// push dependency to master
		Trx.consume(d -> {
			MultichannellingFactory.getChannelVariant(d, channelMap.get(from)).pushToMaster(channelMap.get(to));
		}, dependency);

		// wait for dependency calculation
		testContext.waitForDirtqueueWorker();

		try (Trx trx = new Trx()) {
			for (Channel channel : Channel.values()) {
				Node node = channelMap.get(channel);
				// note: when overwrite is true, the local object is deleted, and this will trigger DELETE for the object in ALL channels,
				// therefore the overview will be dirted in all channels
				if (to == channel || to.parentOf(channel) || overwrite) {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node, overview);
				} else {
					ContentNodeTestUtils.assertDirtedObjects(Page.class, node);
				}
			}
			trx.success();
		}
	}

	/**
	 * Enum of all channels used in the test
	 */
	protected static enum Channel {
		node, sidechannel(node), channel(node), subchannel(channel), subsubchannel(subchannel);

		/**
		 * Parent channel (null for the master)
		 */
		protected Channel parent;

		/**
		 * Create instance without parent
		 */
		Channel() {
			this(null);
		}

		/**
		 * Create instance with parent
		 * @param parent parent
		 */
		Channel(Channel parent) {
			this.parent = parent;
		}

		/**
		 * Check whether this channel is a (sub)channel of the other
		 * @param other other channel
		 * @return true if this is a subchannel, false if not
		 */
		public boolean channelOf(Channel other) {
			if (other == null || parent == null) {
				return false;
			} if (parent == other) {
				return true;
			} else {
				return parent.channelOf(other);
			}
		}

		/**
		 * Check whether this channel is a parent of the other
		 * @param other other channel
		 * @return true if this is a parent, false if not
		 */
		public boolean parentOf(Channel other) {
			if (other == null) {
				return false;
			} else {
				return other.channelOf(this);
			}
		}
	}
}
