package com.gentics.contentnode.rest.resource.impl.proxy;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for a Parameter, that can be used in the baseUrl
 */
public class Parameter {
	@JsonProperty("default")
	private String defaultValue;

	private List<String> values = Collections.emptyList();

	/**
	 * Get the default value
	 * @return default value
	 */
	@JsonProperty("default")
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Set the default value
	 * @param defaultValue default value
	 */
	@JsonProperty("default")
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Get the possible values
	 * @return possible values
	 */
	public List<String> getValues() {
		return values;
	}

	/**
	 * Set the possible values
	 * @param values possible values
	 */
	public void setValues(List<String> values) {
		this.values = values;
	}

	/**
	 * Get the value for the query parameter.
	 * This is either the value contained in the query parameters (if valid),
	 * or the default value (if configured)
	 * or the first value in the list of parameters
	 * or empty.
	 * @param key parameter key
	 * @param queryParameters query parameters
	 * @return value
	 */
	@JsonIgnore
	public String get(String key, MultivaluedMap<String, String> queryParameters) {
		String value = queryParameters.getFirst(key);
		if (value != null && !values.contains(value)) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		if (value == null) {
			value = defaultValue;
		}

		if (value == null && !values.isEmpty()) {
			value = values.get(0);
		}

		if (value == null) {
			value = "";
		}

		return value;
	}
}
