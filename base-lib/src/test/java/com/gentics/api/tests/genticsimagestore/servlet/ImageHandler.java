package com.gentics.api.tests.genticsimagestore.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;

import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * 
 */
public class ImageHandler implements HttpHandler {
	
	/**
	 * Counts the number of requests handled by this instance
	 */
	private int requestCount;

	/**
	 * Query string sent in the request
	 */
	private String queryString;
	
	/**
	 * Cookies sent with the request
	 */
	private List<String> cookies = new ArrayList<String>();

	/**
	 * headers sent in the request
	 */
	private Map<String, String> headers = new HashMap<String, String>(1);
	
	/**
	 * The URI requested
	 */
	private String requestPath;

	/* (non-Javadoc)
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	public void handle(HttpExchange t) throws IOException {
		
		requestCount++;

		// get all headers
		Headers requestHeaders = t.getRequestHeaders();
		Set<String> keySet = requestHeaders.keySet();
		Iterator<String> iter = keySet.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			List<String> values = requestHeaders.get(key);
			headers.put(key, values.get(0).toString());
			
			// Cookies are sent as headers with the key "Cookies"
			if (key.equals("Cookie")) {
				cookies.addAll(values);
			}
		}

		requestPath = t.getRequestURI().toString();
		int qmIndex = requestPath.indexOf('?');
		if (qmIndex >= 0) {
			queryString = requestPath.substring(qmIndex+1, requestPath.length());
			requestPath = requestPath.substring(0, qmIndex);
		}
		if (requestPath.startsWith("/Portal.Node/image/")) {
			try {
				String fileName = requestPath.substring("/Portal.Node/image/".length());
				InputStream in = GenericTestUtils.getPictureResource(fileName);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				FileUtil.inputStreamToOutputStream(in, out);
				in.close();
				byte[] fileData = out.toByteArray();
				OutputStream responseStream = t.getResponseBody();
				t.sendResponseHeaders(200, fileData.length);
				responseStream.write(fileData);
				responseStream.close();
			} catch (IOException e) {
				throw e;
			} catch (AssertionError e) {
				t.sendResponseHeaders(HttpStatus.SC_NOT_FOUND, 0);
			}
		} else {
			throw new IOException();
		}
	}

	/**
	 * Read the file from the given resource path
	 * @param path path to the file resource
	 * @return file as byte array
	 * @throws Exception
	 */
	protected byte[] readFile(String path) throws IOException {
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
	 * Get the query string
	 * @return query string
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * Get the received cookies
	 * @return cookies
	 */
	public List<String> getCookies() {
		return cookies;
	}

	/**
	 * Get the received headers
	 * @return headers
	 */
	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Get the received request path
	 * @return request path
	 */
	public String getRequestPath() {
		return requestPath;
	}

	/**
	 * Get the number of requests handled by this instance
	 * @return number of requests
	 */
	public int getRequestCount() {
		return requestCount;
	}
}
