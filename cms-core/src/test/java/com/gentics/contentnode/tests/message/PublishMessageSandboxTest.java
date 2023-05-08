package com.gentics.contentnode.tests.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;

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
		SystemUser editor = Trx.supply(t -> {
			List<Integer> ids = DBUtils.select("SELECT id FROM systemuser WHERE login = ?", ps -> {
				ps.setString(1, "editor");
			}, DBUtils.IDLIST);

			assertThat(ids).as("User IDs").isNotEmpty();
			return t.getObject(SystemUser.class, ids.get(0));
		});
		SystemUser publisher = Trx.supply(t -> {
			List<Integer> ids = DBUtils.select("SELECT id FROM systemuser WHERE login = ?", ps -> {
				ps.setString(1, "publisher");
			}, DBUtils.IDLIST);

			assertThat(ids).as("User IDs").isNotEmpty();
			return t.getObject(SystemUser.class, ids.get(0));
		});

		// login as editor
		String editorSID = testContext.getContext().login("editor", "editor");
		// login as publisher
		String publisherSID = testContext.getContext().login("publisher", "publisher");

		// set the backend language to "en" for all users
		setBackendLanguage("en", editorSID);
		setBackendLanguage("en", publisherSID);

		// modify the page
		editPage(PAGE_ID, editorSID);

		// let the editor publish the page
		publishPage(PAGE_ID, editorSID);

		// check the messages
		assertMessage(publisher, "editor", "editor editor wants to publish page ProcessTests/Page to translate|mod (53).");

		// let the publisher publish the page
		publishPage(PAGE_ID, publisherSID);

		// check the messages
		assertMessage(editor, "Publisher", "The page ProcessTests/Page to translate|mod (53) has been published.");
	}
}
