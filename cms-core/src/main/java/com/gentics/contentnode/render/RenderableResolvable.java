/*
 * @author norbert
 * @date 17.04.2007
 * @version $Id: RenderableResolvable.java,v 1.10 2009-12-16 16:12:07 herbert Exp $
 */
package com.gentics.contentnode.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionException;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.resolving.StackResolvable;
import com.gentics.lib.etc.MiscUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.Renderable;

/**
 * Wrapper around a resolvable that renders a renderable object upon invocation of
 * {@link #toString()}.
 */
public class RenderableResolvable implements Resolvable, Comparable, GCNRenderable {

	/**
	 * wrapped object
	 */
	private Object wrappedObject;

	/**
	 * wrapped resolvable
	 */
	private Resolvable wrappedResolvable;

	/**
	 * wrapped renderable
	 */
	private GCNRenderable wrappedRenderable;

	/**
	 * object that defines the scope in which this object is rendered
	 */
	private NodeObject scope;

	/**
	 * List of objects to be put upon the render stack while getting a property from the object
	 */
	private List<StackResolvable> stack = new Vector<StackResolvable>();

	/**
	 * logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(RenderableResolvable.class);

	/**
	 * Create resolvable wrapper
	 * @param wrappedObject wrapped object
	 */
	public RenderableResolvable(Object wrappedObject) {
		this(wrappedObject, null, null);
	}

	/**
	 * Create resolvable wrapper
	 * @param wrappedObject wrapped object
	 * @param scope object defining the render scope (may be null)
	 * @param mother mother object, from which this object was resolved
	 */
	public RenderableResolvable(Object wrappedObject, NodeObject scope, RenderableResolvable mother) {
		this.scope = scope;
		if (wrappedObject instanceof GCNRenderable) {
			this.wrappedRenderable = (GCNRenderable) wrappedObject;
		}
		if (wrappedObject instanceof Resolvable) {
			this.wrappedResolvable = (Resolvable) wrappedObject;
		}

		this.wrappedObject = wrappedObject;

		// get the stack from the mother
		if (mother != null) {
			stack.addAll(mother.getStack());
		}
		// add the wrapped object
		if (wrappedObject instanceof StackResolvable) {
			stack.add((StackResolvable) wrappedObject);
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return get(key);
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
	 */
	public Object get(String key) {
		if (wrappedResolvable != null) {
			RenderType renderType = null;
			Transaction t = null;
			int numPushed = 0;

			try {
				t = TransactionManager.getCurrentTransaction();
				if (t != null) {
					renderType = t.getRenderType();
				}
			} catch (TransactionException e) {}
			try {
				// push the stack onto the rendertype
				if (renderType != null && !stack.isEmpty()) {
					for (StackResolvable res : stack) {
						renderType.push(res);
						numPushed++;
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Resolving {" + key + "} for {" + wrappedResolvable + "}");
				}
				return wrapObject(wrappedResolvable.get(key), getSubScope(), this);
			} finally {
				// pop the stack from the rendertype
				for (int i = 0; i < numPushed; ++i) {
					renderType.pop();
				}
			}
		} else {
			return null;
		}
	}

	/**
	 * Get the scope for resolved objects
	 * @return scope for resolved objects
	 */
	public NodeObject getSubScope() {
		// when resolving from a folder, file or page, we switch the scope
		if (wrappedResolvable instanceof Folder) {
			return (Folder) wrappedResolvable;
		} else if (wrappedResolvable instanceof ContentFile) {
			return (ContentFile) wrappedResolvable;
		} else if (wrappedResolvable instanceof Page) {
			return (Page) wrappedResolvable;
		} else if (wrappedResolvable instanceof Tag) {
			return (Tag) wrappedResolvable;
		} else if (wrappedResolvable instanceof Value) {
			// when resolving from a value of a URLPartType, we switch the scope
			// to the target object (if any set)
			try {
				PartType partType = ((Value) wrappedResolvable).getPartType();

				if (partType instanceof UrlPartType) {
					NodeObject target = ((UrlPartType) partType).getTarget();

					if (target != null) {
						return target;
					}
				}
			} catch (NodeException e) {}
		}
		return scope;
	}

	/**
	 * Wrap the object such that all Renderables are rendered upon invocation of
	 * {@link #toString()}.
	 * @param wrappedObject wrapped object
	 * @param scope scope
	 * @param mother mother
	 * @return wrapper
	 */
	public static Object wrapObject(Object wrappedObject, NodeObject scope, RenderableResolvable mother) {
		if (wrappedObject instanceof Map) {
			return new MapWrapper((Map) wrappedObject, scope, mother);
		} else if (wrappedObject instanceof Set) {
			return new SetWrapper((Set) wrappedObject, scope, mother);
		} else if (wrappedObject instanceof Collection) {
			return new CollectionWrapper((Collection) wrappedObject, scope, mother);
		} else if (wrappedObject instanceof Renderable || wrappedObject instanceof Resolvable) {
			return new RenderableResolvable(wrappedObject, scope, mother);
		} else {
			return wrappedObject;
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.Resolvable#canResolve()
	 */
	public boolean canResolve() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (wrappedRenderable != null) {
			try (Scope scope = scope()) {
				try {
					return wrappedRenderable.render(TransactionManager.getCurrentTransaction().getRenderResult());
				} catch (Exception e) {
					logger.error("Error while rendering " + wrappedRenderable, e);
					return null;
				}
			} catch (NodeException e) {
				logger.error("Error while rendering " + wrappedRenderable, e);
				return null;
			}
		} else if (wrappedObject != null) {
			return wrappedObject.toString();
		} else {
			return null;
		}
	}

	/**
	 * Wrapper for collections
	 */
	public static class CollectionWrapper extends RenderableResolvable implements Collection {

		/**
		 * wrapped collection
		 */
		protected Collection wrappedCollection;

		/**
		 * Create a wrapper for the given collection
		 * @param wrappedCollection wrapped collection
		 * @param scope object defining the render scope (may be null)
		 * @param mother mother object
		 */
		public CollectionWrapper(Collection wrappedCollection, NodeObject scope, RenderableResolvable mother) {
			super(wrappedCollection, scope, mother);
			this.wrappedCollection = wrappedCollection;
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#size()
		 */
		public int size() {
			return wrappedCollection.size();
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#clear()
		 */
		public void clear() {
			throw new UnsupportedOperationException("Collection is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#isEmpty()
		 */
		public boolean isEmpty() {
			return wrappedCollection.isEmpty();
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#toArray()
		 */
		public Object[] toArray() {
			return (Object[]) toArray(new Object[wrappedCollection.size()]);
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#add(java.lang.Object)
		 */
		public boolean add(Object o) {
			throw new UnsupportedOperationException("Collection is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#contains(java.lang.Object)
		 */
		public boolean contains(Object o) {
			return wrappedCollection.contains(o);
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#remove(java.lang.Object)
		 */
		public boolean remove(Object o) {
			throw new UnsupportedOperationException("Collection is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#addAll(java.util.Collection)
		 */
		public boolean addAll(Collection c) {
			throw new UnsupportedOperationException("Collection is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#containsAll(java.util.Collection)
		 */
		public boolean containsAll(Collection c) {
			return wrappedCollection.containsAll(c);
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#removeAll(java.util.Collection)
		 */
		public boolean removeAll(Collection c) {
			throw new UnsupportedOperationException("Collection is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#retainAll(java.util.Collection)
		 */
		public boolean retainAll(Collection c) {
			throw new UnsupportedOperationException("Collection is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#iterator()
		 */
		public Iterator iterator() {
			return new IteratorWrapper(wrappedCollection.iterator(), getSubScope(), this);
		}

		/* (non-Javadoc)
		 * @see java.util.Collection#toArray(java.lang.Object[])
		 */
		public Object[] toArray(Object[] a) {
			Object[] array = wrappedCollection.toArray(a);

			for (int i = 0; i < array.length; i++) {
				array[i] = wrapObject(array[i], getSubScope(), this);
			}

			return array;
		}
		//
		// /* (non-Javadoc)
		// * @see com.gentics.api.lib.resolving.Resolvable#get(java.lang.String)
		// */
		// public Object get(String key) {
		// Object superProp = super.get(key);
		// if (superProp != null) {
		// return superProp;
		// }
		//
		// // TODO do we really want this behaviour?
		// List properties = new Vector();
		//
		// for (Iterator iter = iterator(); iter.hasNext();) {
		// Object element = (Object) iter.next();
		//
		// if (element instanceof Resolvable) {
		// Object prop = ((Resolvable) element).get(key);
		// if (prop != null) {
		// properties.add(prop);
		// }
		// }
		// }
		//
		// return ObjectTransformer.isEmpty(properties) ? null : properties;
		// }
	}

	/**
	 * wrapper for sets
	 */
	public static class SetWrapper extends CollectionWrapper implements Set {

		/**
		 * Wrap the given set
		 * @param wrappedSet wrapped set
		 * @param scope object defining the render scope (may be null)
		 * @param mother mother
		 */
		public SetWrapper(Set wrappedSet, NodeObject scope, RenderableResolvable mother) {
			super(wrappedSet, scope, mother);
		}
	}

	/**
	 * Wrapper for maps (wraps all retrieved objects)
	 */
	public static class MapWrapper extends RenderableResolvable implements Map {

		/**
		 * wrapped map
		 */
		protected Map wrappedMap;

		/**
		 * Create map wrapper for the given map
		 * @param wrappedMap wrapped map
		 * @param scope object defining the render scope (may be null)
		 * @param mother mother
		 */
		public MapWrapper(Map wrappedMap, NodeObject scope, RenderableResolvable mother) {
			super(wrappedMap, scope, mother);
			this.wrappedMap = wrappedMap;
		}

		/* (non-Javadoc)
		 * @see java.util.Map#size()
		 */
		public int size() {
			return wrappedMap.size();
		}

		/* (non-Javadoc)
		 * @see java.util.Map#clear()
		 */
		public void clear() {
			throw new UnsupportedOperationException("Map is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Map#isEmpty()
		 */
		public boolean isEmpty() {
			return wrappedMap.isEmpty();
		}

		/* (non-Javadoc)
		 * @see java.util.Map#containsKey(java.lang.Object)
		 */
		public boolean containsKey(Object key) {
			return wrappedMap.containsKey(key);
		}

		/* (non-Javadoc)
		 * @see java.util.Map#containsValue(java.lang.Object)
		 */
		public boolean containsValue(Object value) {
			return wrappedMap.containsValue(value);
		}

		/* (non-Javadoc)
		 * @see java.util.Map#values()
		 */
		public Collection values() {
			return (Collection) wrapObject(wrappedMap.values(), getSubScope(), this);
		}

		/* (non-Javadoc)
		 * @see java.util.Map#putAll(java.util.Map)
		 */
		public void putAll(Map t) {
			throw new UnsupportedOperationException("Map is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Map#entrySet()
		 */
		public Set entrySet() {
			return (Set) wrapObject(wrappedMap.entrySet(), getSubScope(), this);
		}

		/* (non-Javadoc)
		 * @see java.util.Map#keySet()
		 */
		public Set keySet() {
			return (Set) wrapObject(wrappedMap.keySet(), getSubScope(), this);
		}

		/* (non-Javadoc)
		 * @see java.util.Map#get(java.lang.Object)
		 */
		public Object get(Object key) {
			return wrapObject(wrappedMap.get(key), getSubScope(), this);
		}
        
		/**
		 * we need to overwrite get method of parent class, so the right one is called
		 */
		public Object get(String key) {
			return get((Object) key);
		}

		/* (non-Javadoc)
		 * @see java.util.Map#remove(java.lang.Object)
		 */
		public Object remove(Object key) {
			throw new UnsupportedOperationException("Map is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
		 */
		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException("Map is not modifiable");
		}
	}

	/**
	 * Wrapper for iterators
	 */
	public static class IteratorWrapper implements Iterator {

		/**
		 * wrapped iterator
		 */
		protected Iterator wrappedIterator;

		/**
		 * scope of the iterated objects
		 */
		protected NodeObject scope;

		/**
		 * Mother object
		 */
		protected RenderableResolvable mother;

		/**
		 * Wrap the given iterator
		 * @param wrappedIterator wrapped iterator
		 * @param scope of the iterated objects
		 * @param mother mother
		 */
		public IteratorWrapper(Iterator wrappedIterator, NodeObject scope, RenderableResolvable mother) {
			this.wrappedIterator = wrappedIterator;
			this.scope = scope;
			this.mother = mother;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException("Iterator is not modifiable");
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return wrappedIterator.hasNext();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public Object next() {
			return wrapObject(wrappedIterator.next(), scope, mother);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof RenderableResolvable) {
			return wrappedObject.equals(((RenderableResolvable) obj).wrappedObject);
		} else {
			return wrappedObject.equals(obj);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return wrappedObject.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		// unwrap the object
		if (o instanceof RenderableResolvable) {
			return this.toString().compareTo(((RenderableResolvable) o).toString());
		}

		if (wrappedObject instanceof Comparable) {
			return ((Comparable) wrappedObject).compareTo(o);
		} else {
			return MiscUtils.compareObjects(wrappedObject, o);
		}
	}

	/**
	 * Get the wrapped object
	 * @return wrapped object
	 */
	public Object getWrappedObject() {
		return wrappedObject;
	}

	/**
	 * Get the stack of objects to be put upon the render stack while getting properties
	 * @return stack of objects
	 */
	public List<StackResolvable> getStack() {
		return stack;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render()
	 */
	public String render() throws NodeException {
		return render(TransactionManager.getCurrentTransaction().getRenderResult());
	}

	/* (non-Javadoc)
	 * @see com.gentics.lib.render.Renderable#render(com.gentics.lib.render.RenderResult)
	 */
	public String render(RenderResult renderResult) throws NodeException {
		if (wrappedRenderable != null) {
			return wrappedRenderable.render(renderResult);
		} else {
			throw new NodeException("Could not render, because wrapped object is no Renderable");
		}
	}

	/**
	 * Get the AutoCloseable scope of this renderable resolvable
	 * @return Scope as AutoCloseable
	 * @throws NodeException
	 */
	public Scope scope() throws NodeException {
		return new Scope();
	}

	/**
	 * AutoCloseable implementation that pushes the scope onto the render stack in the constructor
	 * and removes them (in reverse order) in {@link #close()}
	 */
	public class Scope implements AutoCloseable {
		/**
		 * List of pushed items
		 */
		protected List<StackResolvable> pushedItems = new ArrayList<>();

		/**
		 * RenderType
		 */
		protected RenderType renderType;

		/**
		 * Create an instance which will push the scope onto the render stack
		 * @throws NodeException
		 */
		public Scope() throws NodeException {
			if (logger.isDebugEnabled()) {
				if (scope != null) {
					logger.debug("Rendering {" + wrappedRenderable + "} in scope {" + scope + "}");
				} else {
					logger.debug("Rendering {" + wrappedRenderable + "} with default scope");
				}
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			renderType = t.getRenderType();

			for (StackResolvable item : stack) {
				if (!item.equals(wrappedRenderable)) {
					// store which item was pushed
					pushedItems.add(item);
					renderType.push(item);
				}
			}
		}

		@Override
		public void close() throws NodeException {
			// pop the items (in reverse order)
			Collections.reverse(pushedItems);
			for (StackResolvable pushedItem : pushedItems) {
				renderType.pop(pushedItem);
			}
		}
	}
}
