package com.gentics.contentnode.tests.migration;

import com.gentics.api.contentnode.migration.IMigrationPostprocessor;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.lib.log.NodeLogger;

/**
 * Empty Implemenation of {@link IMigrationPostprocessor}
 */
public class EmptyPostProcessor implements IMigrationPostprocessor {

	@Override
	public void applyPostMigrationProcessing(Template template, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
	}

	@Override
	public void applyPostMigrationProcessing(Folder folder, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
	}

	@Override
	public void applyPostMigrationProcessing(Image image, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
	}

	@Override
	public void applyPostMigrationProcessing(File file, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TemplateMigrationRequest request, NodeLogger logger) throws MigrationException {
	}
}
