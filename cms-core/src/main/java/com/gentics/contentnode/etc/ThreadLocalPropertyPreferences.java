/*
 * @author norbert
 * @date 23.08.2010
 * @version $Id: ThreadLocalPropertyPreferences.java,v 1.1.4.1 2011-02-04 13:17:26 norbert Exp $
 */
package com.gentics.contentnode.etc;

import java.util.HashMap;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.Node;

/**
 * Implementation of node preferences that work as wrapper for a given instance
 * of {@link NodePreferences} and allows to overwrite properties for a single
 * request
 */
public class ThreadLocalPropertyPreferences implements NodePreferences {

	/**
	 * Wrapped instance of {@link NodePreferences}
	 */
	private NodePreferences wrappedPreferences;

	/**
	 * Threadlocal preferences to overwrite the wrapped preferences
	 */
	private ThreadLocal<Map<String, String[]>> threadLocalPreferences = new ThreadLocal<Map<String, String[]>>();

	/**
	 * Get the threadlocal map of preferences.
	 * @param generate true when a new map shall be generated if none exists, false if not
	 * @return threadlocal map of preferences or null
	 */
	private Map<String, String[]> getThreadLocalPreferencesMap(boolean generate) {
		Map<String, String[]> map = threadLocalPreferences.get();

		if (map == null && generate) {
			map = new HashMap<String, String[]>();
			threadLocalPreferences.set(map);
		}

		return map;
	}

	/**
	 * Create an instance of this class
	 * @param wrappedPreferences wrapped preferences
	 */
	public ThreadLocalPropertyPreferences(NodePreferences wrappedPreferences) {
		this.wrappedPreferences = wrappedPreferences;
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#getFeature(java.lang.String)
	 */
	public boolean getFeature(String name) {
		return wrappedPreferences.getFeature(name);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#getFeature(java.lang.String, com.gentics.contentnode.object.Node)
	 */
	public boolean getFeature(String name, Node node) {
		return wrappedPreferences.getFeature(name, node);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#getProperties(java.lang.String)
	 */
	public String[] getProperties(String name) {
		Map<String, String[]> localPreferencesMap = getThreadLocalPreferencesMap(false);

		if (localPreferencesMap != null && localPreferencesMap.containsKey(name)) {
			return localPreferencesMap.get(name);
		} else {
			return wrappedPreferences.getProperties(name);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#getProperty(java.lang.String)
	 */
	public String getProperty(String name) {
		Map<String, String[]> localPreferencesMap = getThreadLocalPreferencesMap(false);

		if (localPreferencesMap != null && localPreferencesMap.containsKey(name)) {
			return localPreferencesMap.get(name)[0];
		} else {
			return wrappedPreferences.getProperty(name);
		}
	}

	/* non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#getPropertyMap(java.lang.String)
	 */
	public Map getPropertyMap(String name) {
		return wrappedPreferences.getPropertyMap(name);
	}

	@Override
	public void setPropertyMap(String name, Map<String, String> map) throws NodeException {
		wrappedPreferences.setPropertyMap(name, map);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#getPropertyObject(java.lang.String)
	 */
	public Object getPropertyObject(String name) {
		return wrappedPreferences.getPropertyObject(name);
	}

	@Override
	public <T> T getPropertyObject(Node node, String name) throws NodeException {
		return wrappedPreferences.getPropertyObject(node, name);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#setFeature(java.lang.String, boolean)
	 */
	public void setFeature(String name, boolean value) {
		wrappedPreferences.setFeature(name, value);
	}

	@Override
	public void setFeature(Feature feature, boolean value) {
		wrappedPreferences.setFeature(feature, value);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#setProperty(java.lang.String, java.lang.String)
	 */
	public void setProperty(String name, String value) {
		setProperty(name, new String[] {value});
	}

	@Override
	public void setProperty(String name, String[] values) {
		Map<String, String[]> localPreferencesMap = getThreadLocalPreferencesMap(true);

		localPreferencesMap.put(name, values);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#unsetFeature(java.lang.String)
	 */
	public void unsetFeature(String name) {
		wrappedPreferences.unsetFeature(name);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.etc.NodePreferences#unsetProperty(java.lang.String)
	 */
	public void unsetProperty(String name) {
		Map<String, String[]> localPreferencesMap = getThreadLocalPreferencesMap(false);

		if (localPreferencesMap != null && localPreferencesMap.containsKey(name)) {
			localPreferencesMap.remove(name);
			if (localPreferencesMap.isEmpty()) {
				threadLocalPreferences.set(null);
			}
		}
	}

	@Override
	public Map<String, Object> toMap() {
		return wrappedPreferences.toMap();
	}
}
