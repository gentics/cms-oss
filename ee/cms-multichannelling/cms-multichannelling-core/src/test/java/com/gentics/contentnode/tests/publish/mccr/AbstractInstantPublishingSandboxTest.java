package com.gentics.contentnode.tests.publish.mccr;

import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameters;

/**
 * Abstract Test for Instant publishing tests
 */
public class AbstractInstantPublishingSandboxTest extends AbstractMCCRPublishingSandboxTest {

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
		setUpTestData(false, true);
	}


	/**
	 * Create a parameterized instance of the test case
	 * @param isPageOnline true if the page should be published
	 * @param inheritance inheritance setting
	 * @param featureActivated feature setting
	 * @param forceOnline force setting
	 * @param dependency dependency setting
	 */
	public AbstractInstantPublishingSandboxTest(MCSetting onlineStatus, Inheritance inheritance, ObjectType objectType) {
		this.onlineStatus = onlineStatus;
		this.inheritance = inheritance;
		this.objectType = objectType;
	}


	/**
	 * Check if the object should be available in master cr.
	 * 
	 * @param onlineStatus online status of the object
	 * @param inheritance inheritance setting
	 * @param delete true if the object was deleted
	 * @return true if the object is expected to exist in the master, false if not
	 */
	protected boolean isObjectExpectedInMaster(MCSetting onlineStatus, Inheritance inheritance, boolean delete) {
		switch (inheritance) {
		case INHERITED:
			if (delete) {
				// the master object was deleted
				return false;
			} else {
				// master object must be online to be published
				return onlineStatus == MCSetting.MASTER;
			}

		case LOCAL:
			// local objects are never published to the master
			return false;

		case LOCALIZED:
			// the master object must be online to be published
			return onlineStatus == MCSetting.MASTER || onlineStatus == MCSetting.BOTH; 

		default:
			return false;
		}
	}
	
	/**
	 * Check if the object should be available in channel cr.
	 * 
	 * @param onlineStatus online status of the object
	 * @param inheritance inheritance setting
	 * @param delete true if the object was deleted
	 * @return true if the object is expected to exist in the channel, false if not
	 */
	protected boolean isObjectExpectedInChannel(MCSetting onlineStatus, Inheritance inheritance, boolean delete) {
		switch (inheritance) {
		case INHERITED:
			// inherited objects are not written into the channel in instant publishing
			return false;

		case LOCAL:
			if (delete) {
				// local channel object has been deleted
				return false;
			} else {
				// local channel object must be online to be published
				return onlineStatus == MCSetting.CHANNEL;
			}

		case LOCALIZED:
			if (delete) {
				// the localized object was deleted, so the inherited master object is checked
				return onlineStatus == MCSetting.MASTER || onlineStatus == MCSetting.BOTH; 
			} else {
				// the localized object must be online to be published
				return onlineStatus == MCSetting.CHANNEL || onlineStatus == MCSetting.BOTH; 
			}
		default:
			return false;
		}
	}
}
