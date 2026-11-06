package com.gentics.contentnode.tests.nodeobject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTreeSegment;
import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentLanguage;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestUtils;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;
import com.gentics.contentnode.testutils.Creator;

/**
 * Test cases for moving files and pages
 */
@RunWith(value = Parameterized.class)
public class AbstractMCPageMoveTest extends AbstractMCFilePageMoveTest {
	protected Template template;
	protected Page movedGermanPage;
	protected Page localizedGermanPage;
	protected ContentLanguage german;
	protected ContentLanguage english;
	protected Page movedEnglishPage;
	protected Page localizedEnglishPage;



	public AbstractMCPageMoveTest(Channel source, Channel target, boolean differentFolder, Channel localized, boolean targetExcluded,
			List<Channel> targetDisinherited) {
		super(source, target, differentFolder, localized, targetExcluded, targetDisinherited);
	}

	/**
	 * Set up test data
	 * @throws NodeException
	 */
	@Before
	public void setUp() throws Exception {
		setUpBaseData();
		reReadObjects();

		Transaction t = TransactionManager.getCurrentTransaction();

		german = Creator.createLanguage("German", "de");
		english = Creator.createLanguage("English", "en");
		german = t.getObject(ContentLanguage.class, german.getId(), true);
		List<Node> masters = Arrays.asList(channels.getNode(Channel.MASTER), channels.getNode(Channel.OTHERMASTER));
		german.getNodes().addAll(masters);
		german.save();
		german = t.getObject(ContentLanguage.class, german.getId());
		english = t.getObject(ContentLanguage.class, english.getId(), true);
		english.getNodes().addAll(masters);
		english.save();
		english = t.getObject(ContentLanguage.class, english.getId());

		template = ContentNodeTestDataUtils.createTemplate(sourceFolder, "Template source", "Template");
		movedGermanPage = Creator.createPage("Moved Page", sourceFolder, template, german, sourceNode);
		movedGermanPage.publish();
		movedEnglishPage = Creator.createLanguageVariant(movedGermanPage, english);
		movedEnglishPage.publish();
		if (localized != null && localized != Channel.NONE) {
			localizedGermanPage = ContentNodeTestDataUtils.localize(movedGermanPage, channels.getNode(localized));
			t.getObject(Page.class, localizedGermanPage.getId(), true).publish();
			localizedEnglishPage = ContentNodeTestDataUtils.localize(movedEnglishPage, channels.getNode(localized));
			t.getObject(Page.class, localizedEnglishPage.getId(), true).publish();
		}

		t.commit(false);

		setUpPublish();
	}

	/**
	 * Test moving the page
	 * @throws NodeException
	 */
	@Test
	public void testMovePage() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Page toMove = t.getObject(Page.class, movedGermanPage.getId(), true);
		boolean expectFailure = false;
		if (localized != null && localized != Channel.NONE) {
			if (!localized.channelOf(target)) {
				expectFailure = true;
			}
			if (targetExcluded) {
				expectFailure = true;
			}
			if (!ObjectTransformer.isEmpty(targetDisinherited)) {
				for (Channel c : targetDisinherited) {
					if (localized == c || localized.channelOf(c)) {
						expectFailure = true;
						break;
					}
				}
			}
		}
		OpResult result = toMove.move(targetFolder, targetNode.getId(), true);
		t.commit(false);

		reReadObjects();

		if (expectFailure) {
			assertFalse("Moving should have failed", result.isOK());
		} else {
			assertTrue("Moving failed" + result.getMessages(), result.isOK());

			for (Page testedPage : Arrays.asList(movedGermanPage, movedEnglishPage)) {
				testedPage = t.getObject(Page.class, testedPage.getId());
				assertEquals("Check folder of " + testedPage + " after moving", targetFolder, testedPage.getFolder());
				assertEquals("Check channel of " + testedPage + " after moving", targetNode.isChannel() ? targetNode : null, testedPage.getChannel());

				assertEquals("Check exclusion of " + testedPage + " after moving", targetExcluded, testedPage.isExcluded());
				ChannelTreeSegment folderSegment = new ChannelTreeSegment(targetFolder, true);
				Set<Node> folderNodes = new HashSet<Node>(folderSegment.getAllNodes());
				for (Iterator<Node> i = folderNodes.iterator(); i.hasNext();) {
					Node node = i.next();
					if (!node.equals(targetNode) && !node.isChannelOf(targetNode)) {
						i.remove();
					}
				}
				ChannelTreeSegment pageSegment = new ChannelTreeSegment(testedPage, true);
				assertEquals("Check visibility of " + testedPage + " after moving", folderNodes, pageSegment.getAllNodes());
			}

			for (Page testedLocalizedPage : Arrays.asList(localizedGermanPage, localizedEnglishPage)) {
				if (testedLocalizedPage != null) {
					testedLocalizedPage = t.getObject(Page.class, testedLocalizedPage.getId());

					assertEquals("Check folder of " + testedLocalizedPage + " after moving", targetFolder, testedLocalizedPage.getFolder());
					assertEquals("Check channel of " + testedLocalizedPage + " after moving", channels.getNode(localized), localizedGermanPage.getChannel());
				}
			}

			// run a second publish process
			testContext.publish(secondPublishTime);

			// check that objects are published correctly
			testContext.startTransaction(secondCheckTime);
			assertPublishedObjects();
		}
	}

	/**
	 * Reread objects
	 * @throws Exception 
	 */
	protected void reReadObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (movedGermanPage != null) {
			movedGermanPage = t.getObject(Page.class, movedGermanPage.getId());
		}
		if (movedEnglishPage != null) {
			movedEnglishPage = t.getObject(Page.class, movedEnglishPage.getId());
		}
		if (localizedGermanPage != null) {
			localizedGermanPage = t.getObject(Page.class, localizedGermanPage.getId());
		}
		if (localizedEnglishPage != null) {
			localizedEnglishPage = t.getObject(Page.class, localizedEnglishPage.getId());
		}

		reReadBaseObjects();
	}

	/**
	 * Assert that the objects have been published into the correct channels
	 * @throws Exception
	 */
	protected void assertPublishedObjects() throws Exception {
		List<? extends Disinheritable<? extends NodeObject>> checkObjects;
		if (localized == Channel.NONE) {
			checkObjects = Arrays.asList(movedGermanPage);
		} else {
			checkObjects = Arrays.asList(movedGermanPage, movedEnglishPage, localizedGermanPage, localizedEnglishPage);
		}

		for (Disinheritable<? extends NodeObject> object : checkObjects) {
			ChannelTreeSegment objectSegment = new ChannelTreeSegment(object, false);
			for (Node node : channels.getAllNodes()) {
				ContentNodeTestUtils.assertPublishCR(object, node,
						objectSegment.getAllNodes().contains(node) && object.equals(MultichannellingFactory.getChannelVariant(object, node)));
			}
		}
	}
}
