/*
 * @author norbert
 * @date 18.04.2005
 * @version $Id: MapResolver.java,v 1.8 2006-05-19 07:19:53 norbert Exp $
 */
package com.gentics.lib.base;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Resolver that resolves properties delivered in a map
 * @author norbert
 */
public class MapResolver implements Map, Resolvable {

	/**
	 * map holding the properties that can be resolved
	 */
	protected Map properties;

	/**
	 * constructor for the map resolver
	 * @param properties map holding the resolved properties
	 */
	public MapResolver(Map properties) {
		this.properties = properties;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return properties != null ? properties.get(key) : null;
	}

	public Object get(String key) {
		return getProperty(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return properties != null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return properties != null ? properties.toString() : "";
	}

	public int size() {
		return properties.size();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	public Collection values() {
		return properties.values();
	}

	public void putAll(Map t) {
		throw new UnsupportedOperationException();
	}

	public Set entrySet() {
		return properties.entrySet();
	}

	public Set keySet() {
		return properties.keySet();
	}

	public Object get(Object key) {
		return properties.get(key);
	}

	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public Object put(Object key, Object value) {
		throw new UnsupportedOperationException();
	}
}
