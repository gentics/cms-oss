package com.gentics.lib.version;

import com.gentics.lib.log.NodeLogger;

/**
 * The Version provider is used to find a specific Version Implementation and
 * return it. This actually is an ugly hack due to the fact that not all
 * Portal.Node/Content.Node specific classes are located in the correct
 * libraries.
 */
public class VersionProvider {

	/**
	 * Names of the Version classes to check for
	 */
	protected final static String[] VERSION_CLASSES = {
		"com.gentics.portalnode.version.Version", "com.gentics.contentnode.version.Version", "com.gentics.lib.version.Version" };

	/**
	 * Implementation of the Version running
	 */
	protected static IVersion versionImpl;

	/**
	 * Logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(VersionProvider.class);

	static {
		// try to load a version class
		for (int i = 0; i < VERSION_CLASSES.length; i++) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Checking for " + VERSION_CLASSES[i]);
				}
				versionImpl = (IVersion) VersionProvider.class.forName(VERSION_CLASSES[i]).newInstance();
				if (logger.isDebugEnabled()) {
					logger.debug("Found " + VERSION_CLASSES[i]);
				}
				break;
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find " + VERSION_CLASSES[i], e);
				}
			}
		}

		if (versionImpl == null) {
			logger.warn("Could not find a Version class, instantiating it by hand now");
			versionImpl = new Version();
		}
	}

	/**
	 * Get the version implementation
	 * @return version
	 */
	public static IVersion getVersion() {
		return versionImpl;
	}
}
