package com.gentics.contentnode.staging;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.rest.model.response.StagingStatus;

/**
 * Interface for the service which checks whether objects are contained in content packages.
 */
public interface StagingStatusService {
	/**
	 * Check if the given node object collection is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObjects
	 * @param stagingPackageName
	 * @param keyProvider
	 * @param useVariants should the statuc include the object variants, if there are any
	 * @return map of object ID / status.
	 * @throws NodeException
	 */
	<T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects, String stagingPackageName, Function<T, K> keyProvider, boolean useVariants) throws NodeException;

	/**
	 * Check if the given objects are contained in the content package.
	 * This method expects a current transaction.
	 * 
	 * @param stagingPackageName package name
	 * @param ids map of object types to list of IDs
	 * @param useVariants true to also include variants (i.e. language variants of pages) of the queried objects
	 * @return staging status per object ID
	 * @throws NodeException
	 */
	Map<String, StagingStatus> checkStagingStatus(String stagingPackageName, Map<String, List<String>> ids, boolean useVariants) throws NodeException;
}
