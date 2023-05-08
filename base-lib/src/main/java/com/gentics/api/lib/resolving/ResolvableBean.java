/*
 * @author norbert
 * @date 31.08.2005
 * @version $Id: ResolvableBean.java,v 1.1 2006-01-13 15:25:41 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

import java.io.Serializable;

import com.gentics.lib.util.ClassHelper;

/**
 * ChangeableBean serves as base class for JavaBeans that need to be Resolvable.
 * The implementation of the Resolvable interface maps properties to the
 * corresponding getter methods (when they exist). To use this JavaBean
 * integration into Portal.Node, simply let your BeanObject extend this class
 * and create getter methods for all properties. When your bean should be
 * Changeable you should rather extend {@link ChangeableBean}
 * @author norbert
 */
abstract public class ResolvableBean implements Resolvable, Serializable {

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		// simply call the getter on the object
		try {
			return ClassHelper.invokeGetter(this, key);
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}
}
