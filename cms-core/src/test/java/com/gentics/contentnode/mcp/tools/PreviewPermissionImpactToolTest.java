package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ObjectRef;
import com.gentics.contentnode.mcp.model.ObjectRef.Type;
import com.gentics.contentnode.mcp.tools.PreviewPermissionImpactTool.PermissionChange;
import com.gentics.contentnode.mcp.tools.PreviewPermissionImpactTool.PermissionGrant;
import com.gentics.contentnode.mcp.tools.PreviewPermissionImpactTool.Result;
import com.gentics.contentnode.mcp.tools.PreviewPermissionImpactTool.Scope;

/**
 * Unit tests for {@link PreviewPermissionImpactTool}: the composition from fixtures. Reading groups, permissions and
 * restrictions is covered by the live check.
 */
public class PreviewPermissionImpactToolTest {
	private static final ObjectRef NODE = ObjectRef.of(Type.NODE, 1);

	private static final Scope USERADMIN = new Scope("useradmin", null, "useradmin", Set.of());

	private static final Scope PAGE = new Scope("page", NODE, "node",
			PreviewPermissionImpactTool.NODE_PERMISSIONS.get("page"));

	/**
	 * Group 7 may view users, group 9 may also create them, both may read pages on node 1
	 */
	private static final Map<Integer, Map<String, Map<String, Boolean>>> PERMISSIONS = Map.of(
			7, Map.of("useradmin", perms("read", true, "createuser", false), "node:1",
					perms("read", true, "readitems", true, "publishpages", false)),
			9, Map.of("useradmin", perms("read", true, "createuser", true), "node:1",
					perms("read", false, "readitems", true, "publishpages", true)));

	private final PreviewPermissionImpactTool tool = new PreviewPermissionImpactTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "preview_permission_impact", "Preview permission impact", "userId");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("userId", 35));
		assertAccepted(tool, Map.of("userId", 35, "addGroupIds", List.of(9), "removeGroupIds", List.of(7), "types",
				List.of("admin", "page"), "nodeIds", List.of(1)));
		assertRejected(tool, Map.of("userId", 35, "types", List.of("useradmin")));
		assertRejected(tool, Map.of("userId", 35, "addGroupIds", List.of(9, 9)));
		assertRejected(tool, Map.of("addGroupIds", List.of(9)));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("userId", 35));
	}

	@Test
	public void testAddingAGroupMergesWithOr() {
		List<PermissionGrant> before = compose(List.of(7), Map.of());
		List<PermissionGrant> after = compose(List.of(7, 9), Map.of());

		assertThat(before).contains(new PermissionGrant("useradmin", null, "createuser", false),
				new PermissionGrant("page", NODE, "readitems", true));
		assertThat(PreviewPermissionImpactTool.diff(after, before)).containsExactly(
				new PermissionChange("useradmin", null, "createuser"),
				new PermissionChange("page", NODE, "publishpages"));
		assertThat(PreviewPermissionImpactTool.diff(before, after)).isEmpty();
	}

	@Test
	public void testScopeKeepsOnlyItsPermissions() {
		assertThat(compose(List.of(7), Map.of())).extracting(PermissionGrant::permission)
				.containsExactly("read", "createuser", "readitems", "publishpages");
	}

	@Test
	public void testRemovingTheOnlyGroupRemovesEverything() {
		List<PermissionGrant> before = compose(List.of(7), Map.of());
		List<PermissionGrant> after = compose(List.of(), Map.of());

		assertThat(after).isNotEmpty().noneMatch(PermissionGrant::granted);
		assertThat(PreviewPermissionImpactTool.diff(before, after)).containsExactly(
				new PermissionChange("useradmin", null, "read"), new PermissionChange("page", NODE, "readitems"));
	}

	@Test
	public void testRestrictedMembershipGrantsNothingOnOtherNodes() {
		List<PermissionGrant> grants = compose(List.of(7), Map.of(7, Set.of(2)));

		assertThat(grants).contains(new PermissionGrant("useradmin", null, "read", true),
				new PermissionGrant("page", NODE, "readitems", false));
		assertThat(compose(List.of(7), Map.of(7, Set.of(1, 2))))
				.contains(new PermissionGrant("page", NODE, "readitems", true));
	}

	@Test
	public void testOutput() {
		List<PermissionGrant> before = compose(List.of(7), Map.of());
		List<PermissionGrant> after = compose(List.of(7, 9), Map.of());
		Result result = new Result(ObjectRef.of(Type.USER, 35), before, after,
				PreviewPermissionImpactTool.diff(after, before), List.of(), List.of(ObjectRef.of(Type.GROUP, 7)),
				List.of(ObjectRef.of(Type.GROUP, 7), ObjectRef.of(Type.GROUP, 9)), "composition",
				List.of(PreviewPermissionImpactTool.NODE_WARNING));

		assertValidOutput(tool, result);
		assertThat(PreviewPermissionImpactTool.NODE_WARNING).contains("inheritance", "channels", "roles",
				"languages");
	}

	private static List<PermissionGrant> compose(List<Integer> groupIds, Map<Integer, Set<Integer>> restrictions) {
		return PreviewPermissionImpactTool.compose(List.of(USERADMIN, PAGE), groupIds, PERMISSIONS, restrictions);
	}

	private static Map<String, Boolean> perms(Object... namesAndValues) {
		Map<String, Boolean> perms = new LinkedHashMap<>();
		for (int i = 0; i < namesAndValues.length; i += 2) {
			perms.put((String) namesAndValues[i], (Boolean) namesAndValues[i + 1]);
		}
		return perms;
	}
}
