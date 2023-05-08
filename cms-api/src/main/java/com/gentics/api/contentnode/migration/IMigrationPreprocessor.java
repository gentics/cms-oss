package com.gentics.api.contentnode.migration;

import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementations of this interface can be configured during Tag migration to apply pre-processing
 * to objects prior to the migration process.
 */
public interface IMigrationPreprocessor {
	/**
	 * Apply the preprocessor to the tag for a tagtype migration
	 * @param tag REST model of the tag
	 * @param request ttm request
	 * @param logger logger
	 * @return preprocessor result
	 * @throws MigrationException
	 */
	Result apply(Tag tag, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Apply the preprocessor to the tag for a template migration
	 * @param tag REST model of the tag
	 * @param request template migration request
	 * @param logger logger
	 * @return preprocessor result
	 * @throws MigrationException
	 */
	Result apply(Tag tag, TemplateMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Possible result values for preprocessors
	 */
	public enum Result {
		/**
		 * The tag passes the preprocessor, modifications made on the REST model will be
		 * transformed back to the tag
		 */
		pass,

		/**
		 * Migration of the tag shall be skipped, no more preprocessors will be called for this tag
		 * and modifications made on the tag by this or any previous preprocessors will be ignored
		 */
		skiptag,

		/**
		 * Migration of the object containing the tag will be skipped. Any modifications already
		 * made on any of the tags of the object will be ignored
		 */
		skipobject
	}
}
