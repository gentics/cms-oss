/*
 * @author Stefan Hepp
 * @date 02.02.2006
 * @version $Id: ObjectTagResolvable.java,v 1.3 2007-11-13 10:03:41 norbert Exp $
 */
package com.gentics.contentnode.object;

import java.util.Collections;
import java.util.Set;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.resolving.Resolvable;
import com.gentics.lib.resolving.ResolvableMapWrappable;

/**
 * This is a helper to make a list of objecttags, contained by a {@link ObjectTagContainer} resolvable.
 * The names of the objecttags of the container are resolved to the objecttags.
 */
public class ObjectTagResolvable implements Resolvable, ResolvableMapWrappable {

	private ObjectTagContainer container;
	private boolean fallback;

	/**
	 * create a new resolvable wrapper for a given objecttag container.
	 * @param container the container of the objecttags.
	 */
	public ObjectTagResolvable(ObjectTagContainer container) {
		this.container = container;
		this.fallback = true;
	}

	@Override
	public Set<String> getResolvableKeys() {
		try {
			return this.container.getObjectTagNames(fallback);
		} catch (NodeException e) {
			return Collections.emptySet();
		}
	}

	/**
	 * create a new resolvable wrapper for a given objecttag container.
	 * @param container the container of the objecttags.
	 */
	public ObjectTagResolvable(ObjectTagContainer container, boolean fallback) {
		this.container = container;
		this.fallback = fallback;
	}
    
	public Object getProperty(String key) {
		return get(key);
	}

	public Object get(String key) {
		try {
			return container.getObjectTag(key, fallback);
		} catch (NodeException e) {
			// TODO log error
			return null;
		}
	}

	public boolean canResolve() {
		return container != null;
	}
}
