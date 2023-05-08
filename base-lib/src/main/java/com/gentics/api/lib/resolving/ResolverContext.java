/*
 * @author norbert
 * @date 03.12.2007
 * @version $Id: ResolverContext.java,v 1.1 2007-12-03 13:08:05 norbert Exp $
 */
package com.gentics.api.lib.resolving;

/**
 * Interface for a resolver context. The resolver context is notified when a
 * property of a resolved object is modified via the {@link PropertySetter}.
 * This might be necessary to change runtime properties during the call to
 * {@link Changeable#setProperty(String, Object)}.
 */
public interface ResolverContext {

	/**
	 * Push the given object onto the ResolverContext instance
	 * @param object object to push onto the ResolverContext
	 */
	public void push(Object object);

	/**
	 * Pop the last object from the ResolverContext
	 * @param object TODO
	 */
	public void pop(Object object);
}
