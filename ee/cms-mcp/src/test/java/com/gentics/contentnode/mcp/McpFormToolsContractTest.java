package com.gentics.contentnode.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.contentnode.mcp.tools.CreateFormTool;
import com.gentics.contentnode.mcp.tools.DeleteFormTool;
import com.gentics.contentnode.mcp.tools.GetFormTool;
import com.gentics.contentnode.mcp.tools.ListFormTypesTool;
import com.gentics.contentnode.mcp.tools.PublishFormTool;
import com.gentics.contentnode.mcp.tools.UpdateFormTool;

/**
 * Checks the Forms tools against the contract, as {@link McpContractTest} does for the tools of cms-core.
 */
public class McpFormToolsContractTest {
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
}
