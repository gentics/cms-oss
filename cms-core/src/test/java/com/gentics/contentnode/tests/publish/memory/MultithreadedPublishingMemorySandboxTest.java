package com.gentics.contentnode.tests.publish.memory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.api.lib.cache.PortalCache;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.publish.PublishInfo;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.contentnode.testutils.GCNFeature;
import com.gentics.lib.log.NodeLogger;

/**
 * This test will examine the memory footprint of publish runs.
 * 
 * @author johannes2
 * 
 */
@GCNFeature(set = { Feature.MULTITHREADED_PUBLISHING }, unset = { Feature.TAG_IMAGE_RESIZER })
public class MultithreadedPublishingMemorySandboxTest {
	
	@Rule
	public DBTestContext testContext = new DBTestContext().config(prefs -> {
		prefs.set("config.loadbalancing.threadlimit", 6);
	});

	private static final NodeLogger logger = NodeLogger.getNodeLogger(MultithreadedPublishingMemorySandboxTest.class);

	private static int PUBLISH_RUN_COUNT = 100;

	/*
	 * Percent value that will be used to determine the tolerance level
	 */
	private static float MEMORY_USAGE_TOLERANCE = 0.05f;

	/**
	 * Maximum number of subsequent garbage collector calls
	 */
	private static int MAX_GC_CALLS = 10;

	@Test
	public void testPublish() throws Exception {
		final long ADDITIONAL_MEMORY_USAGE_LIMIT = 4674757;

		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

		// clear the node caches
		PortalCache.getCache(NodeFactory.CACHEREGION).clear();
		System.gc();
		long baseHeapMemoryUsage = mbean.getHeapMemoryUsage().getUsed();

		logger.info("Base Memory Usage: " + FileUtils.byteCountToDisplaySize(baseHeapMemoryUsage));

		long totalAdditonalHeapMemoryUsage = 0;

		// Now invoke multiple publish runs and calculate the average memory utilization
		for (int i = 0; i < PUBLISH_RUN_COUNT; i++) {
			PortalCache.getCache(NodeFactory.CACHEREGION).clear();
			System.gc();
			int beforePublish = threadBean.getThreadCount();

			logger.info("Publish Run #" + i);
			PublishInfo info = testContext.getContext().publish(false);

			assertTrue("Publishrun did not terminate successfully.", PublishInfo.RETURN_CODE_SUCCESS == info.getReturnCode());
			PortalCache.getCache(NodeFactory.CACHEREGION).clear();
			logger.info("Before GC: " + FileUtils.byteCountToDisplaySize(mbean.getHeapMemoryUsage().getUsed()));
			// we may make up to MAX_GC_CALLS gc calls
			int numGc = 0;

			long currentHeapMemoryUsageDiff = -1;
			for (numGc = 0; numGc <= MAX_GC_CALLS; numGc++) {
				// call the gc
				System.gc();
				// wait a bit
				Thread.sleep(200);
				// check whether the memory consumption is down to an acceptable level
				currentHeapMemoryUsageDiff = mbean.getHeapMemoryUsage().getUsed() - baseHeapMemoryUsage;
				if (!exceedsLimit(currentHeapMemoryUsageDiff, ADDITIONAL_MEMORY_USAGE_LIMIT)) {
					logger.info("Additional usage is " + FileUtils.byteCountToDisplaySize(currentHeapMemoryUsageDiff));
					break;
				}
			}

			logger.info("Additional usage after " + numGc + " GCs: " + FileUtils.byteCountToDisplaySize(currentHeapMemoryUsageDiff));
			totalAdditonalHeapMemoryUsage += currentHeapMemoryUsageDiff;
			if (i > 0) {
				logger.info("Current average: " + FileUtils.byteCountToDisplaySize(totalAdditonalHeapMemoryUsage / i));
			}
			int afterPublish = threadBean.getThreadCount();

			// if the thread count is different, we wait another second, because the evictor thread in the connection pool in the multiconnection transaction might still
			// have been running
			if (beforePublish != afterPublish) {
				Thread.sleep(1000);
				afterPublish = threadBean.getThreadCount();
			}
			if (i > 0) {
				assertTrue("# of Live threads must not increase during publish run: " + beforePublish + " -> " + afterPublish, beforePublish >= afterPublish);
			}
		}

		logger.info("Total additional usage: {" + FileUtils.byteCountToDisplaySize(totalAdditonalHeapMemoryUsage));
		long averageAdditionalHeapMemoryUsage = totalAdditonalHeapMemoryUsage / PUBLISH_RUN_COUNT;

		assertFalse(
				"The base memory usage was {" + FileUtils.byteCountToDisplaySize(baseHeapMemoryUsage) + "} and exceeded the defined memory usage of {" +  FileUtils.byteCountToDisplaySize(ADDITIONAL_MEMORY_USAGE_LIMIT) + "}. The average memory usage was {" + FileUtils.byteCountToDisplaySize(averageAdditionalHeapMemoryUsage) + "} The tolerance was {" + MEMORY_USAGE_TOLERANCE
				+ "}. The difference was: " + (((float) averageAdditionalHeapMemoryUsage / (float) ADDITIONAL_MEMORY_USAGE_LIMIT * 100) - 100) + "%",
				exceedsLimit(averageAdditionalHeapMemoryUsage, ADDITIONAL_MEMORY_USAGE_LIMIT));

	}

	/**
	 * Checks whether the given value exceeds the given limit.
	 * 
	 * @param value
	 * @param limit
	 * @return
	 */
	private boolean exceedsLimit(long value, long limit) {
		if (value > (limit * (MEMORY_USAGE_TOLERANCE + 1))) {
			return true;
		} else {
			return false;
		}
	}
}
