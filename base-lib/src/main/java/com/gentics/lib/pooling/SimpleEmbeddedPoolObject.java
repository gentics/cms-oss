/*
 * SimpleEmbeddedPoolObject.java
 *
 * Created on 30. August 2004, 21:30
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public class SimpleEmbeddedPoolObject implements Poolable {

	/**
	 * Creates a new instance of SimpleEmbeddedPoolObject this simply represents
	 * a wraper around a simple object
	 */
	private Object SaveObject = null;

	public SimpleEmbeddedPoolObject(Object O) {
		SaveObject = O;
	}

	public void init(java.util.Collection c) {// do nothing to init
	}

	public void reset() {// there is no reset method for this pool object - it won't do anything
		// like this
	}

	public void destroy() {}

	public Object getObject() {
		return SaveObject;
	}

	public void setObject(Object O) {
		SaveObject = O;
	}
}
