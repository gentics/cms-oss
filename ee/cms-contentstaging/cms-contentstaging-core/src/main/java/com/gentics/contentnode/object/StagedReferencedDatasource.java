package com.gentics.contentnode.object;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.devtools.model.DatasourceModel;
import com.gentics.contentnode.etc.Consumer;

/**
 * Staged referenced datasource
 */
public class StagedReferencedDatasource extends AbstractStagedReferencedObject<Datasource> {
	/**
	 * Create the wrapper
	 * @param wrapped datasource
	 */
	public StagedReferencedDatasource(Datasource wrapped) {
		super(wrapped);
	}

	@Override
	public void produceStagedHashComponents(Consumer<Object> consumer) throws NodeException {
		consumer.accept(Datasource.NODE2DEVTOOL.apply(wrapped, new DatasourceModel()));
	}
}
