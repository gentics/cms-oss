/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: PrivilegesResponse.java,v 1.1.6.1 2011-03-17 13:38:56 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Privilege;

/**
 * Privileges response
 * @author norbert
 */
@XmlRootElement
public class PrivilegesResponse extends GenericResponse {

	/**
	 * Privileges for the user on the requested object
	 */
	private List<Privilege> privileges;

	/**
	 * Constructor used by JAXB
	 */
	public PrivilegesResponse() {}

	/**
	 * @param message
	 * @param responseInfo
	 */
	public PrivilegesResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * @param message
	 * @param responseInfo
	 */
	public PrivilegesResponse(Message message, ResponseInfo responseInfo, List<Privilege> privileges) {
		super(message, responseInfo);
		setPrivileges(privileges);
	}

	/**
	 * @return the privileges
	 */
	public List<Privilege> getPrivileges() {
		return privileges;
	}

	/**
	 * @param privileges the privileges to set
	 */
	public void setPrivileges(List<Privilege> privileges) {
		this.privileges = privileges;
	}
}
