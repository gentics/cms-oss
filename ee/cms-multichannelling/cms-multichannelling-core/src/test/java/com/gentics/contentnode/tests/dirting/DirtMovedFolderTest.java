package com.gentics.contentnode.tests.dirting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.OpResult;
import com.gentics.contentnode.publish.PublishQueue;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.tests.utils.TestedInheritance;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;

/**
 * Test cases for dirting moved folders in channel structures
 */
@GCNFeature(set = {Feature.MULTICHANNELLING})
@RunWith(value = Parameterized.class)
public class DirtMovedFolderTest {
	@Rule
	public DBTestContext testContext = new DBTestContext();

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: source {0}, target {1}, moved folder {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (TestedInheritance source : Arrays.asList(TestedInheritance.inherited, TestedInheritance.localized)) {
			for (TestedInheritance target : Arrays.asList(TestedInheritance.inherited, TestedInheritance.localized)) {
				for (TestedInheritance moved : Arrays.asList(TestedInheritance.inherited, TestedInheritance.localized)) {
					data.add(new Object[] { source, target, moved });
				}
			}
		}
		return data;
	}

	@Parameter(0)
	public TestedInheritance source;

	@Parameter(1)
	public TestedInheritance target;

	@Parameter(2)
	public TestedInheritance moved;

	@Test
	public void testMove() throws Exception {
		Node node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		Node channel = Trx.supply(() -> ContentNodeTestDataUtils.createChannel(node, "Channel", "channel", "/"));

		Folder sourceFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Source"));
		Folder sourceInChannel = null;
		switch (source) {
		case inherited:
			sourceInChannel = sourceFolder;
			break;
		case localized:
			sourceInChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(sourceFolder, channel));
			break;
		default:
			fail("source inheritance setting must not be " + source);
		}
		Folder targetFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(node.getFolder(), "Target"));
		Folder targetInChannel = null;
		switch (target) {
		case inherited:
			targetInChannel = targetFolder;
			break;
		case localized:
			targetInChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(targetFolder, channel));
			break;
		default:
			fail("target inheritance setting must not be " + target);
		}

		Folder movedFolder = Trx.supply(() -> ContentNodeTestDataUtils.createFolder(sourceFolder, "Moved"));
		Folder movedInChannel = null;
		switch (moved) {
		case inherited:
			movedInChannel = movedFolder;
			break;
		case localized:
			movedInChannel = Trx.supply(() -> ContentNodeTestDataUtils.localize(movedFolder, channel));
			break;
		default:
			fail("moved inheritance setting must not be " + moved);
		}

		testContext.publish(false);

		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			OpResult result = t.getObject(movedFolder, true).move(targetFolder, 0);
			assertEquals("Check operation result", OpResult.Status.OK, result.getStatus());
		});

		Trx.operate(() ->  {
			try {
				testContext.waitForDirtqueueWorker();
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});

		final Integer[] expectedMasterIds = new Integer[] {movedFolder.getId(), sourceFolder.getId(), targetFolder.getId()};
		final Integer[] expectedChannelIds = new Integer[] {movedInChannel.getId(), sourceInChannel.getId(), targetInChannel.getId()};
		Trx.operate(() -> {
			List<Integer> dirtedFolderIds = PublishQueue.getDirtedObjectIds(Folder.class, false, node);
			assertThat(dirtedFolderIds).as("Dirted folders in node").containsOnly(expectedMasterIds);

			dirtedFolderIds = PublishQueue.getDirtedObjectIds(Folder.class, false, channel);
			assertThat(dirtedFolderIds).as("Dirted folders in channel").containsOnly(expectedChannelIds);
		});
	}
}
