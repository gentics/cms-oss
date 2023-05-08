package com.gentics.lib.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * created at Nov 7, 2004
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class OrderedHashMap implements OrderedMap {
	protected HashMap map = null;

	protected ArrayList keys = null;

	private boolean keepDuplicates = true;

	private class MapEntry implements Map.Entry {
		private Object key;

		public MapEntry(Object key) {
			this.key = key;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return map.get(key);
		}

		public Object setValue(Object value) {
			return map.put(key, value);
		}
	}

	private class MapEntryIterator implements Iterator {
		Iterator keyIterator = keys.iterator();

		public void remove() {
			throw new UnsupportedOperationException("Not supported");
		}

		public boolean hasNext() {
			return keyIterator.hasNext();
		}

		public Object next() {
			return new MapEntry(keyIterator.next());
		}
	}

	private class EntrySetView implements Set {
		public int size() {
			return map.size();
		}

		public void clear() {
			map.clear();
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public Object[] toArray() {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean add(Object o) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean contains(Object o) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean remove(Object o) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean addAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean containsAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean removeAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean retainAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public Iterator iterator() {
			return new MapEntryIterator();
		}

		public Object[] toArray(Object a[]) {
			throw new UnsupportedOperationException("NYI");
		}
	}

	private class ValuesIterator implements Iterator {
		int pos = 0;

		public void remove() {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean hasNext() {
			return pos < keys.size();
		}

		public Object next() {
			Object ret = map.get(keys.get(pos));

			pos++;
			return ret;
		}
	}

	private class KeyView implements Set {
		public int size() {
			return 0;
		}

		public void clear() {}

		public boolean isEmpty() {
			return false;
		}

		public Object[] toArray() {
			return new Object[0];
		}

		public boolean add(Object o) {
			return false;
		}

		public boolean contains(Object o) {
			return false;
		}

		public boolean remove(Object o) {
			return false;
		}

		public boolean addAll(Collection c) {
			return false;
		}

		public boolean containsAll(Collection c) {
			return false;
		}

		public boolean removeAll(Collection c) {
			return false;
		}

		public boolean retainAll(Collection c) {
			return false;
		}

		public Iterator iterator() {
			return null;
		}

		public Object[] toArray(Object a[]) {
			return new Object[0];
		}
	}

	private class ValuesView implements Collection {
		public int size() {
			return map.size();
		}

		public void clear() {
			keys.clear();
			map.clear();
		}

		public boolean isEmpty() {
			return keys.isEmpty();
		}

		public Object[] toArray() {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean add(Object o) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean contains(Object o) {
			return map.containsValue(o);
		}

		public boolean remove(Object o) {
			return map.values().remove(o);
		}

		public boolean addAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean containsAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean removeAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public boolean retainAll(Collection c) {
			throw new UnsupportedOperationException("NYI");
		}

		public Iterator iterator() {
			return new ValuesIterator();
		}

		public Object[] toArray(Object a[]) {
			throw new UnsupportedOperationException("NYI");
		}
	}

	public OrderedHashMap() {
		super();
		map = new HashMap();
		keys = new ArrayList();
		keepDuplicates = true;
	}

	public OrderedHashMap(int size) {
		super();
		map = new HashMap(size);
		keys = new ArrayList(size);
		keepDuplicates = true;
	}

	public OrderedHashMap(int size, boolean keepDuplicates) {
		super();
		map = new HashMap(size);
		keys = new ArrayList(size);
		this.keepDuplicates = keepDuplicates;
	}

	public OrderedHashMap(boolean keepDuplicates) {
		super();
		map = new HashMap();
		keys = new ArrayList();
		this.keepDuplicates = keepDuplicates;
	}

	public int indexOf(Object key) {
		return keys.indexOf(key);
	}

	public Object getKey(int pos) {
		return keys.get(pos);
	}

	public Object getValue(int pos) {
		return map.get(keys.get(pos));
	}

	public void swap(int pos1, int pos2) {
		Collections.swap(keys, pos1, pos2);
	}

	public void swap(Object key1, Object key2) {
		Collections.swap(keys, keys.indexOf(key1), keys.indexOf(key2));
	}

	public int size() {
		return keys.size();
	}

	public void clear() {
		map.clear();
		keys.clear();
	}

	public boolean isEmpty() {
		return keys.isEmpty();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Collection values() {
		return new ValuesView();
	}

	public void putAll(Map t) {
		// TODO support for !keepDuplicates
		keys.addAll(t.keySet());
		map.putAll(t);
	}

	public Set entrySet() {
		return new EntrySetView();
	}

	public Set keySet() {
		return new KeyView();
	}

	public Object get(Object key) {
		return map.get(key);
	}

	public Object remove(Object key) {
		keys.remove(key);
		return map.remove(key);
	}

	public Object put(Object key, Object value) {
		if (!keepDuplicates && keys.contains(key)) {
			keys.remove(key);
		}
		keys.add(key);
		return map.put(key, value);
	}
}
