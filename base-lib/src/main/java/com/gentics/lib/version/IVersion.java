package com.gentics.lib.version;

/**
 * Interface for Version classes
 */
public interface IVersion {

	/**
	 * Get the Product Version
	 * @return Product Version
	 */
	String getVersion();

	/**
	 * Get the Product Name
	 * @return Product Name
	 */
	String getProductName();

	/**
	 * Get the Release Name
	 * @return Release Name
	 */
	String getReleaseName();

	/**
	 * Get the full information
	 * @return full information
	 */
	String getInfo();

	/**
	 * Get the Vendor Name
	 * @return Vendor Name
	 */
	String getVendorName();
}
