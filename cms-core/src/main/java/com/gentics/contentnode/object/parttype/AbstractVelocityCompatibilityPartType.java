/*
 * @author alexander
 * @date 12.04.2007
 * @version $Id: AbstractVelocityCompatibilityPartType.java,v 1.1 2007-04-12 15:18:28 alexander Exp $
 */
package com.gentics.contentnode.object.parttype;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for all compatibility part types
 */
public abstract class AbstractVelocityCompatibilityPartType extends AbstractVelocityPartType {

	/**
	 * Read all text and cdata sections of node and append
	 * 
	 * @param node The node from which to extract all text and cdata nodes
	 * @return The extracted template. 
	 */
	protected static String getTemplateFromNode(Node node) {
        
		StringBuffer text = new StringBuffer();
        
		// read children (text and cdata nodes) and aggregate
		if (node.hasChildNodes()) {
			NodeList children = node.getChildNodes();

			for (int k = 0; k < children.getLength(); k++) {
				Node item = children.item(k);

				if (item.getNodeType() == Node.CDATA_SECTION_NODE || item.getNodeType() == Node.TEXT_NODE) {
					text.append(item.getNodeValue());
				}
			}
		}
        
		return text.toString();
	}

}
