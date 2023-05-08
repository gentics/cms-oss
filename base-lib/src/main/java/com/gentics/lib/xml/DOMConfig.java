/* DOMConfig will assist you when reading xml configuration files 
 * @author clemens
 * @date 21.02.2006
 * @version $Id: DOMConfig.java,v 1.2 2006-07-28 08:46:46 herbert Exp $
 */
package com.gentics.lib.xml;

import java.io.StringReader;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.gentics.api.lib.exception.ParserException;
import com.gentics.lib.log.NodeLogger;

public class DOMConfig {
    
	/**
	 * create logger for internal use
	 * @return logger
	 */
	protected static NodeLogger getLogger() {
		return NodeLogger.getNodeLogger("DOMConfig");
	}
    
	/**
	 * retrieve properties from node. elements which are stored inside the
	 * properties object are estimated to be contained directly in the provided
	 * node. Subchilds will be ignored.
	 * @param element the node which contains the elements
	 * @param parameterElementName name of element which will contain a property
	 * @param idAttributeName name of the element's attribute which will be used
	 *        as the property id
	 * @return
	 */
	public static Properties loadProperties(Element element, String parameterElementName,
			String idAttributeName) {
		Properties properties = new Properties();

		// no children? return an empty property object
		if (element == null || !element.hasChildNodes()) {
			return properties;
		}
		NodeList children = element.getChildNodes();
		int childrenLength = children.getLength();

		// if there are no children we will return an empty property object
		if (childrenLength == 0) {
			return properties;
		}

		Node curNode; // the current node
		Node idAttr; // the id attribute node
		Node curNodeValue; // value node of current node which will contain the

		// current node's text

		for (int i = 0; i < childrenLength - 1; i++) {
			curNode = children.item(i);
			if (!curNode.hasAttributes()) {
				continue; // no need to carry on if there is no id attribute
			}
			idAttr = curNode.getAttributes().getNamedItem(idAttributeName);
			curNodeValue = curNode.getFirstChild();

			// add current node to properties if element name and name of the id
			// attribute apply
			if (curNode.getNodeType() == Node.ELEMENT_NODE && parameterElementName.equals(curNode.getNodeName()) && idAttr != null
					&& idAttr.getNodeType() == Node.ATTRIBUTE_NODE && curNodeValue != null && curNodeValue.getNodeType() == Node.TEXT_NODE) {
				properties.put(idAttr.getNodeValue(), curNodeValue.getNodeValue());
			} else {
				getLogger().warn("unable to turn node `" + curNode + "` into a parameter.");
			}

		}
		return properties;
	}

	/**
	 * Parse provided XML-string, retrieve a specific section as NodeList.
	 * Section refers to a xml sniplet enclosed by a tag which matches the
	 * provided tagName
	 * @param xml XML string
	 * @param tagName describe the tag name of the section which should be
	 *        returned
	 * @return XML section as NodeList, null if section could not be found
	 * @throws ParserException 
	 */
	public static NodeList extractXMLSection(String xml, String tagName) throws ParserException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(xml.trim())));
			NodeList list = document.getElementsByTagName(tagName);

			return list;
		} catch (SAXException e) {
			throw new ParserException("Unable to extract node {" + tagName + "} from xml.", e);
		} catch (Exception e) {
			getLogger().warn("could not extract node {" + tagName + "} from provided xml string.", e);
		}
		return null;
	}
    
	/**
	 * retrieve first element by tag name  
	 * @param element 
	 * @param name
	 * @return first element with desired name, null on failure
	 */
	public static Element getFirstElementByTagName(Element element, String name) {
		if (!element.hasChildNodes() || "".equals(name)) {
			return null;
		}
		NodeList nodeList = element.getElementsByTagName(name);

		if (nodeList.getLength() == 0) {
			return null;
		}
		return (Element) nodeList.item(0);
	}
}
