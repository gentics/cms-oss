package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link UrlSources}, fetching from a local HTTP server
 */
public class UrlSourcesTest {
	private static final Instant NOW = Instant.ofEpochSecond(1_800_000_000L);

	private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(Redirect.NEVER)
			.connectTimeout(Duration.ofSeconds(5)).build();

	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

	private static HttpServer server;

	private static String base;

	@BeforeClass
	public static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/file", exchange -> {
			byte[] body = "hello".getBytes();
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		server.createContext("/chunked", exchange -> {
			// length 0 means chunked, so there is no Content-Length
			exchange.sendResponseHeaders(200, 0);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(new byte[100]);
			}
		});
		server.createContext("/empty", exchange -> {
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", base + "/file");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/slow", exchange -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.setExecutor(EXECUTOR);
		server.start();
		base = "http://127.0.0.1:%d".formatted(server.getAddress().getPort());
	}

	@AfterClass
	public static void stopServer() {
		server.stop(0);
		EXECUTOR.shutdownNow();
	}

	@Test
	public void testAllowedHosts() {
		assertThat(UrlSources.allowedHosts(" Files.Example.org, localhost:8080 ,,")).containsExactlyInAnyOrder(
				"files.example.org", "localhost:8080");
		assertThat(UrlSources.allowedHosts("")).isEmpty();
		assertThat(UrlSources.allowedHosts(null)).isEmpty();
	}

	@Test
	public void testUri() {
		Set<String> hosts = Set.of("files.example.org", "genaix.example.org:8443", "localhost:8000");

		assertThat(uri("https://files.example.org/a.png", hosts)).hasHost("files.example.org");
		assertThat(uri("https://FILES.example.org:443/a.png", hosts)).hasHost("FILES.example.org");
		assertThat(uri("https://genaix.example.org:8443/api/v1/file-links/t", hosts)).hasPort(8443);
		assertThat(uri("http://localhost:8000/a.png", hosts)).hasPort(8000);
		assertThat(UrlSources.uri(Map.of("kind", "url", "url", "https://files.example.org/a", "expiresAt",
				NOW.getEpochSecond() + 300), hosts, NOW)).hasPath("/a");
	}

	@Test
	public void testUriRefused() {
		Set<String> hosts = Set.of("files.example.org", "localhost:8000");

		assertRefused(() -> uri("https://files.example.org/a.png", Set.of()), "not on the allowlist");
		assertRefused(() -> uri("https://other.example.org/a.png", hosts), "other.example.org:443");
		assertRefused(() -> uri("https://files.example.org:8443/a.png", hosts), "not on the allowlist");
		assertRefused(() -> uri("http://files.example.org/a.png", hosts), "only https");
		assertRefused(() -> uri("ftp://files.example.org/a.png", hosts), "only https");
		assertRefused(() -> uri("http://localhost:9000/a.png", hosts), "not on the allowlist");
		assertRefused(() -> uri("https://user@files.example.org/a.png", hosts), "absolute URL");
		assertRefused(() -> uri("/a.png", hosts), "absolute URL");
		assertRefused(() -> uri("https://files.example.org/a b", hosts), "not a valid URL");
		assertRefused(() -> UrlSources.uri(Map.of("kind", "url"), hosts, NOW), "'url' must be");
		assertRefused(() -> UrlSources.uri(Map.of("kind", "url", "url", "https://files.example.org/a", "expiresAt",
				NOW.getEpochSecond() - 1), hosts, NOW), "expired");
		assertRefused(() -> UrlSources.uri(Map.of("kind", "url", "url", "https://files.example.org/a", "expiresAt",
				"soon"), hosts, NOW), "'expiresAt'");
	}

	@Test
	public void testFetch() throws Exception {
		assertThat(fetch("/file", 100)).isEqualTo("hello".getBytes());
		assertThat(fetch("/chunked", 100)).hasSize(100);
	}

	@Test
	public void testFetchRefused() {
		assertRefused(() -> fetch("/file", 4), "the file has 5 bytes");
		assertRefused(() -> fetch("/chunked", 99), "more than 99 bytes");
		assertRefused(() -> fetch("/missing", 100), "status 404");
		assertRefused(() -> fetch("/redirect", 100), "status 302");
		assertRefused(() -> fetch("/empty", 100), "empty");
		assertRefused(() -> UrlSources.fetch(CLIENT, URI.create(base + "/slow"), 100, Duration.ofMillis(200)),
				"timed out");
		assertRefused(() -> UrlSources.fetch(CLIENT, URI.create("http://127.0.0.1:1/file"), 100,
				Duration.ofSeconds(5)), "fetch failed");
	}

	/**
	 * Validate a URL source
	 * @param url URL
	 * @param hosts allowed hosts
	 * @return URL to fetch
	 */
	private static URI uri(String url, Set<String> hosts) {
		return UrlSources.uri(Map.of("kind", "url", "url", url), hosts, NOW);
	}

	/**
	 * Fetch a path from the local server
	 * @param path path
	 * @param maxBytes maximum size
	 * @return file content
	 * @throws InterruptedException
	 */
	private static byte[] fetch(String path, int maxBytes) throws InterruptedException {
		return UrlSources.fetch(CLIENT, URI.create(base + path), maxBytes, Duration.ofSeconds(5));
	}

	/**
	 * Assert that the call is refused with a message containing the text
	 * @param call call
	 * @param text expected text
	 */
	private static void assertRefused(ThrowingCallable call, String text) {
		assertThatThrownBy(call).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith(
				UrlSources.REFUSED).hasMessageContaining(text);
	}
}
