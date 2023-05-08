package com.gentics.contentnode.publish;

import java.util.List;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.NodeConfig;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectIdWithAttributes;
import com.gentics.contentnode.publish.PublishQueue.NodeObjectWithAttributes;

/**
 * Implementation of {@link AbstractPageList} that will return pages from the Transaction
 */
public class PageList extends AbstractPageList {
	/**
	 * Create an instance
	 * @param pageIds list of page IDs
	 * @param config node config
	 * @param prepareThreshold prepare threshold
	 */
	public PageList(List<NodeObjectIdWithAttributes> pageIds, NodeConfig config, int prepareThreshold) {
		super(pageIds, config);
	}

	@Override
	protected NodeObjectWithAttributes<Page> getPage(NodeObjectIdWithAttributes pageId) throws NodeException {
		return new NodeObjectWithAttributes<Page>(TransactionManager.getCurrentTransaction().getObject(Page.class, pageId.id), pageId.attributes);
	}
}
