package com.gentics.contentnode.rest.resource.impl.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Test cases for removing the session secret cookie from forwarded Cookie headers
 */
public class ProxyResourceCookieTest {
	@Test
	public void testOnlySessionSecret() {
		assertThat(ProxyResource.removeSessionSecretCookie("GCN_SESSION_SECRET=secret")).isEmpty();
	}

	@Test
	public void testNoSessionSecret() {
		assertThat(ProxyResource.removeSessionSecretCookie("first=1; second=2")).isEqualTo("first=1; second=2");
	}

	@Test
	public void testSessionSecretBetweenOtherCookies() {
		assertThat(ProxyResource.removeSessionSecretCookie("first=1; GCN_SESSION_SECRET=secret; second=2")).isEqualTo("first=1; second=2");
	}

	@Test
	public void testWithoutSpaces() {
		assertThat(ProxyResource.removeSessionSecretCookie("GCN_SESSION_SECRET=secret;first=1")).isEqualTo("first=1");
	}

	@Test
	public void testSimilarNameIsKept() {
		assertThat(ProxyResource.removeSessionSecretCookie("GCN_SESSION_SECRET_OTHER=1; X_GCN_SESSION_SECRET=2"))
				.isEqualTo("GCN_SESSION_SECRET_OTHER=1; X_GCN_SESSION_SECRET=2");
	}
}
