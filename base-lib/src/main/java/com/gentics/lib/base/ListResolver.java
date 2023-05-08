/*
 * @author norbert
 * @date 21.08.2007
 * @version $Id: ListResolver.java,v 1.2 2008-05-28 11:53:52 herbert Exp $
 */
package com.gentics.lib.base;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Resolvable wrapper around a List
 */
public class ListResolver implements List, Resolvable {

	/**
	 * resolved list
	 */
	protected List list;

	/**
	 * Create an instance of the list
	 * @param list list
	 */
	public ListResolver(List list) {
		this.list = list;
	}

	/* (non-Javadoc)
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	public void add(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	public boolean addAll(int index, Collection c) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#clear()
	 */
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return list.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection c) {
		return list.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#get(int)
	 */
	public Object get(int index) {
		return list.get(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.List#iterator()
	 */
	public Iterator iterator() {
		return list.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator()
	 */
	public ListIterator listIterator() {
		return list.listIterator();
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator listIterator(int index) {
		return list.listIterator(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(int)
	 */
	public Object remove(int index) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	public Object set(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.List#size()
	 */
	public int size() {
		return list.size();
	}

	/* (non-Javadoc)
	 * @see java.util.List#subList(int, int)
	 */
	public List subList(int fromIndex, int toIndex) {
		return new ListResolver(list.subList(fromIndex, toIndex));
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray()
	 */
	public Object[] toArray() {
		return list.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray(java.lang.Object[])
	 */
	public Object[] toArray(Object[] a) {
		return list.toArray(a);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Object name = (Object) iterator.next();

			if (name != null && name.equals(key)) {
				return name;
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}
    
	public String toString() {
		return list.toString();
	}
}
