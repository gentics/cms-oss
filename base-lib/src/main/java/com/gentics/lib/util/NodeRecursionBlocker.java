package com.gentics.lib.util;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TLongIntHashMap;
import gnu.trove.TLongObjectHashMap;

/**
 * @author Erwin Mascher (e.mascher@gentics.com) Date: 30.04.2004
 */
public class NodeRecursionBlocker {
	private static TIntObjectHashMap m_types = new TIntObjectHashMap();

	private static TIntObjectHashMap m_partTypes = new TIntObjectHashMap();

	private static TLongIntHashMap getPartMap(int tType, long id) {
		synchronized (m_partTypes) {
			TLongObjectHashMap idMap = (TLongObjectHashMap) m_partTypes.get(tType);

			if (idMap == null) {
				idMap = new TLongObjectHashMap(10);
				m_partTypes.put(tType, idMap);
			}
			TLongIntHashMap partMap = (TLongIntHashMap) idMap.get(id);

			if (partMap == null) {
				partMap = new TLongIntHashMap(10);
				idMap.put(id, partMap);
			}
			return partMap;
		}
	}

	private static TLongIntHashMap getTypeMap(int tType) {
		TLongIntHashMap lockMap;

		synchronized (m_types) {
			lockMap = (TLongIntHashMap) m_types.get(tType);
			if (lockMap == null) {
				lockMap = new TLongIntHashMap(100);
				m_types.put(tType, lockMap);
			}
		}
		return lockMap;
	}

	public static boolean tryLock(int tType, long id, long param) {
		TLongIntHashMap lockMap = getTypeMap(tType); /*
		 * synchronized( lockMap ) { if ( lockMap.containsKey( id ) ) { int lock =
		 * lockMap.get( id ); if ( lock == 1 ) return false; } lockMap.put( id,
		 * 1 ); return true; }
		 */ {
			TLongIntHashMap partMap = getPartMap(tType, id);

			synchronized (partMap) {
				if (partMap.containsKey(param)) {
					int lock = partMap.get(param);

					if (lock == 1) {
						return false;
					}
				}
				partMap.put(param, 1);
				return true;
			}
		}
	}

	public static void releaseLock(int tType, long id, long param) {

		/*
		 * if ( part == null ) { TLongIntHashMap lockMap = getTypeMap( tType );
		 * synchronized( lockMap ) { lockMap.remove( id ); } } else {
		 */
		TLongIntHashMap partMap = getPartMap(tType, id);

		synchronized (partMap) {
			partMap.remove(param);
		}
	}
}
