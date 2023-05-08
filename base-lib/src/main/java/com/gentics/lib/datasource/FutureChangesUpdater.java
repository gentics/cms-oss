package com.gentics.lib.datasource;

import com.gentics.api.lib.datasource.Datasource;
import com.gentics.api.lib.datasource.WriteableVersioningDatasource;

/**
 * Job to automatically due update future changes
 */
public class FutureChangesUpdater extends AbstractDatasourceJob {
	/**
	 * Create an instance
	 * @param factory datasource factory
	 */
	public FutureChangesUpdater(DatasourceFactory factory) {
		super(factory);
	}

	@Override
	protected void process() {
		Datasource ds = getDatasource();

		if (ds instanceof WriteableVersioningDatasource) {
			if (logger.isDebugEnabled()) {
				logger.debug("job updateFutureChanges fired");
			}
			((WriteableVersioningDatasource) ds).updateDueFutureChanges();
		}
	}
}
