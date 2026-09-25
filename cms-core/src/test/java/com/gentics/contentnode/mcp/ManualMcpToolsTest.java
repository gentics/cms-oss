package com.gentics.contentnode.mcp;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Unit test for {@link ManualMcpTools}.
 */
public class ManualMcpToolsTest {
	@Test
	public void testRegisterAllRegistersPageLoadTool() {
		McpSyncServer server = mock(McpSyncServer.class);

		ManualMcpTools.registerAll(server);

		verify(server).addTool(argThat((SyncToolSpecification spec) -> "page_load".equals(spec.tool().name())));
	}
}
