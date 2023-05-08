package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.request.PagePublishRequest;
import com.gentics.contentnode.rest.model.response.LegacyPageListResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.resource.PageResource;
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for getting pages in the publish queue
 */
public class PublishQueueTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static UserGroup editorGroup;
	private static SystemUser editorUser;
	private static Folder folder;
	private static UserGroup publisherGroup;
	private static SystemUser publisherUser;
	private static Template template;
	private static Page publishedPage;
	private static Page queuedPage;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		editorGroup = Trx.supply(() -> createUserGroup("Editor", NODE_GROUP_ID));
		editorUser = Trx.supply(() -> createSystemUser("editor", "editor", null, "editor-test", "editor", Arrays.asList(editorGroup)));

		publisherGroup = Trx.supply(() -> createUserGroup("Publisher", NODE_GROUP_ID));
		publisherUser = Trx.supply(() -> createSystemUser("publisher", "publisher", null, "publisher-test", "publisher", Arrays.asList(publisherGroup)));

		node = Trx.supply(() -> createNode());
		folder = Trx.supply(() -> createFolder(node.getFolder(), "Testfolder"));

		Trx.operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(editorGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE).toString()));

		Trx.operate(() -> PermHandler.setPermissions(Folder.TYPE_FOLDER, folder.getId(), Arrays.asList(publisherGroup),
				new PermHandler.Permission(PermHandler.PERM_VIEW, PermHandler.PERM_PAGE_VIEW, PermHandler.PERM_PAGE_UPDATE, PermHandler.PERM_PAGE_PUBLISH)
						.toString()));

		template = Trx.supply(() -> createTemplate(folder, "Template"));

		// create a published page
		publishedPage = Trx.supply(() -> createPage(folder, template, "Published"));
		Trx.operate(() -> {
			PageResource res = new PageResourceImpl();
			res.publish(publishedPage.getId().toString(), null, new PagePublishRequest());
		});

		// create a page in the queue
		try (Trx trx = new Trx(null, editorUser.getId())) {
			queuedPage = createPage(folder, template, "In Queue");

			PageResource res = new PageResourceImpl();
			res.publish(queuedPage.getId().toString(), null, new PagePublishRequest());
		}

		// create an unpublished page
		Trx.supply(() -> createPage(folder, template, "Unpublished"));
	}

	/**
	 * Test getting the publish queue
	 * @throws NodeException
	 */
	@Test
	public void testQueue() throws NodeException {
		try (Trx trx = new Trx(null, publisherUser.getId())) {
			PageResourceImpl res = new PageResourceImpl();
			LegacyPageListResponse response = res.pubqueue(0, -1, null, null, null);
			assertThat(response.getResponseInfo().getResponseCode()).as("Response Code").isEqualTo(ResponseCode.OK);
			assertThat(response.getPages()).as("Returned pages").usingElementComparatorOnFields("id").containsOnly(ModelBuilder.getPage(queuedPage));
		}
	}

	/**
	 * Test getting the publish queue without publish permission
	 * @throws NodeException
	 */
	@Test
	public void testQueueNoPermission() throws NodeException {
		try (Trx trx = new Trx(null, editorUser.getId())) {
			PageResourceImpl res = new PageResourceImpl();
			LegacyPageListResponse response = res.pubqueue(0, -1, null, null, null);
			assertThat(response.getResponseInfo().getResponseCode()).as("Response Code").isEqualTo(ResponseCode.OK);
			assertThat(response.getPages()).as("Returned pages").isEmpty();
		}
	}
}
