package com.gentics.contentnode.tests.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CountHelper {
	/**
	 * Render counts
	 */
	protected static Map<String, AtomicInteger> renderCounts = new HashMap<>();

	/**
	 * Rendered content
	 */
	protected static String render = null;

	/**
	 * Reset all render counts and the rendered content to empty
	 */
	public static void reset() {
		renderCounts.clear();
		render = null;
	}

	/**
	 * Get the render count for the given name
	 * @param name name
	 * @return render count
	 */
	public static int get(String name) {
		return renderCounts.getOrDefault(name, new AtomicInteger()).get();
	}

	/**
	 * Let the directive render the given content
	 * @param toRender content to be rendered
	 */
	public static void render(String toRender) {
		render = toRender;
	}

	/**
	 * Get new asserter as {@link #AutoClosable}
	 * @param name rendered name
	 * @param expectedCount expected render count
	 * @return asserter
	 */
	public static AutoCloseable asserter(String name, int expectedCount) {
		return new Asserter(name, expectedCount);
	}

	/**
	 * Asserter for the CountDirective
	 */
	protected static class Asserter implements AutoCloseable {
		protected String name;

		protected int expectedCount;

		/**
		 * Create asserter for name and expected count
		 * @param name name
		 * @param expectedCount expected count
		 */
		protected Asserter(String name, int expectedCount) {
			this.name = name;
			this.expectedCount = expectedCount;
			CountHelper.reset();
		}

		@Override
		public void close() throws Exception {
			assertThat(CountHelper.get(name)).as("Count for " + name).isEqualTo(expectedCount);
		}
	}
}
