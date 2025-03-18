package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Construct;

/**
 * Class representing the construct load response.
 * 
 * @author johannes2
 * 
 */
@XmlRootElement
public class ConstructLoadResponse extends GenericResponse {

	private static final long serialVersionUID = 179915475207553374L;

	private Construct construct;

	/**
	 * Constructor used by JAXB
	 */
	public ConstructLoadResponse() {}

	/**
	 * Create instance with message and responseinfo
	 * @param message
	 * @param responseInfo
	 */
	public ConstructLoadResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Returns the loaded construct
	 * @return
	 */
	public Construct getConstruct() {
		return construct;
	}

	/**
	 * Sets the construct for this response
	 * @param construct
	 */
	public void setConstruct(Construct construct) {
		this.construct = construct;
	}

}
