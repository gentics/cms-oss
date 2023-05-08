package com.gentics.contentnode.version;

/**
 * Model for a product version range defined by a {@code minVersion} and a
 * {@code maxVersion}.
 */
public class ProductVersionRange {

	private String minVersion;
	private String maxVersion;

	public String getMinVersion() {
		return minVersion;
	}

	public ProductVersionRange setMinVersion(String minVersion) {
		this.minVersion = minVersion;

		return this;
	}

	public String getMaxVersion() {
		return maxVersion;
	}

	public ProductVersionRange setMaxVersion(String maxVersion) {
		this.maxVersion = maxVersion;

		return this;
	}
}
