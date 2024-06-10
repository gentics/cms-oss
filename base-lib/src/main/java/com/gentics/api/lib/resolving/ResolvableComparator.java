/*
 * @author norbert
 * @date 28.03.2007
 * @version $Id: ResolvableComparator.java,v 1.5 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.api.lib.resolving;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.Datasource.Sorting;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.UnknownPropertyException;
import com.gentics.lib.etc.MiscUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.Renderable;

/**
 * Comparator class for comparing (and sorting) Resolvables by a attributes
 */
public class ResolvableComparator implements Comparator {

	/**
	 * sortings
	 */
	protected Sorting[] sortings;

	/**
	 * case sensitive comparison, default to false
	 */
	protected boolean caseSensitive;

	/**
	 * collator to be used for sorting Strings
	 */
	protected Collator collator = Collator.getInstance();

	/**
	 * locale
	 */
	protected Locale locale = Locale.getDefault();

	/**
	 * Create a comparator for sorting. Sorting is done case insensitive.
	 * @param sortBy name of the attribute for sorting
	 * @param sortOrder sort order
	 */
	public ResolvableComparator(String sortBy, int sortOrder) {
		this(new Sorting[] { new Sorting(sortBy, sortOrder)}, false, null);
	}

	/**
	 * Create a comparator for sorting
	 * @param sortBy name of the attribute for sorting
	 * @param sortOrder sort order
	 * @param caseSensitive perform case-sensitive sorting
	 */
	public ResolvableComparator(String sortBy, int sortOrder, boolean caseSensitive) {
		this(new Sorting[] { new Sorting(sortBy, sortOrder)}, caseSensitive, null);
	}

	/**
	 * Create a comparator for sorting
	 * @param sortBy name of the attribute for sorting
	 * @param sortOrder sort order
	 * @param caseSensitive perform case-sensitive sorting
	 * @param locale locale to be used for sorting (may be null for the default locale)
	 */
	public ResolvableComparator(String sortBy, int sortOrder, boolean caseSensitive,
			Locale locale) {
		this(new Sorting[] { new Sorting(sortBy, sortOrder)}, caseSensitive, locale);
	}

	/**
	 * Create a comparator for sorting. Sorting is done case insensitive
	 * @param sorting sortings setting
	 */
	public ResolvableComparator(Sorting sorting) {
		this(new Sorting[] { sorting}, false, null);
	}

	/**
	 * Create a comparator for sorting
	 * @param sorting sortings setting
	 * @param caseSensitive perform case-sensitive sorting
	 */
	public ResolvableComparator(Sorting sorting, boolean caseSensitive) {
		this(new Sorting[] { sorting}, caseSensitive, null);
	}

	/**
	 * Create a comparator for sorting
	 * @param sorting sortings setting
	 * @param caseSensitive perform case-sensitive sorting
	 * @param locale locale to be used (null for default locale)
	 */
	public ResolvableComparator(Sorting sorting, boolean caseSensitive, Locale locale) {
		this(new Sorting[] { sorting}, caseSensitive, locale);
	}

	/**
	 * Create a comparator for sorting (multiple attributes). Sorting is done
	 * case insensitive.
	 * @param sortings array of sortings
	 */
	public ResolvableComparator(Sorting[] sortings) {
		this(sortings, false, null);
	}

	/**
	 * Create a comparator for sorting (multiple attributes)
	 * @param sortings array of sortings
	 * @param caseSensitive perform case-sensitive sorting
	 */
	public ResolvableComparator(Sorting[] sortings, boolean caseSensitive) {
		this(sortings, caseSensitive, null);
	}

	/**
	 * Create a comparator for sorting (multiple attributes)
	 * @param sortings array of sortings
	 * @param caseSensitive perform case-sensitive sorting
	 * @param locale locale to be used for sorting (null for default locale)
	 */
	public ResolvableComparator(Sorting[] sortings, boolean caseSensitive, Locale locale) {
		this.sortings = sortings;
		this.caseSensitive = caseSensitive;
		if (locale != null) {
			this.collator = Collator.getInstance(locale);
			this.locale = locale;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		// when the objects cannot be compared, return 0
		if (!(o1 instanceof Resolvable) || !(o2 instanceof Resolvable) || sortings == null || sortings.length == 0) {
			return 0;
		}

		Resolvable r1 = (Resolvable) o1;
		Resolvable r2 = (Resolvable) o2;

		int compareResult = 0;

		for (int i = 0; i < sortings.length && compareResult == 0; i++) {
			compareResult = compare(r1, r2, sortings[i]);
		}

		return compareResult;
	}

	/**
	 * Compare the given resolvables with the given sorting setting
	 * @param r1 first resolvable
	 * @param r2 second resolvable
	 * @param sorting sorting
	 * @return -1 if r1 is "smaller", 1 if r1 is "greater" or 0 if the objects
	 *         are equal
	 */
	protected int compare(Resolvable r1, Resolvable r2, Sorting sorting) {
		NodeLogger logger = NodeLogger.getNodeLogger(getClass());
		Object value1 = "";
		Number number1 = null;
		Number number2 = null;

		try {
			value1 = PropertyResolver.resolve(r1, sorting.getColumnName());
			if (value1 instanceof Renderable && !(value1 instanceof Comparable)) {
				try {
					value1 = ((Renderable) value1).render();
				} catch (Exception e) {
					logger.warn("Error while rendering renderable", e);
				}
			}
			// try to interpret the value as number
			number1 = ObjectTransformer.getNumber(value1, null);
			logger.debug("r1 sortby resolved to {" + value1 + "}");
		} catch (UnknownPropertyException e) {
			logger.warn("Unable to resolve property {" + sorting.getColumnName() + "} for object {" + r1 + "}", e);
		}
		Object value2 = "";

		try {
			value2 = PropertyResolver.resolve(r2, sorting.getColumnName());
			if (value2 instanceof Renderable && !(value2 instanceof Comparable)) {
				try {
					value2 = ((Renderable) value2).render();
				} catch (Exception e) {
					logger.warn("Error while rendering renderable", e);
				}
			}

			// try to interpret the value as number
			number2 = ObjectTransformer.getNumber(value2, null);
			logger.debug("r2 sortby resolved to {" + value2 + "}");
		} catch (UnknownPropertyException e) {
			logger.warn("Unable to resolve property {" + sorting.getColumnName() + "} for object {" + r2 + "}", e);
		}
        
		if (number1 != null && number2 != null) {
			// only use the converted number values if both sides can be converted to numbers.
			value1 = number1;
			value2 = number2;
		}

		switch (sorting.getSortOrder()) {
		case Datasource.SORTORDER_ASC:
			return MiscUtils.compareObjects(value1, value2, caseSensitive, collator, locale);

		case Datasource.SORTORDER_DESC:
			return MiscUtils.compareObjects(value2, value1, caseSensitive, collator, locale);

		default:
			return 0;
		}
	}
}
