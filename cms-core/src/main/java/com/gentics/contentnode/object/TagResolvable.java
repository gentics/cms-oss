/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: TagResolvable.java,v 1.8 2010-09-28 17:01:31 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.events.DependencyObject;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.log.NodeLogger;

/**
 * This is a helper class to resolve tags by name using a {@link TagContainer}.
 * If the container is an objecttagcontainer, the objecttags can also be
 * resolved using the key 'object'.<br/>
 * Instances of this class represent a snapshot of the tags from the time of their creation.
 */
public class TagResolvable implements Resolvable, Collection {

	/**
	 * tag container
	 */
	private TagContainer container;

	/**
	 * object resolver
	 */
	private Resolvable objResolver;

	/**
	 * tags
	 */
	private Map tags;

	/**
	 * logger
	 */
	protected static NodeLogger logger = NodeLogger.getNodeLogger(TagResolvable.class);

	/**
	 * Create a new resolvable wrapper using the given tagcontainer.
	 * @param container the tag container used to resolve tags.
	 */
	public TagResolvable(TagContainer container) {
		this.container = container;
		if (this.container != null) {
			try {
				tags = this.container.getTags();
			} catch (NodeException e) {
				logger.error("Error while getting tags of {" + container + "}", e);
				tags = Collections.EMPTY_MAP;
			}
		} else {
			tags = Collections.EMPTY_MAP;
		}

		if (container instanceof ObjectTagContainer) {
			objResolver = new ObjectTagResolvable((ObjectTagContainer) container);
		} else {
			objResolver = null;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Tags of " + container;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		if (objResolver != null && "object".equals(key)) {
			return objResolver;
		}
		Object tag = tags.get(key);

		if (tag != null) {
			addDependency(key, tag);
		}
		return tag;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return tags.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		throw new UnsupportedOperationException("This collection is unmodifiable");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		return tags.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		return toArray(new Object[tags.values().size()]);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(Object o) {
		throw new UnsupportedOperationException("This collection is unmodifiable");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return tags.values().contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("This collection is unmodifiable");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException("This collection is unmodifiable");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection c) {
		return tags.values().containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException("This collection is unmodifiable");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException("This collection is unmodifiable");
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
	public Iterator iterator() {
		// wee need to provide a dummy iterator to add dependencies for all accessed tags.
		final Iterator i = tags.values().iterator();

		return new Iterator() {
            
			public boolean hasNext() {
				return i.hasNext();
			}

			public Object next() {
				Object obj = i.next();

				if (obj instanceof Tag) {
					addDependency(((Tag) obj).getName(), obj);
				}
				return obj;
			}

			public void remove() {
				throw new UnsupportedOperationException("Not modifiable.");
			}
            
		};
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(java.lang.Object[])
	 */
	public Object[] toArray(Object[] a) {
		a = tags.values().toArray(a);
        
		// dependencies are implemented by the iterator, call it for the side-effect
		for (Iterator i = iterator(); i.hasNext(); i.next()) {
			;
		}
        
		return a;
	}

	/**
	 * Add the dependency on the resolved object (when it was resolved)
	 * @param property resolved property
	 * @param resolvedObject object (value of the resolved property)
	 */
	protected void addDependency(String property, Object resolvedObject) {
		try {
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();

			if (renderType.doHandleDependencies() && container instanceof NodeObject && resolvedObject instanceof NodeObject) {
				renderType.addDependency(new DependencyObject((NodeObject) container, (NodeObject) resolvedObject), null);
			}
		} catch (NodeException e) {
			logger.error("Error while adding dependency {" + this + "}/{" + resolvedObject + "}", e);
		}
	}
}
