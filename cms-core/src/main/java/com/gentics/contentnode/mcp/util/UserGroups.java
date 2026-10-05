package com.gentics.contentnode.mcp.util;

import java.util.List;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.resource.GroupResource;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.parameter.EmbedParameterBean;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

/**
 * Reads users, groups and the groups of a user for the admin tools
 */
public final class UserGroups {
	private UserGroups() {
	}

	/**
	 * Get the groups of a user the caller can view ({@code UserResourceImpl#groups})
	 * @param resource guarded user resource
	 * @param userId user ID
	 * @return group refs
	 * @throws Exception if the groups cannot be read
	 */
	public static List<ObjectRef> refs(UserResource resource, int userId) throws Exception {
		return ListResponses.items(resource.groups(Integer.toString(userId), new FilterParameterBean(),
				new SortParameterBean(), new PagingParameterBean(), new PermsParameterBean())).stream()
				.map(ObjectRef::forGroup).toList();
	}

	/**
	 * Load a user the caller can view ({@code UserResourceImpl#get}, which throws instead of returning a failure)
	 * @param resource guarded user resource
	 * @param userId user ID
	 * @return user ref
	 * @throws Exception if the user cannot be loaded
	 */
	public static ObjectRef userRef(UserResource resource, int userId) throws Exception {
		return ObjectRef.forUser(resource.get(Integer.toString(userId), new EmbedParameterBean()).getUser());
	}

	/**
	 * Load a group the caller can view ({@code GroupResourceImpl#get}, which throws instead of returning a failure)
	 * @param resource guarded group resource
	 * @param groupId group ID
	 * @return group ref
	 * @throws Exception if the group cannot be loaded
	 */
	public static ObjectRef groupRef(GroupResource resource, int groupId) throws Exception {
		return ObjectRef.forGroup(resource.get(Integer.toString(groupId), new PermsParameterBean()).getGroup());
	}
}
