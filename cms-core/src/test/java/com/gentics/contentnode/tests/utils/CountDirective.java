package com.gentics.contentnode.tests.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import com.gentics.api.lib.etc.ObjectTransformer;

/**
 * A simple velocity directive that just counts in a static field, how many
 * times it has been rendered.
 * 
 * Additionally, the rendered content can be changed (default is empty)
 * 
 * It can be used like <code>
 * #gtx_test_count("bla")
 * </code>
 */
public class CountDirective extends Directive {
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

	@Override
	public String getName() {
		return "gtx_test_count";
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException,
			MethodInvocationException {
		String name = ObjectTransformer.getString(node.jjtGetChild(0).value(context), "");
		renderCounts.computeIfAbsent(name, key -> new AtomicInteger()).incrementAndGet();
		if (!ObjectTransformer.isEmpty(render)) {
			writer.write(render);
		}
		return true;
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
			CountDirective.reset();
		}

		@Override
		public void close() throws Exception {
			assertThat(CountDirective.get(name)).as("Count for " + name).isEqualTo(expectedCount);
		}
	}
}
