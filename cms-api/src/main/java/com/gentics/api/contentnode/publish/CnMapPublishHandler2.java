package com.gentics.api.contentnode.publish;

import java.util.Set;

import com.gentics.api.lib.resolving.Resolvable;

/**
 * Enhanced interface for publish handlers
 * @see CnMapPublishHandler
 */
public interface CnMapPublishHandler2 extends CnMapPublishHandler {
	/**
	 * This method is called for every object, which needs to be updated in the Content Repository.
	 * The method completely replaces {@link #updateObject(Resolvable)}
	 * @param object resolvable object that will be written into the Content Repository
	 * @param original original object in the Content Repository
	 * @param attributes set of attribute names that will be published. All other attributes will not be changed in the Content Repository.
	 * @throws CnMapPublishException
	 */
	void updateObject(Resolvable object, Resolvable original, Set<String> attributes) throws CnMapPublishException;
}
