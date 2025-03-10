package com.gentics.lib.jaxb;

import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * Utility class JAXBContextFactory for creation (caching) of JAXBContexts.
 */
public final class JAXBContextFactory {

	/**
	 * internal storage for JAXB Contexts.
	 */
	private final static Map<String, JAXBContext> JAXBCONTEXTMAP = new HashMap<>();

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
		JAXBContext context = JAXBCONTEXTMAP.get(contextPath);

		if (context == null) {
			context = JAXBContext.newInstance(contextPath);
			JAXBCONTEXTMAP.put(contextPath, context);
		}

		return context;
	}
}
