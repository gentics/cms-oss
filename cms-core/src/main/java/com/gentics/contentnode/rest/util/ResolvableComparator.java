package com.gentics.contentnode.rest.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.rest.resource.parameter.SortParameterBean;
import com.gentics.lib.etc.StringUtils;

/**
 * Comparator implementation for sorting resolvables
 *
 * @param <T> class of Resolvables to sort
 */
public class ResolvableComparator<T extends Resolvable> implements Comparator<T> {
	/**
	 * List of sort paths with sort factor (sort order)
	 */
	protected List<SortPath> paths = new ArrayList<>();

	/**
	 * Optional field map. Maps sort field paths to actual resolved field paths
	 */
	protected Map<String, String> fieldMap;

	/**
	 * Set of sort paths, that should have null values sorted last (not first).
	 */
	protected Set<String> pathsWithNullAsLast = new HashSet<>();

	/**
	 * Create an instance for sorting
	 * @param sort comma separated list of sorted attributes. Each attribute may be prefixed with + (sort ascending) or - (sort descending)
	 * @param fieldMap optional field map
	 * @param attrs optional list of allowed sort paths
	 */
	protected ResolvableComparator(String sort, Map<String, String> fieldMap, String...attrs) {
		this.fieldMap = fieldMap;
		Set<String> allowed = new HashSet<>(Arrays.asList(attrs));
		String[] sortedFields = sort.split(",");

		String attr = null;
		for (String field : sortedFields) {
			field = field.trim();
			int sortFactor = 1;
			if (ObjectTransformer.isEmpty(field)) {
				continue;
			}
			if (field.startsWith("+")) {
				if (field.length() == 1) {
					continue;
				}
				attr = field.substring(1);
			} else if (field.startsWith("-")) {
				if (field.length() == 1) {
					continue;
				}
				attr = field.substring(1);
				sortFactor = -1;
			} else {
				attr = field;
			}

			if (allowed.contains(attr)) {
				paths.add(new SortPath(attr, sortFactor));
			}
		}
	}

	/**
	 * Create a comparator instance or null, if sort is null or empty
	 * @param sort comma separated list of sorted attributes. Each attribute may be prefixed with + (sort ascending) or - (sort descending)
	 * @param attrs optional list of allowed sort paths
	 * @return comparator instance or null
	 */
	public static <U extends Resolvable> ResolvableComparator<U> get(String sort, String...attrs) {
		return get(sort, (Map<String, String>)null, attrs);
	}

	/**
	 * Create a comparator instance or null, if sort is null or empty
	 * @param sort comma separated list of sorted attributes. Each attribute may be prefixed with + (sort ascending) or - (sort descending)
	 * @param fieldMap optional field map
	 * @param attrs optional list of allowed sort paths
	 * @return comparator instance or null
	 */
	public static <U extends Resolvable> ResolvableComparator<U> get(String sort, Map<String, String> fieldMap, String...attrs) {
		if (ObjectTransformer.isEmpty(sort)) {
			return null;
		}
		return new ResolvableComparator<>(sort, fieldMap, attrs);
	}

	/**
	 * Create a comparator instance or null, if sort is null or empty
	 * @param sorting sorting parameters
	 * @param attrs optional list of allowed sort paths
	 * @return comparator instance or null
	 */
	public static <U extends Resolvable> ResolvableComparator<U> get(SortParameterBean sorting, String...attrs) {
		return get(sorting, (Map<String, String>)null, attrs);
	}

	/**
	 * Create a comparator instance or null, if sort is null or empty
	 * @param sorting sorting parameters
	 * @param attrs optional list of allowed sort paths
	 * @return comparator instance or null
	 */
	public static <U extends Resolvable> ResolvableComparator<U> get(SortParameterBean sorting, Map<String, String> fieldMap, String...attrs) {
		if (sorting == null) {
			return null;
		}
		return get(sorting.sort, fieldMap, attrs);
	}

	@Override
	public int compare(T o1, T o2) {
		for (SortPath sortPath : paths) {
			try {
				Comparable<?> v1 = sortPath.get(o1);
				Comparable<?> v2 = sortPath.get(o2);

				int result = 0;
				if (v1 instanceof String && v2 instanceof String) {
					result = StringUtils.mysqlLikeCompare((String)v1, (String)v2);
				} else {
					result = ObjectTransformer.compareObjects(v1, v2, !pathsWithNullAsLast.contains(sortPath.path));
				}

				if (result != 0) {
					return result * sortPath.sortFactor;
				}
			} catch (NodeException e) {
			}
		}
		return 0;
	}

	/**
	 * Get the "ORDER BY" clause that can be used in SQL statements (starting with " ORDER BY")
	 * @return ORDER BY clause
	 */
	public String getOrderClause() {
		StringBuilder orderByClause = new StringBuilder();

		if (!paths.isEmpty()) {
			orderByClause.append(" ORDER BY ");
			orderByClause.append(paths.stream().map(sort -> sort.path + (sort.sortFactor > 0 ? " ASC" : " DESC"))
					.collect(Collectors.joining(", ")));
		}

		return orderByClause.toString();
	}

	/**
	 * Set the given sort attributes to be sorted having null values last (not first). This only applies to non-String attributes.
	 * @param attrs array of attribute paths
	 */
	public void setNullsAsLast(String...attrs) {
		pathsWithNullAsLast.addAll(Arrays.asList(attrs));
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

	/**
	 * Internal class for encapsulation of sort path and sort factor
	 */
	protected class SortPath {
		protected int sortFactor;
		protected String path;

		/**
		 * Create an instance
		 * @param path sort path
		 * @param sortFactor sort factor
		 */
		protected SortPath(String path, int sortFactor) {
			this.path = path;
			this.sortFactor = sortFactor;
		}

		/**
		 * Resolve the sort path from the given resolvable.
		 * Return null, the Comparable value or the String representation if not comparable
		 * @param object resolvable object
		 * @return resolved value.
		 * @throws NodeException
		 */
		protected Comparable<?> get(Resolvable object) throws NodeException {
			Object result = PropertyResolver.resolve(object, getPath(path));
			if (result == null) {
				return null;
			} else if (result instanceof Comparable<?>) {
				return (Comparable<?>)result;
			} else {
				return result.toString();
			}
		}
	}
}
