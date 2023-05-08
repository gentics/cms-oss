package com.gentics.contentnode.migration.jobs;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.quartz.JobDataMap;

import com.gentics.api.contentnode.migration.IMigrationPostprocessor;
import com.gentics.api.contentnode.migration.IMigrationPreprocessor;
import com.gentics.api.contentnode.migration.MigrationException;
import com.gentics.api.contentnode.migration.IMigrationPreprocessor.Result;
import com.gentics.api.lib.etc.ObjectTransformer;
import com.gentics.api.lib.exception.NodeException;
import com.gentics.api.lib.exception.ReadOnlyException;
import com.gentics.contentnode.db.DBUtils;
import com.gentics.contentnode.events.TransactionalTriggerEvent;
import com.gentics.contentnode.factory.NodeFactory;
import com.gentics.contentnode.migration.MigrationDBLogger;
import com.gentics.contentnode.migration.MigrationHelper;
import com.gentics.contentnode.migration.MigrationPartMapper;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.Content;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.File;
import com.gentics.contentnode.object.Folder;
import com.gentics.contentnode.object.ImageFile;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.NodeObject;
import com.gentics.contentnode.object.ObjectTag;
import com.gentics.contentnode.object.ObjectTagContainer;
import com.gentics.contentnode.object.ObjectTagDefinition;
import com.gentics.contentnode.object.Overview;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.NodeObjectVersion;
import com.gentics.contentnode.object.Part;
import com.gentics.contentnode.object.SystemUser;
import com.gentics.contentnode.object.Tag;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.Value;
import com.gentics.contentnode.object.parttype.OverviewPartType;
import com.gentics.contentnode.object.parttype.UrlPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.Reference;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.MigrationPostProcessor;
import com.gentics.contentnode.rest.model.migration.MigrationPreProcessor;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.rest.util.ModelBuilder;
import com.gentics.lib.db.IntegerColumnRetriever;
import com.gentics.lib.i18n.CNI18nString;

/**
 * Quartz job for performing Tagtype Migrations
 * 
 * @author Taylor
 * 
 */
public class TagTypeMigrationJob extends AbstractMigrationJob {

	/**
	 * Tag Type Migration mappings object
	 */
	protected List<TagTypeMigrationMapping> mappings;

	/**
	 * Tag Type Migration request
	 */
	protected TagTypeMigrationRequest request;

	/**
	 * The type of objects to be migrated
	 */
	protected String type;

	/**
	 * List of IDs of the objects the migration will be applied to
	 */
	protected List<Integer> objectIds;

	/**
	 * Should this job handle the given page as a template for other pages
	 */
	protected boolean handlePagesByTemplate;

	/**
	 * If other pages with same template are handled, should they be handled for all nodes
	 */
	protected boolean handleAllNodes;

	/**
	 * Flag that indicates whether this job should prevent trigger events
	 */
	protected boolean preventTriggerEvent;

	/**
	 * Size of the used commit batch. -1: no batching 0: commit on every object
	 */
	protected int ttmCommitBatchSize;

	/**
	 * The ID of the object to apply a reinvoke request to
	 */
	protected Integer reinvokeObjectId;

	/**
	 * The type of the object to apply a reinvoke request to
	 */
	protected String reinvokeObjectType;

	@SuppressWarnings("unchecked")
	@Override
	protected boolean getJobParameters(JobDataMap map) {
		reinvokeObjectId = (Integer) map.get(PARAM_SELECTED_ITEM_ID);
		reinvokeObjectType = (String) map.get(PARAM_SELECTED_ITEM_TYPE);
		request = (TagTypeMigrationRequest) map.get(PARAM_REQUEST);
		jobId = (Integer) map.get(PARAM_JOBID);
		type = (String) map.get(PARAM_TYPE);
		objectIds = (List<Integer>) map.get(PARAM_OBJECTIDS);
		handlePagesByTemplate = ObjectTransformer.getBoolean(map.get(PARAM_HANDLE_PAGES_BY_TEMPLATE), false);
		handleAllNodes = ObjectTransformer.getBoolean(map.get(PARAM_HANDLE_ALL_NODES), false);
		preventTriggerEvent = ObjectTransformer.getBoolean(map.get(PARAM_PREVENT_TRIGGER_EVENT), false);

		mappings = request.getMappings();

		// Check that all parameters were set
		return (!mappings.isEmpty() && (jobId != 0) && (type != null) && ("global".equals(type) || !ObjectTransformer.isEmpty(objectIds)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#getJobDescription()
	 */
	public String getJobDescription() {
		return new CNI18nString("tagtypemigrationjob").toString();
	}

	/**
	 * Migrate the set of page Ids
	 * 
	 * @param migrationObjectIds
	 * @throws NodeException
	 */
	private void handlePageMigration(Set<Integer> migrationObjectIds) throws NodeException {
		// Apply migration to all affected pages
		int nPageBatchSizeCounter = 1;
		for (Integer pageId : migrationObjectIds) {

			try {
				if (!migratePage(pageId)) {
					updatePercentCompleted();
					continue;
				}
			} catch (NodeException e) {
				String msg = "Error while migrating page {" + pageId + "}";
				logger.debug(msg, e);
				dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_ERROR);
				throw new NodeException(msg, e);
			}

			if (ttmCommitBatchSize != -1 && nPageBatchSizeCounter >= ttmCommitBatchSize) {
				logger.debug("The current batch size reached {" + nPageBatchSizeCounter + "}. Committing transaction.");
				t.commit(false);
				logger.debug("Batch committed");
				nPageBatchSizeCounter = 0;
			} else {
				nPageBatchSizeCounter++;
			}

			updatePercentCompleted();
		}
	}

	/**
	 * Migrate the set of templateIds
	 * 
	 * @param migrationObjectIds
	 * @throws NodeException
	 */
	private void handleTemplateMigration(Set<Integer> migrationObjectIds) throws NodeException {
		// Apply migration to all affected templates
		int nTemplateBatchSizeCounter = 0;
		for (Integer templateId : migrationObjectIds) {
			try {
				if (!migrateTemplate(templateId)) {
					updatePercentCompleted();
					continue;
				}

			} catch (NodeException e) {
				String msg = "Error while migrating template {" + templateId + "}";

				logger.debug(msg);
				dbLogger.updateMigrationJobItemEntry(jobId, templateId, Template.TYPE_TEMPLATE, STATUS_ERROR);
				throw new NodeException(msg, e);
			}

			if (ttmCommitBatchSize != -1 && nTemplateBatchSizeCounter > ttmCommitBatchSize) {
				logger.debug("The current batch size reached {" + nTemplateBatchSizeCounter + "}. Committing transaction.");
				t.commit(false);
				logger.debug("Batch committed");
				nTemplateBatchSizeCounter = 0;
			} else {
				nTemplateBatchSizeCounter++;
			}
			updatePercentCompleted();
		}
	}

	/**
	 * Migrate the set of objdef ids
	 * 
	 * @param migrationObjectIds
	 * @throws NodeException
	 */
	private void handleObjDefMigration(Set<Integer> migrationObjectIds) throws NodeException {
		// Apply migration to all affected object tag definitions
		int nObjectBatchSizeCounter = 0;
		for (Integer objectId : migrationObjectIds) {
			if (!migrateObjectTagDefintion(objectId)) {
				updatePercentCompleted();
				continue;
			}

			if (ttmCommitBatchSize != -1 && nObjectBatchSizeCounter > ttmCommitBatchSize) {
				logger.debug("The current batch size reached {" + nObjectBatchSizeCounter + "}. Committing transaction.");
				t.commit(false);
				logger.debug("Batch committed");
				nObjectBatchSizeCounter = 0;
			} else {
				nObjectBatchSizeCounter++;
			}
			updatePercentCompleted();
		}
	}

	/**
	 * Collect the objects, that will be affected by a global migration job
	 * @return map of Object Class -> ID-set of affected objects
	 * @throws NodeException
	 */
	private Map<Class<? extends NodeObject>, Set<Integer>> collectGlobalMigrationObjects() throws NodeException {
		Set<Integer> templateIds = new HashSet<>();
		Set<Integer> pageIds = new HashSet<>();
		Set<Integer> objTagDefIds = new HashSet<>();

		List<Integer> restrictedNodeIds = request.getRestrictedNodeIds();

		// collect the objects to migrate
		for (TagTypeMigrationMapping mapping : mappings) {
			final Construct construct = t.getObject(Construct.class, mapping.getFromTagTypeId());
			if (construct == null) {
				continue;
			}

			if (request.isHandleGlobalTemplates()) {
				for (TemplateTag templateTag : construct.getTemplateTags()) {
					Template template = templateTag.getTemplate();
					if (!restrictedNodeIds.isEmpty()) {
						boolean found = false;
						Node templateChannel = template.getChannel();
						if (templateChannel != null) {
							found = restrictedNodeIds.contains(templateChannel.getId());
						} else {
							for (Folder folder : template.getFolders()) {
								if (restrictedNodeIds.contains(folder.getNode().getId())) {
									found = true;
									break;
								}
							}
						}

						if (!found) {
							continue;
						}
					}
					templateIds.add(template.getId());
				}
			}
			if (request.isHandleGlobalPages()) {
				for (ContentTag contentTag : construct.getContentTags()) {
					Content content = (Content) contentTag.getContainer();
					for (Page page : content.getPages()) {
						if (!restrictedNodeIds.isEmpty()) {
							Node node = page.getChannel();
							if (node == null) {
								node = page.getOwningNode();
							}

							if (!restrictedNodeIds.contains(node.getId())) {
								continue;
							}
						}
						pageIds.add(page.getId());
					}
				}
			}
			if (request.isHandleGlobalObjTagDefs()) {
				IntegerColumnRetriever ids = new IntegerColumnRetriever("id") {
					@Override
					public void prepareStatement(PreparedStatement stmt) throws SQLException {
						stmt.setInt(1, construct.getId());
					}
				};
				DBUtils.executeStatement("SELECT id FROM objtag WHERE obj_id = 0 AND construct_id = ?", ids);

				List<ObjectTagDefinition> objTagDefs = t.getObjects(ObjectTagDefinition.class, ids.getValues());
				for (ObjectTagDefinition def : objTagDefs) {
					if (!restrictedNodeIds.isEmpty()) {
						boolean found = false;
						List<Node> nodes = def.getNodes();
						if (nodes.isEmpty()) {
							found = true;
						}
						for (Node node : nodes) {
							if (restrictedNodeIds.contains(node.getId())) {
								found = true;
								break;
							}
						}

						if (!found) {
							continue;
						}
					}
					objTagDefIds.add(def.getId());
				}
			}
		}

		Map<Class<? extends NodeObject>, Set<Integer>> map = new HashMap<>();
		if (!templateIds.isEmpty()) {
			map.put(Template.class, templateIds);
		}
		if (!pageIds.isEmpty()) {
			map.put(Page.class, pageIds);
		}
		if (!objTagDefIds.isEmpty()) {
			map.put(ObjectTagDefinition.class, objTagDefIds);
		}
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gentics.contentnode.job.AbstractUserActionJob#processAction()
	 */
	protected void processAction() {

		init();

		try {
			// Set up loggers
			logger = MigrationHelper.configureLog(logger);
			dbLogger = new MigrationDBLogger(logger);
			logger.info("Beginning tag migration job {" + jobId + "}.");

			// Log in the DB that the migration job has begun
			dbLogger.createMigrationJobEntry(jobId, TAGTYPE_MIGRATION_JOB_TYPE, mappings);
			dbLogger.updateMigrationJobEntryStatus(jobId, AbstractMigrationJob.STATUS_STARTED);

			// prepare preprocessors
			preparePreprocessors();

			// Get list of unique object IDs to apply mappings to
			Set<Integer> migrationObjectIds = getMigrationObjectIds();

			// Validate mappings with the objects they should be applied to
			if (!validateMapping(migrationObjectIds)) {
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_INVALID);
				throw new NodeException("Migration aborted due to invalid mapping.");
			}

			// Update job status
			dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_IN_PROGRESS);

			if (type.equalsIgnoreCase("page")) {
				migrationObjectCountTotal = migrationObjectIds.size();
				logger.debug("Handling {" + migrationObjectCountTotal + "} objects.");

				handleMigrationOptions();
				handlePageMigration(migrationObjectIds);
			} else if (type.equalsIgnoreCase("template")) {
				migrationObjectCountTotal = migrationObjectIds.size();
				logger.debug("Handling {" + migrationObjectCountTotal + "} objects.");

				handleMigrationOptions();
				handleTemplateMigration(migrationObjectIds);
			} else if (type.equalsIgnoreCase("objtagdef")) {
				migrationObjectCountTotal = migrationObjectIds.size();
				logger.debug("Handling {" + migrationObjectCountTotal + "} objects.");

				handleMigrationOptions();
				handleObjDefMigration(migrationObjectIds);
			} else if (type.equalsIgnoreCase("global")) {
				Map<Class<? extends NodeObject>, Set<Integer>> globalMigrationObjects = collectGlobalMigrationObjects();

				migrationObjectCountTotal = 0;
				for (Set<Integer> idSet : globalMigrationObjects.values()) {
					migrationObjectCountTotal += idSet.size();
				}
				logger.debug("Handling {" + migrationObjectCountTotal + "} objects.");

				handleMigrationOptions();

				if (globalMigrationObjects.containsKey(Page.class)) {
					handlePageMigration(globalMigrationObjects.get(Page.class));
				}
				if (globalMigrationObjects.containsKey(Template.class)) {
					handleTemplateMigration(globalMigrationObjects.get(Template.class));
				}
				if (globalMigrationObjects.containsKey(ObjectTagDefinition.class)) {
					handleObjDefMigration(globalMigrationObjects.get(ObjectTagDefinition.class));
				}
			} else {
				// Invalid object type was received
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_ERROR);
				logger.error("Invalid migration type: {" + type + "}.");
				throw new NodeException("Migration aborted due to invalid type.");
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
			logger.info("Tag migration job {" + jobId + "} has been completed. The transaction will now be committed. This may take a while.");
		} catch (NodeException e) {
			logger.error("Error occurred during the execution of tag type migration job {" + jobId + "}.", e);
			try {
				// Rollback the current transaction and mark the migration job as aborted
				t.rollback(false);
				dbLogger.updateMigrationJobEntryStatus(jobId, STATUS_ERROR);
			} catch (Exception e1) {
				logger.error("Error occurred during the error handling of tag type migration job {" + jobId + "}.", e1);
			}
		}
	}

	/**
	 * Set some additional properties
	 */
	private void init() {
		ttmCommitBatchSize = ObjectTransformer.getInt(t.getNodeConfig().getDefaultPreferences().getProperty(MIGRATION_COMMIT_BATCH_SIZE_KEY), -1);

		// prevent immediate unlocking of objects
		t.getAttributes().put(NodeFactory.UNLOCK_AT_TRX_COMMIT, true);
	}

	/**
	 * Sets various migration options
	 */
	private void handleMigrationOptions() {
		// We disable InstantPublishing to speed up the migration process
		t.setInstantPublishingEnabled(false);

		// Handle the prevent trigger event option
		if (preventTriggerEvent) {
			logger.debug("Disabling the trigger event handling. Please note that no objects will be dirted. You have to manually invoke a full publish process after the migration has been completed in order to recalculate changed depdencenies.");
			t.getAttributes().put(TransactionalTriggerEvent.PREVENT_KEY, true);
		}
	}

	/**
	 * Run the page through the post processors
	 * 
	 * @param sortedProcessors
	 * @param page
	 * @throws ReadOnlyException
	 * @throws NodeException
	 */
	private Page invokePostProcessors(List<MigrationPostProcessor> sortedProcessors, Page page) throws ReadOnlyException, NodeException {
		Page newPage = page;
		Collections.sort(sortedProcessors);
		for (MigrationPostProcessor configuredPostProcessor : sortedProcessors) {

			// Save and reload the page. Otherwise the rest page is missing new tag part properties
			newPage.save(false);
			newPage = t.getObject(Page.class, newPage.getId(), true);
			logger.debug("Saved and reloaded page {" + newPage.getId() + "}");

			try {
				Collection<Reference> fillRefs = new Vector<Reference>();

				fillRefs.add(Reference.CONTENT_TAGS);
				fillRefs.add(Reference.OBJECT_TAGS);
				fillRefs.add(Reference.TEMPLATE_SOURCE);
				// Transform the page and store tags for later use
				com.gentics.contentnode.rest.model.Page restPage = ModelBuilder.getPage(newPage, fillRefs);
				List<String> tagList = new ArrayList<String>(restPage.getTags().keySet());

				logger.info("Applying post processor {" + configuredPostProcessor.getClassName() + "} to page {" + newPage.getId() + "}.");
				IMigrationPostprocessor postProcessor = (IMigrationPostprocessor) Class.forName(configuredPostProcessor.getClassName()).newInstance();
				postProcessor.applyPostMigrationProcessing(restPage, request, logger);
				logger.debug("Applied post processor {" + configuredPostProcessor.getClassName() + "} to page {" + newPage.getId() + "}");

				// Load the tags again and determine the deleted tags
				List<String> updatedTagList = new ArrayList<String>(restPage.getTags().keySet());
				tagList.removeAll(updatedTagList);
				if (tagList.size() > 0) {
					logger.info("The post processor removed {" + tagList.size()
							+ "} tags from the page. Those tags will now be deleted from the page.");
				}

				// Convert restPage back to NodeObject
				newPage = ModelBuilder.getPage(restPage, false);
				logger.debug("Converted page back into rest model");
				ModelBuilder.deleteTags(tagList, newPage);
				if (!tagList.isEmpty()) {
					logger.debug("Deleted {" + tagList.size() + "} tags from page {" + newPage.getId() + "}");
				}

				logger.debug("Finished post processor {" + configuredPostProcessor.getClassName() + "}");
			} catch (Exception e) {
				throw new NodeException("Error while applying post processor: {" + configuredPostProcessor.getClassName() + "}", e);
			}
		}

		return newPage;
	}

	/**
	 * Apply the configured migration to a given page
	 * 
	 * @param pageId
	 *            the ID of the page to apply the migration to
	 * @return true if the migration was successful, false otherwise
	 * @throws NodeException
	 */
	private boolean migratePage(Integer pageId) throws NodeException {
		dbLogger.createMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_STARTED);
		logger.debug("Starting migration of page {" + pageId + "}");
		Page page = null;
		boolean onlineBefore = false;
		boolean modifiedBefore = false;
		SystemUser queueUser = null;
		int queuedTimestamp = -1;
		NodeObjectVersion queuedVersion = null;
		if (interrupted) {
			throw new NodeException("Migration was manually interrupted.");
		}
		try {
			// get page for editing
			logger.debug("Loading page {" + pageId + "}");
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
			page = t.getObject(Page.class, pageId, true);
			if (!PermHandler.ObjectPermission.edit.checkObject(page)) {
				dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_SKIPPED);
				logger.error("Unable to edit page {"
						+ pageId
						+ "} due to missing edit permission. The page will be skipped. The migration job can be reinvoked for the page after the permissions have been corrected.");
				return false;
			}
			logger.debug("Loaded page {" + pageId + "}");

		} catch (Exception e) {
			// Mark the tags in the page as skipped
			page = t.getObject(Page.class, pageId);
			Map<String, ContentTag> contentTags = page.getContent().getContentTags();

			markSkippedTags(contentTags.values());
			logger.error("Unable to obtain lock on page {" + pageId + "} during tag type migration.", e);
			incrementOmittedObjectsCount();
			dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_SKIPPED);
			return false;
		}

		dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_IN_PROGRESS);

		// Apply the mappings
		logger.debug("Applying mappings to page {" + pageId + "}");
		Result result = applyMappings(page.getContent().getContentTags().values());
		if (result == Result.skipobject) {
			logger.info(String.format("Skipping page {%d}.", pageId));
			page.unlock();
			incrementOmittedObjectsCount();
			dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_SKIPPED);
			return false;
		} else {
			logger.debug("Applyed mappings to page {" + pageId + "}");
		}

		// Apply post-processors
		List<MigrationPostProcessor> sortedProcessors = request.getEnabledPostProcessors();
		if (!ObjectTransformer.isEmpty(sortedProcessors)) {
			page = invokePostProcessors(sortedProcessors, page);
		}
		page.save();
		handleOriginalPageStatus(page, onlineBefore, modifiedBefore, queueUser, queuedTimestamp, queuedVersion);
		page.unlock();

		dbLogger.updateMigrationJobItemEntry(jobId, pageId, Page.TYPE_PAGE, STATUS_COMPLETED);
		return true;
	}

	/**
	 * Apply the configured migration to a given template
	 * 
	 * @param templateId
	 *            the ID of the template to apply the migration to
	 * @return true if the migration was successful, false otherwise
	 * @throws NodeException
	 */
	private boolean migrateTemplate(Integer templateId) throws NodeException {
		dbLogger.createMigrationJobItemEntry(jobId, templateId, Template.TYPE_TEMPLATE, STATUS_STARTED);
		logger.debug("Starting migration of template {" + templateId + "}");

		Template template = null;

		// Check whether the migration was canceled
		if (interrupted) {
			throw new NodeException("Migration was manually interrupted.");
		}

		try {
			// get template for editing
			template = t.getObject(Template.class, templateId, true);
		} catch (Exception e) {
			// Mark the tags in the page as skipped
			template = t.getObject(Template.class, templateId);
			Map<String, TemplateTag> templateTags = template.getTags();

			markSkippedTags(templateTags.values());
			logger.error("Unable to obtain lock on template {" + templateId + "} during tag type migration.");
			incrementOmittedObjectsCount();
			return false;
		}

		if (!PermHandler.ObjectPermission.edit.checkObject(template)) {
			dbLogger.updateMigrationJobItemEntry(jobId, templateId, Template.TYPE_TEMPLATE, STATUS_SKIPPED);
			logger.error("Unable to edit template {"
					+ templateId
					+ "} due to missing edit permission. The template will be skipped. The migration job can be reinvoked for the template after the permissions have been corrected.");
			return false;
		}

		// Apply the mappings
		Map<String, TemplateTag> templateTags = template.getTags();

		dbLogger.updateMigrationJobItemEntry(jobId, templateId, Template.TYPE_TEMPLATE, STATUS_IN_PROGRESS);
		Result result = applyMappings(templateTags.values());
		if (result == Result.skipobject) {
			logger.info(String.format("Skipping template {%d}.", templateId));
			template.unlock();
			incrementOmittedObjectsCount();
			dbLogger.updateMigrationJobItemEntry(jobId, templateId, Template.TYPE_TEMPLATE, STATUS_SKIPPED);
			return false;
		} else {
			logger.debug("Applied mappings to template {" + templateId + "}");
		}

		// Apply post-processors
		List<MigrationPostProcessor> sortedProcessors = request.getEnabledPostProcessors();
		if (!ObjectTransformer.isEmpty(sortedProcessors)) {
			Collections.sort(sortedProcessors);
			template = invokePostProcessors(sortedProcessors, template);
		}

		template.save();
		template.unlock();
		dbLogger.updateMigrationJobItemEntry(jobId, templateId, Template.TYPE_TEMPLATE, STATUS_COMPLETED);
		return true;
	}

	@Override
	protected List<MigrationPreProcessor> getEnabledPreProcessors() {
		return request.getEnabledPreProcessors();
	}

	@Override
	protected Result invokePreProcessor(com.gentics.contentnode.rest.model.Tag tag, IMigrationPreprocessor preProcessor) throws MigrationException {
		return preProcessor.apply(tag, request, logger);
	}

	/**
	 * Run the given template through the post processors
	 * 
	 * @param sortedProcessors
	 * @param template
	 * @throws NodeException
	 */
	private Template invokePostProcessors(List<MigrationPostProcessor> sortedProcessors, Template template) throws NodeException {
		Template newTemplate = template;
		for (MigrationPostProcessor configuredPostProcessor : sortedProcessors) {

			try {
				// save and reload the template
				// otherwise, the migration would be lost by transforming between NodeObject and REST Model
				newTemplate.save();
				newTemplate = t.getObject(Template.class, newTemplate.getId(), true);

				Collection<Reference> fillRefs = new Vector<Reference>();

				fillRefs.add(Reference.TEMPLATE_TAGS);
				fillRefs.add(Reference.TEMPLATE_SOURCE);
				fillRefs.add(Reference.OBJECT_TAGS);
				com.gentics.contentnode.rest.model.Template restTemplate = ModelBuilder.getTemplate(newTemplate, fillRefs);

				logger.info("Applying post processor {" + configuredPostProcessor.getClassName() + "} to template {" + newTemplate.getName() + "}.");
				IMigrationPostprocessor postProcessor = (IMigrationPostprocessor) Class.forName(configuredPostProcessor.getClassName()).newInstance();

				postProcessor.applyPostMigrationProcessing(restTemplate, request, logger);

				// convert restTemplate back to NodeObject
				newTemplate = ModelBuilder.getTemplate(restTemplate);
			} catch (Exception e) {
				throw new NodeException("Error while applying post processor: {" + configuredPostProcessor.getClassName() + "}", e);
			}
		}

		return newTemplate;
	}

	/**
	 * Run the given folder through the post processors
	 * @param sortedProcessors sorted list of post processors
	 * @param folder folder
	 * @return returned folder
	 * @throws NodeException
	 */
	private Folder invokePostProcessors(List<MigrationPostProcessor> sortedProcessors, Folder folder) throws NodeException {
		Folder newFolder = folder;
		for (MigrationPostProcessor configuredPostProcessor : sortedProcessors) {
			try {
				newFolder.save();
				newFolder = t.getObject(Folder.class, newFolder.getId(), true);

				com.gentics.contentnode.rest.model.Folder restFolder = ModelBuilder.getFolder(newFolder, null, Arrays.asList(Reference.TAGS));

				logger.info("Applying post processor {" + configuredPostProcessor.getClassName() + "} to folder {" + newFolder.getName() + "}.");
				IMigrationPostprocessor postProcessor = (IMigrationPostprocessor) Class.forName(configuredPostProcessor.getClassName()).newInstance();

				postProcessor.applyPostMigrationProcessing(restFolder, request, logger);

				// convert restFolder back to NodeObject
				newFolder = ModelBuilder.getFolder(restFolder);
			} catch (Exception e) {
				throw new NodeException("Error while applying post processor: {" + configuredPostProcessor.getClassName() + "}", e);
			}
		}
		return newFolder;
	}

	/**
	 * Run the given file through the post processors
	 * @param sortedProcessors sorted list of post processors
	 * @param file file
	 * @return returned file
	 * @throws NodeException
	 */
	private File invokePostProcessors(List<MigrationPostProcessor> sortedProcessors, File file) throws NodeException {
		File newFile = file;
		for (MigrationPostProcessor configuredPostProcessor : sortedProcessors) {
			try {
				newFile.save();
				newFile = t.getObject(File.class, newFile.getId(), true);

				com.gentics.contentnode.rest.model.File restFile = ModelBuilder.getFile(newFile, Arrays.asList(Reference.TAGS));

				logger.info("Applying post processor {" + configuredPostProcessor.getClassName() + "} to file {" + newFile.getName() + "}.");
				IMigrationPostprocessor postProcessor = (IMigrationPostprocessor) Class.forName(configuredPostProcessor.getClassName()).newInstance();

				postProcessor.applyPostMigrationProcessing(restFile, request, logger);

				// convert restFile back to NodeObject
				newFile = ModelBuilder.getFile(restFile);
			} catch (Exception e) {
				throw new NodeException("Error while applying post processor: {" + configuredPostProcessor.getClassName() + "}", e);
			}
		}
		return newFile;
	}

	/**
	 * Run the given image through the post processors
	 * @param sortedProcessors sorted list of post processors
	 * @param image image
	 * @return returned image
	 * @throws NodeException
	 */
	private ImageFile invokePostProcessors(List<MigrationPostProcessor> sortedProcessors, ImageFile image) throws NodeException {
		ImageFile newImage = image;
		for (MigrationPostProcessor configuredPostProcessor : sortedProcessors) {
			try {
				newImage.save();
				newImage = t.getObject(ImageFile.class, newImage.getId(), true);

				com.gentics.contentnode.rest.model.Image restImage = ModelBuilder.getImage(newImage, Arrays.asList(Reference.TAGS));

				logger.info("Applying post processor {" + configuredPostProcessor.getClassName() + "} to image {" + newImage.getName() + "}.");
				IMigrationPostprocessor postProcessor = (IMigrationPostprocessor) Class.forName(configuredPostProcessor.getClassName()).newInstance();

				postProcessor.applyPostMigrationProcessing(restImage, request, logger);

				// convert restImageFile back to NodeObject
				newImage = ModelBuilder.getImage(restImage);
			} catch (Exception e) {
				throw new NodeException("Error while applying post processor: {" + configuredPostProcessor.getClassName() + "}", e);
			}
		}
		return newImage;
	}

	/**
	 * Apply the configured migration to a given object tag definition
	 * 
	 * @param objectTagDefId
	 *            the ID of the object tag definition to apply the migration to
	 * @return true if the migration was successful, false otherwise
	 * @throws NodeException
	 */
	private boolean migrateObjectTagDefintion(Integer objectTagDefId) throws NodeException {

		ObjectTagDefinition objectTagDefinition = null;
		logger.debug("Starting migration of object tag definition {" + objectTagDefId + "}");

		// Check whether the migration was canceled
		if (interrupted) {
			throw new NodeException("Migration was manually interrupted.");
		}

		try {
			// get object tag definition for editing
			objectTagDefinition = t.getObject(ObjectTagDefinition.class, objectTagDefId, true);
		} catch (Exception e) {
			// Mark the tags in the object tag definition as skipped
			objectTagDefinition = t.getObject(ObjectTagDefinition.class, objectTagDefId);
			List<ObjectTag> objectTags = objectTagDefinition.getObjectTags();

			markSkippedTags(objectTags);
			incrementOmittedObjectsCount();
			logger.error("Unable to obtain lock on object tag definition {" + objectTagDefId + "} during tag type migration.");
			return false;
		}

		String name = objectTagDefinition.getObjectTag().getName();
		if (name.startsWith("object.")) {
			name = name.substring("object.".length());
		}

		// Apply the mappings on the object tag definition itself
		applyMappings(Collections.singleton(objectTagDefinition.getObjectTag()));

		// Remember all container objects so that post processors can be applied to them following the migration
		Set<NodeObject> containers = new HashSet<NodeObject>();

		// Get all tags based on the object tag definition
		for (ObjectTag objectTag : objectTagDefinition.getObjectTags()) {

			// If a reinvoke process is being performed, check if the current object was part of the reinvoke request
			Integer objectTagId = ObjectTransformer.getInteger(objectTag.getId(), -1);

			if (reinvokeObjectId != null && objectTagId.intValue() != reinvokeObjectId) {
				return false;
			}

			// Retrieve parent object information for logging
			NodeObject container = objectTag.getNodeObject();

			if (container != null) {

				containers.add(container);

				Integer parentObjectId = ObjectTransformer.getInteger(container.getId(), -1);
				Integer parentObjectType = ObjectTransformer.getInteger(container.getTType(), -1);

				dbLogger.createMigrationJobItemEntry(jobId, parentObjectId, parentObjectType, STATUS_STARTED);

				logger.info("Checking permissions for parent object {" + parentObjectId + "} of type {" + parentObjectType + "}.");
				if (!PermHandler.ObjectPermission.edit.checkObject(container)) {
					dbLogger.updateMigrationJobItemEntry(jobId, parentObjectId, parentObjectType, STATUS_SKIPPED);
					logger.error("Unable to edit object {"
							+ parentObjectId
							+ "} of type {"
							+ parentObjectType
							+ "} due to missing edit permission. The object will be skipped. The migration job can be reinvoked for the object after the permissions have been corrected.");
					return false;
				}
				try {
					boolean onlineBefore = false;
					boolean modifiedBefore = false;
					SystemUser queueUser = null;
					int queuedTimestamp = -1;
					NodeObjectVersion queuedVersion = null;
					Page page = null;

					// Acquire lock on container
					container = t.getObject(container.getObjectInfo().getObjectClass(), container.getId(), true);

					if (container instanceof Page) {
						page = (Page)container;
						onlineBefore = page.isOnline();
						modifiedBefore = page.isModified();
						queueUser = page.getPubQueueUser();
						if (page.getTimePubQueue() != null) {
							queuedTimestamp = page.getTimePubQueue().getIntTimestamp();
						}
						queuedVersion = page.getTimePubVersionQueue();
					}

					if (container instanceof ObjectTagContainer) {
						ObjectTag tag = ((ObjectTagContainer) container).getObjectTag(name);

						applyMappings(Collections.singleton(tag));
					}

					container = applyPostProcessors(container);

					container.save();

					if (page != null) {
						handleOriginalPageStatus(page, onlineBefore, modifiedBefore, queueUser, queuedTimestamp, queuedVersion);
					}

					container.unlock();

				} catch (NodeException e) {
					String msg = "Error while applying mapping for object {" + parentObjectId + "} of type {" + parentObjectType + "} for tag {"
							+ objectTagId + "}";

					dbLogger.updateMigrationJobItemEntry(jobId, parentObjectId, parentObjectType, STATUS_ERROR);
					throw new NodeException(msg, e);
				}

				dbLogger.updateMigrationJobItemEntry(jobId, parentObjectId, parentObjectType, STATUS_COMPLETED);
			}
		}

		objectTagDefinition.save();
		objectTagDefinition.unlock();
		updatePercentCompleted();
		return true;
	}

	/**
	 * Retrieve the IDs of all page variants, language variants and localized versions of a given page
	 * 
	 * @param page
	 *            the page to retrieve page variants, language variants and localized versions for
	 * @return a set of IDs of the page variants, language variants and localized versions of the given page
	 */
	private Set<Integer> getPageVariantsAndLocalizedPages(Page page) {

		// Collection of Page IDs to return
		Set<Integer> pageVariantsAndLocalizedPageIds = new HashSet<Integer>();
		Integer pageId = ObjectTransformer.getInteger(page.getId(), null);

		try {
			// Get all page variants
			for (Page pageVariant : page.getPageVariants()) {
				Integer pageVariantId = ObjectTransformer.getInteger(pageVariant.getId(), null);

				if (pageVariantId.intValue() != pageId.intValue()) {
					pageVariantsAndLocalizedPageIds.add(pageVariantId);
					logger.info("Migration will also be applied to the page variant {" + pageVariantId + "} of page {" + pageId + "}.");
				}
			}

			// Get all language variants
			for (Page languageVariant : page.getLanguageVariants(true)) {
				Integer languageVariantId = ObjectTransformer.getInteger(languageVariant.getId(), null);

				if (languageVariantId.intValue() != pageId.intValue()) {
					pageVariantsAndLocalizedPageIds.add(languageVariantId);
					logger.info("Migration will also be applied to the language variant {" + languageVariantId + "} of page {" + pageId + "}.");
				}
			}

			// Get all localized pages
			for (Object object : page.getChannelSet().values()) {
				Integer localizedId = ObjectTransformer.getInteger(object, null);

				if (localizedId != pageId.intValue()) {
					pageVariantsAndLocalizedPageIds.add(localizedId);
					logger.info("Migration will also be applied to the localized page {" + localizedId + "} of page {" + pageId + "}.");
				}
			}

		} catch (NodeException e) {
			logger.error("Error occurred while retrieving variation or localized version of page {" + pageId + "}", e);
		}

		return pageVariantsAndLocalizedPageIds;
	}

	/**
	 * Apply tag type migration mappings to a collection of tags
	 * 
	 * @param tags
	 *            a collection of tags to apply the mappings to
	 * @return either Result.pass if object is migrated or Result.skipobject if object shall not be migrated
	 * @throws NodeException
	 */
	private Result applyMappings(Collection<? extends Tag> tags) throws NodeException {

		// Check all tags in the collection if they are included in the mapping
		for (Tag tag : tags) {
			Integer constructIdOfCurrentTag = ObjectTransformer.getInteger(tag.getConstruct().getId(), null);

			// Iterate over all mappings and determine whether there is a mapping for the construct of the current tag
			for (TagTypeMigrationMapping mapping : mappings) {

				// Check if the current tag is included in the mapping
				if (mapping.getFromTagTypeId().intValue() == constructIdOfCurrentTag.intValue()) {
					String originalTagKeyword = tag.getConstruct().getKeyword();

					// preprocessors
					logger.info(String.format("Invoking preprocessors for tag {%d} of tag type {%s}", tag.getId(), originalTagKeyword));
					Result result = invokePreProcessors(tag);
					if (result == Result.skipobject) {
						logger.info("Preprocessors requested skipping of object");
						return result;
					}
					if (result == Result.skiptag) {
						logger.info(String.format("Preprocessors requested skipping of tag {%d}", tag.getId()));
						break;
					}

					// Log that the migration has begun
					logger.info("Beginning migration for tag {" + tag.getId() + "} of tag type {" + originalTagKeyword + "}.");

					// Migrate the tag
					MigrationHelper.migrateTag(t, logger, tag, mapping);

					// Log that the migration is completed
					logger.info("Tag {" + tag.getId() + "} previously of tag type {" + originalTagKeyword + "} was successfully migrated to {"
							+ tag.getConstruct().getKeyword() + "}.");
				}
			}
		}

		return Result.pass;
	}

	/**
	 * Check if the mapping provided for a tag type migration job is valid according to the data in the tags to be migrated
	 * 
	 * @param migrationObjectIds
	 *            a set of NodeObject IDs to which the migration mapping is being applied
	 * @return true if the mapping is valid, false otherwise
	 * @throws NodeException
	 */
	private boolean validateMapping(Set<Integer> migrationObjectIds) throws NodeException {

		logger.debug("Validating provided mappings...");

		if (type.equalsIgnoreCase("page")) {
			// Iterate over all pages that will be migrated
			for (Integer pageId : migrationObjectIds) {

				// Check whether the migration was canceled
				if (interrupted) {
					throw new NodeException("Migration was manually interrupted.");
				}
				Page page = t.getObject(Page.class, pageId);

				// Iterate over all tags of the current page
				for (Tag tag : page.getContent().getContentTags().values()) {
					logger.debug("Validating tag {" + tag + "} of page {" + page + "}");
					// Iterate over all values of the current tag
					for (Value value : tag.getValues()) {
						if (!validatePartMappings(value)) {
							logger.debug("Validation failed for value {" + value + "} of tag {" + tag + "} of page {" + page + "}");
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check if a value in a tag type migration job are valid
	 * 
	 * @param value
	 *            the value to check
	 * @return true if the mapping is valid, false otherwise
	 * @throws NodeException
	 */
	private boolean validatePartMappings(Value value) throws NodeException {

		// Check if the current part is included in the mapping
		for (TagTypeMigrationMapping mapping : mappings) {
			// Iterate over all part mappings
			for (MigrationPartMapping partMapping : mapping.getPartMappings()) {

				if (value.getPartType() instanceof UrlPartType) {
					if (!validateUrlMapping(value, partMapping)) {
						logger.error("Invalid URL part mapping found: part {" + partMapping.getFromPartId() + "} was mapped to part {"
								+ partMapping.getToPartId() + "}");
						return false;
					}
				} else if (value.getPartType() instanceof OverviewPartType) {
					if (!validateOverviewMapping(value, partMapping)) {
						logger.error("Invalid Overview part mapping found: part {" + partMapping.getFromPartId() + "} was mapped to part {"
								+ partMapping.getToPartId() + "}");
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check if the mapping of an Overview value is valid
	 * 
	 * @param value
	 *            the value of type Overview part to check
	 * @param partMapping
	 *            the partMapping map provided in the tag type migration mapping
	 * @return true if the mapping is valid, false otherwise
	 * @throws NodeException
	 */
	private boolean validateOverviewMapping(Value value, MigrationPartMapping partMapping) throws NodeException {

		int valuePartId = ObjectTransformer.getInt(value.getPartId(), 0);

		// Check if the current part mapping corresponds to the value of the OverviewPart type
		if (partMapping.getFromPartId() == valuePartId) {
			OverviewPartType overviewPartType = (OverviewPartType) value.getPartType();
			Overview fromOverview = overviewPartType.getOverview();
			Part toPart = t.getObject(Part.class, partMapping.getToPartId());

			// Overview mappings cannot be null
			if (toPart == null) {
				return true;
			}

			// Fetch the destination Overview configuration
			Object partTypeObject = toPart.getPartType(toPart.getDefaultValue());

			if (partTypeObject instanceof OverviewPartType) {

				// Check if the source and destination part types are configured to display the same object types
				Overview toOverview = ((OverviewPartType) partTypeObject).getOverview();

				if (!(toOverview.getObjectType() == fromOverview.getObjectType())) {
					return false;
				}
			}

		}

		return true;
	}

	/**
	 * Check if the mapping of a value of a URL value is valid
	 * 
	 * @param value
	 *            the value of type URL part to check
	 * @param partMapping
	 *            the partMapping map provided in the tag type migration mapping
	 * @return true if the mapping is valid, false otherwise
	 * @throws NodeException
	 */
	private boolean validateUrlMapping(Value value, MigrationPartMapping partMapping) throws NodeException {

		int valuePartId = ObjectTransformer.getInt(value.getPartId(), 0);

		// Check if the current part mapping corresponds to the value of the UrlPart type
		if (partMapping.getFromPartId() == valuePartId) {
			Part toPart = t.getObject(Part.class, partMapping.getToPartId());

			// Mapping to null is valid
			if (toPart == null && partMapping.isMarkedAsNotMapped()) {
				return true;
			}

			UrlPartType urlPartType = (UrlPartType) value.getPartType();
			boolean isFromInternal = urlPartType.getInternal() > 0;

			// Mapping to another URL Part
			if (toPart.getPartType(value) instanceof UrlPartType) {
				boolean isToInternal = ((UrlPartType) toPart.getPartType(value)).getInternal() > 0;

				// Internal link mapped to another internal link
				if (isFromInternal && isToInternal) {
					return true;
				}

				// External link mapped to another External link
				if (!isFromInternal && !isToInternal) {
					return true;
				}
			}

			// External link mapped to Text Part
			if (!isFromInternal && MigrationPartMapper.isPartAStringValuePartType(toPart)) {
				return true;
			}

			return false;
		}

		return true;
	}

	/**
	 * Mark a collection of tags that will be skipped during tag type migration because a lock could not be acquired on their parent object
	 * 
	 * @param tags
	 *            collection of tags to be skipped during tag type migration
	 */
	@Override
	protected void markSkippedTags(Collection<? extends Tag> tags) {

		try {
			// Check all tags in the collection if they are included in the mapping
			for (Tag tag : tags) {
				Integer constructIdOfCurrentTag = ObjectTransformer.getInteger(tag.getConstruct().getId(), null);

				for (TagTypeMigrationMapping mapping : mappings) {
					// Check if the current tag is included in the mapping
					if (mapping.getFromTagTypeId().intValue() == constructIdOfCurrentTag.intValue()) {
						logger.error("Migration of " + tag.getClass().getSimpleName() + " {" + mapping.getFromTagTypeId()
								+ "} could not be performed on tag {" + tag.getId() + "} because its parent object is locked.");
					}
				}
			}
		} catch (NodeException e) {
			logger.error("An error occurred while marking tags as skipped.", e);
		}
	}

	/**
	 * Retrieve all additional object IDs for objects that should also be migrated, but were not explicitly included in the request to perform migration such as page
	 * variations and localized objects
	 * 
	 * @return a set of Object IDs to apply the migration mappings to
	 * @throws NodeException
	 */
	private Set<Integer> getMigrationObjectIds() throws NodeException {
		if ("global".equalsIgnoreCase(type)) {
			return Collections.emptySet();
		}

		Set<Integer> migrationObjectIds = new HashSet<Integer>(objectIds);

		if (type.equalsIgnoreCase("page")) {

			// Check whether the handleByTemplate setting was set and this migration should use
			// the given mapping as a template for other pages. All pages that use the same
			// template as the given page does will be used for the migration process.
			if (handlePagesByTemplate && objectIds.size() == 1) {
				// Load the sample page which is the only page in the objects list
				Page samplePage = t.getObject(Page.class, objectIds.get(0));
				if (samplePage == null) {
					logger.error("Page {"
							+ objectIds.get(0)
							+ "} could not be found. We can't proceede since this page should be used as a sample to retrive pages from the page template of this page.");
					throw new NodeException("The sample page with id {" + objectIds.get(0) + "} could not be found. Can't proceede.");
				}
				Template template = samplePage.getTemplate();
				for (Page page : template.getPages()) {
					// Check if page is valid
					if (page == null) {
						logger.debug("Could not find a page from template {" + template.getId() + "}. The page will be omitted.");
						continue;
					}
					if (!handleAllNodes && !page.getFolder().getNode().equals(samplePage.getFolder().getNode())) {
						logger.debug("The page {" + page.getId() + "} does not exist in the same node as the sample page {" + samplePage.getId()
								+ "}. The page will be omitted.");
						continue;
					}
					Integer pageId = ObjectTransformer.getInteger(page.getId(), null);
					migrationObjectIds.add(pageId);

					// Also apply mappings to all page variants and language variants
					migrationObjectIds.addAll(getPageVariantsAndLocalizedPages(page));
				}
			} else {

				// Iterate over all page IDs that were received and also retrieve any variants of the pages
				for (Integer pageId : objectIds) {
					Page page = t.getObject(Page.class, pageId);

					// Check if page is valid
					if (page == null) {
						logger.error("Page {" + pageId + "} could not be found and will be skipped during migration.");
						migrationObjectIds.remove(pageId);
						incrementOmittedObjectsCount();
						continue;
					}

					// Also apply mappings to all page variants and language variants
					migrationObjectIds.addAll(getPageVariantsAndLocalizedPages(page));
				}
			}

		} else if (type.equalsIgnoreCase("template")) {

			for (Integer templateId : objectIds) {
				Template template = t.getObject(Template.class, templateId);

				// Check if template is valid
				if (template == null) {
					logger.error("Template {" + templateId + "} could not be found and will be skipped during migration.");
					migrationObjectIds.remove(templateId);
					incrementOmittedObjectsCount();
					continue;
				}

				// Also apply mappings to all localized templates
				for (Object object : template.getChannelSet().values()) {
					Integer localizedId = ObjectTransformer.getInteger(object, null);

					if (localizedId != templateId.intValue()) {
						migrationObjectIds.add(localizedId);
						logger.info("Migration will also be applied to the localized template {" + localizedId + "} of template {" + templateId
								+ "}.");
					}
				}
			}

		} else if (type.equalsIgnoreCase("objtagdef")) {

			for (Integer objectId : objectIds) {
				ObjectTagDefinition objectTagDefinition = t.getObject(ObjectTagDefinition.class, objectId);

				// Check if object tag definition is valid
				if (objectTagDefinition == null) {
					logger.error("ObjectTagDefinition {" + objectId + "} could not be found and will be skipped during migration.");
					migrationObjectIds.remove(objectId);
					incrementOmittedObjectsCount();
					continue;
				}
			}

		}
		return migrationObjectIds;
	}

	public List<TagTypeMigrationMapping> getMappings() {
		return mappings;
	}

	public void setMappings(List<TagTypeMigrationMapping> mappings) {
		this.mappings = mappings;
	}

	/**
	 * Apply configured post processors
	 * 
	 * @param container
	 *            the NodeObject to apply the post processor to
	 * @return object returned by the post processors
	 * @throws NodeException
	 */
	protected NodeObject applyPostProcessors(NodeObject container) throws NodeException {
		List<MigrationPostProcessor> sortedProcessors = request.getEnabledPostProcessors();
		if (ObjectTransformer.isEmpty(sortedProcessors)) {
			return container;
		}
		Collections.sort(sortedProcessors);

		if (container instanceof Template) {
			return invokePostProcessors(sortedProcessors, (Template)container);
		} else if (container instanceof Page) {
			return invokePostProcessors(sortedProcessors, (Page)container);
		} else if (container instanceof Folder) {
			return invokePostProcessors(sortedProcessors, (Folder)container);
		} else if (container instanceof File) {
			File file = (File)container;
			if (file.isImage()) {
				return invokePostProcessors(sortedProcessors, (ImageFile)file);
			} else {
				return invokePostProcessors(sortedProcessors, file);
			}
		}

		return container;
	}
}
