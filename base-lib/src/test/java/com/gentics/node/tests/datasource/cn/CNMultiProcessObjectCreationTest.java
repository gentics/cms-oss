/*
 * @author norbert
 * @date 19.10.2007
 * @version $Id: CNMultiProcessObjectCreationTest.java,v 1.1 2010-02-04 14:25:05 norbert Exp $
 */
package com.gentics.node.tests.datasource.cn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.DatasourceException;
import com.gentics.api.lib.datasource.WriteableDatasource;
import com.gentics.api.lib.resolving.Changeable;
import com.gentics.contentnode.tests.category.BaseLibTest;
import com.gentics.lib.log.NodeLogger;
import com.gentics.node.testutils.NodeTestUtils;
import com.gentics.testutils.GenericTestUtils;
import org.junit.experimental.categories.Category;

/**
 * @author norbert
 *
 */
@Category(BaseLibTest.class)
public class CNMultiProcessObjectCreationTest {
	private Datasource ds;

	private NodeLogger logger;

	/**
	 * number of concurrent threads for the
	 * {@link #testConcurrentObjectCreation()} test.
	 */
	public final static int CONCURRENT_THREADS = 1;

	public final static String CONTENTID_PREFIX = "test.contentid:";

	public final static String TEST_DONE = "done";

	public final static String ERROR_PREFIX = "error:";

	/*
	 * (non-Javadoc)
	 *
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void doTest() throws Exception {
		logger = NodeLogger.getNodeLogger(this.getClass());
		GenericTestUtils.initConfigPathForCache();
		ds = NodeTestUtils.createWriteableDatasource(true);

		String testName = "JUSTANEVILTEST";
		List contentIds = new Vector();

		ObjectCreationThread[] threads = new ObjectCreationThread[CONCURRENT_THREADS];

		for (int i = 0; i < CONCURRENT_THREADS; ++i) {
			threads[i] = new ObjectCreationThread(testName, contentIds);
			threads[i].start();
		}

		// wait until all threads finished
		int running = 100;

		while (running > 0) {
			running = 0;
			for (int i = 0; i < threads.length; i++) {
				if (threads[i].isAlive()) {
					running++;
				}
			}
			if (running > 0) {
				logger.info(running + " threads still running, waiting a second");
				Thread.sleep(1000);
			}
		}

		// check whether a thread threw an exception
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].getException() != null) {
				throw threads[i].getException();
			}
		}

		// check for number of contentids
		if (CONCURRENT_THREADS != contentIds.size()) {
			throw new Exception("Expected " + CONCURRENT_THREADS + " contentids, but got " + contentIds.size());
		}

		// now output sorted list of contentids
		Collections.sort(contentIds);
		for (Iterator iter = contentIds.iterator(); iter.hasNext();) {
			String contentId = (String) iter.next();

			System.out.println(CONTENTID_PREFIX + contentId);
		}

		// now tell the parent process that we are done
		System.out.println(TEST_DONE);
	}

	/**
	 * Thread class for concurrent object creation test
	 */
	protected class ObjectCreationThread extends Thread {
		protected String testName;

		protected List contentIds;

		protected Exception exception;

		/**
		 * Create instance of this thread
		 *
		 * @param testName
		 *            test name
		 * @param contentIds
		 *            list of contentids
		 */
		public ObjectCreationThread(String testName, List contentIds) {
			this.testName = testName;
			this.contentIds = contentIds;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			Map map = new HashMap();

			map.put("obj_type", "10007");
			map.put("name", testName);
			try {
				Changeable changeable = ((WriteableDatasource) ds).create(map);

				((WriteableDatasource) ds).store(Collections.singleton(changeable));
				contentIds.add(changeable.get("contentid"));
			} catch (DatasourceException e) {
				exception = e;
			}
		}

		/**
		 * Get exception (if any was thrown in the thread) or null
		 *
		 * @return exception or null
		 */
		public Exception getException() {
			return exception;
		}
	}
}
