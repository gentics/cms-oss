package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ObjectProperty;

/**
* Response for an object property load request.
*/
@XmlRootElement
public class ObjectPropertyLoadResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -4749890848262832433L;

	/**
	 * Object Property
	 */
	private ObjectProperty objectProperty;

	/**
	 * Create empty instance
	 */
	public ObjectPropertyLoadResponse() {
	}

	/**
	 * Create instance
	 * @param objectProperty object property
	 * @param responseInfo response info
	 */
	public ObjectPropertyLoadResponse(ObjectProperty objectProperty, ResponseInfo responseInfo) {
		setObjectProperty(objectProperty);
		setResponseInfo(responseInfo);
	}

	/**
	 * Object Property
	 * @return object property
	 */
	public ObjectProperty getObjectProperty() {
		return objectProperty;
	}

	/**
	 * Set the object property
	 * @param objectProperty object property
	 */
	public void setObjectProperty(ObjectProperty objectProperty) {
		this.objectProperty = objectProperty;
	}
}
