/*
 * @author norbert
 * @date 25.04.2006
 * @version $Id: JAXBHelper.java,v 1.4 2009-12-16 16:12:08 herbert Exp $
 */
package com.gentics.lib.jaxb;

import java.io.Writer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.pool.KeyedObjectPool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.gentics.lib.etc.StringUtils;
import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.pooling.PoolFactory;
import com.gentics.lib.pooling.PortalPool;
import com.gentics.lib.pooling.PortalPoolException;
import com.gentics.lib.pooling.PortalPoolProvider;

/**
 * Static helper class to perform JAXB related operations (unmarshalling,
 * marshalling, ...)
 */
public final class JAXBHelper {

	/**
	 * Private constructor since this shall be a static class
	 */
	private JAXBHelper() {}

	/**
	 * whether the JAXBHelper is initialized or not
	 */
	private static boolean initialized = false;

	/**
	 * unmarshaller pool
	 * TODO this pool shall be managed by the PortalPoolProvider
	 */
	private static KeyedObjectPool unmarshallerPool;

	/**
	 * marshaller pool
	 * TODO this pool shall be managed by the PortalPoolProvider
	 */
	private static KeyedObjectPool marshallerPool;

	/**
	 * logger
	 */
	private final static NodeLogger LOGGER = NodeLogger.getNodeLogger(JAXBHelper.class);

	/**
	 * Initialize the JAXBHelper
	 * @param configurationFile name of the configuration file for the pool
	 *        configuration
	 * @throws JAXBException
	 */
	public final static synchronized void init(String configurationFile) throws JAXBException {
		if (!initialized) {
			initialized = true;
			// create the pool of unmarshallers
			unmarshallerPool = PoolFactory.getKeyedObjectPoolFactory(StringUtils.resolveSystemProperties(configurationFile)).createPool();
			unmarshallerPool.setFactory(new JAXBUnmarshallerFactory());

			// create the pool of marshallers
			marshallerPool = PoolFactory.getKeyedObjectPoolFactory(StringUtils.resolveSystemProperties(configurationFile)).createPool();
			marshallerPool.setFactory(new JAXBMarshallerFactory());
		}
	}

	/**
	 * Unmarshall the given Node for the given context path
	 * @param contextPath context path (packages for which to unmarshall)
	 * @param node Node holding the xml data to unmarshall
	 * @return unmarshalled object
	 * @throws JAXBException
	 */
	public final static Object unmarshall(String contextPath, Node node) throws JAXBException {
		assertIsInitialized();
		Unmarshaller unmarshaller = null;

		try {
			// get the unmarshaller from the pool
			unmarshaller = (Unmarshaller) unmarshallerPool.borrowObject(contextPath);
			return unmarshaller.unmarshal(node);
		} catch (JAXBException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JAXBException("Error while unmarshalling", ex);
		} finally {
			if (unmarshaller != null) {
				// return the unmarshaller to the pool
				try {
					unmarshallerPool.returnObject(contextPath, unmarshaller);
				} catch (Exception ex) {
					LOGGER.error("Error while returning unmarshaller to pool", ex);
				}
			}
		}
	}

	/**
	 * Unmarshall the given intputsource for the given context path
	 * @param contextPath context path (packages for which to unmarshall)
	 * @param inputSource inputsource holding the xml data to unmarshall
	 * @return unmarshalled object
	 * @throws JAXBException
	 */
	public final static Object unmarshall(String contextPath, InputSource inputSource) throws JAXBException {
		assertIsInitialized();
		Unmarshaller unmarshaller = null;

		try {
			// get the unmarshaller from the pool
			unmarshaller = (Unmarshaller) unmarshallerPool.borrowObject(contextPath);
			return unmarshaller.unmarshal(inputSource);
		} catch (JAXBException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JAXBException("Error while unmarshalling", ex);
		} finally {
			if (unmarshaller != null) {
				// return the unmarshaller to the pool
				try {
					unmarshallerPool.returnObject(contextPath, unmarshaller);
				} catch (Exception ex) {
					LOGGER.error("Error while returning unmarshaller to pool", ex);
				}
			}
		}
	}

	/**
	 * Marshall the given object into the writer
	 * @param contextPath context path
	 * @param object object to marshall
	 * @param writer writer where to marshall to
	 * @throws JAXBException
	 */
	public final static void marshall(String contextPath, Object object, Writer writer) throws JAXBException {
		assertIsInitialized();
		Marshaller marshaller = null;

		try {
			// get the unmarshaller from the pool
			marshaller = (Marshaller) marshallerPool.borrowObject(contextPath);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(object, writer);
		} catch (JAXBException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JAXBException("Error while marshalling", ex);
		} finally {
			if (marshaller != null) {
				// return the marshaller to the pool
				try {
					marshallerPool.returnObject(contextPath, marshaller);
				} catch (Exception ex) {
					LOGGER.error("Error while returning marshaller to pool", ex);
				}
			}
		}
	}

	/**
	 * Marshall the given object into a Node
	 * @param contextPath context path
	 * @param object object to marshall
	 * @param node Node to marshall to
	 * @throws JAXBException
	 */
	public final static void marshall(String contextPath, Object object, Node node) throws JAXBException {
		assertIsInitialized();
		Marshaller marshaller = null;

		try {
			// get the unmarshaller from the pool
			marshaller = (Marshaller) marshallerPool.borrowObject(contextPath);
			marshaller.marshal(object, node);
		} catch (JAXBException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new JAXBException("Error while marshalling", ex);
		} finally {
			if (marshaller != null) {
				// return the marshaller to the pool
				try {
					marshallerPool.returnObject(contextPath, marshaller);
				} catch (Exception ex) {
					LOGGER.error("Error while returning marshaller to pool", ex);
				}
			}
		}
	}

	/**
	 * Throw an exception when the helper is not yet initialized
	 * @throws JAXBException
	 */
	protected final static void assertIsInitialized() throws JAXBException {
		if (!initialized) {
			throw new JAXBException("JAXBHelper is not yet initialized");
		}
	}

	/**
	 * Create a (real) copy of the given node, such that it can safely be used
	 * in another thread to unmarshal it. Access to the given node object is synchronized
	 * @param node node to copy
	 * @return copy of the node
	 * @throws Exception
	 */
	public final static Node copyNode(Node node) throws Exception {
		// DocumentBuilder docBuilder = null;
		// PortalPool docBuilderPool = null;
		// try {
		// docBuilderPool = PortalPoolProvider
		// .getPortalPool(PortalPoolProvider.DOCUMENT_BUILDER_POOL);
		// // fetch the document builder
		// docBuilder = (DocumentBuilder) docBuilderPool.borrowObject();
		//
		// // synchronize access to the original node and return a real copy of
		// // the
		// // node to avoid synchronization problems
		// Document doc = docBuilder.newDocument();
		// synchronized (node) {
		// return doc.importNode(node, true);
		// }
		// } finally {
		// if (docBuilderPool != null && docBuilder != null) {
		// try {
		// docBuilderPool.returnObject(docBuilder);
		// } catch (PortalPoolException e) {
		// }
		// }
		// }
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		synchronized (node) {
			return doc.importNode(node, true);
		}
	}
}
