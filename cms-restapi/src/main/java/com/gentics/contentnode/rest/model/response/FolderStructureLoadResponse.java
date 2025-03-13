/*
 * @author norbert
 * @date 27.04.2010
 * @version $Id: FolderStructureLoadResponse.java,v 1.1 2010-04-28 15:44:31 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response to a request for loading a folder structure
 * @author norbert
 */
@XmlRootElement
public class FolderStructureLoadResponse extends GenericResponse {

	/**
	 * Root node of the folder structure
	 */
	private TreeNode rootNode;

	/**
	 * Constructor used by JAXB
	 */
	public FolderStructureLoadResponse() {}

	/**
	 * Constructor with a single message, response info and the root node of the folder structure
	 * @param message message
	 * @param responseInfo response info
	 * @param rootNode root node
	 */
	public FolderStructureLoadResponse(Message message, ResponseInfo responseInfo, TreeNode rootNode) {
		super(message, responseInfo);
		this.rootNode = rootNode;
	}

	/**
	 * @return the rootNode
	 */
	public TreeNode getRootNode() {
		return rootNode;
	}

	/**
	 * @param rootNode the rootNode to set
	 */
	public void setRootNode(TreeNode rootNode) {
		this.rootNode = rootNode;
	}
}
