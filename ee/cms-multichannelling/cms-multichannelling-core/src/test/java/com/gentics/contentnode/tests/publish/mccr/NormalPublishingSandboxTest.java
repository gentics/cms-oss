package com.gentics.contentnode.tests.publish.mccr;

import static org.junit.Assert.assertTrue;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.testutils.GCNFeature;
import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.factory.MultichannellingFactory;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;

/**
 * Test cases for publishing objects in multichannelling aware content repositories
 */
@RunWith(value = Parameterized.class)
@GCNFeature(set = { Feature.MCCR})
public class NormalPublishingSandboxTest extends AbstractMCCRPublishingSandboxTest {

	/**
	 * True when page should be published
	 */
	protected MCSetting onlineStatus;

	/**
	 * Inheritance setting of the object
	 */
	protected AbstractMCCRPublishingSandboxTest.Inheritance inheritance;

	/**
	 * Object type setting
	 */
	protected ObjectType objectType;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: onlineStatus {0}, inheritance {1}, objectType {2}")
	public static Collection<Object[]> data() {
		return AbstractMCCRPublishingSandboxTest.getTestParameters();
	}


	@Before
	public void setUp() throws Exception {
		// activate the feature instant publishing
		testContext.getContext().getNodeConfig().getDefaultPreferences().setFeature("instant_cr_publishing", true);
		setUpTestData(true, false);
	}

	/**
	 * Create a parameterized instance of the test case
	 * @param isPageOnline true if the page should be published
	 * @param inheritance inheritance setting
	 * @param featureActivated feature setting
	 * @param forceOnline force setting
	 * @param dependency dependency setting
	 */
	public NormalPublishingSandboxTest(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) {
		this.onlineStatus = onlineStatus;
		this.inheritance = inheritance;
		this.objectType = objectType;
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
					
			expectInMaster = isObjectExpectInMaster(onlineStatus, inheritance);
			expectInChannel = isObjectExpectInChannel(onlineStatus, inheritance);
			
			// check file in master
			assertPageInContentrepository(master, page, expectInMaster);

			// check file in channel
			assertPageInContentrepository(channel, channelPage, expectInChannel);
			break;

		case FILE:
			File file = getTestFile(onlineStatus, inheritance, objectType);
			File channelFile = file.getChannelVariant(channel);
					
			expectInMaster = isObjectExpectInMaster(onlineStatus, inheritance);
			expectInChannel = isObjectExpectInChannel(onlineStatus, inheritance);
			
			// check file in master
			assertFileInContentrepository(master, file, expectInMaster);

			// check file in channel
			assertFileInContentrepository(channel, channelFile, expectInChannel);
			break;

		case IMAGE:
			ImageFile image = getTestImage(onlineStatus, inheritance, objectType);
			File channelImage = ((File) image).getChannelVariant(channel);
			
			expectInMaster = isObjectExpectInMaster(onlineStatus, inheritance);
			expectInChannel = isObjectExpectInChannel(onlineStatus, inheritance);
			
			// check file in master
			assertFileInContentrepository(master, image, expectInMaster);

			// check file in channel
			assertFileInContentrepository(channel, channelImage, expectInChannel);
			break;

		case FOLDER:
			Folder folder = getTestFolder(onlineStatus, inheritance, objectType);
			Folder channelFolder = MultichannellingFactory.getChannelVariant(folder, channel);
					
			expectInMaster = isObjectExpectInMaster(onlineStatus, inheritance);
			expectInChannel = isObjectExpectInChannel(onlineStatus, inheritance);
			
			// check file in master
			assertFolderInContentrepository(master, folder, expectInMaster);

			// check file in channel
			assertFolderInContentrepository(channel, channelFolder, expectInChannel);
			break;

		default:
			break;
		}
		
		assertTrue(true);
	}
	
	/**
	 * Check if the an object should be available in master node.
	 * 
	 * @param onlineStatus
	 * @param inheritance
	 * @return
	 */
	private boolean isObjectExpectInMaster(MCSetting onlineStatus, Inheritance inheritance) {
		switch (inheritance) {
		case INHERITED:
			return onlineStatus == MCSetting.MASTER;

		case LOCAL:
			return false;

		case LOCALIZED:
			return onlineStatus == MCSetting.MASTER || onlineStatus == MCSetting.BOTH; 

		default:
			return false;
		}
	}
	
	/**
	 * Check if the an object should be available in channel node.
	 * 
	 * @param onlineStatus
	 * @param inheritance
	 * @return
	 */
	private boolean isObjectExpectInChannel(MCSetting onlineStatus, Inheritance inheritance) {
		switch (inheritance) {
		case INHERITED:
			return onlineStatus == MCSetting.MASTER;

		case LOCAL:
			return onlineStatus == MCSetting.CHANNEL;

		case LOCALIZED:
			return onlineStatus == MCSetting.CHANNEL || onlineStatus == MCSetting.BOTH; 

		default:
			return false;
		}
	}
}
