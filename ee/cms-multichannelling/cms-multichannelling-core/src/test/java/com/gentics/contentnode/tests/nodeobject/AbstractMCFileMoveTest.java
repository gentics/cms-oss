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
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.ChannelStructure.Channel;

/**
 * Test cases for moving files and pages
 */
@RunWith(value = Parameterized.class)
public class AbstractMCFileMoveTest extends AbstractMCFilePageMoveTest {
	protected File movedFile;
	protected File localizedFile;



	public AbstractMCFileMoveTest(Channel source, Channel target, boolean differentFolder, Channel localized, boolean targetExcluded,
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

		movedFile = ContentNodeTestDataUtils.createFile(sourceFolder, "testfile.txt", "Test File Data".getBytes(), sourceNode);
		if (localized != null && localized != Channel.NONE) {
			localizedFile = ContentNodeTestDataUtils.localize(movedFile, channels.getNode(localized));
		}

		t.commit(false);

		setUpPublish();
		assertPublishedObjects();
	}

	/**
	 * Test moving the file
	 * @throws NodeException
	 */
	@Test
	public void testMoveFile() throws Exception {
		testContext.startTransaction(moveTime);
		Transaction t = TransactionManager.getCurrentTransaction();
		File toMove = t.getObject(File.class, movedFile.getId(), true);
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
		OpResult result = toMove.move(targetFolder, targetNode.getId());
		t.commit(false);

		reReadObjects();

		if (expectFailure) {
			assertFalse("Moving should have failed", result.isOK());
		} else {
			assertTrue("Moving failed" + result.getMessages(), result.isOK());

			movedFile = t.getObject(File.class, movedFile.getId());
			assertEquals("Check folder after moving", targetFolder, movedFile.getFolder());
			assertEquals("Check channel after moving", targetNode.isChannel() ? targetNode : null, movedFile.getChannel());

			assertEquals("Check exclusion", targetExcluded, movedFile.isExcluded());
			ChannelTreeSegment folderSegment = new ChannelTreeSegment(targetFolder, true);
			Set<Node> folderNodes = new HashSet<Node>(folderSegment.getAllNodes());
			for (Iterator<Node> i = folderNodes.iterator(); i.hasNext(); ) {
				Node node = i.next();
				if (!node.equals(targetNode) && !node.isChannelOf(targetNode)) {
					i.remove();
				}
			}
			ChannelTreeSegment fileSegment = new ChannelTreeSegment(movedFile, true);
			assertEquals("Check file visibility", folderNodes, fileSegment.getAllNodes());

			if (localizedFile != null) {
				localizedFile = t.getObject(File.class, localizedFile.getId());

				assertEquals("Check folder after moving", targetFolder, localizedFile.getFolder());
				assertEquals("Check channel after moving", channels.getNode(localized), localizedFile.getChannel());
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
	 * @throws NodeException
	 */
	protected void reReadObjects() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		if (movedFile != null) {
			movedFile = t.getObject(File.class, movedFile.getId());
		}
		if (localizedFile != null) {
			localizedFile = t.getObject(File.class, localizedFile.getId());
		}

		reReadBaseObjects();
	}

	/**
	 * Assert that the objects have been published into the correct channels
	 * @throws Exception
	 */
	protected void assertPublishedObjects() throws Exception {
		if (localized == Channel.NONE) {
			assertPublishedObjects(Arrays.asList(movedFile));
		} else {
			assertPublishedObjects(Arrays.asList(movedFile, localizedFile));
		}
	}
}
