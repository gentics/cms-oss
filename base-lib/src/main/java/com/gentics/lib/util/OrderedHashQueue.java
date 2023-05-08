package com.gentics.lib.util;

import java.util.Map;

/**
 * User: Stefan Hepp Date: 22.06.2005 Time: 17:44:06
 */
public class OrderedHashQueue extends OrderedHashMap implements OrderedQueue {

	private int maxSize;

	private class MapEntry implements Map.Entry {
		private Object key;

		private Object value;

		public MapEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

		public Object setValue(Object value) {
			throw new UnsupportedOperationException("NYI");
		}
	}

	public OrderedHashQueue(int maxSize) {
		super(maxSize, false);
		this.maxSize = maxSize;
	}

	public OrderedHashQueue(int maxSize, int size) {
		super(size, false);
		this.maxSize = maxSize;
	}

	public int getQueueSize() {
		return maxSize;
	}

	public Object put(Object key, Object value) {
		if (!containsKey(key) && size() >= maxSize) {
			popStart();
		}
		return super.put(key, value);
	}

	public Entry pushStart(Object key, Object value) {
		Entry rs = null;

		if (keys.contains(key)) {
			rs = pop(key);
		} else if (keys.size() >= maxSize) {
			rs = popEnd();
		}
		keys.add(0, key);
		map.put(key, value);
		return rs;
	}

	public Entry pushEnd(Object key, Object value) {
		Entry rs = null;

		if (keys.contains(key)) {
			rs = pop(key);
		} else if (keys.size() >= maxSize) {
			rs = popStart();
		}
		keys.add(0, key);
		map.put(key, value);
		return rs;
	}

	public Entry popStart() {
		Object key = keys.remove(0);
		Entry rs = new MapEntry(key, map.get(key));

		map.remove(key);
		return rs;
	}

	public Entry popEnd() {
		Object key = keys.remove(keys.size() - 1);
		Entry rs = new MapEntry(key, map.get(key));

		map.remove(key);
		return rs;
	}

	public Entry pop(Object key) {
		keys.remove(key);
		Entry rs = new MapEntry(key, map.get(key));

		map.remove(key);
		return rs;
	}

}
