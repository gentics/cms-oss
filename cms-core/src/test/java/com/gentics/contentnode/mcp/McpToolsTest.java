package com.gentics.contentnode.mcp;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Unit test for {@link McpTools}.
 */
public class McpToolsTest {
	@Test
	public void testRegisterAllRegistersListNodesTool() {
		McpSyncServer server = mock(McpSyncServer.class);

		McpTools.registerAll(server);

		verify(server).addTool(argThat((SyncToolSpecification spec) -> "list_nodes".equals(spec.tool().name())));
	}

	@Test
	public void testRegisterAllRegistersUpdatePagePropertiesTool() {
		McpSyncServer server = mock(McpSyncServer.class);

		McpTools.registerAll(server);

		verify(server).addTool(
				argThat((SyncToolSpecification spec) -> "update_page_properties".equals(spec.tool().name())));
	}
}
