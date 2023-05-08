package com.gentics.contentnode.rest.configuration;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Provider
public class RestJacksonJsonProvider implements ContextResolver<ObjectMapper> {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	static {
		MAPPER.setSerializationInclusion(Include.NON_NULL);
		MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
		MAPPER.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
	}

	@Override
	public ObjectMapper getContext(Class<?> type) {
		return MAPPER;
	}
}
