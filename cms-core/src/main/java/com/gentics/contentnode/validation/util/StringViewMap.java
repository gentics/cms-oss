/*
 * @author tobiassteiner
 * @date Jan 15, 2011
 * @version $Id: StringViewMap.java,v 1.1.2.2 2011-02-15 10:55:14 norbert Exp $
 */
package com.gentics.contentnode.validation.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements an unmodifiable view of a map with keys and values of any type.
 * The toString() method of the keys and values will be used to arrive at
 * the string representation.
 * 
 * The methods equals() and hashcode() are not passed on to the underlying map.
 */
public class StringViewMap extends AbstractMap<String, String> {

	protected final Map<?, ?> map;
    
	public StringViewMap(Map<?, ?> map) {
		this.map = map;
	}

	// We must be able to assume that the map that backs this view always
	// returns an entry set of type Set<Entry<?,?>>, which is not an
	// unreasonable assumption to make.
	@SuppressWarnings("unchecked")
	@Override
	public Set<Entry<String, String>> entrySet() {
		Set entrySet = map.entrySet();

		return new StringViewEntrySet(entrySet);
	}
    
	public static class StringViewEntrySet extends AbstractSet<Entry<String, String>> {
		protected final Set<Entry<?, ?>> entrySet;
        
		public StringViewEntrySet(Set<Entry<?, ?>> entrySet) {
			this.entrySet = entrySet;
		}

		@Override
		public Iterator<java.util.Map.Entry<String, String>> iterator() {
			return new EntryIterator(entrySet.iterator());
		}
        
		@Override
		public int size() {
			return entrySet.size();
		}
	}
    
	public static class EntryIterator implements Iterator<Entry<String, String>> {
		protected final Iterator<Entry<?, ?>> entryIterator;

		public EntryIterator(Iterator<Entry<?, ?>> entryIterator) {
			this.entryIterator = entryIterator;
		}
        
		public boolean hasNext() {
			return entryIterator.hasNext();
		}

		public java.util.Map.Entry<String, String> next() {
			return new StringViewEntry(entryIterator.next());
		}

		public void remove() {
			throw new UnsupportedOperationException("this map is immutable");
		}
	}
    
	public static class StringViewEntry implements Entry<String, String> {
		protected final Entry<?, ?> entry;
        
		public StringViewEntry(Entry<?, ?> entry) {
			this.entry = entry;
		}

		public String getKey() {
			return entry.getKey().toString();
		}

		public String getValue() {
			return entry.getValue().toString();
		}

		/**
		 * This method is the actual reason why this map
		 * implementation is immutable. We can't convert a string
		 * back to the object that it represents, so this operation
		 * doesn't make sense for a string-view of the map. 
		 */
		public String setValue(String value) {
			throw new UnsupportedOperationException("this map is immutable");
		}
	}

	;
}
