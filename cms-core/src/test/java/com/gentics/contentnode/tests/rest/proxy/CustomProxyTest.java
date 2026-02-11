package com.gentics.contentnode.tests.rest.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

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
import com.gentics.contentnode.rest.client.RestClient;
import com.gentics.contentnode.rest.client.JerseyRestClientImpl;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

import fi.iki.santtu.md5.MD5;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;

/**
 * Test cases for custom proxy
 */
public class CustomProxyTest {
	public final static String RESOURCE_KEY = "myresource";

	public final static String UNKNOWN_RESOURCE_KEY = "unknown";

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
	});;

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
	 * Login of the test user without permission
	 */
	private final static String LOGIN_WITHOUT = "testwithout";

	/**
	 * Password of the test users
	 */
	private final static String PASSWORD = "test";

	@BeforeClass
	public static void setUpOnce() throws NodeException {
		// load group
		UserGroup nodeGroup = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			List<UserGroup> groups = t.getObjects(UserGroup.class, Arrays.asList(2));
			assertThat(groups).as("User Groups").hasSize(1);
			return groups.get(0);
		});

		// create test user with permission
		Trx.supply(() -> {
			return Creator.createUser(LOGIN_WITH, PASSWORD, "Tester", "Tester", "", Arrays.asList(nodeGroup));
		});

		// create subgroup
		UserGroup subGroup = Trx.supply(() -> Creator.createUsergroup("Subgroup", "", nodeGroup));

		// create test user without permission
		Trx.supply(() -> Creator.createUser(LOGIN_WITHOUT, PASSWORD, "Tester", "Tester", "", Arrays.asList(subGroup)));
	}

	/**
	 * Test accessing an unconfigured resource
	 * @throws RestException
	 */
	@Test(expected = NotFoundException.class)
	public void testUnconfigured() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			client.get().base().path("proxy").path(UNKNOWN_RESOURCE_KEY).path("hello").request().get(String.class);
		}
	}

	/**
	 * Test accessing a proxied resource without login
	 * @throws RestException
	 */
	@Test(expected = NotAuthorizedException.class)
	public void testNoLogin() throws RestException {
		RestClient client = new JerseyRestClientImpl(restContext.getBaseUri());
		// set invalid sid, so that the RestClient allows to make the request
		client.setSid("bla");
		client.base().path("proxy").path(RESOURCE_KEY).path("hello").request().get(String.class);
	}

	/**
	 * Test access with a user having no permission
	 * @throws RestException
	 */
	@Test(expected = ForbiddenException.class)
	public void testNoPermission() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITHOUT, PASSWORD)) {
			client.get().base().path("proxy").path(RESOURCE_KEY).path("hello").request().get(String.class);
		}
	}

	/**
	 * Test normal access
	 * @throws RestException
	 */
	@Test
	public void testAccess() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("hello").request().get(String.class)).as("Response").isEqualTo("Hello World!");
		}
	}

	/**
	 * Test forbidden method
	 * @throws RestException
	 */
	@Test(expected = ForbiddenException.class)
	public void testForbiddenMethod() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			client.get().base().path("proxy").path(RESOURCE_KEY).path("hello").request().put(Entity.text(""), String.class);
		}
	}

	/**
	 * Test whether configured header is added
	 * @throws RestException
	 */
	@Test
	public void testAddHeader() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("headers").queryParam("return", "Header-Name").request().get(String.class))
					.as("Sent Headers").isEqualTo("Header Value");
		}
	}

	/**
	 * Test whether header is forwarded
	 * @throws RestException
	 */
	@Test
	public void testForwardHeader() throws RestException {
		String addedHeaderName = "Added-Header";
		String addedHeaderValue = "Added Header Value";
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("headers").queryParam("return", addedHeaderName).request()
					.header(addedHeaderName, addedHeaderValue).get(String.class)).as("Sent Headers").isEqualTo(addedHeaderValue);
		}
	}

	/**
	 * Test whether query parameters are forwarded
	 * @throws RestException
	 */
	@Test
	public void testForwardQueryParams() throws RestException {
		String addedQueryParamName = "query";
		String addedQueryParamValue = "something";
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("queryParams").queryParam(addedQueryParamName, addedQueryParamValue).request()
					.get(String.class)).as("Sent query parameters").isEqualTo(String.format("%s=%s", addedQueryParamName, Arrays.asList(addedQueryParamValue)));
		}
	}

	/**
	 * Test whether query parameters with special characters are forwarded
	 * @throws RestException
	 */
	@Test
	public void testForwardEncodedQueryParams() throws RestException {
		String addedQueryParamName = "äöüß";
		String addedQueryParamValue = "äöüß";
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("queryParams").queryParam(addedQueryParamName, addedQueryParamValue).request()
					.get(String.class)).as("Sent query parameters").isEqualTo(String.format("%s=%s", addedQueryParamName, Arrays.asList(addedQueryParamValue)));
		}
	}

	/**
	 * Test whether multivalue query parameters are forwarded correctly
	 * @throws RestException
	 */
	@Test
	public void testForwardMultivalueQueryParams() throws RestException {
		String addedQueryParamName = "multi";
		String addedQueryParamValue1 = "one";
		String addedQueryParamValue2 = "two";
		String addedQueryParamValue3 = "three";
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("queryParams")
					.queryParam(addedQueryParamName, addedQueryParamValue1)
					.queryParam(addedQueryParamName, addedQueryParamValue2)
					.queryParam(addedQueryParamName, addedQueryParamValue3).request().get(String.class))
							.as("Sent query parameters").isEqualTo(
									String.format("%s=%s", addedQueryParamName, Arrays.asList(addedQueryParamValue1, addedQueryParamValue2, addedQueryParamValue3)));
		}
	}

	/**
	 * Test posting data
	 * @throws RestException
	 */
	@Test
	public void testPostData() throws RestException {
		String postedData = "This is the posted data";
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("post").request().post(Entity.text(postedData), String.class))
					.as("Response to post").isEqualTo(MD5.asHex(postedData.getBytes()));
		}
	}

	/**
	 * Test calling the proxy without extra path
	 * @throws RestException
	 */
	@Test
	public void testNoExtraPath() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).request().get(String.class))
					.as("Response to call").isEqualTo("Root Path");
		}
	}

	/**
	 * Test calling the proxy without extra path, but ending with a slash
	 * @throws RestException
	 */
	@Test
	public void testNoExtraPathEndSlash() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("/").request().get(String.class))
					.as("Response to call").isEqualTo("Root Path");
		}
	}

	/**
	 * Test calling the proxy with encoded non-ASCII characters in the path
	 * @throws RestException
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testEncodedPath() throws RestException, UnsupportedEncodingException {
		String segment = "äöüß";
		try (LoggedInClient client = restContext.client(LOGIN_WITH, PASSWORD)) {
			assertThat(client.get().base().path("proxy").path(RESOURCE_KEY).path("path").path(segment).path(segment).request()
					.get(String.class)).as("Requested path").isEqualTo(segment + "/" + segment);
		}
	}
}
