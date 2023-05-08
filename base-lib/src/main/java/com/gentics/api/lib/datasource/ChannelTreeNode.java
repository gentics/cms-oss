package com.gentics.api.lib.datasource;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * A node in the channel tree
 */
public class ChannelTreeNode implements Comparable<ChannelTreeNode>, Serializable {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -1758744656212337249L;

	/**
	 * The channel of this tree node
	 */
	protected DatasourceChannel channel;

	/**
	 * List of children
	 */
	protected List<ChannelTreeNode> children = new Vector<ChannelTreeNode>();

	/**
	 * Create an instance for the given channel
	 * @param channel channel
	 */
	public ChannelTreeNode(DatasourceChannel channel) {
		this.channel = channel;
	}

	/**
	 * Get the channel
	 * @return channel (or null if this is the root node)
	 */
	public DatasourceChannel getChannel() {
		return channel;
	}

	/**
	 * Get the list of children
	 * @return list of children
	 */
	public List<ChannelTreeNode> getChildren() {
		return children;
	}
	
	public boolean equals(ChannelTreeNode that) {
		DatasourceChannel thisChannel = this.getChannel();
		DatasourceChannel thatChannel = that.getChannel();
		
		if (!thisChannel.equals(thatChannel) || this.getChildren().size() != that.getChildren().size()) {
			return false;
		}
		
		// If there are no children and since the previous check the other node has no children - we're done.
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

	public int compareTo(ChannelTreeNode that) {
		return this.channel.compareTo(that.getChannel());
	}

	@Override
	public String toString() {
		return channel.toString();
	}
}
