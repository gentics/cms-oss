package com.gentics.lib.jaxb;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * ObjectFactory for creation of marshaller objects. The keys are the contextPaths.
 */
public class JAXBMarshallerFactory implements KeyedPoolableObjectFactory<String, Marshaller> {

	@Override
	public Marshaller makeObject(String key) throws Exception {
		if (key == null) {
			return null;
		}
		JAXBContext context = JAXBContextFactory.getContext(key);
		Marshaller marshaller = context.createMarshaller();

		return marshaller;
	}

	@Override
	public void destroyObject(String key, Marshaller obj) throws Exception {// unmarshallers do not need to be destroyed
	}

	@Override
	public boolean validateObject(String key, Marshaller obj) {
		return true;
	}

	@Override
	public void activateObject(String key, Marshaller obj) throws Exception {}

	@Override
	public void passivateObject(String key, Marshaller obj) throws Exception {}
}
