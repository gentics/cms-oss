package com.gentics.lib.util;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TLongObjectHashMap;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 30.04.2004
 */
public class NodeObjectToObjectHashMap {
	TIntObjectHashMap m_typeMap = new TIntObjectHashMap();

	private TLongObjectHashMap getTypeMap(int tType) {
		TLongObjectHashMap ret = (TLongObjectHashMap) m_typeMap.get(tType);

		if (ret == null) {
			ret = new TLongObjectHashMap();
		}
		return ret;
	}

	public void put(int tType, long id, Object obj) {
		TLongObjectHashMap typeMap = getTypeMap(tType);

		typeMap.put(id, obj);
	}

	public boolean contains(int tType, long id) {
		TLongObjectHashMap typeMap = getTypeMap(tType);

		return typeMap.contains(id);
	}

	public Object get(int tType, long id) {
		TLongObjectHashMap typeMap = getTypeMap(tType);

		return typeMap.get(id);
	}
}
