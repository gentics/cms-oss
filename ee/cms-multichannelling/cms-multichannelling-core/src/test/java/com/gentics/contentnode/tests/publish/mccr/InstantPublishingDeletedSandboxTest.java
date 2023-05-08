package com.gentics.contentnode.tests.publish.mccr;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.testutils.GCNFeature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;

/**
 * Test cases for removing objects from mccr contentrepositories with instant publishing
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MCCR})
public class InstantPublishingDeletedSandboxTest extends AbstractInstantPublishingSandboxTest {
	/**
	 * Create a parameterized instance of the test case
	 * @param isPageOnline true if the page should be published
	 * @param inheritance inheritance setting
	 * @param featureActivated feature setting
	 * @param forceOnline force setting
	 * @param dependency dependency setting
	 */
	public InstantPublishingDeletedSandboxTest(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) {
		super(onlineStatus, inheritance, objectType);
	}

	/**
	 * Test deleting the object with instant publishing.
	 * If the object is localized, only the localized copy is deleted.
	 * @throws Exception
	 */
	@Test
	public void testDelete() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		Node master = getMaster();
		Node channel = getChannel();

		boolean expectInMaster = false;
		boolean expectInChannel = false;

		switch (objectType) {
		case PAGE:
			Page page = getTestPage(onlineStatus, inheritance, objectType);
			Page channelPage = MultichannellingFactory.getChannelVariant(page, channel);

			// delete the channel page
			channelPage.delete();
			t.commit(false);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, true);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, true);

			// check page in master
			assertPageInContentrepository(master, page, expectInMaster);

			// check page in channel
			assertPageInContentrepository(channel, channelPage, false);
			assertPageInContentrepository(channel, page, expectInChannel);
			break;

		case FILE:
			File file = getTestFile(onlineStatus, inheritance, objectType);
			File channelFile = file.getChannelVariant(channel);

			// delete the channel file
			channelFile.delete();
			t.commit(false);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, true);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, true);

			// check file in master
			assertFileInContentrepository(master, file, expectInMaster);

			// check file in channel
			assertFileInContentrepository(channel, channelFile, false);
			assertFileInContentrepository(channel, file, expectInChannel);
			break;

		case IMAGE:
			ImageFile image = getTestImage(onlineStatus, inheritance, objectType);
			File channelImage = ((File) image).getChannelVariant(channel);

			// delete the channel file
			channelImage.delete();
			t.commit(false);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, true);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, true);

			// check image in master
			assertFileInContentrepository(master, image, expectInMaster);

			// check image in channel
			assertFileInContentrepository(channel, channelImage, false);
			assertFileInContentrepository(channel, image, expectInChannel);
			break;

		case FOLDER:
			Folder folder = getTestFolder(onlineStatus, inheritance, objectType);
			Folder channelFolder = MultichannellingFactory.getChannelVariant(folder, channel);

			// delete the channel file
			channelFolder.delete();
			t.commit(false);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, true);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, true);

			// check folder in master
			assertFolderInContentrepository(master, folder, expectInMaster);

			// check folder in channel
			assertFolderInContentrepository(channel, channelFolder, false);
			assertFolderInContentrepository(channel, folder, expectInChannel);
			break;

		default:
			break;
		}
	}
}
