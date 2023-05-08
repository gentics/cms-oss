/*
 * @author jan
 * @date Nov 27, 2008
 * @version $Id: AttributedThreadGroup.java,v 1.2 2008-12-03 12:06:48 jan Exp $
 */
package com.gentics.lib.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class has similar functionality as a ThreadLocal. 
 * 
 * It operates on ThreadGroups and not on Threads. 
 * If you want to use the AttributedThreadGroup
 * you have to create your thread with it as parent.
 * 
 * The objects are stored as key-value pairs in an internal map.
 * 
 * @author jan
 */
public class AttributedThreadGroup extends ThreadGroup {
    
	private Map attributes = Collections.synchronizedMap(new HashMap());
    
	public AttributedThreadGroup(String name) {
		super(name);
	}
    
	public AttributedThreadGroup(ThreadGroup parent, String name) {
		super(parent, name);
	}
    
	/**
	 * Return the value for the given key.
	 * @param key
	 * @return
	 */
	public Object get(String key) {
		Object result = attributes.get(key);

		return result;
	}
    
	/**
	 * Set the value for given key
	 * @param key
	 * @param value
	 */
	public void set(String key, Object value) {
		attributes.put(key, value);
	}
    
	/**
	 * Retrieve the value for the given key. The thread group of current thread
	 * should be an AttributedThreadGroup. If this is not the case this method
	 * will return the value of the alternative ThreadLocal.
	 * @param key The key for the value.
	 * @param alternative If current thread group is not an
	 *        AttributedThreadGroup then this alternative ThreadLocal will be
	 *        used (e.g. alternative.get()). Can be null.
	 * @return The value of the key.
	 */
	public static Object getForCurrentThreadGroup(String key, ThreadLocal alternative) {
		// first try to get the value from the threadgroup
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();

		if (threadGroup instanceof AttributedThreadGroup) {
			Object result = ((AttributedThreadGroup) threadGroup).get(key);

			return result;
            
			// else try to load it from threadlocal
		} else {
			if (alternative != null) {
				return alternative.get();
			} else {
				return null;
			}
		}
	}
    
	/**
	 * Sets the value with the given key for current thread group. The thread
	 * group of current thread should be an AttributedThreadGroup. If this is not
	 * the case this method will set the value of the alternative ThreadLocal.
	 * @param key The key under which you want to store the value.
	 * @param value The value object.
	 * @param alternative If the current thread group is not an
	 *        AttributedThreadGroup then the value will be stored in this
	 *        alternative ThreadLocal (e.g. alternative.set(value)). Can be
	 *        null.
	 */
	public static void setForCurrentThreadGroup(String key, Object value, ThreadLocal alternative) {
		// first try to set the value from the threadgroup
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();

		if (threadGroup instanceof AttributedThreadGroup) {
			((AttributedThreadGroup) threadGroup).set(key, value);
            
			// else try to set it for threadlocal
		} else {
			if (alternative != null) {
				alternative.set(value);
			}
		}
	}
}
