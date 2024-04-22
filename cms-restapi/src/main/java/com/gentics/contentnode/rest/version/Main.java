package com.gentics.contentnode.rest.version;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is the main class of the .node package. It just prints the
 * Implementation Title, Vendor and Version from the Manifest
 */
public class Main {

	private final static AtomicReference<Properties> buildInfo = new AtomicReference<>(null);

	/**
	 * Main method
	 *
	 * @param args
	 *            arguments
	 */
	public static void main(String[] args) {
		System.out.println(getInfo());
	}

	/**
	 * Get the product information
	 * @return product information
	 */
	public static String getInfo() {
		Package pack = Main.class.getPackage();

		return pack.getImplementationTitle() + " " + pack.getImplementationVersion() + " by " + pack.getImplementationVendor();
	}

	/**
	 * Get the version information
	 * @return version information
	 */
	public static String getImplementationVersion() {
		if (buildInfo.get() == null) {
			Properties props = new Properties();

			try {
				props.load(Main.class.getResourceAsStream("/cms.build.properties"));
			} catch (IOException e) {
				// We have no NodeLogger here, so just fall back to the old version.
				String implementationVersion = Main.class.getPackage().getImplementationVersion();

				return implementationVersion != null ? implementationVersion : "DEBUG";
			}

			buildInfo.set(props);
		}

		String version = buildInfo.get().getProperty("cms.version");

		return version != null ? version : "DEBUG";
	}
}
