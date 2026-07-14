package com.gentics.contentnode.tests.message;

import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.testutils.DBTestContext.EDITOR_USER_ID;
import static com.gentics.contentnode.testutils.DBTestContext.PUBLISHER_USER_ID;
import static com.gentics.contentnode.testutils.DBTestContext.USER_WITH_PERMS;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.MultiPageAssignRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.testutils.DBSessionClosure;

public class PageAssignMessagingSandboxTest extends AbstractMessagingSandboxTest {

	public static int PAGE_ID = 53;

	@Test
	public void testPubQueueAssign() throws Exception {
		Page page;
		try (DBSessionClosure ses = new DBSessionClosure(USER_WITH_PERMS)) {
			try (Trx trx = new Trx(ses.getSession(), null)) {
				SystemUser editor = trx.getTransaction().getObject(SystemUser.class, EDITOR_USER_ID);

				// Put the page in the publish queue by changing its status
				page = trx.getTransaction().getObject(Page.class, PAGE_ID);
				page.queuePublish(editor);
				trx.success();
			}
		}

		// Set the backend language to "en" for all users
		try (DBSessionClosure ses = new DBSessionClosure(EDITOR_USER_ID)) {
			setBackendLanguage("en", ses.getSession());
		}
		try (DBSessionClosure ses = new DBSessionClosure(PUBLISHER_USER_ID)) {
			setBackendLanguage("en", ses.getSession());
		}

		// Invoke the assign call
		try (DBSessionClosure ses = new DBSessionClosure(PUBLISHER_USER_ID)) {

			MultiPageAssignRequest request = new MultiPageAssignRequest();
			request.setMessage("Some Message");
			request.setUserIds(Arrays.asList(EDITOR_USER_ID));
			request.setPageIds(Arrays.asList((page.getId().toString())));

			PageResourceImpl pageResource = new PageResourceImpl();
			GenericResponse response = pageResource.assign(request);
			assertResponseOK(response);
		}

		// Check the result
		operate(t -> {
			Page reloadedPage = t.getObject(Page.class, page.getId());
			assertThat(reloadedPage).isModified();
		});
		SystemUser editor = supply(t -> t.getObject(SystemUser.class, EDITOR_USER_ID));
		assertMessage(editor, "Publisher", "The page ProcessTests/Page to translate (53) has been taken into revision.\n\n--\n\nSome Message");
	}
}
