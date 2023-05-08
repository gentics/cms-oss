/*
 * @author norbert
 * @date 05.01.2010
 * @version $Id: MultiThreadedPublisherTest.java,v 1.5 2010-09-28 17:08:17 norbert Exp $
 */
package com.gentics.contentnode.tests.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishController;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.util.FileUtil;
import com.gentics.testutils.GenericTestUtils;

/**
 * Test cases for multithreaded publishing
 */
public class MultiThreadedPublisherTest {
	
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	public final static int COMPARE_BUFFER_SIZE = 4096;

	/**
	 * performance factor for publish performance test: assert multithreaded_time * PERFORMANCE_FACTOR < singlethreaded_time
	 */
	public final static float PERFORMANCE_FACTOR = 1.1f;

	static {
		GenericTestUtils.initConfigPathForCache();
	}

	@Before
	public void setUp() throws Exception {
		NodeConfigRuntimeConfiguration.getPreferences().setFeature(Feature.MULTITHREADED_PUBLISHING, true);
	}

	/**
	 * Convenience method to perform a publish process, check whether the publish process succeeds and return the duration in ms
	 * 
	 * @param multithreaded
	 *            true when the publish process shall be done multithreaded, false for singlethreaded
	 * @param dirtAll
	 *            TODO
	 * @return duration in ms
	 * @throws Exception
	 */
	protected long publishProcess(boolean multithreaded, boolean dirtAll) throws Exception {
		long start = System.currentTimeMillis();
		PublishInfo info = testContext.getContext().publish(true);
		long end = System.currentTimeMillis();

		assertEquals("Check status of " + (multithreaded ? "multithreaded" : "singlethreaded") + " publish process: ", PublishInfo.RETURN_CODE_SUCCESS,
				info.getReturnCode());
		return (end - start);
	}

	/**
	 * Compare the given files (paths) for equality
	 * 
	 * @param expected
	 *            expected file/path
	 * @param actual
	 *            actual file/path
	 * @return list of differences
	 */
	protected String createDiff(File expected, File actual) throws IOException {
		StringBuffer diff = new StringBuffer();

		createDiffRecursion(expected, actual, diff);
		return diff.toString();
	}

	/**
	 * Recursive method to compare the given files
	 * 
	 * @param expected
	 *            expected file/directory
	 * @param actual
	 *            actual file/directory
	 * @param diff
	 *            stringbuffer for adding the diffs
	 */
	protected void createDiffRecursion(File expected, File actual, StringBuffer diff) throws IOException {
		// first check whether both files exist
		if (!expected.exists()) {
			if (!actual.exists()) {// ok, both files do not exist, no differences found :-)
			} else {
				diff.append("file " + actual.getAbsolutePath() + " was not expected to exist\n");
			}
		} else {
			if (!actual.exists()) {
				diff.append("file " + actual.getAbsolutePath() + " does not exist\n");
			} else {
				// both files exist, check for identical type
				if (expected.isDirectory() && !actual.isDirectory()) {
					diff.append("file " + actual.getAbsolutePath() + " should be a directory, but is not\n");
				} else if (!expected.isDirectory() && actual.isDirectory()) {
					diff.append("file " + actual.getAbsolutePath() + " should not be a directory, but is\n");
				} else if (expected.isDirectory() && actual.isDirectory()) {
					// check for expected files/directories
					String[] expectedNames = expected.list();

					for (int i = 0; i < expectedNames.length; i++) {
						createDiffRecursion(new File(expected, expectedNames[i]), new File(actual, expectedNames[i]), diff);
					}

					// check for actual files/directories
					String[] actualNames = actual.list();

					for (int i = 0; i < actualNames.length; i++) {
						createDiffRecursion(new File(expected, actualNames[i]), new File(actual, actualNames[i]), diff);
					}
				} else if (!expected.isDirectory() && !actual.isDirectory()) {
					// compare file sizes
					if (expected.length() != actual.length()) {
						diff.append(
								"File lengths of files " + expected.getAbsolutePath() + " and " + actual.getAbsolutePath() + " differ (" + expected.length() + " vs. "
								+ actual.length() + ")\n");
					} else {
						// compare the file contents
						FileInputStream expectedStream = new FileInputStream(expected);
						FileInputStream actualStream = new FileInputStream(actual);

						int read = 0;
						byte[] buffer = new byte[COMPARE_BUFFER_SIZE];
						byte[] compareBuffer = new byte[COMPARE_BUFFER_SIZE];
						boolean differenceFound = false;

						while (!differenceFound && ((read = expectedStream.read(buffer)) > 0)) {
							actualStream.read(compareBuffer);
							for (int i = 0; i < read && !differenceFound; i++) {
								if (buffer[i] != compareBuffer[i]) {
									differenceFound = true;
								}
							}
						}

						actualStream.close();
						expectedStream.close();

						if (differenceFound) {
							diff.append("File contents of files " + expected.getAbsolutePath() + " and " + actual.getAbsolutePath() + " differ\n");
						}
					}
				}
			}
		}
	}

	/**
	 * Simple functionality test. Just run the multithreaded publish process and check whether it succeeds.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishFunctionality() throws Exception {
		publishProcess(true, true);
	}

	/**
	 * Compare the result of the singlethreaded publish process with the result of a multithreaded publish process
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPublishDiff() throws Exception {
		// do a singlethreaded publish process
		publishProcess(false, true);

		// rename the pub directory
		File pubDir = testContext.getContext().getPubDir();
		File singleThreadedPubDir = new File(pubDir.getParentFile(), "single");

		FileUtil.deleteDirectory(singleThreadedPubDir);
		assertTrue("Check whether " + pubDir + " could be renamed to " + singleThreadedPubDir, pubDir.renameTo(singleThreadedPubDir));

		// do a multithreaded publish process
		publishProcess(true, true);

		// rename the pub directory
		pubDir = testContext.getContext().getPubDir();
		File multiThreadedPubDir = new File(pubDir.getParentFile(), "multi");

		FileUtil.deleteDirectory(multiThreadedPubDir);
		assertTrue("Check whether " + pubDir + " could be renamed to " + multiThreadedPubDir, pubDir.renameTo(multiThreadedPubDir));

		// now compare the directories
		String diff = createDiff(singleThreadedPubDir, multiThreadedPubDir);

		assertEquals("Check the published content", "", diff);
	}

	/**
	 * Publish performance test. Multithreaded publish process is done with 2 threads, should take about 1/2 of the time. We check for 2/3 to make sure.
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore("This test works but should now be invoked in parallel with other tests.")
	public void testPublishingPerformance() throws Exception {
		// first a single threaded publish process
		long singleThreadedDuration = publishProcess(false, true);

		// now a multithreaded publish process
		long multiThreadedDuration = publishProcess(true, true);

		// check whether multithread publish process was fast enough
		assertTrue(
				"Check whether the multithreaded publish process was fast enough (multi: " + multiThreadedDuration + " ms vs. single: " + singleThreadedDuration
				+ " ms): multithreaded*" + PERFORMANCE_FACTOR + " < singlethreaded",
				multiThreadedDuration * PERFORMANCE_FACTOR < singleThreadedDuration);
	}

	/**
	 * Test abortion of a publish process
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testPublishAbortion() throws Exception {
		// first start a publish process in the background
		testContext.getContext().publish(true, false);

		// wait two seconds
		Thread.sleep(2000);

		// now check whether the publish process is running
		assertTrue("Check whether the publish process is running now", PublishController.isRunning());
		long startTime = System.currentTimeMillis();
		boolean enteredPageRenderPhase = false;

		while (!enteredPageRenderPhase && PublishController.isRunning()) {
			PublishInfo info = PublishController.getPublishInfo();

			if ("Rendering Pages".equals(info.getCurrentPhaseName())) {
				enteredPageRenderPhase = true;
			} else {
				Thread.sleep(1000);
			}
			long now = System.currentTimeMillis();

			if (now - startTime > 10 * 60 * 1000) {
				fail("Timout reached");
			}
		}

		// check whether we entered the page render phase now
		assertTrue("Check whether we entered the page render phase now", enteredPageRenderPhase);

		// wait another two seconds
		Thread.sleep(2000);

		// now stop the publish process and check the time
		long startStop = System.currentTimeMillis();

		assertTrue("Check whether the publish process was stopped", PublishController.stopPublishLocally(true));
		long stopDuration = System.currentTimeMillis() - startStop;

		// now check whether the publish process is no longer running
		assertFalse("Check whether the publish process is no longer running after being stopped", PublishController.isRunning());

		// check whether stopping the publish process did not take too long
		// (longer than 10 seconds)
		assertTrue("Check whether the publish process was stopped in less than 10 seconds (actually took " + stopDuration + " ms)", stopDuration <= 10000);
	}

	/**
	 * Test with republishing after modification of a filename
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testRepublishModifications() throws Exception {
		// do a multithreaded publish process
		publishProcess(true, true);
		final int pageId = 54;
		// start a new transaction
		Transaction t = testContext.startTransactionWithPermissions(false);

		// getContext().startTransaction();
		// now change the filename of a single page (and dirt that page)
		testContext.getDBSQLUtils().executeQueryManipulation("UPDATE page SET filename = 'modified_filename.html', status = 1 WHERE id = " + pageId);
		// dirt the cache
		t.dirtObjectCache(Page.class, pageId);

		// start a new transaction
		t = testContext.startTransactionWithPermissions(true);

		// do another publish process, only the modified objects shall be
		// published
		publishProcess(true, false);

		// now check whether the filename in the filesystem is updated
		File page = new File(testContext.getContext().getPubDir(), "single/multichannelling/Content.Node/modified_filename.html");

		assertTrue("Check whether the page with modified filename (" + page + ") exists", page.exists());
	}
}
