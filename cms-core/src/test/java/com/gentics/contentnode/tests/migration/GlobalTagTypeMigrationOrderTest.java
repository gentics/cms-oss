package com.gentics.contentnode.tests.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.api.lib.exception.NodeException;
import com.gentics.contentnode.factory.Transaction;
import com.gentics.contentnode.factory.TransactionManager;
import com.gentics.contentnode.factory.Trx;
import com.gentics.contentnode.job.AbstractUserActionJob;
import com.gentics.contentnode.migration.jobs.TagTypeMigrationJob;
import com.gentics.contentnode.object.Construct;
import com.gentics.contentnode.object.ContentTag;
import com.gentics.contentnode.object.Node;
import com.gentics.contentnode.object.Page;
import com.gentics.contentnode.object.Template;
import com.gentics.contentnode.object.TemplateTag;
import com.gentics.contentnode.object.UserGroup;
import com.gentics.contentnode.object.parttype.NormalTextPartType;
import com.gentics.contentnode.perm.PermHandler;
import com.gentics.contentnode.rest.model.migration.MigrationPartMapping;
import com.gentics.contentnode.rest.model.migration.TagTypeMigrationMapping;
import com.gentics.contentnode.rest.model.request.migration.TagTypeMigrationRequest;
import com.gentics.contentnode.tests.utils.ContentNodeRESTUtils;
import com.gentics.contentnode.tests.utils.ContentNodeTestDataUtils;
import com.gentics.contentnode.testutils.Creator;
import com.gentics.contentnode.testutils.DBTestContext;

/**
 * Test the order of a global TTM
 */
public class GlobalTagTypeMigrationOrderTest {
	public final static String LOGIN = "login";

	public final static String PASSWORD = "password";

	public final static String TAGNAME = "content";

	public final static String FROM_CONSTRUCT_KEYWORD = "migrate_from";

	public final static String FROM_PART_KEYWORD = "part_from";

	public final static String TO_CONSTRUCT_KEYWORD = "migrate_to";

	public final static String TO_PART_KEYWORD = "part_to";

	@ClassRule
	public static DBTestContext testContext = new DBTestContext();

	/**
	 * Test node
	 */
	private static Node node;

	/**
	 * Construct to migrate from
	 */
	private static Integer fromConstructId;

	/**
	 * Construct to migrate to
	 */
	private static Integer toConstructId;

	/**
	 * Template to migrate
	 */
	private Template template;

	/**
	 * Page to migrate
	 */
	private Page page;

	/**
	 * Setup static test data
	 * @throws NodeException
	 */
	@BeforeClass
	public static void setupOnce() throws NodeException {
		node = Trx.supply(() -> ContentNodeTestDataUtils.createNode());
		fromConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, NormalTextPartType.class, FROM_CONSTRUCT_KEYWORD, FROM_PART_KEYWORD));
		toConstructId = Trx.supply(() -> ContentNodeTestDataUtils.createConstruct(node, NormalTextPartType.class, TO_CONSTRUCT_KEYWORD, TO_PART_KEYWORD));
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			UserGroup nodeGroup = t.getObject(UserGroup.class, 2);
			Creator.createUser(LOGIN, PASSWORD, "name", "name", "", Arrays.asList(nodeGroup));
			PermHandler.setPermissions(Node.TYPE_NODE, node.getFolder().getId(), Arrays.asList(nodeGroup), PermHandler.FULL_PERM);
		});
	}

	/**
	 * Setup test data
	 * @throws NodeException
	 */
	@Before
	public void setup() throws NodeException {
		template = Trx.supply(() -> ContentNodeTestDataUtils.create(Template.class, template -> {
			template.setFolderId(node.getFolder().getId());
			template.setName("Template");
			template.setMlId(1);
			template.setSource("");
			TemplateTag tag = ContentNodeTestDataUtils.create(TemplateTag.class, tTag -> {
				tTag.setConstructId(fromConstructId);
				tTag.setName(TAGNAME);
				tTag.setEnabled(true);
				tTag.setPublic(true);
				tTag.getValues().getByKeyname(FROM_PART_KEYWORD).setValueText("Old value");
			}, false);
			template.getTemplateTags().put(tag.getName(), tag);
		}));
		page = Trx.supply(() -> ContentNodeTestDataUtils.create(Page.class, page -> {
			page.setFolderId(node.getFolder().getId());
			page.setName("Page");
			page.setTemplateId(template.getId());
		}));
		Trx.operate(() -> {
			template.unlock();
			page.unlock();
		});
	}

	/**
	 * Test global migration of a construct used in template and page
	 * @throws NodeException
	 */
	@Test
	public void test() throws NodeException {
		// create the migration request
		TagTypeMigrationRequest migrationRequest = Trx.supply(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();

			TagTypeMigrationMapping mapping = new TagTypeMigrationMapping();
			mapping.setFromTagTypeId(fromConstructId);
			mapping.setToTagTypeId(toConstructId);
			MigrationPartMapping partMapping = new MigrationPartMapping();

			partMapping.setFromPartId(t.getObject(Construct.class, fromConstructId).getParts().get(0).getId());
			partMapping.setToPartId(t.getObject(Construct.class, toConstructId).getParts().get(0).getId());
			List<MigrationPartMapping> partMappings = new ArrayList<MigrationPartMapping>();

			partMappings.add(partMapping);
			mapping.setPartMappings(partMappings);

			// create request
			TagTypeMigrationRequest request = new TagTypeMigrationRequest();
			request.setType("global");
			ArrayList<TagTypeMigrationMapping> mappings = new ArrayList<TagTypeMigrationMapping>();
			mappings.add(mapping);
			request.setMappings(mappings);
			request.setHandleGlobalPages(true);
			request.setHandleGlobalTemplates(true);
			request.setHandleGlobalObjTagDefs(false);
			request.setRestrictedNodeIds(Collections.emptyList());

			return request;
		});

		String sid = Trx.supply(() -> ContentNodeRESTUtils.login(LOGIN, PASSWORD));
		try (Trx trx = new Trx(sid, 2)) {
			TagTypeMigrationJob job = new TagTypeMigrationJob();
			setJobParameter(job, migrationRequest);
			assertTrue("TagTypeMigrationJob must finish in foreground", job.execute(1000));
			List<Exception> exceptions = job.getExceptions();
			for (Exception e : exceptions) {
				throw new NodeException(e);
			}
		}

		// assert that template was migrated
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			TemplateTag tag = t.getObject(template).getTemplateTag(TAGNAME);
			assertThat(tag.getConstructId()).as("Construct Id of migrated tag").isEqualTo(toConstructId);
			assertThat(tag.getValues().getByKeyname(TO_PART_KEYWORD)).as("Value").isNotNull();
			assertThat(tag.getValues().getByKeyname(TO_PART_KEYWORD).getValueText()).as("Value").isEqualTo("Old value");
		});

		// assert that page was migrated
		Trx.operate(() -> {
			Transaction t = TransactionManager.getCurrentTransaction();
			ContentTag tag = t.getObject(page).getContentTag(TAGNAME);
			assertThat(tag.getConstructId()).as("Construct Id of migrated tag").isEqualTo(toConstructId);
			assertThat(tag.getValues().getByKeyname(TO_PART_KEYWORD)).as("Value").isNotNull();
			assertThat(tag.getValues().getByKeyname(TO_PART_KEYWORD).getValueText()).as("Value").isEqualTo("Old value");
		});
	}

	/**
	 * Sets the ttm job parameters
	 * 
	 * @param job ttm Job instance
	 * @param request TTM Request
	 */
	private void setJobParameter(TagTypeMigrationJob job, TagTypeMigrationRequest request) throws NodeException {
		job.addParameter(TagTypeMigrationJob.PARAM_REQUEST, request);
		job.addParameter(TagTypeMigrationJob.PARAM_TYPE, request.getType());
		job.addParameter(TagTypeMigrationJob.PARAM_OBJECTIDS, (Serializable)request.getObjectIds());
		job.addParameter(TagTypeMigrationJob.PARAM_HANDLE_PAGES_BY_TEMPLATE, request.isHandlePagesByTemplate());
		job.addParameter(TagTypeMigrationJob.PARAM_HANDLE_ALL_NODES, request.isHandleAllNodes());
		job.addParameter(TagTypeMigrationJob.PARAM_PREVENT_TRIGGER_EVENT, request.isPreventTriggerEvent());

		Transaction t = TransactionManager.getCurrentTransaction();
		String sessionId = t.getSessionId();
		assertNotNull("SessionId must be set", sessionId);
		job.addParameter(AbstractUserActionJob.PARAM_SESSIONID, sessionId);
		job.addParameter(AbstractUserActionJob.PARAM_USERID, t.getUserId());
	}
}
