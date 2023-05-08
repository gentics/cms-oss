/*
 * Poolable.java
 *
 * Created on 27. August 2004, 00:14
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public interface Poolable {
	public void init(java.util.Collection c);

	public void reset(); // could also be called reinit - has to reset to the
	// basic settings

	public Object getObject();
}
