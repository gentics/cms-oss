package com.gentics.lib.xml;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by IntelliJ IDEA. User: erwin Date: 22.07.2004 Time: 11:42:24
 */
public class ElementHelper {

	protected NodeList elements;

	private class ElementNodeList implements NodeList {
		private ArrayList nodes = new ArrayList();

		public int getLength() {
			return nodes.size();
		}

		public Node item(int index) {
			return (Node) nodes.get(index);
		}

		public void add(Node node) {
			nodes.add(node);
		}
	}

	public ElementHelper(NodeList elements) {
		this.elements = elements;
	}

	public ElementHelper getElements(int subItem, String elements) {
		NodeList list = this.elements.item(subItem).getChildNodes();
		ElementNodeList ret = new ElementNodeList();

		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);

			if (item.getNodeName().equals(elements)) {
				ret.add(item);
			}
		}
		return new ElementHelper(ret);
	}

	public String getAttributeValue(int subItem, String attributeName) {
		Node item = elements.item(subItem);

		if (item == null) {
			return "";
		}
		Node attribute = item.getAttributes().getNamedItem(attributeName);

		if (attribute == null) {
			return "";
		}
		return attribute.getNodeValue();
	}

	public String getNodeValue(int subItem) {

		String test = "";
		String rcValue = "";

		Node item = elements.item(subItem);

		if (item != null) {
			if (item.getFirstChild() != null) {

				if (item.getFirstChild().getNodeValue() != null) {
					rcValue = item.getFirstChild().getNodeValue();
				}
			}
		}
		// if(item.getFirstChild().getNodeValue()!=null ) {
		// rcValue = item.getFirstChild().getNodeValue();
		// }

		// rcValue = item.getNodeValue();

		return rcValue;

	}

	/**
	 * public String getCDATA(int pos) <br>
	 * returns #cdata-section of child element
	 * @param pos int child element of private NodeList
	 * @return String
	 */
	public String getCDATA(int pos) {

		String textString = "";
		boolean gotIt = false;

		Node node = this.elements.item(pos);

		if (node == null) {
			return null;
		}
		NodeList nList = node.getChildNodes();

		for (int i = 0; i < nList.getLength(); i++) {

			if (("#cdata-section".equals(nList.item(i).getNodeName())) && (!gotIt)) {

				textString = nList.item(i).getNodeValue();
				gotIt = true;
			}

		}
		return textString;

	}

	public int getSize() {
		return this.elements.getLength();
	}
}
