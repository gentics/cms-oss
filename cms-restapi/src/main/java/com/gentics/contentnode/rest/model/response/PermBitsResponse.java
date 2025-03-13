package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.PrivilegeMap;
import com.gentics.contentnode.rest.model.perm.PermissionsMap;

/**
 * Response containing permission and role bits
 */
@XmlRootElement
public class PermBitsResponse extends GenericResponse {
	/**
	 * Permission bits from groups
	 */
	protected String perm;

	/**
	 * Permission bits from roles
	 */
	protected String rolePerm;

	/**
	 * Privilege Map
	 */
	protected PrivilegeMap privilegeMap;

	/**
	 * Permissions Map
	 */
	protected PermissionsMap permissionsMap;

	/**
	 * Create an empty instance
	 */
	public PermBitsResponse() {
	}

	/**
	 * Create response with message and response info
	 * @param message message
	 * @param responseInfo response info
	 */
	public PermBitsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * Create success response with perm bits String.
	 * Will have the response info set to Success
	 * @param perm perm String
	 * @param rolePerm role permissions
	 */
	public PermBitsResponse(String perm, String rolePerm) {
		super(null, new ResponseInfo(ResponseCode.OK, "Successfully fetched perm bits"));
		setPerm(perm);
		setRolePerm(rolePerm);
	}

	/**
	 * Permission bits and roles
	 * @return permission bits and roles
	 */
	public String getPerm() {
		return perm;
	}

	/**
	 * Set the permission bits and roles
	 * @param perm permission bits and roles
	 */
	public void setPerm(String perm) {
		this.perm = perm;
	}

	/**
	 * Get the role permission bits
	 * @return role permission bits
	 */
	public String getRolePerm() {
		return rolePerm;
	}

	/**
	 * Set the role permission bits
	 * @param rolePerm role permission bits
	 */
	public void setRolePerm(String rolePerm) {
		this.rolePerm = rolePerm;
	}

	/**
	 * Map representation of all privileges
	 * @return privilege map
	 * @deprecated use {@link #getPermissionsMap()} instead
	 */
	public PrivilegeMap getPrivilegeMap() {
		return privilegeMap;
	}

	/**
	 * Set the privilege map
	 * @param privilegeMap privilege map
	 * @deprecated
	 */
	public void setPrivilegeMap(PrivilegeMap privilegeMap) {
		this.privilegeMap = privilegeMap;
	}

	/**
	 * Map representation of all permissions
	 * @return permissions map
	 */
	public PermissionsMap getPermissionsMap() {
		return permissionsMap;
	}

	/**
	 * Set permissions map
	 * @param permissionsMap map
	 */
	public void setPermissionsMap(PermissionsMap permissionsMap) {
		this.permissionsMap = permissionsMap;
	}
}
