package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.factory.object.FileOnlineStatus;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.etc.Feature;

/**
 * Test cases for the feature {@link Feature#CONTENTFILE_AUTO_OFFLINE} in nodes and channels
 */
@RunWith(value = Parameterized.class)
public class AutoOfflineForeignStructurePublishTest extends AbstractOfflinePublishSandboxTest {

	/**
	 * True when testing an image
	 */
	protected boolean image;

	/**
	 * Where dependencies are set
	 */
	protected MCSetting dependency;

	/**
	 * Which files have force online set
	 */
	protected MCSetting forceOnline;

	/**
	 * Where the feature is activated
	 */
	protected MCSetting featureActivated;

	/**
	 * Inheritance setting of the file
	 */
	protected Inheritance inheritance;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: image {0}, inheritance {1}, feature {2}, force {3}, dependency {4}")
	public static Collection<Object[]> data() {
		return AbstractOfflinePublishSandboxTest.getTestParameters();
	}

	@BeforeClass
	public static void setUpOnce() throws Exception {
		prepareSandbox();
		setUpTestData(true, false, true);
	}

	/**
	 * Create a parameterized instance of the test case
	 * @param image true to test an image
	 * @param inheritance inheritance setting
	 * @param featureActivated feature setting
	 * @param forceOnline force setting
	 * @param dependency dependency setting
	 */
	public AutoOfflineForeignStructurePublishTest(boolean image, Inheritance inheritance, MCSetting featureActivated, MCSetting forceOnline,
			MCSetting dependency) {
		this.image = image;
		this.inheritance = inheritance;
		this.featureActivated = featureActivated;
		this.forceOnline = forceOnline;
		this.dependency = dependency;
	}

	/**
	 * Test whether the file was published
	 * @throws Exception
	 */
	@Test
	public void testPublish() throws Exception {
		Node channel = getChannel(featureActivated);
		Node master = channel.getMasterNodes().get(0);

		// get the expected file id
		File file = getTestFile(image, inheritance, featureActivated, forceOnline, dependency, true);

		boolean expectInMaster = false;
		boolean expectInChannel = false;

		switch (inheritance) {
		case INHERITED:
			expectInMaster = !featureActivated.inMaster() || forceOnline.inMaster() || dependency.inMaster() || dependency.inChannel();
			expectInChannel = !featureActivated.inChannel() || forceOnline.inMaster();
			break;

		case LOCALIZED:
			expectInMaster = !featureActivated.inMaster() || forceOnline.inMaster() || dependency.inMaster() || dependency.inChannel();
			expectInChannel = !featureActivated.inChannel() || forceOnline.inChannel();
			break;

		case LOCAL:
			expectInMaster = false;
			expectInChannel = !featureActivated.inChannel() || forceOnline.inChannel();
			break;
		}

		// check file in master
		assertFileInFilesystem(master, file, expectInMaster);
		assertFileInContentrepository(master, file, expectInMaster);

		// test online status
		assertEquals("Check online status in master", expectInMaster, FileOnlineStatus.isOnline(file, master));

		// check file in channel
		assertFileInFilesystem(channel, file, expectInChannel);
		assertFileInContentrepository(channel, file, expectInChannel);

		// test online status
		assertEquals("Check online status in channel", expectInChannel, FileOnlineStatus.isOnline(file, channel));
	}
}
