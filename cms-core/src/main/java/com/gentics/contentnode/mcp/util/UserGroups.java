package com.gentics.contentnode.mcp.util;

import java.util.List;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.rest.resource.UserResource;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PagingParameterBean;
import com.gentics.contentnode.rest.resource.parameter.PermsParameterBean;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;

/**
 * Reads the groups of a user
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
}
