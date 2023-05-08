package com.gentics.api.tests.genticsimagestore.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.imagestore.GenticsImageStoreRequest;
import com.gentics.api.imagestore.RequestDecorator;
import com.gentics.api.lib.cache.PortalCache;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.lib.image.GenticsImageStore;
import com.gentics.api.tests.genticsimagestore.servlet.ImageHandler;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.experimental.categories.Category;

/**
 * Test cases for the GenticsImageStoreServlet
 */
@Category(BaseLibTest.class)
public class GenticsImageStoreServletTest {
	/**
	 * Servlet runner
	 */
	protected ServletRunner runner;

	/**
	 * Http Server that will host the images
	 */
	protected HttpServer server;

	/**
	 * Image Handler instance
	 */
	protected ImageHandler imageHandler;

	/**
	 * Port the server will listen to.
	 * Note: when this is changed, also change the config in the web.xml
	 */
	public final static int SERVER_PORT = 7890;

	/**
	 * Valid image filename 1
	 */
	public final static String IMAGE1 = "flower.gif";

	/**
	 * Valid image filename 2
	 */
	public final static String IMAGE2 = "bild2.jpg";

	/**
	 * Valid image filename 3
	 */
	public final static String IMAGE3 = "konzept.jpg";

	/**
	 * Header name
	 */
	public final static String HTTP_HEADER_NAME = "Http_client_ip";

	/**
	 * Header value
	 */
	public final static String HTTP_HEADER_VALUE = "some ip address";

	@Before
	public void setUp() throws Exception {
		imageHandler = new ImageHandler();
		server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
		server.createContext("/Portal.Node", imageHandler);
		server.setExecutor(null);
		server.start();

        runner = new ServletRunner(new File(getClass().getResource("WEB-INF/web.xml").toURI()), "/Portal.Node");
	}

	@After
	public void tearDown() throws Exception {
		if (runner != null) {
			runner.shutDown();
		}
		if (server != null) {
			server.stop(1);
		}
		PortalCache.getCache(GenticsImageStore.CACHEREGION).clear();
	}

	/**
	 * Perform a GET request to Portal.Node with the given request parameters and asserts the expected output (if any given)
	 * @param client client
	 * @param url url
	 * @param requestParameters map holding the request parameters
	 * @param expectedOutput expected output or null to not assert the output
	 * @return the response
	 * @throws Exception
	 */
	protected WebResponse performRequest(ServletUnitClient client, String url, Map<String, String> requestParameters, byte[] expectedOutput) throws Exception {
	    WebRequest request = new GetMethodWebRequest( "http://test.meterware.com" + url );
	    request.setHeaderField("host", "testhost");
	    if (requestParameters != null) {
	    	for (Map.Entry<String, String> entry : requestParameters.entrySet()) {
				request.setParameter(entry.getKey(), entry.getValue());
			}
	    }
	    WebResponse response = client.getResponse( request );
	    assertNotNull( "No response received", response );
	    if (expectedOutput != null) {
	    	ByteArrayOutputStream data = new ByteArrayOutputStream();
	    	InputStream in = response.getInputStream();
	    	byte[] buffer = new byte[4096];
	    	int read = 0;
	    	while ((read = in.read(buffer)) > 0) {
	    		data.write(buffer, 0, read);
	    	}
	    	in.close();
	    	assertEquals( "requested resource", expectedOutput, data.toByteArray());
	    }
	    return response;
	}

	/**
	 * Read the file from the given resource path
	 * @param path path to the file resource
	 * @return file as byte array
	 * @throws Exception
	 */
	protected byte[] readFile(String path) throws Exception {
		InputStream fileStream = getClass().getResourceAsStream(path);
		int read = 0;
		byte[] buffer = new byte[4096];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while ((read = fileStream.read(buffer)) > 0) {
			out.write(buffer, 0, read);
		}
		fileStream.close();
		return out.toByteArray();
	}

	/**
	 * Test a normal resize request
	 * @throws Exception
	 */
	@Test
	public void testResizeRequest() throws Exception {
		ServletUnitClient client = runner.newClient();

		performRequest(client, "/Portal.Node/gis/120/500/prop/Portal.Node/image/" + IMAGE1, null, null);
	}

	/**
	 * Test if content disposition header contains original file name
	 * @throws Exception
	 */
	@Test
	public void testFileName() throws Exception {
		ServletUnitClient client = runner.newClient();

		WebResponse response = performRequest(client, "/Portal.Node/gis/120/500/prop/Portal.Node/image/" + IMAGE1, null, null);

		String headerField = response.getHeaderField("Content-Disposition");
		assertEquals("Content-Dispositon Header", "inline; filename=flower.gif", headerField);
	}

	/**
	 * Test if content disposition header contains original file name, when using query parameters
	 * @throws Exception
	 */
	@Test
	public void testFileNameWithQueryParameters() throws Exception {
		ServletUnitClient client = runner.newClient();
		Map<String, String> params = new HashMap<>();
		params.put("x", "1");

		WebResponse response = performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE1, params, null);

		String headerField = response.getHeaderField("Content-Disposition");
		assertEquals("Content-Dispositon Header", "inline; filename=flower.gif", headerField);
	}

	/**
	 * Test an unvalidated request to a secured GenticsImageStoreServlet
	 * @throws Exception
	 */
	@Test
	public void testUnvalidatedSecuredRequest() throws Exception {
		ServletUnitClient client = runner.newClient();

		try {
			performRequest(client, "/Portal.Node/secretgis/120/500/prop/Portal.Node/image/" + IMAGE1, null, null);
			fail("Request succeeded, but was expected to fail");
		} catch (HttpException e) {
			assertEquals("Response code", 403, e.getResponseCode());
		}
	}

	/**
	 * Test a wrong validated request to a secured GenticsImageStoreServlet
	 * @throws Exception
	 */
	@Test
	public void testWrongValidatedSecuredRequest() throws Exception {
		ServletUnitClient client = runner.newClient();
        Map<String, String> params = new HashMap<String, String>();
        params.put(GenticsImageStore.VALIDATION_PARAMETER, "thisiswrong");

		try {
			performRequest(client, "/Portal.Node/secretgis/120/500/prop/Portal.Node/image/" + IMAGE1, params, null);
			fail("Request succeeded, but was expected to fail");
		} catch (HttpException e) {
			assertEquals("Response code", 403, e.getResponseCode());
		}
	}

	/**
	 * Test a correctly validated request to a secured GenticsImageStoreServlet
	 * @throws Exception
	 */
	@Test
	public void testSecuredRequest() throws Exception {
		ServletUnitClient client = runner.newClient();
		String secret = "totallysecret";
		String path = "/120/500/prop";
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update((secret + path).getBytes("UTF-8"));
        byte[] digest = md.digest();
        String validation = ObjectTransformer.encodeBinary(digest);

        Map<String, String> params = new HashMap<String, String>();
        params.put(GenticsImageStore.VALIDATION_PARAMETER, validation);
		performRequest(client, "/Portal.Node/secretgis/120/500/prop/Portal.Node/image/" + IMAGE1, params, null);
	}

	/**
	 * Tests an attempt to use an incorrectly configured request decorator (configured class does not exist)
	 * @throws Exception
	 */
	@Test
	public void testInvalidDecorator() throws Exception {

		ServletUnitClient client = runner.newClient();

		try {
			performRequest(client, "/Portal.Node/invaliddecorator/120/500/prop/Portal.Node/image/" + IMAGE1, null, null);
			fail("Request succeeded, but was expected to fail");
		} catch (HttpException e) {
			assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getResponseCode());
		}
	}

	/**
	 * Tests using a request decorator that performs no changes on the requests it processes
	 * @throws Exception
	 */
	@Test
	public void testEmptyDecorator() throws Exception {

		ServletUnitClient client = runner.newClient();

		// Set query parameter
		String param = "paramBefore";
		String paramValue = "valueBefore";
		Map<String, String> params = new HashMap<String, String>();
		params.put(param, paramValue);

		performRequest(client, "/Portal.Node/emptydecorator/120/500/prop/Portal.Node/image/" + IMAGE2, params, null);
		assertEquals(param + "=" + paramValue, imageHandler.getQueryString());
		assertEquals(null, imageHandler.getHeaders().get(HTTP_HEADER_NAME));
		assertEquals(0, imageHandler.getCookies().size());
		assertEquals("/Portal.Node/image/" + IMAGE2, imageHandler.getRequestPath());
	}

	/**
	 * Tests using a request decorator that modifies the request being sent
	 * @throws Exception
	 */
	@Test
	public void testModifyingDecorator() throws Exception {

		ServletUnitClient client = runner.newClient();

		// Set query parameter
		String param = "paramBefore";
		String paramValue = "valueBefore";
		Map<String, String> params = new HashMap<String, String>();
		params.put(param, paramValue);

		// Unmodified request
		performRequest(client, "/Portal.Node/emptydecorator/120/500/prop/Portal.Node/image/" + IMAGE3, params, null);
		assertEquals(param + "=" + paramValue, imageHandler.getQueryString());
		assertEquals(null, imageHandler.getHeaders().get(HTTP_HEADER_NAME));
		assertEquals(0, imageHandler.getCookies().size());
		assertEquals("/Portal.Node/image/" + IMAGE3, imageHandler.getRequestPath());

		// Request modified by decorator
		performRequest(client, "/Portal.Node/modifyingdecorator/120/500/prop/Portal.Node/image/" + IMAGE3, params, null);
		assertFalse(imageHandler.getQueryString().equals(param + "=" + paramValue));
		assertEquals(HTTP_HEADER_VALUE, imageHandler.getHeaders().get(HTTP_HEADER_NAME));
		assertEquals(1, imageHandler.getCookies().size());
		assertEquals("/Portal.Node/image/" + IMAGE2, imageHandler.getRequestPath());

	}

	/**
	 * Test if caching works with cacheKeyQueryParameters set and no query parameters
	 * @throws Exception
	 */
	@Test
	public void testQueryParametersNoParams() throws Exception {
		ServletUnitClient client = runner.newClient();
		Map<String, String> params = new HashMap<String, String>();
		// no parameters
		int startCount = imageHandler.getRequestCount();
		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE1, params, null);
		assertEquals(startCount + 1, imageHandler.getRequestCount());
		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE1, params, null);
		assertEquals(startCount + 1, imageHandler.getRequestCount());
	}

	/**
	 * Test if caching is transparent for parameters not mentioned in cacheKeyQueryParameters
	 * @throws Exception
	 */
	@Test
	public void testQueryParametersWrongParams() throws Exception {
		ServletUnitClient client = runner.newClient();
		Map<String, String> params = new HashMap<String, String>();
		// wrong parameters
		params.clear();
		params.put("karl", "lisa");
		int startCount = imageHandler.getRequestCount();
		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE2, params, null);
		assertEquals(startCount + 1, imageHandler.getRequestCount());

		params.clear();
		params.put("lisa", "karl");
		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE2, params, null);
		assertEquals(startCount + 1, imageHandler.getRequestCount());
	}

	/**
	 * Test if images with different parameter values of parameters mentioned in cacheKeyQueryParameters are cached separately
	 * @throws Exception
	 */
	@Test
	public void testQueryParametersRightParams() throws Exception {
		ServletUnitClient client = runner.newClient();
		Map<String, String> params = new HashMap<String, String>();

		// right parameters
		params.clear();
		params.put("x", "1");
		int startCount = imageHandler.getRequestCount();
		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE2, params, null);
		assertEquals(startCount + 1, imageHandler.getRequestCount());

		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE2, params, null);
		assertEquals(startCount + 1, imageHandler.getRequestCount());

		params.clear();
		params.put("x", "2");
		performRequest(client, "/Portal.Node/queryparameters/120/500/prop/Portal.Node/image/" + IMAGE2, params, null);
		assertEquals(startCount + 2, imageHandler.getRequestCount());
	}

	/**
	 * Test if a resize request to an unknown image will get a 404 (not found) response
	 * @throws Exception
	 */
	@Test
	public void testImageNotFound() throws Exception {
		ServletUnitClient client = runner.newClient();
		try {
			performRequest(client, "/Portal.Node/gis/200/200/auto/Portal.Node/image/bla.jpg", null, null);
			fail("Request succeeded, but was expected to fail");
		} catch (HttpException e) {
			assertEquals(HttpStatus.SC_NOT_FOUND, e.getResponseCode());
		}
	}

	/**
	 * Performs no changes on GenticsImageStore requests it receives
	 * @author Taylor
	 */
	public static class EmptyDecorator implements RequestDecorator {

		public void decorateRequest(GenticsImageStoreRequest gisRequest, HttpServletRequest request) {}

	}

	/**
	 * Modifies properties of the GenticsImageStore requests it receives
	 * @author Taylor
	 */
	public static class ModifyingDecorator implements RequestDecorator {

		public void decorateRequest(GenticsImageStoreRequest gisRequest, HttpServletRequest request) {

			// Set new query string
			gisRequest.setQueryString("testparam=anothervalue");

			// Set new cookies
			Cookie cookie = new Cookie("test1", "test2");
			cookie.setDomain("localhost");
			cookie.setPath("/");
			Cookie[] cookies = {cookie};
			gisRequest.setCookies(cookies);

			// Set new headersfdas
			Map<String, String> headers = new HashMap<String, String>(1);
			headers.put(HTTP_HEADER_NAME, HTTP_HEADER_VALUE);
			gisRequest.setHeaders(headers);

			// Set new ImageUri
			gisRequest.setImageUri("http://localhost:" + SERVER_PORT + "/Portal.Node/image/" + IMAGE2);
		}

	}
}
