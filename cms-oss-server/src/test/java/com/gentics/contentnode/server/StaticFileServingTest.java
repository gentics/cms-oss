package com.gentics.contentnode.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.contentnode.runtime.ConfigurationValue;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test case for cache-control headers when serving static files
 */
public class StaticFileServingTest {
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
	 * Static setup. Create a devtool package containing a single static file
	 * @throws IOException
	 */
	@BeforeClass
	public static void setupOnce() throws IOException {
		File packagesBaseDir = new File(ConfigurationValue.PACKAGES_PATH.get());
		File testPackageDir = new File(packagesBaseDir, "caching");
		File filesDir = new File(testPackageDir, "files");
		filesDir.mkdirs();
		assertThat(filesDir).as(String.format("Test files directory %s", filesDir.getAbsolutePath())).isDirectory();

		FileUtils.write(new File(filesDir, "testfile.html"), "<html></html>", "UTF-8");
	}

	/**
	 * Test response headers when getting the static file
	 * @throws HttpException
	 * @throws IOException
	 */
	@Test
	public void testCaching() throws HttpException, IOException {
		HttpClient client = new HttpClient();
		GetMethod getTestfile = new GetMethod(String.format("http://localhost:%d/static/caching/files/testfile.html", OSSRunner.getPort()));

		int responseCode = client.executeMethod(getTestfile);
		assertThat(responseCode).as("Response code").isEqualTo(200);

		Header etagResponseHeader = getTestfile.getResponseHeader("etag");
		assertThat(etagResponseHeader).as("etag response header").isNotNull();
		assertThat(etagResponseHeader.getValue()).as("etag response header value").isNotEmpty();

		Header cacheControleResponseHeader = getTestfile.getResponseHeader("cache-control");
		assertThat(cacheControleResponseHeader).as("cache-control response header").isNotNull().hasFieldOrPropertyWithValue("value", "no-cache");
	}
}
