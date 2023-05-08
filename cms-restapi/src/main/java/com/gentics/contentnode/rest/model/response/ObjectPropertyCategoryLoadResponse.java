package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ObjectPropertyCategory;

/**
* Response for an object property category load request.
*/
@XmlRootElement
public class ObjectPropertyCategoryLoadResponse extends GenericResponse {

	private static final long serialVersionUID = -2482683441359491131L;

	/**
	 * Object Property Category
	 */
	private ObjectPropertyCategory objectPropertyCategory;

	/**
	 * Create empty instance
	 */
	public ObjectPropertyCategoryLoadResponse() {
	}

	/**
	 * Create instance
	 * @param category object property category
	 * @param responseInfo response info
	 */
	public ObjectPropertyCategoryLoadResponse(ObjectPropertyCategory category, ResponseInfo responseInfo) {
		setObjectPropertyCategory(category);
		setResponseInfo(responseInfo);
	}

	/**
	 * Object Property Category
	 * @return object property category
	 */
	public ObjectPropertyCategory getObjectPropertyCategory() {
		return objectPropertyCategory;
	}

	/**
	 * Set the object property category
	 * @param category object property category
	 */
	public void setObjectPropertyCategory(ObjectPropertyCategory category) {
		this.objectPropertyCategory = category;
	}
}
