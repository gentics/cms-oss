package com.gentics.lib.util;

import java.util.Map;

/**
 * created at Nov 7, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public interface OrderedMap extends Map {
	void swap(int pos1, int pos2);

	void swap(Object key1, Object key2);

	int indexOf(Object key);

	Object getKey(int pos);

	Object getValue(int pos);
}
