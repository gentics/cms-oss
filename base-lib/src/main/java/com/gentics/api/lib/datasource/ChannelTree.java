package com.gentics.api.lib.datasource;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * Root Node of the channel tree
 */
public class ChannelTree implements Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -3520944213722587691L;

	/**
	 * List of children
	 */
	protected List<ChannelTreeNode> children = new Vector<ChannelTreeNode>();

	/**
	 * Create an instance
	 */
	public ChannelTree() {}

	/**
	 * Get the list of children
	 * @return list of children
	 */
	public List<ChannelTreeNode> getChildren() {
		return children;
	}
	
	public boolean equals(ChannelTree that) {
		if (this.getChildren().size() != that.getChildren().size()) {
			return false;
		}

		// If there are no children and since the previous check the other tree has no children - we're done.
		if (this.getChildren().size() == 0) {
			return true;
		}
		
		List<ChannelTreeNode> thisChildren = this.getChildren();
		List<ChannelTreeNode> thatChildren = that.getChildren();
		
		for (int i = 0; i < thisChildren.size(); i++) {
			if (!thisChildren.get(i).equals(thatChildren.get(i))) {
				return false;
			}
		}
		
		return true;
	}
}
