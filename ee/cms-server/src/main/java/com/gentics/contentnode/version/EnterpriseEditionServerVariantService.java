package com.gentics.contentnode.version;

import com.gentics.contentnode.update.CMSVersion;

/**
 * Implementation of {@link ServerVariantService} for the EE Version of the CMS
 */
public class EnterpriseEditionServerVariantService implements ServerVariantService {
	/**
	 * EE Update URL
	 */
	private static final String CMS_EE_UPDATE_URL = "https://updates.gentics.com/maven2/com/gentics/cms/maven-metadata.xml";

	@Override
	public String getVariant() {
		return "EE";
	}

	@Override
	public String getUpdateUrl() {
		return CMS_EE_UPDATE_URL;
	}

	@Override
	public String getChangelogUrl(CMSVersion version, CmpVersionRequirement req) {
		return String.format("https://www.gentics.com/cmp/changelog/%s.0/merged_changelog.html", req.getCmpVersion());
	}
}
