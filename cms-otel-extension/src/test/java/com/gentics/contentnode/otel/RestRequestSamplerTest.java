package com.gentics.contentnode.otel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;

/**
 * Test cases for the sampler created by the {@link RestRequestSamplerProvider}
 */
public class RestRequestSamplerTest {
	private final static String TRACE_ID = "71ed2eac5bb03cd01bcc828ca0673c1b";

	private final static String SPAN_ID = "a1b2c3d4e5f60718";

	private final Sampler sampler = new RestRequestSamplerProvider().createSampler(null);

	@Test
	public void testName() {
		assertThat(new RestRequestSamplerProvider().getName()).as("Sampler name").isEqualTo("gentics_cms_rest");
	}

	@Test
	public void testRestRequest() {
		assertThat(sample(Context.root(), SpanKind.SERVER, "/rest/folder/load/1")).as("Decision").isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
	}

	@Test
	public void testImageStoreRequest() {
		assertThat(sample(Context.root(), SpanKind.SERVER, "/GenticsImageStore/100/auto/prop/image.jpg"))
				.as("Decision").isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
	}

	@Test
	public void testReadinessProbe() {
		assertThat(sample(Context.root(), SpanKind.SERVER, "/rest/info/maintenance")).as("Decision").isEqualTo(SamplingDecision.DROP);
	}

	@Test
	public void testOtherRequest() {
		assertThat(sample(Context.root(), SpanKind.SERVER, "/editor/index.html")).as("Decision").isEqualTo(SamplingDecision.DROP);
	}

	@Test
	public void testRequestWithoutPath() {
		assertThat(sampler.shouldSample(Context.root(), TRACE_ID, "GET", SpanKind.SERVER, Attributes.empty(), Collections.emptyList())
				.getDecision()).as("Decision").isEqualTo(SamplingDecision.DROP);
	}

	@Test
	public void testRootClientSpan() {
		assertThat(sample(Context.root(), SpanKind.CLIENT, null)).as("Decision").isEqualTo(SamplingDecision.DROP);
	}

	@Test
	public void testChildOfSampledParent() {
		assertThat(sample(parent(false, TraceFlags.getSampled()), SpanKind.CLIENT, null)).as("Decision")
				.isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
	}

	@Test
	public void testChildOfNotSampledParent() {
		assertThat(sample(parent(false, TraceFlags.getDefault()), SpanKind.CLIENT, null)).as("Decision").isEqualTo(SamplingDecision.DROP);
	}

	@Test
	public void testRestRequestWithSampledRemoteParent() {
		assertThat(sample(parent(true, TraceFlags.getSampled()), SpanKind.SERVER, "/rest/page/load/1")).as("Decision")
				.isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
	}

	@Test
	public void testOtherRequestWithSampledRemoteParent() {
		assertThat(sample(parent(true, TraceFlags.getSampled()), SpanKind.SERVER, "/admin/index.html")).as("Decision")
				.isEqualTo(SamplingDecision.DROP);
	}

	@Test
	public void testRestRequestWithNotSampledRemoteParent() {
		assertThat(sample(parent(true, TraceFlags.getDefault()), SpanKind.SERVER, "/rest/page/load/1")).as("Decision")
				.isEqualTo(SamplingDecision.DROP);
	}

	/**
	 * Let the sampler decide
	 * @param parentContext parent context
	 * @param kind span kind
	 * @param path url.path attribute, may be null
	 * @return sampling decision
	 */
	protected SamplingDecision sample(Context parentContext, SpanKind kind, String path) {
		Attributes attributes = path != null ? Attributes.of(RestRequestSampler.URL_PATH, path) : Attributes.empty();
		return sampler.shouldSample(parentContext, TRACE_ID, "span", kind, attributes, Collections.emptyList()).getDecision();
	}

	/**
	 * Create a context with a parent span
	 * @param remote true for a remote parent
	 * @param flags trace flags of the parent
	 * @return context
	 */
	protected Context parent(boolean remote, TraceFlags flags) {
		SpanContext spanContext = remote ? SpanContext.createFromRemoteParent(TRACE_ID, SPAN_ID, flags, TraceState.getDefault())
				: SpanContext.create(TRACE_ID, SPAN_ID, flags, TraceState.getDefault());
		return Context.root().with(Span.wrap(spanContext));
	}
}
