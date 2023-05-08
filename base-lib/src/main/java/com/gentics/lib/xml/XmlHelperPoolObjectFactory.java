/*
 * XmlHelperPoolObjectFactory.java
 *
 * Created on 02. September 2004, 15:13
 */

package com.gentics.lib.xml;

import com.gentics.lib.pooling.Poolable;
import com.gentics.lib.pooling.PoolingException;
import com.gentics.lib.pooling.SimpleEmbeddedPoolObject;

/**
 * @author Dietmar
 */
public class XmlHelperPoolObjectFactory implements com.gentics.lib.pooling.PoolFactoryInterface {

	/** Creates a new instance of XmlHelperPoolObjectFactory */
	public XmlHelperPoolObjectFactory() {}

	public com.gentics.lib.pooling.Poolable createObject() throws PoolingException {
		return new SimpleEmbeddedPoolObject(new XmlHelper());
	}

	public void destroyObject(Poolable object) {}

	public void reinitObject(com.gentics.lib.pooling.Poolable object) {// do nothing
	}

}
