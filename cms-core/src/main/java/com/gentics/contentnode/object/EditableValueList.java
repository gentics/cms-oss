/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: EditableValueList.java,v 1.9 2008-05-26 15:05:56 norbert Exp $
 */
package com.gentics.contentnode.object;


import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.rest.util.MiscUtils;
import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.SetUtils;

/**
 * This is an implementation of a ValueList, where values can be added using the {@link #addValue(Value)} method.
 */
public class EditableValueList implements ValueList {
	protected static NodeLogger logger = NodeLogger.getNodeLogger(EditableValueList.class);

	/**
	 * A total ordering of values contained by a tag using the part order and part id.
	 * The part id is added just in case, because the part order theoretically allows for duplicates.
	 * If a value has no part, it will get ordered first. If a value's part has no ID, it will be ordered first.
	 */
	private static final Comparator<Value> valueOrder = new Comparator<Value>(){

		public int compare(Value o1, Value o2) {
			try {
				Part p1 = o1.getPart(false);
				Part p2 = o2.getPart(false);

				if (p1==null && p2==null) {
					return 0;
				}
				if (p1==null) {
					return -1;
				}
				if (p2==null) {
					 return 1;
				}
				int order1 = o1.getPart(false).getPartOrder();
				int order2 = o2.getPart(false).getPartOrder();

				if (order1 < order2) {
					return -1;
				}
				if (order1 > order2) {
					return 1;
				}
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}

			Integer id1 = (Integer) o1.getPartId();
			Integer id2 = (Integer) o2.getPartId();

			if (id1 == null && id2 == null) {
				return 0;
			}
			if (id1 == null) {
				return -1;
			}
			if (id2 == null) {
				return 1;
			}

			if (id1 < id2) {
				return -1;
			}
			if (id1 > id2) {
				return 1;
			}

			return 0;
		}
	};

	/**
	 * Iterator containing a wrapped iterator that
	 * keeps partIds and keyNames of the outer class up to date
	 * @author escitalopram
	 *
	 */
	private class WrappedIterator implements Iterator<Value>{
		Iterator<Value> wrappedIterator;
		Value current;
		boolean beforeFirst = true;
		boolean removed;
		public WrappedIterator(Iterator<Value> i) {
			this.wrappedIterator = i;
		}
		public boolean hasNext() {
			return wrappedIterator.hasNext();
		}

		public Value next() {
			current = wrappedIterator.next();
			removed = false;
			beforeFirst = false;
			return current;
		}

		public void remove() {
			if (beforeFirst || removed) {
				throw new IllegalStateException("Cannot remove at this point");
			}
			try {
				Part part = current.getPart();
				wrappedIterator.remove();
				EditableValueList.this.partIds.remove(current.getPartId());
				EditableValueList.this.keynames.remove(part.getKeyname());
				removed = true;
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private TreeSet<Value> values;
	private TreeMap<Integer, Value> partIds;
	private TreeMap<String, Value> keynames;
	// TODO uniqueTagId is currently initialized by the constructor.
	private Object uniqueTagId;

	/**
	 * Create a new value-list with a default initial capacity.
	 * @param uniqueTagId unique tag ID
	 */
	public EditableValueList(Object uniqueTagId) {
		values = new TreeSet<Value>(valueOrder);
		partIds = new TreeMap<Integer, Value>();
		keynames = new TreeMap<String, Value>();
		this.uniqueTagId = uniqueTagId;
	}

	/**
	 * Create an new value list as copy of the given source list
	 * @param uniqueTagId unique tag ID
	 * @param source source list
	 * @param container value container for the value copies
	 * @throws NodeException
	 */
	public EditableValueList(Object uniqueTagId, ValueList source, ValueContainer container) throws NodeException {
		this(uniqueTagId);

		if (source != null) {
			Objects.requireNonNull(container, "Container must not be null when copying values from source list");
			for (Value v : source) {
				Value valueCopy = (Value) v.copy();

				valueCopy.setContainer(container);

				PartType origPT = v.getPartType();
				PartType newPT = valueCopy.getPartType();

				newPT.copyFrom(origPT);

				addValue(valueCopy);
			}
		}
	}

	@Override
	public Set<String> getResolvableKeys() {
		return SetUtils.union(keynames.keySet(), SetUtils.hashSet("unique_tag_id"));
	}

	/**
	 * Add a new value to the list.
	 *
	 * @param value the value to be added to the list.
	 * @return true if the collection has changed as a result of this operation
	 */
	public boolean addValue(Value value) throws NodeException {
		if (value == null) {
			return false;
		}
		Part part = value.getPart(false);
		if (part == null) {
			logger.warn("Found value without part! id [" + value.getId() + "]");
		}
		if (value.getPartId() != null) {
			partIds.put((Integer) value.getPartId(), value);
		}
		if (values.add(value) && part != null) {
			// do not store values by keyname, if the part has no key
			if (!StringUtils.isEmpty(part.getKeyname())) {
				keynames.put(part.getKeyname(), value);
			}
			return true;
		}
		return false;
	}

	public int size() {
		return values.size();
	}

	public Value getByPartId(Object partId) {
		if (partId == null) {
			return null;
		}
		return partIds.get(partId);
	}

	public Value getByKeyname(String keyname) {
		return keynames.get(keyname);
	}

	@Override
	public Value getById(int id) {
		return values.stream().filter(v -> Objects.equals(v.getId(), id)).findFirst().orElse(null);
	}

	public Object getProperty(String key) {
		return get(key);
	}

	public Object get(String key) {
		if ("unique_tag_id".equals(key)) {
			return uniqueTagId;
		}
		return getByKeyname(key);
	}

	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		values.clear();
		partIds.clear();
		keynames.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return values.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		return values.toArray(new Object[values.size()]);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Value v) {
		try {
			return addValue(v);
		} catch (NodeException e) {
			logger.error("Error while adding value", e);
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return values.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		boolean changed = false;
		if (values.contains(o)) {
			Value v = (Value) o;
			changed = true;
			values.remove(v);
			partIds.remove((Integer)v.getPartId());
			try {
				keynames.remove(v.getPart().getKeyname());
			} catch (NodeException e) {
				throw new RuntimeException(e);
			}
		}
		return changed;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends Value> c) {
		boolean changed = false;

		for (Value value : c) {
			changed |= add(value);
		}
		return changed;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return values.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		boolean changed = false;

		for (Object object : c) {
			changed |= remove(object);
		}
		return changed;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		boolean changed = false;

		for (Iterator<Value> i = iterator(); i.hasNext();) {
			if (!c.contains(i.next())) {
				i.remove();
				changed = true;
			}
		}
		return changed;
	}

	public Iterator<Value> iterator() {
		return new WrappedIterator(values.iterator());
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(java.lang.Object[])
	 */
	public <T extends Object> T[] toArray(T[] a) {
		return values.toArray(a);
	}

	@Override
	public boolean hasSameValues(ValueList other) throws NodeException {
		return MiscUtils.equals(this, other, (v1, v2) -> v1.getPartType().hasSameContent(v2.getPartType()));
	}
}
