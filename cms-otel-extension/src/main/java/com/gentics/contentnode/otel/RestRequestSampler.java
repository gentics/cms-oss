package com.gentics.contentnode.otel;

import java.util.List;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * Sampler, which samples only the server spans of requests to the REST API and the GenticsImageStore.
 * All other spans (e.g. database access of background jobs, requests to the UI resources) are dropped.
 * Used as root sampler of a parent based sampler (see {@link RestRequestSamplerProvider}), so that
 * the work done for a sampled request is sampled as well.
 */
public class RestRequestSampler implements Sampler {
	/**
	 * Attribute containing the path of the requested URL
	 */
	public final static AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");

	/**
	 * Path prefixes of the requests, which are sampled
	 */
	public final static List<String> SAMPLED_PREFIXES = List.of("/rest/", "/GenticsImageStore/");

	/**
	 * Paths of requests, which are not sampled (the readiness probe of the helm chart)
	 */
	public final static List<String> EXCLUDED_PATHS = List.of("/rest/info/maintenance");

	@Override
	public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
			Attributes attributes, List<LinkData> parentLinks) {
		if (spanKind == SpanKind.SERVER && isSampledPath(attributes.get(URL_PATH))) {
			return SamplingResult.recordAndSample();
		} else {
			return SamplingResult.drop();
		}
	}

	@Override
	public String getDescription() {
		return "RestRequestSampler";
	}

	/**
	 * Check whether requests to the given path are sampled
	 * @param path path, may be null
	 * @return true for sampled paths
	 */
	protected boolean isSampledPath(String path) {
		if (path == null || EXCLUDED_PATHS.contains(path)) {
			return false;
		}
		return SAMPLED_PREFIXES.stream().anyMatch(path::startsWith);
	}
}
