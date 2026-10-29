package com.gentics.contentnode.tests.rest;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.create;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.localize;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.update;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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
import com.gentics.contentnode.etc.Supplier;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for searching pages by live URL
 */
@GCNFeature(set = { Feature.MULTICHANNELLING })
@RunWith(value = Parameterized.class)
public class PageSearchLiveUrlTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node master;
	private static Node channel;
	private static Template template;

	/**
	 * Test cases, keys contain the searched URL and the optional nodeId
	 * restriction, values contain: expected hit, expected fileName of hit, expected
	 * "inherited" flag of hit
	 */
	private static Map<Pair<String, NodeId>, Triple<Boolean, String, Boolean>> searchTests = new HashMap<>();

	@Parameters(name = "{index}: liveUrl {0}, nodeId {1}")
	public static Collection<Object[]> data() {
		searchTests.put(Pair.of("bla", NodeId.MASTER), Triple.of(false, null, null));
		searchTests.put(Pair.of("bla", NodeId.CHANNEL), Triple.of(false, null, null));
		searchTests.put(Pair.of("bla", NodeId.NONE), Triple.of(false, null, null));

		for (boolean masterHost : Arrays.asList(true, false)) {
			String host = masterHost ? "www.master.com" : "www.channel.com";
			for (Inheritance folderInheritance : Inheritance.values()) {
				for (Inheritance pageInheritance : Inheritance.values()) {
					String fileName = String.format("%s.html", pageInheritance.name().toLowerCase());
					String url = String.format("%s/pages/%s/%s", host, folderInheritance.name().toLowerCase(),
							fileName);
					for (NodeId nodeIdSetting : NodeId.values()) {
						boolean expected = false;
						boolean inherited = false;

						if (masterHost) {
							// searching with master hostname will find page, if folder and page exist in master
							// and we are not restricting node to channel
							if (folderInheritance.master && pageInheritance.master && nodeIdSetting != NodeId.CHANNEL) {
								expected = true;
								// searching in master will never find inherited pages
								inherited = false;
							}
						} else {
							if (folderInheritance == Inheritance.LOCAL && pageInheritance != Inheritance.LOCAL) {
								expected = false;
							} else if (folderInheritance.channel && pageInheritance.channel && nodeIdSetting != NodeId.MASTER) {
								// searching with channel hostname will find page, if folder and page exist in channel
								// and we are not restricting node to master

								expected = true;
								// searching in channel can find inherited pages
								inherited = pageInheritance.master;
							}
						}

						searchTests.put(Pair.of(url, nodeIdSetting), Triple.of(expected, fileName, inherited));
					}
				}
			}
		}

		return searchTests.keySet().stream().map(p -> new Object[] { p.getLeft(), p.getRight() })
				.collect(Collectors.toList());
	}

	@BeforeClass
	public static void setupOnce() throws Exception {
		testContext.getContext().getTransaction().commit();

		master = supply(() -> createNode("Master", "www.master.com", "/pages", "/binaries", false, false));
		channel = supply(() -> createChannel(master, "Channel", "www.channel.com", "/pages"));

		template = supply(() -> createTemplate(master.getFolder(), "Template"));

		Folder inheritedFolder = supply(() -> create(Folder.class, f -> {
			f.setMotherId(master.getFolder().getId());
			f.setName("Inherited Folder");
			f.setPublishDir("/inherited");
		}));

		createPage(master, inheritedFolder, "inherited.html");
		Page masterPageInInheritedFolder = createPage(master, inheritedFolder, "master.html");
		supply(() -> update(localize(masterPageInInheritedFolder, channel), upd -> {
			upd.setName("localized.html");
			upd.setFilename("localized.html");
			upd.publish();
		}));
		createPage(channel, inheritedFolder, "local.html");

		Folder masterFolder = supply(() -> create(Folder.class, f -> {
			f.setMotherId(master.getFolder().getId());
			f.setName("Master Folder");
			f.setPublishDir("/master");
		}));
		createPage(master, masterFolder, "inherited.html");
		Page masterPageInMasterFolder = createPage(master, masterFolder, "master.html");
		supply(() -> update(localize(masterPageInMasterFolder, channel), upd -> {
			upd.setName("localized.html");
			upd.setFilename("localized.html");
			upd.publish();
		}));
		createPage(channel, masterFolder, "local.html");

		supply(() -> update(localize(masterFolder, channel), upd -> {
			upd.setName("Localized Folder");
			upd.setPublishDir("/localized");
		}));

		Folder localFolder = supply(() -> create(Folder.class, f -> {
			f.setMotherId(master.getFolder().getId());
			f.setName("Local Folder");
			f.setPublishDir("/local");
			f.setChannelInfo(channel.getId(), null);
		}));
		createPage(channel, localFolder, "local.html");

		// run the publish process
		try (Trx trx = new Trx()) {
			testContext.getContext().publish(false, true);
			trx.success();
		}
	}

	/**
	 * Create and publish a page
	 * @param n node
	 * @param f folder
	 * @param filename filename (will also be used as name)
	 * @return created page
	 * @throws NodeException
	 */
	protected static Page createPage(Node n, Folder f, String filename) throws NodeException {
		return supply(() -> update(create(Page.class, p -> {
			p.setFolderId(f.getId());
			p.setTemplateId(template.getId());
			if (n.isChannel()) {
				p.setChannelInfo(n.getId(), null);
			}
			p.setName(filename);
			p.setFilename(filename);
		}), upd -> {
			upd.publish();
		}));
	}

	/**
	 * Searched liveUrl
	 */
	@Parameter(0)
	public String liveUrl;

	/**
	 * Optional nodeId restriction
	 */
	@Parameter(1)
	public NodeId searchNodeId;

	/**
	 * True, if hit is expected
	 */
	protected boolean expectHit;

	/**
	 * Expected fileName of hit
	 */
	protected String expectedPageFileName;

	/**
	 * Expected "inherited" flag of hit
	 */
	protected boolean expectedInherited;

	@Before
	public void setup() throws NodeException {
		Triple<Boolean, String, Boolean> expectedResult = searchTests.getOrDefault(Pair.of(liveUrl, searchNodeId),
				Triple.of(false, null, false));
		expectHit = expectedResult.getLeft();
		if (expectHit) {
			expectedPageFileName = expectedResult.getMiddle();
			expectedInherited = expectedResult.getRight();
		}
	}

	/**
	 * Test searching with live URL
	 * @throws NodeException
	 */
	@Test
	public void testSearch() throws NodeException {
		Integer nodeId = null;
		if (searchNodeId == NodeId.MASTER) {
			nodeId = master.getId();
		} else if (searchNodeId == NodeId.CHANNEL) {
			nodeId = channel.getId();
		}
		final Integer fNodeId = nodeId;
		Supplier<PageLoadResponse> method = () -> getPageResource().search(false, false, true, false, false, false, false, false,
				false, fNodeId, liveUrl);

		if (expectHit) {
			PageLoadResponse response = assertSuccess(method, null);
			assertThat(response.getPage()).as("Found Page")
					.hasFieldOrPropertyWithValue("fileName", expectedPageFileName)
					.hasFieldOrPropertyWithValue("inherited", expectedInherited);
		} else {
			assertError(method, EntityNotFoundException.class, ResponseCode.NOTFOUND, "");
		}
	}

	/**
	 * Possible values for nodeId restriction
	 */
	public static enum NodeId {
		/**
		 * Search for master nodeId
		 */
		MASTER,

		/**
		 * Search for channel nodeId
		 */
		CHANNEL,

		/**
		 * No nodeId restriction
		 */
		NONE
	}

	/**
	 * Possible inheritance settings
	 */
	public static enum Inheritance {
		/**
		 * Object is inherited in the channel
		 */
		INHERITED(true, true),

		/**
		 * Object is master of a localized copy
		 */
		MASTER(true, false),

		/**
		 * Object is localized copy
		 */
		LOCALIZED(false, true),

		/**
		 * Object is local master
		 */
		LOCAL(false, true);

		/**
		 * True, iff the object exists in the master
		 */
		public boolean master;

		/**
		 * True, iff the object exists in the channel
		 */
		public boolean channel;

		/**
		 * Create instance
		 * @param master true iff object exists in master
		 * @param channel true iff object exists in channel
		 */
		Inheritance(boolean master, boolean channel) {
			this.master = master;
			this.channel = channel;
		}
	}
}
