package com.gentics.contentnode.tests.migration;

import com.gentics.api.contentnode.migration.IMigrationPreprocessor;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.lib.log.NodeLogger;

/**
 * Abstract Preprocessor implementation
 */
public abstract class AbstractPreprocessor implements IMigrationPreprocessor {

	@Override
	public Result apply(Tag tag, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		return apply(tag, logger);
	}

	@Override
	public Result apply(Tag tag, TemplateMigrationRequest request, NodeLogger logger) throws MigrationException {
		return apply(tag, logger);
	}

	/**
	 * Apply the preprocessor
	 * @param tag tag
	 * @param logger logger
	 * @return result
	 * @throws MigrationException
	 */
	protected abstract Result apply(Tag tag, NodeLogger logger) throws MigrationException;
}
