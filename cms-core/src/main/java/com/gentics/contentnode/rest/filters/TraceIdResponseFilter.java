package com.gentics.contentnode.rest.filters;

import java.io.IOException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter implementation that adds the ID of the trace of the request (if the request is traced with OpenTelemetry)
 * as response header {@link #TRACE_ID_HEADER}, so that the trace can be found.
 */
@Provider
public class TraceIdResponseFilter implements ContainerResponseFilter {
	/**
	 * Name of the response header containing the trace ID
	 */
	public final static String TRACE_ID_HEADER = "X-Trace-Id";

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		SpanContext spanContext = Span.current().getSpanContext();
		if (spanContext.isValid() && spanContext.isSampled()) {
			responseContext.getHeaders().putSingle(TRACE_ID_HEADER, spanContext.getTraceId());
		}
	}
}
