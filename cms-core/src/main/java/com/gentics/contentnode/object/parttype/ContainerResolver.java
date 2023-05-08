/*
 * @author norbert
 * @date 19.12.2008
 * @version $Id: ContainerResolver.java,v 1.2 2009-12-16 16:12:12 herbert Exp $
 */
package com.gentics.contentnode.object.parttype;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.contentnode.resolving.StackResolvable;

public class ContainerResolver implements StackResolvable, Resolvable {
	protected StackResolvable wrappedResolvable;

	public final static String[] KEYWORDS = new String[] { "container"};

	public ContainerResolver(StackResolvable wrappedResolvable) {
		this.wrappedResolvable = wrappedResolvable;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getStackKeywords()
	 */
	public String[] getStackKeywords() {
		return KEYWORDS;
	}

	/*
	 * (non-Javadoc)
	 * @see com.gentics.lib.base.StackResolvable#getKeywordResolvable(java.lang.String)
	 */
	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		if ("container".equals(keyword) && wrappedResolvable instanceof Resolvable) {
			return (Resolvable) wrappedResolvable;
		} else {
			return wrappedResolvable.getKeywordResolvable(keyword);
		}
	}

	public Resolvable getShortcutResolvable() throws NodeException {
		return null;
		// return wrappedResolvable.getShortcutResolvable();
	}

	public String getStackHashKey() {
		return "container/" + wrappedResolvable.getStackHashKey();
	}

	public Object getProperty(String key) {
		return get(key);
	}

	public Object get(String key) {
		if (wrappedResolvable instanceof Resolvable) {
			Object ret = null;
			Resolvable res = (Resolvable) wrappedResolvable;

			return res.get(key);
		}
		return null;
	}

	public boolean canResolve() {
		return true;
	}
}
