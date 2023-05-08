/*
 * @author tobiassteiner
 * @date Jan 6, 2011
 * @version $Id: SAX2DOMHandler.java,v 1.1.2.2 2011-02-26 08:51:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.sax.xerces;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Can be used to build a DOM tree from a xerces (XNI) SAX source. 
 * TODO: There are some events that are not implemented and result in
 * skipped content.
 */
public class SAX2DOMHandler implements XMLDocumentHandler {

	/**
	 * Whether CDATA sections should be created as Text Nodes, not
	 * CDATA nodes. (used for jTidy compatibility since CDATA sections
	 * can't be created with jTidy.)
	 */
	protected boolean cdataAsText = false;

	/**
	 * Whether to use the non-namespace variants of
	 * createElementNS() and setAttributeNS() etc.
	 * (used for jTidy compatibility again because jTidy doesn't handle
	 * namespace information.)
	 */
	protected boolean noNamespaceHandling = false;

	/**
	 * Whether processing instructions should be skipped (will not be created
	 * and result in skippedContent). (Again for jTidy compatibility, because
	 * jTidy doesn't allow the creation of PIs.)
	 */
	protected boolean skipProcessingInstructions = false;
    
	protected Node root = null;
	protected Document doc = null;
	protected Node cursor = null;
	protected StringBuilder cdata = null;
	protected boolean skippedContent = false;

	/**
	 * @see #setRoot(Node)
	 */
	public SAX2DOMHandler(Node root) {
		setRoot(root);
	}
    
	/**
	 * @param root the owner document of the given root node is used to create
	 *   nodes. the created nodes will be appended to the root node.
	 */
	public void setRoot(Node root) {
		Document doc = root.getOwnerDocument();

		// documents themselves may not have a owner document
		if (null == doc) {
			if (root instanceof Document) {
				doc = (Document) root;
			} else {
				throw new NullPointerException("passed root node has no owner document");
			}
		}
		this.root = root;
		this.doc = doc;
		this.cursor = root;
	}
    
	/**
	 * @return the root node of the assembled DOM tree. 
	 */
	public Node getRoot() {
		return root;
	}

	/**
	 * @return true if some sax events could not be assembled into the DOM tree.
	 */
	public boolean hasSkippedContent() {
		return skippedContent;
	}

	public void characters(XMLString xmlstring, Augmentations augmentations) throws XNIException {
		if (null != cdata) {
			cdata.append(xmlstring.toString());
		} else {
			cursor.appendChild(doc.createTextNode(xmlstring.toString()));
		}
	}

	public void comment(XMLString xmlstring, Augmentations augmentations) throws XNIException {
		cursor.appendChild(doc.createComment(xmlstring.toString()));
	}

	public void emptyElement(QName qname, XMLAttributes xmlattributes,
			Augmentations augmentations) throws XNIException {
		this.startElement(qname, xmlattributes, augmentations);
		this.endElement(qname, augmentations);
	}

	public void startCDATA(Augmentations augmentations) throws XNIException {
		cdata = new StringBuilder();
	}

	public void endCDATA(Augmentations augmentations) throws XNIException {
		Node node;

		if (cdataAsText) {
			node = doc.createTextNode(cdata.toString());
		} else {
			node = doc.createCDATASection(cdata.toString());
		}
		cursor.appendChild(node);
		cdata = null;
	}

	public void startElement(QName qname, XMLAttributes attr,
			Augmentations augmentations) throws XNIException {
		Element enter;

		if (noNamespaceHandling || null == qname.uri) {
			enter = doc.createElement(qname.rawname);
		} else {
			enter = doc.createElementNS(qname.uri, qname.rawname);
		}
		for (int i = 0; i < attr.getLength(); i++) {
			String attQName = attr.getQName(i);
			String value = attr.getValue(i);
			String uri = attr.getURI(i);

			if (noNamespaceHandling || null == uri) {
				enter.setAttribute(attQName, value);
			} else {
				enter.setAttributeNS(uri, attQName, value);
			}
		}
		cursor.appendChild(enter);
		cursor = enter;
	}

	public void endElement(QName qname, Augmentations augmentations) throws XNIException {
		cursor = (Element) cursor.getParentNode();
	}

	public XMLDocumentSource getDocumentSource() {
		// nothing to do / not supported
		return null;
	}

	public void setDocumentSource(XMLDocumentSource xmldocumentsource) {// nothing to do / not supported
	}

	public void startDocument(XMLLocator xmllocator, String s,
			NamespaceContext namespacecontext, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void endDocument(Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void endGeneralEntity(String s, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void ignorableWhitespace(XMLString xmlstring, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void processingInstruction(String s, XMLString xmlstring, Augmentations augmentations) throws XNIException {
		if (!skipProcessingInstructions) {
			Node pi = doc.createProcessingInstruction(s, xmlstring.toString());

			cursor.appendChild(pi);
			return;
		}
		skippedContent = true;
	}
    
	public void doctypeDecl(String s, String s1, String s2, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void startGeneralEntity(String s, XMLResourceIdentifier xmlresourceidentifier,
			String s1, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void textDecl(String s, String s1, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}

	public void xmlDecl(String s, String s1, String s2, Augmentations augmentations) throws XNIException {
		skippedContent = true;
	}
}
