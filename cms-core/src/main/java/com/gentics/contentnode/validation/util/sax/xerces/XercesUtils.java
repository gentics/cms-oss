/*
 * @author tobiassteiner
 * @date Jan 6, 2011
 * @version $Id: XercesUtils.java,v 1.1.2.1 2011-02-10 13:43:40 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.sax.xerces;

import org.apache.xerces.util.AugmentationsImpl;
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLString;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XercesUtils {
    
	/**
	 * Will generate sax events for the given node tree, on the given xerces (XNI) handler.
	 * @see #generateSAXEvents(NodeList, XMLDocumentHandler)
	 */
	public static boolean generateSAXEvents(Node node, XMLDocumentHandler handler) {
		return generateSAXEvents(new ArrayNodeList(new Node[] { node }), handler);
	}
    
	/**
	 * Will generate SAX events for the given nodes and descendants, on the
	 * given xerces (XNI) handler. Please note that while it is quite possible
	 * to have multiple root nodes in the node list, the handler may require a
	 * single root node (e.g. the document-element).
	 * TODO: events for the document node are being sent, but with null
	 *   values, which may cause an NPE in the handler. due to the incomplete
	 *   handling, document nodes, among others, will result in skipped
	 *   content. 
	 * @return true if some content could not be turned into SAX events,
	 *   false otherwise.
	 */
	public static boolean generateSAXEvents(NodeList nodes, XMLDocumentHandler handler) {
		Augmentations augs = new AugmentationsImpl();

		return generateSAXEventsForNodes(nodes, handler, augs);
	}
    
	private static boolean generateSAXEventsForNodes(NodeList nodes, XMLDocumentHandler handler, Augmentations augs) {
		boolean skippedContent = false;

		for (int i = 0; i < nodes.getLength(); i++) {
			skippedContent |= generateSAXEventsForNode(nodes.item(i), handler, augs);
		}
		return skippedContent;
	}
    
	private static boolean generateSAXEventsForNode(Node node, XMLDocumentHandler handler, Augmentations augs) {
		boolean skippedContent = false;

		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			Element elem = (Element) node;
			QName name = newQName(elem);

			handler.startElement(name, newXMLAttributes(elem.getAttributes()), augs);
			skippedContent |= generateSAXEventsForNodes(node.getChildNodes(), handler, augs);
			handler.endElement(name, augs);
			break;

		case Node.CDATA_SECTION_NODE:
			handler.startCDATA(augs);
			handler.characters(newXMLString(node.getNodeValue()), augs);
			handler.endCDATA(augs);
			break;

		case Node.COMMENT_NODE:
			handler.comment(newXMLString(node.getNodeValue()), augs);
			break;

		case Node.TEXT_NODE:
			handler.characters(newXMLString(node.getNodeValue()), augs);
			break;

		case Node.DOCUMENT_FRAGMENT_NODE:
			NodeList fragment = node.getChildNodes();

			for (int j = 0; j < fragment.getLength(); j++) {
				skippedContent |= generateSAXEventsForNode(fragment.item(j), handler, augs);
			}
			break;

		case Node.PROCESSING_INSTRUCTION_NODE:
			handler.processingInstruction(node.getNodeName(), newXMLString(node.getNodeValue()), augs);
			break;

		case Node.ATTRIBUTE_NODE:
			throw new IllegalArgumentException("Don't pass attribute nodes to me"); 

		case Node.DOCUMENT_NODE:
			// document nodes result in skipped content due to the incomplete
			// handling (since we are passing null values). 
			skippedContent = true;
			handler.startDocument(null, null, null, augs);
			skippedContent |= generateSAXEventsForNodes(node.getChildNodes(), handler, augs);
			handler.endDocument(augs);
			break;

		case Node.DOCUMENT_TYPE_NODE:
			skippedContent = true;
			break;

		case Node.ENTITY_NODE:
			skippedContent = true;
			break;

		case Node.ENTITY_REFERENCE_NODE:
			skippedContent = true;
			break;

		case Node.NOTATION_NODE:
			skippedContent = true;
			break;

		default:
			// this shouldn't happen unless we forgot to handle a node type
			// above (which shouldn't be the case).
			skippedContent = true;
			break;
		}
		return skippedContent;
	}        
        
	public static XMLString newXMLString(String str) {
		char[] chars = str.toCharArray();

		return new XMLString(chars, 0, chars.length);
	}

	public static QName newQName(Node node) {
		return new QName(node.getPrefix(), node.getLocalName(), node.getNodeName(), node.getNamespaceURI());
	}

	public static XMLAttributes newXMLAttributes(NamedNodeMap atts) {
		XMLAttributesImpl wrapper = new XMLAttributesImpl();

		for (int i = 0; i < atts.getLength(); i++) {
			Node attr = atts.item(i);
			QName qName = newQName(attr);

			wrapper.addAttribute(qName, "", attr.getNodeValue());
		}
		return wrapper;
	}
}
