package com.gentics.contentnode.tests.rest.admin;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;
import static com.gentics.contentnode.tests.utils.Builder.create;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertError;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getAdminResource;
import static com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils.NODE_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.i18n.I18NHelper;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.perm.PermissionStore;
import com.gentics.contentnode.rest.exceptions.EntityNotFoundException;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationRequest;
import com.gentics.contentnode.rest.model.token.ApiTokenCreationResponse;
import com.gentics.contentnode.rest.model.token.ApiTokenDataModel;
import com.gentics.contentnode.rest.model.token.ApiTokenListResponse;
import com.gentics.contentnode.testutils.DBSessionClosure;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.Type;

/**
 * Test cases for the Api Token Management
 */
public class AdminResourceApiTokenManagementTest {
	/**
	 * Database context
	 */
	protected static DBTestContext testContext = new DBTestContext();

	/**
	 * REST App context
	 */
	protected static RESTAppContext restContext = new RESTAppContext(Type.jetty);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	private static UserGroup testGroup;

	private static SystemUser normalUser;

	private static SystemUser otherUser;

	private static SystemUser supportUser;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		testGroup = create(UserGroup.class, g -> {
			g.setMotherId(NODE_GROUP_ID);
			g.setName("Testgroup");
		}).build();

		// delete all permissions for the group
		operate(() -> update("DELETE FROM perm WHERE usergroup_id = ?", testGroup.getId()));
		operate(() -> PermissionStore.getInstance().refreshGroupLocal(testGroup.getId()));

		normalUser = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("normal.user");
			u.setPassword(RandomStringUtils.insecure().nextAlphabetic(10));
		}).build();

		otherUser = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("other.user");
			u.setPassword(RandomStringUtils.insecure().nextAlphabetic(10));
		}).build();

		supportUser = create(SystemUser.class, u -> {
			u.setActive(true);
			u.setLogin("support.user");
			u.setSupportUser(true);
		}).build();
	}

	@After
	public void tearDown() throws NodeException {
		operate(() -> update("DELETE FROM api_token"));
	}

	/**
	 * Test creating an Api Token for the normal user
	 * @throws NodeException 
	 */
	@Test
	public void testCreate() throws NodeException {
		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			ApiTokenCreationResponse response = assertSuccess(() -> getAdminResource().createAPIToken(new ApiTokenCreationRequest().setName("Test Token")),
					null);
			assertThat(response.getData())
				.as("Created Api Token")
				.hasName("Test Token")
				.hasUserId(normalUser.getId())
				.doesNotExpire()
				.isValid()
				.wasNeverUsed();
			assertThat(response.getToken()).as("Created token").isNotBlank();

			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").containsOnly(response.getData());
		}
	}

	/**
	 * Test creating an Api token without name
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithoutName() throws NodeException {
		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			String msg = I18NHelper.get("exception.missing.field", "name");
			assertError(() -> getAdminResource().createAPIToken(new ApiTokenCreationRequest()),
					RestMappedException.class, ResponseCode.INVALIDDATA, msg,
					new Message().setType(Message.Type.CRITICAL).setMessage(msg));

			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").isEmpty();
		}
	}

	/**
	 * Test creating an Api token with expiry date (in the future)
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithExpiryDate() throws NodeException {
		int expires = (int) Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();

		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			ApiTokenCreationResponse response = assertSuccess(() -> getAdminResource()
					.createAPIToken(new ApiTokenCreationRequest().setName("Test with expiry date").setExpires(expires)),
					null);
			assertThat(response.getData())
				.as("Created Api Token")
				.expiresAt(expires)
				.isValid();

			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").containsOnly(response.getData());
		}
	}

	/**
	 * Test creating an Api token with expiry date in the past
	 * @throws NodeException
	 */
	@Test
	public void testCreateWithExpiryDateInPast() throws NodeException {
		int expires = (int) Instant.now().minus(1, ChronoUnit.HOURS).getEpochSecond();

		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			String msg = I18NHelper.get("exception.expires.past", String.valueOf(expires));
			assertError(
					() -> getAdminResource().createAPIToken(new ApiTokenCreationRequest()
							.setName("Test with expiry date in the past").setExpires(expires)),
					RestMappedException.class, ResponseCode.INVALIDDATA, msg,
					new Message(Message.Type.CRITICAL, msg));

			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").isEmpty();
		}
	}

	/**
	 * Test creating an Api token for a support user
	 * @throws NodeException
	 */
	@Test
	public void testCreateForSupportUser() throws NodeException {
		try (DBSessionClosure ses = new DBSessionClosure(supportUser)) {
			String msg = I18NHelper.get("exception.apitoken.supportuser");
			assertError(
					() -> getAdminResource()
							.createAPIToken(new ApiTokenCreationRequest().setName("Test for support user")),
					RestMappedException.class, ResponseCode.INVALIDDATA, msg, new Message(Message.Type.CRITICAL, msg));

			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").isEmpty();
		}
	}

	/**
	 * Test deleting an Api token
	 * @throws NodeException
	 */
	@Test
	public void testDelete() throws NodeException {
		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			ApiTokenCreationResponse response = assertSuccess(() -> getAdminResource().createAPIToken(new ApiTokenCreationRequest().setName("Test Token")),
					null);

			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").containsOnly(response.getData());

			assertSuccess(() -> getAdminResource().deleteAPIToken(response.getData().getId()), null);

			listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null),
					null);
			assertThat(listResponse.getItems()).as("Api Tokens for user").isEmpty();
		}
	}

	/**
	 * Test deleting an inexistent Api token
	 * @throws NodeException
	 */
	@Test
	public void testDeleteInexistent() throws NodeException {
		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			int tokenId = 4711;
			String msg = I18NHelper.get("apitoken.notfound", String.valueOf(tokenId));
			assertError(() -> getAdminResource().deleteAPIToken(tokenId), EntityNotFoundException.class, ResponseCode.NOTFOUND,
					msg, new Message(Message.Type.CRITICAL, msg));
		}
	}

	/**
	 * Test deleting an Api Token of another user
	 * @throws NodeException
	 */
	@Test
	public void testDeleteForeign() throws NodeException {
		AtomicInteger tokenId = new AtomicInteger();
		ApiTokenDataModel foreign;
		try (DBSessionClosure ses = new DBSessionClosure(otherUser)) {
			ApiTokenCreationResponse response = assertSuccess(() -> getAdminResource().createAPIToken(new ApiTokenCreationRequest().setName("Test Token")),
					null);
			tokenId.set(response.getData().getId());
			foreign = response.getData();
		}

		try (DBSessionClosure ses = new DBSessionClosure(normalUser)) {
			String msg = I18NHelper.get("apitoken.notfound", String.valueOf(tokenId.get()));
			assertError(() -> getAdminResource().deleteAPIToken(tokenId.get()), EntityNotFoundException.class, ResponseCode.NOTFOUND,
					msg, new Message(Message.Type.CRITICAL, msg));
		}

		try (DBSessionClosure ses = new DBSessionClosure(otherUser)) {
			ApiTokenListResponse listResponse = assertSuccess(() -> getAdminResource().listAPITokens(null, null, null), null);
			assertThat(listResponse.getItems()).as("Api Tokens for other user").containsOnly(foreign);
		}
	}
}
