package com.gentics.contentnode.tests.rest.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.rest.configuration.KeyProvider;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * Test cases for adding a JWT to forwarded calls
 */
public class CustomProxyJWTTest {
	public final static String RESOURCE_KEY = "myresource";

	public final static String RESOURCE_JWT_KEY = "myresource_jwt";

	public final static String RESOURCE_JWT_PREFIX_KEY = "myresource_jwt_prefix";

	/**
	 * Pattern for the auth header value
	 */
	public final static Pattern authHeaderPattern = Pattern.compile("Bearer (?<token>.+)");

	/**
	 * Custom app context
	 */
	private static RESTAppContext customAppContext = new RESTAppContext(new ResourceConfig(CustomToolResource.class)).baseUriPattern("http://localhost:%d/")
			.startPort(9080);

	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext().config(CustomProxyParametersTest.class.getResource("custom_proxy.yml").getFile()).config(prefs -> {
		// set the URL of the custom app
		prefs.set("custom_proxy." + RESOURCE_KEY + ".baseUrl", customAppContext.getBaseUri() + "tool/");
		prefs.set("custom_proxy." + RESOURCE_JWT_KEY + ".baseUrl", customAppContext.getBaseUri() + "tool/");
		prefs.set("custom_proxy." + RESOURCE_JWT_PREFIX_KEY + ".baseUrl", customAppContext.getBaseUri() + "tool/");
	});

	/**
	 * REST App context
	 */
	private static RESTAppContext restContext = new RESTAppContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(customAppContext).around(testContext).around(restContext);

	/**
	 * Login of the test user with permission
	 */
	private final static String LOGIN_WITH = "testwith";

	/**
	 * Password of the test users
	 */
	private final static String PASSWORD = "test";

	@BeforeClass
	public static void setUpOnce() throws NodeException {
		KeyProvider.init(testContext.getGcnBasePath().getAbsolutePath());
		// load group
		UserGroup nodeGroup = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});

		// create test user with permission
		Trx.supply(() -> {
			return Creator.createUser(LOGIN_WITH, PASSWORD, "First Tester", "Last Tester", "tester@nowhere", Arrays.asList(nodeGroup));
		});
	}

	/**
	 * Test no forwarding
	 * @throws RestException
	 */
	@Test
	public void testNoForward() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			String authHeader = client.get().base().path("proxy").path(RESOURCE_KEY).path("headers")
					.queryParam("return", "Authorization").request().get(String.class);
			assertThat(authHeader).as("Auth header").isEqualTo("null");
		}
	}

	/**
	 * Test forwarding without prefix
	 * @throws RestException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testForward() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			String authHeader = client.get().base().path("proxy").path(RESOURCE_JWT_KEY).path("headers")
					.queryParam("return", "Authorization").request().get(String.class);
			Matcher matcher = authHeaderPattern.matcher(authHeader);
			assertThat(matcher.matches()).as("Header Value matches pattern").isTrue();
			String token = matcher.group("token");
			JwtParser parser = KeyProvider.signedWith(Jwts.parserBuilder()).build();
			assertThat(parser.isSigned(token)).as("Token signed").isTrue();
			Jws<Claims> parsed = parser.parseClaimsJws(token);
			assertThat(parsed.getBody().getIssuer()).as("Issuer").isEqualTo("Gentics CMS");
			assertThat(parsed.getBody().getSubject()).as("subject").isEqualTo(LOGIN_WITH);
			assertThat(parsed.getBody().get("preferred_username", String.class)).as("Username").isEqualTo(LOGIN_WITH);
			assertThat(parsed.getBody().get("given_name", String.class)).as("First Name").isEqualTo("First Tester");
			assertThat(parsed.getBody().get("family_name", String.class)).as("Last Name").isEqualTo("Last Tester");
			assertThat(parsed.getBody().get("email", String.class)).as("Email").isEqualTo("tester@nowhere");
			assertThat(parsed.getBody().get("gcms_groups", List.class)).as("Groups").containsOnly("Node Super Admin");
		}
	}

	/**
	 * Test forwarding with prefix
	 * @throws RestException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testForwardPrefix() throws RestException, NodeException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			String authHeader = client.get().base().path("proxy").path(RESOURCE_JWT_PREFIX_KEY).path("headers")
					.queryParam("return", "Authorization").request().get(String.class);
			Matcher matcher = authHeaderPattern.matcher(authHeader);
			assertThat(matcher.matches()).as("Header Value matches pattern").isTrue();
			String token = matcher.group("token");
			JwtParser parser = KeyProvider.signedWith(Jwts.parserBuilder()).build();
			assertThat(parser.isSigned(token)).as("Token signed").isTrue();
			Jws<Claims> parsed = parser.parseClaimsJws(token);
			assertThat(parsed.getBody().getIssuer()).as("Issuer").isEqualTo("Gentics CMS");
			assertThat(parsed.getBody().getSubject()).as("subject").isEqualTo("prefix_" + LOGIN_WITH);
			assertThat(parsed.getBody().get("preferred_username", String.class)).as("Username").isEqualTo("prefix_" + LOGIN_WITH);
			assertThat(parsed.getBody().get("given_name", String.class)).as("First Name").isEqualTo("First Tester");
			assertThat(parsed.getBody().get("family_name", String.class)).as("Last Name").isEqualTo("Last Tester");
			assertThat(parsed.getBody().get("email", String.class)).as("Email").isEqualTo("tester@nowhere");
			assertThat(parsed.getBody().get("gcms_groups", List.class)).as("Groups").containsOnly("prefix_Node Super Admin");
		}
	}
}
