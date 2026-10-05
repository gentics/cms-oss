package com.gentics.contentnode.mcp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.rest.model.Group;
import com.gentics.contentnode.rest.model.response.GroupList;
import com.gentics.contentnode.rest.resource.UserResource;

/**
 * Unit tests for {@link UserGroups}
 */
public class UserGroupsTest {
	@Test
	public void testRefs() throws Exception {
		Group group = new Group();
		group.setId(7);
		group.setName("Editors");
		GroupList list = new GroupList();
		list.setItems(List.of(group));
		UserResource resource = mock(UserResource.class);
		when(resource.groups(eq("35"), any(), any(), any(), any())).thenReturn(list);

		assertThat(UserGroups.refs(resource, 35)).containsExactly(new ObjectRef(Type.GROUP, 7, null, null, "Editors",
				null, null, null, null));
		assertThat(UserGroups.refs(resource, 36)).isEmpty();
	}
}
