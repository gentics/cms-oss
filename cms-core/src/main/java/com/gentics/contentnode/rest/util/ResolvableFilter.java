package com.gentics.contentnode.rest.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.rest.resource.parameter.FilterParameterBean;

/**
 * Filter implementation for filtering resolvables
 *
 * @param <T> class of Resolvables to filter
 */
public class ResolvableFilter<T extends Resolvable> implements Filter<T> {
	/**
	 * Filter query
	 */
	protected String query;

	/**
	 * Property paths which are filtered
	 */
	protected List<String> paths;

	/**
	 * Optional field map. Maps searched field paths to actual resolved field paths
	 */
	protected Map<String, String> fieldMap;

	/**
	 * Create an instance
	 * @param query filter query
	 * @param fieldMap optional field map
	 * @param attrs property paths
	 */
	protected ResolvableFilter(String query, Map<String, String> fieldMap, String ...attrs) {
		this.query = query;
		this.fieldMap = fieldMap;
		if (!ObjectTransformer.isEmpty(this.query)) {
			this.query = this.query.toLowerCase();
		}
		paths = new ArrayList<>(Arrays.asList(attrs));
	}

	/**
	 * Create an instance if the query is not empty or return null if the query is empty
	 * @param query query (may be null or empty)
	 * @param attrs optional list of property paths that are filtered
	 * @return filter instance or null
	 * @throws NodeException
	 */
	public static <U extends Resolvable> ResolvableFilter<U> get(String query, String...attrs) throws NodeException {
		return get(query, null, attrs);
	}

	/**
	 * Create an instance if the query is not empty or return null if the query is empty
	 * @param query query (may be null or empty)
	 * @param fieldMap optional field map
	 * @param attrs optional list of property paths that are filtered
	 * @return filter instance or null
	 * @throws NodeException
	 */
	public static <U extends Resolvable> ResolvableFilter<U> get(String query, Map<String, String> fieldMap, String...attrs) throws NodeException {
		if (ObjectTransformer.isEmpty(query)) {
			return null;
		}
		return new ResolvableFilter<>(query, fieldMap, attrs);
	}

	/**
	 * Create an instance if the filter is not empty (or null) or return null otherwise
	 * @param filter filter setting (may be null or empty)
	 * @param attrs optional list of property paths that are filtered
	 * @return filter instance or null
	 * @throws NodeException
	 */
	public static <U extends Resolvable> ResolvableFilter<U> get(FilterParameterBean filter, String... attrs) throws NodeException {
		return get(filter, null, attrs);
	}

	/**
	 * Create an instance if the filter is not empty (or null) or return null otherwise
	 * @param filter filter setting (may be null or empty)
	 * @param fieldMap optional field map
	 * @param attrs optional list of property paths that are filtered
	 * @return filter instance or null
	 * @throws NodeException
	 */
	public static <U extends Resolvable> ResolvableFilter<U> get(FilterParameterBean filter, Map<String, String> fieldMap, String... attrs) throws NodeException {
		if (filter == null) {
			return null;
		}
		return get(filter.query, fieldMap, attrs);
	}

	@Override
	public boolean matches(T object) throws NodeException {
		for (String path : paths) {
			Object value = PropertyResolver.resolve(object, getPath(path));
			if (value != null && value.toString().toLowerCase().contains(query)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the possibly transformed path to resolve
	 * @param path original path
	 * @return resolved path
	 */
	protected String getPath(String path) {
		if (fieldMap != null) {
			return fieldMap.getOrDefault(path, path);
		} else {
			return path;
		}
	}
}
