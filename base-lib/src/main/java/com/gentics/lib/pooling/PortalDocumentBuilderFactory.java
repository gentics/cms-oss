/*
 * @author norbert
 * @date 26.04.2006
 * @version $Id: PortalDocumentBuilderFactory.java,v 1.1 2006-04-27 10:12:45 norbert Exp $
 */
package com.gentics.lib.pooling;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * PoolableObjectFactory to generate DocumentBuilders.
 */
public class PortalDocumentBuilderFactory implements PoolableObjectFactory {

	/**
	 * instance of the DocumentBuilderFactory used to generate document builders
	 */
	private final static DocumentBuilderFactory DOCUMENTBUILDERFACTORY = DocumentBuilderFactory.newInstance();

	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
	 */
	public Object makeObject() throws Exception {
		// generate a new instance of the document builder
		return DOCUMENTBUILDERFACTORY.newDocumentBuilder();
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
	 */
	public void destroyObject(Object obj) throws Exception {}

	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
	 */
	public boolean validateObject(Object obj) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
	 */
	public void activateObject(Object obj) throws Exception {}

	/*
	 * (non-Javadoc)
	 * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
	 */
	public void passivateObject(Object obj) throws Exception {}
}
