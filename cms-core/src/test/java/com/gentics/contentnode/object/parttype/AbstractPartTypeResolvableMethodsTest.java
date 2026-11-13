package com.gentics.contentnode.object.parttype;

import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.contentnode.object.parttype.handlebars.HandlebarsPartType;

/**
 * Test cases for the static cache of resolvable methods in {@link AbstractPartType}, which is shared between all threads (e.g. publish workers)
 */
public class AbstractPartTypeResolvableMethodsTest {
	/**
	 * Part type implementation classes, for which the resolvable methods will be determined concurrently
	 */
	private final static List<Class<? extends AbstractPartType>> PART_TYPE_CLASSES = List.of(HandlebarsPartType.class,
			ShortTextPartType.class, NormalTextPartType.class, HTMLPartType.class, HTMLTextPartType.class, LongHTMLPartType.class,
			LongHTMLTextPartType.class, CheckboxPartType.class, PageURLPartType.class, FileURLPartType.class, ImageURLPartType.class,
			FolderURLPartType.class, ImageHeightPartType.class, ImageWidthPartType.class, NodePartType.class, OverviewPartType.class,
			PageTagPartType.class, TemplateTagPartType.class, SingleSelectPartType.class, MultiSelectPartType.class,
			DatasourcePartType.class, OrderedListPartType.class, UnorderedListPartType.class, ChangeableListPartType.class, TablePartType.class);

	/**
	 * Number of concurrent threads
	 */
	private final static int THREADS = 8;

	/**
	 * Number of rounds (each round starts with an empty cache)
	 */
	private final static int ROUNDS = 200;

	/**
	 * Test that determining the resolvable methods concurrently from multiple threads, while the cache is still empty, does not fail
	 * and yields the same result as determining them in a single thread
	 * @throws Exception
	 */
	@Test
	public void testConcurrentAccess() throws Exception {
		// determine the expected result single threaded
		AbstractPartType.resolvableKeys.clear();
		Map<Class<? extends AbstractPartType>, Map<String, Method>> expected = new HashMap<>();
		for (Class<? extends AbstractPartType> clazz : PART_TYPE_CLASSES) {
			expected.put(clazz, new HashMap<>(AbstractPartType.getResolvableMethods(clazz)));
		}
		assertThat(expected.get(HandlebarsPartType.class)).as("Resolvable methods of HandlebarsPartType").isNotEmpty();

		ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		try {
			for (int round = 0; round < ROUNDS; round++) {
				// start with cold caches, like after a server start
				AbstractPartType.resolvableKeys.clear();
				Introspector.flushCaches();

				CountDownLatch start = new CountDownLatch(1);
				List<Future<Map<Class<? extends AbstractPartType>, Map<String, Method>>>> futures = new ArrayList<>();
				for (int t = 0; t < THREADS; t++) {
					// every thread iterates the classes with a different offset, so that threads add different keys at the same time
					int offset = t * PART_TYPE_CLASSES.size() / THREADS;
					Callable<Map<Class<? extends AbstractPartType>, Map<String, Method>>> task = () -> {
						start.await();
						Map<Class<? extends AbstractPartType>, Map<String, Method>> result = new HashMap<>();
						for (int i = 0; i < PART_TYPE_CLASSES.size(); i++) {
							Class<? extends AbstractPartType> clazz = PART_TYPE_CLASSES.get((i + offset) % PART_TYPE_CLASSES.size());
							result.put(clazz, new HashMap<>(AbstractPartType.getResolvableMethods(clazz)));
						}
						return result;
					};
					futures.add(executor.submit(task));
				}
				start.countDown();

				for (Future<Map<Class<? extends AbstractPartType>, Map<String, Method>>> future : futures) {
					// get() rethrows any exception thrown in the thread (wrapped in an ExecutionException)
					assertThat(future.get(1, TimeUnit.MINUTES)).as("Resolvable methods in round %d", round).isEqualTo(expected);
				}
			}
		} finally {
			executor.shutdownNow();
		}
	}
}
