package com.gentics.lib.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * this class is a implementation of SortedMap which sorts it's entries by value
 * intead of by key.
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 10.12.2003 TODO improve
 *         implementation
 */
public class ValueSortedMap implements SortedMap, Comparator {
	protected SortedMap sortedMap;

	protected Map plainMap = new HashMap();

	public ValueSortedMap() {
		sortedMap = new TreeMap((Comparator) this);
	}

	// redirects

	public boolean isEmpty() {
		return plainMap.isEmpty();
	}

	public int size() {
		return plainMap.size();
	}

	public boolean containsKey(Object key) {
		return plainMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return plainMap.containsValue(value);
	}

	public Object get(Object key) {
		return plainMap.get(key);
	}

	// add and remove objects

	public Object put(Object key, Object value) {
		plainMap.put(key, value);
		return sortedMap.put(key, value);
	}

	public Object remove(Object key) {
		sortedMap.remove(key);
		return plainMap.remove(key);
	}

	public void putAll(Map t) {
		Iterator it = t.entrySet().iterator();

		while (it.hasNext()) {
			Entry entry = (Entry) it.next();

			put(entry.getKey(), entry.getValue());
		}
	}

	public void clear() {
		sortedMap.clear();
		plainMap.clear();
	}

	// data-getters

	public Set keySet() {
		return sortedMap.keySet();
	}

	public Collection values() {
		return sortedMap.values();
	}

	public Set entrySet() {
		return sortedMap.entrySet();
	}

	public SortedMap subMap(Object fromKey, Object toKey) {
		return sortedMap.subMap(fromKey, toKey);
	}

	public SortedMap headMap(Object toKey) {
		return sortedMap.headMap(toKey);
	}

	public SortedMap tailMap(Object fromKey) {
		return sortedMap.tailMap(fromKey);
	}

	public Object firstKey() {
		return sortedMap.firstKey();
	}

	public Object lastKey() {
		return sortedMap.lastKey();
	}

	public int hashCode() {
		return sortedMap.hashCode();
	}

	public Comparator comparator() {
		return this;
	}

	// compare it

	public int compare(Object o1, Object o2) {
		Object value1 = plainMap.get(o1);
		Object value2 = plainMap.get(o2);

		if (value1 == null && value2 == null) {
			return 0;
		}
		if (value1 == null) {
			return 1;
		}
		if (value2 == null) {
			return -1;
		}
		if (!(value1 instanceof Comparable)) {
			return 0;
		}
		if (!(value2 instanceof Comparable)) {
			return 0;
		}
		return ((Comparable) value1).compareTo(value2);
	}
}
