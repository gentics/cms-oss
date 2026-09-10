package com.gentics.contentnode.tests.rest.page;

import static com.gentics.contentnode.factory.Trx.consume;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getPageResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createFolder;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

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
import com.gentics.contentnode.rest.resource.impl.PageResourceImpl;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.contentnode.tests.utils.Auth;
import com.gentics.contentnode.tests.utils.Auth.AuthType;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test cases for getting pages in the publish queue
 */
@RunWith(Parameterized.class)
public class PublishQueueTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();
	private static Node node;
	private static UserGroup editorGroup;
	private static SystemUser editorUser;
	private static Auth editorUserAuth;
	private static Folder folder;
	private static UserGroup publisherGroup;
	private static SystemUser publisherUser;
	private static Auth publisherUserAuth;
	private static Template template;
	private static Page publishedPage;
	private static Page queuedPage;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		editorGroup = Trx.supply(() -> createUserGroup("Editor", NODE_GROUP_ID));
		editorUser = Trx.supply(() -> createSystemUser("editor", "editor", null, "editor-test", "editor", Arrays.asList(editorGroup)));
		editorUserAuth = new Auth(editorUser);

		publisherGroup = Trx.supply(() -> createUserGroup("Publisher", NODE_GROUP_ID));
		publisherUser = Trx.supply(() -> createSystemUser("publisher", "publisher", null, "publisher-test", "publisher", Arrays.asList(publisherGroup)));
		publisherUserAuth = new Auth(publisherUser);

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
			getPageResource().publish(publishedPage.getId().toString(), null, new PagePublishRequest());
		});

		// create a page in the queue
		try (Trx trx = new Trx(null, editorUser.getId())) {
			queuedPage = createPage(folder, template, "In Queue");
			trx.success();
		}
		consume(Page::unlock, queuedPage);

		try (DBSessionClosure ses = new DBSessionClosure(editorUser.getId())) {
			getPageResource().publish(queuedPage.getId().toString(), null, new PagePublishRequest());
		}

		// create an unpublished page
		Trx.supply(() -> createPage(folder, template, "Unpublished"));
	}

	@Parameters(name = "{index}: auth: {0}")
	public static Collection<Object[]> data() {
		return Stream.of(AuthType.LOGIN, AuthType.TOKEN).map(type -> new Object[] {type}).collect(Collectors.toList());
	}

	@Parameter(0)
	public AuthType authType;

	/**
	 * Test getting the publish queue
	 * @throws NodeException
	 */
	@Test
	public void testQueue() throws NodeException {
		publisherUserAuth.withAuth(authType, () -> {
			PageResourceImpl res = new PageResourceImpl();
			LegacyPageListResponse response = res.pubqueue(0, -1, null, null, null);
			assertThat(response.getResponseInfo().getResponseCode()).as("Response Code").isEqualTo(ResponseCode.OK);
			assertThat(response.getPages()).as("Returned pages").usingElementComparatorOnFields("id").containsOnly(supply(() -> ModelBuilder.getPage(queuedPage)));
		});
	}

	/**
	 * Test getting the publish queue without publish permission
	 * @throws NodeException
	 */
	@Test
	public void testQueueNoPermission() throws NodeException {
		editorUserAuth.withAuth(authType, () -> {
			PageResourceImpl res = new PageResourceImpl();
			LegacyPageListResponse response = res.pubqueue(0, -1, null, null, null);
			assertThat(response.getResponseInfo().getResponseCode()).as("Response Code").isEqualTo(ResponseCode.OK);
			assertThat(response.getPages()).as("Returned pages").isEmpty();
		});
	}
}
