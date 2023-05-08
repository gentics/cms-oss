/*
 * @author herbert
 * @date May 19, 2008
 * @version $Id: CombinedResolvable.java,v 1.2 2008-05-26 15:05:55 norbert Exp $
 */
package com.gentics.lib.base;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Simple resolvable which wraps multiple resolvables into one.
 */
public class CombinedResolvable implements Resolvable {
    
	private Resolvable[] resolvables;

	public CombinedResolvable(Resolvable[] resolvables) {
		this.resolvables = resolvables;
	}

	public boolean canResolve() {
		return true;
	}

	public Object get(String key) {
		for (int i = 0; i < resolvables.length; i++) {
			Object res = resolvables[i].get(key);

			if (res != null) {
				return res;
			}
		}
		return null;
	}

	public Object getProperty(String key) {
		return this.get(key);
	}

}
