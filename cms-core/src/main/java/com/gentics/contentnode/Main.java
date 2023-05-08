package com.gentics.contentnode;

/**
 * This is the main class of the content.node package. It just prints the
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
}
