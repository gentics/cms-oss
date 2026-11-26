package com.gentics.contentnode.tests.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.rest.configuration.RESTApplication;
import com.gentics.contentnode.rest.filters.AuthenticationRequestFilter;
import com.gentics.contentnode.rest.filters.TraceIdResponseFilter;
import com.gentics.contentnode.rest.client.exceptions.RestException;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.RESTAppContext;
import com.gentics.contentnode.testutils.RESTAppContext.LoggedInClient;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;

/**
 * Test cases for the tracing information of REST requests: the user attributes of the request span and the trace ID response header
 */
public class RequestTracingTest {
	public final static String LOGIN = "tracinguser";

	public final static String PASSWORD = "tracinguser";

	/**
	 * Request header, which makes the {@link ServerSpanFilter} create a span, which is not sampled
	 */
	public final static String NOT_SAMPLED_HEADER = "X-Test-Not-Sampled";

	private static DBTestContext testContext = new DBTestContext();

	private static RESTAppContext restContext = new RESTAppContext(
			ResourceConfig.forApplication(new RESTApplication()).register(ServerSpanFilter.class));

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(restContext);

	@Rule
	public OpenTelemetryRule otel = OpenTelemetryRule.create();

	private static SystemUser user;

	@BeforeClass
	public static void setupOnce() throws NodeException {
		testContext.getContext().getTransaction().commit();

		UserGroup group = Trx.supply(t -> t.getObject(UserGroup.class, 2));
		user = Trx.supply(() -> Creator.createUser(LOGIN, PASSWORD, "Tracing", "User", "", Arrays.asList(group)));
	}

	/**
	 * Test that the trace ID of a sampled request is returned in the response header and the user is added to the span of the request
	 * @throws RestException
	 */
	@Test
	public void testSampledRequest() throws RestException {
		String traceId;
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			otel.clearSpans();
			Response response = client.get().base().path("user").path("me").request().get();
			assertThat(response.getStatus()).as("Response status").isEqualTo(200);
			traceId = response.getHeaderString(TraceIdResponseFilter.TRACE_ID_HEADER);
		}

		SpanData requestSpan = getRequestSpan("user/me");
		assertThat(traceId).as("Trace ID header").isEqualTo(requestSpan.getTraceId());
		assertThat(requestSpan.getAttributes().get(AuthenticationRequestFilter.USER_ID)).as("User ID")
				.isEqualTo(Integer.toString(user.getId()));
		assertThat(requestSpan.getAttributes().get(AuthenticationRequestFilter.USER_NAME)).as("User name").isEqualTo(LOGIN);
	}

	/**
	 * Test that no trace ID is returned for a request, which is not sampled
	 * @throws RestException
	 */
	@Test
	public void testNotSampledRequest() throws RestException {
		try (LoggedInClient client = restContext.client(LOGIN, PASSWORD)) {
			Response response = client.get().base().path("user").path("me").request().header(NOT_SAMPLED_HEADER, "true").get();
			assertThat(response.getStatus()).as("Response status").isEqualTo(200);
			assertThat(response.getHeaderString(TraceIdResponseFilter.TRACE_ID_HEADER)).as("Trace ID header").isNull();
		}
	}

	/**
	 * Get the finished request span for the given path
	 * @param path path
	 * @return span data
	 */
	protected SpanData getRequestSpan(String path) {
		Optional<SpanData> span = otel.getSpans().stream().filter(s -> s.getKind() == SpanKind.SERVER && s.getName().endsWith(path)).findFirst();
		assertThat(span).as("Request span for " + path).isPresent();
		return span.get();
	}

	/**
	 * Filter, which creates a server span for every request (like the OpenTelemetry Java agent does). Requests with header {@link RequestTracingTest#NOT_SAMPLED_HEADER}
	 * get a span, which is not sampled.
	 * The filter is executed as first request filter and last response filter.
	 */
	@PreMatching
	@Priority(1)
	public static class ServerSpanFilter implements ContainerRequestFilter, ContainerResponseFilter {
		private final static String SPAN_PROPERTY = "test.span";

		private final static String SCOPE_PROPERTY = "test.scope";

		@Override
		public void filter(ContainerRequestContext requestContext) throws IOException {
			Span span;
			if (requestContext.getHeaderString(NOT_SAMPLED_HEADER) != null) {
				span = Span.wrap(SpanContext.create("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331", TraceFlags.getDefault(), TraceState.getDefault()));
			} else {
				span = GlobalOpenTelemetry.getTracer("test").spanBuilder(requestContext.getMethod() + " " + requestContext.getUriInfo().getPath(true))
						.setSpanKind(SpanKind.SERVER).startSpan();
			}
			requestContext.setProperty(SPAN_PROPERTY, span);
			requestContext.setProperty(SCOPE_PROPERTY, span.makeCurrent());
		}

		@Override
		public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
			if (requestContext.getProperty(SCOPE_PROPERTY) instanceof Scope scope) {
				scope.close();
			}
			if (requestContext.getProperty(SPAN_PROPERTY) instanceof Span span) {
				span.end();
			}
		}
	}
}
