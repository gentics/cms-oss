/*
 * @(#) ResolvableGenticsContentObject/ResolvableGenticsContentObject.java   1.0   19.08.2004 18:29:49
 *
 * Copyright 2004 Gentics Net.Solutions
 *
 * created on 19.08.2004 by Robert Reinhardt 
 *
 * Canges:
 */
package com.gentics.lib.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.base.CMSUnavailableException;
import com.gentics.lib.base.NodeIllegalArgumentException;

/**
 * @author robert
 * @deprecated GenticsContentObjects are now Resolvables themselves
 */
public class ResolvableGenticsContentObject implements Resolvable, Comparable {

	private GenticsContentObject contentobject;

	public ResolvableGenticsContentObject(GenticsContentObject contenObject) {
		this.contentobject = contenObject;

	}

	protected Object getStringAttribute(GenticsContentAttribute attrib) throws NodeIllegalArgumentException, CMSUnavailableException {
		if (this.contentobject == null) {
			return null;
		}
		if (attrib != null) {
			if (!attrib.isMultivalue()) {
				return attrib.getNextValue();
			} else {
				ArrayList ret = new ArrayList(20);
				Iterator it = attrib.valueIterator();

				while (it.hasNext()) {
					String s = (String) it.next();

					ret.add(s);
				}
				return ret;
			}
		}
		return null;
	}

	protected Object getObjectAttribute(GenticsContentAttribute attrib) throws NodeIllegalArgumentException, CMSUnavailableException {
		if (this.contentobject == null) {
			return null;
		}
		if (attrib != null) {
			if (!attrib.isMultivalue() && attrib.getAttributeType() != GenticsContentAttribute.ATTR_TYPE_FOREIGNOBJ) {
				attrib.resetIterator();
				return getAsResolvable(attrib.getNextContentObject(), attrib.getAttributeName(), 0);
			} else {
				attrib.resetIterator();
				ArrayList ret = new ArrayList(20);
				Iterator it = attrib.objectIterator();
				int i = 0;

				while (it.hasNext()) {
					Object nextObject = it.next();

					if (nextObject instanceof GenticsContentObject) {
						ret.add(getAsResolvable((GenticsContentObject) nextObject, attrib.getAttributeName(), i));
						i++;
					} else {
						throw new NodeIllegalArgumentException(
								"attribute " + attrib.getAttributeName() + " is supposed to return GenticsContentObject but returned object of class "
								+ nextObject.getClass().getName());
					}
				}
				return ret;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getPropertyNames()
	 */
	public HashMap getPropertyNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object get(String key) {
		return getProperty(key);
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		if (this.contentobject == null) {
			return null;
		}
		return this.contentobject.getProperty(key);
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
	 * @return Returns the contentobject.
	 */
	public GenticsContentObject getContentobject() {
		return contentobject;
	}

	/**
	 * internal method to get values of an object attribute as resolvable
	 * objects. must be overwritten in subclasses to provide compatibility
	 * @param object GenticsContentObject
	 * @param attributeName name of the attribute
	 * @param number index of the object in the list of objects (for multivalue
	 *        or foreign objects)
	 * @return a Resolvable object
	 */
	protected ResolvableGenticsContentObject getAsResolvable(GenticsContentObject object,
			String attributeName, int number) {
		return new ResolvableGenticsContentObject(object);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return contentobject != null ? contentobject.getContentId() : null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		// compare the GenticsContentObjects
		if (obj instanceof ResolvableGenticsContentObject) {
			return contentobject.equals(((ResolvableGenticsContentObject) obj).contentobject);
		} else if (obj instanceof GenticsContentObject) {
			return contentobject.equals(obj);
		} else if (obj instanceof String) {
			return contentobject.equals(obj);
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (contentobject == null) {
			return -1;
		}

		if (o instanceof GenticsContentObject) {
			return contentobject.getContentId().compareTo(((GenticsContentObject) o).getContentId());
		} else if (o instanceof ResolvableGenticsContentObject) {
			return contentobject.getContentId().compareTo(((ResolvableGenticsContentObject) o).getContentobject().getContentId());
		} else {
			// TODO this cast origins from java 1.4's compareTo(Object) implementation, clean this up.
			return contentobject.getContentId().compareTo((String) o);
		}
	}
}
