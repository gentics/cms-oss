package com.gentics.contentnode.tests.rest.client;

import java.util.HashSet;
import java.util.Set;

import com.gentics.contentnode.rest.configuration.RESTApplication;

/**
 * Dummy REST Application that adds the {@link DummySSOFilter}
 */
public class DummySSOApplication extends RESTApplication {
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> resources = new HashSet<>(super.getClasses());
		resources.add(DummySSOFilter.class);
		return resources;
	}
}
