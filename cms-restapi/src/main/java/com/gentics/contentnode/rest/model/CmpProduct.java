package com.gentics.contentnode.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Products used in the Gentics Content Management Platform.
 */
public enum CmpProduct {
	CMS("cms", "Gentics CMS"),
	MESH("mesh", "Gentics Mesh"),
	PORTAL_JAVA("portal-java", "Gentics Portal | java"),
	PORTAL_PHP("portal-php", "Gentics Portal | php"),
	PORTAL_ESSENTIALS("portal-essentials", "Gentics Portal | essentials");

	private final String shortCode;
	private final String productName;

	CmpProduct(String shortCode, String productName) {
		this.shortCode = shortCode;
		this.productName = productName;
	}

	/**
	 * Return the respective enum value for the given product name.
	 *
	 * @param productName The product name to find an enum value for
	 * @return The respective enum value for the given product name
	 */
	public static CmpProduct forName(String productName) {
		if (CMS.productName().equals(productName)) {
			return CMS;
		}

		if (MESH.productName().equals(productName)) {
			return MESH;
		}

		if (PORTAL_JAVA.productName().equals(productName)) {
			return PORTAL_JAVA;
		}

		if (PORTAL_PHP.productName().equals(productName)) {
			return PORTAL_PHP;
		}

		if (PORTAL_ESSENTIALS.productName().equals(productName)) {
			return PORTAL_ESSENTIALS;
		}

		return null;
	}

	/**
	 * Get the short code for this enum value.
	 *
	 * <p>
	 *     The short code is also used for deserialization.
	 * </p>
	 *
	 * @return The short code for this enum value
	 */
	@JsonValue
	public String shortCode() {
		return shortCode;
	}

	/**
	 * Get the product name for this enum value.
	 *
	 * @return The product name for this enum value
	 */
	public String productName() {
		return productName;
	}
}
