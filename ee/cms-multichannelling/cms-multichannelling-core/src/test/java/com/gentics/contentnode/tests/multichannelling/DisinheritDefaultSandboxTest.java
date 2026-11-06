package com.gentics.contentnode.tests.multichannelling;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.ChannelTrx;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Disinheritable;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.page.PageCopyOpResult;
import com.gentics.contentnode.object.page.PageCopyOpResultInfo;
import com.gentics.contentnode.rest.model.request.NodeSaveRequest;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.NodeLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.NodeResourceImpl;
import com.gentics.contentnode.testutils.Creator;

/**
 * Test cases for the <code>disinheritDefault</code> flag.
 */
public class DisinheritDefaultSandboxTest extends
		AbstractMultichannellingRestrictionSandboxTest {

	/**
	 * Creates test data.
	 *
	 * @return An array containing the test data.
	 */
	@Parameterized.Parameters(name = "{index} Object: {0} useRootFolder: {1}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> result = new ArrayList<>();

		for (DisinheritableObjTypes type : DisinheritableObjTypes.values()) {
			result.add(new Object[] { type, true });
			result.add(new Object[] { type, false });
		}

		return result;
	}

	@After
	public void cleanUp() throws NodeException {
		testFolder.setDisinheritDefault(false, true);
	}

	/**
	 * Default constructor for each test case.
	 *
	 * @see #parameters
	 *
	 * @param objType The type of the object to be create.
	 * @param useRootFolder Whether the object is to be created in the root folder,
	 *		or a subfolder.
	 */
	public DisinheritDefaultSandboxTest(
			DisinheritableObjTypes objType,
			boolean useRootFolder) {
		super(objType, useRootFolder);
	}

	/**
	 * Tests whether setting the <code>disinheritDefault</code> flag on
	 * a folder automatically propagates to elements in the folder.
	 *
	 * @throws Exception On Test failire.
	 */
	@Test
	public void testSetDisinheritDefault() throws NodeException {
		createObject(m);

		Transaction t = TransactionManager.getCurrentTransaction();

		testFolder.setDisinheritDefault(true, false);
		masterObject = (Disinheritable<?>) t.getObject(masterObject.getObjectInfo().getObjectClass(), masterObject.getId());

		assertTrue(
			"Disinherit default flag must be set for object, if it is set for the parent",
			masterObject.isDisinheritDefault());
	}

	/**
	 * Tests if the <code>disinheritDefault</code> flag is automatically
	 * set for newly created object if their containing folder has it set.
	 *
	 * @throws NodeException On test failure.
	 */
	@Test
	public void testCreateInFolderWithFlagSet() throws NodeException {
		testFolder.setDisinheritDefault(true, false);
		createObject(m);

		assertTrue(
			"Object must be created with disinherit default flag set if its folder has it set",
			masterObject.isDisinheritDefault());
	}

	/**
	 * Test moving an object without the <code>disinheritDefault</code> flag
	 * into a folder which has the flag set.
	 *
	 * The result should be the object having the flag set after moving.
	 *
	 * @throws Exception On test failure.
	 */
	@Test
	public void testMoveToFolderWithFlagSet() throws NodeException {
		createObject(m);

		Transaction t = TransactionManager.getCurrentTransaction();
		Folder dst = Creator.createFolder(testFolder, "dst", "/");

		dst.setDisinheritDefault(true, false);

		if (masterObject instanceof Page) {
			((Page) masterObject).move(dst, m.getId(), true);
		} else if (masterObject instanceof Folder) {
			masterObject = t.getObject(Folder.class, masterObject.getId(), true);
			((Folder) masterObject).move(dst, m.getId());
		} else if (masterObject instanceof ContentFile) {
			((ContentFile) masterObject).move(dst, m.getId());
		}

		masterObject = (Disinheritable<?>) t.getObject(
			masterObject.getObjectInfo().getObjectClass(),
			masterObject.getId());

		assertTrue(
			"Object must have the disinherite default flag set after moving to a folder which has it set",
			masterObject.isDisinheritDefault());
	}

	/**
	 * Test copying an object without the <code>disinheritDefault</code> flag
	 * into a folder which has the flag set.
	 *
	 * The result should be the object having the flag set after copying.
	 *
	 * This test only applies to pages.
	 *
	 * @throws Exception On test failure.
	 */
	@Test
	public void testCopyToFolderWithFlagSet() throws NodeException {
		if (objType != DisinheritableObjTypes.PAGE) {
			return;
		}

		createObject(m);

		Folder dst = Creator.createFolder(testFolder, "dst", "/");
		Integer masterId = m.getId();

		dst.setDisinheritDefault(true, false);

		if (masterObject instanceof Page) {
			PageCopyOpResult result = ((Page) masterObject).copyTo(masterId, dst, false, 0, masterId);

			for (PageCopyOpResultInfo info: result.getCopyInfos()) {
				assertTrue(
					"Copied object must have the disinherite default flag set after copying to a folder which has it set",
					info.getCreatedPageCopy().isDisinheritDefault());
			}
		}
	}

	/**
	 * Test whether objects with the <code>disinheritDefault</code> flag
	 * are automatically disinherited in a newly created channel.
	 *
	 * @throws NodeException On test failure.
	 */
	@Test
	public void testCreateNewChannel() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		createObject(m);

		if (testFolder == someFolder) {
			someFolder.setDisinheritDefault(true, true);
		} else {
			masterObject.setDisinheritDefault(true, true);
		}
		t.commit(false);

		NodeResourceImpl resource = new NodeResourceImpl();

		NodeLoadResponse response = resource.add(getSaveRequest());
		t.commit(false);

		assertNotNull(
			"Channel creation request must succeed but response was null",
			response);

		ResponseCode code = response.getResponseInfo().getResponseCode();
		StringJoiner messages = new StringJoiner(", ");

		if (code != ResponseCode.OK) {
			for (Message m: response.getMessages()) {
				messages.add(m.getMessage() + " (" + m.getFieldName() + ")");
			}
		}

		assertTrue(
			"Channel creation must succeed but response code was " + code
				+ "(messages: " + messages.toString() + ")",
			code == ResponseCode.OK);

		Node newChannel = t.getObject(Node.class, response.getNode().getId());

		try (ChannelTrx trx = new ChannelTrx(newChannel)) {
			if (testFolder == someFolder) {
				assertNull(
					"The testfolder should not be visible in the new channel",
					t.getObject(Folder.class, testFolder.getId()));
			}

			assertNull(
				"The object should not be visible in the new channel",
				t.getObject(masterObject.getObjectInfo().getObjectClass(), masterObject.getId()));
		}
	}
	
	/**
	 * Creates a {@link NodeSaveRequest} with a unique node- and hostname
	 * for the current test data.
	 *
	 * @return A <code>NodeSaveRequest</code> for the current test data.
	 */
	private NodeSaveRequest getSaveRequest() {
		com.gentics.contentnode.rest.model.Node restNode = new com.gentics.contentnode.rest.model.Node();
		String name = "channel-" + objType.toString().toLowerCase() + "-"
			+ (testFolder == rootFolder ? "rootfolder" : "somefolder");
		
		restNode.setName(name);
		restNode.setHost(name);
		restNode.setUtf8(true); // Aloha editor is used by default, and it needs utf8.
		restNode.setMasterId(rootFolder.getId());

		NodeSaveRequest request = new NodeSaveRequest();

		request.setNode(restNode);

		return request;
	}
}
