package com.gentics.lib.util;

import java.util.ArrayList;

/**
 * created at Dec 9, 2003
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class IntRangeMap {
	private ArrayList rangeList = new ArrayList(10);

	private ArrayList objList = new ArrayList(10);

	public void put(IntRange range, Object o) {
		// todo overlap checks and stuff
		// todo use something more optimized that a sequentiell HashMap
		int pos = rangeList.size();

		rangeList.add(range);
		objList.add(o);
	}

	public Object get(int value) {
		for (int i = 0; i < rangeList.size(); i++) {
			IntRange range = (IntRange) rangeList.get(i);

			if (range.contains(value)) {
				return objList.get(i);
			}
		}
		return null;
	}
}
