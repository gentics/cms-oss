/*
 * EmbeddingObjectPoolFactory.java
 *
 * Created on 30. August 2004, 21:26
 */

package com.gentics.lib.pooling;

/**
 * @author Dietmar
 */
public class SimpleEmbeddedPoolObjectFactory implements PoolFactoryInterface {

	protected Class InstantiationClass = null;

	protected boolean recreate;

	/** Creates a new instance of EmbeddingObjectPoolFactory */
	public SimpleEmbeddedPoolObjectFactory(Class BaseClass) {
		this(BaseClass, true);
	}

	public SimpleEmbeddedPoolObjectFactory(Class BaseClass, boolean RecreateOnInit) {
		InstantiationClass = BaseClass;
		recreate = RecreateOnInit;
	}

	public Poolable createObject() throws PoolingException {
		// save class - check if it has a simple constructur
		try {
			return new SimpleEmbeddedPoolObject(InstantiationClass.newInstance());
		} catch (Exception ex) {
			throw new PoolingException("Could not instantiate class because of:" + ex.getMessage());
		}
	}

	public void destroyObject(Poolable object) {}

	public void reinitObject(Poolable object) {
		if (object instanceof SimpleEmbeddedPoolObject) {
			SimpleEmbeddedPoolObject embedded = (SimpleEmbeddedPoolObject) object;

			try {
				// recreate object
				embedded.setObject(InstantiationClass.newInstance());
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
	}

}
