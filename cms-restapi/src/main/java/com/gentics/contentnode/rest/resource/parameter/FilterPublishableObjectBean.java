package com.gentics.contentnode.rest.resource.parameter;

import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlEnum;

/**
 * A bean class used to filter publishable objects
 */
public class FilterPublishableObjectBean {

	/**
	 * The type of the publishable object.
	 */
	@QueryParam("type")
	public PublishTypeDto type;

	/**
	 * The ID of the publishable object.
	 */
	@QueryParam("objId")
	public Integer objId;


	/**
	 * Sets the type filter for the publishable object.
	 *
	 * @param typeFilter the type filter to set
	 * @return the updated FilterPublishableObjectBean instance
	 */
	public FilterPublishableObjectBean withTypeFilter(PublishTypeDto typeFilter) {
		this.type = typeFilter;
		return this;
	}


	/**
	 * Sets the object ID filter for the publishable object.
	 *
	 * @param objId the object ID filter to set
	 * @return the updated FilterPublishableObjectBean instance
	 */
	public FilterPublishableObjectBean withObjectIdFilter(Integer objId) {
		this.objId = objId;
		return this;
	}


	/**
	 * The publish type filter dto enum
	 */
	@XmlEnum()
	public enum PublishTypeDto {
		PAGE,
		FORM;

		/**
		 * Utility method to obtain the type enum from a given string (case-insensitive)
		 *
		 * @param value the value that should be converted to the type
		 * @return the type enum
		 */
		public static PublishTypeDto fromString(String value) {
			String toUpper = value.toUpperCase();
			try {
				return valueOf(toUpper);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
}
