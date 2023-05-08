package com.gentics.contentnode.api.version;

import com.gentics.contentnode.api.version.Main;

/**
 * This is the main class of the .node package. It just prints the
 * Implementation Title, Vendor and Version from the Manifest
 */
public class Main {

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
		Package pack = Main.class.getPackage();

		if (pack.getImplementationVersion() != null) {
			return pack.getImplementationVersion();
		} else {
			return "DEBUG";
		}
	}
}
