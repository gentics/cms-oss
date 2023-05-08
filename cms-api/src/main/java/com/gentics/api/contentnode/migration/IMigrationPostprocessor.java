package com.gentics.api.contentnode.migration;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.lib.log.NodeLogger;

/**
 * Implementations of this interface can be configured during Tag migration to apply post-processing to migrated objects following the migration process.
 * 
 * @author Taylor
 */
public interface IMigrationPostprocessor {

	/**
	 * Performs post-processing on a template following a tag type migration.
	 * 
	 * @param template
	 *            the template to apply the post-processing to
	 * @param request
	 *            the TtmMigrationRequest which initiated the migration process, containing the tag migration mappings
	 * @param logger
	 *            the tag type migration logger
	 * @throws NodeException
	 */
	void applyPostMigrationProcessing(Template template, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Performs post-processing on a page following a tag type migration.
	 * 
	 * @param page
	 *            the page to apply the post-processing to
	 * @param request
	 *            the TtmMigrationRequest which initiated the migration process, containing the tag migration mappings
	 * @param logger
	 *            the tag type migration logger
	 * @throws NodeException
	 */
	void applyPostMigrationProcessing(Page page, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Performs post-processing on a folder following a tag type migration.
	 * 
	 * @param folder
	 *            the folder to apply the post-processing to
	 * @param request
	 *            the TtmMigrationRequest which initiated the migration process, containing the tag migration mappings
	 * @param logger
	 *            the tag type migration logger
	 * @throws NodeException
	 */
	void applyPostMigrationProcessing(Folder folder, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Performs post-processing on a image following a tag type migration.
	 * 
	 * @param image
	 *            the image to apply the post-processing to
	 * @param request
	 *            the TtmMigrationRequest which initiated the migration process, containing the tag migration mappings
	 * @param logger
	 *            the tag type migration logger
	 * @throws NodeException
	 */
	void applyPostMigrationProcessing(Image image, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Performs post-processing on a file following a tag type migration.
	 * 
	 * @param file
	 *            the file to apply the post-processing to
	 * @param request
	 *            the TtmMigrationRequest which initiated the migration process, containing the tag migration mappings
	 * @param logger
	 *            the tag type migration logger
	 * @throws NodeException
	 */
	void applyPostMigrationProcessing(File file, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException;

	/**
	 * Performs post-processing on a page following a template migration.
	 * 
	 * @param page
	 *            the page to apply the post-processing to
	 * @param request
	 *            the TemplateMigrationRequest which initiated the migration process, containing the template migration mappings
	 * @param logger
	 *            the template migration logger
	 * @throws NodeException
	 */
	void applyPostMigrationProcessing(Page page, TemplateMigrationRequest request, NodeLogger logger) throws MigrationException;

}
