/*
 * @author herbert
 * @date 17.05.2006
 * @version $Id: CollectionResolver.java,v 1.3 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.lib.base;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * a simple resolver which allows access to the elements through
 * a resolver.
 */
public class CollectionResolver implements Collection, Resolvable, Serializable {
	private static final long serialVersionUID = 8358950223105950958L;
    
	private Collection collection;

	public CollectionResolver(Collection collection) {
		this.collection = collection;
	}

	public int size() {
		return collection.size();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
		return collection.isEmpty();
	}

	public Object[] toArray() {
		return collection.toArray();
	}

	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		return collection.contains(o);
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection c) {
		return collection.containsAll(c);
	}

	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public Iterator iterator() {
		return collection.iterator();
	}

	public Object[] toArray(Object[] a) {
		return collection.toArray(a);
	}

	public Object getProperty(String key) {
		if (collection instanceof List) {
			return ((List) collection).get(ObjectTransformer.getInt(key, 0));
		}
		return null;
	}

	public Object get(String key) {
		return getProperty(key);
	}

	public boolean canResolve() {
		return true;
	}

}
