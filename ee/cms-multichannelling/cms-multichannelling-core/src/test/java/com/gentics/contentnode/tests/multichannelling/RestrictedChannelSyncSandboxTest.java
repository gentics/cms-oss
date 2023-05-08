package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest;
import com.gentics.contentnode.rest.model.request.PushToMasterRequest.Type;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.MCPageResourceImpl;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for trying to do channel synchronization with restricted permissions
 */
@RunWith(value = Parameterized.class)
public class RestrictedChannelSyncSandboxTest extends AbstractMultichannellingSandboxTest {
	/**
	 * True if restricted to master
	 */
	protected boolean master;

	/**
	 * True if restricted to channel
	 */
	protected boolean channel;

	/**
	 * True if restricted to subchannel
	 */
	protected boolean subchannel;

	/**
	 * Push target
	 */
	protected PushTarget target;

	/**
	 * Test user
	 */
	private SystemUser user;

	/**
	 * Get the test parameters
	 * @return collection of test parameter sets
	 */
	@Parameters(name = "{index}: master {0}, channel {1}, subchannel {2}, push to {3}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<Object[]>();
		for (boolean master : Arrays.asList(true, false)) {
			for (boolean channel : Arrays.asList(true, false)) {
				for (boolean subchannel : Arrays.asList(true, false)) {
					for (PushTarget target : PushTarget.values()) {
						data.add(new Object[] {master, channel, subchannel, target});
					}
				}
			}
		}
		return data;
	}

	/**
	 * Create a test instance
	 * @param master true if user is restricted to master node
	 * @param channel true if user is restricted to channel
	 * @param subchannel true if user is restricted to subchannel
	 * @param target push target
	 */
	public RestrictedChannelSyncSandboxTest(boolean master, boolean channel, boolean subchannel, PushTarget target) {
		this.master = master;
		this.channel = channel;
		this.subchannel = subchannel;
		this.target = target;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		if (master) {
			restrictUserToChannel(DBTestContext.USER_WITH_PERMS, MASTER_ID);
		}
		if (channel) {
			restrictUserToChannel(DBTestContext.USER_WITH_PERMS, channelId);
		}
		if (subchannel) {
			restrictUserToChannel(DBTestContext.USER_WITH_PERMS, subChannelId);
		}
		TransactionManager.getCurrentTransaction().commit(false);
		testContext.getContext().startTransaction(DBTestContext.USER_WITH_PERMS);

		user = Trx.supply(t -> t.getObject(SystemUser.class, DBTestContext.USER_WITH_PERMS));
	}

	/**
	 * Test pushing a page to the master
	 * @throws NodeException
	 */
	@Test
	public void testPushPageToMaster() throws Exception {
		Transaction t = TransactionManager.getCurrentTransaction();
		
		// Get the master node.
		Node masterNode = t.getObject(Node.class, MASTER_ID);
    	
		// Get the root folder from the master node.
		Folder masterNodeRootFolder = masterNode.getFolder();
    	
		// Create test folder in the master node
		Folder testFolder = t.createObject(Folder.class);
		testFolder.setName("ChannelSyncTest");
		testFolder.setMotherId(masterNodeRootFolder.getId());
		testFolder.save();
		t.commit(false);
		
		// Localize the test page in the channel
		Page localTestPage = t.createObject(Page.class);
		localTestPage.setName("LocalChannelSyncTest");
		localTestPage.setTemplateId(C.reference_test_data.template1_id);
		localTestPage.setFolderId(testFolder.getId());
		localTestPage.setChannelInfo(channelId, localTestPage.getChannelSetId());
		localTestPage.save();
		localTestPage.publish();
		t.commit(false);

		testContext.startTransactionWithPermissions(false);

		try (Trx trx = new Trx(user)) {
			PushToMasterRequest req = new PushToMasterRequest();
			req.setMasterId(target == PushTarget.master ? MASTER_ID : channelId);
			req.setChannelId(subChannelId);
			req.setForegroundTime(Integer.MAX_VALUE);
			req.setTypes(Arrays.asList(Type.page));
			GenericResponse response = new MCPageResourceImpl().pushToMaster(ObjectTransformer.getInteger(localTestPage.getId(), null), req);
			assertEquals("Check response code", getExpectedResponseCode(), response.getResponseInfo().getResponseCode());
		}
	}

	/**
	 * Get the expected response code for the test
	 * @return expected response code
	 */
	protected ResponseCode getExpectedResponseCode() {
		// not restricted at all, so pushing is allowed
		if (!master && !channel && !subchannel) {
			return ResponseCode.OK;
		}
		// cannot push if no permission on the subchannel or the channel
		if (!subchannel || !channel) {
			return ResponseCode.PERMISSION;
		}
		if (target == PushTarget.master && !master) {
			return ResponseCode.PERMISSION;
		}
		return ResponseCode.OK;
	}

	/**
	 * Push targets
	 */
	public static enum PushTarget {
		master,
		channel
	}
}
