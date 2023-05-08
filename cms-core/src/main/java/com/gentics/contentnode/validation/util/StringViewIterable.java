/*
 * @author tobiassteiner
 * @date Jan 15, 2011
 * @version $Id: StringViewIterable.java,v 1.1.2.1 2011-02-10 13:43:39 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.Iterator;

/**
 * Implements a view of a given iterable and iterator. The only additional
 * functionality is implemented in the StringViewIterator.next() method.
 * 
 * The methods equals() and hashcode() are not passed on to the underlying
 * iterable. 
 **/
public class StringViewIterable implements Iterable<String> {
	protected final Iterable<?> iterable;
    
	public StringViewIterable(Iterable<?> iterable) {
		this.iterable = iterable;
	}
    
	public Iterator<String> iterator() {
		return new StringViewIterator(iterable.iterator());
	}
    
	public static class StringViewIterator implements Iterator<String> {
		protected final Iterator<?> iterator;
        
		public StringViewIterator(Iterator<?> iterator) {
			this.iterator = iterator;
		}

		/**
		 * @return the string representation of the object
		 *   returned by the hasNext() method of the underlying
		 *   iterator. 
		 */
		public boolean hasNext() {
			return iterator.hasNext();
		}

		public String next() {
			return iterator.next().toString();
		}

		public void remove() {
			iterator.remove();
		}
	}
    
	public String toString() {
		return iterable.toString();
	}
}
