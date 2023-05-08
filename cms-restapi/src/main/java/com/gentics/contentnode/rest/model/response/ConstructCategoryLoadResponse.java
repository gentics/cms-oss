package com.gentics.contentnode.rest.model.response;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.ConstructCategory;

/**
 * Class representing the construct category load response.
 * 
 * @author plyhun
 * 
 */
@XmlRootElement
public class ConstructCategoryLoadResponse extends GenericResponse {

	private static final long serialVersionUID = -4576547672935705070L;

	private ConstructCategory constructCategory;

	/**
	 * Constructor used by JAXB
	 */
	public ConstructCategoryLoadResponse() {}

	/**
	 * Create instance with message and responseinfo
	 * @param message
	 * @param responseInfo
	 */
	public ConstructCategoryLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Returns the loaded construct category
	 * @return
	 */
	public ConstructCategory getConstructCategory() {
		return constructCategory;
	}

	/**
	 * Sets the construct category for this response
	 * @param category
	 */
	public void setConstruct(ConstructCategory category) {
		this.constructCategory = category;
	}

}
