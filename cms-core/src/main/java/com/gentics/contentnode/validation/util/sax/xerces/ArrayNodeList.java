/*
 * @author tobiassteiner
 * @date Jan 7, 2011
 * @version $Id: ArrayNodeList.java,v 1.1.2.2 2011-02-26 08:51:52 tobiassteiner Exp $
 */
package com.gentics.contentnode.validation.util.sax.xerces;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An implementation of a {@link NodeList} that wraps
 * an array of {@link Node}s.
 */
public class ArrayNodeList implements NodeList {

	protected final Node[] list;
    
	public ArrayNodeList(Node[] list) {
		this.list = list;
	}
    
	public int getLength() {
		return list.length;
	}

	public Node item(int index) {
		return list[index];
	}
}
