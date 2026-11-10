package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.PortalImplementationModel;

/**
 * Response containing a Portal Implementation
 */
@XmlRootElement
public class PortalImplementationResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 6314572283090146911L;

	private PortalImplementationModel portalImplementation;

	/**
	 * Create empty instance
	 */
	public PortalImplementationResponse() {
	}

	/**
	 * Create instance with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PortalImplementationResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create instance with response info and portal implementation
	 * @param portalImplementation portal implementation
	 * @param responseInfo response info
	 */
	public PortalImplementationResponse(PortalImplementationModel portalImplementation, ResponseInfo responseInfo) {
		super(null, responseInfo);
		setPortalImplementation(portalImplementation);
	}

	/**
	 * Portal Implementation
	 * @return the Portal Implementation
	 */
	public PortalImplementationModel getPortalImplementation() {
		return portalImplementation;
	}

	/**
	 * Set the Portal Implementation
	 * @param portalImplementation Portal Implementation
	 */
	public void setPortalImplementation(PortalImplementationModel portalImplementation) {
		this.portalImplementation = portalImplementation;
	}
}
