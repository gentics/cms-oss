package com.gentics.contentnode.tests.publish.disinherit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.portalnode.connector.PortalConnectorFactory;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.PublishTarget;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.testutils.TestFileProvider;

@RunWith(value = Parameterized.class)
public class DisinheritPublishTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	private static Set<Node> emptyNodeSet = new HashSet<>();

	private final static int creation = 1;
	private final static int firstPublish = creation + 1;
	private final static int firstCheck = firstPublish + 1;
	private final static int exclusion = firstCheck + 1;
	private final static int secondPublish = exclusion + 1;
	private final static int secondCheck = secondPublish + 1;
	private final static int inclusion = secondCheck + 1;
	private final static int thirdPublish = inclusion + 1;
	private final static int thirdCheck = thirdPublish + 1;


	/**
	 * Variation settings
	 */
	private Type type;
	private Status status;
	private Set<Channel> disinherit;
	private boolean mccr;

	private boolean instant;

	private Node master;

	private Node channel;

	private Node subchannel;

	private Node sidechannel;

	private Folder folder;

	private Template template;

	private Page page;

	private File file;

	private File image;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: type {0}, status {1}, disinherit {2}, mccr {3}, instant {4}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Type type : Type.values()) {
			for (boolean mccr : Arrays.asList(true, false)) {
				for (boolean instant : Arrays.asList(true, false)) {
					data.add(new Object[] { type, Status.exclude, Collections.emptyList(), mccr, instant });
				}
			}
		}
		for (Type type : Type.values()) {
			Collection<List<Channel>> disinheritOptions = Arrays.asList(
				Arrays.asList(Channel.subchannel),
				Arrays.asList(Channel.channel),
				Arrays.asList(Channel.subchannel, Channel.sidechannel),
				Arrays.asList(Channel.channel, Channel.sidechannel)
			);
			for (List<Channel> disinherit : disinheritOptions) {
				for (boolean mccr : Arrays.asList(true, false)) {
					for (boolean instant : Arrays.asList(true, false)) {
						data.add(new Object[] { type, Status.disinherit, disinherit, mccr, instant });
					}
				}
			}
		}
		return data;
	}


	/**
	 * Create a test instance
	 * @param type object type
	 * @param status exclusion or disinheriting
	 * @param disinherit disinherited channels
	 * @param mccr mccr
	 * @param instant instant publishing
	 */
	public DisinheritPublishTest(Type type, Status status, Collection<Channel> disinherit, boolean mccr, boolean instant) {
		this.type = type;
		this.status = status;
		this.disinherit = new HashSet<Channel>(disinherit);
		this.mccr = mccr;
		this.instant = instant;
	}

	/**
	 * Setup test data
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature(Feature.MULTICHANNELLING.toString().toLowerCase(), true);
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("mccr", true);
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("instant_cr_publishing", true);
		testContext.startTransaction(creation);
		Transaction t = TransactionManager.getCurrentTransaction();

		master = ContentNodeTestDataUtils.createNode("master", "Master", PublishTarget.CONTENTREPOSITORY, mccr, instant);
		channel = ContentNodeTestDataUtils.createChannel(master, "Channel", "channel", "/", PublishTarget.CONTENTREPOSITORY);
		subchannel = ContentNodeTestDataUtils.createChannel(channel, "Subchannel", "subchannel", "/", PublishTarget.CONTENTREPOSITORY);
		sidechannel = ContentNodeTestDataUtils.createChannel(master, "Sidechannel", "sidechannel", "/", PublishTarget.CONTENTREPOSITORY);

		folder = ContentNodeTestDataUtils.createFolder(master.getFolder(), "Folder");
		template = ContentNodeTestDataUtils.createTemplate(folder, "Dummy Template", "Template");
		page = ContentNodeTestDataUtils.createPage(folder, template, "Page");
		t.getObject(Page.class, page.getId(), true).publish();
		t.commit(false);

		file = ContentNodeTestDataUtils.createFile(folder, "testfile.bin", "Testfile Content".getBytes());
		image = ContentNodeTestDataUtils.createFile(folder, "image.png", TestFileProvider.getTestPNG(), null);

		testContext.publish(firstPublish);

		testContext.startTransaction(firstCheck);

		for (Node node : Arrays.asList(master, channel, subchannel, sidechannel)) {
			for (NodeObject object : Arrays.asList(folder, page, file, image)) {
				ContentNodeTestUtils.assertPublishCR(object, node, true);
			}
		}
	}

	/**
	 * Destroy datasources
	 */
	@After
	public void tearDown() {
		PortalConnectorFactory.destroy();
	}

	/**
	 * Test publishing with mc exclusions and disinheriting
	 * @throws Exception
	 */
	@Test
	public void testPublishExclusion() throws Exception {
		testContext.startTransaction(exclusion);
		Transaction t = TransactionManager.getCurrentTransaction();
		// exclude/disinherit
		switch (type) {
		case folder:
			folder.changeMultichannellingRestrictions(status == Status.exclude, status == Status.disinherit ? getNodeSet(disinherit) : emptyNodeSet, false);
			break;
		case page:
			page.changeMultichannellingRestrictions(status == Status.exclude, status == Status.disinherit ? getNodeSet(disinherit) : emptyNodeSet, false);
			break;
		case file:
			file.changeMultichannellingRestrictions(status == Status.exclude, status == Status.disinherit ? getNodeSet(disinherit) : emptyNodeSet, false);
			break;
		case image:
			image.changeMultichannellingRestrictions(status == Status.exclude, status == Status.disinherit ? getNodeSet(disinherit) : emptyNodeSet, false);
			break;
		}
		t.commit(false);
		reReadObjects();

		// run publish process again
		testContext.publish(secondPublish);

		testContext.startTransaction(secondCheck);
		// assert publish result
		for (Node node : Arrays.asList(master, channel, subchannel, sidechannel)) {
			for (NodeObject object : Arrays.asList(folder, page, file, image)) {
				ChannelTreeSegment visibility = new ChannelTreeSegment((Disinheritable<?>) object, true);
				ContentNodeTestUtils.assertPublishCR(object, node, visibility.getAllNodes().contains(node));
			}
		}

		// include/inherit
		testContext.startTransaction(inclusion);
		t = TransactionManager.getCurrentTransaction();
		folder.changeMultichannellingRestrictions(false, emptyNodeSet, false);
		page.changeMultichannellingRestrictions(false, emptyNodeSet, false);
		file.changeMultichannellingRestrictions(false, emptyNodeSet, false);
		image.changeMultichannellingRestrictions(false, emptyNodeSet, false);
		t.commit(false);
		reReadObjects();

		// run publish process again
		testContext.publish(thirdPublish);

		testContext.startTransaction(thirdCheck);
		// everything must be published everywhere
		for (Node node : Arrays.asList(master, channel, subchannel, sidechannel)) {
			for (NodeObject object : Arrays.asList(folder, page, file, image)) {
				ContentNodeTestUtils.assertPublishCR(object, node, true);
			}
		}
	}

	protected void reReadObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		folder = t.getObject(Folder.class, folder.getId());
		page = t.getObject(Page.class, page.getId());
		file = t.getObject(File.class, file.getId());
		image = t.getObject(File.class, image.getId());
	}

	protected Set<Node> getNodeSet(Collection<Channel> channels) {
		Set<Node> nodeSet = new HashSet<>();
		for (Channel c : channels) {
			switch(c) {
			case master:
				nodeSet.add(master);
				break;
			case channel:
				nodeSet.add(channel);
				break;
			case subchannel:
				nodeSet.add(subchannel);
				break;
			case sidechannel:
				nodeSet.add(sidechannel);
				break;
			}
		}
		return nodeSet;
	}

	protected static enum Type {
		folder, page, image, file
	}

	protected static enum Status {
		exclude, disinherit
	}

	protected static enum Channel {
		master, channel, subchannel, sidechannel
	}
}
