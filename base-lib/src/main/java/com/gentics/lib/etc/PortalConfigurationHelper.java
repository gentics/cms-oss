package com.gentics.lib.etc;

import java.io.File;

/**
 * Utility class for runtimes in Apache Tomcat
 */
public final class PortalConfigurationHelper {
	/**
	 * Name of the system property used to store the configuration path.
	 * (usually tomcat/config/gentics)
	 */
	public final static String PROPERTY_CONFIGPATH = "com.gentics.portalnode.confpath";

	/**
	 * Returns the Configuration Path. The path set by com.gentics.portalnode.confpath system property. If the system property has not been set the catalina.home property
	 * will be used. Otherwise the configuration path is an empty string.
	 * 
	 * @return
	 */
	public static String getConfigurationPath() {
		String ret = "";

		String SystemOverwrite = System.getProperty(PROPERTY_CONFIGPATH);

		if ((SystemOverwrite != null) && (!SystemOverwrite.equals(""))) {
			// check if last character is file seperator
			ret = SystemOverwrite;
			if (!ret.endsWith(File.separator)) {
				ret += File.separator;
			}
			// Since there will be a silent fallback on most occasions, output
			// an error message here.
			if (!new File(ret).exists()) {
				System.err.println("The given portal configuration path in {" + PROPERTY_CONFIGPATH + "} does not exist. {" + ret + "}");
			}
		} else {
			// first check if this is a tomcat application server
			// get catalina base path
			String CatalinaBase = System.getProperty("catalina.home");

			if ((CatalinaBase != null) && (!CatalinaBase.equals(""))) {
				// tomcat Catalina container
				ret = CatalinaBase + File.separator + "conf" + File.separator + "gentics" + File.separator;
			}
		}
		return ret;
	}
}
