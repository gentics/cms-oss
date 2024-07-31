package com.gentics.contentnode.staging;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.rest.resource.impl.ContentStagingResourceImpl;

/**
 * Implementation of the {@link StagingStatusService}
 */
public class StagingStatusServiceImpl implements StagingStatusService {
	@Override
	public <T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects,
			String stagingPackageName, Function<T, K> keyProvider, boolean useVariants) throws NodeException {
		return ContentStagingResourceImpl.checkStagingStatus(nodeObjects, stagingPackageName, keyProvider, useVariants);
	}

	@Override
	public Map<String, StagingStatus> checkStagingStatus(String stagingPackageName, Map<String, List<String>> ids,
			boolean useVariants) throws NodeException {
		return ContentStagingResourceImpl.checkStagingStatus(stagingPackageName, ids, useVariants);
	}
}
