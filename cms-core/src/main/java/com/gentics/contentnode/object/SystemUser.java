/*
 * @author clemens
 * @date 23.01.2007
 * @version $Id: SystemUser.java,v 1.7.10.1 2011-03-14 15:12:26 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.Consumer;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.GCNRenderable;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.User;

/**
 * this object represents a systemuser in Gentis Content.Node 
 * @author clemens
 *
 */
@TType(SystemUser.TYPE_SYSTEMUSER)
public interface SystemUser extends GCNRenderable, NodeObject, Resolvable, NamedNodeObject, MetaDateNodeObject {
	/**
	 * The ttype of the user administration
	 */
	public static final int TYPE_USERADMIN = 3;

	/**
	 * The ttype of the systemuser object as defined in system/include/public.inc.php
	 */
	public static final int TYPE_SYSTEMUSER = 10;

	/**
	 * Lambda that transforms the node model into the rest model
	 */
	public final static Function<SystemUser, User> TRANSFORM2REST = nodeUser -> {
		// beware of NPE
		if (nodeUser == null) {
			return null;
		}
		User restUser = new User();

		restUser.setId(ObjectTransformer.getInt(nodeUser.getId(), 0));
		restUser.setFirstName(nodeUser.getFirstname());
		restUser.setLastName(nodeUser.getLastname());
		restUser.setEmail(nodeUser.getEmail());
		restUser.setDescription(nodeUser.getDescription());
		restUser.setLogin(nodeUser.getLogin());

		return restUser;
	};

	/**
	 * Function that transforms the rest model into the node object
	 */
	public final static BiFunction<User, SystemUser, SystemUser> REST2NODE = (restUser, nodeUser) -> {
		if (restUser.getFirstName() != null) {
			nodeUser.setFirstname(restUser.getFirstName());
		}
		if (restUser.getLastName() != null) {
			nodeUser.setLastname(restUser.getLastName());
		}
		if (restUser.getLogin() != null) {
			nodeUser.setLogin(restUser.getLogin());
		}
		if (restUser.getEmail() != null) {
			nodeUser.setEmail(restUser.getEmail());
		}
		if (restUser.getDescription() != null) {
			nodeUser.setDescription(restUser.getDescription());
		}
		return nodeUser;
	};

	Consumer<User> EMBED_GROUPS = (restUser) -> {
		Transaction t = TransactionManager.getCurrentTransaction();
		SystemUser nodeUser = t.getObject(SystemUser.class, restUser.getId());
		if (nodeUser == null) {
			return;
		}

		List<UserGroup> groups = nodeUser.getUserGroups();
		List<Group> groupsTransformed = new ArrayList<>();

		for (UserGroup group : groups) {
			groupsTransformed.add(UserGroup.TRANSFORM2REST.apply(group));
		}

		restUser.setGroups(groupsTransformed);
	};

	/**
	 * get the user's firstname
	 * @return firstname
	 */
	String getFirstname();

	/**
	 * Set the firstname
	 * @param firstname firstname
	 * @throws ReadOnlyException
	 */
	void setFirstname(String firstname) throws ReadOnlyException;

	/**
	 * get the user's lastname
	 * @return lastname
	 */
	String getLastname();

	/**
	 * Set the lastname
	 * @param lastname lastname
	 * @throws ReadOnlyException
	 */
	void setLastname(String lastname) throws ReadOnlyException;

	/**
	 * get the user's full name (last name + first name)
	 * @return Full name
	 */
	default String getFullName() {
		return getLastname() + " " + getFirstname();
	}

	/**
	 * get the user's login
	 * @return login
	 */
	String getLogin();

	/**
	 * Set the login
	 * @param login login
	 * @throws ReadOnlyException
	 */
	void setLogin(String login) throws ReadOnlyException;

	/**
	 * get the user's password, which is stored as an bcrypt hash
	 * @return password 
	 */
	String getPassword();

	/**
	 * Set the password hash
	 * @param password password
	 * @throws ReadOnlyException
	 */
	void setPassword(String password) throws ReadOnlyException;

	/**
	 * get the user's email
	 * @return email
	 */
	String getEmail();

	/**
	 * Set the email
	 * @param email user email
	 * @throws ReadOnlyException
	 */
	void setEmail(String email) throws ReadOnlyException;

	/**
	 * get the user's bonus points
	 * @return bonus points
	 */
	int getBonus();

	/**
	 * get the user's active state which may be 0 or 1
	 * @return active state which may be 0 for inactive or 1 for active
	 */
	int getActive();

	/**
	 * Set whether the user is active
	 * @param active true if the user is active, false if not
	 * @throws ReadOnlyException
	 */
	void setActive(boolean active) throws ReadOnlyException;

	/**
	 * get the user's creator id
	 * @return id of the user's creator
	 */
	int getCreator();

	/**
	 * get the user's editor id
	 * @return id of the last systemuser who edited this user
	 */
	int getEditor();

	/**
	 * get the user's description
	 * @return a descriptive text about this user, and maybe some catchy secrets?
	 */
	String getDescription();

	/**
	 * Set the description
	 * @param description description
	 * @throws ReadOnlyException
	 */
	void setDescription(String description) throws ReadOnlyException;

	/**
	 * determine if the user is an LDAP user
	 * @return 0 if the user is not an LDAP user, 1 otherwise
	 */
	int getIsLDAPUser();

	/**
	 * Set whether the user is an ldap user or not
	 * @param ldapUser true for ldap users
	 * @throws ReadOnlyException
	 */
	void setIsLDAPUser(boolean ldapUser) throws ReadOnlyException;

	/**
	 * determine if the user wants its inbox as mails
	 */
	boolean isInboxToEmail();

	/**
	 * Set whether the user has inbox to email activated
	 * @param inboxToEmail true or false
	 * @throws ReadOnlyException
	 */
	void setInboxToEmail(boolean inboxToEmail) throws ReadOnlyException;

	/**
	 * Get the usergroups of this user
	 * @return usergroups of this user
	 * @throws NodeException
	 */
	default List<UserGroup> getUserGroups() throws NodeException {
		return getUserGroups((Function<PermHandler, Boolean>)null);
	}

	/**
	 * Get the usergroups of this user, filtered with a permission filter
	 * @param permissionFilter permission filter
	 * @return filtered usergroups
	 * @throws NodeException
	 */
	List<UserGroup> getUserGroups(Function<PermHandler, Boolean> permissionFilter) throws NodeException;

	/**
	 * Get the usergroups of this user for the given node. This will return all usergroups
	 * the user is member of either unrestricted, or restricted to the given node (if not null)
	 * If node is null, all usergroups of the user will be returned
	 * @param node node (may be null)
	 * @return usergroups of the user
	 * @throws NodeException
	 */
	List<UserGroup> getUserGroups(Node node) throws NodeException;

	/**
	 * Get Node Restrictions of assignments to groups.
	 * Keys are the group IDs, values are the sets of restricted node IDs
	 * For editable Users, the map is modifiable and will be saved
	 * @return map containing the restrictions of user-group assignments to nodes
	 */
	Map<Integer, Set<Integer>> getGroupNodeRestrictions() throws NodeException;

	/**
	 * Get all usergroups of this user, with all parent groups. Every group is only contained once in this list
	 * @return list of usergroups and all parents
	 * @throws NodeException
	 */
	List<UserGroup> getAllGroupsWithParents() throws NodeException;

	/**
	 * Get all usergroups of this user, with all child groups. Every group is only contained once in this list
	 * @param addUserGroups true to add the user's groups, false if not
	 * @return list of usergroups and all children
	 * @throws NodeException
	 */
	default List<UserGroup> getAllGroupsWithChildren(boolean addUserGroups) throws NodeException {
		return getAllGroupsWithChildren(null, addUserGroups);
	}

	/**
	 * Get all usergroups of this user, filtered with the given permission filter, with all child groups. Every group is only contained once in this list
	 * @param permissionFilter permission filter
	 * @param addUserGroups true to add the user's groups, false if not
	 * @return list of usergroups and all children
	 * @throws NodeException
	 */
	public List<UserGroup> getAllGroupsWithChildren(Function<PermHandler, Boolean> permissionFilter, boolean addUserGroups) throws NodeException;
}
