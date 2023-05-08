package com.gentics.lib.util;

import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectIterator;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 27.04.2004
 */
public class TroveUtils {
	public static final TLongObjectHashMap putAll(TLongObjectHashMap target,
			TLongObjectHashMap[] maps) {
		for (int i = 0; i < maps.length; i++) {
			TLongObjectHashMap map = maps[i];
			TLongObjectIterator it = map.iterator();

			while (it.hasNext()) {
				it.advance();
				target.put(it.key(), it.value());
			}
		}
		return target;
	}

	public static final TLongLongHashMap mergeMaps(TLongLongHashMap[] maps) {
		int size = 0;

		for (int i = 0; i < maps.length; i++) {
			size += maps[i].size();
		}
		TLongLongHashMap ret = new TLongLongHashMap(size);

		for (int i = 0; i < maps.length; i++) {
			TLongLongHashMap map = maps[i];
			long[] keys = map.keys();

			for (int j = 0; j < keys.length; j++) {
				long key = keys[j];

				ret.put(key, map.get(key));
			}
		}
		return ret;
	}
}
