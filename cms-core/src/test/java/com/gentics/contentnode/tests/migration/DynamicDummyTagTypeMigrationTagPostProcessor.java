package com.gentics.contentnode.tests.migration;

import java.util.HashSet;
import java.util.Set;

import com.gentics.api.contentnode.migration.IMigrationPostprocessor;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.contentnode.rest.model.File;
import com.gentics.contentnode.rest.model.Folder;
import com.gentics.contentnode.rest.model.Image;
import com.gentics.contentnode.rest.model.Page;
import com.gentics.contentnode.rest.model.Property;
import com.gentics.contentnode.rest.model.Tag;
import com.gentics.contentnode.rest.model.Template;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.lib.log.NodeLogger;

/**
 * A dummy post processor that changes the tags of a page
 * 
 * @author johannes2
 * 
 */
public class DynamicDummyTagTypeMigrationTagPostProcessor implements IMigrationPostprocessor {

	public static final int THROW_EXCEPTION = 10;
	public static final int THROW_RUNTIME_EXCEPTION = 20;
	public static final int DEFAULT_BEHAVIOUR = 0;

	private static int postProcessorTestBehavior = DEFAULT_BEHAVIOUR;

	/**
	 * Sets the post processor test behavior
	 * 
	 * @param value
	 */
	public static void setPostProcessorTestBehavior(int value) {
		postProcessorTestBehavior = value;
	}

	@Override
	public void applyPostMigrationProcessing(Template template, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {
	}

	@Override
	public void applyPostMigrationProcessing(Page page, TagTypeMigrationRequest request, NodeLogger logger) throws MigrationException {

		if (postProcessorTestBehavior == THROW_EXCEPTION) {
			throw new MigrationException("This is a test exception");
		}

		if (postProcessorTestBehavior == THROW_RUNTIME_EXCEPTION) {
			throw new RuntimeException("This is a runtime test exception");
		}

		Set<String> tagsToBeRemoved = new HashSet<>();
		for (Tag tag : page.getTags().values()) {

			// Remove the vtl1 tag
			if ("vtl1".equalsIgnoreCase(tag.getName())) {
				tagsToBeRemoved.add(tag.getName());
			// Modifiy the htmltag
			} else if ("htmltag".equalsIgnoreCase(tag.getName())) {
				Property html = tag.getProperties().get("html");
				html.setStringValue(html.getStringValue() + "MODIFIED");
			}
		}

		page.getTags().keySet().removeAll(tagsToBeRemoved);

		// Modify the name
		page.setName("migrated page_" + page.getId());
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
