package com.gentics.contentnode.mcp.tools;

import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertAccepted;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertDefinition;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejected;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertRejectsWithoutCredentials;
import static com.gentics.contentnode.mcp.tools.ToolAssertions.assertValidOutput;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.contentnode.mcp.model.ListResult;
import com.gentics.contentnode.mcp.tools.ListPackagesTool.Counts;
import com.gentics.contentnode.mcp.tools.ListPackagesTool.PackageInfo;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.devtools.Package;

/**
 * Unit tests for {@link ListPackagesTool}. Listing and the feature check are covered by the live check.
 */
public class ListPackagesToolTest {
	private final ListPackagesTool tool = new ListPackagesTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_packages", "List packages");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("q", "genaix", "size", 10, "from", 10));
		assertRejected(tool, Map.of("size", 201));
		assertRejected(tool, Map.of("name", "genaix"));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testOutput() {
		Package pkg = new Package();
		pkg.setName("genaix");
		pkg.setDescription("GenAIx constructs");
		pkg.setConstructs(3);
		pkg.setTemplates(1);

		PackageInfo info = PackageInfo.of(pkg);

		assertThat(info).isEqualTo(new PackageInfo("genaix", "GenAIx constructs", new Counts(3, 1, 0, 0, 0, 0)));
		assertValidOutput(tool, ListResult.of(Slice.of(0, 25, 1), List.of(info)));
	}
}
