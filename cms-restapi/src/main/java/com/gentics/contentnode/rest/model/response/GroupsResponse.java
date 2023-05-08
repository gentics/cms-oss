/*
 * @author norbert
 * @date 16.03.2011
 * @version $Id: GroupsResponse.java,v 1.1.2.1 2011-03-17 13:38:56 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.request.Permission;

/**
 * Response for the request to get the groups
 */
@XmlRootElement
public class GroupsResponse extends GenericResponse {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -6807908191532009756L;

	/**
	 * List of groups
	 */
	private List<Group> groups;

	/**
	 * Permissions map
	 */
	private Map<Integer, Set<Permission>> perms;

	/**
	 * Create an empty response
	 */
	public GroupsResponse() {}

	/**
	 * @param message
	 * @param responseInfo
	 */
	public GroupsResponse(Message message, ResponseInfo responseInfo) {
		super(message, responseInfo);
	}

	/**
	 * @return the groups
	 */
	public List<Group> getGroups() {
		return groups;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	/**
	 * User permissions on the returned items, if requested
	 * @return map of permissions
	 */
	public Map<Integer, Set<Permission>> getPerms() {
		return perms;
	}

	/**
	 * Set user permissions
	 * @param perms permissions
	 */
	public void setPerms(Map<Integer, Set<Permission>> perms) {
		this.perms = perms;
	}
}
