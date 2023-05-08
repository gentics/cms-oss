/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: JAXBContextFactory.java,v 1.1 2006-04-27 10:13:21 norbert Exp $
 */
package com.gentics.lib.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Utility class JAXBContextFactory for creation (caching) of JAXBContexts.
 */
public final class JAXBContextFactory {

	/**
	 * internal storage for JAXB Contexts.
	 */
	private final static Map JAXBCONTEXTMAP = new HashMap();

	/**
	 * Hide the constructor for this utility class
	 */
	private JAXBContextFactory() {}

	/**
	 * Get the jaxb context for the given context path
	 * @param contextPath context path
	 * @return context instance
	 * @throws JAXBException
	 */
	protected final static synchronized JAXBContext getContext(String contextPath) throws JAXBException {
		JAXBContext context = (JAXBContext) JAXBCONTEXTMAP.get(contextPath);

		if (context == null) {
			context = JAXBContext.newInstance(contextPath);
			JAXBCONTEXTMAP.put(contextPath, context);
		}

		return context;
	}
}
