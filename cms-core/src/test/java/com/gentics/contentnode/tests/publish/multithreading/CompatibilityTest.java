/*
 * @author jan
 * @date Oct 22, 2008
 * @version $Id: CompatibilityTest.java,v 1.1 2010-02-04 14:25:06 norbert Exp $
 */
package com.gentics.contentnode.tests.publish.multithreading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.testutils.SimpleHttpServer;

/**
 * Runs the multithreaded publisher test an checks the results
 * 
 * You might want to run this with "-Djava.net.preferIPv4Stack=true"
 * 
 * @author jan
 * 
 */

@Ignore("This test is disabled since it needs an obsolete tool on dev6. The test was never finished.")
public class CompatibilityTest {

	public static final int PUBLISHER_CONFIG_PORT = 42901;

	private SimpleHttpServer configServer;

	@Before
	public void setUp() throws Exception {

		configServer = new SimpleHttpServer(PUBLISHER_CONFIG_PORT);
		configServer.startServing();

	}

	@After
	public void tearDown() throws Exception {
		configServer.stopServing();
	}

	@Test
	public void testMultithreadedPublishing() {
		String propertiesString;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			Properties defaults = new Properties();

			defaults.load(CompatibilityTest.class.getResourceAsStream("node.conf.properties"));
			Properties multiThreadedConfig = new Properties();

			multiThreadedConfig.load(CompatibilityTest.class.getResourceAsStream("multithreaded.publisher.properties"));
			// override and extend the default properties
			defaults.putAll(multiThreadedConfig);
			defaults.store(outputStream, null);
			propertiesString = outputStream.toString();
			configServer.registerPage("/config", propertiesString);

			/*
			 * try { Thread.sleep(30000); } catch (InterruptedException e) { //
			 * TODO Auto-generated catch block e.printStackTrace(); }
			 */
			System.out.println("press enter to stop");
			System.in.read();
			configServer.stopServing();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
