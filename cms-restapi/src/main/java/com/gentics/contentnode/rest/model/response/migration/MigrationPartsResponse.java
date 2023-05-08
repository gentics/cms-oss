package com.gentics.contentnode.rest.model.response.migration;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Part;
import com.gentics.contentnode.rest.model.response.GenericResponse;
import com.gentics.contentnode.rest.model.response.Message;
import com.gentics.contentnode.rest.model.response.ResponseInfo;

/**
 * Response to request to load parts for a given tag
 * 
 * @author Taylor
 * 
 */
@XmlRootElement
public class MigrationPartsResponse extends GenericResponse {

	/**
	 * List of Parts
	 */
	private List<Part> parts;

	/**
	 * Constructor used by JAXB
	 */
	public MigrationPartsResponse() {}

	/**
	 * Create an instance of the response with single message and response info
	 * 
	 * @param message
	 *            message
	 * @param responseInfo
	 *            response info
	 */
	public MigrationPartsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Returns the list of parts for this response
	 * @return
	 */
	public List<Part> getParts() {
		return parts;
	}

	/**
	 * Sets the list of parts for this reponse
	 * @param parts
	 */
	public void setParts(List<Part> parts) {
		this.parts = parts;
	}
}
