package com.gentics.contentnode.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.contentnode.factory.SessionToken;
import com.jayway.jsonpath.JsonPath;

/**
 * Test cases for the security schemes in the generated OpenAPI specification,
 * which must match the authentication supported by the REST API
 * (API Token as bearer token or session secret cookie)
 */
public class OpenAPISecurityTest {
	/**
	 * Generated OpenAPI specification
	 */
	private static Object spec;

	@BeforeClass
	public static void setupOnce() throws IOException {
		try (InputStream in = OpenAPISecurityTest.class.getResourceAsStream("/webroot/openapi/openapi.json")) {
			assertThat(in).as("Generated OpenAPI specification").isNotNull();
			spec = JsonPath.parse(in).json();
		}
	}

	/**
	 * Test that only the supported security schemes are defined
	 */
	@Test
	public void testSecuritySchemes() {
		Map<String, Object> schemes = JsonPath.read(spec, "$.components.securitySchemes");
		assertThat(schemes).as("Security schemes").containsOnlyKeys("bearerAuth", "sessionSecret");
	}

	/**
	 * Test the security scheme for API Tokens
	 */
	@Test
	public void testBearerAuth() {
		Map<String, Object> scheme = JsonPath.read(spec, "$.components.securitySchemes.bearerAuth");
		assertThat(scheme).as("Bearer auth scheme").containsEntry("type", "http").containsEntry("scheme", "bearer");
	}

	/**
	 * Test the security scheme for the session secret cookie
	 */
	@Test
	public void testSessionSecret() {
		Map<String, Object> scheme = JsonPath.read(spec, "$.components.securitySchemes.sessionSecret");
		assertThat(scheme).as("Session secret scheme").containsEntry("type", "apiKey").containsEntry("in", "cookie")
				.containsEntry("name", SessionToken.SESSION_SECRET_COOKIE_NAME);
	}

	/**
	 * Test that the global security requirements allow either of the security schemes
	 */
	@Test
	public void testSecurityRequirements() {
		List<Map<String, Object>> security = JsonPath.read(spec, "$.security");
		assertThat(security).as("Security requirements").containsExactlyInAnyOrder(Map.of("bearerAuth", List.of()),
				Map.of("sessionSecret", List.of()));
	}
}
