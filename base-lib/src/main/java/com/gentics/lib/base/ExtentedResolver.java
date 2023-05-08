/**
 *
 * ExtentedResolver
 * Is used if a resolver, e.g. the portal has to be extended by some custom
 * user properties represented as a HashMap.
 * 
 * Method setBasicResolver(Resolveable) is used to set the basic properties (e.g. Portal)
 * 
 * Method setExtendedProperties(Resolveable) is used to set the extending properties
 * 
 *
 * @author robert
 * @date 17.12.2004
 * @version $Id: ExtentedResolver.java,v 1.6 2006-01-13 15:25:41 herbert Exp $
 *
 */
package com.gentics.lib.base;

import java.util.HashMap;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * @author robert
 * @deprecated should not be used in new developments. use StackResolver instead.
 */
public class ExtentedResolver implements Resolvable {

	// extended properties
	public HashMap extentedProperties;

	// basic resolver
	public Resolvable basicResolver;

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getPropertyNames()
	 */
	public HashMap getPropertyNames() {
		// TODO Auto-generated method stub
		// HashMap

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		// TODO Auto-generated method stub

		Object object;

		if (this.extentedProperties != null && this.extentedProperties.containsKey(key)) {
			object = this.extentedProperties.get(key);
		} else {
			object = this.basicResolver.getProperty(key);
		}

		return object;
	}

	public Object get(String key) {
		return getProperty(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * used to set the extended properties
	 * @param properties HashMap
	 */
	public void setExtendedProperties(HashMap properties) {

		this.extentedProperties = properties;

	}

	/**
	 * set the basic resolver
	 * @param basicResolver Resolvable
	 */
	public void setBasicResolver(Resolvable basicResolver) {

		this.basicResolver = basicResolver;

	}

}
