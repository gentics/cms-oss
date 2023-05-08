package com.gentics.contentnode.version;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for the version response of Gentics Portals.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalVersionResponse {

	private String productName;
	private String productVersion;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getProductVersion() {
		return productVersion;
	}

	public void setProductVersion(String productVersion) {
		this.productVersion = productVersion;
	}
}
