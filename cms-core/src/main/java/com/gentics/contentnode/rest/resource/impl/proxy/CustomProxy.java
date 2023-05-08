package com.gentics.contentnode.rest.resource.impl.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.api.lib.exception.NodeException;

/**
 * POJO representing the configuration of a custom proxy
 */
public class CustomProxy {
	/**
	 * Base URL
	 */
	private String baseUrl;

	/**
	 * Headers to be added to the requests
	 */
	private Map<String, String> headers = new HashMap<>();

	/**
	 * Permission to check (null for no permission required)
	 */
	private CustomProxyPermission permission;

	/**
	 * Set of allowed methods (null for all request methods allowed)
	 */
	private Set<String> methods;

	/**
	 * Map of parameters
	 */
	private Map<String, Parameter> parameters = new HashMap<>();

	/**
	 * HTTP proxy setting
	 */
	private CustomProxyProxy proxy;

	/**
	 * JWT settings
	 */
	private CustomProxyJWT jwt = new CustomProxyJWT();

	/**
	 * Get the base URL
	 * @return base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL
	 * @param baseUrl base URL
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the headers
	 * @return headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Set the headers
	 * @param headers headers
	 */
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	/**
	 * Get permission
	 * @return permission
	 */
	public CustomProxyPermission getPermission() {
		return permission;
	}

	/**
	 * Set the permission
	 * @param permission permission
	 */
	public void setPermission(CustomProxyPermission permission) {
		this.permission = permission;
	}

	/**
	 * Get allowed methods
	 * @return allowed methods
	 */
	public Set<String> getMethods() {
		return methods;
	}

	/**
	 * Set allowed methods
	 * @param methods allowed methods
	 */
	public void setMethods(Set<String> methods) {
		this.methods = methods;
	}

	/**
	 * Get the map of parameters
	 * @return map of parameters
	 */
	public Map<String, Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Set the map of parameters
	 * @param parameters map of parameters
	 */
	public void setParameters(Map<String, Parameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get HTTP proxy settings
	 * @return HTTP proxy settings
	 */
	public CustomProxyProxy getProxy() {
		return proxy;
	}

	/**
	 * Set HTTP proxy settings
	 * @param proxy proxy settings
	 */
	public void setProxy(CustomProxyProxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * Get JWT settings
	 * @return JWT settings
	 */
	public CustomProxyJWT getJwt() {
		return jwt;
	}

	/**
	 * Set JWT settings
	 * @param jwt settings
	 */
	public void setJwt(CustomProxyJWT jwt) {
		this.jwt = jwt != null ? jwt : new CustomProxyJWT();
	}

	/**
	 * Get the base URL with optional parameters replaced by query parameters
	 * @param queryParameters query parameters
	 * @return base URL
	 */
	@JsonIgnore
	public String getBaseUrl(MultivaluedMap<String, String> queryParameters) {
		String base = baseUrl;
		for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Parameter parameter = entry.getValue();

			String placeHolder = String.format("{{%s}}", key);
			String regexPlaceHolder = String.format("\\{\\{%s\\}\\}", key);
			if (base.contains(placeHolder)) {
				String value = parameter.get(key, queryParameters);
				base = base.replaceAll(regexPlaceHolder, Matcher.quoteReplacement(value));
			}
		}
		return base;
	}

	/**
	 * Check whether access to the proxied resource is allowed
	 * @param methodName request method name
	 * @return true iff access is allowed (or no permission check is done)
	 * @throws NodeException
	 */
	@JsonIgnore
	public boolean allowAccess(String methodName) throws NodeException {
		// check whether permission settings formbids access
		if (permission != null && !permission.allowAccess()) {
			return false;
		}
		// check whether method is allowed
		if (methods != null && !methods.contains(methodName)) {
			return false;
		}
		// all is good, allow access
		return true;
	}
}
