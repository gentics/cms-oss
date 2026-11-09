package com.gentics.contentnode.tests.rest;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.parttype.FileURLPartType;
import com.gentics.contentnode.object.parttype.LongHTMLPartType;
import com.gentics.contentnode.rest.model.response.FileListResponse;
import com.gentics.contentnode.rest.model.response.LegacyFileListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.parameter.EditableParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FileListParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.InFolderParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyFilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacyPagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.LegacySortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.contentnode.rest.resource.parameter.WastebinParameterBean;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFile;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.getPartType;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;

/**
 * Test cases for filtering files by their usage
 */
@GCNFeature(set = Feature.MULTICHANNELLING)
public class FileUsageTest {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static Node channel;
	private static Integer constructId;
	private static Template template;
	private static File unusedInMaster;
	private static File unusedInChannel;
	private static File usedInMasterAndChannel;
	private static File usedInMaster;
	private static File usedInChannel;
	private static File channelFile;
	private static Integer fileConstructId;

	/**
	 * Setup the test data
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setProperty(
				"contentnode.maxfilesize", Integer.toString(5 * 1024 * 1024));
		node = Trx.supply(() -> createNode());
		channel = Trx.supply(() -> createChannel(node, "Channel", "channel", "/"));

		constructId = Trx.supply(() -> createConstruct(node, LongHTMLPartType.class, "content", "content"));
		fileConstructId = Trx.supply(() -> createConstruct(node, FileURLPartType.class, FileURLPartType.class.getSimpleName().toLowerCase(), "part"));

		template = Trx.supply(() -> {
			return create(Template.class, t -> {
				t.getFolders().add(node.getFolder());
				t.setName("Template");
				t.setSource("<node page.name>: [<node content>]");

				TemplateTag tag = create(TemplateTag.class, tTag -> {
					tTag.setConstructId(constructId);
					tTag.setEnabled(true);
					tTag.setName("content");
					tTag.setPublic(true);
				}, false);

				t.getTemplateTags().put("content", tag);
			});
		});

		unusedInMaster = Trx.supply(() -> createFile(node.getFolder(), "unused.txt", "Content".getBytes()));
		unusedInChannel = Trx.supply(() -> createFile(node.getFolder(), "unused_channel.txt", "Content".getBytes(), channel));

		usedInMasterAndChannel = Trx.supply(() -> createFile(node.getFolder(), "master_and_channel.txt", "Content".getBytes()));
		createPage("Master and Channel", usedInMasterAndChannel, null);

		usedInMaster = Trx.supply(() -> createFile(node.getFolder(), "master.txt", "Content".getBytes()));
		Page masterOnly = createPage("Master Only", usedInMaster, null);
		Trx.operate(() -> update(masterOnly, p -> {
			p.changeMultichannellingRestrictions(true, Collections.emptySet(), false);
		}));

		usedInChannel = Trx.supply(() -> createFile(node.getFolder(), "channel.txt", "Content".getBytes()));
		createPage("Channel only", usedInChannel, channel);

		channelFile = Trx.supply(() -> createFile(node.getFolder(), "channelfile.txt", "Content".getBytes(), channel));
		createPage("Master and Channel 2", channelFile, null);

		testContext.publish(false);
	}

	/**
	 * Test getting files used in master
	 * @throws NodeException
	 */
	@Test
	public void testUsedInMaster() throws NodeException {
		doTest(true, node, null, usedInMaster, usedInMasterAndChannel);
	}

	/**
	 * Test getting files not used in master
	 * @throws NodeException
	 */
	@Test
	public void testNotUsedInMaster() throws NodeException {
		doTest(false, node, null, unusedInMaster, usedInChannel);
	}

	/**
	 * Test getting files in master used in channel
	 * @throws NodeException
	 */
	@Test
	public void testMasterUsedInChannel() throws NodeException {
		doTest(true, node, Arrays.asList(channel.getId()), usedInMasterAndChannel, usedInChannel);
	}

	/**
	 * Test getting files in master not used in channel
	 * @throws NodeException
	 */
	@Test
	public void testMasterNotUsedInChannel() throws NodeException {
		doTest(false, node, Arrays.asList(channel.getId()), unusedInMaster, usedInMaster);
	}

	/**
	 * Test getting files in channel used in channel
	 * @throws NodeException
	 */
	@Test
	public void testChannelUsedInChannel() throws NodeException {
		doTest(true, channel, Arrays.asList(channel.getId()), usedInMasterAndChannel, usedInChannel, channelFile);
	}

	/**
	 * Test getting files in channel not used in channel
	 * @throws NodeException
	 */
	@Test
	public void testChannelNotUsedInChannel() throws NodeException {
		doTest(false, channel, Arrays.asList(channel.getId()), unusedInMaster, unusedInChannel, usedInMaster);
	}

	/**
	 * Test getting files from master used in master or channel
	 * @throws NodeException
	 */
	@Test
	public void testUsedInMasterOrChannel() throws NodeException {
		doTest(true, node, Arrays.asList(node.getId(), channel.getId()), usedInMaster, usedInChannel, usedInMasterAndChannel);
	}

	/**
	 * Test getting files from master not used in master or channel
	 * @throws NodeException
	 */
	@Test
	public void testNotUsedInMasterOrChannel() throws NodeException {
		doTest(false, node, Arrays.asList(node.getId(), channel.getId()), unusedInMaster);
	}

	/**
	 * Create a page with given name, that uses the given file and publish the page.
	 * @param name page name
	 * @param file used file
	 * @param channel channel of the page
	 * @return page
	 * @throws NodeException
	 */
	protected static Page createPage(String name, File file, Node channel) throws NodeException {
		return Trx.supply(() -> {
			return update(create(Page.class, p -> {
				p.setTemplateId(template.getId());
				p.setFolderId(node.getFolder().getId());
				p.setName(name);
				if (channel != null) {
					p.setChannelInfo(channel.getId(), null);
				}

				ContentTag tag = p.getContent().addContentTag(fileConstructId);
				tag.setName("testtag");
				tag.setEnabled(true);
				getPartType(FileURLPartType.class, tag, "part").setTargetFile(file);
				p.getContentTag("content").getValues().getByKeyname("content").setValueText("<node testtag>");
			}), p -> p.publish());
		});
	}

	/**
	 * Do the test
	 * @param used flag for used/unused
	 * @param node for which to test
	 * @param channelIds list of channel IDs
	 * @param expected files expected in the response
	 * @throws NodeException
	 */
	protected static void doTest(boolean used, Node node, List<Integer> channelIds, File... expected) throws NodeException {
		String[] expectedNames = new String[expected.length];

		for (int i = 0; i < expected.length; i++) {
			expectedNames[i] = expected[i].getName();
		}

		Trx.operate(() -> {
			String folderId = node.getFolder().getId().toString();
			Integer nodeId = node.getId();
			FileListResponse response = ContentNodeRESTUtils.getFileResource().list(
				new InFolderParameterBean().setFolderId(folderId),
				new FileListParameterBean().setNodeId(nodeId).setUsed(used).setUsedIn(channelIds),
				new FilterParameterBean(),
				new SortParameterBean(),
				new PagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean());

			assertThat(response).as("File list response").hasCode(ResponseCode.OK);
			assertThat(response.getItems().stream().map(f -> f.getName()).collect(Collectors.toList())).as("File list").containsOnly(expectedNames);

			// Check again with legacy endpoint.
			LegacyFileListResponse legacyResponse = ContentNodeRESTUtils.getFolderResource().getFiles(
				folderId,
				new InFolderParameterBean().setFolderId(folderId),
				new FileListParameterBean().setNodeId(nodeId).setUsed(used).setUsedIn(channelIds),
				new LegacyFilterParameterBean(),
				new LegacySortParameterBean(),
				new LegacyPagingParameterBean(),
				new EditableParameterBean(),
				new WastebinParameterBean());

			assertThat(legacyResponse).as("File list response (legacy)").hasCode(ResponseCode.OK);
			assertThat(legacyResponse.getFiles().stream().map(f -> f.getName()).collect(Collectors.toList()))
				.as("File list (legacy)")
				.containsOnly(expectedNames);
		});
	}
}
