package com.gentics.contentnode.tests.message;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.Arrays;

import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.model.request.MultiPageAssignRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;

public class PageAssignMessagingSandboxTest extends AbstractMessagingSandboxTest {

	private static final Integer EDITOR_ID = 26;

	public static int PAGE_ID = 53;

	@Test
	public void testPubQueueAssign() throws Exception {
		Transaction t = testContext.startTransactionWithPermissions(true);
		SystemUser editor = t.getObject(SystemUser.class, EDITOR_ID);

		// Put the page in the publish queue by changing its status
		Page page = t.getObject(Page.class, PAGE_ID);
		page.queuePublish(editor);

		// Login test users
		String editorSID = testContext.getContext().login("editor", "editor");
		String publisherSID = testContext.getContext().login("publisher", "publisher");

		// Set the backend language to "en" for all users
		setBackendLanguage("en", publisherSID);
		setBackendLanguage("en", editorSID);

		// Invoke the assign call
		try (Trx trx = new Trx(publisherSID, 0)) {

			MultiPageAssignRequest request = new MultiPageAssignRequest();
			request.setMessage("Some Message");
			request.setUserIds(Arrays.asList(EDITOR_ID));
			request.setPageIds(Arrays.asList((page.getId().toString())));

			PageResourceImpl pageResource = new PageResourceImpl();
			pageResource.setSessionId(publisherSID);
			pageResource.initialize();
			GenericResponse response = pageResource.assign(request);
			assertResponseOK(response);
			trx.success();

		}

		// Check the result
		t = testContext.startTransactionWithPermissions(true);
		Page reloadedPage = t.getObject(Page.class, page.getId());
		assertThat(reloadedPage).isModified();
		assertMessage(editor, "Publisher", "The page ProcessTests/Page to translate (53) has been taken into revision.\n\n--\n\nSome Message");

	}

}
