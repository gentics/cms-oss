package com.gentics.node.tests.formatter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.portalnode.formatter.URLIncludeImp;
import com.gentics.testutils.http.HttpServer;
import org.junit.experimental.categories.Category;

@Category(BaseLibTest.class)
public class UrlIncludeImpTest {

	public final static int HTTPPORT = 0;
	public final static long BLOCKTIME = 8000;
	public HttpServer server;
	public BlockingHttpConnectionHandler handler;
	public int cacheLifeTime = 20;
	public String defaultContent = "This is the default content";
	String url = "";

	@Before
	public void setup() throws IOException {

		server = new HttpServer(HTTPPORT);
		handler = new BlockingHttpConnectionHandler(1000);
		server.setConnectionHandler(handler);
		new Thread(server).start();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		url = getServerURL();
	}

	@After
	public void tearDown() throws IOException {
		server.stop();
	}

	@Test
	public void testIncludeImpTimeout() throws NodeException {

		URLIncludeImp urlImp = new URLIncludeImp();

		int soTimeout = 200;

		String output = urlImp.includeUrl(url, cacheLifeTime, soTimeout, defaultContent);

		assertEquals("We expected the default content since the connection should have been failed.", defaultContent, output);

		// Wait some more time to give the blocking http connection handler time to finish
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getServerURL() {
		System.out.println("Selected ServerPort:" + server.getPort());
		String url = "http://localhost:" + server.getPort();

		return url;
	}

	@Test
	public void testIncludeImpMultithreading() throws NodeException {

		final URLIncludeImp urlImp = new URLIncludeImp();

		final int soTimeout = 200;

		for (int i = 0; i < 2000; i++) {
			System.out.println("Starting thread: " + i);

			if (i % 5 == 0) {
				System.out.println("Waiting");
				// Wait some more time to give the blocking http connection handler time to finish
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			new Thread(new Runnable() {

				public void run() {
					try {
						String output = urlImp.includeUrl(url, cacheLifeTime, soTimeout, defaultContent);

						System.out.println(output);
					} catch (NodeException e) {
						e.printStackTrace();
					}
				}
			}).start();

		}

	}

	/**
	 * Test the url imp include with a higher socket timeout value.
	 *
	 * @throws NodeException
	 */
	@Test
	public void testIncludeImpTimeout2() throws NodeException {

		URLIncludeImp urlImp = new URLIncludeImp();
		int soTimeout = 30000;

		handler.setBlockingTime(0);
		url += "?blaa";
		String output = urlImp.includeUrl(url, cacheLifeTime, soTimeout, defaultContent);

		assertEquals("We expected the acctual server response since the connection should be fine.", "Here you go: GET /?blaa HTTP/1.1\n", output);
		System.out.println(output);

		// Wait some more time to give the blocking http connection handler time to finish
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
