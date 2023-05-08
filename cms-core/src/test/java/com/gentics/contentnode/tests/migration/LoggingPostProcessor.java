package com.gentics.contentnode.tests.migration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.api.contentnode.migration.IMigrationPostprocessor;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.lib.log.NodeLogger;

/**
 * PostProcessor implementation that simply stores all objects that are migrated
 */
public class LoggingPostProcessor implements IMigrationPostprocessor {
	/**
	 * Map for storing all post processed objects
	 */
	protected static Map<Integer, Set<Integer>> processed = new HashMap<>();

	/**
	 * Reset the processing information
	 */
	public static void reset() {
		processed.clear();
	}

	/**
	 * Check whether the given object was processed
	 * @param object object
	 * @return true if the object was processed, false if not
	 */
	public static boolean isProcessed(NodeObject object) {
		if (object == null) {
			return false;
		}
		return getTypeSet(object.getTType()).contains(object.getId());
	}

	/**
	 * Get the ID-set of processed objects of given type. If no such set exists, create one
	 * @param type type
	 * @return ID-set
	 */
	protected static Set<Integer> getTypeSet(int type) {
		Set<Integer> typeSet = processed.get(type);
		if (typeSet == null) {
			typeSet = new HashSet<>();
			processed.put(type, typeSet);
		}
		return typeSet;
	}

	@Override
	public void applyPostMigrationProcessing(Template template, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		getTypeSet(com.gentics.contentnode.object.Template.TYPE_TEMPLATE).add(template.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		getTypeSet(com.gentics.contentnode.object.Page.TYPE_PAGE).add(page.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Folder folder, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		getTypeSet(com.gentics.contentnode.object.Folder.TYPE_FOLDER).add(folder.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Image image, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		getTypeSet(ImageFile.TYPE_IMAGE).add(image.getId());
	}

	@Override
	public void applyPostMigrationProcessing(File file, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
		getTypeSet(com.gentics.contentnode.object.File.TYPE_FILE).add(file.getId());
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TemplateMigrationRequest request, NodeLogger logger) throws MigrationException {
	}
}
