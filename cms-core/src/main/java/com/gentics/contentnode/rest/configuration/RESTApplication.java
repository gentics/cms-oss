/*
 * @author norbert
 * @date 05.05.2010
 * @version $Id: RESTApplication.java,v 1.1 2010-05-05 15:12:31 norbert Exp $
 */
package com.gentics.contentnode.rest.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ServerProperties;

import com.gentics.contentnode.rest.CannotModifySubpackageMapper;
import com.gentics.contentnode.rest.CommittingResponseFilter;
import com.gentics.contentnode.rest.DuplicateEntityMapper;
import com.gentics.contentnode.rest.EntityNotFoundMapper;
import com.gentics.contentnode.rest.ExceptionRestModelMapper;
import com.gentics.contentnode.rest.InsufficientPrivilegesMapper;
import com.gentics.contentnode.rest.JsonMappingExceptionMapper;
import com.gentics.contentnode.rest.JsonParseExceptionMapper;
import com.gentics.contentnode.rest.ReadOnlyMapper;
import com.gentics.contentnode.rest.RestExceptionMapper;
import com.gentics.contentnode.rest.WebApplicationExceptionMapper;

/**
 * @author norbert
 */
public class RESTApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> resources = new HashSet<>();
		resources.add(JsonParseExceptionMapper.class);
		resources.add(JsonMappingExceptionMapper.class);
		resources.add(JacksonJaxbJsonProvider.class);
		resources.add(MultiPartFeature.class);
		resources.add(RestJacksonJsonProvider.class);
		resources.add(CommittingResponseFilter.class);
		resources.add(ExceptionRestModelMapper.class);
		resources.add(EntityNotFoundMapper.class);
		resources.add(DuplicateEntityMapper.class);
		resources.add(InsufficientPrivilegesMapper.class);
		resources.add(WebApplicationExceptionMapper.class);
		resources.add(CannotModifySubpackageMapper.class);
		resources.add(RestExceptionMapper.class);
		resources.add(ReadOnlyMapper.class);

		return resources;
	}

	/* (non-Javadoc)
	 * @see com.sun.jersey.api.core.DefaultResourceConfig#getSingletons()
	 */
	public Set<Object> getSingletons() {
		return Collections.emptySet();
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put("jersey.config.server.provider.packages", "com.gentics.contentnode.rest.resource.impl,com.gentics.contentnode.rest.filters");
		properties.put(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, "true");
		properties.put(ServerProperties.APPLICATION_NAME, "GCMS");
		return properties;
	}
}
