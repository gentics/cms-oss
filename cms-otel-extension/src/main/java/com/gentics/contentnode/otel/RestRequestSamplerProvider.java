package com.gentics.contentnode.otel;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * Provider for the sampler "gentics_cms_rest" (selected with OTEL_TRACES_SAMPLER=gentics_cms_rest).
 * Spans without parent and spans with a sampled remote parent are sampled by the {@link RestRequestSampler},
 * all other spans follow the sampling decision of their parent.
 */
public class RestRequestSamplerProvider implements ConfigurableSamplerProvider {
	/**
	 * Name of the sampler
	 */
	public final static String NAME = "gentics_cms_rest";

	@Override
	public Sampler createSampler(ConfigProperties config) {
		Sampler rest = new RestRequestSampler();
		return Sampler.parentBasedBuilder(rest).setRemoteParentSampled(rest).build();
	}

	@Override
	public String getName() {
		return NAME;
	}
}
