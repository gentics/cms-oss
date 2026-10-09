package com.gentics.contentnode.mcp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.gentics.contentnode.runtime.ConfigurationValue;

import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;

/**
 * Unit tests for {@link CmsMcpSecurityValidator}.
 */
public class CmsMcpSecurityValidatorTest {
	private final CmsMcpSecurityValidator validator = new CmsMcpSecurityValidator();

	@After
	public void clearRequireAuthFlag() {
		System.clearProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName());
	}

	@Test
	public void testDisabledAllowsRequestWithoutAnyCredential() {
		// explicitly disabled, rather than relying on the (equivalent, but otherwise DB-config-
		// dependent, see ConfigurationValue#get) default - this is also the realistic case, since
		// a deployment leaves the flag at its documented default of "false" rather than unset
		System.setProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName(), "false");

		assertThatCode(() -> validator.validateHeaders(Map.of())).doesNotThrowAnyException();
	}

	@Test
	public void testEnabledRejectsRequestWithoutAnyCredential() {
		System.setProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName(), "true");

		assertThatThrownBy(() -> validator.validateHeaders(Map.of()))
				.isInstanceOf(ServerTransportSecurityException.class)
				.satisfies(e -> assertThat(((ServerTransportSecurityException) e).getStatusCode()).isEqualTo(401));
	}

	@Test
	public void testEnabledAllowsRequestWithBearerHeader() {
		System.setProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName(), "true");

		assertThatCode(() -> validator
				.validateHeaders(Map.of("Authorization", List.of("Bearer some-token")))).doesNotThrowAnyException();
	}

	@Test
	public void testEnabledAllowsRequestWithSessionCookie() {
		System.setProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName(), "true");

		assertThatCode(() -> validator.validateHeaders(
				Map.of("Cookie", List.of("other=x; GCN_SESSION_SECRET=abc123")))).doesNotThrowAnyException();
	}

	@Test
	public void testEnabledRejectsIrrelevantHeaders() {
		System.setProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName(), "true");

		assertThatThrownBy(() -> validator.validateHeaders(Map.of("Cookie", List.of("other=x"))))
				.isInstanceOf(ServerTransportSecurityException.class);
	}

	@Test
	public void testHeaderLookupIsCaseInsensitive() {
		System.setProperty(ConfigurationValue.MCP_REQUIRE_AUTH.getSystemPropertyName(), "true");

		assertThatCode(() -> validator
				.validateHeaders(Map.of("authorization", List.of("Bearer some-token")))).doesNotThrowAnyException();
	}
}
