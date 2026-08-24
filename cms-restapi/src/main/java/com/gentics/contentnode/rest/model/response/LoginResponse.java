package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response to a login request
 */
@XmlRootElement
public class LoginResponse extends AuthenticationResponse {

	private static final long serialVersionUID = -5241126069170279247L;

	/**
	 * Create an instance
	 */
	public LoginResponse() {}
}
