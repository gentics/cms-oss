package com.gentics.contentnode.resolving;

import java.util.Stack;

import com.gentics.api.lib.resolving.ResolverContext;
import com.gentics.contentnode.object.NodeObject;

/**
 * ResolverContext implementation for resolving of NodeObjects
 */
public class NodeObjectResolverContext implements ResolverContext {

	/**
	 * ThreadLocal stack of resolved objects
	 */
	protected static ThreadLocal<Stack<NodeObject>> OBJECT_STACK = new ThreadLocal<Stack<NodeObject>>();

	/**
	 * Get the threadlocal stack (or create a new one, if none found)
	 * @return threadlocal stack
	 */
	protected static Stack<NodeObject> getStack() {
		Stack<NodeObject> stack = OBJECT_STACK.get();

		if (stack == null) {
			stack = new Stack<NodeObject>();
			OBJECT_STACK.set(stack);
		}

		return stack;
	}

	/**
	 * Get the current nodeobject
	 * @return current nodeobject or null
	 */
	public static NodeObject getNodeObject() {
		Stack<NodeObject> stack = getStack();

		if (stack.isEmpty()) {
			return null;
		} else {
			return stack.peek();
		}
	}

	/**
	 * Get the topmost NodeObject, which is an implementation of the given class
	 * @return topmost NodeObject of the given class or null
	 */
	@SuppressWarnings("unchecked")
	public static <T extends NodeObject> T getNodeObject(Class<T> clazz) {
		if (clazz == null) {
			return null;
		}
		Stack<NodeObject> stack = getStack();
		T result = null;

		for (NodeObject nodeObject : stack) {
			if (clazz.isInstance(nodeObject)) {
				result = (T) nodeObject;
			}
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.ResolverContext#pop(java.lang.Object)
	 */
	public void pop(Object object) {
		if (object instanceof NodeObject) {
			Stack<NodeObject> stack = getStack();

			stack.pop();
		}
	}

	/* (non-Javadoc)
	 * @see com.gentics.api.lib.resolving.ResolverContext#push(java.lang.Object)
	 */
	public void push(Object object) {
		if (object instanceof NodeObject) {
			Stack<NodeObject> stack = getStack();

			stack.push((NodeObject) object);
		}
	}
}
