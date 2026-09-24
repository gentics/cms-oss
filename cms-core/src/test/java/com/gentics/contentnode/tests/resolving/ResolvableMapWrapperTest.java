package com.gentics.contentnode.tests.resolving;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.gentics.contentnode.resolving.ResolvableMapWrapper;
import com.gentics.contentnode.resolving.ResolvableMapWrappable;

/**
 * Test cases for {@link ResolvableMapWrapper}
 */
public class ResolvableMapWrapperTest {
	/**
	 * Test that resolving a key which is contained in {@link ResolvableMapWrappable#getResolvableKeys()}
	 * is delegated to the wrapped resolvable
	 */
	@Test
	public void testGetResolvableKey() {
		TestResolvable wrapped = new TestResolvable();
		wrapped.values.put("name", "Test Name");

		assertThat(asMap(new ResolvableMapWrapper(wrapped)).get("name")).as("Resolved value").isEqualTo("Test Name");
		assertThat(wrapped.resolved).as("Keys resolved from the wrapped resolvable").containsExactly("name");
	}

	/**
	 * Test that resolving a key which is <em>not</em> contained in
	 * {@link ResolvableMapWrappable#getResolvableKeys()} is still delegated to the wrapped resolvable.
	 *
	 * This is required, because resolvables may resolve more keys than they can enumerate (e.g. object
	 * properties, which do not exist yet), and resolving such a key may have side effects (like creating
	 * a dependency, which is needed for dirting).
	 */
	@Test
	public void testGetUnknownKey() {
		TestResolvable wrapped = new TestResolvable();
		wrapped.values.put("name", "Test Name");

		assertThat(asMap(new ResolvableMapWrapper(wrapped)).get("unknown")).as("Resolved value").isNull();
		assertThat(wrapped.resolved).as("Keys resolved from the wrapped resolvable").containsExactly("unknown");
	}

	/**
	 * Test that only the keys contained in {@link ResolvableMapWrappable#getResolvableKeys()} are enumerated
	 */
	@Test
	public void testEntrySet() {
		TestResolvable wrapped = new TestResolvable();
		wrapped.values.put("name", "Test Name");
		wrapped.values.put("description", "Test Description");

		assertThat(asMap(new ResolvableMapWrapper(wrapped))).as("Wrapped map")
			.containsOnlyKeys("description", "name");
	}

	/**
	 * Test that resolving a key which is not a String does not fail
	 */
	@Test
	public void testGetNonStringKey() {
		TestResolvable wrapped = new TestResolvable();
		wrapped.values.put("name", "Test Name");

		assertThat(asMap(new ResolvableMapWrapper(wrapped)).get(Integer.valueOf(4711))).as("Resolved value").isNull();
		assertThat(wrapped.resolved).as("Keys resolved from the wrapped resolvable").isEmpty();
	}

	/**
	 * Get the wrapper as {@link Map}, so that {@link Map#get(Object)} (which is what handlebars uses) is
	 * called, instead of the more specific {@link ResolvableMapWrapper#get(String)}
	 * @param wrapper wrapper
	 * @return wrapper as map
	 */
	protected Map<String, Object> asMap(ResolvableMapWrapper wrapper) {
		return wrapper;
	}

	/**
	 * {@link ResolvableMapWrappable} implementation, which only enumerates the keys it has values for,
	 * but records every key it is asked to resolve
	 */
	protected static class TestResolvable implements ResolvableMapWrappable {
		/**
		 * Resolvable values (also used as enumerated keys)
		 */
		protected Map<String, Object> values = new HashMap<>();

		/**
		 * Keys, which were resolved
		 */
		protected List<String> resolved = new ArrayList<>();

		@Override
		public Set<String> getResolvableKeys() {
			return values.keySet();
		}

		@Override
		public Object getProperty(String key) {
			return get(key);
		}

		@Override
		public Object get(String key) {
			resolved.add(key);
			return values.get(key);
		}

		@Override
		public boolean canResolve() {
			return true;
		}
	}
}
