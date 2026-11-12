package com.gentics.contentnode.tests.rest.group;

import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertResponse;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.assertSuccess;
import static com.gentics.contentnode.tests.utils.ContentNodeRESTUtils.getAuthResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.auth.PasswordTokens;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.exception.RestMappedException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.User;
import com.gentics.contentnode.rest.model.request.LoginRequest;
import com.gentics.contentnode.rest.model.request.SetPasswordRequest;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.UserLoadResponse;
import com.gentics.contentnode.rest.resource.impl.GroupResourceImpl;
import com.gentics.contentnode.rest.resource.impl.UserResourceImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

/**
 * Test cases for creating a user with a link for setting the password
 */
public class PasswordLinkTest extends AbstractGroupEditTest {
	/**
	 * Password satisfying the password rule
	 */
	private static final String PASSWORD = "secret.Pw-1";

	private SystemUser systemUser;

	private Group group;

	@BeforeClass
	public static void setupKeys() throws NodeException {
		KeyProvider.init(context.getGcnBasePath().getAbsolutePath());
	}

	@Before
	public void setup() throws NodeException {
		systemUser = Trx.supply(t -> t.getObject(SystemUser.class, 1));
		group = Trx.supply(systemUser, () -> new GroupResourceImpl()
				.add(String.valueOf(NODE_GROUP), new Group().setName("Password Link Group")).getGroup());
	}

	/**
	 * Test that the user created with a password link can set the password once and then log in with it
	 * @throws NodeException
	 */
	@Test
	public void testSetPasswordAndLogin() throws NodeException {
		UserLoadResponse created = createUser("linkuser", true);
		String token = created.getPasswordToken();
		assertThat(token).as("Password token").isNotBlank();

		assertSuccess(() -> getAuthResource().validatePasswordToken(token), null);
		assertSuccess(() -> getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword(PASSWORD)),
				null);

		LoginRequest login = new LoginRequest();
		login.setLogin("linkuser");
		login.setPassword(PASSWORD);
		assertSuccess(() -> getAuthResource().login(login), null);
		assertThat(storedJti(created.getUser().getId())).as("Stored token ID after use").isNull();
	}

	/**
	 * Test that a token cannot be used a second time
	 * @throws NodeException
	 */
	@Test
	public void testTokenUsableOnce() throws NodeException {
		String token = createUser("linkuser", true).getPasswordToken();
		assertSuccess(() -> getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword(PASSWORD)),
				null);

		assertInvalid(token);
		assertResponse(getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword("other.pw")),
				ResponseCode.FAILURE);
	}

	/**
	 * Test that creating a user without the option creates no token
	 * @throws NodeException
	 */
	@Test
	public void testNoTokenWithoutOption() throws NodeException {
		UserLoadResponse created = createUser("plainuser", false);
		assertThat(created.getPasswordToken()).as("Password token").isNull();
		assertThat(storedJti(created.getUser().getId())).as("Stored token ID").isNull();
	}

	/**
	 * Test that a password and the option together are rejected
	 * @throws NodeException
	 */
	@Test
	public void testPasswordTogetherWithLinkRejected() throws NodeException {
		assertThatThrownBy(() -> Trx.supply(systemUser, () -> new GroupResourceImpl().createUser(
				String.valueOf(group.getId()), user("bothuser").setPassword(PASSWORD), true)))
				.isInstanceOf(RestMappedException.class);
	}

	/**
	 * Test that a new token replaces the old one
	 * @throws NodeException
	 */
	@Test
	public void testNewTokenReplacesOld() throws NodeException {
		UserLoadResponse created = createUser("linkuser", true);
		String oldToken = created.getPasswordToken();
		String newToken = Trx.supply(() -> PasswordTokens.create(created.getUser().getId()));

		assertInvalid(oldToken);
		assertSuccess(() -> getAuthResource().validatePasswordToken(newToken), null);
	}

	/**
	 * Test that an expired token is rejected, although it is still the stored one
	 * @throws NodeException
	 */
	@Test
	public void testExpiredToken() throws NodeException {
		Claims claims = claims(createUser("linkuser", true).getPasswordToken());
		String expired = sign(Jwts.builder().setSubject(claims.getSubject()).setId(claims.getId())
				.setExpiration(new Date(System.currentTimeMillis() - 60_000)).claim("purpose", "set-password"));

		assertInvalid(expired);
	}

	/**
	 * Test that a token without expiry is rejected, like the JWTs for custom proxies, which are signed with the same key
	 * @throws NodeException
	 */
	@Test
	public void testTokenWithoutExpiry() throws NodeException {
		Claims claims = claims(createUser("linkuser", true).getPasswordToken());
		String noExpiry = sign(Jwts.builder().setSubject(claims.getSubject()).setId(claims.getId())
				.claim("purpose", "set-password"));

		assertInvalid(noExpiry);
	}

	/**
	 * Test that a token without the purpose is rejected
	 * @throws NodeException
	 */
	@Test
	public void testTokenWithoutPurpose() throws NodeException {
		Claims claims = claims(createUser("linkuser", true).getPasswordToken());
		String noPurpose = sign(Jwts.builder().setSubject(claims.getSubject()).setId(claims.getId())
				.setExpiration(claims.getExpiration()));

		assertInvalid(noPurpose);
	}

	/**
	 * Test that a token with a changed payload is rejected
	 * @throws NodeException
	 */
	@Test
	public void testTamperedToken() throws NodeException {
		UserLoadResponse first = createUser("linkuser", true);
		UserLoadResponse second = createUser("otheruser", true);
		String[] parts = first.getPasswordToken().split("\\.");
		Claims claims = claims(first.getPasswordToken());

		// same token ID and signature, but for the other user
		String payload = "{\"sub\":\"%d\",\"jti\":\"%s\",\"exp\":%d,\"purpose\":\"set-password\"}".formatted(
				second.getUser().getId(), claims.getId(), claims.getExpiration().getTime() / 1000);
		String tampered = parts[0] + "."
				+ Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "."
				+ parts[2];

		assertInvalid(tampered);
	}

	/**
	 * Test that missing and malformed tokens are rejected
	 * @throws NodeException
	 */
	@Test
	public void testMalformedToken() throws NodeException {
		assertInvalid(null);
		assertInvalid("");
		assertInvalid("not-a-token");
	}

	/**
	 * Test that a password violating the rule is rejected, and the token stays usable
	 * @throws NodeException
	 */
	@Test
	public void testInvalidPasswordRejected() throws NodeException {
		String token = createUser("linkuser", true).getPasswordToken();

		assertResponse(getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword("abc")),
				ResponseCode.INVALIDDATA);
		assertResponse(getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword("with space")),
				ResponseCode.INVALIDDATA);
		assertResponse(getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword("a".repeat(21))),
				ResponseCode.INVALIDDATA);

		assertSuccess(() -> getAuthResource().validatePasswordToken(token), null);
	}

	/**
	 * Test that an admin setting the password revokes the link
	 * @throws NodeException
	 */
	@Test
	public void testAdminSettingPasswordRevokesToken() throws NodeException {
		UserLoadResponse created = createUser("linkuser", true);
		Trx.operate(systemUser, () -> new UserResourceImpl().update(String.valueOf(created.getUser().getId()),
				new User().setPassword(PASSWORD)));

		assertInvalid(created.getPasswordToken());
	}

	/**
	 * Test that the token of a deactivated user is rejected
	 * @throws Exception
	 */
	@Test
	public void testDeactivatedUser() throws Exception {
		UserLoadResponse created = createUser("linkuser", true);
		Trx.operate(systemUser, () -> {
			try {
				new UserResourceImpl().deactivate(String.valueOf(created.getUser().getId()));
			} catch (NodeException e) {
				throw e;
			} catch (Exception e) {
				throw new NodeException(e);
			}
		});

		assertInvalid(created.getPasswordToken());
	}

	/**
	 * Create a user in the test group
	 * @param login login
	 * @param passwordLink whether to create a password link
	 * @return response
	 * @throws NodeException
	 */
	private UserLoadResponse createUser(String login, boolean passwordLink) throws NodeException {
		return Trx.supply(systemUser,
				() -> new GroupResourceImpl().createUser(String.valueOf(group.getId()), user(login), passwordLink));
	}

	/**
	 * REST user with login and names
	 * @param login login
	 * @return user
	 */
	private static User user(String login) {
		return new User().setLogin(login).setFirstName("First").setLastName("Last");
	}

	/**
	 * Assert that the token is rejected when validated and when used
	 * @param token token
	 * @throws NodeException
	 */
	private static void assertInvalid(String token) throws NodeException {
		assertResponse(getAuthResource().validatePasswordToken(token), ResponseCode.FAILURE);
		assertResponse(getAuthResource().setPassword(new SetPasswordRequest().setToken(token).setPassword(PASSWORD)),
				ResponseCode.FAILURE);
	}

	/**
	 * Get the stored token ID of the user
	 * @param userId user ID
	 * @return token ID or null
	 * @throws NodeException
	 */
	private static String storedJti(int userId) throws NodeException {
		return Trx.supply(() -> DBUtils.select("SELECT password_token FROM systemuser WHERE id = ?",
				st -> st.setInt(1, userId), rs -> rs.next() ? rs.getString("password_token") : null));
	}

	/**
	 * Parse the claims of a valid token
	 * @param token token
	 * @return claims
	 * @throws NodeException
	 */
	private static Claims claims(String token) throws NodeException {
		return KeyProvider.signedWith(Jwts.parserBuilder()).build().parseClaimsJws(token).getBody();
	}

	/**
	 * Sign the JWT with the CMS key
	 * @param jwt builder
	 * @return token
	 * @throws NodeException
	 */
	private static String sign(JwtBuilder jwt) throws NodeException {
		return KeyProvider.sign(jwt).compact();
	}
}
