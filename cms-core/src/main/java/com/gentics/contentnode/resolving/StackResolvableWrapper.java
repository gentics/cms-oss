package com.gentics.contentnode.resolving;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;

/**
 * User: Stefan Hepp
 * Date: 31.12.2005
 * Time: 16:31:41
 */
public class StackResolvableWrapper implements StackResolvable {
	private String[] keys;
	private Resolvable resolvable;

	public StackResolvableWrapper(String[] keys, Resolvable resolvable) {
		this.keys = (String[]) keys.clone();
		this.resolvable = resolvable;
	}

	public Resolvable getResolvable() {
		return resolvable;
	}

	public StackResolvableWrapper(String key, Resolvable resolvable) {
		this.keys = new String[] { key};
		this.resolvable = resolvable;
	}

	public String[] getStackKeywords() {
		return keys;
	}

	public Resolvable getKeywordResolvable(String keyword) throws NodeException {
		return resolvable;
	}

	public Resolvable getShortcutResolvable() throws NodeException {
		return null;
	}

	public String getStackHashKey() {
		return "resolvable:" + resolvable.hashCode();
	}
}
