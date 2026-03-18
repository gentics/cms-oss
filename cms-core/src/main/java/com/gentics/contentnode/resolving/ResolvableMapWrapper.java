package com.gentics.contentnode.resolving;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Vector;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.ContentFile;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.PartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.render.Renderable;

/**
 * Wrapper for instances of {@link ResolvableMapWrappable}, that implements {@link Map}
 */
public class ResolvableMapWrapper extends AbstractMap<String, Object> implements Resolvable, Renderable {
	/**
	 * logger
	 */
	private static NodeLogger logger = NodeLogger.getNodeLogger(ResolvableMapWrapper.class);

	/**
	 * Entry Set
	 */
	protected final Set<Entry<String, Object>> entrySet;

	/**
	 * Wrapped instance
	 */
	protected final ResolvableMapWrappable wrapped;

	/**
	 * object that defines the contexxt in which this object is rendered
	 */
	private NodeObject context;

	/**
	 * List of objects to be put upon the render stack while getting a property from the object
	 */
	private List<StackResolvable> stack = new Vector<StackResolvable>();

	/**
	 * Wrap the value into instance(s) of {@link ResolvableMapWrapper}.
	 * This will handle single instances, instances in lists and maps
	 * @param value value to wrap
	 * @param context optional context
	 * @param mother optional mother
	 * @return optionally wrapped value
	 */
	protected static Object wrap(Object value, NodeObject context, ResolvableMapWrapper mother) {
		if (value instanceof ResolvableMapWrappable resolvableMapWrappable) {
			return new ResolvableMapWrapper(resolvableMapWrappable, context, mother);
		} else if (value instanceof List<?> listValue) {

			return new AbstractList<Object>() {
				@Override
				public int size() {
					return listValue.size();
				}

				@Override
				public Object get(int index) {
					return wrap(listValue.get(index), context, mother);
				}
			};
		} else if (value instanceof Map<?, ?> mapValue) {

			Map<Object, Object> wrappedMap = value instanceof SortedMap<?, ?> ? new LinkedHashMap<>(mapValue.size())
					: new HashMap<>();
			mapValue.forEach((k, v) -> {
				wrappedMap.put(k, wrap(v, context, mother));
			});

			return wrappedMap;
		} else {
			return value;
		}
	}

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped instance
	 */
	public ResolvableMapWrapper(ResolvableMapWrappable wrapped) {
		this(wrapped, null, null);
	}

	/**
	 * Create wrapper instance
	 * @param wrapped wrapped instance
	 * @param context optional context
	 * @param mother optional mother
	 */
	public ResolvableMapWrapper(ResolvableMapWrappable wrapped, NodeObject context, ResolvableMapWrapper mother) {
		this.wrapped = wrapped;
		this.context = context;
		// sort the keys
		List<String> keys = new ArrayList<>(wrapped.getResolvableKeys());
		Collections.sort(keys);
		// entry set is a linked hashset to retain insertion order
		this.entrySet = new LinkedHashSet<>();
		keys.forEach(key -> this.entrySet.add(new MapEntry(key)));

		// get the stack from the mother
		if (mother != null) {
			stack.addAll(mother.stack);
		}
		// add the wrapped object
		if (wrapped instanceof StackResolvable stackResolvable) {
			stack.add(stackResolvable);
		}
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return entrySet;
	}

	@Override
	public Object getProperty(String key) {
		return get(key);
	}

	@Override
	public Object get(String key) {
		return wrap(wrapped.get(key), getSubContext(), this);
	}

	@Override
	public boolean canResolve() {
		return wrapped.canResolve();
	}

	/**
	 * Get the wrapped object
	 * @return wrapped object
	 */
	public ResolvableMapWrappable getWrapped() {
		return wrapped;
	}

	@Override
	public String toString() {
		return String.format("Wrapper for %s", wrapped.toString());
	}

	@Override
	public String render() throws NodeException {
		try (RenderContext renderContext = new RenderContext()) {
			if (wrapped instanceof Renderable renderable) {
				return renderable.render();
			} else if (wrapped != null) {
				return wrapped.toString();
			} else {
				return null;
			}
		}
	}

	/**
	 * Get the AutoCloseable RenderContext of this renderable resolvable
	 * @return RenderContext as AutoCloseable
	 * @throws NodeException
	 */
	public RenderContext withContext() throws NodeException {
		return new RenderContext();
	}

	/**
	 * Get the context for resolved objects
	 * @return context for resolved objects
	 */
	protected NodeObject getSubContext() {
		// when resolving from a folder, file or page, we switch the context
		if (wrapped instanceof Folder folder) {
			return folder;
		} else if (wrapped instanceof ContentFile contentFile) {
			return contentFile;
		} else if (wrapped instanceof Page page) {
			return page;
		} else if (wrapped instanceof Tag tag) {
			return tag;
		} else if (wrapped instanceof Value value) {
			// when resolving from a value of a URLPartType, we switch the context
			// to the target object (if any set)
			try {
				PartType partType = value.getPartType();

				if (partType instanceof UrlPartType) {
					NodeObject target = ((UrlPartType) partType).getTarget();

					if (target != null) {
						return target;
					}
				}
			} catch (NodeException e) {}
		}
		return context;
	}

	/**
	 * Internal implementation of a {@link Map.Entry}
	 */
	protected class MapEntry implements Map.Entry<String, Object> {
		/**
		 * Key of the entry
		 */
		protected String key;

		/**
		 * Create instance for the key
		 * @param key key
		 */
		protected MapEntry(String key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return wrap(wrapped.get(key), getSubContext(), ResolvableMapWrapper.this);
		}

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * AutoCloseable implementation that pushes the context onto the render stack in the constructor
	 * and removes them (in reverse order) in {@link #close()}
	 */
	public class RenderContext implements AutoCloseable {
		/**
		 * List of pushed items
		 */
		protected List<StackResolvable> pushedItems = new ArrayList<>();

		/**
		 * RenderType
		 */
		protected RenderType renderType;

		/**
		 * Create an instance which will push the context onto the render stack
		 * @throws NodeException
		 */
		public RenderContext() throws NodeException {
			if (logger.isDebugEnabled()) {
				if (context != null) {
					logger.debug("Rendering {" + wrapped + "} in context {" + context + "}");
				} else {
					logger.debug("Rendering {" + wrapped + "} with default context");
				}
			}
			Transaction t = TransactionManager.getCurrentTransaction();
			renderType = t.getRenderType();

			for (StackResolvable item : stack) {
				if (!item.equals(wrapped)) {
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
