/*
 * @author norbert
 * @date 28.09.2007
 * @version $Id: SortImp.java,v 1.2 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.portalnode.formatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.resolving.ResolvableComparator;
import com.gentics.api.portalnode.imp.AbstractGenticsImp;
import com.gentics.lib.log.NodeLogger;

/**
 * An imp for sorting collection of resolvables
 */
public class SortImp extends AbstractGenticsImp {

	/**
	 * logger
	 */
	protected NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	/**
	 * constant for ascending sorting
	 */
	public static final String TYPE_ASCENDING_SHORT = "asc";

	/**
	 * constant for descending sorting
	 */
	public static final String TYPE_DESCENDING_SHORT = "desc";

	/**
	 * Sort the object (collection, object array or map). Sorting is done case
	 * insensitive
	 * @param object collection, object array or map
	 * @param property sorted property (might contain :asc or :desc)
	 * @return sorted collection
	 */
	public Collection sort(Object object, String property) {
		return sort(object, property, false, null);
	}

	/**
	 * Sort the object (collection, object array or map)
	 * @param object collection, object array or map
	 * @param property sorted property (might contain :asc or :desc)
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @return sorted collection
	 */
	public Collection sort(Object object, String property, boolean caseSensitive) {
		return sort(object, property, caseSensitive, null);
	}

	/**
	 * Sort the object (collection, object array or map)
	 * @param object collection, object array or map
	 * @param property sorted property (might contain :asc or :desc)
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Object object, String property, String localeCode) {
		return sort(object, property, false, localeCode);
	}

	/**
	 * Sort the object (collection, object array or map)
	 * @param object collection, object array or map
	 * @param property sorted property (might contain :asc or :desc)
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Object object, String property, boolean caseSensitive, String localeCode) {
		List properties = new ArrayList(1);

		properties.add(property);

		if (object instanceof Collection) {
			return sort((Collection) object, properties, caseSensitive, localeCode);
		} else if (object instanceof Object[]) {
			return sort((Object[]) object, properties, caseSensitive, localeCode);
		} else if (object instanceof Map) {
			return sort((Map) object, properties, caseSensitive, localeCode);
		}
		// the object type is not supported
		return null;
	}

	/**
	 * Sort the collection with the given list of properties
	 * @param collection collection to sort
	 * @param properties list of sorted properties
	 * @return sorted collection
	 */
	public Collection sort(Collection collection, List properties) {
		return sort(collection, properties, false, null);
	}

	/**
	 * Sort the collection with the given list of properties
	 * @param collection collection to sort
	 * @param properties list of sorted properties
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @return sorted collection
	 */
	public Collection sort(Collection collection, List properties, boolean caseSensitive) {
		return sort(collection, properties, caseSensitive, null);
	}

	/**
	 * Sort the collection with the given list of properties
	 * @param collection collection to sort
	 * @param properties list of sorted properties
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Collection collection, List properties, String localeCode) {
		return sort(collection, properties, false, localeCode);
	}

	/**
	 * Sort the collection with the given list of properties
	 * @param collection collection to sort
	 * @param properties list of sorted properties
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Collection collection, List properties, boolean caseSensitive, String localeCode) {
		List list = new ArrayList(collection.size());

		list.addAll(collection);
		return internalSort(list, properties, caseSensitive, localeCode);
	}

	/**
	 * Sort the map values
	 * @param map map to sort
	 * @param properties list of sorted properties
	 * @return sorted collection
	 */
	public Collection sort(Map map, List properties) {
		return sort(map.values(), properties, false, null);
	}

	/**
	 * Sort the map values
	 * @param map map to sort
	 * @param properties list of sorted properties
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @return sorted collection
	 */
	public Collection sort(Map map, List properties, boolean caseSensitive) {
		return sort(map.values(), properties, caseSensitive, null);
	}

	/**
	 * Sort the map values
	 * @param map map to sort
	 * @param properties list of sorted properties
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Map map, List properties, String localeCode) {
		return sort(map, properties, false, localeCode);
	}

	/**
	 * Sort the map values
	 * @param map map to sort
	 * @param properties list of sorted properties
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Map map, List properties, boolean caseSensitive, String localeCode) {
		return sort(map.values(), properties, caseSensitive, localeCode);
	}

	/**
	 * Sort the array with the given list of properties
	 * @param array array to sort
	 * @param properties list of sorted properties
	 * @return sorted collection
	 */
	public Collection sort(Object[] array, List properties) {
		return sort(array, properties, false, null);
	}

	/**
	 * Sort the array with the given list of properties
	 * @param array array to sort
	 * @param properties list of sorted properties
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @return sorted collection
	 */
	public Collection sort(Object[] array, List properties, boolean caseSensitive) {
		return internalSort(Arrays.asList(array), properties, caseSensitive, null);
	}

	/**
	 * Sort the array with the given list of properties
	 * @param array array to sort
	 * @param properties list of sorted properties
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Object[] array, List properties, String localeCode) {
		return sort(array, properties, false, localeCode);
	}

	/**
	 * Sort the array with the given list of properties
	 * @param array array to sort
	 * @param properties list of sorted properties
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @param localeCode code of the locale to be used for sorting
	 * @return sorted collection
	 */
	public Collection sort(Object[] array, List properties, boolean caseSensitive, String localeCode) {
		return internalSort(Arrays.asList(array), properties, caseSensitive, localeCode);
	}

	/**
	 * Parse the given property into a sorting property (detect sortorder)
	 * @param property property, might contain :asc or :desc
	 * @return sorting parameter with sortorder (defaults to asc)
	 */
	protected Datasource.Sorting getSorting(String property) {
		int colonIndex = property.indexOf(':');

		if (colonIndex != -1) {
			String sortType = property.substring(colonIndex + 1);

			property = property.substring(0, colonIndex);

			if (TYPE_ASCENDING_SHORT.equalsIgnoreCase(sortType)) {
				return new Datasource.Sorting(property, Datasource.SORTORDER_ASC);
			} else if (TYPE_DESCENDING_SHORT.equalsIgnoreCase(sortType)) {
				return new Datasource.Sorting(property, Datasource.SORTORDER_DESC);
			} else {
				return new Datasource.Sorting(property, Datasource.SORTORDER_ASC);
			}
		} else {
			return new Datasource.Sorting(property, Datasource.SORTORDER_ASC);
		}
	}

	/**
	 * Parse the given list of properties into an array of sorting parameters
	 * @param properties list of properties
	 * @return array of sorting parameters
	 */
	protected Datasource.Sorting[] getSorting(List properties) {
		int size = properties.size();
		Datasource.Sorting[] sorting = new Datasource.Sorting[size];

		for (int i = 0; i < size; ++i) {
			sorting[i] = getSorting(properties.get(i).toString());
		}

		return sorting;
	}

	/**
	 * Internal method to sort the given list
	 * @param list list to sort
	 * @param properties sorting parameters
	 * @param caseSensitive true for case sensitive sorting, false for case
	 *        insensitive
	 * @param localeCode code of the locale used for sorting (may be null for default locale)
	 * @return sorted collection
	 */
	protected Collection internalSort(List list, List properties, boolean caseSensitive, String localeCode) {
		try {
			if (properties == null) {
				throw new Exception("Cannot sort without sorting properties");
			} else {
				Comparator c = null;

				if (localeCode != null) {
					Locale locale = new Locale(localeCode);

					c = new ResolvableComparator(getSorting(properties), caseSensitive, locale);
				} else {
					c = new ResolvableComparator(getSorting(properties), caseSensitive);
				}
				Collections.sort(list, c);
			}
			return list;
		} catch (Exception e) {
			logger.error("Error while sorting: ", e);
			return null;
		}
	}
}
