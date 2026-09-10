package com.gentics.contentnode.tests.message;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Session;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.request.PageSaveRequest;
import com.gentics.contentnode.rest.model.request.SetLanguageRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.PageLoadResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.impl.I18nResourceImpl;
import com.gentics.contentnode.rest.resource.impl.MessagingResourceImpl;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Abstract Testclass for messaging tests
 */
public abstract class AbstractMessagingSandboxTest  {

	@ClassRule
	public static DBTestContext testContext = new DBTestContext().config(prefs -> {
		// disable the feature "inbox_to_email"
		prefs.setFeature("inbox_to_email", false);
	});

	@BeforeClass
	public static void setupOnce() throws NodeException {
		TransactionManager.getCurrentTransaction().commit();
	}

	@Before
	public void setUp() throws NodeException {
		// mark all current messages as "read"
		operate(() -> {
			update("UPDATE msg SET oldmsg = ?", 1);
		});
	}

	/**
	 * Assert ResponseCode.OK in the given response
	 * 
	 * @param response
	 *            response
	 * @throws Exception
	 */
	public void assertResponseOK(GenericResponse response) throws Exception {
		assertEquals("Check response code", ResponseCode.OK, response.getResponseInfo().getResponseCode());
	}

	/**
	 * Set the backend language
	 * 
	 * @param code
	 *            code of the backend language ("de" or "en")
	 * @param session
	 *            session
	 * @throws Exception
	 */
	public void setBackendLanguage(String code, Session session) throws Exception {
		try (DBSessionClosure ses = new DBSessionClosure(session)) {
			I18nResourceImpl i18nRes = new I18nResourceImpl();

			SetLanguageRequest setLangReq = new SetLanguageRequest();

			setLangReq.setCode(code);
			assertResponseOK(i18nRes.setLanguage(setLangReq));
		}
	}

	/**
	 * Edit the page with the user with given sid
	 * 
	 * @param pageId
	 *            page id
	 * @param session
	 *            session
	 * @throws Exception
	 */
	public void editPage(int pageId, Session session) throws Exception {
		try (DBSessionClosure ses = new DBSessionClosure(session)) {
			PageResourceImpl pageRes = new PageResourceImpl();

			// load the page
			PageLoadResponse pageLoadResp = pageRes.load(Integer.toString(pageId), true, false, false, false, false, false, false, false, false, false, null, null);

			assertResponseOK(pageLoadResp);
			Page page = pageLoadResp.getPage();

			assertNotNull("Check loaded page", page);

			// modify the page
			page.setName(page.getName() + "|mod");

			// save the page
			PageSaveRequest pageSaveReq = new PageSaveRequest(page);

			pageSaveReq.setUnlock(true);
			GenericResponse pageSaveResp = pageRes.save(Integer.toString(pageId), pageSaveReq);

			assertResponseOK(pageSaveResp);
		}
	}

	/**
	 * Publish the given page
	 * 
	 * @param pageId
	 *            page id
	 * @param session
	 *            session
	 * @throws Exception
	 */
	public void publishPage(int pageId, Session session) throws Exception {
		try (DBSessionClosure ses = new DBSessionClosure(session)) {
			PageResourceImpl pageRes = new PageResourceImpl();

			PagePublishRequest pubReq = new PagePublishRequest();

			pubReq.setAlllang(false);
			pubReq.setAt(0);
			GenericResponse pubResp = pageRes.publish(Integer.toString(pageId), null, pubReq);

			assertResponseOK(pubResp);
		}
	}

	/**
	 * List the new (unread) messages of the user
	 * 
	 * @param user user
	 * @return list of messages
	 * @throws Exception
	 */
	public List<Message> listNewMessages(SystemUser user) throws Exception {
		try (DBSessionClosure ses = new DBSessionClosure(user.getId())) {
			MessagingResourceImpl msgRes = new MessagingResourceImpl();

			GenericResponse msgListResp = msgRes.list(true);

			assertResponseOK(msgListResp);
			return msgListResp.getMessages();
		}
	}

	/**
	 * Assert that the user has one new message
	 * @param user user
	 * @param expectedSender expected sender
	 * @param expectedMessage expected message
	 * @throws Exception
	 */
	protected void assertMessage(SystemUser user, String expectedSender, String expectedMessage) throws Exception {
		List<Message> msgList = listNewMessages(user);

		assertEquals("Check # of new messages", 1, msgList.size());

		Message message = msgList.get(0);

		assertEquals("Check message sender", expectedSender, message.getSender().getFirstName());
		assertEquals("Check message text", expectedMessage, message.getMessage());
	}
}
