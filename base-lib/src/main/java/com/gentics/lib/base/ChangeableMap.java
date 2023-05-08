/*
 * @author Stefan Hepp
 * @date ${date}
 * @version $Id: ChangeableMap.java,v 1.8 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.base;

import java.io.Serializable;
import java.util.Map;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.api.lib.resolving.Changeable;

/**
 * Changeable Map Resolver Helper Class
 */
public class ChangeableMap extends MapResolver implements Changeable, Serializable {
	private static final long serialVersionUID = -104924045889474655L;

	/**
	 * Create a Changeable Map with the given map holding the data
	 * @param properties properties map
	 */
	public ChangeableMap(Map properties) {
		super(properties);
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.Changeable#setProperty(java.lang.String, java.lang.Object)
	 */
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		properties.put(name, value);
		return true;
	}
    
	public Object put(Object key, Object value) {
		return properties.put(ObjectTransformer.getString(key, null), value);
	}
}
