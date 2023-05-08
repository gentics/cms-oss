package com.gentics.api.tests.genticsimagestore.servlet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.imagestore.GenticsImageStoreServlet;
import com.gentics.node.testutils.ServletAppContext;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the GenticsImageStoreServlet
 */
@Category(BaseLibTest.class)
public class GISServletTest {
	@SuppressWarnings("unchecked")
	@ClassRule
	public static ServletAppContext context = new ServletAppContext()
			.servlet("/gis/*", GenticsImageStoreServlet.class, Pair.of("portalBasePath", "/image"), Pair.of("loadTimeout", "3000"))
			.servlet("/image/*", ImageProviderServlet.class);

	/**
	 * Test getting the image
	 * @throws HttpException
	 * @throws IOException
	 */
	@Test
	public void testGet() throws HttpException, IOException {
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(context.getBaseUri() + "gis/100/auto/prop/image/flower.gif");

		int responseStatus = httpClient.executeMethod(getMethod);
		assertEquals("Check response code", 200, responseStatus);
	}

	/**
	 * Test posting to the image (which will just get the image)
	 * @throws HttpException
	 * @throws IOException
	 */
	@Test
	public void testPost() throws HttpException, IOException {
		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(context.getBaseUri() + "gis/100/auto/prop/image/flower.gif");

		int responseStatus = httpClient.executeMethod(postMethod);
		assertEquals("Check response code", 200, responseStatus);
	}

	/**
	 * Test timeout. Test will fail, if the request takes longer than 5 seconds. The request to the resource would wait for 60 seconds, but the GenticsImageStoreServlet has a timeout of 3 seconds.
	 * @throws HttpException
	 * @throws IOException
	 */
	@Test(timeout = 5000)
	public void testTimeout() throws HttpException, IOException {
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(context.getBaseUri() + "gis/100/auto/prop/image/wait/60000");

		// since we had a timeout, we expect an internal server error
		int responseStatus = httpClient.executeMethod(getMethod);
		assertEquals("Check response code", 500, responseStatus);
	}
}
