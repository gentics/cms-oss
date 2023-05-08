/*
 * StringBufferPoolObjectFactory.java
 *
 * Created on 30. August 2004, 22:36
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public class StringBufferPoolObjectFactory implements PoolFactoryInterface {
	protected int size = 0;

	/** Creates a new instance of StringBufferPoolObjectFactory */
	public StringBufferPoolObjectFactory(int Size) {
		this.size = Size;
	}

	public Poolable createObject() throws PoolingException {
		// clone
		// ReferenceObject.clone();
		return new SimpleEmbeddedPoolObject(new StringBuffer(size));
	}

	public void destroyObject(Poolable object) {}

	public void reinitObject(Poolable object) {
		if (object instanceof SimpleEmbeddedPoolObject) {
			// SimpleEmbeddedPoolObject embedded =
			// (SimpleEmbeddedPoolObject)object;
			StringBuffer buf = (StringBuffer) object.getObject();

			// delete buffer
			buf.delete(0, buf.length());
		} else {// do nothing yet
		}
	}

}
