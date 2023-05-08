/*
 * @author norbert
 * @date 31.08.2005
 * @version $Id: ChangeableBean.java,v 1.3 2006-01-26 10:23:53 herbert Exp $
 * @gentics.sdk
 */
package com.gentics.api.lib.resolving;

import com.gentics.api.lib.exception.InsufficientPrivilegesException;
import com.gentics.lib.util.ClassHelper;

/**
 * ChangeableBean serves as base class for JavaBeans that need to be Changeable.
 * The implementation of the Changeable and Resolvable interfaces map properties
 * to the corresponding getter and setter methods (when they exist). To use this
 * JavaBean integration into Portal.Node, simply let your BeanObject extend this
 * class and create getter/setter methods for all properties.
 * @author norbert
 */
abstract public class ChangeableBean extends ResolvableBean implements Changeable {

	/* (non-Javadoc)
	 * @see com.gentics.lib.base.Changeable#setProperty(java.lang.String, java.lang.Object)
	 */
	public boolean setProperty(String name, Object value) throws InsufficientPrivilegesException {
		// simply call the setter on the object
		try {
			ClassHelper.invokeSetter(this, name, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
