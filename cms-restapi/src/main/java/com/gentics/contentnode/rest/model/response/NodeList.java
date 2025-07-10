package com.gentics.contentnode.rest.model.response;

import java.util.List;

import com.gentics.contentnode.rest.model.Node;

/**
 * Model of a node list
 */
public class NodeList extends AbstractStagingStatusListResponse<Node> {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public List<Node> getItems() {
		return super.getItems();
	}
}
