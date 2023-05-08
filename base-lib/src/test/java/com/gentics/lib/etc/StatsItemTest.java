package com.gentics.lib.etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.gentics.contentnode.tests.category.BaseLibTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test cases for StatsItems
 */
@Category(BaseLibTest.class)
public class StatsItemTest {
	@Test
	public void testMultithreadedAccess() throws InterruptedException {
		int numThreads = 1000;
		int numJobs = 1000000;
		final int numSamples = 100;

		final StatsItem sharedItem = new StatsItem();
		ExecutorService service = Executors.newFixedThreadPool(numThreads);

		List<Future<Void>> futures = new ArrayList<>();
		final Random rand = new Random();
		for (int i = 0; i < numJobs; i++) {
			futures.add(service.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					String category = Integer.toString(rand.nextInt(1000));
					for (int j = 0; j < numSamples; j++) {
						sharedItem.start();
						sharedItem.stop(1, category);
					}
					return null;
				}
			}));
		}

		service.shutdown();
		assertTrue("All tasks should have succeeded", service.awaitTermination(60, TimeUnit.SECONDS));
		assertEquals("Check number of samples", numJobs * numSamples, sharedItem.getNumSamples());
	}
}
