package com.gentics.contentnode.rest.resource.impl.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * POJO for adding a JWT to the forwarded requests
 */
public class CustomProxyJWT {
	/**
	 * Enable the JWT
	 */
	private boolean enabled = false;

	/**
	 * Prefix for username and group names
	 */
	private String prefix = "";

	/**
	 * Enabled flag
	 * @return flag
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set enabled flag
	 * @param enabled enabled flag
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get prefix
	 * @return prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Set prefix
	 * @param prefix prefix
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix != null ? prefix : "";
	}

	/**
	 * Prepend the name with the configured prefix
	 * @param name name
	 * @return prefixed name
	 */
	@JsonIgnore
	public String prefix(String name) {
		return String.format("%s%s", prefix, name);
	}
}
