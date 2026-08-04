package com.gentics.contentnode.tests.message;

import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.testutils.DBTestContext.EDITOR_USER_ID;
import static com.gentics.contentnode.testutils.DBTestContext.PUBLISHER_USER_ID;

import org.junit.Test;

import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.testutils.DBSessionClosure;

/**
 * Test cases for inbox message when sending pages to the queue
 */
public class PublishMessageSandboxTest extends AbstractMessagingSandboxTest {
	public static int PAGE_ID = 53;

	/**
	 * Test inbox messages when sending a page in the publish queue and publishing a page from there.
	 * @throws Exception
	 */
	@Test
	public void testPublishQueue() throws Exception {
		SystemUser editor = supply(t -> t.getObject(SystemUser.class, EDITOR_USER_ID));
		SystemUser publisher = Trx.supply(t -> t.getObject(SystemUser.class, PUBLISHER_USER_ID));

		// set the backend language to "en" for all users
		try (DBSessionClosure ses = new DBSessionClosure(EDITOR_USER_ID)) {
			setBackendLanguage("en", ses.getSession());
		}
		try (DBSessionClosure ses = new DBSessionClosure(PUBLISHER_USER_ID)) {
			setBackendLanguage("en", ses.getSession());
		}

		try (DBSessionClosure ses = new DBSessionClosure(EDITOR_USER_ID)) {
			// modify the page
			editPage(PAGE_ID, ses.getSession());
			// let the editor publish the page
			publishPage(PAGE_ID, ses.getSession());
		}

		// check the messages
		assertMessage(publisher, "editor", "editor editor möchte die Seite ProcessTests/Page to translate|mod (53) veröffentlichen.");

		try (DBSessionClosure ses = new DBSessionClosure(PUBLISHER_USER_ID)) {
			// let the publisher publish the page
			publishPage(PAGE_ID, ses.getSession());
		}

		// check the messages
		assertMessage(editor, "Publisher", "Die Seite ProcessTests/Page to translate|mod (53) wurde veröffentlicht.");
	}
}
