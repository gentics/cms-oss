package com.gentics.contentnode.auth;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import com.gentics.contentnode.auth.filter.AbstractSSOFilter;
import org.apache.commons.collections4.iterators.IteratorEnumeration;

public class TestFilterConfig implements FilterConfig {

	public static final String INIT_GROUPS_PARAM = "sso.init_groups";

	private static final Map<String, String> params = new HashMap<>() {{
		put(AbstractSSOFilter.INIT_GROUPS_PARAM, INIT_GROUPS_PARAM);
		put(AbstractSSOFilter.INIT_GROUPS_SYNC, "false");
		put(AbstractSSOFilter.INIT_CALLBACK, "");
	}};

	@Override
	public String getFilterName() {
		return "TestSsoFilter";
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public String getInitParameter(String name) {
		return params.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return new IteratorEnumeration<>(params.keySet().iterator());
	}
}
