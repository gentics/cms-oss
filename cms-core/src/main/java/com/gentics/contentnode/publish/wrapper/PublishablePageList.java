package com.gentics.contentnode.publish.wrapper;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.AbstractPageList;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectIdWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;

/**
 * Implementation of {@link AbstractPageList} that will return instances of {@link PublishablePage}
 */
public class PublishablePageList extends AbstractPageList {
	/**
	 * Create an instance for the given list of page IDs
	 * @param pageIds list of pageIds
	 * @param config configuration
	 * @param prepareThreshold prepare threshold
	 */
	public PublishablePageList(List<NodeObjectIdWithAttributes> pageIds, NodeConfig config, int prepareThreshold) {
		super(pageIds, config);
	}

	@Override
	protected NodeObjectWithAttributes<Page> getPage(NodeObjectIdWithAttributes pageId) throws NodeException {
		return new NodeObjectWithAttributes<Page>(PublishablePage.getInstance(pageId.getId()), pageId.getAttributes());
	}
}
