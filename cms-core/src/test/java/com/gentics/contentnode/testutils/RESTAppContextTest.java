package com.gentics.contentnode.testutils;

import static com.gentics.contentnode.tests.assertj.GCNAssertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.contentnode.testutils.RESTAppContext.Type;

/**
 * Test cases for RESTAppContext
 */
@RunWith(value = Parameterized.class)
public class RESTAppContextTest {
	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	@Parameters(name = "{index}: type {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Type type : Type.values()) {
			data.add(new Object[] { type });
		}
		return data;
	}

	@Parameter(0)
	public Type type;

	/**
	 * Test creating multiple instances of {@link RESTAppContext} (each should use a separate port)
	 * @throws Throwable
	 */
	@Test
	public void testMultipleUse() throws Throwable {
		try (ContextWrapper wrapper1 = new ContextWrapper(new RESTAppContext(type))) {
			try (ContextWrapper wrapper2 = new ContextWrapper(new RESTAppContext(type))) {
				try (ContextWrapper wrapper3 = new ContextWrapper(new RESTAppContext(type))) {
					Set<String> baseUriSet = new HashSet<>();
					baseUriSet.add(wrapper1.ctx.getBaseUri());
					baseUriSet.add(wrapper2.ctx.getBaseUri());
					baseUriSet.add(wrapper3.ctx.getBaseUri());

					assertThat(baseUriSet).as("Set of base URIs").hasSize(3);
				}
			}
		}
	}

	/**
	 * Autocloseable wrapper for {@link RESTAppContext}. Calls
	 * {@link RESTAppContext#before()} on initialization and
	 * {@link RESTAppContext#after()} in {@link #close()}.
	 */
	protected static class ContextWrapper implements AutoCloseable {
		protected RESTAppContext ctx;

		/**
		 * Create instance
		 * @param ctx wrapped context
		 * @throws Throwable
		 */
		public ContextWrapper(RESTAppContext ctx) throws Throwable {
			this.ctx = ctx;
			this.ctx.before();
		}

		@Override
		public void close() throws Exception {
			if (ctx != null) {
				ctx.after();
			}
		}
	}
}
