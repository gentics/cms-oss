package com.gentics.contentnode.object.utility;

import java.util.Comparator;
import java.util.List;

/**
 * Comparator implementation that uses a list of comparators to compare
 *
 * @param <T> Type of compared objects
 */
public class MultiComparator<T> implements Comparator<T> {
	/**
	 * List of comparators to use
	 */
	protected List<Comparator<T>> comparators;

	/**
	 * Create an instance
	 * @param comparators list of comparators
	 */
	public MultiComparator(List<Comparator<T>> comparators) {
		this.comparators = comparators;
	}

	@Override
	public int compare(T o1, T o2) {
		// iterate over the comparators and use them to compare the objects
		for (Comparator<T> comparator : comparators) {
			int cmp = comparator.compare(o1, o2);
			// first result not having the objects "equal" is returned
			if (cmp != 0) {
				return cmp;
			}
		}
		// no comparator found any differences, the objects are equal
		return 0;
	}
}
