package com.gentics.contentnode.update;

/**
 * POJO for Maven Metadata of an Artifact (read from the updatesite)
 */
public class MavenMetadata {
	protected String groupId;

	protected String artifactId;

	protected CMSVersion version;

	protected MavenMetadataVersioning versioning;

	/**
	 * Group ID
	 * @return group ID
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Set Group ID
	 * @param groupId ID
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * Artifact ID
	 * @return artifact ID
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Set the artifact ID
	 * @param artifactId ID
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * Get the version
	 * @return version
	 */
	public CMSVersion getVersion() {
		return version;
	}

	/**
	 * Set version
	 * @param version version
	 */
	public void setVersion(CMSVersion version) {
		this.version = version;
	}

	/**
	 * Get versioning information
	 * @return versioning information
	 */
	public MavenMetadataVersioning getVersioning() {
		return versioning;
	}

	/**
	 * Set Versioning information
	 * @param versioning information
	 */
	public void setVersioning(MavenMetadataVersioning versioning) {
		this.versioning = versioning;
	}
}
