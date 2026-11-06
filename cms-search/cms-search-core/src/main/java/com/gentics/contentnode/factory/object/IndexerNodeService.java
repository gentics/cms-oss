package com.gentics.contentnode.factory.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.UpdateNodeIdsInIndexTransactional;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Implementation of {@link NodeService} that updates node IDs in search indices
 */
public class IndexerNodeService implements NodeService {
	@Override
	public void onSave(Node object, boolean isNew, boolean contentModified, int userId) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.ELASTICSEARCH) && isNew && object.isChannel()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.addTransactional(new UpdateNodeIdsInIndexTransactional(object.getMaster().getId()));
		}
	}

	@Override
	public void onDelete(Node object, boolean wastebin, int userId) throws NodeException {
		if (NodeConfigRuntimeConfiguration.isFeature(Feature.ELASTICSEARCH) && object.isChannel()) {
			Transaction t = TransactionManager.getCurrentTransaction();
			t.addTransactional(new UpdateNodeIdsInIndexTransactional(object.getMaster().getId()));
		}
	}
}
