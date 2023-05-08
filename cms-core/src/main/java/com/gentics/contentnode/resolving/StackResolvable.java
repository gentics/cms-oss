/*
 * @author Stefan Hepp
 * @date 13.1.2006
 * @version $Id: StackResolvable.java,v 1.6 2008-11-10 10:54:29 norbert Exp $
 */
package com.gentics.contentnode.resolving;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * An interface for objects to push them into the stackresolver.
 * {@link StackResolver}
 */
public interface StackResolvable {

	/**
	 * get all keywords for which this resolver should be used.
	 * @return list of all keywords.
	 */
	String[] getStackKeywords();

	/**
	 * get a resolvable for a base-keyword
	 * @param keyword one of the keywords from {@link #getStackKeywords()}
	 * @return the resolvable for this keyword.
	 * @throws NodeException TODO
	 */
	Resolvable getKeywordResolvable(String keyword) throws NodeException;

	/**
	 * get the default resolver for tag-names without known prefix.
	 * @return a default resolver used for unknown keywords
	 * @throws NodeException TODO
	 */
	Resolvable getShortcutResolvable() throws NodeException;

	/**
	 * get a unique identifier of this object, used to find the object in the stack.
	 * @return a unique identifier for this object in the render-stack.
	 */
	String getStackHashKey();

	/**
	 * This is very ugly: we instantiate an object as dummy implementation of a
	 * StackResolvable, that will break the recursion in the stack resolving
	 */
	public final static StackResolvable STOP_OBJECT = new StackResolvable() {

		public Resolvable getKeywordResolvable(String keyword) throws NodeException {
			return null;
		}

		public Resolvable getShortcutResolvable() throws NodeException {
			return null;
		}

		public String getStackHashKey() {
			return null;
		}

		public String[] getStackKeywords() {
			return new String[0];
		}
	};
}
