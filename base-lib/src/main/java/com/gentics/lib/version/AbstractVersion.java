package com.gentics.lib.version;

/**
 * Abstract Base class for Version Information classes.
 */
public abstract class AbstractVersion implements IVersion {

	/**
	 * Full product information
	 */
	protected String info;

	/**
	 * Product Name
	 */
	protected String productName;

	/**
	 * Release Name
	 */
	protected String releaseName;

	/**
	 * Vendor Name
	 */
	protected String vendorName;

	/**
	 * Product Version
	 */
	protected String version;

	/**
	 * Create an instance of the Version class
	 */
	public AbstractVersion() {
		Package pack = getClass().getPackage();

		productName = pack.getImplementationTitle();
		vendorName = pack.getImplementationVendor();
		version = pack.getImplementationVersion();
		info = productName + " " + version + " by " + vendorName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.version.IVersion#getInfo()
	 */
	public String getInfo() {
		return info;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.version.IVersion#getProductName()
	 */
	public String getProductName() {
		return productName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.version.IVersion#getReleaseName()
	 */
	public String getReleaseName() {
		return releaseName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.lib.version.IVersion#getVendorName()
	 */
	public String getVendorName() {
		return vendorName;
	}

	/*
	 *2 (non-Javadoc)
	 * 
	 * @see com.gentics.lib.version.IVersion#getVersion()
	 */
	public String getVersion() {
		return version;
	}
}
