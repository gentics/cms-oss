package com.gentics.contentnode.tests.rest.util;

import static com.gentics.contentnode.db.DBUtils.update;
import static com.gentics.contentnode.factory.Trx.operate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;
import java.util.function.Function;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.ResponseCode;
import com.gentics.contentnode.rest.model.response.ResponseInfo;
import com.gentics.contentnode.rest.util.Operator;
import com.gentics.contentnode.testutils.DBTestContext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Test cases for the spans of jobs executed by the {@link Operator}
 */
public class OperatorTracingTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Rule
	public OpenTelemetryRule otel = OpenTelemetryRule.create();

	@After
	public void tearDown() throws NodeException {
		operate(() -> update("DELETE FROM msg"));
	}

	/**
	 * Test that the span of a job executed in the foreground is a child of the calling span
	 * and the parent of the spans created by the job
	 * @throws NodeException
	 */
	@Test
	public void testForegroundJob() throws NodeException {
		Tracer tracer = otel.getOpenTelemetry().getTracer("test");
		Span request = tracer.spanBuilder("request").startSpan();
		try (Scope scope = request.makeCurrent()) {
			Trx.operate(() -> Operator.execute("Foreground Job", 0, () -> {
				tracer.spanBuilder("work").startSpan().end();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, ""));
			}, Function.identity()));
		} finally {
			request.end();
		}

		SpanData job = getSpan(Operator.JOB_SPAN_NAME);
		SpanData work = getSpan("work");

		assertThat(job.getTraceId()).as("Trace ID of job span").isEqualTo(request.getSpanContext().getTraceId());
		assertThat(job.getParentSpanId()).as("Parent of job span").isEqualTo(request.getSpanContext().getSpanId());
		assertThat(work.getParentSpanId()).as("Parent of span created by the job").isEqualTo(job.getSpanId());
		assertThat(job.getAttributes().get(Operator.JOB_DESCRIPTION)).as("Job description").isEqualTo("Foreground Job");
		assertThat(job.getAttributes().get(Operator.JOB_BACKGROUND)).as("Background flag").isNull();
		assertThat(job.getStatus().getStatusCode()).as("Status of job span").isEqualTo(StatusCode.UNSET);
	}

	/**
	 * Test that the span of a job, which is continued in the background, is marked and ends after the calling span
	 * @throws Exception
	 */
	@Test
	public void testBackgroundJob() throws Exception {
		Tracer tracer = otel.getOpenTelemetry().getTracer("test");
		Span request = tracer.spanBuilder("request").startSpan();
		try (Scope scope = request.makeCurrent()) {
			GenericResponse response = Trx.supply(() -> Operator.execute("Background Job", 100, () -> {
				Thread.sleep(1000);
				tracer.spanBuilder("work").startSpan().end();
				return new GenericResponse(null, new ResponseInfo(ResponseCode.OK, ""));
			}, Function.identity()));
			assertThat(response.getResponseInfo().getResponseCode()).as("Response code").isEqualTo(ResponseCode.OK);
		} finally {
			request.end();
		}

		SpanData job = waitForSpan(Operator.JOB_SPAN_NAME, 10000);
		SpanData work = getSpan("work");
		SpanData requestData = getSpan("request");

		assertThat(job.getParentSpanId()).as("Parent of job span").isEqualTo(request.getSpanContext().getSpanId());
		assertThat(work.getParentSpanId()).as("Parent of span created by the job").isEqualTo(job.getSpanId());
		assertThat(job.getAttributes().get(Operator.JOB_BACKGROUND)).as("Background flag").isTrue();
		assertThat(job.getEndEpochNanos()).as("End of job span").isGreaterThan(requestData.getEndEpochNanos());
	}

	/**
	 * Test that the span of a failing job gets the error status
	 * @throws NodeException
	 */
	@Test
	public void testFailingJob() throws NodeException {
		Throwable thrown = catchThrowable(() -> Trx.operate(() -> Operator.executeRethrowing("Failing Job", 0, () -> {
			throw new NodeException("Job failed");
		}, Function.identity())));
		assertThat(thrown).as("Thrown exception").isNotNull();

		SpanData job = getSpan(Operator.JOB_SPAN_NAME);
		assertThat(job.getStatus().getStatusCode()).as("Status of job span").isEqualTo(StatusCode.ERROR);
		assertThat(job.getEvents()).as("Events of job span").anyMatch(event -> "exception".equals(event.getName()));
	}

	/**
	 * Get the finished span with the given name
	 * @param name span name
	 * @return span data
	 */
	protected SpanData getSpan(String name) {
		Optional<SpanData> span = findSpan(name);
		assertThat(span).as("Span " + name).isPresent();
		return span.get();
	}

	/**
	 * Find the finished span with given name
	 * @param name span name
	 * @return optional span data
	 */
	protected Optional<SpanData> findSpan(String name) {
		return otel.getSpans().stream().filter(span -> name.equals(span.getName())).findFirst();
	}

	/**
	 * Wait until the span with the given name is finished
	 * @param name span name
	 * @param timeoutMs timeout in ms
	 * @return span data
	 * @throws InterruptedException
	 */
	protected SpanData waitForSpan(String name, long timeoutMs) throws InterruptedException {
		long end = System.currentTimeMillis() + timeoutMs;
		while (findSpan(name).isEmpty() && System.currentTimeMillis() < end) {
			Thread.sleep(100);
		}
		return getSpan(name);
	}
}
