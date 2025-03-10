package com.gentics.lib.jaxb;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * ObjectFactory for creation of unmarshaller objects. The keys are the contextPaths.
 */
public class JAXBUnmarshallerFactory implements KeyedPoolableObjectFactory<String, Unmarshaller> {

	@Override
	public Unmarshaller makeObject(String key) throws Exception {
		if (key == null) {
			return null;
		}
		JAXBContext context = JAXBContextFactory.getContext(key);
		Unmarshaller unmarshaller = context.createUnmarshaller();

		return unmarshaller;
	}

	@Override
	public void destroyObject(String key, Unmarshaller obj) throws Exception {// unmarshallers do not need to be destroyed
	}

	@Override
	public boolean validateObject(String key, Unmarshaller obj) {
		return true;
	}

	@Override
	public void activateObject(String key, Unmarshaller obj) throws Exception {// unmarshallers do not need to be activated
	}

	@Override
	public void passivateObject(String key, Unmarshaller obj) throws Exception {// unmarshallers do not need to be passivated
	}
}
