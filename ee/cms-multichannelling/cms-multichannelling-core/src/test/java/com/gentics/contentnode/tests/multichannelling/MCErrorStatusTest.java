package com.gentics.contentnode.tests.multichannelling;

import static com.gentics.contentnode.db.DBUtils.executeUpdate;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.factory.Trx.supply;
import static com.gentics.contentnode.tests.utils.Builder.update;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createChannel;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createConstruct;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createNode;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createPage;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createSystemUser;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplate;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createTemplateTag;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.createUserGroup;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.object.SystemUserFactory;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.ShortTextPartType;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.model.request.PageLocalizeRequest;
import com.gentics.contentnode.rest.model.request.UnlocalizeRequest;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Test cases for the HTTP status of failing multichannelling REST requests.
 * Errors must not be answered with status 200
 */
@GCNFeature(set = { Feature.MULTICHANNELLING, Feature.PARTIAL_MULTICHANNELLING })
public class MCErrorStatusTest {
	public static DBTestContext testContext = new DBTestContext();

	public static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private final static String TAGNAME = "testtag";

	private final static String NO_PERM_LOGIN = "noperm";

	private static Node node;

	private static Node channel;

	private static Page page;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();
		node = supply(() -> createNode());
		channel = supply(() -> createChannel(node, "Channel", "channel", "/"));
		operate(() -> node.activateFeature(Feature.PARTIAL_MULTICHANNELLING));

		int constructId = supply(() -> createConstruct(node, ShortTextPartType.class, "construct", "text"));
		Template template = supply(() -> createTemplate(node.getFolder(), "<node " + TAGNAME + ">", "Template",
				createTemplateTag(constructId, TAGNAME, true, false)));
		page = supply(() -> createPage(node.getFolder(), template, "Page"));

		// user without any permissions (the group initially gets the permissions of its mother group)
		UserGroup group = supply(() -> createUserGroup("No Permissions", NODE_GROUP_ID));
		operate(() -> executeUpdate("DELETE FROM perm WHERE usergroup_id = ?", new Object[] { group.getId() }));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(group.getId()));
		supply(() -> createSystemUser("No", "Permissions", null, NO_PERM_LOGIN, NO_PERM_LOGIN, Arrays.asList(group)));

		SystemUser systemUser = supply(t -> t.getObject(SystemUser.class, 1));
		update(systemUser, upd -> {
			upd.setPassword(SystemUserFactory.hashPassword("system", upd.getId()));
		}).build();
	}

	/**
	 * Test that localizing a page without permission responds with status 403
	 * @throws Exception
	 */
	@Test
	public void testLocalizeWithoutPermission() throws Exception {
		PageLocalizeRequest request = new PageLocalizeRequest();
		request.setChannelId(channel.getId());
		request.setForegroundTime(0);

		try (LoggedInClient client = restContext.client(NO_PERM_LOGIN, NO_PERM_LOGIN)) {
			Response response = client.get().base().path("page").path("localize").path(Integer.toString(page.getId())).request()
					.post(Entity.json(request));
			assertError(response, Status.FORBIDDEN, ResponseCode.PERMISSION);
		}
	}

	/**
	 * Test that getting the localizations of an unknown page responds with status 404
	 * @throws Exception
	 */
	@Test
	public void testLocalizationsOfUnknownPage() throws Exception {
		try (LoggedInClient client = restContext.client("system", "system")) {
			Response response = client.get().base().path("page").path("localizations").path("4711").request().get();
			assertError(response, Status.NOT_FOUND, ResponseCode.NOTFOUND);
		}
	}

	/**
	 * Test that localizing a tag in a page, which is not partially localized, responds with status 400
	 * @throws Exception
	 */
	@Test
	public void testLocalizeTagInNotPartiallyLocalizedPage() throws Exception {
		try (LoggedInClient client = restContext.client("system", "system")) {
			Response response = client.get().base().path("page").path("localize").path(Integer.toString(page.getId())).path("tags").path(TAGNAME)
					.request().post(Entity.json(""));
			assertError(response, Status.BAD_REQUEST, ResponseCode.INVALIDDATA);
		}
	}

	/**
	 * Test that unlocalizing an unknown tag (which fails in the background job) responds with status 404
	 * @throws Exception
	 */
	@Test
	public void testUnlocalizeUnknownTag() throws Exception {
		UnlocalizeRequest request = new UnlocalizeRequest();
		request.setChannelId(channel.getId());
		request.setForegroundTime(0);

		try (LoggedInClient client = restContext.client("system", "system")) {
			Response response = client.get().base().path("page").path("unlocalize").path(Integer.toString(page.getId())).path("tags")
					.path("unknowntag").request().post(Entity.json(request));
			assertError(response, Status.NOT_FOUND, ResponseCode.NOTFOUND);
		}
	}

	/**
	 * Assert that the response has the expected status and response code
	 * @param response response
	 * @param status expected status
	 * @param responseCode expected response code
	 */
	protected void assertError(Response response, Status status, ResponseCode responseCode) {
		assertThat(response.getStatusInfo().toEnum()).as("Response status").isEqualTo(status);
		GenericResponse genericResponse = response.readEntity(GenericResponse.class);
		assertThat(genericResponse.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(responseCode);
	}
}
