package com.gentics.contentnode.resolving;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.PropertyResolver;
import com.gentics.api.lib.resolving.PropertyResolver.PropertyPathEntry;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.TagContainer;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.render.RenderType;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.log.RuntimeProfiler;
import com.gentics.lib.log.profilerconstants.JavaParserConstants;

/*
 * @author Stefan Hepp
 * @date ${date}
 * @version $Id: StackResolver.java,v 1.22 2010-09-28 17:01:33 norbert Exp $
 */

public class StackResolver implements Resolvable, Cloneable {

	/**
	 * map of base objects, maps keys to stacks of objects
	 */
	private Map baseObjects;
	private Stack objectStack;

	private NodeLogger logger = NodeLogger.getNodeLogger(getClass());

	public StackResolver() {
		objectStack = new Stack();
		baseObjects = new HashMap();
	}

	public void push(StackResolvable resolvable) {
		objectStack.push(resolvable);
		hashBase(resolvable);
	}

	public StackResolvable peek() {
		return (StackResolvable) objectStack.peek();
	}

	public StackResolvable pop() {
		StackResolvable obj = (StackResolvable) objectStack.pop();

		removeBase(obj);
		return obj;
	}

	/**
	 * removes the given from the top of the resolver stack and removes the keys
	 * under which this object was stored.
	 * @param resolvable object to remove
	 * @return true if the object was found and removed
	 */
	public boolean remove(StackResolvable resolvable) {
		boolean found = false;

		synchronized (objectStack) {
			int i = objectStack.size() - 1;

			for (; i >= 0 && !found; i--) {
				if (resolvable.equals(objectStack.get(i))) {
					found = true;
					objectStack.remove(i);
				}
			}
		}
		if (found) {
			removeBase(resolvable);
		}
		return found;
	}

	public boolean empty() {
		return objectStack.empty();
	}

	/**
	 * get the size of the baseObject stack.
	 *
	 * @return the size of the stack.
	 */
	public int size() {
		return objectStack.size();
	}

	// public Resolvable pushResolvable(String key, Resolvable resolvable) throws NodeException {
	// StackResolvable old = putKey(key, new StackResolvableWrapper(key, resolvable));
	// return old != null ? old.getKeywordResolvable(key) : null;
	// }

	// public Resolvable popResolvable(String key) throws NodeException {
	// StackResolvable old = removeKey(key);
	// return old != null ? old.getKeywordResolvable(key) : null;
	// }

	public Object getProperty(String key) {
		return get(key);
	}

	public Object get(String key) {
		try {
			return resolve(key);
		} catch (NodeException e) {
			logger.error("Error while resolving property {" + key + "}", e);
			return null;
		}
	}

	public boolean canResolve() {
		return true;
	}

	/**
	 * Resolves the given property (tagname) using the base object keys and the
	 * stack. When the first part of the tagname is one of the given keys, the
	 * stored resolvable is used, otherwise the stack of resolvables is used from
	 * top to bottom to get the property.
	 * @param property
	 * @return the object resolved for this property, or null if not found.
	 */
	public Object resolve(String property) throws NodeException {
		return resolve(property, false);
	}
    
	/**
	 * @see #resolve(String)
	 * @param property
	 * @param returnFullPath if true, method will return the whole resolved path, not just the last element
	 * @return
	 */
	public Object resolve(String property, boolean returnFullPath) throws NodeException {
		Object rs = null;

		RuntimeProfiler.beginMark(JavaParserConstants.STACKRESOLVE);
        
		try {
        
			RenderType renderType = TransactionManager.getCurrentTransaction().getRenderType();
			boolean handleDependencies = renderType.doHandleDependencies();

			int pos = property.indexOf('.');
			String propname = pos > -1 ? property.substring(0, pos) : property;
			StackResolvable resolvable = null;

			// -- resolve using the shortcut resolvables here --
			// not found yet, try stack using the full path ..
			int i = objectStack.size() - 1;

			while (rs == null && i >= 0) {
				resolvable = (StackResolvable) objectStack.get(i);

				// don't go beyond the stop object
				if (resolvable == StackResolvable.STOP_OBJECT) {
					break;
				}

				try {
					Resolvable baseObject = resolvable.getShortcutResolvable();

					if (baseObject == null) {
						// no base object -> no resolving
						i--;
						continue;
					}
					PropertyResolver resolver = new PropertyResolver(baseObject);

					List entries = resolver.resolvePath(property);

					if (!entries.isEmpty()) {
						PropertyPathEntry lastEntry = (PropertyPathEntry) entries.get(entries.size() - 1);

						if (lastEntry.isLastEntry()) {
							rs = lastEntry.getEntry();
							if (rs != null && returnFullPath) {
								return entries;
							}
						}
					}
				} catch (Exception e) {
					logger.error("Error while resolving property {" + property + "}", e);
					rs = null;
				}
				i--;
			}

			// -- resolve from the base objects here --

			if (rs == null) {
				resolvable = getBaseObject(propname);

				// we found the name in the baseObject list by key, this has first priority
				if (resolvable != null) {
					try {
						Resolvable baseObject = resolvable.getKeywordResolvable(propname);
						PropertyResolver resolver = new PropertyResolver(baseObject);

						List entries = resolver.resolvePath(property.substring(pos + 1));

						if (!entries.isEmpty()) {
							PropertyPathEntry lastEntry = (PropertyPathEntry) entries.get(entries.size() - 1);

							if (lastEntry.isLastEntry()) {
								rs = lastEntry.getEntry();
								if (rs != null && returnFullPath) {
									return entries;
								}
							}
						}
					} catch (Exception e) {
						// TODO log errormessage?, break search?
						logger.error("Error while resolving property {" + property + "}", e);
						rs = null;
					}
				}
			}

			// try to resolve the property from the base object (only a
			// single property requested)
			if (rs == null && pos < 0) {
				resolvable = getBaseObject(propname);
				if (resolvable instanceof Resolvable) {
					rs = ((Resolvable) resolvable).get(propname);
				}
			}

			// this is a hack to be able to resolve tags named like "table1.A1"
			// which is necessary to migrate table tags with non-inline editable cell tags to aloha editor.
			if (rs == null && property.contains(".")) {
				i = objectStack.size() - 1;

				while (rs == null && i >= 0) {
					resolvable = (StackResolvable) objectStack.get(i);

					// don't go beyond the stop object
					if (resolvable == StackResolvable.STOP_OBJECT) {
						break;
					}

					Resolvable baseObject = resolvable.getShortcutResolvable();

					if (baseObject != null) {
						rs = baseObject.get(property);
					}

					i--;
				}
			}

			return rs;
        
		} finally {
			RuntimeProfiler.endMark(JavaParserConstants.STACKRESOLVE);
		}
        
	}

	public StackResolver getCopy() {
		StackResolver stack = new StackResolver();

		// TODO do a copy of the stacks here
		stack.baseObjects = new HashMap(baseObjects);
		for (int i = 0; i < objectStack.size(); i++) {
			stack.objectStack.add(objectStack.get(i));
		}

		return stack;
	}

	protected Object clone() throws CloneNotSupportedException {
		return getCopy();
	}

	private void hashBase(StackResolvable resolvable) {
		String[] keywords = resolvable.getStackKeywords();

		for (int i = 0; i < keywords.length; i++) {
			String keyword = keywords[i];

			putKey(keyword, resolvable);
		}

	}

	private void removeBase(StackResolvable resolvable) {

		String[] keywords = resolvable.getStackKeywords();

		for (int i = 0; i < keywords.length; i++) {
			String keyword = keywords[i];

			removeKey(keyword);
		}
        
	}

	/**
	 * Get the baseobject stack for the given keyword. If the Stack does not yet exist, create a new onw
	 * @param keyword keyword
	 * @return stack of baseobjects
	 */
	private Stack getBaseObjectStack(String keyword) {
		Stack baseObjectStack = (Stack) baseObjects.get(keyword);

		if (baseObjectStack == null) {
			baseObjectStack = new Stack();
			baseObjects.put(keyword, baseObjectStack);
		}
		return baseObjectStack;
	}

	/**
	 * Get base object for the given keyword
	 * @param keyword keyword
	 * @return base object or null if non exists
	 */
	private StackResolvable getBaseObject(String keyword) {
		Stack baseObjectStack = (Stack) baseObjects.get(keyword);

		if (baseObjectStack != null && !baseObjectStack.isEmpty()) {
			return (StackResolvable) baseObjectStack.peek();
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @param keyword
	 * @param resolvable
	 */
	private void putKey(String keyword, StackResolvable resolvable) {
		Stack baseObjectStack = getBaseObjectStack(keyword);

		baseObjectStack.push(resolvable);
	}

	private void removeKey(String keyword) {
		Stack baseObjectStack = getBaseObjectStack(keyword);

		if (!baseObjectStack.isEmpty()) {
			baseObjectStack.pop();
		}
	}

	/**
	 * Get the lowest object in the stack (without removing it)
	 * @return lowest object in the stack
	 */
	public StackResolvable getRootObject() throws NodeException {
		if (objectStack.isEmpty()) {
			throw new NodeException("Error while getting root object from stack: stack is empty");
		} else {
			return (StackResolvable) objectStack.get(0);
		}
	}

	/**
	 * Get the topmost tag container
	 * @return tag container
	 * @throws NodeException when no tag container was found
	 */
	public TagContainer getTopmostTagContainer() throws NodeException {
		int size = objectStack.size();

		for (int i = size - 1; i >= 0; i--) {
			Object object = objectStack.get(i);

			if (object instanceof TagContainer) {
				return (TagContainer) object;
			}
		}

		throw new NodeException("Could not find a TagContainer in the object stack");
	}

	/**
	 * Get the topmost Tag from the object stack
	 * @return topmost Tag or null if non found
	 * @throws NodeException
	 */
	public Tag getTopmostTag() throws NodeException {
		int size = objectStack.size();

		for (int i = size - 1; i >= 0; i--) {
			Object object = objectStack.get(i);

			if (object instanceof Tag) {
				return (Tag) object;
			}
		}

		return null;
	}

	/**
	 * Get the topmost object of (or extending) the given clazz
	 * @param <T> type
	 * @param clazz requested class
	 * @return topmost object or null, if not found
	 * @throws NodeException
	 */
	@SuppressWarnings("unchecked")
	public <T> T getTopmost(Class<T> clazz) throws NodeException {
		int size = objectStack.size();

		for (int i = size - 1; i >= 0; i--) {
			Object object = objectStack.get(i);

			if (object != null && clazz.isAssignableFrom(object.getClass())) {
				return (T) object;
			}
		}

		return null;
	}

	/**
	 * Count the number of instances on the stack that implement or extend the given class
	 * @param clazz class
	 * @param ignore optional objects to ignore
	 * @return number of instances
	 */
	public int countInstances(Class<?> clazz, Object...ignore) {
		int count = 0;
		int size = objectStack.size();
		Set<Object> ignoredSet = new HashSet<Object>();
		for (Object i : ignore) {
			ignoredSet.add(i);
		}

		for (int i = size - 1; i >= 0; i--) {
			Object object = objectStack.get(i);

			if (ignoredSet.contains(object)) {
				continue;
			}

			if (clazz.isAssignableFrom(object.getClass())) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Get an unmodifiable map holding the base objects
	 * @return unmodifiable map of the base objects
	 */
	public Map getBaseObjects() {
		return Collections.unmodifiableMap(baseObjects);
	}

	/**
	 * Get the current render stack in human readable form (for logging what is currently rendered).
	 * @return human readable render stack
	 */
	public String getReadableStack() {
		StringBuffer stringBuffer = new StringBuffer();
		boolean firstObject = true;

		for (Iterator iterator = objectStack.iterator(); iterator.hasNext();) {
			Object stackElement = (Object) iterator.next();

			if (!(stackElement instanceof NodeObject)) {
				continue;
			}

			if (firstObject) {
				firstObject = false;
			} else {
				stringBuffer.append("/");
			}

			stringBuffer.append(stackElement);
		}

		return stringBuffer.toString();
	}

	/**
	 * This is almost the same as {@link #getReadableStack()}, except that it is suitable for
	 * the enduser of the CMS (displayable in the error stream)
	 * @return
	 */
	public String getUIReadableStack() {
		StringBuffer stringBuffer = new StringBuffer();
		boolean firstObject = true;

		for (Iterator iterator = objectStack.iterator(); iterator.hasNext();) {
			Object stackElement = (Object) iterator.next();

			if (!(stackElement instanceof NodeObject)) {
				continue;
			}
            
			if (!(stackElement instanceof Page) && !(stackElement instanceof Template) && !(stackElement instanceof Tag)) {
				continue;
			}

			if (firstObject) {
				firstObject = false;
			} else {
				stringBuffer.append("/");
			}

			stringBuffer.append(stackElement);
		}

		return stringBuffer.toString();
	}

	/**
	 * Get the object stack
	 * @return object stack
	 */
	public Stack getObjectStack() {
		return objectStack;
	}
}
