package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Part;

/**
 * Construct part response entity
 * 
 * @author plyhun
 *
 */
@XmlRootElement
public class PartResponse extends GenericResponse {

	private static final long serialVersionUID = -7825195242630188944L;

	private Part part;

	/**
	 * JAXB constructor
	 */
	public PartResponse() {}

	/**
	 * Create instance with message and responseinfo
	 * @param message
	 * @param responseInfo
	 */
	public PartResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	public Part getPart() {
		return part;
	}

	public void setPart(Part part) {
		this.part = part;
	}
}
