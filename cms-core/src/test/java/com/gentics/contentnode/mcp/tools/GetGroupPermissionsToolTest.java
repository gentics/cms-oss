package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.GetGroupPermissionsTool.GroupPermission;
import com.gentics.contentnode.mcp.tools.GetGroupPermissionsTool.GroupRole;
import com.gentics.contentnode.mcp.tools.GetGroupPermissionsTool.Result;
import com.gentics.contentnode.rest.model.perm.PermType;
import com.gentics.contentnode.rest.model.perm.RoleItem;
import com.gentics.contentnode.rest.model.perm.TypePermissionItem;

/**
 * Unit tests for {@link GetGroupPermissionsTool}. Reading the permissions, the node's root folder and the permission
 * checks are covered by the live check.
 */
public class GetGroupPermissionsToolTest {
	private final GetGroupPermissionsTool tool = new GetGroupPermissionsTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_group_permissions", "Get group permissions", "groupId", "type");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("groupId", 7, "type", "admin"));
		assertAccepted(tool, Map.of("groupId", 7, "type", "node", "instanceId", 1));
		assertRejected(tool, Map.of("groupId", 7, "type", "useradmin"));
		assertRejected(tool, Map.of("groupId", 7));
		assertRejected(tool, Map.of("groupId", 0, "type", "admin"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("groupId", 7, "type", "admin"));
	}

	@Test
	public void testFolderBoundTypesAreRejected() {
		for (String type : GetGroupPermissionsTool.FOLDER_BOUND_TYPES) {
			assertThatThrownBy(() -> tool.invoke(Map.of("groupId", 7, "type", type, "instanceId", 3), Optional.empty()))
					.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'folder'")
					.hasMessageContaining("'node'");
		}
	}

	@Test
	public void testOutput() {
		TypePermissionItem item = new TypePermissionItem().setType(PermType.createitems).setLabel("Create")
				.setCategory("Pages, images and files").setValue(true).setEditable(false);
		RoleItem role = new RoleItem().setId(2).setLabel("Translator").setValue(false);

		assertThat(GroupPermission.of(item))
				.isEqualTo(new GroupPermission("createitems", "Create", null, "Pages, images and files", true, false));
		assertThat(GroupRole.of(role)).isEqualTo(new GroupRole(2, "Translator", false));

		Result result = new Result(ObjectRef.of(Type.GROUP, 7), "node", ObjectRef.of(Type.NODE, 1),
				List.of(GroupPermission.of(item)), List.of(GroupRole.of(role)));
		assertValidOutput(tool, result);
		assertThat(assertValidOutput(tool, new Result(ObjectRef.of(Type.GROUP, 7), "admin", null, List.of(), null)))
				.containsOnlyKeys("groupRef", "type", "permissions");
	}
}
