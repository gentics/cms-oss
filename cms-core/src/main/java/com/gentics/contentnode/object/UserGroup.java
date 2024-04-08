/*
 * @author norbert
 * @date 10.03.2011
 * @version $Id: UserGroup.java,v 1.1.2.3 2011-03-17 13:38:55 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.etc.BiFunction;
import com.gentics.contentnode.etc.ContentNodeDate;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.factory.TType;
import com.gentics.contentnode.factory.object.ExtensibleObject;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.lib.log.NodeLogger;

/**
 * This class represents UserGroup objects 
 */
@TType(UserGroup.TYPE_USERGROUP)
public abstract class UserGroup extends AbstractContentObject implements NamedNodeObject, ExtensibleObject<UserGroup> {
	/**
	 * The ttype of the group admin
	 */
	public final static int TYPE_GROUPADMIN = 4;

	/**
	 * The ttype of the usergroup object as defined in system/include/public.inc.php
	 */
	public static final int TYPE_USERGROUP = 6;

	/**
	 * static map of resolvable properties
	 */
	protected static Map<String, Property> resolvableProperties;

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(UserGroup.class);

	/**
	 * Function that transforms the node model into the rest model
	 */
	public final static Function<UserGroup, Group> TRANSFORM2REST = nodeGroup -> {
		// beware of NPE
		if (nodeGroup == null) {
			return null;
		}
		Group restGroup = new Group();
		restGroup.setId(ObjectTransformer.getInteger(nodeGroup.getId(), null));
		restGroup.setName(nodeGroup.getName());
		restGroup.setDescription(nodeGroup.getDescription());
		return restGroup;
	};

	/**
	 * Function that transforms the rest model into the node object
	 */
	public final static BiFunction<Group, UserGroup, UserGroup> REST2NODE = (restGroup, nodeGroup) -> {
		if (restGroup.getName() != null) {
			nodeGroup.setName(restGroup.getName());
		}
		if (restGroup.getDescription() != null) {
			nodeGroup.setDescription(restGroup.getDescription());
		}
		return nodeGroup;
	};

	/**
	 * Static initialization
	 */
	static {
		// initialize the property resolvers
		resolvableProperties = new HashMap<String, Property>();
		resolvableProperties.put("name", new Property(new String[] { "name"}) {
			@Override
			public Object get(UserGroup group, String key) {
				return group.getName();
			}
		});
		resolvableProperties.put("description", new Property(new String[] { "description"}) {
			@Override
			public Object get(UserGroup group, String key) {
				return group.getDescription();
			}
		});
		resolvableProperties.put("mother", new Property(new String[] { "mother"}) {
			@Override
			public Object get(UserGroup group, String key) {
				try {
					UserGroup mother = group.getMother();

					if (mother == null) {
						return 0;
					} else {
						return mother.getId();
					}
				} catch (NodeException e) {
					logger.error("Error while resolving mother of " + group, e);
					return 0;
				}
			}
		});
		resolvableProperties.put("creator", new Property(new String[] { "creator"}) {
			public Object get(UserGroup group, String key) {
				try {
					return group.getCreator();
				} catch (NodeException e) {
					logger.error("Error while getting creator of " + this, e);
					return null;
				}
			}
		});
		Property cdate = new Property(new String[] { "cdate"}) {
			public Object get(UserGroup group, String key) {
				return group.getCDate();
			}
		};

		resolvableProperties.put("cdate", cdate);
		resolvableProperties.put("creationdate", cdate);
		resolvableProperties.put("creationtimestamp", new Property(new String[] { "cdate"}) {
			public Object get(UserGroup group, String key) {
				return group.getCDate().getTimestamp();
			}
		});
		resolvableProperties.put("editor", new Property(new String[] { "editor"}) {
			public Object get(UserGroup group, String key) {
				try {
					return group.getEditor();
				} catch (NodeException e) {
					logger.error("Error while getting editor of " + this, e);
					return null;
				}
			}
		});
		Property edate = new Property(new String[] { "edate"}) {
			public Object get(UserGroup group, String key) {
				return group.getEDate();
			}
		};

		resolvableProperties.put("edate", edate);
		resolvableProperties.put("editdate", edate);
		resolvableProperties.put("edittimestamp", new Property(new String[] { "edate"}) {
			public Object get(UserGroup group, String key) {
				return group.getEDate().getTimestamp();
			}
		});
	}

	/**
	 * Reduce the given list of usergroups to not contain any pairs where one
	 * group is direct or indirect descendant of the other.
	 * @param list list of user groups to reduce
	 * @param type which group members shall be kept.
	 * @return reduced list
	 * @throws NodeException
	 */
	public static List<UserGroup> reduceUserGroups(List<UserGroup> list, ReductionType type) throws NodeException {
		List<UserGroup> reducedList = new Vector<UserGroup>();

		if (type == ReductionType.CHILD) {
			// add all groups to the list
			reducedList.addAll(list);

			// iterate through all groups, and remove all parents from the list
			for (UserGroup group : list) {
				reducedList.removeAll(group.getParents());
			}
		} else if (type == ReductionType.PARENT) {
			// iterate over the groups
			for (UserGroup group : list) {
				// check whether the group shall be added to the reduced list.
				// It shall not be added, if one of its parent groups is also in
				// the list

				// get the parents of the group
				List<UserGroup> parents = group.getParents();

				// keep the parents, which are in the original group list
				parents.retainAll(list);

				// only add the group to the reduced list, if no parents are left (where in the original list)
				if (parents.size() == 0) {
					reducedList.add(group);
				}
			}
		}
		return reducedList;
	}

	/**
	 * Create an instance of a group
	 * @param id id of the group
	 * @param info object info
	 */
	protected UserGroup(Integer id, NodeObjectInfo info) {
		super(id, info);
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

	/**
	 * Get group name
	 * @return group name
	 */
	public abstract String getName();

	/**
	 * Set the group name
	 * @param name group name
	 * @throws ReadOnlyException
	 */
	public void setName(String name) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the group description
	 * @return description
	 */
	public abstract String getDescription();

	/**
	 * Set the group description
	 * @param description group description
	 * @throws ReadOnlyException
	 */
	public void setDescription(String description) throws ReadOnlyException {
		failReadOnly();
	}

	/**
	 * Get the mother group ID
	 * @return mother groud ID
	 */
	public abstract int getMotherId();

	/**
	 * Set the mother group ID
	 * @param motherId mother group ID
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	public void setMotherId(int motherId) throws ReadOnlyException, NodeException {
		failReadOnly();
	}

	/**
	 * Get the mother group (or null for the root group)
	 * @return mother group or null
	 * @throws NodeException
	 */
	public abstract UserGroup getMother() throws NodeException;

	/**
	 * Get the list of all supergroups, starting with the direct mother.
	 * @return list of all supergroups
	 * @throws NodeException
	 */
	public List<UserGroup> getParents() throws NodeException {
		List<UserGroup> parents = new Vector<UserGroup>();
		UserGroup parent = getMother();

		while (parent != null) {
			parents.add(parent);
			parent = parent.getMother();
		}

		return parents;
	}

	/**
	 * Get child groups
	 * @return list of child groups
	 * @throws NodeException
	 */
	public abstract List<UserGroup> getChildGroups() throws NodeException;

	/**
	 * Get the IDs of group members
	 * @return list of member IDs
	 * @throws NodeException
	 */
	public abstract List<Integer> getMemberIds() throws NodeException;

	/**
	 * Get the group members
	 * @return group members
	 * @throws NodeException
	 */
	public abstract List<SystemUser> getMembers() throws NodeException;

	/**
	 * get the group's creator
	 * @return group's creator
	 * @throws NodeException
	 */
	public abstract SystemUser getCreator() throws NodeException;

	/**
	 * get the group's creation date as a unix timestamp
	 * @return unix timestamp of the group's creation date
	 */
	public abstract ContentNodeDate getCDate();

	/**
	 * get the group's editor
	 * @return systemuser who edited this group
	 * @throws NodeException
	 */
	public abstract SystemUser getEditor() throws NodeException;

	/**
	 * get the user's last edit date as a unix timestamp
	 * @return unix timestamp of the user's last edit date
	 */
	public abstract ContentNodeDate getEDate();

	/**
	 * Check whether the given user is member of this group
	 * @param user user to check
	 * @return true when the user is member of the group, false if not
	 * @throws NodeException
	 */
	public boolean isMember(SystemUser user) throws NodeException {
		List<SystemUser> members = getMembers();

		return members.contains(user);
	}

	/**
	 * Move this group into a new target group
	 * @param target target group
	 * @throws NodeException
	 */
	public void move(UserGroup target) throws NodeException {
		failReadOnly();
	}

	/**
	 * Return a set containing this group and all its (direct and indirect) subgroups
	 * @return set of groups
	 * @throws NodeException
	 */
	protected Set<UserGroup> allGroups() throws NodeException {
		Set<UserGroup> groups = new HashSet<>();
		groups.add(this);
		for (UserGroup child : getChildGroups()) {
			groups.addAll(child.allGroups());
		}
		return groups;
	}

	/**
	 * Enum for the reduction type used in method {@link UserGroup#reduceUserGroups(List, com.gentics.contentnode.object.UserGroup.ReductionType)}
	 */
	public static enum ReductionType {
		CHILD, PARENT
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
		public abstract Object get(UserGroup object, String key);
	}
}
