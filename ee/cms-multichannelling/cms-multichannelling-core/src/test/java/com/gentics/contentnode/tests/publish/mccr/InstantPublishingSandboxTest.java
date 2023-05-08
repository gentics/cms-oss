package com.gentics.contentnode.tests.publish.mccr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;

/**
 * Test cases for the instant publishing
 */
@RunWith(value = Parameterized.class)
public class InstantPublishingSandboxTest extends AbstractInstantPublishingSandboxTest {
	/**
	 * Create a parameterized instance of the test case
	 * @param isPageOnline true if the page should be published
	 * @param inheritance inheritance setting
	 * @param featureActivated feature setting
	 * @param forceOnline force setting
	 * @param dependency dependency setting
	 */
	public InstantPublishingSandboxTest(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) {
		super(onlineStatus, inheritance, objectType);
	}

	/**
	 * Publishing test
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Node master = getMaster();
		Node channel = getChannel();

		boolean expectInMaster = false;
		boolean expectInChannel = false;

		switch (objectType) {
		case PAGE:
			Page page = getTestPage(onlineStatus, inheritance, objectType);
			Page channelPage = MultichannellingFactory.getChannelVariant(page, channel);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, false);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, false);

			// check page in master
			assertPageInContentrepository(master, page, expectInMaster);

			// check page in channel
			assertPageInContentrepository(channel, channelPage, expectInChannel);
			break;

		case FILE:
			File file = getTestFile(onlineStatus, inheritance, objectType);
			File channelFile = file.getChannelVariant(channel);
					
			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, false);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, false);

			// check file in master
			assertFileInContentrepository(master, file, expectInMaster);

			// check file in channel
			assertFileInContentrepository(channel, channelFile, expectInChannel);
			break;

		case IMAGE:
			ImageFile image = getTestImage(onlineStatus, inheritance, objectType);
			File channelImage = ((File) image).getChannelVariant(channel);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, false);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, false);

			// check image in master
			assertFileInContentrepository(master, image, expectInMaster);

			// check image in channel
			assertFileInContentrepository(channel, channelImage, expectInChannel);
			break;

		case FOLDER:
			Folder folder = getTestFolder(onlineStatus, inheritance, objectType);
			Folder channelFolder = MultichannellingFactory.getChannelVariant(folder, channel);

			expectInMaster = isObjectExpectedInMaster(onlineStatus, inheritance, false);
			expectInChannel = isObjectExpectedInChannel(onlineStatus, inheritance, false);

			// check folder in master
			assertFolderInContentrepository(master, folder, expectInMaster);

			// check folder in channel
			assertFolderInContentrepository(channel, channelFolder, expectInChannel);
			break;

		default:
			break;
		}
	}
}
