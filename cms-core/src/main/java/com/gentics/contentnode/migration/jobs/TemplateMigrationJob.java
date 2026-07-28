package com.gentics.contentnode.migration.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.gentics.api.contentnode.migration.IMigrationPostprocessor;
import com.gentics.api.contentnode.migration.IMigrationPreprocessor;
import com.gentics.api.contentnode.migration.IMigrationPreprocessor.Result;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.migration.MigrationDBLogger;
import com.gentics.contentnode.migration.MigrationHelper;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.MigrationPreProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationEditableTagMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationNonEditableTagMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationTagMapping;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Quartz job for performing Template Migrations
 * 
 * @author Taylor
 * 
 */
public class TemplateMigrationJob extends AbstractMigrationJob {

	/**
	 * The request object received that invoked the migration process
	 */
	private TemplateMigrationRequest request;

	/**
	 * The mapping object that defines how tags should be migrated
	 */
	private TemplateMigrationMapping mapping;

	/**
	 * Create instance
	 * @throws NodeException
	 */
	public TemplateMigrationJob() throws NodeException {
		super();
	}

	/**
	 * Set the request
	 * @param request request
	 * @return fluent API
	 */
	public TemplateMigrationJob setRequest(TemplateMigrationRequest request) {
		this.request = request;
		this.mapping = request.getMapping();
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	public String getJobDescription() {
		return new CNI18nString("templatemigrationjob").toString();
	}

	/**
	 * Execute the template migration job
	 */
	protected void processAction() throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		try {
			// Set up loggers
			logger = MigrationHelper.configureLog(logger);
			dbLogger = new MigrationDBLogger(logger);
			logger.info("Beginning template migration job {" + jobId + "}.");

			// Log in the DB that the migration job has begun
			dbLogger.createMigrationJobEntry(jobId, TEMPLATE_MIGRATION_JOB_TYPE, request.getMapping());

			// prepare preprocessors
			preparePreprocessors();

			// Validate mappings with the objects they should be applied to
			if (!isMappingValid(mapping)) {
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_INVALID);
				throw new NodeException("Migration aborted due to invalid mapping.");
			}

			// Update job status
			dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_IN_PROGRESS);

			// Retrieve the source and target templates
			Template fromTemplate = t.getObject(Template.class, mapping.getFromTemplateId());
			Template toTemplate = t.getObject(Template.class, mapping.getToTemplateId(), true);

			// Generate list of unique object IDs to apply mappings to
			Set<Integer> migrationObjectIds = getMigrationObjectIds(fromTemplate);

			migrationObjectCountTotal = migrationObjectIds.size();

			// If no migration objects are found, there is nothing to do
			if (migrationObjectIds.isEmpty()) {
				logger.info("No pages in node {" + mapping.getNodeId() + "} are based on the selected source template. There is nothing to migrate.");
				logger.info("Template migration job {" + jobId + "} has been completed.");
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_COMPLETED);
				return;
			}

			// Make a backup export of affected pages
//			backup(migrationObjectIds, Page.class, "Template Migration");

			// Apply mappings to all migration objects
			for (Integer pageId : migrationObjectIds) {

				dbLogger.createMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_STARTED);
				boolean onlineBefore = false;
				boolean modifiedBefore = false;
				SystemUser queueUser = null;
				int queuedTimestamp = -1;
				NodeObjectVersion queuedVersion = null;
				Page page;

				try {
					// We need to load the page in read only mode first.
					page = t.getObject(Page.class, pageId, false);
					// We need to store the page status since the post processor may change the page status
					onlineBefore = page.isOnline();
					modifiedBefore = page.isModified();
					queueUser = page.getPubQueueUser();
					if (page.getTimePubQueue() != null) {
						queuedTimestamp = page.getTimePubQueue().getIntTimestamp();
					}
					queuedVersion = page.getTimePubVersionQueue();
					logger.debug(String.format("Page was originally online: %b, modified: %b", onlineBefore, modifiedBefore));
					// get page for editing
					page = t.getObject(Page.class, pageId, true);
					if (!PermHandler.ObjectPermission.edit.checkObject(page)) {
						dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_SKIPPED);
						logger.error("Unable to edit page {" + pageId + "} due to missing edit permission. The page will be skipped.");
						incrementOmittedObjectsCount();
						continue;
					}
				} catch (Exception e) {
					// Mark the tags in the page as skipped
					page = t.getObject(Page.class, pageId);
					Map<String, ContentTag> contentTags = page.getContentTags();

					markSkippedTags(contentTags.values());
					logger.error("Unable to obtain lock on page {" + pageId + "} during tag type migration.", e);
					incrementOmittedObjectsCount();
					continue;
				}

				// Log that the migration of the page has begun
				dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_IN_PROGRESS);

				// Perform migration
				Result result = migratePage(page, fromTemplate, toTemplate);
				if (result == Result.skipobject) {
					logger.info(String.format("Skipping page {%d}.", pageId));
					page.unlock();
					incrementOmittedObjectsCount();
					dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_SKIPPED);
				} else {
					updateTagReferences(page, fromTemplate, toTemplate);

					// Update the template of the page
					String oldTemplateName = fromTemplate.getName();

					page.setTemplateId(mapping.getToTemplateId(), false);
					logger.info("Changed template of page {" + page.getName() + "} from {" + oldTemplateName + "} to {" + page.getTemplate().getName()
							+ "}.");

					// Apply post processors
					List<MigrationPostProcessor> sortedProcessors = request.getEnabledPostProcessors();
					page = invokePostProcessors(sortedProcessors,page);
					page.save();
					handleOriginalPageStatus(page, onlineBefore, modifiedBefore, queueUser, queuedTimestamp, queuedVersion);
					page.unlock();

					dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_COMPLETED);
					updatePercentCompleted();
				}
			}

			String templateLinkOption = request.getOptions().get(TemplateMigrationRequest.LINK_FOLDER_OPTION);

			if ("true".equalsIgnoreCase(templateLinkOption)) {
				// Link the target template to the same folders as the source template is linked to
				for (Folder folder : fromTemplate.getFolders()) {
					if (request.getMapping().getNodeId() == ObjectTransformer.getInteger(folder.getNode().getId(), -1)) {
						logger.info("Linking template {" + toTemplate.getName() + "} to folder {" + folder.getId() + "} of Node {"
								+ request.getMapping().getNodeId() + "}");
						toTemplate.addFolder(folder);
					}
				}
				toTemplate.save();
			}

			// Update job status
			int nObjectOmitted = getOmittedObjectsCount();
			if (nObjectOmitted > 0) {
				logger.error("This job did complete with warning because {" + nObjectOmitted
						+ "} objects were omitted. Check the log for more details.");
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_COMPLETED_WITH_WARNINGS);
			} else {
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_COMPLETED);
			}
			logger.info("Template migration job {" + jobId + "} has been completed.");

		} catch (NodeException e) {
			logger.error("Error occurred during the execution of template migration job {" + jobId + "}.", e);
			try {
				// Rollback the current transaction and mark the migration job as aborted
				t.rollback(false);
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_ERROR);
			} catch (Exception e1) {
				logger.error("Error occurred during the error handling of template migration job {" + jobId + "}.", e1);
			}
		}
	}

	@Override
	protected Result invokePreProcessor(com.gentics.contentnode.rest.model.Tag tag, IMigrationPreprocessor preProcessor) throws MigrationException {
		return preProcessor.apply(tag, request, logger);
	}

	/**
	 * Run the page through the post processors
	 *
	 * @param sortedProcessors
	 * @param page
	 * @return
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	private Page invokePostProcessors(List<MigrationPostProcessor> sortedProcessors, Page page) throws ReadOnlyException, NodeException {
		if (ObjectTransformer.isEmpty(sortedProcessors)) {
			return page;
		}
		Collections.sort(sortedProcessors);
		for (MigrationPostProcessor configuredPostProcessor : sortedProcessors) {
			try {
				Collection<Reference> fillRefs = new Vector<Reference>();

				fillRefs.add(Reference.CONTENT_TAGS);
				fillRefs.add(Reference.OBJECT_TAGS);
				com.gentics.contentnode.rest.model.Page restPage = ModelBuilder.getPage(page, fillRefs);

				logger.info("Applying post processor {" + configuredPostProcessor.getClassName() + "} to page {" + page.getName() + "}.");
				IMigrationPostprocessor postProcessor = (IMigrationPostprocessor) Class.forName(configuredPostProcessor.getClassName())
						.newInstance();

				postProcessor.applyPostMigrationProcessing(restPage, request, logger);

				// Convert restPage back to NodeObject
				page = ModelBuilder.getPage(restPage, false);
			} catch (Exception e) {
				throw new NodeException("Error while applying post processor: {" + configuredPostProcessor.getClassName() + "}", e);
			}
		}
		return page;
	}


	/**
	 * Checks whether the given mapping is valid and whether it contains all mandatory information
	 * 
	 * @param mapping
	 * @return
	 */
	private boolean isMappingValid(TemplateMigrationMapping mapping) {

		if (mapping.getFromTemplateId() == null || mapping.getToTemplateId() == null) {
			logger.error("The mapping is missing the from template or the to template information.");
			return false;
		}

		if (mapping.getEditableTagMappings() == null || mapping.getNonEditableTagMappings() == null) {
			logger.error("The mapping does not contain a list of editable or non editable tag mappings.");
			return false;
		}

		if (mapping.getNodeId() == null) {
			logger.error("The mapping is missing a valid node id.");
			return false;
		}

		logger.debug("Checking editable tagmappings.");
		for (TemplateMigrationEditableTagMapping tagmapping : mapping.getEditableTagMappings()) {
			if (tagmapping.getFromTagId() == null) {
				logger.error("At least one editable tagmapping did not contain a valid from tag information.");
				return false;
			}

			if (tagmapping.getPartMappings() == null) {
				logger.error("At least one editable tagmapping did not contain a list of part mappings.");
				return false;
			}

			if (tagmapping.getToTagId() == null) {
				logger.error("At least one editable tagmapping did not contain a valid to tag information.");
				return false;
			}
		}

		logger.debug("Checking non editable tagmappings");

		for (TemplateMigrationNonEditableTagMapping tagmapping : mapping.getNonEditableTagMappings()) {
			if (tagmapping.getFromTagId() == null) {
				logger.error("At least one editable tagmapping did not contain a valid from tag information.");
				return false;
			}

			if (tagmapping.getToTagId() == null) {
				logger.error("At least one editable tagmapping did not contain a valid to tag information.");
				return false;
			}
		}
		return true;
	}

	/**
	 * Retrieve a template tag in a given template with a given ID
	 * 
	 * @param tagId
	 *            the ID of the tag to retrieve
	 * @param template
	 *            the template to search in
	 * @return
	 * @throws NodeException
	 */
	private TemplateTag getTemplateTag(Integer tagId, Template template) throws NodeException {
		for (TemplateTag tag : template.getTemplateTags().values()) {
			if (ObjectTransformer.getInteger(tag.getId(), -1) == tagId.intValue()) {
				return tag;
			}
		}
		return null;
	}

	/**
	 * Perform a template migration on a given page
	 * 
	 * @param page
	 *            the page to apply the migration to
	 * @param fromTemplate
	 *            the source template
	 * @param toTemplate
	 *            the target template
	 * @return migration result
	 * @throws NodeException
	 */
	private Result migratePage(Page page, Template fromTemplate, Template toTemplate) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();

		// Iterate over all content tags in the current page
		for (Tag tag : page.getTags().values()) {
			int fromTagTypeId = ObjectTransformer.getInt(tag.getConstruct().getId(), -1);

			// Iterate over all tag mappings
			for (TemplateMigrationTagMapping tagMapping : mapping.getEditableTagMappings()) {

				// Get the source and target tag IDs for each mapping
				Integer fromTagId = tagMapping.getFromTagId();
				Integer toTagId = tagMapping.getToTagId();
				TemplateTag fromTemplateTag = getTemplateTag(fromTagId, fromTemplate);
				TemplateTag toTemplateTag = getTemplateTag(toTagId, toTemplate);

				if (fromTemplateTag == null) {
					throw new NodeException("The tag with id {" + fromTagId + "} could not be found.");
				}

				int fromTemplateTagTagTypeId = ObjectTransformer.getInt(fromTemplateTag.getConstruct().getId(), -1);

				// Check if the current tag is included in the mapping. Compare the keyname and construct type
				if (fromTemplateTag.getName().equalsIgnoreCase(tag.getName()) && fromTemplateTagTagTypeId == fromTagTypeId) {

					String originalTagKeyword = tag.getConstruct().getKeyword();

					logger.info("Beginning migration for editable tag {" + tag.getId() + "} with name {" + tag.getName() + "} of tag type {"
							+ originalTagKeyword + "} in page {" + page.getName() + "}.");

					// Create TagTypeMigrationMapping from TemplateMigrationMapping
					TagTypeMigrationMapping ttmMapping = new TagTypeMigrationMapping();

					ttmMapping.setFromTagTypeId(fromTagTypeId);
					ttmMapping.setToTagTypeId(getTagTypeIdOfTargetTag(toTemplate, toTagId));
					ttmMapping.setPartMappings(((TemplateMigrationEditableTagMapping) tagMapping).getPartMappings());

					Result result = invokePreProcessors(tag);
					if (result == Result.skipobject) {
						logger.info("Preprocessors requested skipping of object");
						return result;
					}
					if (result == Result.skiptag) {
						logger.info(String.format("Preprocessors requested skipping of tag {%d}", tag.getId()));
						break;
					}

					// Apply mapping
					MigrationHelper.migrateTag(t, logger, tag, ttmMapping);

					// rename tag
					tag.setName(toTemplateTag.getName());

					// Log that the tag migration is completed
					logger.info("Tag {" + tag.getId() + "} previously of tag type {" + originalTagKeyword + "} in page {" + page.getName()
							+ "} was successfully migrated to {" + tag.getConstruct().getKeyword() + "}.");
				}
			}
		}
		return Result.pass;
	}

	/**
	 * Update tag references in migrated pages
	 * 
	 * @param page
	 *            the page being migrated
	 * @param fromTemplate
	 *            the template being migrated from
	 * @param toTemplate
	 *            the template being migrated to
	 * @throws NodeException
	 */
	private void updateTagReferences(Page page, Template fromTemplate, Template toTemplate) throws NodeException {
		Transaction t = TransactionManager.getCurrentTransaction();
		logger.info("Checking for references to migrated tags in page {" + page.getName() + "}.");

		// Retrieve all editable and noneditable tags in the mapping
		List<TemplateMigrationTagMapping> allMappings = new ArrayList<TemplateMigrationTagMapping>();

		allMappings.addAll(mapping.getEditableTagMappings());
		allMappings.addAll(mapping.getNonEditableTagMappings());

		// Create hashmap of all tag mappings
		Map<String, String> references = new HashMap<String, String>();

		for (TemplateMigrationTagMapping tagMapping : allMappings) {

			// Get the source and target tags for each mapping
			Integer fromTagId = tagMapping.getFromTagId();
			Integer toTagId = tagMapping.getToTagId();
			TemplateTag fromTag = getTemplateTag(fromTagId, fromTemplate);
			TemplateTag toTag = getTemplateTag(toTagId, toTemplate);

			// Add each mapping to the hashmap
			if (toTag != null) {
				references.put(fromTag.getName(), toTag.getName());
			} else {
				references.put(fromTag.getName(), null);
			}

			// Iterate over all content tags in the current page
			for (Tag tag : page.getTags().values()) {
				for (Map.Entry<String, String> entry : references.entrySet()) {
					try {
						MigrationHelper.checkReferences(t, logger, tag, entry.getKey(), entry.getValue());
					} catch (ReadOnlyException e) {// logger.error("Unable to update references because the part is not editable.", e);
					}
				}
			}
		}

		// TODO check for circular references
		logger.info("Finished updating references to migrated tags in page {" + page.getName() + "}.");
	}

	/**
	 * Retrieve the tag type (construct) ID of a given tag in a template
	 * 
	 * @param toTemplate
	 *            the template of the tag
	 * @param toTagId
	 *            the ID of the tag
	 * @return
	 * @throws NodeException
	 */
	private int getTagTypeIdOfTargetTag(Template toTemplate, Integer toTagId) throws NodeException {

		for (TemplateTag templateTag : toTemplate.getTemplateTags().values()) {
			// Check if the current tag is the tag being mapped to
			int currentTagId = ObjectTransformer.getInt(templateTag.getId(), -1);

			if (currentTagId == toTagId) {
				return ObjectTransformer.getInt(templateTag.getConstruct().getId(), 0);
			}
		}
		return 0;
	}

	/**
	 * Retrieve all IDs for objects that should also be migrated
	 * 
	 * @return a set of Object IDs to apply the migration mappings to
	 * @throws NodeException
	 */
	private Set<Integer> getMigrationObjectIds(Template fromTemplate) throws NodeException {

		Set<Integer> migrationObjectIds = new HashSet<Integer>();

		// Iterate over all pages that are based on the source template
		for (Page page : fromTemplate.getPages()) {

			// Restrict migration to the node specified in the request
			if (ObjectTransformer.getInt(page.getFolder().getNode().getId(), -1) != mapping.getNodeId()) {
				continue;
			}
			migrationObjectIds.add(ObjectTransformer.getInt(page.getId(), -1));
		}
		return migrationObjectIds;
	}

	/**
	 * Mark a collection of tags that will be skipped during tag type migration because a lock could not be acquired on their parent object
	 * 
	 * @param tags
	 *            collection of tags to be skipped during tag type migration
	 */
	@Override
	protected void markSkippedTags(Collection<? extends Tag> tags) {

		// Check all tags in the collection if they are included in the mapping
		for (Tag tag : tags) {
			int currentTagId = ObjectTransformer.getInt(tag.getId(), -1);

			for (TemplateMigrationEditableTagMapping editableTagMapping : mapping.getEditableTagMappings()) {

				// Get the source and target tag IDs for each mapping
				Integer fromTagId = editableTagMapping.getFromTagId();

				// Check if the current tag is included in the mapping
				if (currentTagId == fromTagId) {
					logger.error("Migration of " + tag.getClass().getSimpleName() + " could not be performed on tag {" + tag.getId()
							+ "} because its parent object is locked.");
				}
			}
		}
	}

	@Override
	protected List<MigrationPreProcessor> getEnabledPreProcessors() {
		return request.getEnabledPreProcessors();
	}
}
