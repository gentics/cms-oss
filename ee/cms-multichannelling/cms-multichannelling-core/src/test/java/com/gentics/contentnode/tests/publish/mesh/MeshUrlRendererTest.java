package com.gentics.contentnode.tests.publish.mesh;

import static com.gentics.contentnode.factory.Trx.execute;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeMeshCRUtils.createMeshCR;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateTag;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createVelocityConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
import com.gentics.contentnode.factory.RenderTypeTrx;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.object.parttype.PageURLPartType;
import com.gentics.contentnode.publish.mesh.MeshPublisher;
import com.gentics.contentnode.tests.utils.Builder;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

@GCNFeature(set = { Feature.MESH_CONTENTREPOSITORY, Feature.MULTICHANNELLING })
@RunWith(Parameterized.class)
public class MeshUrlRendererTest {

	enum Location {
		MASTER,
		CHANNEL,
		SIDECHANNEL,
		OTHER_MASTER,
		OTHER_CHANNEL;

		Page page() {
			switch (this) {
			case MASTER: return masterPage;
			case CHANNEL: return channelPage;
			case SIDECHANNEL: return sideChannelPage;
			case OTHER_MASTER: return otherMasterPage;
			case OTHER_CHANNEL: return otherChannelPage;
			}

			throw new RuntimeException("Unexpected Location: " + this);
		}

		Node node() {
			switch (this) {
			case MASTER: return master;
			case CHANNEL: return channel;
			case SIDECHANNEL: return sideChannel;
			case OTHER_MASTER: return otherMaster;
			case OTHER_CHANNEL: return otherChannel;
			}

			throw new RuntimeException("Unexpected Location: " + this);
		}
	}

	private final static String MESH_PROJECT_NAME = "testproject";
	private final static String VTL_TEMPLATE_FMT = "#gtx_channel(%d)$cms.page.tags.page#end";

	@ClassRule
	public static DBTestContext context = new DBTestContext();

	private static Node master;
	private static Page masterPage;
	private static Node channel;
	private static Page channelPage;
	private static Node sideChannel;
	private static Page sideChannelPage;
	private static Node otherMaster;
	private static Page otherMasterPage;
	private static Node otherChannel;
	private static Page otherChannelPage;
	private static Integer pageConstructId;
	private static Integer vtlConstructId;

	@Parameter(0)
	public Location source;

	@Parameter(1)
	public Location target;

	@Parameters(name = "{index} source: {0} target: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (Location source : Arrays.asList(Location.MASTER, Location.CHANNEL)) {
			for (Location target : Location.values()) {
				// Filter unnecessary combinations:
				// - source and target are the same
				// - MASTER to CHANNEL is the same as MASTER to SIDECHANNEL so the later combination can be skipped
				if (source != target && (source != Location.MASTER || target != Location.SIDECHANNEL)) {
					data.add(new Object[] { source, target });
				}
			}
		}

		return data;
	}

	@BeforeClass
	public static void setupOnce() throws NodeException {
		context.getContext().getTransaction().commit();

		Integer meshCrId = createMeshCR("", 0, MESH_PROJECT_NAME);

		master = createNode("master", meshCrId, null);
		channel = createNode("channel", meshCrId, master);
		sideChannel = createNode("sideChannel", meshCrId, master);

		otherMaster = createNode("otherMaster", meshCrId, null);
		otherChannel = createNode("otherChannel", meshCrId, otherMaster);

		pageConstructId = Trx.supply(() -> createConstruct(master, PageURLPartType.class, "pageurl", "page"));
		vtlConstructId = Trx.supply(() -> createVelocityConstruct(master, "velocity", "vtl"));

		Template template = supply(() -> createTemplate(
			master.getFolder(), "<node vtl>", "Template",
			createTemplateTag(vtlConstructId, "vtl", true, false),
			createTemplateTag(pageConstructId, "page", true, false)));

		Folder folder = supply(() -> ContentNodeTestDataUtils.createFolder(master.getFolder(), "folder"));
		masterPage = execute(f -> update(createPage(f, template, "master page", null), Page::publish), folder);
		channelPage = execute(f -> update(createPage(f, template, "channel page", channel), Page::publish), folder);
		sideChannelPage = execute(f -> update(createPage(f, template, "sideChannel page", sideChannel), Page::publish), folder);

		folder = supply(() -> ContentNodeTestDataUtils.createFolder(otherMaster.getFolder(), "folder"));
		otherMasterPage = execute(f -> update(createPage(f, template, "otherMaster page", otherMaster), Page::publish), folder);
		otherChannelPage = execute(f -> update(createPage(f, template, "otherChannel page", otherChannel), Page::publish), folder);
	}

	private static Node createNode(String name, Integer meshCrId, Node master) throws NodeException {
		Node node = Trx.supply(() -> master == null
			? ContentNodeTestDataUtils.createNode(name, name, PublishTarget.NONE)
			: ContentNodeTestDataUtils.createChannel(master, name, name, "/"));

		update(
			node,
			n -> {
				n.setContentrepositoryId(meshCrId);
				n.setPublishContentmap(true);
			});

		return  node;
	}

	@Test
	public void testCrossChannelLink() throws NodeException {
		Page sourcePage = Builder.update(source.page(), p -> {
			getPartType(PageURLPartType.class, p.getContentTag("page"), "page").setTargetPage(target.page());
			getPartType(LongHTMLPartType.class, p.getContentTag("vtl"), "template").getValueObject()
					.setValueText(String.format(VTL_TEMPLATE_FMT, target.node().getId()));
		}).publish().unlock().build();

		Trx.operate(() -> {
			try (ChannelTrx cTrx = new ChannelTrx(source.node());
					RenderTypeTrx rtTrx = RenderTypeTrx.publish()) {
				String targetPageUuid = MeshPublisher.getMeshUuid(target.page());
				String expected = String.format(
					"{{mesh.link(%s, en, %s)}}",
					targetPageUuid,
					target.node().getFolder().getName());

				assertThat(sourcePage.render())
					.as(sourcePage + " rendered")
					.isEqualTo(expected);
			}
		});
	}
}
