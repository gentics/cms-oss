package com.gentics.contentnode.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test case for serving the Workspace UI under /workspace/.
 * The UI is unpacked from the workspace-ui artifact, so its dist/ must have been built with npm before the Maven build.
 */
public class WorkspaceUiServingTest {
	/**
	 * Test context
	 */
	private static DBTestContext testContext = new DBTestContext();

	/**
	 * OSSRunner context
	 */
	private static OSSRunnerContext ossRunnerContext = new OSSRunnerContext();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(testContext).around(ossRunnerContext);

	/**
	 * Test that the index.html of the Workspace UI is served
	 * @throws HttpException
	 * @throws IOException
	 */
	@Test
	public void testIndex() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		GetMethod getIndex = new GetMethod(String.format("http://localhost:%d/workspace/index.html", OSSRunner.getPort()));

		int responseCode = client.executeMethod(getIndex);
		assertThat(responseCode).as("Response code").isEqualTo(200);
		assertThat(getIndex.getResponseBodyAsString()).as("Response body").contains("<div id=\"root\"></div>");
	}
}
