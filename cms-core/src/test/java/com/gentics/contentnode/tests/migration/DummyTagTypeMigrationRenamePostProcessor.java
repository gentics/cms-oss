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
 * A simple post processor that changes the names of the various objects
 * 
 * @author johannes2
 * 
 */
public class DummyTagTypeMigrationRenamePostProcessor implements IMigrationPostprocessor {

	@Override
	public void applyPostMigrationProcessing(Template template, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		template.setName("migrated template_" + template.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		page.setName("migrated page_" + page.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Folder folder, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		folder.setName("migrated folder_" + folder.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Image image, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		image.setName("migrated image_" + image.getId());
	}

	@Override
	public void applyPostMigrationProcessing(File file, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		file.setName("migrated file_" + file.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TemplateMigrationRequest request, NodeLogger logger) throws MigrationException {
		page.setName("migrated page_" + page.getId());
	}

}
