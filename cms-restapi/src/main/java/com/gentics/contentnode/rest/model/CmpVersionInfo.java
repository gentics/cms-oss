package com.gentics.contentnode.rest.model;

/**
 * Model class for CMP version information.
 */
public class CmpVersionInfo {

	private String meshVersion;
	private String portalType;
	private String portalVersion;
	private CmpCompatibility compatibility;

	public String getMeshVersion() {
		return meshVersion;
	}

	public CmpVersionInfo setMeshVersion(String meshVersion) {
		this.meshVersion = meshVersion;
		return this;
	}

	public String getPortalType() {
		return portalType;
	}

	public CmpVersionInfo setPortalType(String portalType) {
		this.portalType = portalType;
		return this;
	}

	public String getPortalVersion() {
		return portalVersion;
	}

	public CmpVersionInfo setPortalVersion(String portalVersion) {
		this.portalVersion = portalVersion;
		return this;
	}

	public CmpCompatibility getCompatibility() {
		return compatibility;
	}

	public CmpVersionInfo setCompatibility(CmpCompatibility compatibility) {
		this.compatibility = compatibility;
		return this;
	}
}
