package com.gentics.contentnode.mcp.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

/**
 * Unit tests for {@link McpAuthenticator}.
 *
 * <p>
 * Only covers the credential-shape-independent short circuit (empty credentials never even
 * attempt a DB lookup, see {@link McpAuthenticator#resolve}). Resolving an actual API token or
 * session secret against the database is covered by an integration test using
 * {@code DBTestContext} instead (see {@code docs/mcp-tests.md}), since it needs a real CMS
 * instance/DB, not a unit test fixture.
 * </p>
 */
public class McpAuthenticatorTest {
	@Test
	public void testEmptyCredentialsResolveToEmptySessionWithoutDbAccess() {
		Optional<?> result = McpAuthenticator.resolve(McpRequestCredentials.EMPTY);

		assertThat(result).isEmpty();
	}
}
