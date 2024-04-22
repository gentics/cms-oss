package com.gentics.contentnode.version;

import com.gentics.contentnode.update.CMSVersion;

/**
 * Interface for service, that returns CMS variant specific information
 */
public interface ServerVariantService {
	/**
	 * Server variant (e.g. "OSS" or "EE")
	 * @return server variant
	 */
	String getVariant();

	/**
	 * Update URL
	 * @return update URL
	 */
	String getUpdateUrl();

	/**
	 * Get the changelog URL for the given version
	 * @param version CMS version
	 * @param req CMP Version Requirement
	 * @return changelog URL
	 */
	String getChangelogUrl(CMSVersion version, CmpVersionRequirement req);
}
