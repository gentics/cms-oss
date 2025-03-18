/*
 * @author norbert
 * @date 28.04.2010
 * @version $Id: TreeNode.java,v 1.1 2010-04-28 15:44:30 norbert Exp $
 */
package com.gentics.contentnode.rest.model.response;

import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

import com.gentics.contentnode.rest.model.Folder;

/**
 * TreeNode Object, which build a folder structure
 * @author norbert
 */
@XmlRootElement
public class TreeNode {

	/**
	 * folder of the treenode
	 */
	private Folder folder;

	/**
	 * List of children
	 */
	private List<TreeNode> children;

	/**
	 * Constructor used by JAXB
	 */
	public TreeNode() {}

	/**
	 * @return the folder
	 */
	public Folder getFolder() {
		return folder;
	}

	/**
	 * @return the children
	 */
	public List<TreeNode> getChildren() {
		return children;
	}

	/**
	 * @param folder the folder to set
	 */
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	/**
	 * @param children the children to set
	 */
	public void setChildren(List<TreeNode> children) {
		this.children = children;
	}
}
