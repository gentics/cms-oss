package com.gentics.contentnode.mcp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.gentics.contentnode.factory.SessionToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import io.modelcontextprotocol.common.McpTransportContext;

/**
 * Unit tests for {@link CmsMcpContextExtractor}.
 */
public class CmsMcpContextExtractorTest {
	private final CmsMcpContextExtractor extractor = new CmsMcpContextExtractor();

	private McpRequestCredentials extract(HttpServletRequest request) {
		McpTransportContext context = extractor.extract(request);
		Object value = context.get(McpRequestCredentials.CONTEXT_KEY);
		assertThat(value).isInstanceOf(McpRequestCredentials.class);
		return (McpRequestCredentials) value;
	}

	@Test
	public void testExtractsBearerToken() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Bearer my-token-123");
		when(request.getCookies()).thenReturn(null);

		McpRequestCredentials credentials = extract(request);

		assertThat(credentials.apiToken()).contains("my-token-123");
		assertThat(credentials.sessionSecret()).isEmpty();
	}

	@Test
	public void testExtractsSessionCookie() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn(null);
		when(request.getCookies()).thenReturn(
				new Cookie[] { new Cookie("other", "x"), new Cookie(SessionToken.SESSION_SECRET_COOKIE_NAME, "secret-abc") });

		McpRequestCredentials credentials = extract(request);

		assertThat(credentials.apiToken()).isEmpty();
		assertThat(credentials.sessionSecret()).contains("secret-abc");
	}

	@Test
	public void testNoCredentialsPresent() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn(null);
		when(request.getCookies()).thenReturn(null);

		McpRequestCredentials credentials = extract(request);

		assertThat(credentials.isEmpty()).isTrue();
	}

	@Test
	public void testNonBearerAuthorizationHeaderIsIgnored() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
		when(request.getCookies()).thenReturn(null);

		McpRequestCredentials credentials = extract(request);

		assertThat(credentials.apiToken()).isEmpty();
	}

	@Test
	public void testCookieArrayWithoutSessionCookieIsIgnored() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn(null);
		when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("other", "x") });

		McpRequestCredentials credentials = extract(request);

		assertThat(credentials.sessionSecret()).isEmpty();
	}

	@Test
	public void testBothCredentialsCanBePresentAtOnce() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Bearer my-token-123");
		when(request.getCookies())
				.thenReturn(new Cookie[] { new Cookie(SessionToken.SESSION_SECRET_COOKIE_NAME, "secret-abc") });

		McpRequestCredentials credentials = extract(request);

		assertThat(credentials.apiToken()).contains("my-token-123");
		assertThat(credentials.sessionSecret()).contains("secret-abc");
	}
}
