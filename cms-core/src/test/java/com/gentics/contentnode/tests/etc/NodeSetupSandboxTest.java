package com.gentics.contentnode.tests.etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodeSetup;
import com.gentics.contentnode.etc.NodeSetupValuePair;
import com.gentics.contentnode.testutils.DBTestContext;

public class NodeSetupSandboxTest {
	
	@Rule
	public DBTestContext testContext = new DBTestContext();

	@Test
	public void testGetNodeSetupValue() throws SQLException, NodeException {
		NodeSetupValuePair valuePair = NodeSetup.getKeyValue(NodeSetup.NODESETUP_KEY.globalprefix);

		assertNotNull(valuePair);
		assertNotNull(valuePair.getTextValue());
		assertEquals("The version nodesetup int value should always be 0.", 0, valuePair.getIntValue());
	}

}
