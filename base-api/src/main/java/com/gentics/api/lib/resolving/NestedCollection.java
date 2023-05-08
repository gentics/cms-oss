/*
 * @author norbert
 * @date 17.07.2006
 * @version $Id: NestedCollection.java,v 1.1 2006-07-19 09:01:59 norbert Exp $
 */
package com.gentics.api.lib.resolving;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

/**
 * This class is just a wrapper aroung a vector. The
 * com.gentics.api.lib.resolving.PropertyResolver might return instances
 * of this class for property paths that contain collections of objects on the
 * way to the resolved attribute (but not the last part of the property path).
 * <p>
 * Example: When resolving the property
 * 
 * <pre>
 * object.users.name
 * </pre>
 * 
 * where users returns a collection of linked objects, the resulting collection
 * is an instance of this class.
 * 
 * @author norbert
 */
public class NestedCollection extends Vector {
	protected Collection innerCollection = new Vector();

	/**
	 * generated serial version UID
	 */
	private static final long serialVersionUID = 7516872677478414809L;

	/* (non-Javadoc)
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, Object element) {
		innerCollection.add(element);
		super.add(index, element);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public synchronized boolean add(Object o) {
		innerCollection.add(o);
		return super.add(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public synchronized boolean addAll(Collection c) {
		innerCollection.add(c);
		return super.addAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public synchronized boolean addAll(int index, Collection c) {
		innerCollection.add(c);
		return super.addAll(index, c);
	}

	/* (non-Javadoc)
	 * @see java.util.Vector#addElement(java.lang.Object)
	 */
	public synchronized void addElement(Object obj) {
		innerCollection.add(obj);
		super.addElement(obj);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		innerCollection.clear();
		super.clear();
	}

	/**
	 * Get the inner collection (unmodifiable)
	 * @return inner collection
	 */
	public Collection getInnerCollection() {
		return Collections.unmodifiableCollection(innerCollection);
	}
}
