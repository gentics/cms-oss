package com.gentics.lib.resolving;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.render.Renderable;

/**
 * Wrapper for instances of {@link ResolvableMapWrappable}, that implements {@link Map}
 */
public class ResolvableMapWrapper extends AbstractMap<String, Object> implements Resolvable, Renderable {
	/**
	 * Entry Set
	 */
	protected final Set<Entry<String, Object>> entrySet;

	/**
	 * Wrapped instance
	 */
	protected final ResolvableMapWrappable wrapped;

	/**
	 * Wrap the value into instance(s) of {@link ResolvableMapWrapper}.
	 * This will handle sindle instances, instances in lists and maps
	 * @param value value to wrap
	 * @return optionally wrapped value
	 */
	protected static Object wrap(Object value) {
		if (value instanceof ResolvableMapWrappable) {
			return new ResolvableMapWrapper((ResolvableMapWrappable) value);
		} else if (value instanceof List<?>) {
			List<?> listValue = List.class.cast(value);

			return new AbstractList<Object>() {
				@Override
				public int size() {
					return listValue.size();
				}

				@Override
				public Object get(int index) {
					return wrap(listValue.get(index));
				}
			};
		} else if (value instanceof Map<?, ?>) {
			Map<?, ?> mapValue = Map.class.cast(value);

			Map<Object, Object> wrappedMap = value instanceof SortedMap<?, ?> ? new LinkedHashMap<>(mapValue.size())
					: new HashMap<>();
			mapValue.forEach((k, v) -> {
				wrappedMap.put(k, wrap(v));
			});

			return wrappedMap;
		} else {
			return value;
		}
	}

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped instance
	 */
	public ResolvableMapWrapper(ResolvableMapWrappable wrapped) {
		this.wrapped = wrapped;
		this.entrySet = wrapped.getResolvableKeys().stream().map(key -> new MapEntry(key)).collect(Collectors.toSet());
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return entrySet;
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public Object get(String key) {
		return wrapped.get(key);
	}

	@Override
	public boolean canResolve() {
		return wrapped.canResolve();
	}

	@Override
	public String toString() {
		return String.format("Wrapper for %s", wrapped.toString());
	}

	@Override
	public String render() throws NodeException {
		if (wrapped instanceof Renderable) {
			return ((Renderable) wrapped).render();
		} else if (wrapped != null) {
			return wrapped.toString();
		} else {
			return null;
		}
	}

	/**
	 * Internal implementation of a {@link Map.Entry}
	 */
	protected class MapEntry implements Map.Entry<String, Object> {
		/**
		 * Key of the entry
		 */
		protected String key;

		/**
		 * Create instance for the key
		 * @param key key
		 */
		protected MapEntry(String key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return wrap(wrapped.get(key));
		}

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}
	}
}
