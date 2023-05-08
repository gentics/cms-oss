/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: JAXBMarshallerFactory.java,v 1.1 2006-04-27 10:13:21 norbert Exp $
 */
package com.gentics.lib.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * ObjectFactory for creation of marshaller objects. The keys are the contextPaths.
 */
public class JAXBMarshallerFactory implements KeyedPoolableObjectFactory {

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#makeObject(java.lang.Object)
	 */
	public Object makeObject(Object key) throws Exception {
		if (key == null) {
			return null;
		}
		JAXBContext context = JAXBContextFactory.getContext(key.toString());
		Marshaller marshaller = context.createMarshaller();

		return marshaller;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#destroyObject(java.lang.Object, java.lang.Object)
	 */
	public void destroyObject(Object key, Object obj) throws Exception {// unmarshallers do not need to be destroyed
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#validateObject(java.lang.Object, java.lang.Object)
	 */
	public boolean validateObject(Object key, Object obj) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#activateObject(java.lang.Object, java.lang.Object)
	 */
	public void activateObject(Object key, Object obj) throws Exception {}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#passivateObject(java.lang.Object, java.lang.Object)
	 */
	public void passivateObject(Object key, Object obj) throws Exception {}
}
