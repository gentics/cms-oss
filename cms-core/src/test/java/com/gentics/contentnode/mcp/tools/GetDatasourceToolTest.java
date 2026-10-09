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

import com.gentics.contentnode.mcp.tools.GetDatasourceTool.Entry;
import com.gentics.contentnode.mcp.tools.GetDatasourceTool.Result;
import com.gentics.contentnode.mcp.tools.ListDatasourcesTool.DatasourceInfo;
import com.gentics.contentnode.mcp.util.Slice;
import com.gentics.contentnode.rest.model.Construct;
import com.gentics.contentnode.rest.model.DatasourceEntryModel;
import com.gentics.contentnode.rest.model.DatasourceType;

/**
 * Unit tests for {@link GetDatasourceTool}. Loading a datasource is covered by the live check.
 */
public class GetDatasourceToolTest {
	private final GetDatasourceTool tool = new GetDatasourceTool();

	private static final DatasourceInfo DATASOURCE = new DatasourceInfo(7, "A547.7", "Colors",
			DatasourceType.STATIC, null);

	private static final List<DatasourceEntryModel> ENTRIES = List.of(entry(1, "red", "Red"), entry(2, "green",
			"Green"), entry(3, "blue", "Blue"));

	@Test
	public void testDefinition() {
		assertDefinition(tool, "get_datasource", "Get datasource", "id");
	}

	@Test
	public void testInputValidation() {
		assertAccepted(tool, Map.of("id", 7));
		assertAccepted(tool, Map.of("id", 7, "includeEntries", false, "size", 10, "from", 10));
		assertRejected(tool, Map.of());
		assertRejected(tool, Map.of("id", 7, "size", 201));
	}

	@Test
	public void testRejectsCallWithoutCredentials() {
		assertRejectsWithoutCredentials(tool, Map.of("id", 7));
	}

	@Test
	public void testEntries() {
		Construct construct = new Construct().setId(12).setName("Teaser");

		Result result = GetDatasourceTool.result(DATASOURCE, ENTRIES, List.of(construct), true, Slice.of(1, 25, 3));

		assertThat(result.total()).isEqualTo(3);
		assertThat(result.entries()).containsExactly(new Entry(2, null, "green", "Green"), new Entry(3, null, "blue",
				"Blue"));
		assertThat(result.constructRefs()).extracting(ref -> ref.id()).containsExactly(12);
		assertValidOutput(tool, result);
	}

	@Test
	public void testWithoutEntries() {
		Result result = GetDatasourceTool.result(DATASOURCE, ENTRIES, List.of(), false, Slice.of(0, 25, 3));

		assertThat(result.total()).isEqualTo(3);
		assertThat(result.entries()).isNull();
	}

	private static DatasourceEntryModel entry(int id, String key, String value) {
		return new DatasourceEntryModel().setId(id).setKey(key).setValue(value);
	}
}
