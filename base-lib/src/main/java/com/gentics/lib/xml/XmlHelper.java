package com.gentics.lib.xml;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gentics.lib.log.NodeLogger;
import com.gentics.lib.pooling.PoolFactoryInterface;
import com.gentics.lib.pooling.PoolInterface;
import com.gentics.lib.pooling.PoolWrapper;
import com.gentics.lib.pooling.Poolable;
import com.gentics.lib.pooling.PoolingException;
import com.gentics.lib.pooling.SimpleObjectPool;

/**
 * Created by IntelliJ IDEA. User: laurin Date: 14.07.2004 Time: 13:48:37 To
 * change this template use File | Settings | File Templates.
 */
public final class XmlHelper {

	/* !MOD 20040902 DG added pool for XML Helper */
	private static PoolInterface helperPool = null; // new SimpleObjectPool();

	// for initialising pool
	private static int minimumPoolObjects = 5;

	private static int maximumPoolObjects = 100;

	/* !MOD 20040902 DG added pool for XML Helper */

	private Document doc;

	private DocumentBuilder builder;

	private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

	private static java.util.HashMap XMLPooled = new java.util.HashMap();

	public XmlHelper() {
		try {
			this.builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace(); // To change body of catch statement use File |
			// Settings | File Templates.
		}
	}

	public void parseXML(String xml) throws SAXException {
		InputSource is2 = new InputSource(new StringReader(xml));

		this.doc = null;
		try {
			doc = builder.parse(is2);
			// } catch (SAXException e) {
			// throw e;
			// // e.printStackTrace(); //To change body of catch statement use
			// File | Settings | File Templates.
		} catch (IOException e) {
			e.printStackTrace(); // To change body of catch statement use File |
			// Settings | File Templates.
		}
	}

	public NodeList getElements(String tagName) {
		return doc.getElementsByTagName(tagName);
	}

	public NodeList getAllElements() {
		return doc.getChildNodes();
	}

	public String findAttribute(String childName, String attributeName) {
		return null;
	}

	/* !MOD 20040902 DG added static method for asking pool */
	public static XmlHelper getPooledObject() throws PoolingException,
				IllegalAccessException, com.gentics.lib.pooling.PoolEmptyException {
		if (helperPool == null) {
			// reset pool
			createPool();
		}
		Poolable PooledObject = helperPool.getInstance();
		// save PooledObject
		XmlHelper helper = (XmlHelper) PooledObject.getObject();

		XMLPooled.put(helper, PooledObject);
		return helper;
	}

	public static XmlHelper tryPooledObject() {
		try {
			if (helperPool == null) {
				// reset pool
				createPool();
			}
			Poolable PooledObject = helperPool.getInstance();
			// save PooledObject
			XmlHelper helper = (XmlHelper) PooledObject.getObject();

			XMLPooled.put(helper, PooledObject);
			return helper;
		} catch (Exception e) {
			NodeLogger.getLogger(XmlHelper.class).error("Could not fetch pool object", e);
			return new XmlHelper();
		}
	}

	public static void releasePooledObject(XmlHelper PooledObject) throws com.gentics.lib.pooling.NotPoolObjectException {
		// release
		Poolable pooled = (Poolable) XMLPooled.get(PooledObject);

		if (pooled != null) {
			helperPool.releaseInstance(pooled);
		}
	}

	private static PoolFactoryInterface factory = new XmlHelperPoolObjectFactory();

	public static void createPool() throws PoolingException, IllegalAccessException {

		/*
		 * TODO check what we shall do with pooled objects that have not been
		 * released
		 */
		// helperPool = new SimpleObjectPool(MinmimumPoolObjects, MaximumPoolObjects, factory);
		helperPool = new PoolWrapper("XmlHelperPool", minimumPoolObjects, maximumPoolObjects, factory);
	}

	/**
	 * remove all objects from the helper pool
	 */
	public final static void clearPool() {
		if (helperPool != null) {
			helperPool.removeAll();
		}
		XMLPooled.clear();
	}

	// methods for setting min and maximum object pool size .. need resetPool
	// call */
	public static void setMinimumPoolSize(int NewMinimum) {
		if (NewMinimum <= maximumPoolObjects) {
			minimumPoolObjects = NewMinimum;
		}
	}

	public static void setMaximumPoolSize(int NewMaximum) {
		if (NewMaximum >= minimumPoolObjects) {
			maximumPoolObjects = NewMaximum;
		}
	}

	/* !MOD 20040902 DG END */

	/**
	 * Get the text content of the node
	 * @param node node
	 * @return text content
	 */
	public static String getTextContent(Node node) {
		return getTextContent(node, null, null);
	}

	/**
	 * Get the text content of the node
	 * @param node node
	 * @param nodeName name of the node for which the content is fetched (may be null for all nodes)
	 * @param attributes map of attributes the node must have set (may be null for no restriction to attributes)
	 * @return text content
	 */
	public static String getTextContent(Node node, String nodeName, Map attributes) {
		StringBuffer content = new StringBuffer();

		recursiveFillContent(node, nodeName, attributes, content, false);
		return content.toString().trim();
	}

	/**
	 * Recursive method to collect the text contents of all children of the node
	 * @param node node
	 * @param content stringbuffer
	 */
	protected static void recursiveFillContent(Node node, String nodeName,
			Map attributes, StringBuffer content, boolean takeTextContent) {
		if (node.getNodeType() == Node.CDATA_SECTION_NODE || node.getNodeType() == Node.TEXT_NODE) {
			if (takeTextContent) {
				content.append(node.getNodeValue());
			}
		} else {
			boolean takeSubText = true;

			if (nodeName != null) {
				if (!nodeName.equals(node.getNodeName())) {
					takeSubText = false;
				}
			}
			if (attributes != null && node instanceof Element) {
				Element element = (Element) node;

				for (Iterator iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
					Map.Entry entry = (Map.Entry) iterator.next();

					if (!entry.getValue().equals(element.getAttribute(entry.getKey().toString()))) {
						takeSubText = false;
						break;
					}
				}
			}
			NodeList childNodes = node.getChildNodes();

			for (int i = 0; i < childNodes.getLength(); ++i) {
				recursiveFillContent(childNodes.item(i), nodeName, attributes, content, takeSubText);
			}
		}
	}
}
