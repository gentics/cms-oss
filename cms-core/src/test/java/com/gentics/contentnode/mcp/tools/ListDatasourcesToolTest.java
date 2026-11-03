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
import com.gentics.contentnode.mcp.tools.ListDatasourcesTool.DatasourceInfo;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Datasource;
import com.gentics.contentnode.rest.model.DatasourceType;

/**
 * Unit tests for {@link ListDatasourcesTool}. Listing and counting entries is covered by the live check.
 */
public class ListDatasourcesToolTest {
	private final ListDatasourcesTool tool = new ListDatasourcesTool();

	@Test
	public void testDefinition() {
		assertDefinition(tool, "list_datasources", "List datasources");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of());
		assertAccepted(tool, Map.of("q", "colors", "type", "STATIC", "from", 25));
		assertRejected(tool, Map.of("type", "DYNAMIC"));
		assertRejected(tool, Map.of("size", 0));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of());
	}

	@Test
	public void testOutput() {
		Datasource datasource = new Datasource().setId(7).setGlobalId("A547.7").setName("Colors")
				.setType(DatasourceType.STATIC);

		assertThat(DatasourceInfo.of(datasource, 3)).isEqualTo(new DatasourceInfo(7, "A547.7", "Colors",
				DatasourceType.STATIC, 3));
		assertValidOutput(tool, ListResult.of(Slice.of(0, 25, 1), List.of(DatasourceInfo.of(datasource, 3))));
	}
}
