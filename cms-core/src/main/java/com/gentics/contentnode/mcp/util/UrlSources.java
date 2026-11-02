package com.gentics.contentnode.mcp.util;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;

import com.gentics.contentnode.runtime.ConfigurationValue;

/**
 * Fetches the file of a URL source ({@code {kind: url, url, expiresAt}}) for a write tool. Only hosts on the
 * allowlist {@link ConfigurationValue#MCP_UPLOAD_URL_HOSTS} are fetched, over https (plain http only from localhost),
 * without following redirects, with timeouts and a size limit. Every refusal is an {@link IllegalArgumentException}
 * whose message starts with {@link #REFUSED}, so that a client can fall back to a base64 source.
 */
public final class UrlSources {
	/**
	 * Start of the message of every refusal
	 */
	public static final String REFUSED = "URL source refused: ";

	/**
	 * Maximum length of the URL
	 */
	public static final int MAX_URL_LENGTH = 2000;

	/**
	 * Maximum size of the fetched file
	 */
	public static final int MAX_BYTES = 25 * 1024 * 1024;

	/**
	 * Timeout for connecting
	 */
	static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * Timeout for the whole fetch, including the body
	 */
	static final Duration TIMEOUT = Duration.ofSeconds(60);

	/**
	 * Hosts that may be fetched over plain http
	 */
	static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

	private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(Redirect.NEVER)
			.connectTimeout(CONNECT_TIMEOUT).build();

	private UrlSources() {
	}

	/**
	 * Check whether the source is a URL source
	 * @param source source argument
	 * @return true for {@code {kind: url}}
	 */
	public static boolean isUrl(Object source) {
		return source instanceof Map<?, ?> map && "url".equals(map.get("kind"));
	}

	/**
	 * Validate a URL source against the configured allowlist, without fetching it
	 * @param source URL source
	 * @return URL to fetch
	 * @throws IllegalArgumentException if the source is refused
	 */
	public static URI uri(Map<?, ?> source) {
		return uri(source, allowedHosts(ConfigurationValue.MCP_UPLOAD_URL_HOSTS.get()), Instant.now());
	}

	/**
	 * Validate a URL source, without fetching it
	 * @param source URL source
	 * @param allowedHosts allowed hosts, lowercase {@code host} or {@code host:port}
	 * @param now current time
	 * @return URL to fetch
	 * @throws IllegalArgumentException if the source is refused
	 */
	static URI uri(Map<?, ?> source, Set<String> allowedHosts, Instant now) {
		if (!(source.get("url") instanceof String url) || url.isBlank() || url.length() > MAX_URL_LENGTH) {
			throw refused("'url' must be a URL of at most %d characters".formatted(MAX_URL_LENGTH));
		}
		Object expiresAt = source.get("expiresAt");
		if (expiresAt != null) {
			if (!(expiresAt instanceof Number seconds)) {
				throw refused("'expiresAt' must be a unix timestamp in seconds");
			}
			if (seconds.longValue() <= now.getEpochSecond()) {
				throw refused("the URL expired at %d".formatted(seconds.longValue()));
			}
		}

		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw refused("'%s' is not a valid URL".formatted(url));
		}
		String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
		String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : null;
		if (host == null || uri.getRawUserInfo() != null) {
			throw refused("'%s' is not an absolute URL with a host and without user info".formatted(url));
		}
		if (!"https".equals(scheme) && !("http".equals(scheme) && LOCAL_HOSTS.contains(host))) {
			throw refused("only https URLs are fetched, not %s".formatted(scheme));
		}
		int port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
		boolean defaultPort = port == ("https".equals(scheme) ? 443 : 80);
		if (!allowedHosts.contains(host + ":" + port) && !(defaultPort && allowedHosts.contains(host))) {
			throw refused("host %s:%d is not on the allowlist %s".formatted(host, port,
					ConfigurationValue.MCP_UPLOAD_URL_HOSTS.getConfigurationProperty()));
		}
		return uri;
	}

	/**
	 * Parse the allowlist
	 * @param value comma-separated hosts, may be null
	 * @return lowercase hosts, empty if none
	 */
	static Set<String> allowedHosts(String value) {
		Set<String> hosts = new HashSet<>();
		for (String host : StringUtils.split(StringUtils.defaultString(value), ',')) {
			if (!host.isBlank()) {
				hosts.add(host.trim().toLowerCase(Locale.ROOT));
			}
		}
		return hosts;
	}

	/**
	 * Fetch the file
	 * @param uri URL, validated by {@link #uri(Map)}
	 * @return file content
	 * @throws IllegalArgumentException if the fetch fails
	 * @throws InterruptedException if interrupted while waiting
	 */
	public static byte[] fetch(URI uri) throws InterruptedException {
		return fetch(CLIENT, uri, MAX_BYTES, TIMEOUT);
	}

	/**
	 * Fetch the file
	 * @param client HTTP client, which must not follow redirects
	 * @param uri URL
	 * @param maxBytes maximum size of the file
	 * @param timeout timeout for the whole fetch
	 * @return file content
	 * @throws IllegalArgumentException if the fetch fails
	 * @throws InterruptedException if interrupted while waiting
	 */
	static byte[] fetch(HttpClient client, URI uri, int maxBytes, Duration timeout) throws InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
		BodyHandler<byte[]> handler = info -> {
			if (info.statusCode() != 200) {
				return new LimitedBody(maxBytes, "fetch failed with status %d".formatted(info.statusCode()));
			}
			OptionalLong length = info.headers().firstValueAsLong("Content-Length");
			if (length.isPresent() && length.getAsLong() > maxBytes) {
				return new LimitedBody(maxBytes, "the file has %d bytes, at most %d are fetched"
						.formatted(length.getAsLong(), maxBytes));
			}
			return new LimitedBody(maxBytes, null);
		};

		CompletableFuture<byte[]> response = client.sendAsync(request, handler).thenApply(r -> r.body());
		byte[] data;
		try {
			data = response.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			response.cancel(true);
			throw refused("fetch timed out after %d seconds".formatted(timeout.toSeconds()));
		} catch (ExecutionException e) {
			// a refusal of the body may come wrapped by the client
			for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
				if (cause instanceof IllegalArgumentException refusal && refusal.getMessage().startsWith(REFUSED)) {
					throw refusal;
				}
			}
			throw refused("fetch failed: %s".formatted(e.getCause()));
		}
		if (data.length == 0) {
			throw refused("the file is empty");
		}
		return data;
	}

	/**
	 * Build a refusal
	 * @param reason reason
	 * @return exception
	 */
	private static IllegalArgumentException refused(String reason) {
		return new IllegalArgumentException(REFUSED + reason);
	}

	/**
	 * Body that collects at most a maximum number of bytes, or fails right away with a refusal
	 */
	private static class LimitedBody implements BodySubscriber<byte[]> {
		private final CompletableFuture<byte[]> result = new CompletableFuture<>();

		private final ByteArrayOutputStream data = new ByteArrayOutputStream();

		private final int maxBytes;

		private Flow.Subscription subscription;

		/**
		 * Create an instance
		 * @param maxBytes maximum number of bytes
		 * @param refusal reason to refuse the response without reading it, null to read it
		 */
		LimitedBody(int maxBytes, String refusal) {
			this.maxBytes = maxBytes;
			if (refusal != null) {
				result.completeExceptionally(refused(refusal));
			}
		}

		@Override
		public CompletionStage<byte[]> getBody() {
			return result;
		}

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			if (result.isDone()) {
				subscription.cancel();
			} else {
				subscription.request(Long.MAX_VALUE);
			}
		}

		@Override
		public void onNext(List<ByteBuffer> items) {
			if (result.isDone()) {
				return;
			}
			for (ByteBuffer item : items) {
				if (data.size() + item.remaining() > maxBytes) {
					subscription.cancel();
					result.completeExceptionally(refused("the file has more than %d bytes".formatted(maxBytes)));
					return;
				}
				byte[] bytes = new byte[item.remaining()];
				item.get(bytes);
				data.writeBytes(bytes);
			}
		}

		@Override
		public void onError(Throwable throwable) {
			result.completeExceptionally(throwable);
		}

		@Override
		public void onComplete() {
			result.complete(data.toByteArray());
		}
	}
}
