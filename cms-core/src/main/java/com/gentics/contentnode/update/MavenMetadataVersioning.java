package com.gentics.contentnode.update;

import java.util.List;

/**
 * POJO for versioning information in {@link MavenMetadata}
 */
public class MavenMetadataVersioning {
	protected CMSVersion latest;

	protected CMSVersion release;

	protected List<CMSVersion> versions;

	protected String lastUpdated;

	/**
	 * Get the latest version
	 * @return latest version
	 */
	public CMSVersion getLatest() {
		return latest;
	}

	/**
	 * Set latest version
	 * @param latest latest version
	 */
	public void setLatest(CMSVersion latest) {
		this.latest = latest;
	}

	/**
	 * Get the latest release version
	 * @return latest release version
	 */
	public CMSVersion getRelease() {
		return release;
	}

	/**
	 * Set release
	 * @param release version
	 */
	public void setRelease(CMSVersion release) {
		this.release = release;
	}

	/**
	 * Get list of available versions
	 * @return list of available versions
	 */
	public List<CMSVersion> getVersions() {
		return versions;
	}

	/**
	 * Set available versions
	 * @param versions list of versions
	 */
	public void setVersions(List<CMSVersion> versions) {
		this.versions = versions;
	}

	/**
	 * Get date of last update
	 * @return last update date
	 */
	public String getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * Set last update date
	 * @param lastUpdated date
	 */
	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
