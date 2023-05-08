package com.gentics.contentnode.version;

import com.gentics.contentnode.rest.model.CmpProduct;

import java.util.Map;

/**
 * Model class for the component version requirements of a CMP version.
 */
public class CmpVersionRequirement {

	private String cmpVersion;
	private Map<CmpProduct, ProductVersionRange> productVersions;

	public String getCmpVersion() {
		return cmpVersion;
	}

	public void setCmpVersion(String cmpVersion) {
		this.cmpVersion = cmpVersion;
	}

	public Map<CmpProduct, ProductVersionRange> getProductVersions() {
		return productVersions;
	}

	public void setProductVersions(Map<CmpProduct, ProductVersionRange> productVersions) {
		this.productVersions = productVersions;
	}

	@Override
	public String toString() {
		return "[CmpVersionRequirement " + cmpVersion + "]";
	}
}
