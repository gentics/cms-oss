package com.gentics.lib.util;

import java.util.Map;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * created at Oct 24, 2004 this class takes another resolvable plus a map, and
 * tries to resolve via the map first. if the property that is to be resolved
 * cannot be found in the map, the request is passed through to the resolvable
 * object.
 * @author Erwin Mascher (e.mascher@gentics.com)
 */
public class MapOverloadResolvable implements Resolvable {

	private Resolvable relay;

	private Map primaryMap;

	public MapOverloadResolvable(Map primaryMap, Resolvable relay) {
		this.relay = relay;
		this.primaryMap = primaryMap;
	}

	public MapOverloadResolvable() {}

	public void setRelay(Resolvable relay) {
		this.relay = relay;
	}

	public void setPrimaryMap(Map primaryMap) {
		this.primaryMap = primaryMap;
	}

	/*
	 * public HashMap getPropertyNames() { HashMap ret = new HashMap(); if (
	 * relay != null ) ret.putAll(relay.getPropertyNames()); if (this.primaryMap !=
	 * null ) { Iterator it = this.primaryMap.keySet().iterator(); while
	 * (it.hasNext()) { Object o = it.next(); ret.put(o.toString(), ""); } }
	 * return ret; }
	 */

	public Object getProperty(String key) {
		if (this.primaryMap != null) {
			if (this.primaryMap.containsKey(key)) {
				return this.primaryMap.get(key);
			}
		}
		if (this.relay != null) {
			if (this.relay.canResolve()) {
				return relay.getProperty(key);
			}
		}
		return null;
	}

	public Object get(String key) {
		return getProperty(key);
	}

	// if canResolve returns false, all properties equate to null
	public boolean canResolve() {
		return this.primaryMap != null || relay.canResolve();
	}
}
