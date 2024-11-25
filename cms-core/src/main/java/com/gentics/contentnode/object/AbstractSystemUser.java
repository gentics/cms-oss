package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.collections4.SetUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.render.RenderResult;

public abstract class AbstractSystemUser extends AbstractContentObject implements SystemUser {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -238894809604763479L;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	protected final static Set<String> resolvableKeys;

	static {
		// TODO check correct dependencies
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("firstname", new Property(new String[] { "firstname"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getFirstname();
			}
		});
		resolvableProperties.put("lastname", new Property(new String[] { "lastname"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getLastname();
			}
		});
		resolvableProperties.put("login", new Property(new String[] { "login"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getLogin();
			}
		});
		resolvableProperties.put("email", new Property(new String[] { "email"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getEmail();
			}
		});
		resolvableProperties.put("bonus", new Property(new String[] { "bonus"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getBonus();
			}
		});
		resolvableProperties.put("active", new Property(new String[] { "active"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getActive();
			}
		});
		resolvableProperties.put("creator", new Property(new String[] { "creator"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getCreator();
			}
		});
		Property cdate = new Property(new String[] { "cdate"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getCDate();
			}
		};

		resolvableProperties.put("cdate", cdate);
		resolvableProperties.put("creationdate", cdate);
		resolvableProperties.put("creationtimestamp", new Property(new String[] { "cdate"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getCDate().getTimestamp();
			}
		});
		resolvableProperties.put("editor", new Property(new String[] { "editor"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getEditor();
			}
		});
		Property edate = new Property(new String[] { "edate"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getEDate();
			}
		};

		resolvableProperties.put("edate", edate);
		resolvableProperties.put("editdate", edate);
		resolvableProperties.put("edittimestamp", new Property(new String[] { "edate"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getEDate().getTimestamp();
			}
		});
		resolvableProperties.put("description", new Property(new String[] { "description"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getDescription();
			}
		});
		resolvableProperties.put("isldapuser", new Property(new String[] { "isldapuser"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.getIsLDAPUser();
			}
		});
		resolvableProperties.put("inboxtoemail", new Property(new String[] { "inboxtoemail"}) {
			public Object get(AbstractSystemUser user, String key) {
				return user.isInboxToEmail();
			}
		});

		resolvableKeys = SetUtils.union(AbstractContentObject.resolvableKeys, resolvableProperties.keySet());
	}

	protected AbstractSystemUser(Integer id, NodeObjectInfo info) {
		super(id, info);
	}

	@Override
	public Set<String> getResolvableKeys() {
		return resolvableKeys;
	}

	/**
	 * Get all usergroups of this user, with all parent groups. Every group is only contained once in this list
	 * @return list of usergroups and all parents
	 * @throws NodeException
	 */
	public List<UserGroup> getAllGroupsWithParents() throws NodeException {
		List<UserGroup> collectedGroups = new Vector<UserGroup>();
		List<UserGroup> groups = getUserGroups();

		for (UserGroup group : groups) {
			addGroupWithMothers(group, collectedGroups);
		}

		return collectedGroups;
	}

	@Override
	public List<UserGroup> getAllGroupsWithChildren(Function<PermHandler, Boolean> permissionFilter, boolean addUserGroups) throws NodeException {
		List<UserGroup> collectedGroups = new Vector<UserGroup>();
		List<UserGroup> groups = getUserGroups(permissionFilter);

		for (UserGroup group : groups) {
			addGroupWithChildren(group, collectedGroups, addUserGroups);
		}

		return collectedGroups;
	}

	/**
	 * Recursive method to add the given group with its mother groups to the list of groups
	 * @param group group to add
	 * @param groups list of groups
	 * @throws NodeException
	 */
	protected void addGroupWithMothers(UserGroup group, List<UserGroup> groups) throws NodeException {
		if (group != null && !groups.contains(group)) {
			groups.add(group);
			addGroupWithMothers(group.getMother(), groups);
		}
	}

	/**
	 * Recursive method to add the given group with its child groups to the list of groups
	 * @param group group to add
	 * @param groups list of groups
	 * @param addGroup true to add the group itself, false to only add child groups
	 * @throws NodeException
	 */
	protected void addGroupWithChildren(UserGroup group, List<UserGroup> groups, boolean addGroup) throws NodeException {
		if (group != null && !groups.contains(group)) {
			if (addGroup) {
				groups.add(group);
			}
			List<UserGroup> children = group.getChildGroups();
			for (UserGroup child : children) {
				addGroupWithChildren(child, groups, true);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}

	/**
	 * render will just concatenate first- and lastname
	 */
	public String render(RenderResult renderResult) throws NodeException {
		return get("firstname") + " " + get("lastname");
	}

	/**
	 * retrieve a property of this object identified by key
	 * @param key of the property to be returned
	 */
	public Object get(String key) {
		Property prop = (Property) resolvableProperties.get(key);

		if (prop != null) {
			Object value = prop.get(this, key);

			addDependency(key, value);
			return value;
		} else {
			return super.get(key);
		}
	}

	@Override
	public void setActive(boolean active) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setDescription(String description) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setEmail(String email) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setFirstname(String firstname) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setInboxToEmail(boolean inboxToEmail) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setIsLDAPUser(boolean ldapUser) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setLastname(String lastname) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setLogin(String login) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public void setPassword(String password) throws ReadOnlyException {
		failReadOnly();
	}

	@Override
	public String getName() {
		return getLogin();
	}

	/**
	 * Inner property class
	 */
	private abstract static class Property extends AbstractProperty {

		/**
		 * Create instance of the property
		 * @param dependsOn
		 */
		public Property(String[] dependsOn) {
			super(dependsOn);
		}

		/**
		 * Get the property value for the given object
		 * @param object object
		 * @param key property key
		 * @return property value
		 */
		public abstract Object get(AbstractSystemUser object, String key);
	}
}
