package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.contentnode.mcp.tools.CountContentTool;
import com.gentics.contentnode.mcp.tools.CreateFormTool;
import com.gentics.contentnode.mcp.tools.DeleteFormTool;
import com.gentics.contentnode.mcp.tools.FindSimilarTool;
import com.gentics.contentnode.mcp.tools.GetFormTool;
import com.gentics.contentnode.mcp.tools.ListFormTypesTool;
import com.gentics.contentnode.mcp.tools.PublishFormTool;
import com.gentics.contentnode.mcp.tools.SearchContentTool;
import com.gentics.contentnode.mcp.tools.SearchMcpToolService;
import com.gentics.contentnode.mcp.tools.UpdateFormTool;

/**
 * Checks the tools of this module against the contract, as {@link McpContractTest} does for the tools of cms-core.
 */
public class McpModuleToolsContractTest {
	@BeforeClass
	public static void loadContract() throws IOException {
		McpContractTest.loadContract();
	}

	@Test
	public void testFormToolsConform() {
		List<String> mismatches = new ArrayList<>();
		for (AbstractMcpTool tool : List.of(new GetFormTool(), new ListFormTypesTool(), new CreateFormTool(),
				new UpdateFormTool(), new DeleteFormTool(), new PublishFormTool())) {
			mismatches.addAll(McpContractTest.mismatches(tool.tool()));
		}
		assertThat(mismatches).isEmpty();
	}

	@Test
	public void testSearchToolsConform() {
		List<AbstractMcpTool> tools = new SearchMcpToolService().tools();
		assertThat(tools).extracting(tool -> tool.tool().name()).containsExactlyInAnyOrder("search_content",
				"count_content", "find_similar");
		List<String> mismatches = new ArrayList<>();
		for (AbstractMcpTool tool : List.of(new SearchContentTool(), new CountContentTool(), new FindSimilarTool())) {
			mismatches.addAll(McpContractTest.mismatches(tool.tool()));
		}
		assertThat(mismatches).isEmpty();
	}
}
