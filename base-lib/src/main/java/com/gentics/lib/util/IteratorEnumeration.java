/*
 * @author norbert
 * @date 25.07.2005
 * @version $Id: IteratorEnumeration.java,v 1.1 2005/08/11 14:27:44 norbert Exp $
 */
package com.gentics.lib.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * wrapper class for converting iterators to enumerations
 * @author norbert
 */
public class IteratorEnumeration<T> implements Enumeration<T> {

	/**
	 * wrapped iterator
	 */
	protected Iterator<T> iterator;

	/**
	 * 
	 */
	public IteratorEnumeration(Iterator<T> iterator) {
		this.iterator = iterator;
	}

	/* (non-Javadoc)
	 * @see java.util.Enumeration#hasMoreElements()
	 */
	public boolean hasMoreElements() {
		return iterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see java.util.Enumeration#nextElement()
	 */
	public T nextElement() {
		return iterator.next();
	}
}
