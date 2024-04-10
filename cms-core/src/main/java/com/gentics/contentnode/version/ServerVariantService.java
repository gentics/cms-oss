package com.gentics.contentnode.version;

import com.gentics.contentnode.update.CMSVersion;

public interface ServerVariantService {
	String getVariant();

	String getUpdateUrl();

	String getChangelogUrl(CMSVersion version, CmpVersionRequirement req);
}
