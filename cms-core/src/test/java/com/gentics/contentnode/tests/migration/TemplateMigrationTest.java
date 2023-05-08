package com.gentics.contentnode.tests.migration;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.job.AbstractUserActionJob;
import com.gentics.contentnode.migration.jobs.AbstractMigrationJob;
import com.gentics.contentnode.migration.jobs.TemplateMigrationJob;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationEditableTagMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationMapping;
import com.gentics.contentnode.rest.model.migration.TemplateMigrationNonEditableTagMapping;
import com.gentics.contentnode.rest.model.request.migration.TemplateMigrationRequest;
import com.gentics.contentnode.testutils.DBTestContext;
import com.gentics.lib.log.NodeLogger;

/**
 * Tests for a template tag migration
 * 
 * @author johannes2
 * 
 */
public class TemplateMigrationTest {

	@Rule
	public DBTestContext testContext = new DBTestContext();

	private static final NodeLogger logger = NodeLogger.getNodeLogger(TemplateMigrationTest.class);

	/**
	 * Returns a valid template migration mapping
	 * 
	 * @return
	 */
	private TemplateMigrationMapping getValidTemplateMigrationMapping() {
		final int FROM_TEMPLATE_ID = 90;
		final int TO_TEMPLATE_ID = 45;

		TemplateMigrationMapping mapping = new TemplateMigrationMapping();

		mapping.setFromTemplateId(FROM_TEMPLATE_ID);
		mapping.setToTemplateId(TO_TEMPLATE_ID);

		// Editable Template Tag
		ArrayList<TemplateMigrationEditableTagMapping> editableTagMappings = new ArrayList<TemplateMigrationEditableTagMapping>();
		TemplateMigrationEditableTagMapping editableTagMapping = new TemplateMigrationEditableTagMapping();

		editableTagMapping.setFromTagId(10);
		editableTagMapping.setToTagId(10);
		ArrayList<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();
		MigrationPartMapping partMapping = new MigrationPartMapping();

		partMapping.setFromPartId(10);
		partMapping.setToPartId(20);
		partMapping.setPartMappingType("tag");
		partMappings.add(partMapping);
		editableTagMapping.setPartMappings(partMappings);
		editableTagMappings.add(editableTagMapping);
		mapping.setEditableTagMappings(editableTagMappings);

		// Non Editable Template Tag
		ArrayList<TemplateMigrationNonEditableTagMapping> nonEditableTagMappings = new ArrayList<TemplateMigrationNonEditableTagMapping>();
		TemplateMigrationNonEditableTagMapping nonEditableTagMapping = new TemplateMigrationNonEditableTagMapping();

		nonEditableTagMapping.setFromTagId(1);
		nonEditableTagMapping.setFromTagId(2);
		nonEditableTagMappings.add(nonEditableTagMapping);
		mapping.setNonEditableTagMappings(nonEditableTagMappings);
		logger.debug("Created valid template migration mapping.");

		return mapping;
	}

	@Test
	public void testPerformMigration() throws Exception {

		Transaction t = testContext.startTransactionWithPermissions(true);

		// 1. Create a valid mapping
		TemplateMigrationMapping mapping = getValidTemplateMigrationMapping();

		// Create request object
		TemplateMigrationRequest request = new TemplateMigrationRequest();

		request.setMapping(mapping);

		// Set the options
		HashMap<String, String> options = new HashMap<String, String>();

		options.put(TemplateMigrationRequest.LINK_FOLDER_OPTION, "true");
		request.setOptions(options);

		// Create and execute job
		TemplateMigrationJob job = new TemplateMigrationJob();

		job.addParameter(AbstractMigrationJob.PARAM_REQUEST, request);
		job.addParameter(AbstractUserActionJob.PARAM_SESSIONID, t.getSessionId());
		job.addParameter(AbstractUserActionJob.PARAM_USERID, t.getUserId());

		assertTrue(job.execute(10000));

	}
}
