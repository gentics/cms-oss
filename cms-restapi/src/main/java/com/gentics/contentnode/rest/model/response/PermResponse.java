package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response containing the permission flag for a specific permission request
 */
@XmlRootElement
public class PermResponse extends GenericResponse {
	/**
	 * Permission granted
	 */
	protected boolean granted = false;

	/**
	 * Create an empty instance
	 */
	public PermResponse() {
	}

	/**
	 * Create response with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PermResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create success response with perm bits String.
	 * Will have the response info set to Success
	 * @param granted true if permission is granted, false if not
	 */
	public PermResponse(boolean granted) {
		super(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched perm"));
		setGranted(granted);
	}

	/**
	 * Flag whether the permission is granted
	 * @param granted true if granted, false if not
	 */
	public void setGranted(boolean granted) {
		this.granted = granted;
	}

	/**
	 * Flag whether the permission is granted
	 * @return true if granted, false if not
	 */
	public boolean isGranted() {
		return granted;
	}
}
