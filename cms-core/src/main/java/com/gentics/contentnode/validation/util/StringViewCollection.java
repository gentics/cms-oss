/*
 * @author tobiassteiner
 * @date Jan 15, 2011
 * @version $Id: StringViewCollection.java,v 1.1.2.1 2011-02-10 13:43:40 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Like {@link StringViewIterable} but wraps a collection.
 */
public class StringViewCollection extends AbstractCollection<String> {
	protected final Collection<?> collection;
    
	public StringViewCollection(Collection<?> collection) {
		this.collection = collection;
	}

	@Override
	public Iterator<String> iterator() {
		return new StringViewIterable.StringViewIterator(collection.iterator());
	}

	@Override
	public int size() {
		return collection.size();
	}
}
