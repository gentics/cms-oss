package com.gentics.contentnode.staging;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Feature;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.etc.ServiceLoaderUtil;
import com.gentics.contentnode.object.StageableNodeObject;
import com.gentics.contentnode.rest.model.response.StagingStatus;
import com.gentics.contentnode.runtime.NodeConfigRuntimeConfiguration;

/**
 * Utility class, which acts as bridge to the optional content staging module
 */
public class StagingUtil {
	/**
	 * {@link StagingStatusService} service
	 */
	private final static ServiceLoaderUtil<StagingStatusService> loader = ServiceLoaderUtil.load(StagingStatusService.class);

	/**
	 * Check if the given node object is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObject
	 * @param stagingPackageName
	 * @return
	 * @throws NodeException
	 */
	public static <T extends StageableNodeObject> StagingStatus checkStagingStatus(T nodeObject, String stagingPackageName) throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.CONTENT_STAGING) || StringUtils.isBlank(stagingPackageName)) {
			return null;
		}

		return checkStagingStatus(Collections.singleton(nodeObject), stagingPackageName, T::getId).getOrDefault(nodeObject.getId(), new StagingStatus());
	}

	/**
	 * Check if the given node object collection is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObjects
	 * @param stagingPackageName
	 * @param keyProvider
	 * @return map of object ID / status.
	 * @throws NodeException
	 */
	public static <T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects, String stagingPackageName, Function<T, K> keyProvider) throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.CONTENT_STAGING) || StringUtils.isBlank(stagingPackageName)) {
			return null;
		}
		return checkStagingStatus(nodeObjects, stagingPackageName, keyProvider, false);
	}

	/**
	 * Check if the given node object collection is contained into the asked content package, and how recent its data is.
	 * This method expects a current transaction.
	 * 
	 * @param <T>
	 * @param nodeObjects
	 * @param stagingPackageName
	 * @param keyProvider
	 * @param useVariants should the status include the object variants, if there are any
	 * @return map of object ID / status.
	 * @throws NodeException
	 */
	public static <T extends StageableNodeObject, K> Map<K, StagingStatus> checkStagingStatus(Collection<T> nodeObjects,
			String stagingPackageName, Function<T, K> keyProvider, boolean useVariants) throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.CONTENT_STAGING) || StringUtils.isBlank(stagingPackageName)) {
			return null;
		}
		return loader.execForFirst(
				service -> service.checkStagingStatus(nodeObjects, stagingPackageName, keyProvider, useVariants))
				.orElse(Collections.emptyMap());
	}

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
	public static Map<String, StagingStatus> checkStagingStatus(String stagingPackageName, Map<String, List<String>> ids, boolean useVariants) throws NodeException {
		if (!NodeConfigRuntimeConfiguration.isFeature(Feature.CONTENT_STAGING) || StringUtils.isBlank(stagingPackageName)) {
			return null;
		}
		return loader.execForFirst(service -> service.checkStagingStatus(stagingPackageName, ids, useVariants)).orElse(Collections.emptyMap());
	}
}
