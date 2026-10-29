package com.gentics.contentnode.rest.util;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.etc.Function;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.StagedReferencedObject;
import com.gentics.contentnode.rest.model.staging.ImplementationHashResponse;

/**
 * Staging specific utilities
 */
public class MiscStagingUtils {
	/**
	 * Load the given object (with check for existence and view permission) and return the implementation hash for it
	 * @param <T> type of the object class
	 * @param clazz object class
	 * @param wrapper wrapper function
	 * @param id object id
	 * @return response containing the implementation hash
	 * @throws NodeException
	 */
	public static <T extends NodeObject, U extends StagedReferencedObject<T>> ImplementationHashResponse getImplementationHash(
			Class<T> clazz, Function<T, U> wrapper, String id) throws NodeException {
		T entity = MiscUtils.load(clazz, id);
		Pair<String, String> stagedHash = wrapper.apply(entity).getStagedHash();
		return new ImplementationHashResponse().setHash(stagedHash.getLeft()).setBase(stagedHash.getRight());
	}
}
